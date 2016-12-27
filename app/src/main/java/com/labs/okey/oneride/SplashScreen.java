package com.labs.okey.oneride;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.view.View;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.LoggingBehavior;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.labs.okey.oneride.gcm.GCMHandler;
import com.labs.okey.oneride.utils.Globals;
import com.labs.okey.oneride.utils.wamsUtils;
import com.microsoft.windowsazure.notifications.NotificationsManager;

import java.text.DateFormat;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Oleg Kleiman
 * created 15-Jul-16.
 */
/**
 * Full-screen activity
 */
public class SplashScreen extends AppCompatActivity {

    private final String        LOG_TAG = getClass().getSimpleName();

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };

    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }

        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    private Handler mHandler =  new Handler(Looper.getMainLooper());
    private AccessTokenTracker mFbAccessTokenTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash_screen);

        Globals.initMobileServices(getApplicationContext());

        // Setup up Crashlytics as app monitor
        Globals.initializeMonitor(this, this.getApplication());

        Globals.initializeVolley(getApplicationContext());

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Globals.myRides_update_required = sharedPrefs.getBoolean(Globals.UPDATE_MYRIDES_REQUIRED, false);

        mVisible = true;
        mContentView = findViewById(R.id.fullscreen_content);

        ExecutorService service = Executors.newFixedThreadPool(1);
        ListeningExecutorService executor = MoreExecutors.listeningDecorator(service);

        Callable<Boolean> validationTask = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                //return isSimPresent() && isRegistrationValid();
                return isRegistrationValid();
            }
        };

        ListenableFuture<Boolean> regValidationFuture = executor.submit(validationTask);
        Futures.addCallback(regValidationFuture, new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(final Boolean result) {

                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        Intent intent;
                        if (result) {

                            NotificationsManager.handleNotifications(getApplicationContext(),
                                    Globals.SENDER_ID,
                                    GCMHandler.class);
                            registerWithNotificationHubs();

                            intent = new Intent(SplashScreen.this, MainActivity.class);
                            Globals.__log(LOG_TAG, getString(R.string.log_validation_succeeded));
                        } else {
                            intent = new Intent(SplashScreen.this, RegisterActivity.class);
                            Globals.__log(LOG_TAG, getString(R.string.log_validation_failed));
                        }

                        startActivity(intent);
                        SplashScreen.this.finish();

                    }
                }, 2000);
            }

            @Override
            public void onFailure(Throwable t) {
                Globals.__logException(t);
            }
        });

    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);

        if (resultCode != ConnectionResult.SUCCESS) {

            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, Globals.PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Globals.__log(LOG_TAG, getString(R.string.no_playservices));
            }

            return false;
        } else
            return true;
    }

    public void registerWithNotificationHubs(){

        if (checkPlayServices()) {
            // Start IntentService to register this application with FCM.
            Intent intent = new Intent(this, AzureRegistrationIntentService.class);
            startService(intent);
        }
    }

    private boolean isSimPresent() {
        TelephonyManager telMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        return !(TelephonyManager.SIM_STATE_ABSENT == telMgr.getSimState()) ;
    }

    private Boolean isRegistrationValid(){

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String userRegistrationId = sharedPrefs.getString(Globals.USERIDPREF, "");
        if( userRegistrationId.isEmpty() )
            return false;

        String provider = sharedPrefs.getString(Globals.REG_PROVIDER_PREF, "");
        if( provider.isEmpty() )
            return false;

        final CountDownLatch latch = new CountDownLatch(1);
        final Boolean[] bRes = new Boolean[1];

        if( provider.equalsIgnoreCase(Globals.FB_PROVIDER)) {

            FacebookSdk.addLoggingBehavior(LoggingBehavior.REQUESTS);

            FacebookSdk.sdkInitialize(getApplicationContext(), new FacebookSdk.InitializeCallback() {
                @Override
                public void onInitialized() {
                    AccessToken fbAccessToken = AccessToken.getCurrentAccessToken();
                    if (fbAccessToken == null) {
                        bRes[0] = false;
                    } else {

                        if (fbAccessToken.isExpired()) {

                            mFbAccessTokenTracker = new AccessTokenTracker() {
                                @Override
                                protected void onCurrentAccessTokenChanged(AccessToken oldAccessToken,
                                                                           AccessToken currentAccessToken) {
                                    Globals.__log(LOG_TAG, getString(R.string.log_token_changed));
                                    AccessToken.setCurrentAccessToken(currentAccessToken);
                                }
                            };

                            final CountDownLatch refreshLatch = new CountDownLatch(1);

                            AccessToken.refreshCurrentAccessTokenAsync();

//                            AccessToken.refreshCurrentAccessTokenAsync(new AccessToken.AccessTokenRefreshCallback() {
//                                @Override
//                                public void OnTokenRefreshed(AccessToken accessToken) {
//                                    AccessToken.setCurrentAccessToken(accessToken);
//                                    bRes[0] = true;
//
//                                    refreshLatch.countDown();
//                                }
//
//                                @Override
//                                public void OnTokenRefreshFailed(FacebookException exception) {
//                                    bRes[0] = false;
//
//                                    refreshLatch.countDown();
//                                }
//
//                            });

//                            try {
//                                refreshLatch.await();
//                            } catch (InterruptedException ex) {
//                                Globals.__logException(ex);
//                            }

                        } else {
                            Globals.initializeTokenTracker(getApplicationContext());
                            bRes[0] = true;
                        }
                    }

                    bRes[0] = true;

                    latch.countDown();
                }
            });

        } else if(provider.equalsIgnoreCase(Globals.TWITTER_PROVIDER) ||
                provider.equalsIgnoreCase(Globals.DIGITS_PROVIDER)) {
            // According to https://dev.twitter.com/oauth/overview/faq ('How long does an access token last?')
            // Twitter (and Digits) tokens are not expired
            bRes[0] = true;
            latch.countDown();
        } else if( provider.equalsIgnoreCase(Globals.MICROSOFT_PROVIDER) ) {
            // If Microsoft Account tokens are expire?
            bRes[0] = true;
            latch.countDown();
        } else if( provider.equalsIgnoreCase(Globals.GOOGLE_PROVIDER) ) {
            // Google tokens are expire in 1 (one) hour
            String token = sharedPrefs.getString(Globals.TOKEN_PREF, "");
            try {
                Boolean _bRes = wamsUtils.isJWTTokenValid(token);
                bRes[0] = _bRes;
            } catch( Exception ex) {
                Globals.__logException(ex);
            }

            latch.countDown();
        } else {
            bRes[0] = false;
            latch.countDown();
        }

        try {

            latch.await();

        } catch (InterruptedException ex) {
            Globals.__logException(ex);
            return false;
        }

        return bRes[0];
    }

    private Boolean validateFbToken() {

        AccessToken fbAccessToken = AccessToken.getCurrentAccessToken();

        if( fbAccessToken == null || fbAccessToken.isExpired()) {
            Globals.__log(LOG_TAG, "FB Token is null or expired");
            AccessToken.refreshCurrentAccessTokenAsync(new AccessToken.AccessTokenRefreshCallback() {
                @Override
                public void OnTokenRefreshed(AccessToken accessToken) {
                    AccessToken.setCurrentAccessToken(accessToken);
                }

                @Override
                public void OnTokenRefreshFailed(FacebookException exception) {

                }
            });
            return false;
        } else {
            DateFormat df = DateFormat.getDateTimeInstance();
            Globals.__log(LOG_TAG, "Token is valid. Will expire at " + df.format(fbAccessToken.getExpires()));

            Globals.initializeTokenTracker(getApplicationContext());

            return true;
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

//    private void toggle() {
//        if (mVisible) {
//            hide();
//        } else {
//            show();
//        }
//    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }
}
