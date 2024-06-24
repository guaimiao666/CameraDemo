package com.example.camerademo;

import android.app.Application;

public class MyApplication extends Application {

    private int cameraMode; //使用camera1还是camera2 0：camera1 1：camera2
    private int cameraIndex; //使用前置还是后置摄像头 0：后置 1：前置

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

    public int getCameraIndex() {
        return cameraIndex;
    }

    public void setCameraIndex(int cameraIndex) {
        this.cameraIndex = cameraIndex;
    }
}
