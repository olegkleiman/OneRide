package com.labs.okey.oneride;

import android.*;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.WindowManager;

import com.labs.okey.oneride.utils.Globals;

public class CameraCVActivity extends Activity {

    private final String        LOG_TAG = getClass().getSimpleName();

    OrientationEventListener mOrientationEventListener;
    private int              mCurrentOrientation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_camera_cv);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void checkCameraPermissions() throws SecurityException {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED )
                throw new SecurityException();
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults){
        try {

            switch( requestCode ) {

                case Globals.CAMERA_PERMISSION_REQUEST: {

                    if(  grantResults.length > 0
                            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                        initCamera();

                    } else {
                        //finish();
                    }
                }
            }

        } catch (Exception ex) {
            Log.e(LOG_TAG, ex.getMessage());

        }
    }

    private void initCamera() {

    }

}
