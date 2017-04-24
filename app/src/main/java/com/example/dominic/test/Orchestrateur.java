package com.example.dominic.test;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
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
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.SizeF;
import android.view.Gravity;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class Orchestrateur extends Activity implements Runnable {
    private static final int CAMERA_PERMISSION_REQUEST = 42;
    private static final int FINE_LOCATION_PERMISSION_REQUEST = CAMERA_PERMISSION_REQUEST + 1;
    private static final int COARSE_LOCATION_PERMISSION_REQUEST = FINE_LOCATION_PERMISSION_REQUEST + 1;
    private static final int NETWORK_PERMISSION_REQUEST = COARSE_LOCATION_PERMISSION_REQUEST + 1;
    private static final int INTERNET_PERMISSION_REQUEST = NETWORK_PERMISSION_REQUEST + 1;

    private float WIDTH = 1920;
    private float HEIGHT = 1080;
    private Float PIXEL_PER_METER;
    private Float PIXEL_PER_DEGREE;

    private Boolean TESTING = false;
    private SizeF FIELD_OF_VIEW;

    private PlaceService mPlaceService;
    private GPSTracker mGpsTracker;
    private CameraService mCameraService;
    private ViewGroup mViewContainer;
    private List<TextView> mTextViewList;
    private SensorEventBuffer mSensorEventBuffer;

    private Thread loopThread;
    private Runnable setTextView;
    private Runnable clearTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Hardcode pour une question de simplicite
        FIELD_OF_VIEW = new SizeF(67, 53);

        //Set la vue
        setContentView(R.layout.activity_main);
        mViewContainer = (ViewGroup) findViewById(R.id.layoutCameraContainer);

        //On creer un pool de textView pour pas avoir a faire toujours des news
        mTextViewList = createTextViewPool(100);

        if (!TESTING) {
            getCameraPermission();
            return;
        }

        mGpsTracker = new GPSTrackerMock(this);
        mPlaceService = new PlaceServiceMock(this, 0, 160);
        mPlaceService.getPlaces(mGpsTracker.getLatitude(), mGpsTracker.getLongitude());
        mSensorEventBuffer = new SensorEventBuffer((SensorManager) getSystemService(Context.SENSOR_SERVICE));
        startThread();
    }

    private void startThread() {
        loopThread = new Thread(this);
        loopThread.start();
    }

    private void getCameraPermission() {
        if(getPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_REQUEST)){
            getFineLocationPermission();
        }
    }

    private void getFineLocationPermission() {
        if(getPermission(Manifest.permission.ACCESS_FINE_LOCATION, FINE_LOCATION_PERMISSION_REQUEST)){
            getCoarseLocationPermission();
        }
    }

    private void getCoarseLocationPermission() {
        if(getPermission(Manifest.permission.ACCESS_COARSE_LOCATION, COARSE_LOCATION_PERMISSION_REQUEST)){
            getNetworkPermission();
        }
    }

    private void getNetworkPermission() {
        if(getPermission(Manifest.permission.ACCESS_NETWORK_STATE, NETWORK_PERMISSION_REQUEST)){
            getInternetPermission();
        }
    }

    private void getInternetPermission() {
        if(getPermission(Manifest.permission.INTERNET, INTERNET_PERMISSION_REQUEST)){
            allPermissionsReceived();
        }
    }

    private void allPermissionsReceived() {
        //Creer les services
        mCameraService = new CameraService(this, (TextureView) findViewById(R.id.cameraTexture));
        mGpsTracker = new GPSTracker(this);
        mPlaceService = new PlaceService(this);
        //On va chercher les places
        mPlaceService.getPlaces(mGpsTracker.getLatitude(), mGpsTracker.getLongitude());
        //On demarre le motionSensor qui part le tout
        mSensorEventBuffer = new SensorEventBuffer((SensorManager) getSystemService(Context.SENSOR_SERVICE));
        startThread();
    }

    private boolean getPermission(String permission, int code) {
        if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, code);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case CAMERA_PERMISSION_REQUEST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getFineLocationPermission();
                }
                else {
                    getCameraPermission();
                }
                return;
            }
            case FINE_LOCATION_PERMISSION_REQUEST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getCoarseLocationPermission();
                }
                else {
                    getFineLocationPermission();
                }
                return;
            }
            case COARSE_LOCATION_PERMISSION_REQUEST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getNetworkPermission();
                }
                else {
                    getCoarseLocationPermission();
                }
                return;
            }
            case NETWORK_PERMISSION_REQUEST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getInternetPermission();
                }
                else {
                    getNetworkPermission();
                }
                return;
            }
            case INTERNET_PERMISSION_REQUEST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    allPermissionsReceived();
                }
                else {
                    getInternetPermission();
                }
                return;
            }
        }
    }

    public void run() {
        long start;
        while(true) {
            start = System.currentTimeMillis();
            processPoint();
            if(start - System.currentTimeMillis() > 16) {
                System.out.println("We took " + (start - System.currentTimeMillis() - 16) + "ms too long.");
                continue;
            }

            try {
                //60 frames/secondes environ égale à 16 millisecondes/frame
                Thread.sleep(16 - (start - System.currentTimeMillis()));
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    public float getAzimuth() {
        return (float)Math.toDegrees(mSensorEventBuffer.getOrientationX());
    }

    private float getPixelPerDegree() {
        if(PIXEL_PER_DEGREE == null) {
            PIXEL_PER_DEGREE = WIDTH/FIELD_OF_VIEW.getWidth();
        }
        return PIXEL_PER_DEGREE;
    }

    private float getPixelPerMeter() {
        if(PIXEL_PER_METER == null) {
            PIXEL_PER_METER = HEIGHT/5000;
        }
        return PIXEL_PER_METER;
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
    public void processPoint() {
        long start = SystemClock.elapsedRealtimeNanos();
        final List<InterestPoint> interestPoints = mPlaceService.getPointList();
        for (int i = 0; i < interestPoints.size(); ++i) {
            final Location myLocation = mGpsTracker.getLocation();
            if (myLocation == null) {
                continue;
            }
            final InterestPoint interestPoint = interestPoints.get(i);

            //Angle entre le nord magnetique et le point d'interet
            float bearing = myLocation.bearingTo(interestPoint.getLocation());
            float azimuth = getAzimuth();
            //Pour simplicite, on ajuste les angles entre 0 et 360, au lieu de -180 a 180
            azimuth = azimuth < 0 ? 360 + azimuth : azimuth;
            bearing = bearing < 0 ? 360 + bearing : bearing;
            float diffAngle =  azimuth - bearing;
            float angle = diffAngle < 0 ? diffAngle + 360 : diffAngle;

            final float xPosition = WIDTH/2 - (angle - 90) * getPixelPerDegree();

            //Est visible. On prend un peu plus de l'ecran pour eviter les glitch d'affichage
            //et preparer les points qui sont tout prets a etre affiches
            if (xPosition > -1000 && xPosition < WIDTH + 1000) {
                final float distance = myLocation.distanceTo(interestPoint.getLocation());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setTextView(interestPoint, xPosition, distance);
                    }
                });
            }
            else if (interestPoint.getTextView() != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        cleatTextView(interestPoint);
                    }
                });
            }
        }
        long end = SystemClock.elapsedRealtimeNanos();
        if (TESTING)  {
            System.out.println(end-start + " nanos");
        }
    }

    private void setTextView(InterestPoint interestPoint, float xPosition, float distance) {
        TextView tv = null;
        //On lui trouve un textView
        if (interestPoint.getTextView() == null) {
            if (mTextViewList.size() > 0) {
                tv = mTextViewList.remove(mTextViewList.size() - 1);
                interestPoint.setTextView(tv);

                tv.setX(xPosition);
                tv.setY(distance * getPixelPerMeter());
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
    private void cleatTextView(InterestPoint interestPoint) {
        // Je ne sais pas pourquoi c'est parfois null...
        if(interestPoint.getTextView() == null) {
            return;
        }
        interestPoint.getTextView().setVisibility(TextView.INVISIBLE);
        mTextViewList.add(interestPoint.getTextView());
        interestPoint.setTextView(null);
    }

    private List<TextView> createTextViewPool(int n) {
        final List<TextView> textViewList = new ArrayList<>();
        final Orchestrateur o = this;

        Runnable runnable = new Runnable() {
            public void run() {
                textViewList.add(GetTextView(o));
            }
        };

        for(int i = 0; i < n; ++i) {
            runOnUiThread(runnable);
        }

        return textViewList;
    }

    private TextView GetTextView(Context context) {
        TextView tv = new TextView(context);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(Color.CYAN);
        tv.setVisibility(TextView.INVISIBLE);
        return tv;
    }
}
