package com.labs.okey.oneride;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.labs.okey.oneride.model.GFCircle;
import com.labs.okey.oneride.model.GeoFence;
import com.labs.okey.oneride.services.GeofenceErrorMessages;
import com.labs.okey.oneride.utils.Globals;
import com.labs.okey.oneride.utils.IInitializeNotifier;
import com.labs.okey.oneride.utils.ITrace;
import com.labs.okey.oneride.utils.wamsUtils;
import com.microsoft.windowsazure.mobileservices.MobileServiceList;
import com.microsoft.windowsazure.mobileservices.table.query.Query;
import com.microsoft.windowsazure.mobileservices.table.sync.MobileServiceSyncTable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Oleg Kleiman
 * created 09-Jun-15.
 */
public class BaseActivityWithGeofences extends BaseActivity
        implements ResultCallback<Status>,
                GoogleApiClient.ConnectionCallbacks
{
    private final String                            LOG_TAG = getClass().getSimpleName();
    private MobileServiceSyncTable<GeoFence>        mGFencesSyncTable;
    private String                                  mCurrentGeoFenceName;
    protected String getCurrentGFenceName() { return mCurrentGeoFenceName; }
    private Boolean                                 isGeoFencesInitialized = false;
    protected Boolean isGeoFencesInitialized() { return isGeoFencesInitialized; }

    private android.location.LocationListener       mLocationListener;
    protected CopyOnWriteArrayList<GFCircle>        mGFCircles = new CopyOnWriteArrayList<GFCircle>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    //
    // Implementation of GoogleApiClient.ConnectionCallbacks
    //

    @Override
    public void onConnected(Bundle bundle) {
        //initGeofencesAPI();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @TargetApi(Build.VERSION_CODES.M)
    @RequiresPermission(anyOf = {
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION})
    protected Location getCurrentLocation(Activity permissionsHandler) throws SecurityException {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, "android.permission.ACCESS_FINE_LOCATION")
                    != PackageManager.PERMISSION_GRANTED )
                // Permission android.permission.ACCESS_FINE_LOCATION includes
                // permission for both NETWORK_PROVIDER and GPS_PROVIDER

                    throw new SecurityException();

        } else {

            try {
                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            } catch (Exception ex) {
                Log.e(LOG_TAG, ex.getMessage());

                if( Crashlytics.getInstance() != null )
                    Crashlytics.logException(ex);

            }
        }

        return null;
    }

    protected void startLocationUpdates(Activity permissionsHandler,
                                        android.location.LocationListener locationListener) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, "android.permission.ACCESS_FINE_LOCATION")
                    != PackageManager.PERMISSION_GRANTED )
                // Permission android.permission.ACCESS_FINE_LOCATION includes
                // permission for both NETWORK_PROVIDER and GPS_PROVIDER

                throw new SecurityException();
        }

        try {
            mLocationListener = locationListener;
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            if( locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) )
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        Globals.LOCATION_UPDATE_MIN_FEQUENCY, 0, locationListener);
        } catch (Exception ex) {
            Log.e(LOG_TAG, ex.getMessage());

            if( Crashlytics.getInstance() != null )
                Crashlytics.logException(ex);
        }

    }

    protected void stopLocationUpdates(android.location.LocationListener locationListener) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, "android.permission.ACCESS_FINE_LOCATION")
                    != PackageManager.PERMISSION_GRANTED)
                throw new SecurityException();
        }

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.removeUpdates(locationListener);
    }

    @Override
    protected void onPause(){

        if( mLocationListener != null )
            stopLocationUpdates(mLocationListener);

        super.onPause();
    }

    protected ListenableFuture<CopyOnWriteArrayList<GFCircle>> _initGeofences() {
        ExecutorService service = Executors.newFixedThreadPool(1);
        ListeningExecutorService executor = MoreExecutors.listeningDecorator(service);

        Callable<MobileServiceList<GeoFence>> getGeoFencesTask =
                new Callable<MobileServiceList<GeoFence>>() {
                @Override
                public MobileServiceList<GeoFence> call() throws Exception {
                    wamsUtils.sync(Globals.getMobileServiceClient(), "geofences");

                    Query pullQuery = Globals.getMobileServiceClient().getTable(GeoFence.class).where();
                    return mGFencesSyncTable.read(pullQuery).get();
                }
        };

        if (mGFencesSyncTable == null)
            mGFencesSyncTable = Globals.getMobileServiceClient().getSyncTable("geofences", GeoFence.class);

        ListenableFuture<MobileServiceList<GeoFence>> future = executor.submit(getGeoFencesTask);
        return Futures.transform(future, toCircles);
    }


    final Function<MobileServiceList<GeoFence>, CopyOnWriteArrayList<GFCircle>> toCircles =
            new Function<MobileServiceList<GeoFence>, CopyOnWriteArrayList<GFCircle>>() {
                @Override
                public CopyOnWriteArrayList<GFCircle> apply(MobileServiceList<GeoFence> gFences){

                    for (GeoFence _gFence : gFences) {
                        if( _gFence.isActive() ) {
                            // About the issues of converting float to double
                            // see here: http://programmingjungle.blogspot.co.il/2013/03/float-to-double-conversion-in-java.html
                            double lat = new BigDecimal(String.valueOf(_gFence.getLat())).doubleValue();
                            double lon = new BigDecimal(String.valueOf(_gFence.getLon())).doubleValue();
                            int radius = Math.round(_gFence.getRadius());

                            GFCircle gfCircle = new GFCircle(lat, lon,
                                    radius,
                                    _gFence.getLabel());
                            mGFCircles.add(gfCircle);

                        }
                    }

                    isGeoFencesInitialized = true;

                    return mGFCircles;
    }};

    protected void initGeofences(final IInitializeNotifier notifier) {

        if (mGFencesSyncTable == null)
            mGFencesSyncTable = Globals.getMobileServiceClient().getSyncTable("geofences", GeoFence.class);

        new AsyncTask<Object, Void, Void>() {

            MobileServiceList<GeoFence> gFences = null;

            @Override
            protected void onPostExecute(Void result){
                if( gFences == null )
                    return;

                for (GeoFence _gFence : gFences) {
                    if( _gFence.isActive() ) {
                        // About the issues of converting float to double
                        // see here: http://programmingjungle.blogspot.co.il/2013/03/float-to-double-conversion-in-java.html
                        double lat = new BigDecimal(String.valueOf(_gFence.getLat())).doubleValue();
                        double lon = new BigDecimal(String.valueOf(_gFence.getLon())).doubleValue();
                        int radius = Math.round(_gFence.getRadius());

                        GFCircle gfCircle = new GFCircle(lat, lon,
                                radius,
                                _gFence.getLabel());
                        mGFCircles.add(gfCircle);

                    }
                }

                isGeoFencesInitialized = true;

                if( notifier != null ) {
                    notifier.initialized(null);
                }

            }

            @Override
            protected Void doInBackground(Object... params) {

                try {

                    wamsUtils.sync(Globals.getMobileServiceClient(), "geofences");

                    Query pullQuery = Globals.getMobileServiceClient().getTable(GeoFence.class).where();
                    gFences = mGFencesSyncTable.read(pullQuery).get();

                } catch (Exception ex) {
                    Log.e(LOG_TAG, ex.getMessage() + " Cause: " + ex.getCause());
                }

                return null;
            }
        }.execute();

    }

    protected String getGFenceForLocation(Location location) {

        String strStatus = getString(R.string.geofence_outside_title);
        if( location == null )
            return strStatus;

        Boolean bInsideGeoFences = false;

        for(GFCircle circle : mGFCircles) {
            float[] res = new float[3];
            Location.distanceBetween(location.getLatitude(),
                                    location.getLongitude(),
                                    circle.getX(),
                                    circle.getY(),
                                    res);
            if (res[0] < circle.getRadius()) {

                bInsideGeoFences = true;

                strStatus = getString(R.string.geofences_inside_title);

                mCurrentGeoFenceName = circle.getTag();
                strStatus += " " + circle.getTag();

                break;
            }
        }

        Globals.setInGeofenceArea(bInsideGeoFences);

        return strStatus;
    }

    protected boolean isAccurate(Location loc){

        return loc.hasAccuracy() && loc.getAccuracy() < Globals.MIN_ACCURACY;

    }

    protected void initGeofencesAPI() {

        final ResultCallback<Status> resultCallback = this;

        if( mGFencesSyncTable == null )
            mGFencesSyncTable = Globals.getMobileServiceClient().getSyncTable("geofences", GeoFence.class);

        new AsyncTask<Object, Void, Void>() {

            @Override
            protected void onPostExecute(Void result){

                Globals.GEOFENCES.clear();

                for (Map.Entry<String, LatLng> entry : Globals.FWY_AREA_LANDMARKS.entrySet()) {

                    Globals.GEOFENCES.add(new Geofence.Builder()
                            .setRequestId(entry.getKey())

                             // Set the circular region of this geofence.
                            .setCircularRegion(
                                    entry.getValue().latitude,
                                    entry.getValue().longitude,
                                    Globals.GEOFENCE_RADIUS_IN_METERS
                            )
                            .setLoiteringDelay(Globals.GEOFENCE_LOITERING_DELAY)
                             //.setNotificationResponsiveness(Globals.GEOFENCE_RESPONSIVENESS)
                             // Set the expiration duration of the geofence. This geofence gets automatically
                             // removed after this period of time.
                            .setExpirationDuration(Globals.GEOFENCE_EXPIRATION_IN_MILLISECONDS)
                             // Set the transition types of interest. Alerts are only generated for these
                             // transition. We track entry and exit transitions here.
                            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                                    Geofence.GEOFENCE_TRANSITION_EXIT |
                                    Geofence.GEOFENCE_TRANSITION_DWELL)
                            .build());
                }

                GeofencingRequest geoFencingRequest = getGeofencingRequest();
                GoogleApiClient googleApiClient = getGoogleApiClient();

                if( geoFencingRequest != null
                        && googleApiClient != null ) {

                    if (googleApiClient.isConnected()) {

                        LocationServices.GeofencingApi.addGeofences(
                                getGoogleApiClient(), // from base activity
                                // The GeofenceRequest object.
                                geoFencingRequest,
                                // A pending intent that that is reused when calling removeGeofences(). This
                                // pending intent is used to generate an intent when a matched geofence
                                // transition is observed.
                                getGeofencePendingIntent()
                        ).setResultCallback(resultCallback); // Result processed in onResult().
                    } else {
                        Log.e(LOG_TAG, "Google API is not connected yet");
                    }
                }

            }

            @Override
            protected Void doInBackground(Object... params) {

                MobileServiceList<GeoFence> gFences = null;
                try {

                    //wamsUtils.sync(getMobileServiceClient(), "gfences");
                    wamsUtils.sync(Globals.getMobileServiceClient(), "geofences");

                    Query pullQuery = Globals.getMobileServiceClient().getTable(GeoFence.class).where();
                    gFences = mGFencesSyncTable.read(pullQuery).get();

                } catch (Exception ex) {
                    Log.e(LOG_TAG, ex.getMessage() + " Cause: " + ex.getCause());
                }

                if( gFences == null )
                    return null;

                try {
                    // After getting landmark coordinates from WAMS,
                    // the steps for dealing with geofences are following:
                    // 1. populate FWY_AREA_LANDMARKS hashmap in Globals
                    // 2. (from this step on, performed in onPostExecute) based on this hashmap, populate GEOFENCES in Globals
                    // 3. create GeofencingRequest request based on GEOFENCES list
                    // 4. define pending intent for geofences transitions
                    // 5. add geofences to Google API service

                    for (GeoFence _gFence : gFences) {

                        if( _gFence.isActive() ) {

                            // About the issues of converting float to double
                            // see here: http://programmingjungle.blogspot.co.il/2013/03/float-to-double-conversion-in-java.html
                            double lat = new BigDecimal(String.valueOf(_gFence.getLat())).doubleValue();
                            double lon = new BigDecimal(String.valueOf(_gFence.getLon())).doubleValue();
                            //int radius = _gFence.getRaius();

                            LatLng latLng = new LatLng(lat, lon);

                            Globals.FWY_AREA_LANDMARKS.put(_gFence.getLabel(), latLng);
                        }
                    }


                } catch (Exception ex) {
                    Log.e(LOG_TAG, ex.getMessage() + " Cause: " + ex.getCause());
                }

                return null;
            }
        }.execute();


    }


    @Override
    protected void onDestroy() {

        try {

            if( getGoogleApiClient().isConnected() ) {

                Globals.setInGeofenceArea(false);
                Globals.setMonitorStatus("");

                LocationServices.GeofencingApi.removeGeofences(
                        getGoogleApiClient(),
                        // This is the same pending intent that was used in addGeofences().
                        getGeofencePendingIntent()
                ).setResultCallback(this); // Result processed in onResult().
            }

        } catch(IllegalStateException ex) { // Nothing special here :
            // it may happen if GoogleApiClient was not connected yet
            if( Crashlytics.getInstance() != null )
                Crashlytics.logException(ex);

            Log.e(LOG_TAG, ex.getMessage());
        }
        super.onDestroy();
    }

    @Nullable
    private GeofencingRequest getGeofencingRequest() {

        try {
            GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

            // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
            // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
            // is already inside that geofence.
            builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);

            // Add the geofences to be monitored by geofencing service.
            builder.addGeofences(Globals.GEOFENCES);

            // Return a GeofencingRequest.
            return builder.build();
        } catch (Exception ex) {
            if( Crashlytics.getInstance() != null ) {
                Crashlytics.logException(ex);
            }

            Log.e(LOG_TAG, ex.getMessage());
            return null;
        }
    }

    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if ( Globals.GeofencePendingIntent != null) {
            return Globals.GeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        return PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Runs when the result of calling addGeofences() and removeGeofences() becomes available.
     * Either method can complete successfully or with an error.
     *
     * Since this activity implements the {@link ResultCallback} interface, we are required to
     * define this method.
     *
     * @param status The Status returned through a PendingIntent when addGeofences() or
     *               removeGeofences() get called.
     */
    public void onResult(Status status) {
        if (status.isSuccess()) {

            if( this instanceof ITrace ) {
                ITrace tracer = (ITrace)this;
                tracer.trace(getString(R.string.geofences_added));
            }

        } else {
            // Get the status code for the error and log it using a user-friendly message.
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    status.getStatusCode());
            Log.e(LOG_TAG, errorMessage);

            if( this instanceof ITrace ) {

                String message = getString(R.string.enable_location_question);
                ITrace tracer = (ITrace)this;
                tracer.alert(errorMessage + "." + message,
                        Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            }
        }
    }

}
