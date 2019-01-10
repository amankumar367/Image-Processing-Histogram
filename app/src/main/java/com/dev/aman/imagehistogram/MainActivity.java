package com.dev.aman.imagehistogram;

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
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private ImageView mSelecterImage;
    private LinearLayout mImageHistogram;
    private Button mOpenCameraBtn;
    private Bitmap mImageBitmap, bi;

    boolean flag, isColored;

    private int SIZE = 256;
    // Red, Green, Blue
    private int NUMBER_OF_COLOURS = 3;

    public final int RED = 0;
    public final int GREEN = 1;
    public final int BLUE = 2;

    private int[][] colourBins;
    private volatile boolean loaded = false;
    private int maxY;

    private static final int LDPI = 0;
    private static final int MDPI = 1;
    private static final int TVDPI = 2;
    private static final int HDPI = 3;
    private static final int XHDPI = 4;

    float offset = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        if(metrics.densityDpi==metrics.DENSITY_LOW)
            offset = 0.75f;
        else if(metrics.densityDpi==metrics.DENSITY_MEDIUM)
            offset = 1f;
        else if(metrics.densityDpi==metrics.DENSITY_TV)
            offset = 1.33f;
        else if(metrics.densityDpi==metrics.DENSITY_HIGH)
            offset = 1.5f;
        else if(metrics.densityDpi==metrics.DENSITY_XHIGH)
            offset = 2f;


        colourBins = new int[NUMBER_OF_COLOURS][];

        for (int i = 0; i < NUMBER_OF_COLOURS; i++) {
            colourBins[i] = new int[SIZE];
        }

        loaded = false;

        init();
        onClick();
    }

    private void init() {
        mOpenCameraBtn = findViewById(R.id.openCamera);
        mSelecterImage = findViewById(R.id.selectedImage);
        mImageHistogram = findViewById(R.id.imageHistogram);
    }

    private void onClick() {
        mOpenCameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkPermission()){
                    openCamera();
                    isColored = true;
                    mImageHistogram.addView(new MyHistogram(MainActivity.this, bi));
                }
                else {
                    requestPermission();
                }
            }
        });
    }

    private void openCamera() {
        mSelecterImage.setVisibility(View.GONE);
        startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE), REQUEST_IMAGE_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (data != null) {
                Uri selectedImage = data.getData();
                String filename = getRealPathFromURI(selectedImage);
                bi = BitmapFactory.decodeFile(filename);

                if (bi != null) {
                    try {
                        new MyAsync().execute();
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                mImageBitmap = data.getParcelableExtra("data");
                mSelecterImage.setImageBitmap(mImageBitmap);
                mSelecterImage.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
            }

        }
    }

    class MyAsync extends AsyncTask
    {
        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
            showDialog(0);
        }

        @Override
        protected Object doInBackground(Object... params) {
            // TODO Auto-generated method stub

            try {
                load(bi);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Object result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);

            mImageHistogram = findViewById(R.id.imageHistogram);
            mImageHistogram.setVisibility(View.VISIBLE);

            dismissDialog(0);
        }

    }

    public void load(Bitmap bi) throws IOException {

        if (bi != null) {
            // Reset all the bins
            for (int i = 0; i < NUMBER_OF_COLOURS; i++) {
                for (int j = 0; j < SIZE; j++) {
                    colourBins[i][j] = 0;
                }
            }

            for (int x = 0; x < bi.getWidth(); x++) {
                for (int y = 0; y < bi.getHeight(); y++) {

                    int pixel = bi.getPixel(x, y);

                    colourBins[RED][Color.red(pixel)]++;
                    colourBins[GREEN][Color.green(pixel)]++;
                    colourBins[BLUE][Color.blue(pixel)]++;
                }
            }

            maxY = 0;

            for (int i = 0; i < NUMBER_OF_COLOURS; i++) {
                for (int j = 0; j < SIZE; j++) {
                    if (maxY < colourBins[i][j]) {
                        maxY = colourBins[i][j];
                    }
                }
            }
            loaded = true;
        } else {
            loaded = false;
        }
    }

    public String getRealPathFromURI(Uri contentUri) {

        try {
            if (contentUri.toString().contains("video")) {
                String[] proj = { MediaStore.Video.Media.DATA };
                Cursor cursor = managedQuery(contentUri, proj, null, null, null);
                int column_index = cursor
                        .getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                cursor.moveToFirst();
                return cursor.getString(column_index);
            } else {
                String[] proj = { MediaStore.Images.Media.DATA };
                Cursor cursor = managedQuery(contentUri, proj, null, null, null);
                int column_index = cursor
                        .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                return cursor.getString(column_index);
            }
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
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

    class MyHistogram extends View {

        public MyHistogram(Context context, Bitmap bi) {
            super(context);

        }

        @Override
        protected void onDraw(Canvas canvas) {
            // TODO Auto-generated method stub
            super.onDraw(canvas);

            if (loaded) {
                canvas.drawColor(Color.GRAY);

                int xInterval = (int) ((double) getWidth() / ((double) SIZE + 1));

                for (int i = 0; i < NUMBER_OF_COLOURS; i++) {

                    Paint wallpaint;

                    wallpaint = new Paint();
                    if (isColored) {
                        if (i == RED) {
                            wallpaint.setColor(Color.RED);
                        } else if (i == GREEN) {
                            wallpaint.setColor(Color.GREEN);
                        } else if (i == BLUE) {
                            wallpaint.setColor(Color.BLUE);
                        }
                    } else {
                        wallpaint.setColor(Color.WHITE);
                    }

                    wallpaint.setStyle(Paint.Style.FILL);

                    Path wallpath = new Path();
                    wallpath.reset();
                    wallpath.moveTo(0, getHeight());
                    for (int j = 0; j < SIZE - 1; j++) {
                        int value = (int) (((double) colourBins[i][j] / (double) maxY) * (getHeight()+100));


                        //if(j==0) {
                        //   wallpath.moveTo(j * xInterval* offset, getHeight() - value);
                        //}
                        // else {
                        wallpath.lineTo(j * xInterval * offset, getHeight() - value);
                        // }
                    }
                    wallpath.lineTo(SIZE * offset, getHeight());
                    canvas.drawPath(wallpath, wallpaint);
                }

            }

        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        ProgressDialog dataLoadProgress = new ProgressDialog(this);
        dataLoadProgress.setMessage("Loading...");
        dataLoadProgress.setIndeterminate(true);
        dataLoadProgress.setCancelable(false);
        dataLoadProgress.setProgressStyle(android.R.attr.progressBarStyleLarge);
        return dataLoadProgress;

    }
}
