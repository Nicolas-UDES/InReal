package com.example.dominic.test;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.SizeF;
import android.view.Gravity;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class Orchestrateur extends Activity implements SensorEventListener {
    private Boolean TESTING = false;
    private SizeF FIELD_OF_VIEW;

    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private PlaceService mPlaceService;
    private GPSTracker mGpsTracker;
    private CameraService mCameraService;
    private SensorManager mSensorManager;
    private Sensor mAccelerometerSensor;
    private Sensor mMagneticFieldSensor;
    private ViewGroup mViewContainer;
    private List<TextView> mTextViewList;
    private Runnable pointProcessor;

    private float mGravity[];
    private float mGeomagnetic[];
    private float orientation[] = new float[3];
    private float Rmat[] = new float[9];
    private float Imat[] = new float[9];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Hardcode pour une question de simplicite
        FIELD_OF_VIEW = new SizeF(67, 53);

        //Set la vue
        setContentView(R.layout.activity_main);
        mViewContainer = (ViewGroup) findViewById(R.id.layoutCameraContainer);

        if (TESTING == true) {
            mGpsTracker = new GPSTrackerMock(this);
            mPlaceService = new PlaceServiceMock(this, 0, 160);
        }
        else {
            //Creer les services
            mGpsTracker = new GPSTracker(this);
            mPlaceService = new PlaceService(this);
        }
        mCameraService = new CameraService(this, (TextureView) findViewById(R.id.cameraTexture));

        //On creer un pool de textView pour pas avoir a faire toujours des news
        mTextViewList = createTextViewPool(100);
        //On va chercher les places
        mPlaceService.getPlaces(mGpsTracker.getLatitude(), mGpsTracker.getLongitude());

        pointProcessor = new PointsProcessor();

        //On demarre le motionSensor qui part le tout
        createMotionSensor();
    }

    private void createMotionSensor() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagneticFieldSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        startBackgroundThread();

        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 1, mBackgroundHandler);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), 1, mBackgroundHandler);

    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("SensorBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * mGravity = gravity vector
     * mGeomagnetic = geomagnetic vector
     *
     * R = rotation matrix
     * R is the identity matrix when the device is aligned with the world's coordinate system,
     * that is, when the device's X axis points toward East, the Y axis points to the North Pole
     * and the device is facing the sky.
     *
     * I is a rotation matrix
     * I is a rotation matrix transforming the geomagnetic vector into the same coordinate space
     * as gravity (the world's coordinate space). I is a simple rotation around the X axis.
     * The inclination angle in radians can be computed with getInclination(float[]).
     *
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;
        if (mGravity != null && mGeomagnetic != null) {
            boolean success = SensorManager.getRotationMatrix(Rmat, Imat, mGravity, mGeomagnetic);
            //On trouve l'orientation du telephone par rapport au nord magnetique
            if (success) {
                SensorManager.getOrientation(Rmat, orientation);
            }
        }
        //On calcul les points a afficher;
        processPoints();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    public float getAzimuth() {
        return (float)Math.toDegrees(orientation[0]);
    }

    private class PointsProcessor implements Runnable {
        float WIDTH = 1920;
        float HEIGHT = 1080;
        float PIXEL_PER_METER = HEIGHT/5000;
        float PIXEL_PER_DEGREE = WIDTH/FIELD_OF_VIEW.getWidth();
        @Override
        public void run() {
            long start = SystemClock.elapsedRealtimeNanos();
            List<InterestPoint> interestPoints = mPlaceService.getPointList();
            for (InterestPoint interestPoint : interestPoints) {
                Location myLocation = mGpsTracker.getLocation();

                if (myLocation != null) {
                    //Angle entre le nord magnetique et le point d'interet
                    float bearing = myLocation.bearingTo(interestPoint.getLocation());
                    float azimuth = getAzimuth();
                    //Pour simplicite, on ajuste les angles entre 0 et 360, au lieu de -180 a 180
                    azimuth = azimuth < 0 ? 360 + azimuth : azimuth;
                    bearing = bearing < 0 ? 360 + bearing : bearing;
                    float diffAngle =  azimuth - bearing;
                    float angle = diffAngle < 0 ? diffAngle + 360 : diffAngle;

                    float xPosition = WIDTH/2 - (angle - 90) * PIXEL_PER_DEGREE;

                    //Est visible. On prend un peu plus de l'ecran pour eviter les glitch d'affichage
                    //et preparer les points qui sont tout prets a etre affiches
                    if (xPosition > -100 && xPosition < WIDTH + 100) {
                        float distance = myLocation.distanceTo(interestPoint.getLocation());

                        TextView tv = null;
                        //On lui trouve un textView
                        if (interestPoint.getTextView() == null) {
                            if (mTextViewList.size() > 0) {
                                tv = mTextViewList.remove(mTextViewList.size() - 1);
                                interestPoint.setTextView(tv);

                                tv.setX(xPosition);
                                tv.setY(distance * PIXEL_PER_METER);
                                tv.setText(interestPoint.getName());
                                tv.setVisibility(TextView.VISIBLE);
                            }
                        }
                        //Il a deja un textView
                        else {
                            tv = interestPoint.getTextView();

                            //On smooth un peu les deplacement, sinon ca bouge trop
                            tv.setX((19 * tv.getX() + xPosition) / 20);
                        }
                    }
                    //N'est pas visible et a un textView, on le remet dans le pool
                    else if (interestPoint.getTextView() != null) {
                        interestPoint.getTextView().setVisibility(TextView.INVISIBLE);
                        mTextViewList.add(interestPoint.getTextView());
                        interestPoint.setTextView(null);
                    }
                }
            }
            long end = SystemClock.elapsedRealtimeNanos();
            if (TESTING == true)  {
                System.out.println(end-start + " nanos");
            }
        }
    }
    private void processPoints() {
        runOnUiThread(pointProcessor);
    }

    private List<TextView> createTextViewPool(int n) {
        List<TextView> textViewList = new ArrayList<>();

        for(int i = 0; i < n; ++i) {
            TextView tv = new TextView(this);
            tv.setGravity(Gravity.CENTER);
            tv.setTextColor(Color.CYAN);
            tv.setVisibility(TextView.INVISIBLE);
            mViewContainer.addView(tv);
            textViewList.add(tv);
        }
        return textViewList;
    }
}
