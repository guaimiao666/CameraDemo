package com.example.camerademo.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.camerademo.MyApplication;
import com.example.camerademo.R;
import com.example.camerademo.camera.Camera1Manager;
import com.example.camerademo.camera.Camera2Manager;

public class CameraPreviewActivity extends AppCompatActivity implements View.OnClickListener {

    private final String TAG = getClass().getSimpleName();
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private ImageButton cameraButton;
    private MyApplication application;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_camera_preview);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        application = (MyApplication) getApplicationContext();
        initView();
        initAction();
    }

    private void initView() {
        surfaceView = findViewById(R.id.camera_preview_surface);
        surfaceHolder = surfaceView.getHolder();
        cameraButton = findViewById(R.id.camera_preview_camera_image);
    }

    private void initAction() {
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
                Log.d(TAG, "surfaceCreated");

            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {
                Log.d(TAG, "surfaceChanged");
                if (application.getCameraMode() == 0) {
                    if (Camera1Manager.getInstance().openCamera()) {
                        Camera1Manager.getInstance().startPreview(surfaceHolder);
                    }
                } else {
                    if (Camera2Manager.getInstance().isSupportCamera2()) {
                        Camera2Manager.getInstance().setSurfaceHolder(surfaceHolder);
                        Camera2Manager.getInstance().openCamera();
                    }
                }
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
                Log.d(TAG, "surfaceDestroyed");
                if (application.getCameraMode() == 0) {
                    Camera1Manager.getInstance().releaseCamera();
                } else {
                    Camera2Manager.getInstance().releaseCamera();
                }
            }
        });
        cameraButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == cameraButton.getId()) {
            if (application.getCameraMode() == 0) {
                Camera1Manager.getInstance().takePicture();
            } else {
                Camera2Manager.getInstance().takePicture();
            }
        }
    }
}