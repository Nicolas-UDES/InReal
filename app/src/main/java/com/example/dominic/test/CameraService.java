package com.example.dominic.test;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraDevice;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.view.TextureView;

/**
 * Inspiré de https://github.com/googlesamples/android-Camera2Basic
 */

public class CameraService extends Service {
    private final Context mContext;
    private final TextureView mTextureView;
    private final CameraDevice.StateCallback mStateCallback;

    public CameraService(Context context, TextureView textureView) {

        mContext = context;
        mTextureView = textureView;
        mStateCallback = new CameraTextureState(context, textureView);
        mTextureView.setSurfaceTextureListener((TextureView.SurfaceTextureListener) mStateCallback);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
