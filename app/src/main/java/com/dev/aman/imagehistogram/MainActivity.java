package com.dev.aman.imagehistogram;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private ImageView mSelecterImage;
    private Button mOpenCameraBtn;
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private Bitmap mImageBitmap;
    private String mCurrentPhotoPath;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
        onClick();
    }

    private void init() {
        mOpenCameraBtn = findViewById(R.id.openCamera);
        mSelecterImage = findViewById(R.id.selectedImage);
    }

    private void onClick() {
        mOpenCameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkPermission()){
                    openCamera();
                    startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE), REQUEST_IMAGE_CAPTURE);
                }
                else {
                    requestPermission();
                }
            }
        });
    }

    private void openCamera() {

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if(data != null){
                mImageBitmap= data.getParcelableExtra("data");
                mSelecterImage.setImageBitmap(mImageBitmap);
            }
            else {
                Toast.makeText(this, "Canceled", Toast.LENGTH_SHORT).show();
            }

        }
    }


            private Boolean checkPermission() {
        boolean flag = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flag = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA ) + ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            flag = true;
        }
        return flag;
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (permissions.length > 0) {

            } else {
                requestPermission();
            }
        }
    }
}
