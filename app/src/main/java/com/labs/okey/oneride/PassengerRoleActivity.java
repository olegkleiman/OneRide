package com.labs.okey.oneride;

import android.*;
import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.labs.okey.oneride.adapters.WiFiPeersAdapter2;
import com.labs.okey.oneride.model.GFCircle;
import com.labs.okey.oneride.model.WifiP2pDeviceUser;
import com.labs.okey.oneride.utils.Globals;
import com.labs.okey.oneride.utils.RoundedDrawable;
import com.labs.okey.oneride.utils.UiThreadExecutor;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Oleg on 04-Apr-16.
 */
public class PassengerRoleActivity extends BaseActivityWithGeofences
        implements android.location.LocationListener,
                    OnMapReadyCallback,
                    GoogleApiClient.ConnectionCallbacks{

    private final String                LOG_TAG = getClass().getSimpleName();
    final int                           MAKE_PICTURE_REQUEST = 1;

    private Boolean                     mDriversShown;
    private TextSwitcher                mTextSwitcher;
    private GoogleMap                   mGoogleMap;
    private Circle                      meCircle;

    private Location                    mCurrentLocation;

    private WiFiPeersAdapter2           mDriversAdapter;
    public ArrayList<WifiP2pDeviceUser> mDrivers = new ArrayList<>();

    private String                      mRideCode;
    private long                        mLastLocationUpdateTime = System.currentTimeMillis();

    @Override
    @CallSuper
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passenger);

        setupUI(getString(R.string.title_activity_passenger_role), "");

        wamsInit(false); // without auto-update for this activity

        geoFencesInit();
    }

    @Override
    @UiThread
    protected void setupUI(String title, String subTitle) {
        super.setupUI(title, subTitle);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.gf_map);
        mapFragment.getMapAsync(this);

        RecyclerView driversRecycler = (RecyclerView) findViewById(R.id.recyclerViewDrivers);
        if( driversRecycler != null ) {
            driversRecycler.setHasFixedSize(true);
            driversRecycler.setLayoutManager(new LinearLayoutManager(this));
            driversRecycler.setItemAnimator(new DefaultItemAnimator());

            mDriversAdapter = new WiFiPeersAdapter2(this,
                    R.layout.drivers_header,
                    R.layout.row_devices,
                    mDrivers);
            driversRecycler.setAdapter(mDriversAdapter);
        }

        mDriversShown = false;

        mTextSwitcher = (TextSwitcher) findViewById(R.id.passenger_monitor_text_switcher);
        Animation in = AnimationUtils.loadAnimation(this, R.anim.push_up_in);
        Animation out = AnimationUtils.loadAnimation(this, R.anim.push_up_out);
        mTextSwitcher.setInAnimation(in);
        mTextSwitcher.setOutAnimation(out);
        // Set the initial text without an animation
        String currentMonitorStatus = getString(R.string.geofence_outside_title);
        mTextSwitcher.setCurrentText(currentMonitorStatus);

        Globals.setMonitorStatus(currentMonitorStatus);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        mGoogleMap.getUiSettings().setZoomControlsEnabled(true);
        mGoogleMap.getUiSettings().setMyLocationButtonEnabled(true);

        for (GFCircle gfCircle : mGFCircles) {

            CircleOptions circleOpt = new CircleOptions()
                    .center(new LatLng(gfCircle.getX(), gfCircle.getY()))
                    .radius(gfCircle.getRadius())
                    .strokeColor(Color.CYAN)
                    .fillColor(Color.TRANSPARENT);
            mGoogleMap.addCircle(circleOpt);
        }
    }

    // Show geo-fences on Google Map
    private void showGeofencesOnMap() {
        for (GFCircle gfCircle : mGFCircles) {
            CircleOptions circleOpt = new CircleOptions()
                    .center(new LatLng(gfCircle.getX(), gfCircle.getY()))
                    .radius(gfCircle.getRadius())
                    .strokeColor(Color.CYAN)
                    .fillColor(Color.TRANSPARENT);
            mGoogleMap.addCircle(circleOpt);
        }
    }

    // Showing the current location in Google Map
    private void showMeOnMap(LatLng latLng) {

        if( mGoogleMap == null)
            return;

        if( meCircle != null )
            meCircle.remove();
        CircleOptions circleOpt = new CircleOptions()
                .center(latLng)
                .radius(10)
                .strokeColor(Color.CYAN)
                .strokeWidth(1)
                .fillColor(Color.RED);
        meCircle = mGoogleMap.addCircle(circleOpt);
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(17));
    }

    @Override
    @CallSuper
    public void onResume() {
        super.onResume();

        try {
            mCurrentLocation = getCurrentLocation(this);// check Location Permission inside!

            // Global flag 'inGeofenceArea' is updated inside getGFenceForLocation()
            String msg = getGFenceForLocation(mCurrentLocation);
            mTextSwitcher.setCurrentText(msg);

            startLocationUpdates(this, this);

        } catch (SecurityException ex) {

            // Returns true if app has requested this permission previously
            // and the user denied the request
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                mTextSwitcher.setCurrentText(getString(R.string.permission_location_denied));
                Log.d(LOG_TAG, getString(R.string.permission_location_denied));

            } else {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        Globals.LOCATION_PERMISSION_REQUEST);

                // to be continued on onRequestPermissionsResult() in permissionsHandler's activity
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        try {

            switch (requestCode) {

                case Globals.LOCATION_PERMISSION_REQUEST: {

                    if (grantResults.length > 0
                            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                        mCurrentLocation = getCurrentLocation(this);
                        startLocationUpdates(this, this);
                    }
                }
                break;

                case Globals.CAMERA_PERMISSION_REQUEST: {
                    if (grantResults.length > 0
                            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        onCameraCVInternal(null);
                    } else {
                        mTextSwitcher.setCurrentText(getString(R.string.permission_camera_denied));
                        Log.d(LOG_TAG, getString(R.string.permission_camera_denied));
                    }
                }
                break;

            }

        } catch (Exception ex) {
            Log.e(LOG_TAG, ex.getMessage());

        }

    }

    @Override
    @CallSuper
    public void onPause() {
        try {
            stopLocationUpdates(this);
        } catch (SecurityException sex) {
            Log.e(LOG_TAG, "n/a");
        }

        super.onPause();
    }

    @Override
    @CallSuper
    protected void onStop() {

        // Ride Code will be re-newed on next activity's launch
        mRideCode = null;

        super.onStop();
    }

    private void geoFencesInit() {
        ListenableFuture<CopyOnWriteArrayList<GFCircle>> transformFuture = _initGeofences();
        Futures.addCallback(transformFuture, new FutureCallback<CopyOnWriteArrayList<GFCircle>>() {
            @Override
            public void onSuccess(final CopyOnWriteArrayList<GFCircle> result) {

                String msg = getGFenceForLocation(mCurrentLocation);
                mTextSwitcher.setText(msg);

                showGeofencesOnMap();

            }

            @Override
            public void onFailure(Throwable t) {
                if (Crashlytics.getInstance() != null) {
                    Crashlytics.logException(t);
                }
            }
        }, new UiThreadExecutor());
    }


    //
    // Implementation of LocationListener
    //
    @Override
    public void onLocationChanged(Location location) {

        if (!Globals.DEBUG_WITHOUT_GEOFENCES) {

            if (!isAccurate(location)) {
                Log.d(LOG_TAG, getString(R.string.location_inaccurate));
                return;
            }

            mCurrentLocation = location;

            LatLng latLng = new LatLng(location.getLatitude(),
                    location.getLongitude());

            showMeOnMap(latLng);

            // Global flag 'inGeofenceArea' is updated inside getGFenceForLocation()
            String msg = getGFenceForLocation(location);

            TextView textView = (TextView) mTextSwitcher.getCurrentView();
            String msgRepeat = textView.getText().toString();

            if (Globals.isInGeofenceArea()) {
                mLastLocationUpdateTime = System.currentTimeMillis();

                // Send notification and log the transition details.
                if (Globals.getRemindGeofenceEntrance()) {

                    Globals.clearRemindGeofenceEntrance();

                    sendNotification(msg, PassengerRoleActivity.class);
                }

            } else {
                long elapsed = System.currentTimeMillis() - mLastLocationUpdateTime;
                if (mLastLocationUpdateTime != 0 // for the first-time
                        && elapsed < Globals.GF_OUT_TOLERANCE) {

                    Globals.setInGeofenceArea(true);

                    msg = msgRepeat;
                }

            }

            mTextSwitcher.setCurrentText(msg);
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MAKE_PICTURE_REQUEST) {
            switch (resultCode) {

                case RESULT_OK: {
                    if (data != null) {
                        Bundle extras = data.getExtras();

                        FloatingActionButton passengerPicture = (FloatingActionButton) this.findViewById(R.id.join_ride_button);
                        if( passengerPicture == null )
                            break;

                        passengerPicture.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.green)));
                        Bitmap bmp = extras.getParcelable(getString(R.string.detection_face_bitmap));
                        if (bmp != null) {
                            Drawable drawable = new BitmapDrawable(this.getResources(), bmp);

                            drawable = RoundedDrawable.fromDrawable(drawable);
                            ((RoundedDrawable) drawable)
                                    .setCornerRadius(Globals.PICTURE_CORNER_RADIUS)
                                    .setBorderColor(Color.WHITE)
                                    .setBorderWidth(0)
                                    .setOval(true);

                            passengerPicture.setImageDrawable(drawable);
                        }
                    }
                }
                break;
            }
        }
    }

    public void onCameraCV(View view) {
        try {
            checkCameraAndStoragePermissions();
            onCameraCVInternal(view);
        }  catch (SecurityException ex) {

            // Returns true if app has requested this permission previously 
            // and the user denied the request 
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.CAMERA)) {

                Toast.makeText(this, getString(R.string.permission_camera_denied), Toast.LENGTH_LONG).show();
                Log.d(LOG_TAG, getString(R.string.permission_camera_denied));

            } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(this, getString(R.string.permission_storage_denied), Toast.LENGTH_LONG).show();
                Log.d(LOG_TAG, getString(R.string.permission_storage_denied));
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        Globals.CAMERA_PERMISSION_REQUEST);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void checkCameraAndStoragePermissions() throws SecurityException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ( (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED )
                    || (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) )
                throw new SecurityException();
        }
    }

    private void onCameraCVInternal(View v) {
        final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean bShowSelfieDescription = sharedPrefs.getBoolean(Globals.SHOW_SELFIE_DESC, true);

        Intent intent = new Intent(this, CameraCVActivity.class);
        startActivityForResult(intent, MAKE_PICTURE_REQUEST);
    }
}
