package com.formichelli.dfflashlight;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Locale;

@SuppressWarnings("deprecation")
public class FlashLight extends Activity {
    boolean flash, kitkat, lollipop, locked, motorola;
    Camera mCamera;
    DroidLED led;
    Parameters pOn, pOff;
    View background;
    ImageView lock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        kitkat = android.os.Build.VERSION.SDK_INT >= 19 && android.os.Build.VERSION.SDK_INT <= 20;
        lollipop = android.os.Build.VERSION.SDK_INT >= 21;
        locked = false;

        initCamera();

        setScreen(kitkat);
        setContentView(R.layout.activity_main);

        lock = (ImageView) findViewById(R.id.lock);
        background = findViewById(R.id.background);

        if (motorola) {
            flashOn();
            Toast.makeText(this, getString(R.string.hello), Toast.LENGTH_SHORT)
                    .show();
        } else if (mCamera != null) {
            flashOn();
            mCamera.startPreview();
            Toast.makeText(this, getString(R.string.hello), Toast.LENGTH_SHORT)
                    .show();
        } else {
            background.setOnClickListener(null);
            lock.setVisibility(View.GONE);
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && kitkat) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return locked || super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() { // release the camera
        super.onDestroy();

        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            finish();
        }
    }

    public void switchFlash(View v) {
        if (locked)
            return;

        if (flash)
            flashOff();
        else
            flashOn();
    }

    private void initCamera() {
        motorola = android.os.Build.MANUFACTURER.toLowerCase(Locale.ENGLISH).contains("motorola");

        if (motorola)
            led = new DroidLED(this);
        else {
            try {
                if (!getPackageManager().hasSystemFeature(
                        PackageManager.FEATURE_CAMERA_FLASH))
                    throw new Exception();

                for (int i = 0, l = Camera.getNumberOfCameras(); i < l; i++) {
                    mCamera = Camera.open(i);
                    if (mCamera.getParameters().getSupportedFlashModes().contains(Parameters.FLASH_MODE_TORCH))
                        break;
                    else {
                        mCamera.release();
                        mCamera = null;
                    }
                }

                if (mCamera == null)
                    Toast.makeText(this, getString(R.string.no_camera),
                            Toast.LENGTH_SHORT).show();

                pOff = mCamera.getParameters();
                pOff.setFlashMode(Parameters.FLASH_MODE_OFF);

                pOn = mCamera.getParameters();
                pOn.setFlashMode(Parameters.FLASH_MODE_TORCH);

            } catch (Exception e) {
                mCamera = null;
                Toast.makeText(this, getString(R.string.no_flash),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setScreen(boolean kitkat) {
        Window w = getWindow();
        WindowManager.LayoutParams layout = w.getAttributes();

        if (!kitkat && !lollipop) {
            requestWindowFeature(Window.FEATURE_NO_TITLE); // hide title
            w.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN); // hide statusbar
            // w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION); // hide softkeys
        }

        w.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        layout.screenBrightness = 1; // WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL;
        w.setAttributes(layout);

    }

    private void flashOn() {
        if (motorola)
            led.enable();
        else
            try {
                mCamera.setParameters(pOn);
                if (Build.VERSION.SDK_INT >= 11)
                    mCamera.setPreviewTexture(new SurfaceTexture(0));
            } catch (IOException e) {
                e.printStackTrace();
            }

        flash = true;
    }

    private void flashOff() {
        if (motorola)
            led.disable();
        else
            mCamera.setParameters(pOff);

        flash = false;
    }

    public void switchLocked(View v) {
        locked = !locked;

        lock.setImageResource(locked ? R.drawable.locked
                : R.drawable.unlocked);
    }
}
