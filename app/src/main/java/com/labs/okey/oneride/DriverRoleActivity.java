package com.labs.okey.oneride;

import android.*;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.location.Location;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
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
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.labs.okey.oneride.model.GFCircle;
import com.labs.okey.oneride.model.Ride;
import com.labs.okey.oneride.utils.Globals;
import com.labs.okey.oneride.utils.UiThreadExecutor;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Oleg on 04-Apr-16.
 */
public class DriverRoleActivity extends BaseActivityWithGeofences
        implements
                android.location.LocationListener,
                // Added for Google Map support within sliding panel
                OnMapReadyCallback,
                GoogleApiClient.ConnectionCallbacks{

    private final String                        LOG_TAG = getClass().getSimpleName();

    private ImageView                           mImageTransmit;
    private GoogleMap                           mGoogleMap;
    private Circle                              meCircle;

    private MobileServiceTable<Ride>            mRidesTable;
    String                                      mCarNumber;
    private String                              mRideCode;

    private TextSwitcher                        mTextSwitcher;

    private Location                            mCurrentLocation;
    private long                                mLastLocationUpdateTime;

    Ride                                        mCurrentRide;

    private AtomicBoolean mRideCodeUploaded = new AtomicBoolean(false);

    @Override
    @CallSuper
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_role);

        // Keep device awake when advertising for Wi-Fi Direct
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        forceLTR();

        setupUI(getString(R.string.title_activity_driver_role), "");
        mRideCodeUploaded.set(false);

        wamsInit();
        geoFencesInit();
    }

    private void restoreState(Bundle savedInstanceState) {

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    @UiThread
    protected void setupUI(String title, String subTitle) {
        super.setupUI(title, subTitle);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.gf_map);
        mapFragment.getMapAsync(this);

        int min = 100000;
        int max = 1000000;

        Random r = new Random();
        int rideCode = r.nextInt(max - min + 1) + min;
        mRideCode = Integer.toString(rideCode);

        TextView txtRideCodeCaption = (TextView)findViewById(R.id.code_label_caption);
        if( txtRideCodeCaption != null )
            txtRideCodeCaption.setText(R.string.ride_code_label);

        TextView txtRideCode = (TextView) findViewById(R.id.txtRideCode);
        if( txtRideCode != null ) {
            txtRideCode.setVisibility(View.VISIBLE);
            txtRideCode.setText(mRideCode);
        }

        List<String> _cars = new ArrayList<>();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Set<String> carsSet = sharedPrefs.getStringSet(Globals.CARS_PREF, new HashSet<String>());
        if (carsSet.size() > 0) {
            Iterator<String> iterator = carsSet.iterator();
            while (iterator.hasNext()) {
                String carNumber = iterator.next();
                _cars.add(carNumber);
            }
        }
        String[] cars = new String[_cars.size()];
        cars = _cars.toArray(cars);
        if (cars.length == 0) {
            new MaterialDialog.Builder(this)
                    .title(R.string.edit_car_dialog_caption2)
                    .content(R.string.edit_car_dialog_text)
                    .iconRes(R.drawable.ic_exclamation)
                    .autoDismiss(true)
                    .cancelable(false)
                    .positiveText(getString(R.string.edit_car_button_title2))
                    .onPositive(new MaterialDialog.SingleButtonCallback(){
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog,
                                            @NonNull DialogAction which) {
                            Intent intent = new Intent(getApplicationContext(),
                                                        SettingsActivity.class);
                            startActivity(intent);
                        }
                    })
                    .show();
        } else if (cars.length > 1) {

            new MaterialDialog.Builder(this)
                    .title(R.string.edit_car_dialog_caption1)
                    .iconRes(R.drawable.ic_info)
                    .autoDismiss(true)
                    .cancelable(false)
                    .items(cars)
                    .positiveText(getString(R.string.edit_car_button_title))
                    .itemsCallback(new MaterialDialog.ListCallback() {
                        @Override
                        public void onSelection(MaterialDialog dialog,
                                                View view,
                                                int which,
                                                CharSequence text) {
                            mCarNumber = text.toString();
                        }
                    })
                    .onPositive(new MaterialDialog.SingleButtonCallback(){
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog,
                                            @NonNull DialogAction which) {
                            Intent intent = new Intent(getApplicationContext(),
                                                        SettingsActivity.class);
                            startActivity(intent);
                        }
                    })
                    .show();
        } else {
            mCarNumber = cars[0];
        }


        mImageTransmit = (ImageView) findViewById(R.id.img_transmit);
        if( mImageTransmit != null ) {
            mImageTransmit.setVisibility(View.VISIBLE);
            AnimationDrawable animationDrawable = (AnimationDrawable) mImageTransmit.getDrawable();
            animationDrawable.start();
        }

        mTextSwitcher = (TextSwitcher) findViewById(R.id.monitor_text_switcher);
        Animation in = AnimationUtils.loadAnimation(this, R.anim.push_up_in);
        Animation out = AnimationUtils.loadAnimation(this, R.anim.push_up_out);
        mTextSwitcher.setInAnimation(in);
        mTextSwitcher.setOutAnimation(out);
        // Set the initial text without an animation
        String currentMonitorStatus = getString(R.string.geofence_outside_title);
        mTextSwitcher.setCurrentText(currentMonitorStatus);
    }

    private void wamsInit() {

        super.wamsInit(true);

        mRidesTable = Globals.getMobileServiceClient().getTable("rides", Ride.class);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mGoogleMap = googleMap;
        mGoogleMap.getUiSettings().setZoomControlsEnabled(true);
        mGoogleMap.getUiSettings().setMyLocationButtonEnabled(true);

    }

    // Show geo-fences on Google Map
    @UiThread
    private void showGeofencesOnMap() {

        if( mGoogleMap == null)
            return;

        for (GFCircle gfCircle : mGFCircles) {
            CircleOptions circleOpt = new CircleOptions()
                    .center(new LatLng(gfCircle.getX(), gfCircle.getY()))
                    .radius(gfCircle.getRadius())
                    .strokeColor(Color.CYAN)
                    .fillColor(Color.TRANSPARENT);
            mGoogleMap.addCircle(circleOpt);
        }

    }

    // Show current location on Google Map
    @UiThread
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
    protected void onStart() {
        super.onStart();
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
            if( ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)) {

                mTextSwitcher.setCurrentText(getString(R.string.permission_location_denied));
                Log.d(LOG_TAG, getString(R.string.permission_location_denied));

            } else {

                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        Globals.LOCATION_PERMISSION_REQUEST);

                // to be continued on onRequestPermissionsResult() in permissionsHandler's activity
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults){

        try {

            switch( requestCode ) {

                case Globals.LOCATION_PERMISSION_REQUEST: {

                    if(  grantResults.length > 0
                            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                        mCurrentLocation = getCurrentLocation(this);
                        startLocationUpdates(this, this);
                    }
                }
                break;

                case Globals.CAMERA_PERMISSION_REQUEST : {
                    if (grantResults.length > 0
                            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        onSubmitRideInternal();
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

        super.onStop();
    }

    @Override
    @CallSuper
    protected void onDestroy() {
        super.onDestroy();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void forceLTR() {
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 ) {
            View v = findViewById(R.id.driver_status_layout);
            if( v != null )
                v.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

            v = findViewById(R.id.cabin_background_layout);
            if( v!= null )
                v.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            //getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        }
    }

    private void geoFencesInit() {
        ListenableFuture<CopyOnWriteArrayList<GFCircle>> transformFuture = _initGeofences();
        Futures.addCallback(transformFuture, new FutureCallback<CopyOnWriteArrayList<GFCircle>>() {
            @Override
            public void onSuccess(final CopyOnWriteArrayList<GFCircle> result) {

                String msg = getGFenceForLocation(mCurrentLocation);
                mTextSwitcher.setText(msg);

                if (Globals.isInGeofenceArea()) { // set or not set inside getGFenceForLocation()
                    if (mRideCode != null && mRideCodeUploaded.compareAndSet(false, true)) {
                        final Ride ride = createRideForUpload();
                        uploadRideControl(ride);
                    }
                }

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

        if( !Globals.DEBUG_WITHOUT_GEOFENCES ) {

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
                if( Globals.getRemindGeofenceEntrance() ) {

                    Globals.clearRemindGeofenceEntrance();

                    sendNotification(msg, DriverRoleActivity.class);
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

            // Upload generated ride-code (within whole ride) once if geo-fences were initialized
            if ( isGeoFencesInitialized()
                    && mRideCode != null
                    && mRideCodeUploaded.compareAndSet(false, true) ) {

                final Ride ride = createRideForUpload();
                uploadRideControl(ride);
            }

        } else {
            if ( mRideCode != null && mRideCodeUploaded.compareAndSet(false, true) ) {

                final Ride ride = createRideForUpload();
                uploadRideControl(ride);
            }
        }

    }

    private void uploadRideControl(final Ride ride) {

        ListenableFuture<Ride> rideFuture = asyncUploadRide(ride);
        Futures.addCallback(rideFuture, new FutureCallback<Ride>() {
            @Override
            public void onSuccess(Ride result) {
                mCurrentRide = ride;
                Assert.assertNotNull(mCurrentRide);

                View v = findViewById(R.id.passenger_snackbar);
                if( v != null )
                    Snackbar.make(v, R.string.ride_uploaded, Snackbar.LENGTH_SHORT)
                            .show();

                CustomEvent ce = new CustomEvent(getString(R.string.ride_started_answers_name))
                        .putCustomAttribute("User", getUser().getFullName())
                        .putCustomAttribute("RideCode", mRideCode);
                Answers.getInstance().logCustom(ce);
            }

            @Override
            public void onFailure(Throwable t) {
                Toast.makeText(DriverRoleActivity.this,
                        t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private ListenableFuture<Ride> asyncUploadRide(final Ride ride) {
        ExecutorService service = Executors.newFixedThreadPool(1);
        ListeningExecutorService executor = MoreExecutors.listeningDecorator(service);

        Callable<Ride> uploadRideTask =
                new Callable<Ride>() {
                    @Override
                    public Ride call() throws Exception {

                        try {

                            MobileServiceTable<Ride> ridesTable = Globals.getMobileServiceClient()
                                    .getTable("rides", Ride.class);
                            return ridesTable.insert(ride).get();

                        } catch(ExecutionException | InterruptedException ex) {
                            return null;
                        }
                    }
                };
        return executor.submit(uploadRideTask);
    }

    private Ride createRideForUpload() {

        Ride ride = new Ride();
        ride.setRideCode(mRideCode);
        ride.setCarNumber(mCarNumber);
        ride.setGFenceName(getCurrentGFenceName());
        ride.setDriverName(getUser().getFullName());
        ride.setCreated(new Date());
        ride.setPictureRequiredByDriver(false);

        return ride;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        String msg = String.format("%s location provider %s",
                provider,
                getLocationProviderStatus(status));
        Log.i(LOG_TAG, msg);
    }

    private String getLocationProviderStatus(int status) {

        String strStatus = "undefined";

        switch(status) {
            case LocationProvider.AVAILABLE:
                strStatus = "Available";
                break;

            case LocationProvider.OUT_OF_SERVICE:
                strStatus = "Out of service";
                break;

            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                strStatus = "Temporarily unavailable";
                break;

        }

        return strStatus;
    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    private void onSubmitRideInternal() {

    }

}
