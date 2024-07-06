package com.example.camerademo.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.camerademo.MyApplication;
import com.example.camerademo.R;
import com.example.camerademo.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, RadioGroup.OnCheckedChangeListener {

    private final String TAG = getClass().getSimpleName();
    private Button camera1Btn;
    private RadioGroup cameraApiGroup, cameraGroup;
    private final String[] permissions = {
        Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private List<String> requestPermissions = new ArrayList<>();
    private MyApplication application;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        application = (MyApplication) getApplicationContext();
        initView();
        initData();
        initAction();
    }

    private void initView() {
        camera1Btn = findViewById(R.id.jump_camera1_btn);
        cameraApiGroup = findViewById(R.id.main_activity_camera_api_group);
        cameraGroup = findViewById(R.id.main_activity_camera_group);
    }

    private void initData() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkAndRequestPermissions();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 1024);
            }
        }
        cameraApiGroup.check(application.getCameraMode() == 0
                ? R.id.main_activity_camera1_radio : R.id.main_activity_camera2_radio);
        cameraGroup.check(application.getCameraIndex() == 0
                ? R.id.main_activity_rear_camera_radio : R.id.main_activity_front_camera_radio);
    }

    private void initAction() {
        camera1Btn.setOnClickListener(this);
        cameraApiGroup.setOnCheckedChangeListener(this);
        cameraGroup.setOnCheckedChangeListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == camera1Btn.getId()) {
            Intent intent = new Intent(this, CameraPreviewActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
    }

    private void checkAndRequestPermissions() {
        for (int i = 0; i < permissions.length; i++) {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions.add(permissions[i]);
            }
        }
        if (!requestPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(MainActivity.this, requestPermissions.toArray(new String[requestPermissions.size()]), 100);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "有未授权权限！");
                    return;
                }
            }
            Utils.createFile(Utils.PICTURE_PATH);
            Log.d(TAG, "权限已授权");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "requestCode:" + requestCode + " resultCode:" + resultCode);
        if (requestCode == 1024) {
            if (Environment.isExternalStorageManager()) {
                Log.d(TAG, "文件读写权限授权成功！");
                Utils.createFile(Utils.PICTURE_PATH);
            } else {
                Log.d(TAG, "文件读写权限授权失败");
            }
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int i) {
        if (i == R.id.main_activity_camera1_radio) {
            application.setCameraMode(0);
        } else if (i == R.id.main_activity_camera2_radio) {
            application.setCameraMode(1);
        } else if (i == R.id.main_activity_rear_camera_radio) {
            application.setCameraIndex(0);
        } else if (i == R.id.main_activity_front_camera_radio) {
            application.setCameraIndex(1);
        }
    }
}