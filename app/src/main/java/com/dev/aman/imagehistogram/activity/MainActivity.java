package com.dev.aman.imagehistogram.activity;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.dev.aman.imagehistogram.R;
import com.dev.aman.imagehistogram.helper.BitmapHelper;
import com.dev.aman.imagehistogram.helper.HistogramHelper;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private ImageView mSelectedImage, mImageHistogram;
    private Button mOpenCameraBtn;

    static {
        if (OpenCVLoader.initDebug()) {
            Log.i(TAG, "OpenCV initialize success");
        } else {
            Log.i(TAG, "OpenCV initialize failed");
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
        onClick();
    }

    private void init() {
        mOpenCameraBtn = findViewById(R.id.openCamera);
        mSelectedImage = findViewById(R.id.selectedImage);
        mImageHistogram = findViewById(R.id.imageHistogram);
    }

    private void onClick() {
        mOpenCameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkPermission()){
                    openCamera();
                }
                else {
                    requestPermission();
                }
            }
        });
    }

    private void openCamera() {
        startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE), REQUEST_IMAGE_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (data != null) {
                Uri selectedImage = data.getData();
                try {
                    drawHistogram(BitmapHelper.readBitmapFromPath(this, selectedImage));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
            }

        }
    }

    private void drawHistogram(Bitmap bitmap) {
        try {
            Mat rgba = new Mat();
            Utils.bitmapToMat(bitmap, rgba);

            Size rgbaSize = rgba.size();
            int histSize = 256;
            MatOfInt histogramSize = new MatOfInt(histSize);

            int histogramHeight = (int) rgbaSize.height;
            int binWidth = 5;

            MatOfFloat histogramRange = new MatOfFloat(0f, 256f);

            Scalar[] colorsRgb = new Scalar[]{new Scalar(200, 0, 0, 255), new Scalar(0, 200, 0, 255), new Scalar(0, 0, 200, 255)};
            MatOfInt[] channels = new MatOfInt[]{new MatOfInt(0), new MatOfInt(1), new MatOfInt(2)};

            Mat[] histograms = new Mat[]{new Mat(), new Mat(), new Mat()};
            Mat histMatBitmap = new Mat(rgbaSize, rgba.type());

            for (int i = 0; i < channels.length; i++) {
                Imgproc.calcHist(Collections.singletonList(rgba), channels[i], new Mat(), histograms[i], histogramSize, histogramRange);
                Core.normalize(histograms[i], histograms[i], histogramHeight, 0, Core.NORM_INF);
                for (int j = 0; j < histSize; j++) {
                    Point p1 = new Point(binWidth * (j - 1), histogramHeight - Math.round(histograms[i].get(j - 1, 0)[0]));
                    Point p2 = new Point(binWidth * j, histogramHeight - Math.round(histograms[i].get(j, 0)[0]));
                    Imgproc.line(histMatBitmap, p1, p2, colorsRgb[i], 2, 8, 0);
                }
            }

            for (int i = 0; i < histograms.length; i++) {
                calculationsOnHistogram(histograms[i]);
            }

            // Don't do that at home or work it's for visualization purpose.
            BitmapHelper.showBitmap(this, bitmap, mSelectedImage);

            Bitmap histBitmap = Bitmap.createBitmap(histMatBitmap.cols(), histMatBitmap.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(histMatBitmap, histBitmap);
            BitmapHelper.showBitmap(this, histBitmap, mImageHistogram);

            mSelectedImage.setVisibility(View.VISIBLE);
            mImageHistogram.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void calculationsOnHistogram(Mat histogram) {
        SparseArray<ArrayList<Float>> compartments = HistogramHelper.createCompartments(histogram);
        float sumAll = HistogramHelper.sumCompartmentsValues(compartments);
        float averageAll = HistogramHelper.averageValueOfCompartments(compartments);
        Log.i(TAG, "Sum: " + Core.sumElems(histogram));
        Log.i(TAG, "Sum of all compartments " + String.valueOf(sumAll));
        Log.i(TAG, "Average value of all compartments " + String.valueOf(averageAll));
        Log.i(TAG, " ");

        for (int i = 0; i < compartments.size(); i++) {
            float sumLast = HistogramHelper.sumCompartmentValues(i, compartments);
            float averageLast = HistogramHelper.averageValueOfCompartment(i, compartments);
            float averagePercentageLastCompartment = HistogramHelper.averagePercentageOfCompartment(i, compartments);
            float percentageLastCompartment = HistogramHelper.percentageOfCompartment(i, compartments);
            Log.i(TAG, "Sum of " + (i + 1) + " compartment " + String.valueOf(sumLast));
            Log.i(TAG, "Average value of the " + (i + 1) + " compartment " + String.valueOf(averageLast));
            Log.i(TAG, "Average percentage of the " + (i + 1) + " compartment " + String.valueOf(averagePercentageLastCompartment));
            Log.i(TAG, "Percentage of the " + (i + 1) + " compartment " + String.valueOf(percentageLastCompartment));
            Log.i(TAG, " ");
        }
        Log.i(TAG, " ");
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

