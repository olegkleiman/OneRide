package com.labs.okey.oneride;

import android.*;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import com.github.brnunes.swipeablerecyclerview.SwipeableRecyclerViewTouchListener;
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
import com.labs.okey.oneride.adapters.PassengersAdapter;
import com.labs.okey.oneride.model.GFCircle;
import com.labs.okey.oneride.model.PassengerFace;
import com.labs.okey.oneride.model.Ride;
import com.labs.okey.oneride.model.User;
import com.labs.okey.oneride.utils.Globals;
import com.labs.okey.oneride.utils.ITrace;
import com.labs.okey.oneride.utils.RoundedDrawable;
import com.labs.okey.oneride.utils.UiThreadExecutor;
import com.labs.okey.oneride.utils.wamsAddAppeal;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;

import junit.framework.Assert;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Oleg Kleiman
 * created 04-Apr-16
 */
public class DriverRoleActivity extends BaseActivityWithGeofences
        implements ITrace,
                Handler.Callback,
                android.location.LocationListener,
                // Added for Google Map support within sliding panel
                OnMapReadyCallback,
                GoogleApiClient.ConnectionCallbacks{

    private final String                        LOG_TAG = getClass().getSimpleName();

    private PassengersAdapter                   mPassengersAdapter;
    SwipeableRecyclerViewTouchListener          mSwipeTouchListener;
    private ArrayList<User>                     mPassengers = new ArrayList<>();
    private int                                 mLastPassengersLength;

    private ArrayList<Integer>                  mCapturedPassengersIDs = new ArrayList<>();

    private ImageView                           mImageTransmit;
    private GoogleMap                           mGoogleMap;
    private Circle                              meCircle;

    private MobileServiceTable<Ride>            mRidesTable;
    String                                      mCarNumber;
    Uri                                         mUriPhotoAppeal;
    private String                              mRideCode;
    int                                         mEmojiID;

    private ScheduledExecutorService            mCheckPasengersTimer = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?>                  mCheckPassengerTimerResult;

    private TextSwitcher                        mTextSwitcher;
    private RecyclerView                        mPeersRecyclerView;

    private Location                            mCurrentLocation;
    private long                                mLastLocationUpdateTime;

    Ride                                        mCurrentRide;

    private AtomicBoolean                       mRideCodeUploaded = new AtomicBoolean(false);

    // codes handled in onActivityResult()
    final int WIFI_CONNECT_REQUEST  = 100;// request code for starting WiFi connection
    final int REQUEST_IMAGE_CAPTURE = 1000;

    private Handler handler = new Handler(this);

    public Handler getHandler() {
        return handler;
    }

    MaterialDialog                              mOfflineDialog;
    MaterialDialog                              mAppealDialog;

    private boolean                             mCabinPictureButtonShown = false;
    private boolean                             mCabinShown = false;
    private boolean                             mSubmitButtonShown = false;
    private boolean                             mEmptyTextShown = true;

    private Runnable                            mEnableCabinPictureButtonRunnable = new Runnable() {

        @Override
        public void run() {

            if( Globals.isInGeofenceArea() ) {
                FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.submit_ride_button);
                if( fab != null )
                    fab.setVisibility(View.VISIBLE);

                mTextSwitcher.setText(getString(R.string.instruction_not_enought_passengers));

                mCabinPictureButtonShown = true;
            } else {
                getHandler().postDelayed(mEnableCabinPictureButtonRunnable,
                        Globals.CABIN_PICTURES_BUTTON_SHOW_INTERVAL);
            }
        }
    };

    @Override
    @CallSuper
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_role);

        mOfflineDialog = new MaterialDialog.Builder(this)
                .title(R.string.offline)
                .content(R.string.offline_prompt)
                .iconRes(R.drawable.ic_exclamation)
                .autoDismiss(true)
                .cancelable(false)
                .positiveText(getString(R.string.try_again))
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {

                        if (!isConnectedToNetwork()) {
                            getHandler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mOfflineDialog.show();
                                }
                            }, 200);
                        } else {
                            setupNetwork();
                        }
                    }
                }).build();

        mAppealDialog = new MaterialDialog.Builder(this)
                .title(R.string.appeal_answer)
                .iconRes(R.drawable.ic_info)
                .positiveText(R.string.appeal_send)
                .negativeText(R.string.appeal_cancel)
                .neutralText(R.string.appeal_another_picture)
                .customView(R.layout.dialog_appeal_answer, false) // do not wrap in scroll
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        sendAppeal();
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        finish();
                    }

                    @Override
                    public void onNeutral(MaterialDialog dialog) {
                        onAppealCamera();
                    }
                }).build();

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

        TextView txtRideCode = (TextView) findViewById(R.id.txtRideCode);
        String rideCode = txtRideCode.getText().toString();
        if( !rideCode.isEmpty() ) {

            outState.putString(Globals.PARCELABLE_KEY_RIDE_CODE, rideCode);
            outState.putBoolean(Globals.PARCELABLE_KEY_RIDE_CODE_UPLOADED, mRideCodeUploaded.get()); // isRideCodeUploaded());
            outState.putParcelableArrayList(Globals.PARCELABLE_KEY_PASSENGERS, mPassengers);

            outState.putParcelable(Globals.PARCELABLE_KEY_CURRENT_RIDE, mCurrentRide);

            outState.putBoolean(Globals.PARCELABLE_KEY_CABIN_PICTURES_BUTTON_SHOWN, mCabinPictureButtonShown);

            outState.putIntegerArrayList(Globals.PARCELABLE_KEY_CAPTURED_PASSENGERS_IDS, mCapturedPassengersIDs);

            outState.putSerializable(Globals.PARCELABLE_KEY_PASSENGERS_FACE_IDS, Globals.get_PassengerFaces());

            if( mUriPhotoAppeal != null)
                outState.putString(Globals.PARCELABLE_KEY_APPEAL_PHOTO_URI, mUriPhotoAppeal.toString());

            outState.putInt(Globals.PARCELABLE_KEY_EMOJIID, mEmojiID);

            outState.putBoolean(Globals.PARCELABLE_KEY_DRIVER_CABIN_SHOWN, mCabinShown);

            outState.putBoolean(Globals.PARCELABLE_KEY_SUBMIT_BUTTON_SHOWN, mSubmitButtonShown);

            outState.putBoolean(Globals.PARCELABLE_KEY_EMPTY_TEXT_SHOWN, mEmptyTextShown);

            // Store passengers captured thumbnails, if any
            View rootView = findViewById(R.id.cabin_background_layout);
            try {
                for (int i = 1; i <= Globals.REQUIRED_PASSENGERS_NUMBER; i++) {
                    String tag = Integer.toString(i);
                    FloatingActionButton passengerPictureButton = (FloatingActionButton) rootView.findViewWithTag(tag);
                    if (passengerPictureButton != null) {

                        String parcelableKey = Globals.PARCELABLE_KEY_PASSENGER_PREFIX + tag;

                        Drawable drawableThumb = passengerPictureButton.getDrawable();
                        if (drawableThumb instanceof BitmapDrawable) {
                            BitmapDrawable bmpDrawable = (BitmapDrawable) drawableThumb;

                            outState.putParcelable(parcelableKey, bmpDrawable.getBitmap());
                        } else {
                            Bitmap bmp = convertToBitmap(drawableThumb,
                                    drawableThumb.getIntrinsicWidth(),
                                    drawableThumb.getIntrinsicHeight());
                            outState.putParcelable(parcelableKey, bmp);
                        }

                    } else {
                        String msg = String.format("onSaveInstanceState: Passenger FAB %d is not found", i);
                        Log.e(LOG_TAG, msg);
                    }

                }
            }
            catch(Exception ex){

                if( Crashlytics.getInstance() != null )
                    Crashlytics.logException(ex);

                Log.e(LOG_TAG, ex.getMessage());
            }

            if( mAppealDialog.isShowing() ) {
                outState.putBoolean(Globals.PARCELABLE_KEY_APPEAL_DIALOG_SHOWN, true);
            }
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    @UiThread
    protected void setupUI(String title, String subTitle) {
        super.setupUI(title, subTitle);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.gf_map);
        mapFragment.getMapAsync(this);

        // Prevent memory leak on ImageView
        View cabinImageView = findViewById(R.id.centerImage);
        if( cabinImageView != null )
            cabinImageView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        mPeersRecyclerView = (RecyclerView) findViewById(R.id.recyclerViewPeers);
        if( mPeersRecyclerView != null ) {
            mPeersRecyclerView.setHasFixedSize(true);

            Globals.LayoutManagerType passengerLayoutManagerType;

            if( getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ) {
                mPeersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                passengerLayoutManagerType = Globals.LayoutManagerType.LINEAR_LAYOUT_MANAGER;
            }
            else {
                mPeersRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
                passengerLayoutManagerType = Globals.LayoutManagerType.GRID_LAYOUT_MANAGER;
            }

            mPeersRecyclerView.setItemAnimator(new DefaultItemAnimator());

            mPassengersAdapter = new PassengersAdapter(this,
                    passengerLayoutManagerType,
                    R.layout.peers_header,
                    R.layout.row_passenger,
                    mPassengers);
            mPeersRecyclerView.setAdapter(mPassengersAdapter);

            mSwipeTouchListener =
                    new SwipeableRecyclerViewTouchListener(mPeersRecyclerView,
                            new SwipeableRecyclerViewTouchListener.SwipeListener() {
                                @Override
                                public boolean canSwipe(int position) {
                                    return true;
                                }

                                @Override
                                public void onDismissedBySwipeLeft(RecyclerView recyclerView, int[] reverseSortedPositions) {
//                                for (int position : reverseSortedPositions) {
////                                    Toast.makeText(MainActivity.this, mItems.get(position) + " swiped left", Toast.LENGTH_SHORT).show();
//                                    mItems.remove(position);
//                                    mAdapter.notifyItemRemoved(position);
//                                }
//                                mAdapter.notifyDataSetChanged();
                                }

                                @Override
                                public void onDismissedBySwipeRight(RecyclerView recyclerView, int[] reverseSortedPositions) {
//                                for (int position : reverseSortedPositions) {
////                                    Toast.makeText(MainActivity.this, mItems.get(position) + " swiped right", Toast.LENGTH_SHORT).show();
//                                    mItems.remove(position);
//                                    mAdapter.notifyItemRemoved(position);
//                                }
//                                mAdapter.notifyDataSetChanged();
                                }
                            });
            mPeersRecyclerView.addOnItemTouchListener(mSwipeTouchListener);
        }

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

        Globals.setMonitorStatus(getString(R.string.geofence_outside_title));

        for(int i = 0; i < Globals.REQUIRED_PASSENGERS_NUMBER; i++)
            mCapturedPassengersIDs.add(0);

        if( Globals.REQUIRED_PASSENGERS_NUMBER == 3 ) {
            View view = findViewById(R.id.passenger4);
            if( view != null )
                view.setVisibility(View.GONE);
        }
    }

    private void setupNetwork() {

        getHandler().postDelayed(mEnableCabinPictureButtonRunnable,
                Globals.CABIN_PICTURES_BUTTON_SHOW_INTERVAL);

        mCheckPassengerTimerResult =
                mCheckPasengersTimer.scheduleAtFixedRate(new Runnable() {
                                                             @Override
                                                             public void run() {

                                                                 final List<User> passengers = Globals.getMyPassengers();

                                                                 if( mLastPassengersLength != passengers.size()
                                                                         || Globals.isPassengerListAlerted() ) {

                                                                     mLastPassengersLength = passengers.size();
                                                                     Globals.setPassengerListAlerted(false);

                                                                     // Update UI on UI thread
                                                                     runOnUiThread(new Runnable() {
                                                                         @Override
                                                                         public void run() {
                                                                             mPassengers.clear();
                                                                             mPassengers.addAll(passengers);

                                                                             for(User passenger: mPassengers) {
                                                                                 if( passenger.wasSelfPictured() ) {
                                                                                     mTextSwitcher.setText(getString(R.string.instruction_advanced_mode));
                                                                                     break;
                                                                                 }
                                                                             }

                                                                             mPassengersAdapter.notifyDataSetChanged();

                                                                             View v = findViewById(R.id.empty_view);
                                                                             if( v != null ) {
                                                                                 mEmptyTextShown = false;
                                                                                 v.setVisibility(View.GONE);
                                                                             }

                                                                             if( mLastPassengersLength >= Globals.REQUIRED_PASSENGERS_NUMBER){
                                                                                 FloatingActionButton fab = (FloatingActionButton)findViewById(R.id.submit_ride_button);

                                                                                 Context ctx =  getApplicationContext();
                                                                                 fab.setImageDrawable(ContextCompat.getDrawable(ctx, R.drawable.ic_action_done));
                                                                                 fab.setTag(getString(R.string.submit_tag));

                                                                                 mTextSwitcher.setText(getString(R.string.instruction_can_submit_no_fee));
                                                                             }

                                                                         }
                                                                     });

                                                                 }

                                                             }
                                                         },
                        1, // 1-sec delay
                        2, // period between successive executions
                        TimeUnit.SECONDS);


    }

    private void wamsInit() {

        super.wamsInit(true);

        mRidesTable = Globals.getMobileServiceClient().getTable("rides", Ride.class);
    }

    @Override
    @CallSuper
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        View rootView = findViewById(R.id.cabin_background_layout);
        String tag = Integer.toString(requestCode);

        if( requestCode == REQUEST_IMAGE_CAPTURE
                && resultCode == RESULT_OK) {

            try {

                View view = mAppealDialog.getCustomView();
                if( view == null)
                    return;

                ImageView imageViewAppeal =  (ImageView)view.findViewById(R.id.imageViewAppeal);
                if( imageViewAppeal != null ) {

                    Drawable drawable = imageViewAppeal.getDrawable();
                    if( drawable != null ) {
                        ((BitmapDrawable) drawable).getBitmap().recycle();
                    }

                    // Downsample the image to consume less memory
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 2;
                    Bitmap bitmap = BitmapFactory.decodeFile(mUriPhotoAppeal.getPath(), options);
                    imageViewAppeal.setImageBitmap(bitmap);
                }

                mAppealDialog.show();

            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
            }

        } else if( requestCode == WIFI_CONNECT_REQUEST ) {

            // if( resultCode == RESULT_OK ) {
            // How to distinguish between successful connection
            // and just pressing back from there?
            wamsInit(true);

        } else if( (requestCode >= 1 && requestCode <= 4 )  // passengers selfies
                && resultCode == RESULT_OK ) {

            FloatingActionButton passengerPicture = (FloatingActionButton)rootView.findViewWithTag(tag);
            if( passengerPicture != null ) {
                passengerPicture.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.green)));

                Bundle extras = data.getExtras();
                if( extras != null ) {
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

                    UUID _faceId = (UUID) extras.getSerializable(getString(R.string.detection_face_id));
                    URI faceURI = (URI) extras.getSerializable(getString(R.string.detection_face_uri));
                    if( faceURI != null )
                        addPassengerFace(requestCode - 1, _faceId, faceURI.toString());
                }
            }

            mCapturedPassengersIDs.set(requestCode -1, 1);
        }
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

    //
    // Implementation of Handler.Callback
    //
    @Override
    public boolean handleMessage(Message msg) {

        switch (msg.what) {
            case Globals.TRACE_MESSAGE:
                Bundle bundle = msg.getData();
                String strMessage = bundle.getString("message");
                trace(strMessage);
                break;

            case Globals.MESSAGE_READ:
                byte[] buffer = (byte[]) msg.obj;
                strMessage = new String(buffer);
                trace(strMessage);
                break;

        }

        return true;
    }

    //
    // ITrace implementation
    //

    @Override
    public void trace(final String status) {

    }

    @Override
    public void alert(String message, final String actionIntent) {
//        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
//
//            @Override
//            public void onClick(DialogInterface dialogInterface, int which) {
//                if (which == DialogInterface.BUTTON_POSITIVE) {
//                    startActivityForResult(new Intent(actionIntent), WIFI_CONNECT_REQUEST);
//                }
//            }
//        };
//
//        new AlertDialogWrapper.Builder(this)
//                .setTitle(message)
//                .setNegativeButton(R.string.no, dialogClickListener)
//                .setPositiveButton(R.string.yes, dialogClickListener)
//                .show();

        AnimationDrawable animationDrawable = (AnimationDrawable) mImageTransmit.getDrawable();
        if( actionIntent.isEmpty() ) {
            animationDrawable.start();
        } else {
            animationDrawable.stop();
        }

    }

    private void onSubmitRideInternal() {

    }

    private Bitmap convertToBitmap(Drawable drawable, int widthPixels, int heighPixels) {
        Bitmap mutableBitmap = Bitmap.createBitmap(widthPixels, heighPixels, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mutableBitmap);
        drawable.setBounds(0, 0, widthPixels, heighPixels);
        drawable.draw(canvas);

        return mutableBitmap;
    }

    public  void sendAppeal(){
        mCurrentRide.setApproved(Globals.RIDE_STATUS.APPEAL.ordinal());
        new wamsAddAppeal(DriverRoleActivity.this,
                getUser().getFullName(),
                "appeals",
                mCurrentRide.Id,
                getUser().getRegistrationId(),
                mEmojiID)
                .execute(new File(mUriPhotoAppeal.getPath()));
    }

    public void onAppealCamera(){

        MaterialDialog.Builder builder = new MaterialDialog.Builder(this);

        try {

            builder.title(R.string.appeal)
                    .positiveText(android.R.string.ok)
                    .negativeText(android.R.string.cancel)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            AppealCamera();
                        }

                        @Override
                        public void onNegative(MaterialDialog dialog) {
                            finish();
                        }

//                        @Override
//                        public void onNeutral(MaterialDialog dialog) {
//                            Intent intent = new Intent(getApplicationContext(),
//                                                        TutorialActivity.class);
//                            intent.putExtra(getString(R.string.tutorial_id), Globals.TUTORIAL_Appeal);
//                            startActivity(intent);
//                        }
                    });

            View customDialog = getLayoutInflater().inflate(R.layout.dialog_appeal, null);
            builder.customView(customDialog, false);

            if( mEmojiID == 0 ){
                mEmojiID =  new Random().nextInt(Globals.NUM_OF_EMOJIS)
                        // nextInt() gets number between 0 (inclusive and specified value (exclusive)
                        + 1;
            }

            String uri = "@drawable/emoji_" + Integer.toString(mEmojiID);
            int imageResource = getResources().getIdentifier(uri, "id",  this.getPackageName());
            ImageView emojiImageView = (ImageView)customDialog.findViewById(R.id.appeal_emoji);
            emojiImageView.setImageResource(imageResource);

            builder.show();

        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            // better that catch the exception here would be use handle to send events the activity
        }
    }

    public void AppealCamera(){

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {

            try {
                mUriPhotoAppeal = createImageFile();
            } catch (IOException e) {
                Log.e(LOG_TAG, e.getMessage());
            }

            if (mUriPhotoAppeal != null) {

                intent.putExtra(MediaStore.EXTRA_OUTPUT, mUriPhotoAppeal);
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private Uri createImageFile() throws IOException {

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String photoFileName = "AppealJPEG_" + timeStamp + "_";

        File storageDir = getExternalFilesDir(null);

        File photoFile = File.createTempFile(
                photoFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        return Uri.fromFile(photoFile);
    }

    private void addPassengerFace(int at, UUID faceID, String faceURI) {

        try {

            boolean bPFaceAdded = false;

            PassengerFace pFace;
            pFace = Globals.get_PassengerFace(at);
            if( pFace == null ) {
                pFace = new PassengerFace();

                bPFaceAdded = true;
            }
            pFace.setFaceId(faceID.toString());
            pFace.setPictureUrl(faceURI);

            if( bPFaceAdded )
                Globals.get_PassengerFaces().put(at, pFace);

            int size = 0;
            for (Map.Entry<Integer,PassengerFace> entry : Globals.get_PassengerFaces().entrySet()) {

                PassengerFace pf = entry.getValue();

                if (pf.isInitialized())
                    size++;
            }

            if( size >= Globals.REQUIRED_PASSENGERS_NUMBER ) {
                getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        showSubmitPicsButton();
                    }
                });
            }


        } catch(Exception ex) {

            if( Crashlytics.getInstance() != null )
                Crashlytics.logException(ex);

            Log.e(LOG_TAG, ex.getMessage());
        }
    }

    @UiThread
    private void showSubmitPicsButton() {

        FloatingActionButton fab = (FloatingActionButton)findViewById(R.id.submit_ride_button_pics);
        Context ctx =  getApplicationContext();
        if( fab != null) {
            fab.setVisibility(View.VISIBLE);
            fab.setImageDrawable(ContextCompat.getDrawable(ctx, R.drawable.ic_action_done));

            mTextSwitcher.setText(getString(R.string.instruction_can_submit_no_fee));

            mSubmitButtonShown = true;
        }
    }
}
