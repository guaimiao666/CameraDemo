package com.example.camerademo.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import com.example.camerademo.MyApplication;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.List;

public class Camera1Manager {

    private final String TAG = getClass().getSimpleName();
    private static Context mContext;
    private static MyApplication application;
    private int cameraId;
    private Camera mCamera;

    private static class Single {
        private static final Camera1Manager INSTANCE = new Camera1Manager();
    }

    public static Camera1Manager getInstance(Context context) {
        mContext = context;
        application = (MyApplication) context.getApplicationContext();
        return Single.INSTANCE;
    }

    public int getCameraId() {
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK
                    && cameraInfo.facing == application.getCameraIndex()) {
                return Camera.CameraInfo.CAMERA_FACING_BACK;
            } else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT
                    && cameraInfo.facing == application.getCameraIndex()) {
                return Camera.CameraInfo.CAMERA_FACING_FRONT;
            }
        }
        return -1;
    }

    public boolean openCamera() {
        if (mCamera != null)
            return false;
        cameraId = getCameraId();
        if (cameraId == -1)
            return false;
        mCamera = Camera.open(cameraId);
        return true;
    }

    public void startPreview(SurfaceHolder holder) {
        if (mCamera == null)
            return;
        try {
            mCamera.setPreviewDisplay(holder);
            setCameraDisplayOrientation();
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewFormat(ImageFormat.NV21);
            List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
            Log.d(TAG, "supportedPreviewSizes:" + new Gson().toJson(supportedPreviewSizes));
            List<Camera.Size> supportedPictureSizes = parameters.getSupportedPictureSizes();
            Log.d(TAG, "supportedPictureSizes:" + new Gson().toJson(supportedPictureSizes));
            List<String> supportedFocusModes = parameters.getSupportedFocusModes();
            Log.d(TAG, "supportedFocusModes:" + new Gson().toJson(supportedFocusModes));
            if (!supportedPreviewSizes.isEmpty()) {
                parameters.setPreviewSize(supportedPreviewSizes.get(0).width, supportedPreviewSizes.get(0).height);
            }
            if (!supportedPictureSizes.isEmpty()) {
                parameters.setPictureSize(supportedPictureSizes.get(0).width, supportedPictureSizes.get(0).height);
            }
            if (supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }
            mCamera.setParameters(parameters);
            mCamera.startPreview();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void release() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    private void setCameraDisplayOrientation() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, cameraInfo);
        int rotation = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();  //自然方向
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        //cameraInfo.orientation 图像传感方向
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (cameraInfo.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (cameraInfo.orientation - degrees + 360) % 360;
        }

        //相机预览方向
        mCamera.setDisplayOrientation(result);
    }

    public void takePicture() {
        if (mCamera == null)
            return;
        mCamera.takePicture(new Camera.ShutterCallback() {
            @Override
            public void onShutter() {

            }
        }, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes, Camera camera) {

            }
        }, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes, Camera camera) {

            }
        });
    }
}
