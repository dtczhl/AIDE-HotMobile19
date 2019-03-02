package com.huanlezhang.aide;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.HashMap;

public class MainActivity extends Activity {

    private static final String TAG = "DTC MainActivity";

    private static final String PERMISSION_STRINGS[] = {
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
    };
    private final int PERMISSION_ID = 1;

    private MyCamera mMyCamera;
    private MyBle mMyBle;

    private ToggleButton mCaptureBtn;
    private Button mBleBtn;
    private Button mMeasureBtn;
    private TextView mMeasureTimeTextView;
    private Button mConfirmBtn;
    private ToggleButton mOnOffToggleBtn;

    private AutoFitTextureView mCameraView;

    private ConstraintLayout mMainLayout;

    private boolean bCameraCaptured = false;
    private boolean bBleSelected = false;

    private HashMap<String, BleLocViewInfoStore> mLocViewMap = new HashMap<>();


    @Override
    protected void onResume() {
        super.onResume();

        mMyCamera.onResume();
    }

    @Override
    protected void onPause() {

        mMyCamera.onPause();

        super.onPause();
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSION_STRINGS, PERMISSION_ID);
        }

        mCaptureBtn = findViewById(R.id.captureBtn);
        mCaptureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mOnOffToggleBtn.setEnabled(false);
                mMyBle.disconnectAllBle();

                ToggleButton toggleButton = (ToggleButton) v;
                if (toggleButton.isChecked()) {
                    // capture, stop camera view
                    mMyCamera.stopCamera();
                    bCameraCaptured = true;
                    mLocViewMap.clear();

                    mConfirmBtn.setEnabled(false);

                } else {
                    // continue camera view

                    mOnOffToggleBtn.setChecked(false);

                    mMyCamera.resumeCamera();
                    bCameraCaptured = false;
                    bBleSelected = false;

                    for (BleLocViewInfoStore bleLocViewInfoStore: mLocViewMap.values()) {
                        Integer viewId = bleLocViewInfoStore.getViewId();
                        mMainLayout.removeView(findViewById(viewId));
                    }

                    mMeasureTimeTextView.setText("0");

                    mBleBtn.setEnabled(false);
                    mMeasureBtn.setEnabled(false);
                    mConfirmBtn.setEnabled(false);
                }
            }
        });

        mCameraView = findViewById(R.id.cameraView);
        mCameraView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (bCameraCaptured && !bBleSelected) {

                        mBleBtn.setEnabled(true);

                        Button button = new Button(getApplicationContext());

                        String buttonText = Integer.toString(mLocViewMap.size() + 1);
                        int buttonId = View.generateViewId();

                        mLocViewMap.put(buttonText, new BleLocViewInfoStore(buttonText, buttonId));

                        button.setText(buttonText);
                        button.setTypeface(null, Typeface.BOLD);
                        button.setId(buttonId);
                        button.setBackgroundColor(Color.WHITE);

                        ConstraintLayout.LayoutParams buttonLayoutParams = new ConstraintLayout.LayoutParams(
                                100, ConstraintLayout.LayoutParams.WRAP_CONTENT
                        );
                        buttonLayoutParams.leftToLeft = mCameraView.getId();
                        buttonLayoutParams.leftMargin = (int) (event.getX() - 50);
                        buttonLayoutParams.topToTop = mCameraView.getId();
                        buttonLayoutParams.topMargin = (int) (event.getY() - 50);

                        button.setLayoutParams(buttonLayoutParams);
                        mMainLayout.addView(button);
                    }
                }
                return false;
            }
        });

        mBleBtn = findViewById(R.id.bleBtn);
        mBleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCaptureBtn.setEnabled(false);
                bBleSelected = true;
                v.setEnabled(false);
                mMyBle.showBleLayout();
            }
        });

        mMeasureBtn = findViewById(R.id.measureBtn);
        mMeasureTimeTextView = findViewById(R.id.measureTextview);

        mConfirmBtn = findViewById(R.id.resultBtn);

        mOnOffToggleBtn = findViewById(R.id.onOffBtn);

        mMyCamera = new MyCamera(this);
        mMyBle = new MyBle(this, mLocViewMap);

        mMainLayout = findViewById(R.id.mainLayout);

    }

    @Override
    public void onBackPressed() {
        // disable return button
        // do nothing
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_ID) {
            for (int grantResult: grantResults) {
                if (grantResult == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, "Permission Denied!", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void onAboutClick(View v) {
        AboutMe.showDialog(this);
    }
}
