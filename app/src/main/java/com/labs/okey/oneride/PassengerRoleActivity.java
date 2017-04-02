package com.labs.okey.oneride;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.CallSuper;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.UiThread;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.labs.okey.oneride.adapters.BtPeersAdapter;
import com.labs.okey.oneride.model.BtDeviceUser;
import com.labs.okey.oneride.model.GFCircle;
import com.labs.okey.oneride.model.Join;
import com.labs.okey.oneride.model.User;
import com.labs.okey.oneride.model.WifiP2pDeviceUser;
import com.labs.okey.oneride.services.ChildEventListenerService;
import com.labs.okey.oneride.services.PassengerCIService;
import com.labs.okey.oneride.utils.Globals;
import com.labs.okey.oneride.utils.IRecyclerClickListener;
import com.labs.okey.oneride.utils.IRefreshable;
import com.labs.okey.oneride.utils.ITrace;
import com.labs.okey.oneride.utils.UiThreadExecutor;
import com.labs.okey.oneride.utils.wifip2p.P2pConversator;
import com.labs.okey.oneride.utils.wifip2p.P2pPreparer;
import com.microsoft.windowsazure.mobileservices.MobileServiceException;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;

import junit.framework.Assert;

import net.steamcrafted.loadtoast.LoadToast;

import java.net.URI;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;

import io.fabric.sdk.android.Fabric;

/**
 * @author Oleg Kleiman
 * created 04-Apr-16.
 */
public class PassengerRoleActivity extends BaseActivityWithGeofences
        implements ITrace,
                    IRecyclerClickListener,
                    Handler.Callback,
                    IRefreshable,
                    P2pConversator.IPeersChangedListener,
                    LocationListener,
                    OnMapReadyCallback,
                    GoogleApiClient.ConnectionCallbacks{

    private final String                LOG_TAG = getClass().getSimpleName();
    final int                           MAKE_PICTURE_REQUEST = 1;

    private Boolean                     mDriversShown;
    private TextSwitcher                mTextSwitcher;
    private GoogleMap                   mGoogleMap;
    private Circle                      meCircle;

    private MobileServiceTable<Join>    joinsTable;

    private P2pPreparer                 mP2pPreparer;
    private P2pConversator              mP2pConversator;

    private Location                    mCurrentLocation;

    private SearchDialogFragment        mSearchDriverDialogFragment;
    private CountDownTimer              mSearchDriverCountDownTimer;
    private Integer                     mCountDiscoveryFailures = 0;
    private Integer                     mCountDiscoveryTrials = 1;

    //private WiFiPeersAdapter2           mDriversAdapter;
    private BtPeersAdapter              _mDriversAdapter;
    //public ArrayList<WifiP2pDeviceUser> mDrivers = new ArrayList<>();
    public ArrayList<BtDeviceUser>      _mDrivers = new ArrayList<>();

    private Handler                     handler = new Handler(this);
    public Handler                      getHandler() {
        return handler;
    }

    private String                      mRideCode;
    private String                      mDriverName;
    private URI                         mPictureURI;
    private UUID                        mFaceId;
    private long                        mLastLocationUpdateTime = System.currentTimeMillis();

    private SharedPreferences           mSharedPrefs = null; // declared here to enable the access
                                                             // from BroadcastReceiver

    private BluetoothAdapter            mBluetoothAdapter;
    private final int                   BT_DISCOVERY_PERMISSSION_REQUEST = 1001;
    private final BroadcastReceiver     mBtReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            int currentRssiLevel = Globals.DEFAULT_RSSI_LEVEL;

            if( mSharedPrefs!= null )
                currentRssiLevel = mSharedPrefs.getInt(Globals.PREF_RSSI_LEVEL, Globals.DEFAULT_RSSI_LEVEL);

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                BtDeviceUser btDeviceUser = btAnalyzeDevice(device);
                if( btDeviceUser != null ) {

                    int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                    btDeviceUser.set_Rssi(rssi);

                    Globals.__log(LOG_TAG, String.format(Locale.getDefault(),
                                                "Got device with RSSI: %d. Current level: %d",
                                                rssi, currentRssiLevel));

                    if( rssi >= -(currentRssiLevel) ) { // these are negative values

                        Globals.__log(LOG_TAG, "Adding device");

                        _mDriversAdapter.add(btDeviceUser);
                        _mDriversAdapter.notifyDataSetChanged();

                    } else {
                        Globals.__log(LOG_TAG, "Skipping device due to poor RSSI");
                    }
                }
            }
        }
    };

    @Override
    @CallSuper
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passenger);

        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        setupUI(getString(R.string.title_activity_passenger_role), "");

        wamsInit(false); // without auto-update for this activity

        geoFencesInit();

        joinsTable = Globals.getMobileServiceClient().getTable("joins", Join.class);

        if( savedInstanceState != null ) {

            btInit();
            btRefresh();

            restoreInstanceState(savedInstanceState);

        } else {

            btInit();
            btRefresh();
            showSearchDialog();
        }
    }

    @Override
    @CallSuper
    public void onSaveInstanceState(Bundle outState) {

        outState.putParcelableArrayList(Globals.PARCELABLE_KEY_DRIVERS, _mDrivers);
        outState.putString(Globals.PARCELABLE_KEY_DRVER_NAME, mDriverName);
        outState.putString(Globals.PARCELABLE_KEY_RIDE_CODE, mRideCode);

        try {
            getSupportFragmentManager().putFragment(outState, "searchDialog", mSearchDriverDialogFragment);
        } catch(Exception ex) {
            // Dismiss the situation when SearchDialog is not currently in FragmentManager
        }

        Globals.__log(LOG_TAG, getString(R.string.log_state_saved));

        super.onSaveInstanceState(outState);
    }

    private void restoreInstanceState(Bundle savedInstanceState) {

        try {
            mSearchDriverDialogFragment = (SearchDialogFragment)
                    getSupportFragmentManager().getFragment(savedInstanceState, "searchDialog");
        } catch(Exception ex) {
            // Dismiss the situation when SearchDialog is not currently in FragmentManager
        }

        if( savedInstanceState.containsKey(Globals.PARCELABLE_KEY_RIDE_CODE) ) {
            mRideCode = savedInstanceState.getString(Globals.PARCELABLE_KEY_RIDE_CODE);
        }

        if( savedInstanceState.containsKey(Globals.PARCELABLE_KEY_DRIVERS) ) {
            ArrayList<BtDeviceUser> drivers = savedInstanceState.getParcelableArrayList(Globals.PARCELABLE_KEY_DRIVERS);
            if( drivers != null ) {

                _mDriversAdapter.addAll(drivers);
                _mDriversAdapter.notifyDataSetChanged();
                Globals.__log(LOG_TAG, "CountDown: drivers re-loaded. Count: " + _mDrivers.size());
            }
        }

        if( savedInstanceState.containsKey(Globals.PARCELABLE_KEY_DRVER_NAME) )
            mDriverName = savedInstanceState.getString(Globals.PARCELABLE_KEY_DRVER_NAME);

        Globals.__log(LOG_TAG, getString(R.string.log_state_restored));

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

            _mDriversAdapter = new BtPeersAdapter(this,
                                                R.layout.drivers_header,
                                                R.layout.row_devices,
                                                _mDrivers);
            driversRecycler.setAdapter(_mDriversAdapter);

            final GestureDetector mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {

                @Override public boolean onSingleTapUp(MotionEvent e) {
                    return true;
                }

            });

            driversRecycler.addOnItemTouchListener(new RecyclerView.OnItemTouchListener(){
               @Override
               public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
                   View child = recyclerView.findChildViewUnder(motionEvent.getX(),motionEvent.getY());
                   if( child == null )
                       return false;

                   if( mGestureDetector.onTouchEvent(motionEvent)){

                       child.setEnabled(false);
                       child.setClickable(true);

                       int position = recyclerView.getChildAdapterPosition(child);
                       clicked(null, position - 1); // Count header as item

                       return true;
                   }

                   return false;
               }

                @Override
                public void onTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {

                }

                @Override
                public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

                }
            });


//            mDriversAdapter = new WiFiPeersAdapter2(this,
//                    R.layout.drivers_header,
//                    R.layout.row_devices,
//                    mDrivers);
//            driversRecycler.setAdapter(mDriversAdapter);
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

        //mGoogleMap.setMyLocationEnabled(true);

        for (GFCircle gfCircle : mGFCircles) {

            CircleOptions circleOpt = new CircleOptions()
                    .center(new LatLng(gfCircle.getX(), gfCircle.getY()))
                    .radius(gfCircle.getRadius())
                    .strokeColor(Color.CYAN)
                    .fillColor(Color.TRANSPARENT);

            if( mGoogleMap != null )
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

            if( mGoogleMap != null )
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
    public void onDestroy() {
        super.onDestroy();

        btRestore();
    }

    @Override
    @CallSuper
    public void onResume() {
        super.onResume();

        try {
            // Global flag 'inGeofenceArea' is updated inside getGFenceForLocation()
            String msg = getGFenceForLocation(mCurrentLocation);
            mTextSwitcher.setCurrentText(msg);

        } catch (SecurityException ex) {

            // Returns true if app has requested this permission previously
            // and the user denied the request
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                mTextSwitcher.setCurrentText(getString(R.string.permission_location_denied));
                Globals.__log(LOG_TAG, getString(R.string.permission_location_denied));

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

                case BT_DISCOVERY_PERMISSSION_REQUEST: {
                    mBluetoothAdapter.startDiscovery();
                }
                break;

                case Globals.LOCATION_PERMISSION_REQUEST: {

                    if (grantResults.length > 0
                            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                        mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(
                                getGoogleApiClient());

                        LocationRequest locRequest = LocationRequest.create();
                        locRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                        locRequest.setFastestInterval(Globals.HIGH_PRIORITY_FAST_INTERVAL);
                        locRequest.setInterval(Globals.HIGH_PRIORITY_UPDATE_INTERVAL);

                        LocationServices.FusedLocationApi.requestLocationUpdates(
                                getGoogleApiClient(), locRequest, this);


                    }
                }
                break;

                case Globals.CAMERA_PERMISSION_REQUEST: {
                    if (grantResults.length > 0
                            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        onCameraCVInternal();
                    } else {
                        mTextSwitcher.setCurrentText(getString(R.string.permission_camera_denied));
                        Globals.__log(LOG_TAG, getString(R.string.permission_camera_denied));
                    }
                }
                break;

            }

        } catch (Exception e) {
            Globals.__logException(e);

        }

    }

    @Override
    @CallSuper
    public void onPause() {
        super.onPause();
    }

    @Override
    @CallSuper
    protected void onStart() {

        super.onStart();

        mSearchDriverCountDownTimer = new CountDownTimer(Globals.PASSENGER_DISCOVERY_PERIOD * 1000, 1000) {

            public void onTick(long millisUntilFinished) {
                int driversCount = _mDriversAdapter.getItemCount() - 1; // count header

                Globals.__log(LOG_TAG,
                        String.format(Locale.getDefault(),
                                "CountDown: Tick. Remains %d sec. Drivers size: %d",
                                millisUntilFinished, driversCount));

                if( driversCount > 0 ) {

                    this.cancel();
                    Globals.__log(LOG_TAG, "CountDown: Cancelling timer");

                    if( mSearchDriverDialogFragment != null ) {
                        Globals.__log(LOG_TAG, "CountDown: Dismissing dialog");
                        try {
                            mSearchDriverDialogFragment.dismiss();
                        } catch(IllegalStateException ex){
                            Globals.__logException(ex);
                        }
                    }
                }
            }

            public void onFinish() {
                Globals.__log(LOG_TAG, "CountDown: Finish");

                int driversCount = _mDriversAdapter.getItemCount() - 1; // count header

                if( driversCount == 0) {

                    if (mCountDiscoveryTrials++ >= Globals.MAX_DISCOVERY_TRIALS) {
                        Globals.__log(LOG_TAG, "CountDown: Exceeded");

                        if( mSearchDriverDialogFragment != null )
                            mSearchDriverDialogFragment.dismiss();

                        mCountDiscoveryTrials = 1;
                        this.cancel();

                        // No drivers found - Show ride code pane
                        showRideCodePane(R.string.ride_code_dialog_content, Color.BLACK);

                    } else {
                        Globals.__log(LOG_TAG, "CountDown: Restarting for " + mCountDiscoveryTrials);
                        btRefresh();
                        this.start();
                    }
                } else {
                    Globals.__log(LOG_TAG, String.format(Locale.getDefault(),
                                           "CountDown: There are %d drivers on Finish", driversCount));
                    if( mSearchDriverDialogFragment != null )
                        mSearchDriverDialogFragment.dismiss();
                }
            }
        };

        mSearchDriverCountDownTimer.start();
    }

    @Override
    @CallSuper
    protected void onStop() {

        // Ride Code will be re-newed on next activity's launch
        mRideCode = null;

        mSearchDriverCountDownTimer.cancel();
        mSearchDriverCountDownTimer = null;

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
            public void onFailure(@NonNull Throwable t) {
                Globals.__logException(t);
            }
        }, new UiThreadExecutor());
    }


    //
    // Implementation of LocationListener
    //
    @Override
    public void onLocationChanged(Location location) {

        if (!isAccurate(location)) {
            Globals.__log(LOG_TAG, getString(R.string.location_inaccurate));
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MAKE_PICTURE_REQUEST) {
            switch (resultCode) {

                case RESULT_OK: {
                    if (data != null) {
                        Bundle extras = data.getExtras();

//                        FloatingActionButton passengerPicture = (FloatingActionButton) this.findViewById(R.id.join_ride_button);
//                        if( passengerPicture == null )
//                            break;
//
//                        passengerPicture.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.green)));
//                        Bitmap bmp = extras.getParcelable(getString(R.string.detection_face_bitmap));
//                        if (bmp != null) {
//                            Drawable drawable = new BitmapDrawable(this.getResources(), bmp);
//
//                            drawable = RoundedDrawable.fromDrawable(drawable);
//                            ((RoundedDrawable) drawable)
//                                    .setCornerRadius(Globals.PICTURE_CORNER_RADIUS)
//                                    .setBorderColor(Color.WHITE)
//                                    .setBorderWidth(0)
//                                    .setOval(true);
//
//                            passengerPicture.setImageDrawable(drawable);
//                        }
//
//                        mPictureURI = (URI) extras.getSerializable(getString(R.string.detection_face_uri));
//                        mFaceId = (UUID) extras.getSerializable(getString(R.string.detection_face_id));
//
//                        mTextSwitcher.setText(getString(R.string.instruction_make_additional_selfies));
                    }
                }
                break;
            }
        }
    }

    public void onCameraCV(View view) {
        try {
            checkCameraAndStoragePermissions();
            onCameraCVInternal();
        }  catch (SecurityException ex) {

            // Returns true if app has requested this permission previously 
            // and the user denied the request 
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.CAMERA)) {

                Toast.makeText(this, getString(R.string.permission_camera_denied), Toast.LENGTH_LONG).show();
                Globals.__log(LOG_TAG, getString(R.string.permission_camera_denied));

            } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(this, getString(R.string.permission_storage_denied), Toast.LENGTH_LONG).show();
                Globals.__log(LOG_TAG, getString(R.string.permission_storage_denied));
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

    private void onCameraCVInternal() {
        final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean bShowSelfieDescription = sharedPrefs.getBoolean(Globals.SHOW_SELFIE_DESC, true);

    }

    @Override
    public boolean handleMessage(Message msg) {
        String strMessage;

        switch (msg.what) {
            case Globals.TRACE_MESSAGE:
                Bundle bundle = msg.getData();
                strMessage = bundle.getString("message");
                trace(strMessage);
                break;

            case Globals.MESSAGE_READ:
                byte[] buffer = (byte[]) msg.obj;
                strMessage = new String(buffer);
                trace(strMessage);
                break;

            case Globals.MESSAGE_DISCOVERY_FAILED:
                if (mSearchDriverDialogFragment != null
                        && mSearchDriverDialogFragment.getDialog() != null ) {
                    mSearchDriverDialogFragment.dismiss();
                    mSearchDriverCountDownTimer.cancel();

                    if (mCountDiscoveryFailures++ < Globals.MAX_ALLOWED_DISCOVERY_FAILURES) {
                        mSearchDriverDialogFragment = null;
                        refresh();
                    } else {
                        mCountDiscoveryFailures = 0;
                        showRideCodePane(R.string.discovery_failure,
                                Color.RED);
                    }
                }
                break;
        }

        return true;

    }

    private void btInit() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Globals.__log(LOG_TAG, "Device does not support Bluetooth");
        }

        try {
            // Register the BroadcastReceiver
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(mBtReceiver, filter); // Don't forget to unregister during onDestroy
        } catch(IllegalArgumentException e) {
            // There is no API to check if a receiver is registered.
            // When trying to register it for more than first time, the IllegalArgumentException
            // is raised. Here this exception may be safely ignored.
        }
    }

//    private void btStartDiscovery() {
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            int permissionCheck = this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
//            permissionCheck += this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
//
//            if( permissionCheck != 0 ) {
//                ActivityCompat.requestPermissions(this,
//                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
//                                Manifest.permission.ACCESS_COARSE_LOCATION},
//                                BT_DISCOVERY_PERMISSSION_REQUEST);
//            } else {
//                _btStartDiscovery();
//            }
//        } else {
//            _btStartDiscovery();
//        }
//    }

    private void btStartDiscovery() {
        if( mBluetoothAdapter == null )
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if( !mBluetoothAdapter.isDiscovering() )
            mBluetoothAdapter.startDiscovery ();
    }

    private void btRestore() {
        try {
            unregisterReceiver(mBtReceiver);
        } catch(Exception e) {
            Globals.__logException(e);
        }
    }

    private BtDeviceUser btAnalyzeDevice(BluetoothDevice device){
        if( device == null )
            return null;

        String deviceName = device.getName();
        if( deviceName == null || deviceName.isEmpty() ) {
            Globals.__log(LOG_TAG, "Device name is empty");
            return null;
        }

        BluetoothClass bluetoothClass = device.getBluetoothClass();
        if( bluetoothClass == null )
            return null;

        int deviceClass = bluetoothClass.getMajorDeviceClass();

        if( deviceClass != BluetoothClass.Device.Major.PHONE
                && deviceClass != BluetoothClass.Device.Major.COMPUTER) {
            Globals.__log(LOG_TAG, "Device " + deviceName + "is not PHONE nor COMPUTER");
            return null;
        }

        String[] tokens = deviceName.split(Globals.BT_DELIMITER);
        if( tokens.length != 3)
            return null;

        String authProvider = tokens[0];
        if( !Globals.GOOGLE_PROVIDER.equalsIgnoreCase(authProvider) &&
           !Globals.FB_PROVIDER.equalsIgnoreCase(authProvider)){
            Globals.__log(LOG_TAG, "Unrecognized provider: " + authProvider);
            return null;
        } else
            Globals.__log(LOG_TAG, "Provider: " + authProvider);

        String userId = tokens[1];
        Globals.__log(LOG_TAG, "User registration id: " + userId);

        String rideCode = tokens[2];
        Globals.__log(LOG_TAG, "Ride code: " + rideCode);

        BtDeviceUser btDeviceUser = new BtDeviceUser(device);
        btDeviceUser.set_authProvider(authProvider);
        btDeviceUser.set_UserId(userId);
        btDeviceUser.set_RideCode(rideCode);

        return btDeviceUser;
    }

    public void btRefresh() {

//        _mDrivers.clear();
//        _mDriversAdapter.notifyDataSetChanged();

        if( mBluetoothAdapter != null ) {
            if( mBluetoothAdapter.isDiscovering() )
                mBluetoothAdapter.cancelDiscovery();
        }

        btStartDiscovery();
    }

    //
    // Implementation of IRefreshable
    //
    @Override
    @UiThread
    public void refresh() {
        _mDrivers.clear();
        _mDriversAdapter.notifyDataSetChanged();

        try {
            stopDiscovery(new Runnable() {
                @Override
                public void run() {

                    startDiscovery(PassengerRoleActivity.this,
                            getUser().getRegistrationId(),
                            ""); // empty ride code!

                    showSearchDialog();

                }
            });

        } catch (Exception ex) {
            Globals.__logException(ex);
        }
    }

    @UiThread
    private void showSearchDialog() {
        try {

            mSearchDriverDialogFragment = new SearchDialogFragment();
            mSearchDriverDialogFragment.show(getSupportFragmentManager(), "searchDialog");

        } catch (Exception ex) {
            Globals.__logException(ex);
        }

    }

    @UiThread
    private void showRideCodePane(@StringRes int contentStringResId,
                                  @ColorInt int contentColor) {

        try {
            String dialogContent = getString(contentStringResId);

            new MaterialDialog.Builder(this)
                    .onNegative((new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            btRefresh();
                            showSearchDialog();
                        }
                    }))
                    .title(R.string.ride_code_title)
                    .content(dialogContent)
                    .positiveText(android.R.string.ok)
                    .negativeText(R.string.code_retry_action)
                    .contentColor(contentColor)
                    .inputType(InputType.TYPE_NUMBER_VARIATION_NORMAL | InputType.TYPE_CLASS_NUMBER)
                    .inputRange(Globals.RIDE_CODE_INPUT_LENGTH, Globals.RIDE_CODE_INPUT_LENGTH)
                    .input(R.string.ride_code_hint,
                            R.string.ride_code_refill,
                            new MaterialDialog.InputCallback() {
                                @Override
                                public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                                    mRideCode = input.toString();
                                    onSubmitCode(null);
                                }
                            }

                    ).show();
        } catch (Exception e) {
            Globals.__logException(e);
        }
    }

    private void startDiscovery(final P2pConversator.IPeersChangedListener peersListener,
                                final String userID,
                                final String rideCode) {

        mP2pPreparer = new P2pPreparer(this);
        mP2pPreparer.prepare(new P2pPreparer.P2pPreparerListener() {
            @Override
            public void prepared() {
                Map<String, String> record = new HashMap<>();
                record.put(Globals.TXTRECORD_PROP_PORT, Globals.SERVER_PORT);
                if (!rideCode.isEmpty())
                    record.put(Globals.TXTRECORD_PROP_RIDECODE, rideCode);
                record.put(Globals.TXTRECORD_PROP_USERID, userID);

                mP2pConversator = new P2pConversator(PassengerRoleActivity.this,
                                                    mP2pPreparer,
                                                    getHandler());
                mP2pConversator.startConversation(record, peersListener);

            }

            @Override
            public void interrupted() {

            }
        });
    }

    private void stopDiscovery(final Runnable r) throws Exception {
        if (mP2pPreparer == null && r != null) {
            r.run();
            return;
        }

        mP2pPreparer.undo(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mP2pConversator != null) {
                        mP2pConversator.stopConversation();
                    }

                    if (r != null)
                        r.run();

                } catch (Exception e) {
                    Globals.__logException(e);
                }
            }
        });
    }

    public void onSubmitCode(final String driverId) {

        // Only allow participation request from monitored areas
        if (!Globals.isInGeofenceArea()) {
            new MaterialDialog.Builder(this)
                    .title(R.string.geofence_outside_title)
                    .content(R.string.geofence_outside)
                    .positiveText(R.string.geofence_positive_answer)
                    .negativeText(R.string.geofence_negative_answer)
                    .onPositive(new MaterialDialog.SingleButtonCallback(){
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            Globals.setRemindGeofenceEntrance();
                        }
                    })

                    .show();

            return;
        }

        final String android_id = Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);

        new AsyncTask<Void, Void, Void>() {

            Exception mEx;
            LoadToast lt;

            @Override
            protected void onPreExecute() {

//                RecyclerView driversRecycler = (RecyclerView) findViewById(R.id.recyclerViewDrivers);
//                driversRecycler.setClickable(true);

                lt = new LoadToast(PassengerRoleActivity.this);
                lt.setText(getString(R.string.processing));
                Display display = getWindow().getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                lt.setTranslationY(size.y / 2);
                lt.show();

            }

            @Override
            protected void onPostExecute(Void result) {

//                RecyclerView driversRecycler = (RecyclerView) findViewById(R.id.recyclerViewDrivers);
//                driversRecycler.setClickable(true);

                cancelNotification();

                // Prepare to play sound loud :)
                AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                int sb2value = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, sb2value / 2, 0);

                CustomEvent confirmEvent = new CustomEvent(getString(R.string.passenger_confirmation_answer_name));
                confirmEvent.putCustomAttribute("User", getUser().getFullName());

                Bundle fireEventBundle = new Bundle();
                fireEventBundle.putString(FirebaseAnalytics.Param.VALUE, getString(R.string.passenger_confirmation_answer_name));
                fireEventBundle.putString("Passenger", getUser().getFullName());

                if (mEx != null) {

                    confirmEvent.putCustomAttribute("Error", 0);
                    fireEventBundle.putBoolean("Error", true);

                    try {
                        MobileServiceException mse = (MobileServiceException) mEx.getCause();
                        int responseCode;
                        if (mse.getCause() instanceof UnknownHostException) {
                            responseCode = 503; // Some artificially: usually 503 means
                            // 'Service Unavailable'.
                            // To this extent, we mean 'Connection lost'
                        } else {

                            ServiceFilterResponse response = mse.getResponse();
                            if( response == null ) {
                                responseCode = 503; // No better solution for now.
                                                    // Just interpret this code as 'Connection lost'
                            } else {
                                responseCode = response.getStatus().code;
                            }
                        }

                        switch (responseCode) {

                            case 403: { // HTTP 'Forbidden' means than IDs of the
                                // driver and passenger are same
                                showRideCodePane(R.string.ride_same_ids,
                                        Color.RED);

                                lt.error();
                                beepError.start();
                            }
                            break;

                            case 404: { // HTTP 'Not found' means 'no such ride code'
                                // i.e.
                                // try again with appropriate message
                                showRideCodePane(R.string.ride_code_wrong,
                                        Color.RED);

                                lt.error();
                                beepError.start();
                            }
                            break;

                            case 409: {// HTTP 'Conflict'
                                // picture required
                                // Ride code was successfully validated,
                                // but selfie is required

                                lt.success();

                                onCameraCV(null);
                            }
                            break;

                            case 503: { // HTTP 'Service Unavailable' interpreted as 'Connection Lost'
                                // Try again
                                showRideCodePane(R.string.connection_lost,
                                        Color.RED);
                                lt.error();
                                beepError.start();
                            }
                            break;

                            default:
                                lt.error();
                                Toast.makeText(PassengerRoleActivity.this,
                                        mEx.getMessage(),
                                        Toast.LENGTH_LONG).show();
                                break;
                        }
                    } catch (Exception ex) {
                        Globals.__logException(ex);
                    }

                } else {

                    confirmEvent.putCustomAttribute("Success", 1);
                    fireEventBundle.putBoolean("Success", true);

                    lt.success();
                    beepSuccess.start();

                    getHandler().postDelayed(thanksRunnable, 1500);

                }

                if( Fabric.isInitialized() )
                    Answers.getInstance().logCustom(confirmEvent);

                FirebaseAnalytics firebaseAnalytics =
                        FirebaseAnalytics.getInstance(getApplicationContext());
                firebaseAnalytics.logEvent(getString(R.string.passenger_confirmation_answer_name),
                                           fireEventBundle);


            }

            @Override
            protected Void doInBackground(Void... voids) {

                try {
                    Join _join = new Join();
                    _join.setWhenJoined(new Date());
                    if (mPictureURI != null && !mPictureURI.toString().isEmpty())
                        _join.setPictureURL(mPictureURI.toString());
                    if (mFaceId != null && !mFaceId.toString().isEmpty())
                        _join.setFaceId(mFaceId.toString());
                    _join.setRideCode(mRideCode);
                    String currentGeoFenceName = Globals.get_currentGeoFenceName();
                    _join.setGFenceName(currentGeoFenceName);
                    _join.setDeviceId(android_id);

                    try {
                        Location loc = mCurrentLocation;
                        if (loc != null) {
                            _join.setLat((float) loc.getLatitude());
                            _join.setLon((float) loc.getLongitude());
                        }
                    } catch (Exception e) {
                        Globals.__logException(e);
                    }

                    // The rest of params are set within WAMS insert script
                    String userId = Globals.getMobileServiceClient().getCurrentUser().getUserId();
                    Globals.__log(LOG_TAG, userId);
                    joinsTable.insert(_join).get();

                    if( Globals.isRealtimeDbNotificationsMode() ) {

                        FirebaseDatabase database = FirebaseDatabase.getInstance();
                        DatabaseReference ridesRef = database.getReference("rides");
                        DatabaseReference passengerRef = ridesRef.child(mRideCode)
                                .child("passengers")
                                .child(userId);

                        Map<String, Object> passengerMap = new HashMap<>();
                        passengerMap.put("name", User.load(PassengerRoleActivity.this).getFullName());
                        SimpleDateFormat simpleDate = new SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.US);
                        String dt = simpleDate.format(new Date());
                        passengerMap.put("last_seen", dt);

                        passengerRef
                        .updateChildren(passengerMap)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Globals.__log(LOG_TAG, "Last seen updated");
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Globals.__logException(LOG_TAG, e);
                            }
                        });

                    }

                    // driverId is null when submitted manually
                    // In this case, continuous ride is inappropriate
                    if( driverId != null) {

                        Intent serviceIntent = new Intent(PassengerRoleActivity.this,
                                PassengerCIService.class);
                        serviceIntent.putExtra("driverId", driverId);
                        serviceIntent.putExtra("passengerId", userId);
                        serviceIntent.putExtra("rideCode", mRideCode);

                        // This service will start discovery process again and again
                        // At this point of execution the previous discovery mode is already cancelled.
                        startService(serviceIntent);
                    }

                } catch (ExecutionException | InterruptedException ex) {
                    mEx = ex;
                    Globals.__logException(ex);
                }

                return null;
            }
        }.execute();
    }

    //
    // Implementations of P2pConversator.IPeersChangedListener
    //
    @Override
    public void addDeviceUser(final WifiP2pDeviceUser device) {

        if (device.getRideCode() == null)
            return;

        String remoteUserID = device.getUserId();
        if (remoteUserID == null || remoteUserID.isEmpty()) {
            // remote user id was not transmitted
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
//                    mDriversAdapter.add(device);
//                    mDriversAdapter.notifyDataSetChanged();
                }
            });
        } else {

            String[] tokens = remoteUserID.split(":");
            Assert.assertTrue(tokens.length == 2);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
//                    mDriversAdapter.add(device);
//                    mDriversAdapter.notifyDataSetChanged();

                    mDriversShown = true;
                }
            });

        }
    }

    //
    // Implementation of ITrace
    //
    @Override
    public void trace(final String status) {

    }

    @Override
    public void alert(String message, final String actionIntent) {

        new MaterialDialog.Builder(this)
                .title(message)
                .negativeText(android.R.string.no) // dialogClickListener)
                .positiveText(android.R.string.yes) //, dialogClickListener)
                .onAny(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        if (which == DialogAction.POSITIVE) {
                            startActivity(new Intent(actionIntent));
                        }
                    }
                })
                .show();
    }

    //
    // Implementation of IRecyclerClickListener
    //
    @Override
    public void clicked(View view, int position) {

        if (!Globals.isInGeofenceArea()) {
            new MaterialDialog.Builder(this)
                    .title(R.string.geofence_outside_title)
                    .content(R.string.geofence_outside)
                    .positiveText(R.string.geofence_positive_answer)
                    .negativeText(R.string.geofence_negative_answer)
                    .onPositive(new MaterialDialog.SingleButtonCallback(){
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            Globals.setRemindGeofenceEntrance();
                        }
                    })
                    .show();
        } else {

            if (position >= 0
                    && mRideCode == null) {

                // Ride code was stored if the activity is invoked from notification
                // In this case the position is -1 because this
                // function is called manually (from within onNewIntent())
                Assert.assertNotNull(_mDrivers);

                BtDeviceUser driverDevice = _mDrivers.get(position);
                Assert.assertNotNull(driverDevice);

                mBluetoothAdapter.cancelDiscovery();

                mRideCode = driverDevice.get_RideCode();
                mDriverName = driverDevice.get_UserName();

                String driverId = driverDevice.get_UserId();
                onSubmitCode(driverId);
            }
        }

    }

    private Runnable thanksRunnable = new Runnable() {
        @Override
        public void run() {
            try {

                DialogFragment f = new ThanksDialogFragment();
                f.show(getSupportFragmentManager(), "dialog");

            } catch(Exception ex){
                Globals.__log(LOG_TAG, "PassengerActivity was dismissed before showing dialog: " + ex.getMessage());
            }
        }
    };

    // DialogFragments

    public static class SearchDialogFragment extends DialogFragment {

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState){
            return new MaterialDialog.Builder(getContext())
                    .title(R.string.passenger_progress_dialog)
                    .content(R.string.please_wait)
                    .iconRes(R.drawable.ic_wait)
                    .cancelable(false)
                    .autoDismiss(false)
                    //.progress(false, Globals.PASSENGER_DISCOVERY_PERIOD, true)
                    .progress(true, 0)
                    .negativeText(android.R.string.cancel)
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            dialog.dismiss();

                            PassengerRoleActivity activity = ((PassengerRoleActivity)getActivity());
                            if( activity != null ) {

                                if( activity.mSearchDriverCountDownTimer != null )
                                    activity.mSearchDriverCountDownTimer.cancel();

                                activity.showRideCodePane(R.string.ride_code_dialog_content,
                                                        Color.BLACK);
                            }
                        }
                    })
                    .show();
        }

    }

    public static class JoinConfirmDialogFragment extends DialogFragment {

        private static final String DRIVER_NAME_TAG = "driverName";
        private static String mDriverName;

        public static JoinConfirmDialogFragment newInstance(String driverName) {
            JoinConfirmDialogFragment f = new JoinConfirmDialogFragment();
            Bundle args = new Bundle();
            args.putString(DRIVER_NAME_TAG, driverName);
            f.setArguments(args);
            return f;
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putString(Globals.PARCELABLE_KEY_DRVER_NAME, mDriverName);
        }

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState){

            StringBuilder sb = new StringBuilder(getString(R.string.passenger_confirm));

            if( savedInstanceState != null ) {
                mDriverName = savedInstanceState.getString(Globals.PARCELABLE_KEY_DRVER_NAME);
            }
            else {
                mDriverName = getArguments().getString(DRIVER_NAME_TAG);
            }

            if (mDriverName != null) {
                sb.append(" ");
                sb.append(getString(R.string.with));
                sb.append(" ");
                sb.append(mDriverName);
            }

            sb.append("?");

            return new MaterialDialog.Builder(getContext())
                    .title(sb.toString())
                    .negativeText(android.R.string.no) //, dialogClickListener)
                    .positiveText(android.R.string.yes) // dialogClickListener)
                    .onAny(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            if (which == DialogAction.POSITIVE) {
                                ((PassengerRoleActivity)getActivity()).onSubmitCode(null);
                            }
                        }
                    })
                    .show();

        }
    }

    public static class ThanksDialogFragment extends DialogFragment {
        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState){
            return  new MaterialDialog.Builder(getContext())
                    .title(R.string.thanks)
                    .content(R.string.confirmation_accepted)
                    .cancelable(false)
                    .positiveText(R.string.close)
                    .onPositive(new MaterialDialog.SingleButtonCallback(){
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            ((Activity)getContext()).finish();
                        }
                    })
                    .show();
        }
    }
}
