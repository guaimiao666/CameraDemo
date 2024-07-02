package com.example.camerademo.camera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.example.camerademo.MyApplication;
import com.example.camerademo.utils.Utils;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Camera2Manager {

    private final String TAG = getClass().getSimpleName();
    private Context mContext;
    private MyApplication application;
    private CameraManager cameraManager;
    private CameraCharacteristics cameraCharacteristics;
    private String cameraId;
    private CameraDevice cameraDevice;
    private SurfaceHolder surfaceHolder;
    private Integer supportLevel;
    private Size[] previewOutputSizes;
    private Size[] takePictureOutputSizes;
    private Range<Integer>[] fpsRanges;
    private int[] focusModes;
    private CaptureRequest.Builder captureRequestBuilder; //捕获请求构建器
    private CaptureRequest captureRequest; //捕获请求，每一次相机操作代表一次请求，捕获请求包含了此次请求的参数
    private CameraCaptureSession cameraCaptureSession; //捕获会话，用于发送捕获请求，一个cameraDevice只能有一个会话
    private ImageReader takePictureImageReader; //用于读取缓存的图像数据 获取图像渲染的surface

    private static class Instance {
        private static final Camera2Manager camera2Manager = new Camera2Manager();
    }

    public static Camera2Manager getInstance() {
        return Instance.camera2Manager;
    }

    public void init(Context context) {
        this.mContext = context;
        this.application = (MyApplication) context.getApplicationContext();
    }

    //方向
    private static final SparseIntArray ORIENTATION = new SparseIntArray();
    static {
        ORIENTATION.append(Surface.ROTATION_0, 90);
        ORIENTATION.append(Surface.ROTATION_90, 0);
        ORIENTATION.append(Surface.ROTATION_180, 270);
        ORIENTATION.append(Surface.ROTATION_270, 180);
    }

    public boolean isSupportCamera2() {
        cameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        if (cameraManager == null)
            return false;
        String[] cameraIdList;
        try {
            cameraIdList = cameraManager.getCameraIdList();
        } catch (CameraAccessException e) {
            throw new RuntimeException(e);
        }
        if (cameraIdList.length < 1)
            return false;
        for (String id : cameraIdList) {
            try {
                cameraCharacteristics = cameraManager.getCameraCharacteristics(id);
            } catch (CameraAccessException e) {
                e.printStackTrace();
                Log.e(TAG, "ERROR:" + e.getMessage());
                cameraCharacteristics = null;
                continue;
            }
            if (cameraCharacteristics == null) {
                continue;
            }
            StreamConfigurationMap streamConfigurationMap = null;
            if (application.getCameraIndex() == 0
                    && cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            } else if (application.getCameraIndex() == 1
                    && cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            } else {
                continue;
            }

            if (streamConfigurationMap == null) {
                Log.d(TAG, "相机无数据");
                continue;
            }
            cameraId = id;
            supportLevel = cameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
            //预览分辨率集合
            previewOutputSizes = streamConfigurationMap.getOutputSizes(SurfaceTexture.class);
            Log.d(TAG, "previewOutputSizes:" + new Gson().toJson(previewOutputSizes));
            Log.d(TAG, "WIDTH:" + previewOutputSizes[0].getWidth());
            //拍照分辨率集合
            takePictureOutputSizes = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG);
            Log.d(TAG, "takePictureOutputSizes:" + new Gson().toJson(takePictureOutputSizes));
            //帧率
            fpsRanges = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
            Log.d(TAG, "fpsRanges:" + new Gson().toJson(fpsRanges));
            //对焦模式合集
            focusModes = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
            Log.d(TAG, "focusMode:" + new Gson().toJson(focusModes));
            return supportLevel != CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
        }
        return false;
    }

    //打开摄像头
    public void openCamera() {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "无相机权限！");
            return;
        }
        cameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        if (cameraManager == null)
            return;
        if (cameraCharacteristics == null)
            return;
        startBackgroundThread();
        try {
            cameraManager.openCamera(cameraId, stateCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.d(TAG, "ERROR:" + e.getMessage());
        }
    }

    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    //开启背景线程
    private void startBackgroundThread() {
        if (backgroundThread == null || backgroundHandler == null) {
            backgroundThread = new HandlerThread("camera2BackgroundThread");
            backgroundThread.start();
            backgroundHandler = new Handler(backgroundThread.getLooper());
        }
    }

    //关闭背景线程
    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quit();
            backgroundThread = null;
            backgroundHandler = null;
        }
    }

    //打开相机回调
    CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.d(TAG, "相机打开");
            cameraDevice = camera;
            //创建缓存曲面
            createImageReader();
            //开始预览
            startPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            Log.d(TAG, "相机断开连接");
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            Log.e(TAG, "camera error: " + "ERROR[" + i + "]");
        }
    };

    public void setSurfaceHolder(SurfaceHolder holder) {
        this.surfaceHolder = holder;
        surfaceHolder.setFixedSize(previewOutputSizes[0].getWidth(), previewOutputSizes[0].getHeight());
    }

    //开始预览
    private void startPreview() {
        try {
            if (cameraCaptureSession != null) {
                cameraCaptureSession.stopRepeating();
                cameraCaptureSession.close();
                cameraCaptureSession = null;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.e(TAG, "error:" + e.getMessage());
        }
        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            List<Surface> surfaceList = new ArrayList<>();
            if (surfaceHolder != null) {
                captureRequestBuilder.addTarget(surfaceHolder.getSurface());
                surfaceList.add(surfaceHolder.getSurface());
            }
            surfaceList.add(takePictureImageReader.getSurface());
            cameraDevice.createCaptureSession(surfaceList, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession captureSession) {
                    cameraCaptureSession = captureSession;
                    //这里设置捕获请求参数
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRanges[fpsRanges.length - 1]);
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);

                    captureRequest = captureRequestBuilder.build();
                    if (cameraCaptureSession == null || captureRequest == null)
                        return;

                    //发送预览请求
                    try {
                        cameraCaptureSession.setRepeatingRequest(captureRequest, captureCallback, backgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                        Log.e(TAG, "error:" + e.getMessage());
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Log.d(TAG, "onConfigureFailed");
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
           e.printStackTrace();
           Log.d(TAG, "error:" + e.getMessage());
        }
    }

    //发送捕获请求回调
    CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
        }
    };

    //创建图片渲染Surface
    private void createImageReader() {
        takePictureImageReader = ImageReader.newInstance(takePictureOutputSizes[0].getWidth(), takePictureOutputSizes[0].getHeight(), ImageFormat.JPEG, 1);
        takePictureImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                //从ImageReader队列中获取最新的一张图片
                Image image = reader.acquireLatestImage();
                //获取图片的图层，JPEG格式只有一层所以取下标为零的的图层
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);
                String dateStr = Utils.formatDate(new Date());
                Utils.savePicture(dateStr, data);
                image.close();
            }
        }, null);
    }

    //拍照
    public void takePicture() {
        if (cameraCaptureSession == null || cameraDevice == null)
            return;
        CaptureRequest.Builder capture = null;
        try {
            capture = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            //获取屏幕方向
            int rotation = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
            //设置拍照方向
            capture.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATION.get(rotation));
            capture.addTarget(takePictureImageReader.getSurface());
            CaptureRequest build = capture.build();
            cameraCaptureSession.stopRepeating();
            cameraCaptureSession.abortCaptures();
            cameraCaptureSession.capture(build, new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    startPreview();
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            throw new RuntimeException(e);
        }
    }

    //释放相机资源
    public void releaseCamera() {
        if (cameraCaptureSession != null) {
            try {
                cameraCaptureSession.stopRepeating();
                cameraCaptureSession.close();
                cameraCaptureSession = null;
            } catch (CameraAccessException e) {
                throw new RuntimeException(e);
            }

            if (cameraDevice != null) {
                cameraDevice.close();
                cameraDevice = null;
            }

            surfaceHolder = null;
            stopBackgroundThread();
        }
    }
}
