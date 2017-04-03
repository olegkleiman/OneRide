package com.labs.okey.oneride.utils;

import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.renderscript.Matrix4f;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.maps.model.LatLng;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.labs.okey.oneride.DriverRoleActivity;
import com.labs.okey.oneride.model.GlobalSettings;
import com.labs.okey.oneride.model.PassengerFace;
import com.labs.okey.oneride.model.User;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.MobileServiceList;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.twitter.sdk.android.core.TwitterAuthConfig;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;

import io.fabric.sdk.android.Fabric;


/**
 * @author Oleg Kleiman
 * created 22-Aug-15.
 */
public class Globals {

    private static final String LOG_TAG = "OneRide";

    public static long REQUIRED_PASSENGERS_NUMBER = 3;
    public static boolean APPLY_CHALLENGE = false;

//    @IntDef({RIDE_APPROVED, RIDE_NOT_APPROVED, RIDE_WAITING})
//    public static int RIDE_APPROVED = 0;
//    public static int RIDE_NOT_APPROVED = 1;
//    public static int RIDE_WAITING = 2;
//
//    public abstract int getRideStatus();

    public static String userID;
    public static boolean myrides_update_required = true;

    public enum RIDE_STATUS {
        WAIT, // = 0
        APPROVED, // = 1
        APPROVED_BY_SELFY, // = 2
        DENIED, // = 3
        BE_VALIDATED_MANUALLY, // = 4
        BE_VALIDATED_MANUALLY_SELFIE, // = 5
        VALIDATED_MANUALLY // = 6
    }

    ; // use it as casted to int like : Globals.RIDE_STATUS.APPROVED.ordinal())

    public enum LayoutManagerType {
        GRID_LAYOUT_MANAGER,
        LINEAR_LAYOUT_MANAGER
    }

    public final static String PUSH_NOTIFICATION_JOIN = "join";
    public final static String PUSH_NOTIFICATION_APPROVAL = "approval";

    public static int NUM_OF_EMOJIS = 7;

    private static class DManClassFactory {

        static DrawMan drawMan;

        static DrawMan getDrawMan() {
            if (drawMan == null)
                return new DrawMan();
            else
                return drawMan;
        }
    }

    public static final DrawMan drawMan = DManClassFactory.getDrawMan();

    public static VolleySingletone volley;

    public static void initializeVolley(Context context) {
        volley = VolleySingletone.getInstance(context);
    }

    private static AccessTokenTracker mFbAccessTokenTracker;

    public static void initializeTokenTracker(Context context) {

        if (!FacebookSdk.isInitialized())
            FacebookSdk.sdkInitialize(context);

        mFbAccessTokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(AccessToken oldAccessToken,
                                                       AccessToken currentAccessToken) {
                Log.d(LOG_TAG, "Current access token was changed");
                AccessToken.setCurrentAccessToken(currentAccessToken);
            }
        };

        mFbAccessTokenTracker.startTracking();
    }

    public static void stopTokenTracker() {
        if (mFbAccessTokenTracker != null)
            mFbAccessTokenTracker.stopTracking();
    }

    private static FirebaseAuth mFirebaseAuth;
    public static FirebaseAuth getFirebaseAuth() {

        if( mFirebaseAuth == null ) {
            mFirebaseAuth = FirebaseAuth.getInstance();
        }

        return mFirebaseAuth;
    }


    private static MobileServiceClient wamsClient;

    public static boolean initMobileServices(Context ctx) {
        try {
            wamsClient = new MobileServiceClient(Globals.WAMS_URL, ctx);
            //.withFilter(new RefreshTokenCacheFilter());
            //.withFilter(new wamsUtils.ProgressFilter());
        } catch (MalformedURLException ex) {

            if (Fabric.isInitialized() && Crashlytics.getInstance() != null)
                Crashlytics.logException(ex);

            return false;
        }

        return true;
    }


    public static MobileServiceClient getMobileServiceClient() {
        return wamsClient;
    }

    private static Boolean _monitorInitialized = false;

    private static Boolean isMonitorInitialized() {
        return _monitorInitialized;
    }

    public static void initializeMonitor(Context ctx, Application app) {

        if (isMonitorInitialized())
            return;

        try {

            if (!FacebookSdk.isInitialized())
                FacebookSdk.sdkInitialize(ctx);

            AppEventsLogger.activateApp(app);

            TwitterAuthConfig authConfig = new TwitterAuthConfig(TWITTER_CONSUMER_KEY,
                    TWITTER_CONSUMER_SECRET);
            final Fabric fabric = new Fabric.Builder(ctx)
                    .kits(new Crashlytics())
                    .debuggable(true)
                    .build();
            Fabric.with(fabric);// ctx, new Twitter(authConfig), new Crashlytics(), new CrashlyticsNdk(), new Digits());

            User user = User.load(ctx);
            Crashlytics.setUserIdentifier(user.getRegistrationId());
            Crashlytics.setUserName(user.getFullName());
            Crashlytics.setUserEmail(user.getEmail());

            MobileServiceTable<GlobalSettings> settingsTable
                    = Globals.getMobileServiceClient()
                    .getTable("globalsettings", GlobalSettings.class);
            ListenableFuture<MobileServiceList<GlobalSettings>> settingsFuture =
                    settingsTable
                            .execute();
            Futures.addCallback(settingsFuture, new FutureCallback<MobileServiceList<GlobalSettings>>() {
                @Override
                public void onSuccess(MobileServiceList<GlobalSettings> list) {

                    if (!list.isEmpty()) {

                        for (GlobalSettings _s : list) {
                            switch (_s.getName()) {
                                case "apply_challenge": {
                                    APPLY_CHALLENGE = _s.getValue().equals("1");
                                }
                                break;

                                case "passengers_required": {
                                    REQUIRED_PASSENGERS_NUMBER = Long.parseLong(_s.getValue());
                                }
                                break;
                            }
                        }
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    Log.e(LOG_TAG, t.getMessage());
                }
            });

            _monitorInitialized = true;

        } catch (Exception e) {

            Globals.__logException(e);
        }
    }

    public static void __log(String tag, String message) {
        if (Fabric.isInitialized() && Crashlytics.getInstance() != null) {
            Crashlytics.log(Log.DEBUG, tag, message); // should also writes to logcat?
        }

        Log.d(tag, message);
    }

    public static void __logException(Throwable e) {
        __logException(LOG_TAG, e);
    }

    public static void __logException(String tag, Throwable e) {
        if (Fabric.isInitialized() && Crashlytics.getInstance() != null)
            Crashlytics.logException(e);

        String message = e.getMessage();
        if (message != null && !message.isEmpty())
            Log.e(tag, e.getMessage());
    }

    public static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    public static float MIN_ACCURACY = 50;

    public static int HIGH_PRIORITY_UPDATE_INTERVAL = 1;
    public static int HIGH_PRIORITY_FAST_INTERVAL = 1;

    public static float PICTURE_CORNER_RADIUS = 20;
    public static float PICTURE_BORDER_WIDTH = 4;

    public static int RIDE_CODE_INPUT_LENGTH = 6;

    public static int CABIN_PICTURES_BUTTON_SHOW_INTERVAL = 40 * 1000;

    private static DriverRoleActivity driverActivity;

    public static void setDriverActivity(DriverRoleActivity activity) {
        driverActivity = activity;
    }

    public static DriverRoleActivity getDriverActivity() {
        return driverActivity;
    }

    // PERMISSIONS
    static final public int LOCATION_PERMISSION_REQUEST = 1;
    static final public int CAMERA_PERMISSION_REQUEST = 2;

    static final public int MAX_DISCOVERY_TRIALS = 3;
    static final public Integer MAX_ALLOWED_DISCOVERY_FAILURES = 5;
    static final public String SERVER_PORT = "4545";
    static final public int SOCKET_TIMEOUT = 5000;
    public static final String TXTRECORD_PROP_USERID = "userid";
    public static final String TXTRECORD_PROP_USERNAME = "username";
    public static final String TXTRECORD_PROP_RIDECODE = "ridecode";
    public static final String TXTRECORD_PROP_PORT = "port";
    public static final String SERVICE_INSTANCE = "_wififastride";
    public static final String SERVICE_REG_TYPE = "_presence._tcp";

    public static final String BT_DELIMITER = ":";
    public static final String PREF_DISCOVERABLE_DURATION = "bt_discoverable_period";
    public static final int PREF_DISCOVERABLE_DURATION_DEFAULT = 300;

    public static final String PREF_LAST_SEEN_BEFORE = "last_seen_interval";
    public static final int PREF_LAST_SEEN_DEFAULT_INTERVAL = 40;
    public static long LAST_SEEN_INTERVAL = 40L;

    public static final String WAMS_URL = "https://oneride.azurewebsites.net";
    //public static final String WAMS_API_KEY = "omCudOMCUJgIGbOklMKYckSiGKajJU91";

    public static final String FB_USERNAME_PREF = "username";
    public static final String FB_LASTNAME_PREF = "lastUsername";

    public static final String NH_REGISTRATION_ID_PREF = "nhRegistrationID";
    public static final String FCM_TOKEN_PREF = "FCMtoken";

    public static final String REG_PROVIDER_PREF = "registrationProvider";
    public static final String UPDATE_MYRIDES_REQUIRED = "myRides_update_required";

    public static final String FIRST_NAME_PREF = "firstname";
    public static final String LAST_NAME_PREF = "lastname";
    public static final String REG_ID_PREF = "regid";
    public static final String PICTURE_URL_PREF = "pictureurl";
    public static final String EMAIL_PREF = "email";
    public static final String PHONE_PREF = "phone";
    public static final String USE_PHONE_PFER = "usephone";

    public static final String FB_PROVIDER = "facebook";
    public static final String GOOGLE_PROVIDER = "google";
    public static final String TWITTER_PROVIDER = "twitter";
    public static final String DIGITS_PROVIDER = "digits";
    public static final String MICROSOFT_PROVIDER = "MicrosoftAccount";
    public static final String PLATFORM = "Android" + Build.VERSION.SDK_INT;

    // 'Sender ID' of project 'OneRide"
    // See Firebase Developer Console ->Settings -> Cloud Messaging
    // https://console.firebase.google.com/project/oneride-1273/settings/cloudmessaging
    public static final String SENDER_ID = "982244912173";
    public static final String AZURE_HUB_NAME = "oneridehub";
    public static final String AZURE_HUB_CONNECTION_STRING = "Endpoint=sb://oneridehub-ns.servicebus.windows.net/;SharedAccessKeyName=DefaultFullSharedAccessSignature;SharedAccessKey=VA/kIgB0KzxhenuquHpqbqceD2z9MUwarmN+FdOFTck=";
    public static final String AZURE_STORAGE_CONNECTION_STRING =
            "DefaultEndpointsProtocol=https;AccountName=oneride;" +
                    "AccountKey=bdNIAOimN48pj29UmMQRgo5UK5a29cyJ3HnTM5Ikc4HzI7/DUOpxclfedehnQ/D7uSFEm8YOtcUyxUiSKpDqvw==";

    public static final String NOTIFICATION_ID_EXTRA = "NOTIFICATION_ID";
    public static final String ACTION_CONFIRM = "oneride.intent.action.CONFIRM";
    public static final String ACTION_CANCEL = "oneride.intent.action.CANCEL";


    // Names of shared preferences
    public static final String USERIDPREF = "userid";
    public static final String CARS_PREF = "cars";
    public static final String TOKEN_PREF = "accessToken";
    public static final String TOKENSECRET_PREF = "accessTokenSecret";
    public static final String AUTHORIZATION_CODE_PREF = "authorizationToken";
    public static final String WAMSTOKENPREF = "wamsToken";
    public static final String SHOW_SELFIE_DESC = "selfieDesc";
    public static final String PREF_DEBUG_WITHOUT_GEOFENCES = "debug_without_geofences";
    public static final String PREF_RSSI_LEVEL = "rssi_level";
    public static final int DEFAULT_RSSI_LEVEL = 80;

    public static final String PREF_ALLOW_SAME_PASSENGERS = "allow_same_passengers";
    public static final String PREF_PUSH_MODE = "push_mode";
    public static final String PREF_REALTIMEDB__MODE = "scan_mode";
    public static final String PREF_SOCKETS_MODE = "sockets_mode";

    private static boolean bPushNotificationsMode = true;

    public static boolean isPushNotificationsModeEnabled() {
        return bPushNotificationsMode;
    }

    public static void setPushNotificationsModeEnabled(boolean mode) {
        bPushNotificationsMode = mode;
    }

    private static boolean bRealtimeDbNotificationsMode = true;

    public static boolean isRealtimeDbNotificationsMode() {
        return bRealtimeDbNotificationsMode;
    }

    public static void setRealtimeDbNotificationsMode(boolean mode) {
        bRealtimeDbNotificationsMode = mode;
    }

    private static final Object lock2 = new Object();
    private static String MONITOR_STATUS;

    public static String getMonitorStatus() {
        synchronized (lock2) {
            return MONITOR_STATUS;
        }
    }

    public static void setMonitorStatus(String value) {
        synchronized (lock2) {
            MONITOR_STATUS = value;
        }
    }

    // TODO: synchronize with Geo-Fences
    public static boolean myRides_update_required = true;

    private static String _currentGeoFenceName;

    public static void set_CurrentGeoFenceName(String value) {
        _currentGeoFenceName = value;
    }

    public static String get_currentGeoFenceName() {
        return _currentGeoFenceName;
    }

    // Driver/passenger 'chat' messages
    public static final int MESSAGE_READ = 0x400 + 1;
    public static final int MESSAGE_DISCOVERY_FAILED = 0x400 + 2;
    public static final int TRACE_MESSAGE = 0x400 + 3;

    // Geofences
    public static final HashMap<String, LatLng> FWY_AREA_LANDMARKS = new HashMap<String, LatLng>();
    public static ArrayList<Geofence> GEOFENCES = new ArrayList<>();
    public static PendingIntent GeofencePendingIntent;

    public static Boolean IGNORE_GEOFENCES = true;

    private static final Object lock = new Object();
    private static boolean inGeofenceArea;

    public static boolean isInGeofenceArea() {
        synchronized (lock) {
            if (Globals.IGNORE_GEOFENCES)
                return true;
            else
                return inGeofenceArea;
        }
    }

    public static void setInGeofenceArea(boolean value) {
        synchronized (lock) {
            inGeofenceArea = value;
        }
    }

    public static long GF_OUT_TOLERANCE = 7000; // Globals.LOCATION_UPDATE_MIN_FEQUENCY * 2;

    private static final Object lock3 = new Object();
    private static boolean _REMIND_GEOFENCE_ENTRANCE;

    public static void setRemindGeofenceEntrance() {
        synchronized (lock3) {
            _REMIND_GEOFENCE_ENTRANCE = true;
        }
    }

    public static Boolean getRemindGeofenceEntrance() {
        synchronized (lock3) {
            return _REMIND_GEOFENCE_ENTRANCE;
        }
    }

    public static void clearRemindGeofenceEntrance() {
        synchronized (lock3) {
            _REMIND_GEOFENCE_ENTRANCE = false;
        }
    }

    public static String CASCADE_URL = "http://oneride.azurewebsites.net/data/haarcascades/haarcascade_frontalface_default.xml";
    private static String CASCADE_PATH;

    public static void initCascadePath(Context ctx) {
        String DEFAULT_CASCADE_NAME = "haarcascade_frontalface_default.xml";
        File file = new File(ctx.getFilesDir(), DEFAULT_CASCADE_NAME);
        synchronized (lock) {
            CASCADE_PATH = file.getAbsolutePath();
        }
    }

    public static String getCascadePath(Context ctx) {
        if (CASCADE_PATH == null || CASCADE_PATH.isEmpty())
            initCascadePath(ctx);

        return CASCADE_PATH;
    }

    public static void setCascadePath(String path) {
        synchronized (lock) {
            CASCADE_PATH = path;
        }
    }

    private static final Object lockPassengerFaces = new Object();
    private static HashMap<Integer, PassengerFace> _passengerFaces = new HashMap<>();

    public static HashMap<Integer, PassengerFace> get_PassengerFaces() {
        synchronized (lockPassengerFaces) {
            return _passengerFaces;
        }
    }

    public static void set_PassengerFaces(HashMap<Integer, PassengerFace> faces) {
        _passengerFaces.putAll(faces);
    }

    public static void add_PassengerFace(PassengerFace pf) {
        for (int i = 0; i < Globals.REQUIRED_PASSENGERS_NUMBER; i++) {
            if (_passengerFaces.get(i) == null) {
                _passengerFaces.put(i, pf);
                break;
            }
        }
    }

    public static PassengerFace get_PassengerFace(int at) {
        synchronized (lockPassengerFaces) {
            return _passengerFaces.get(at);
        }
    }

    public static void clearPassengerFaces() {
        synchronized (lockPassengerFaces) {
            _passengerFaces.clear();
        }
    }

    // Identity matrix : ones on the main diagonal
    // and zeros elsewhere.
    public static Matrix4f verificationMat = new Matrix4f();

    public static int PASSENGER_DISCOVERY_PERIOD = 8;
    public static int PASSENGER_ADVERTISING_PERIOD = 40;
    public static int DRIVER_DISCOVERY_PERIOD = 20;

    // Parcels
    public static String PARCELABLE_KEY_RIDE_CODE = "ride_code_key";
    public static String PARCELABLE_KEY_RIDE_CODE_UPLOADED = "ride_code_uploaded_key";
    public static String PARCELABLE_KEY_PASSENGERS = "passengers_key";
    public static String PARCELABLE_KEY_DRIVERS = "drivers_key";
    public static String PARCELABLE_KEY_DRVER_NAME = "driver_name";
    public static String PARCELABLE_KEY_EMOJI_ID = "emoji_id";
    public static String PARCELABLE_KEY_CURRENT_RIDE = "current_ride";
    public static String PARCELABLE_KEY_CABIN_PICTURES_BUTTON_SHOWN = "cabin_button_shown";
    public static String PARCELABLE_KEY_UPLOAD_PROGRESS_SHOWN = "upload_progress_shown";
    public static String PARCELABLE_KEY_CAPTURED_PASSENGERS_IDS = "captured_passengers_ids";
    public static String PARCELABLE_KEY_PASSENGERS_FACE_IDS = "face_ids";
    public static String PARCELABLE_KEY_APPROVAL_PHOTO_URI = "approval_uri";
    public static String PARCELABLE_KEY_EMOJIID = "emoji_id";
    public static String PARCELABLE_KEY_DRIVER_CABIN_SHOWN = "cabin_shown";
    public static String PARCELABLE_KEY_EMPTY_TEXT_SHOWN = "empty_text_shown";
    public static String PARCELABLE_KEY_SUBMIT_BUTTON_SHOWN = "submit_button_shown";
    public static String PARCELABLE_KEY_PASSENGER_PREFIX = "thumb_";
    public static String PARCELABLE_KEY_RIDES_HISTORY = "rides_history";
    public static String PARCELABLE_KEY_DIALOG_MESSAGE = "dialog_Message";
    public static String PARCELABLE_LOCATION = "currentLocation";

    // Check out http://go.microsoft.com/fwlink/p/?LinkId=193157 to get your own client id
    public static final String MICROSOFT_CLIENT_ID = "0000000048137798";
//    public static LiveAuthClient liveAuthClient;
//    public static final String[] LIVE_SCOPES = {
//            "wl.signin",
//            "wl.basic",
//            "wl.emails",
//            "wl.offline_access"
//    };

    public static GoogleApiClient googleApiClient;


    public static String TWITTER_CONSUMER_KEY = "jxvXE5xHG84JvuI4bLJApTzYb";
    public static String TWITTER_CONSUMER_SECRET = "EzJlpFBvSkeaoPA28wJT9sHvEnxAEpvKDOLTImwM0Jk9wLsnQK";

    public static final String storageConnectionString =
            "DefaultEndpointsProtocol=http;" +
                    "AccountName=fastride;" +
                    "AccountKey=tuyeJ4EmEuaoeGsvptgyXD0Evvsu1cTiYPAF2cwaDzcGkONdAOZ/3VEY1RHAmGXmXwwkrPN1yQmRVdchXQVgIQ==";

    public static final int FACE_VERIFY_TASK_TAG = 1;
    public static final int APPROVAL_UPLOAD_TASK_TAG = 2;

    public static final int TUTORIAL_Intro = 1;
    public static final int TUTORIAL_Driver = 2;
    public static final int TUTORIAL_Passenger = 3;
    public static final int TUTORIAL_Appeal = 4;
}
