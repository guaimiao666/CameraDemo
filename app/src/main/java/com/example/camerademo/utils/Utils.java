package com.example.camerademo.utils;

import android.annotation.SuppressLint;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {
    private static String ROOT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
    public static String PICTURE_PATH = ROOT_PATH + "/" + "cameraDemoDirectory";

    public static boolean createFile(String path) {
        if (TextUtils.isEmpty(path))
            return false;
        File file = new File(path);
        if (file.exists())
            return false;
        return file.mkdirs();
    }

    public static void savePicture(String date, byte[] data) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(PICTURE_PATH + "/" + date + ".jpg");
            fos.write(data);
            fos.flush();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    public static String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        return sdf.format(date);
    }
}
