package com.labs.okey.oneride;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationProvider;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.CallSuper;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Display;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.github.brnunes.swipeablerecyclerview.SwipeableRecyclerViewTouchListener;
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
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.labs.okey.oneride.adapters.PassengersAdapter;
import com.labs.okey.oneride.model.Approval;
import com.labs.okey.oneride.model.GFCircle;
import com.labs.okey.oneride.model.Join;
import com.labs.okey.oneride.model.PassengerFace;
import com.labs.okey.oneride.model.Ride;
import com.labs.okey.oneride.model.User;
import com.labs.okey.oneride.model.WifiP2pDeviceUser;
import com.labs.okey.oneride.utils.Globals;
import com.labs.okey.oneride.utils.ITrace;
import com.labs.okey.oneride.utils.IUploader;
import com.labs.okey.oneride.utils.RoundedDrawable;
import com.labs.okey.oneride.utils.UiThreadExecutor;
import com.labs.okey.oneride.utils.faceapiUtils;
import com.labs.okey.oneride.utils.wifip2p.P2pConversator;
import com.labs.okey.oneride.utils.wifip2p.P2pPreparer;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;

import junit.framework.Assert;

import net.steamcrafted.loadtoast.LoadToast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import io.fabric.sdk.android.Fabric;

/**
 * @author Oleg Kleiman
 * created 04-Apr-16
 */
public class DriverRoleActivity extends BaseActivityWithGeofences
        implements P2pConversator.IPeersChangedListener,
                ITrace,
                Handler.Callback,
                LocationListener,
                IUploader,
                // Added for Google Map support within sliding panel
                OnMapReadyCallback,
                GoogleApiClient.ConnectionCallbacks{

    private final String                        LOG_TAG = getClass().getSimpleName();

    private PassengersAdapter                   mPassengersAdapter;
    SwipeableRecyclerViewTouchListener          mSwipeTouchListener;
    private ArrayList<User>                     mPassengers = new ArrayList<>();

    private ImageView                           mImageTransmit;
    private GoogleMap                           mGoogleMap;
    private Circle                              meCircle;

    private MobileServiceTable<Ride>            mRidesTable;
    String                                      mCarNumber;
    Uri                                         mUriPhotoApproval;
    private String                              mRideCode;
    private int                                 mEmojiID = 0;
    public void setEmojiId(int value) {
        mEmojiID = value;
    }
    public int getmEmojiID() {
        return mEmojiID;
    }


    private P2pPreparer                         mP2pPreparer;
    private P2pConversator                      mP2pConversator;

    private TextSwitcher                        mTextSwitcher;
    private RecyclerView                        mPeersRecyclerView;

    private long                                mLastLocationUpdateTime;

    Ride                                        mCurrentRide;

    private AtomicBoolean                       mRideCodeUploaded = new AtomicBoolean(false);

    // codes handled in onActivityResult()
    final int                                   WIFI_CONNECT_REQUEST  = 100;// request code for starting WiFi connection
    final int                                   BT_REQUEST_DISCOVERABLE_CODE = 101;
    final int                                   REQUEST_IMAGE_CAPTURE = 1000;

    private Handler handler = new Handler(this);

    public Handler getHandler() {
        return handler;
    }

    MaterialDialog                              mOfflineDialog;
    MaterialDialog                              mApprovalDialog;

    private boolean                             mCabinPictureButtonShown = false;
    private boolean                             mCabinShown = false;
    private boolean                             mSubmitButtonShown = false;
    private boolean                             mEmptyTextShown = true;

    private BluetoothAdapter                    mBluetoothAdapter;

    private final BroadcastReceiver mBtReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if( BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action) ) {
                final int newScanMode = intent.getIntExtra( BluetoothAdapter.EXTRA_SCAN_MODE,
                                                            BluetoothAdapter.ERROR);
                Globals.__log(LOG_TAG, String.format(Locale.getDefault(), "Scan mode changed to: %d", newScanMode) );
                if( newScanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE ) {
                    // Start animation
                    startTransmitAnimation();
                } else {
                    // Stop animation
                    stopTransmitAnimation();
                }
            }
        }
    };

    private Runnable mEnableCabinPictureButtonRunnable = new Runnable() {

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
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_role);

        mOfflineDialog = new MaterialDialog.Builder(this)
                .title(R.string.offline)
                .content(R.string.offline_prompt)
                .iconRes(R.drawable.ic_exclamation)
                .autoDismiss(true)
                .cancelable(false)
                .positiveText(getString(R.string.try_again))
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        if( !isConnectedToNetwork()) {
                            getHandler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mOfflineDialog.show();
                                }
                            }, 200);
                        }
                    }
                })
                .build();

        mApprovalDialog = new MaterialDialog.Builder(this)
                .title(R.string.approval_answer)
                .iconRes(R.drawable.ic_info)
                .positiveText(R.string.appeal_send)
                .negativeText(R.string.appeal_cancel)
                .customView(R.layout.dialog_approval_answer, false) // do not wrap in scroll
                .onPositive(new MaterialDialog.SingleButtonCallback(){
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        sendToValidateManually(getmEmojiID());
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback(){
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        finish();
                    }
                })
                .build();

        // Keep device awake when advertising for Wi-Fi Direct
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        forceLTR();

        setupUI(getString(R.string.title_activity_driver_role), "");
        mRideCodeUploaded.set(false);

        if (savedInstanceState != null) {
            wamsInit();
            geoFencesInit();

            restoreInstanceState(savedInstanceState);
        } else {
            if( isConnectedToNetwork() ) {

                wamsInit();
                geoFencesInit();

                btInit();
                btStartAdvertise();
                //setupNetwork();
            }
        }

        Globals.setDriverActivity(this);

    }

    private void restoreInstanceState(Bundle savedInstanceState) {
        boolean bInitializedBeforeRotation = false;

        if( savedInstanceState.containsKey(Globals.PARCELABLE_LOCATION) )
            mCurrentLocation = savedInstanceState.getParcelable(Globals.PARCELABLE_LOCATION);

        if (savedInstanceState.containsKey(Globals.PARCELABLE_KEY_RIDE_CODE)) {
            bInitializedBeforeRotation = true;

            mRideCode = savedInstanceState.getString(Globals.PARCELABLE_KEY_RIDE_CODE);
            mRideCodeUploaded.set( savedInstanceState.getBoolean(Globals.PARCELABLE_KEY_RIDE_CODE_UPLOADED) );

            TextView txtRideCode = (TextView) findViewById(R.id.txtRideCode);
            txtRideCode.setVisibility(View.VISIBLE);
            txtRideCode.setText(mRideCode);

            TextView txtRideCodeCaption = (TextView) findViewById(R.id.code_label_caption);
            txtRideCodeCaption.setText(R.string.ride_code_label);

            ImageView imageTransmit = (ImageView) findViewById(R.id.img_transmit);
            imageTransmit.setVisibility(View.VISIBLE);
            AnimationDrawable animationDrawable = (AnimationDrawable) imageTransmit.getDrawable();
            animationDrawable.start();
        }

        if (savedInstanceState.containsKey(Globals.PARCELABLE_KEY_CURRENT_RIDE)) {
            bInitializedBeforeRotation = true;
            mCurrentRide = savedInstanceState.getParcelable(Globals.PARCELABLE_KEY_CURRENT_RIDE);
        }

        if (savedInstanceState.containsKey(Globals.PARCELABLE_KEY_PASSENGERS)) {
            bInitializedBeforeRotation = true;
            ArrayList<User> passengers = savedInstanceState.getParcelableArrayList(Globals.PARCELABLE_KEY_PASSENGERS);
            if (passengers != null) {

                mPassengers.addAll(passengers);
                mPassengersAdapter.notifyDataSetChanged();

                if (passengers.size() >= Globals.REQUIRED_PASSENGERS_NUMBER) {
                    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.submit_ride_button);
                    fab.setVisibility(View.VISIBLE);
                    Context ctx = getApplicationContext();
                    fab.setImageDrawable(ContextCompat.getDrawable(ctx, R.drawable.ic_action_done));
                    fab.setTag(getString(R.string.submit_tag));
                }

            }

        }

        if (savedInstanceState.containsKey(Globals.PARCELABLE_KEY_CABIN_PICTURES_BUTTON_SHOWN)) {
            bInitializedBeforeRotation = true;

            mCabinPictureButtonShown = savedInstanceState.getBoolean(Globals.PARCELABLE_KEY_CABIN_PICTURES_BUTTON_SHOWN);
            if (mCabinPictureButtonShown)
                findViewById(R.id.submit_ride_button).setVisibility(View.VISIBLE);
            else
                findViewById(R.id.submit_ride_button).setVisibility(View.GONE);
        }

        if (savedInstanceState.containsKey(Globals.PARCELABLE_KEY_PASSENGERS_FACE_IDS)) {
            bInitializedBeforeRotation = true;

            HashMap<Integer, PassengerFace> hash_map =
                    (HashMap) savedInstanceState.getSerializable(Globals.PARCELABLE_KEY_PASSENGERS_FACE_IDS);

            Globals.set_PassengerFaces(hash_map);

            // See the explanation of covariance in Java here
            // http://stackoverflow.com/questions/6951306/cannot-cast-from-arraylistparcelable-to-arraylistclsprite
//                ArrayList<PassengerFace> _temp = savedInstanceState.getParcelableArrayList(Globals.PARCELABLE_KEY_PASSENGERS_FACE_IDS);
//                Globals.set_PassengerFaces(_temp);
        }

        if (savedInstanceState.containsKey(Globals.PARCELABLE_KEY_APPROVAL_PHOTO_URI)) {
            bInitializedBeforeRotation = true;

            String str = savedInstanceState.getString(Globals.PARCELABLE_KEY_APPROVAL_PHOTO_URI);
            mUriPhotoApproval = Uri.parse(str);
        }

        if (savedInstanceState.containsKey(Globals.PARCELABLE_KEY_EMOJIID)) {
            bInitializedBeforeRotation = true;

            mEmojiID = savedInstanceState.getInt(Globals.PARCELABLE_KEY_EMOJIID);
        }

        if (savedInstanceState.containsKey(Globals.PARCELABLE_KEY_DRIVER_CABIN_SHOWN)) {
            bInitializedBeforeRotation = true;

            mCabinShown = savedInstanceState.getBoolean(Globals.PARCELABLE_KEY_DRIVER_CABIN_SHOWN);

            if (mCabinShown)
                showCabinView();
        }

        if (savedInstanceState.containsKey(Globals.PARCELABLE_KEY_SUBMIT_BUTTON_SHOWN)) {
            bInitializedBeforeRotation = true;

            mSubmitButtonShown = savedInstanceState.getBoolean(Globals.PARCELABLE_KEY_SUBMIT_BUTTON_SHOWN);

            if (mSubmitButtonShown)
                showSubmitButton();

        }

        View vEmptyText = findViewById(R.id.empty_view);
        if( vEmptyText != null && savedInstanceState.containsKey(Globals.PARCELABLE_KEY_EMPTY_TEXT_SHOWN )) {

            mEmptyTextShown = savedInstanceState.getBoolean(Globals.PARCELABLE_KEY_EMPTY_TEXT_SHOWN );
            if( mEmptyTextShown )
                vEmptyText.setVisibility(View.VISIBLE);
            else
                vEmptyText.setVisibility(View.GONE);
        }

        if (savedInstanceState.containsKey(Globals.PARCELABLE_KEY_APPEAL_DIALOG_SHOWN)) {
            bInitializedBeforeRotation = true;

            View view = mApprovalDialog.getCustomView();
            if (view != null) {
                ImageView imageViewAppeal = (ImageView) view.findViewById(R.id.imageViewApproval);
                if (imageViewAppeal != null)
                    imageViewAppeal.setImageURI(mUriPhotoApproval);
            }
            mApprovalDialog.show();
        }

        Globals.__log(LOG_TAG, getString(R.string.log_state_restored));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        if( mCurrentLocation != null )
            outState.putParcelable(Globals.PARCELABLE_LOCATION, mCurrentLocation);

        TextView txtRideCode = (TextView) findViewById(R.id.txtRideCode);
        if( txtRideCode == null )
            return;

        String rideCode = txtRideCode.getText().toString();
        if( !rideCode.isEmpty() ) {

            outState.putString(Globals.PARCELABLE_KEY_RIDE_CODE, rideCode);
            outState.putBoolean(Globals.PARCELABLE_KEY_RIDE_CODE_UPLOADED, mRideCodeUploaded.get()); // isRideCodeUploaded());
            outState.putParcelableArrayList(Globals.PARCELABLE_KEY_PASSENGERS, mPassengers);

            outState.putParcelable(Globals.PARCELABLE_KEY_CURRENT_RIDE, mCurrentRide);

            outState.putBoolean(Globals.PARCELABLE_KEY_CABIN_PICTURES_BUTTON_SHOWN, mCabinPictureButtonShown);

            outState.putSerializable(Globals.PARCELABLE_KEY_PASSENGERS_FACE_IDS, Globals.get_PassengerFaces());

            if( mUriPhotoApproval != null)
                outState.putString(Globals.PARCELABLE_KEY_APPROVAL_PHOTO_URI, mUriPhotoApproval.toString());

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
                        Globals.__log(LOG_TAG, String.format(Locale.getDefault(),
                                        "onSaveInstanceState: Passenger FAB %d is not found", i));
                    }

                }
            }
            catch(Exception ex){
                Globals.__logException(ex);
            }

            if( mApprovalDialog.isShowing() ) {
                outState.putBoolean(Globals.PARCELABLE_KEY_APPEAL_DIALOG_SHOWN, true);
            }
        }

        Globals.__log(LOG_TAG, getString(R.string.log_state_saved));

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
                                public boolean canSwipeRight(int position) {
                                    return true;
                                }

                                @Override
                                public boolean canSwipeLeft(int position) {
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

        TextView vEmptyText = (TextView)findViewById(R.id.empty_view);
        String txt = String.format(getString(R.string.no_passengers_placeholder),
                                   Globals.REQUIRED_PASSENGERS_NUMBER);
        vEmptyText.setText(txt);

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
//        if( mImageTransmit != null ) {
//            mImageTransmit.setVisibility(View.VISIBLE);
//            AnimationDrawable animationDrawable = (AnimationDrawable) mImageTransmit.getDrawable();
//            animationDrawable.start();
//        }

        mTextSwitcher = (TextSwitcher) findViewById(R.id.monitor_text_switcher);
        Animation in = AnimationUtils.loadAnimation(this, R.anim.push_up_in);
        Animation out = AnimationUtils.loadAnimation(this, R.anim.push_up_out);
        mTextSwitcher.setInAnimation(in);
        mTextSwitcher.setOutAnimation(out);
        // Set the initial text without an animation
        String currentMonitorStatus = getString(R.string.geofence_outside_title);
        mTextSwitcher.setCurrentText(currentMonitorStatus);

        Globals.setMonitorStatus(getString(R.string.geofence_outside_title));

        if( Globals.REQUIRED_PASSENGERS_NUMBER == 3 ) {
            View view = findViewById(R.id.passenger4);
            if( view != null )
                view.setVisibility(View.GONE);
        }
    }

    private void startTransmitAnimation() {
        if( mImageTransmit != null ) {
            mImageTransmit.setVisibility(View.VISIBLE);
            AnimationDrawable animationDrawable = (AnimationDrawable) mImageTransmit.getDrawable();
            animationDrawable.start();
        }
    }

    private void stopTransmitAnimation() {
        if( mImageTransmit != null ) {
            mImageTransmit.setVisibility(View.VISIBLE);
            AnimationDrawable animationDrawable = (AnimationDrawable) mImageTransmit.getDrawable();
            animationDrawable.stop();
        }
    }

    private void btInit() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String bluetoothOriginalName = mBluetoothAdapter.getName();
        prefs.edit().putString(bluetoothOriginalName, "btOriginalName").apply();

        User currentUser = User.load(this);
        String btDeviceName = currentUser.getRegistrationId() + Globals.BT_DELIMITER + mRideCode;
        if( mBluetoothAdapter.setName(btDeviceName) ) {
            Globals.__log(LOG_TAG, "Device name has been changed to " + btDeviceName);
        } else {
            Globals.__log(LOG_TAG, "Device name has not been changed");
        }

        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        }
    }

    private void btRestore() {

        try {
            if( Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2 ) {
                _disableBluetoothDiscoverability();
            }
            else {
                enableBluetoothDiscoverability(1); // 1 actually means 'enable for 1 sec.', i.e. disable
            }
        } catch (Exception e) {
            Globals.__logException(e);
        }

        try {
            unregisterReceiver(mBtReceiver);
        } catch( IllegalArgumentException e){
            Globals.__log(LOG_TAG, "Safely dismiss unregisterReceiver");
        }


        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String bluetoothOriginalName = prefs.getString("btOriginalName", "");

        if( mBluetoothAdapter != null )
            mBluetoothAdapter.setName(bluetoothOriginalName);
    }

    private void btStartAdvertise() {

        try {
            // Register the BroadcastReceiver
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
            registerReceiver(mBtReceiver, filter); // Don't forget to unregister during onDestroy
        } catch(IllegalArgumentException e) {
            // There is no API to check if a receiver is registered.
            // When trying to register it for more than first time, the IllegalArgumentException
            // is raised. Here this exception may be safely ignored.
            Globals.__logException(e);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int discoverableDuration = prefs.getInt(Globals.PREF_DISCOVERABLE_DURATION,
                                                Globals.PREF_DISCOVERABLE_DURATION_DEFAULT);

        if( Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2 ) {
            try {

                int nScanMode = mBluetoothAdapter.getScanMode();
                Globals.__log(LOG_TAG, String.format(Locale.getDefault(), "Scan mode is %d", nScanMode));

                _enableBluetoothDiscoverability(discoverableDuration);

                nScanMode = mBluetoothAdapter.getScanMode();
                Globals.__log(LOG_TAG, String.format(Locale.getDefault(), "Now scan mode is %d", nScanMode));
            }
            catch (Exception e){

                Globals.__logException(e);
                enableBluetoothDiscoverability(discoverableDuration);
            }
        } else {
            enableBluetoothDiscoverability(discoverableDuration);
        }
    }

    private void _disableBluetoothDiscoverability()
            throws NoSuchMethodException,
            IllegalAccessException,
            InvocationTargetException {

        if( mBluetoothAdapter != null ) {
            Method method = mBluetoothAdapter.getClass().getMethod("setScanMode", int.class, int.class);
            method.invoke(mBluetoothAdapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE, 1);
            Globals.__log(LOG_TAG, "setScanMode() invoked successfully");
        }
    }

    private void _enableBluetoothDiscoverability(int duration)
            throws NoSuchMethodException,
                   IllegalAccessException,
                   InvocationTargetException {

        if( mBluetoothAdapter != null ) {
            Method method = mBluetoothAdapter.getClass().getMethod("setScanMode", int.class, int.class);
            method.invoke(mBluetoothAdapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE,
                    duration);
            Globals.__log(LOG_TAG, "setScanMode() invoked successfully");
        }
    }

    private void enableBluetoothDiscoverability(int duration){

        Intent discoverableIntent = new
                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);

        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, duration);
        startActivityForResult(discoverableIntent, BT_REQUEST_DISCOVERABLE_CODE);
    }

    public boolean isPassengerJoined(@NonNull User passenger) {
        return mPassengers.contains(passenger);
    }

    @WorkerThread
    public void addPassenger(User passenger, Boolean addEvenAlreadyJoined) {

        if( addEvenAlreadyJoined || !mPassengers.contains(passenger) ) {
            mPassengers.add(passenger);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mPassengersAdapter.notifyDataSetChanged();

                    View vEmptyText = findViewById(R.id.empty_view);
                    vEmptyText.setVisibility(View.GONE);
                    mEmptyTextShown = false;

                    if( mPassengers.size() >= Globals.REQUIRED_PASSENGERS_NUMBER ) {
                        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.submit_ride_button);
                        if( fab != null ) {
                            fab.setVisibility(View.VISIBLE);
                            mSubmitButtonShown = true;
                        }
                    }
                }
            });
        }
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

            sendToValidateManually(getmEmojiID());

//            try {
//
//                View view = mApprovalDialog.getCustomView();
//                if( view == null)
//                    return;
//
//                ImageView imageViewAppeal =  (ImageView)view.findViewById(R.id.imageViewApproval);
//                if( imageViewAppeal != null ) {
//
//                    Drawable drawable = imageViewAppeal.getDrawable();
//                    if( drawable != null ) {
//                        ((BitmapDrawable) drawable).getBitmap().recycle();
//                    }
//
//                    // Downsample the image to consume less memory
//                    BitmapFactory.Options options = new BitmapFactory.Options();
//                    options.inSampleSize = 2;
//                    Bitmap bitmap = BitmapFactory.decodeFile(mUriPhotoApproval.getPath(), options);
//                    imageViewAppeal.setImageBitmap(bitmap);
//                }
//
//                mApprovalDialog.show();
//
//            } catch (Exception e) {
//                Globals.__logException(e);
//            }

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
        }
    }

    //
    // Implementations of WifiUtil.IPeersChangedListener
    //
    @Override
    public void addDeviceUser(final WifiP2pDeviceUser device) {

    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    @Override
    public void onMapReady(GoogleMap googleMap) {

        mGoogleMap = googleMap;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        Globals.LOCATION_PERMISSION_REQUEST);
            }

        } else {
            mGoogleMap.setMyLocationEnabled(true);
        }

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
    @MainThread
    @CallSuper
    protected void onStart() {
        super.onStart();
    }

    @Override
    @MainThread
    @CallSuper
    @RequiresPermission(anyOf = {
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION})
    public void onResume() {
        super.onResume();

        try {

            if( !isLocationEnabled(this) )
                throw new SecurityException();

            // Global flag 'inGeofenceArea' is updated inside getGFenceForLocation()
            String msg = getGFenceForLocation(mCurrentLocation);
            mTextSwitcher.setCurrentText(msg);

        } catch (SecurityException ex) {

            // Returns true if app has requested this permission previously
            // and the user denied the request
            if( ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)) {

                mTextSwitcher.setCurrentText(getString(R.string.permission_location_denied));
                Globals.__log(LOG_TAG, getString(R.string.permission_location_denied));

            } else {

                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        Globals.LOCATION_PERMISSION_REQUEST);

                // to be continued on onRequestPermissionsResult() in permissionsHandler's activity
            }
        }

        try {

            if (!isConnectedToNetwork()) {
                TextView txtRideCodeLabel = (TextView) findViewById(R.id.code_label_caption);
                if( txtRideCodeLabel != null )
                    txtRideCodeLabel.setText("");
                mOfflineDialog.show();
            } else {
                if (mOfflineDialog != null && mOfflineDialog.isShowing()) {
                    mOfflineDialog.dismiss();
                }

                //setupNetwork();

            }
        }catch(Exception ex) {
            Globals.__logException(ex);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    @MainThread
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults){

        try {

            switch( requestCode ) {

                case Globals.LOCATION_PERMISSION_REQUEST: {

                    if(  grantResults.length > 0
                            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                        if( mGoogleMap != null )
                            mGoogleMap.setMyLocationEnabled(true);

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

                case Globals.CAMERA_PERMISSION_REQUEST : {
                    if (grantResults.length > 0
                            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        takePictureWithIntent();
                    }
                }
                break;
            }

        } catch (Exception e) {
            Globals.__logException(e);

        }

    }

    @Override
    @MainThread
    @CallSuper
    public void onPause() {

        if( mP2pPreparer != null ) {

            mP2pPreparer.undo(new Runnable() {
                @Override
                public void run() {
                    if( mP2pConversator != null )
                        mP2pConversator.stopConversation();
                }
            });
        }

        super.onPause();
    }

    @Override
    @MainThread
    @CallSuper
    protected void onDestroy() {

        if( !isChangingConfigurations() ) // Do not cancel BT transmission
                                          // when display orientation changed
            btRestore();

        Globals.setDriverActivity(null);

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
                Globals.__logException(t);
            }

        }, new UiThreadExecutor());
    }

    //
    // Implementation of LocationListener
    //
    @Override
    public void onLocationChanged(Location location) {

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
                if( Fabric.isInitialized() )
                    Answers.getInstance().logCustom(ce);

                FirebaseAnalytics firebaseAnalytics =
                        FirebaseAnalytics.getInstance(getApplicationContext());
                Bundle params = new Bundle();
                params.putString("User", getUser().getFullName());
                params.putString("RideCode", mRideCode);
                firebaseAnalytics.logEvent(getString(R.string.ride_started_answers_name), params);

                boolean bPictureRequired = mCurrentRide.isPictureRequired();
                if( bPictureRequired ) {
                    Globals.__log(LOG_TAG, "Picture required");

                    if( Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2 ) {
                        btRestore();
                    }
                    stopTransmitAnimation();

                    if( Globals.APPLY_CHALLENGE ) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                try {
                                    PictureChallengePromptFragmentDialog dialogFragment =
                                            new PictureChallengePromptFragmentDialog();
                                    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                                    //dialogFragment.show(getSupportFragmentManager(), "picturePromptDialog");
                                    dialogFragment.show(ft, "picturePromptDialog");
                                } catch(Exception e){
                                    Globals.__logException(e);
                                }
                            }
                        });
                    } else {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                try {

                                    PicturePromptFragmentDialog dialogFragment
                                            = new PicturePromptFragmentDialog();
                                    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                                    //dialogFragment.show(getSupportFragmentManager(), "picturePromptDialog");
                                    dialogFragment.show(ft, "picturePromptDialog");
                                } catch (Exception e) {
                                    Globals.__logException(e);
                                }
                            }
                        });
                    }
                }
                else
                    Globals.__log(LOG_TAG, "Picture IS NOT required");
            }

            @Override
            public void onFailure(final Throwable t) {

                Globals.__logException(t);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(DriverRoleActivity.this,
                                t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });

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
                            Ride _ride = ridesTable.insert(ride).get();
                            Globals.__log(LOG_TAG, String.format("Ride uploaded with id: %s" , _ride.id));
                            return _ride;

                        } catch(ExecutionException | InterruptedException e) {
                            Globals.__logException(e);
                            throw e;
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
        ride.setDriverId(getUser().getRegistrationId());
        ride.setCreated(new Date());
        ride.setSmartMode(false);

        return ride;
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

    private void startAdvertise(final P2pConversator.IPeersChangedListener peersListener,
                                final String userID,
                                final String rideCode,
                                final String userName) {



        mP2pPreparer = new P2pPreparer(this);
        mP2pPreparer.prepare(new P2pPreparer.P2pPreparerListener() {
            @Override
            public void prepared() {
                Map<String, String> record = new HashMap<>();
                record.put(Globals.TXTRECORD_PROP_PORT, Globals.SERVER_PORT);
                if( !rideCode.isEmpty() )
                    record.put(Globals.TXTRECORD_PROP_RIDECODE, rideCode);
                record.put(Globals.TXTRECORD_PROP_USERID, userID);
                record.put(Globals.TXTRECORD_PROP_USERNAME, userName);

                mP2pConversator = new P2pConversator(DriverRoleActivity.this,
                        mP2pPreparer,
                        getHandler());
                mP2pConversator.startConversation(record,
                        null); // This param is null because Driver is not interested in peers findings

            }

            @Override
            public void interrupted() {

            }
        });
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

        AnimationDrawable animationDrawable = (AnimationDrawable) mImageTransmit.getDrawable();
        if( actionIntent.isEmpty() ) {
            animationDrawable.start();
        } else {
            animationDrawable.stop();
        }

    }

    public void onSubmitRide(View view) {

        boolean bRequestApprovalBySefies = false;

        if( bRequestApprovalBySefies )
            onSubmitRidePics(null);
        else {

            // Only allow no-fee request from monitored area
            if( !Globals.isInGeofenceArea() ) {

                new MaterialDialog.Builder(this)
                        .title(R.string.geofence_outside_title)
                        .content(R.string.geofence_outside)
                        .positiveText(R.string.geofence_positive_answer)
                        .negativeText(R.string.geofence_negative_answer)
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog){
                                Globals.setRemindGeofenceEntrance();
                            }
                        })
                        .show();

                return;
            }

            if( mCurrentRide == null ) {
                View v = findViewById(R.id.passenger_snackbar);
                if( v != null )
                    Snackbar.make(v, R.string.ride_upload_failed, Snackbar.LENGTH_LONG)
                            .show();
            } else {
                mCurrentRide.setApproved(Globals.RIDE_STATUS.APPROVED.ordinal());
                new UpdateCurrentRide().execute();
            }
        }
    }

    public void onSubmitRidePics(View v){
        cancelNotification();

        // Only allow no-fee request from monitored area
        if( !Globals.isInGeofenceArea() ) {

            new MaterialDialog.Builder(this)
                    .title(R.string.geofence_outside_title)
                    .content(R.string.geofence_outside)
                    .positiveText(R.string.geofence_positive_answer)
                    .negativeText(R.string.geofence_negative_answer)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog){
                            Globals.setRemindGeofenceEntrance();
                        }
                    })
                    .show();

            return;
        }

        // Process the Faces with Oxford FaceAPI
        // Will be continued on finish() from IPictureURLUpdater
        new faceapiUtils(this).execute();
    }

    //
    // Implementation of IUploader
    //
    @Override
    public void update(String url) {
    }

    @Override
    public void finished(int task_tag, boolean success) {

        switch( task_tag ) {
            case Globals.FACE_VERIFY_TASK_TAG: {

                Globals.verificationMat.loadIdentity(); // restore Matrix

                // Upload pictures as joins
                new AsyncTask<Void, Void, Void>() {

                    Exception mEx;

                    @Override
                    protected void onPreExecute() {

                    }

                    @Override
                    protected Void doInBackground(Void... voids) {

                        MobileServiceTable<Join> joinsTable = Globals.getMobileServiceClient()
                                                                    .getTable("joins", Join.class);

                        try {
                            for(Map.Entry<Integer, PassengerFace> entry : Globals.get_PassengerFaces().entrySet()) {

                                PassengerFace pf = entry.getValue();

                                Join _join = new Join();
                                _join.setWhenJoined(new Date());
                                String pictureURI = pf.getPictureUrl();
                                if (pictureURI != null && !pictureURI.isEmpty())
                                    _join.setPictureURL(pictureURI);

                                String faceId = pf.getFaceId().toString();
                                if (!faceId.isEmpty())
                                    _join.setFaceId(faceId);
                                _join.setRideCode(mCurrentRide.getRideCode());

                                // AndroidId is meaningless in this situation: it's only a picture of passenger
                                //_join.setDeviceId(android_id);

                                try {
                                    Location loc = mCurrentLocation;
                                    if (loc != null) {
                                        _join.setLat((float) loc.getLatitude());
                                        _join.setLon((float) loc.getLongitude());
                                    }
                                } catch (Exception e) {
                                    Globals.__logException(e);
                                }

                                String msg = String.format("Inserting join for user with FaceID %s", faceId);
                                Globals.__log(LOG_TAG, msg);

                                joinsTable.insert(_join).get();
                            }

                        } catch (Exception e) { // ExecutionException | InterruptedException ex ) {
                            mEx = e;

                            Globals.__logException(e);
                        }

                        return null;
                    }
                }.execute();

                if (success) {
                    mCurrentRide.setApproved(Globals.RIDE_STATUS.APPROVED_BY_SELFY.ordinal());
                    new UpdateCurrentRide().execute();
                } else {

                    mCurrentRide.setApproved(Globals.RIDE_STATUS.DENIED.ordinal());

                    new AsyncTask<Void, Void, Void>() {

                        Exception mEx;

                        @Override
                        protected Void doInBackground(Void... args) {

                            try {
                                String currentGeoFenceName = Globals.get_currentGeoFenceName();
                                mCurrentRide.setGFenceName(currentGeoFenceName);
                                mRidesTable.update(mCurrentRide).get();
                            } catch (InterruptedException | ExecutionException e) {
                                mEx = e;
                                Globals.__logException(e);
                            }

                            return null;

                        }

                        @Override
                        protected void onPostExecute(Void result) {

                            CustomEvent requestEvent = new CustomEvent(getString(R.string.no_fee_answer_name));
                            requestEvent.putCustomAttribute("User", getUser().getFullName());

                            requestEvent.putCustomAttribute(getString(R.string.answer_approved_attribute), 0);
                            if( Fabric.isInitialized() )
                                Answers.getInstance().logCustom(requestEvent);
                        }
                    }.execute();

                    onAppealCamera();
                }
            }
            break;

            case Globals.APPROVAL_UPLOAD_TASK_TAG: {
                finish();
            }
            break;
        }
    }

    private Bitmap convertToBitmap(Drawable drawable, int widthPixels, int heighPixels) {
        Bitmap mutableBitmap = Bitmap.createBitmap(widthPixels, heighPixels, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mutableBitmap);
        drawable.setBounds(0, 0, widthPixels, heighPixels);
        drawable.draw(canvas);

        return mutableBitmap;
    }

    public void sendToValidateManually(final int emojiId){

        SendingPictureFragmentDialog dialogFragment = new SendingPictureFragmentDialog();
        dialogFragment.show(getSupportFragmentManager(), "pictureSendingDialog");

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int sb2value = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, sb2value / 2, 0);

        final File photoFile = new File(mUriPhotoApproval.getPath());

        Callable<Void> sendToManualValidationTask = new Callable<Void>() {
            @Override
            public Void call() throws Exception {

                // 1 - Upload blog
                CloudStorageAccount storageAccount = CloudStorageAccount.parse(Globals.AZURE_STORAGE_CONNECTION_STRING);
                CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
                CloudBlobContainer container = blobClient.getContainerReference("pictures");

                CloudBlockBlob blob = container.getBlockBlobReference(photoFile.getName());
                //AccessCondition ac = new AccessCondition();
                blob.upload(new FileInputStream(photoFile), photoFile.length());

                java.net.URI publishedUri = blob.getQualifiedUri();

                Approval approval = new Approval();
                approval.setPictureUrl(publishedUri.toString());
                approval.setRideId(mCurrentRide.id);
                approval.setDriverId(getUser().getRegistrationId());
                if( emojiId != 0 )
                    approval.setEmojiId(emojiId);

                // 2 - insert new approval
                MobileServiceTable<Approval> wamsApprovalsTable =
                        Globals.getMobileServiceClient().getTable("approvals", Approval.class);
                ListenableFuture<Approval> insertFuture = wamsApprovalsTable.insert(approval);
                Futures.addCallback(insertFuture, new FutureCallback<Approval>(){
                    @Override
                    public void onSuccess(Approval approval) {

                        if( approval == null ) {
                            Globals.__log(LOG_TAG, getString(R.string.approval_add_failed) + mCurrentRide.id);
                            return;
                        }

                        Globals.__log(LOG_TAG, String.format(Locale.getDefault(),
                                                            getString(R.string.approval_add_format),
                                                            mCurrentRide.id, approval.Id));

                        // 3 - update ride
                        mCurrentRide.setApproved(Globals.RIDE_STATUS.BE_VALIDATED_MANUALLY.ordinal());
                        ListenableFuture<Ride> _rideFuture = mRidesTable.update(mCurrentRide);
                        Futures.addCallback(_rideFuture, new FutureCallback<Ride>() {
                            @Override
                            public void onSuccess(Ride result) {

                                // 4 - create new crashlytics event
                                CustomEvent requestEvent = new CustomEvent(getString(R.string.approval_answer_name));
                                requestEvent.putCustomAttribute("User", getUser().getRegistrationId());

                                if( Fabric.isInitialized() )
                                    Answers.getInstance().logCustom(requestEvent);

                                beepSuccess.start();

                            }

                            @Override
                            public void onFailure(Throwable t) {
                                Globals.__logException(t);
                                beepError.start();
                            }
                        });

                    }
                    @Override
                    public void onFailure(Throwable t) {
                        Globals.__logException(t);
                        beepError.start();
                    }
                });

                return null;
            }
        };

        ExecutorService service = Executors.newFixedThreadPool(1);
        ListeningExecutorService executor = MoreExecutors.listeningDecorator(service);
        executor.submit(sendToManualValidationTask);
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
                            takePictureWithIntent();
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
            Globals.__logException(e);
            // better that catch the exception here would be use handle to send events the activity
        }
    }

    private boolean isDeviceSupportCamera() {
        if (getApplicationContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    /**
     * Returns true if permission was requested.
     * Otherwise = false
     */
    private boolean requestCameraPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if( ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                    != PackageManager.PERMISSION_GRANTED ) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        Globals.CAMERA_PERMISSION_REQUEST);
                return true;
            }
        }

        return false;
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    public void takePictureWithIntent(){

        boolean bFrontCameraFound = false;

        int nCameras = Camera.getNumberOfCameras();

        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();

        for(int camIndex =0; camIndex < nCameras; camIndex++) {
            Camera.getCameraInfo(camIndex, cameraInfo);
            if( cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT ) {
                bFrontCameraFound = true;
                break;
            }
        }

        if( !bFrontCameraFound ) {
            new MaterialDialog.Builder(this)
                    .positiveText(getString(R.string.no_front_camera))
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            finish();
                        }
                    })
                    .show();
        }


        try {

            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

                try {
                    mUriPhotoApproval = createImageFile();
                } catch (IOException e) {
                    Globals.__logException(e);
                }

                if (mUriPhotoApproval != null) {

                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mUriPhotoApproval);
                    takePictureIntent.putExtra("android.intent.extras.CAMERA_FACING",
                            Camera.CameraInfo.CAMERA_FACING_FRONT);
                    takePictureIntent.putExtra(MediaStore.EXTRA_SCREEN_ORIENTATION,
                            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            }
        } catch(Exception e) {
            Globals.__logException(e);
        }
    }

    private Uri createImageFile() throws IOException {

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String photoFileName = "ApprovalJPEG_" + timeStamp + "_";

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
                        showSubmitButton();
                    }
                });
            }


        } catch(Exception e) {
            Globals.__logException(e);
        }
    }

    @UiThread
    private void showSubmitButton() {

        FloatingActionButton fab = (FloatingActionButton)findViewById(R.id.submit_ride_button);
        Context ctx =  getApplicationContext();
        if( fab != null) {
            fab.setVisibility(View.VISIBLE);
            fab.setImageDrawable(ContextCompat.getDrawable(ctx, R.drawable.ic_action_done));

            mTextSwitcher.setText(getString(R.string.instruction_can_submit_no_fee));

            mSubmitButtonShown = true;
        }
    }

    private void showCabinView() {

        findViewById(R.id.drive_internal_layout).setVisibility(View.GONE);
        findViewById(R.id.driver_status_layout).setVisibility(View.GONE);
        findViewById(R.id.status_strip).setVisibility(View.GONE);

        findViewById(R.id.cabin_background_layout).setVisibility(View.VISIBLE);

        final ImageView cabinImageView = (ImageView)findViewById(R.id.centerImage);
        ViewTreeObserver observer = cabinImageView.getViewTreeObserver();
        if( observer.isAlive() ) {
            observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    cabinImageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    loadBitmap(R.drawable.cabin_portrait, cabinImageView);
                }
            });
        }

        mCabinShown = true;
    }

    public void hideCabinView(View v){

        findViewById(R.id.cabin_background_layout).setVisibility(View.GONE);

        findViewById(R.id.status_strip).setVisibility(View.VISIBLE);
        findViewById(R.id.driver_status_layout).setVisibility(View.VISIBLE);
        findViewById(R.id.drive_internal_layout).setVisibility(View.VISIBLE);

        mCabinShown = false;
    }

    private Runnable thanksRunnable = new Runnable() {
        @Override
        public void run() {

            try {

                new MaterialDialog.Builder(DriverRoleActivity.this)
                        .title(R.string.thanks)
                        .content(R.string.nofee_request_accepted)
                        .iconRes(R.drawable.ic_info)
                        .cancelable(false)
                        .positiveText(android.R.string.ok)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                finish();
                            }
                        })
                        .show();
            } catch(MaterialDialog.DialogException e) {
                // Safely dismiss the situation when
                // an Activity is not yet created or it's hidden
                Globals.__logException(e);
            }
        }
    };

    class UpdateCurrentRide extends AsyncTask<Void, Void, Void> {

        Exception mEx;
        LoadToast lt;

        @Override
        protected void onPreExecute() {
            lt = new LoadToast(DriverRoleActivity.this);
            lt.setText(getString(R.string.processing));
            Display display = getWindow().getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            lt.setTranslationY(size.y / 2);
            lt.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                String currentGeoFenceName = Globals.get_currentGeoFenceName();
                mCurrentRide.setGFenceName(currentGeoFenceName);
                mRidesTable.update(mCurrentRide).get();
        } catch (InterruptedException | ExecutionException e) {
            mEx = e;
            Globals.__logException(e);
        }
            return null;
        }

        @Override
        protected void onPostExecute(Void result){

            CustomEvent requestEvent = new CustomEvent(getString(R.string.no_fee_answer_name));
            requestEvent.putCustomAttribute("User", getUser().getFullName());

            if( mEx != null ) {

                View v = findViewById(R.id.passenger_snackbar);
                if( v != null )
                    Snackbar.make(v, mEx.getMessage(), Snackbar.LENGTH_LONG)
                            .show();

                lt.error();
                beepError.start();
            }
            else {

                lt.success();
                beepSuccess.start();

                getHandler().postDelayed(thanksRunnable, 1500);

            }

            requestEvent.putCustomAttribute(getString(R.string.answer_approved_attribute), 1);
            if( Fabric.isInitialized() )
                Answers.getInstance().logCustom(requestEvent);
        }
    }

    // DialogFragments

    public static class PictureChallengePromptFragmentDialog extends DialogFragment {

        int _mEmojiId;

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState){

            final DriverRoleActivity activity = ((DriverRoleActivity)getActivity());

            View customDialog = activity.getLayoutInflater().inflate(R.layout.dialog_picture_prompt,null);

            if( savedInstanceState != null ) {
                _mEmojiId = savedInstanceState.getInt(Globals.PARCELABLE_KEY_EMOJI_ID);
            } else {
                _mEmojiId = new Random().nextInt(Globals.NUM_OF_EMOJIS)
                        // nextInt() gets number between 0 (inclusive and specified value (exclusive)
                        + 1;
            }

            activity.setEmojiId(_mEmojiId);

            String uri = "@drawable/emoji_" + Integer.toString(_mEmojiId);
            int imageResource = getResources().getIdentifier(uri, "id", activity.getPackageName());
            ImageView emojiImageView = (ImageView)customDialog.findViewById(R.id.appeal_emoji);
            emojiImageView.setImageResource(imageResource);

            return new MaterialDialog.Builder(activity)
                    .title(R.string.picture_required_dialog_title)
                    .iconRes(R.drawable.ic_camera_blue)
                    .customView(customDialog, false) // do not wrap in scroll
                    .positiveText(android.R.string.ok)
                    .cancelable(false)
                    .autoDismiss(true)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            if( !activity.requestCameraPermissions() )
                                activity.takePictureWithIntent();
                        }
                    })
                    .show();
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putInt(Globals.PARCELABLE_KEY_EMOJI_ID, _mEmojiId);
            super.onSaveInstanceState(outState);
        }

    }

    public static class PicturePromptFragmentDialog extends DialogFragment {
        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            final DriverRoleActivity activity = ((DriverRoleActivity)getActivity());

            return  new MaterialDialog.Builder(activity)
                    .title(R.string.picture_required_dialog_title)
                    .content(R.string.prompt_take_picture)
                    .iconRes(R.drawable.ic_camera_blue)
                    .positiveText(android.R.string.ok)
                    .negativeText(android.R.string.no)
                    .cancelable(false)
                    .autoDismiss(true)
//                    .onNegative(new MaterialDialog.SingleButtonCallback() {
//                        @Override
//                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
//                            dialog.dismiss();
//                            activity.finish();
//                        }
//                    })
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            if( !activity.requestCameraPermissions() )
                                activity.takePictureWithIntent();
                        }
                    })
                    .show();
        }
    }

    public static class SendingPictureFragmentDialog extends DialogFragment {
        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final DriverRoleActivity activity = ((DriverRoleActivity)getActivity());

            return  new MaterialDialog.Builder(activity)
                    .title(getString(R.string.approval_sending_title))
                    .content(getString(R.string.approval_send_future))
                    .iconRes(R.drawable.selfie_office)
                    .positiveText(android.R.string.ok)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog,
                                            @NonNull DialogAction which) {
                            activity.finish();
                        }
                    })
                    .show();
        }
    }
}
