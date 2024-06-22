package com.example.camerademo;

import android.app.Application;

public class MyApplication extends Application {

    private int cameraMode; //使用camera1还是camera2 0：camera1 1：camera2

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public int getCameraMode() {
        return cameraMode;
    }

    public void setCameraMode(int cameraMode) {
        this.cameraMode = cameraMode;
    }
}
