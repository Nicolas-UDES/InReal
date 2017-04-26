package com.example.dominic.test;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;

public class SensorEventBuffer implements SensorEventListener {
    private Handler mBackgroundHandler;

    private float mGravity[];
    private float mGeomagnetic[];
    private float orientation[] = new float[3];
    private float Rmat[] = new float[9];
    private float Imat[] = new float[9];

    public SensorEventBuffer(SensorManager sensorManager) {
        startBackgroundThread();

        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI, mBackgroundHandler);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_UI, mBackgroundHandler);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;
        if (mGravity != null && mGeomagnetic != null) {
            //On trouve l'orientation du telephone par rapport au nord magnetique
            if (SensorManager.getRotationMatrix(Rmat, Imat, mGravity, mGeomagnetic)) {
                SensorManager.getOrientation(Rmat, orientation);
            }
        }
    }

    private void startBackgroundThread() {
        HandlerThread mBackgroundThread = new HandlerThread("SensorBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // On pleure
    }

    public float getAzimuth() {
        return orientation != null && orientation.length > 0 ? (float)Math.toDegrees(orientation[0]) : 0;
    }
}
