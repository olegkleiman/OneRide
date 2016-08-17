package com.labs.okey.oneride;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Pair;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonElement;
import com.labs.okey.oneride.fragments.ConfirmRegistrationFragment;
import com.labs.okey.oneride.fragments.RegisterCarsFragment;
import com.labs.okey.oneride.gcm.GCMHandler;
import com.labs.okey.oneride.model.GeoFence;
import com.labs.okey.oneride.model.User;
import com.labs.okey.oneride.utils.Globals;
import com.labs.okey.oneride.utils.wamsUtils;
import com.microsoft.windowsazure.mobileservices.MobileServiceList;
import com.microsoft.windowsazure.mobileservices.http.HttpConstants;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.microsoft.windowsazure.mobileservices.table.query.Query;
import com.microsoft.windowsazure.mobileservices.table.sync.MobileServiceSyncTable;
import com.microsoft.windowsazure.notifications.NotificationsManager;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterApiClient;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterAuthClient;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import io.fabric.sdk.android.Fabric;

public class RegisterActivity extends FragmentActivity
        implements ConfirmRegistrationFragment.RegistrationDialogListener,
        GoogleApiClient.OnConnectionFailedListener {

    private final String        LOG_TAG = getClass().getSimpleName();
    private final String        PENDING_ACTION_BUNDLE_KEY = "com.labs.okey.freeride:PendingAction";

    private String              mAndroidId;
    private String              mDeviceModel;

    private CallbackManager     mFBCallbackManager;
    private LoginButton         mFBLoginButton;
    private ProfileTracker      mFbProfileTracker;

//    DigitsAuthButton        mDigitsButton;
//    private AuthCallback    mDigitsAuthCallback;
//    public AuthCallback     getAuthCallback(){
//        return mDigitsAuthCallback;
//    }
    TwitterLoginButton          mTwitterloginButton;

    private User                mNewUser;
    private String              mAccessToken;
    private String              mAuthorizationCode;
    private String              mAccessTokenSecret; // used by Twitter
    private boolean             mAddNewUser = true;

    private static final int    RC_SIGN_IN = 9001; // Used by Google+
    private static final int    REQUEST_CODE_RESOLVE_ERR = 9002;

    MaterialDialog              mGoogleProgressDialog;

    @Override
    public void onDialogPositiveClick(DialogFragment dialog, final User user) {
        final String android_id = Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected void onPostExecute(Void result) {
                showRegistrationForm();
                findViewById(R.id.btnRegistrationNext).setVisibility(View.VISIBLE);
            }

            @Override
            protected Void doInBackground(Void... voids) {

                user.setDeviceId(android_id);
                user.setPlatform(Globals.PLATFORM);

                try {
                    usersTable.delete(user).get();
                } catch (InterruptedException | ExecutionException ex) {
                    Globals.__logException(ex);
                }

                return null;
            }
        }.execute();
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {

    }

    private enum PendingAction {
        NONE,
        POST_PHOTO,
        POST_STATUS_UPDATE
    }

    private PendingAction pendingAction = PendingAction.NONE;

    // 'Users' table is defined with 'everyone'
    // permissions for READ, INSERT and DELETE operations, so no authentication is
    // required for adding new user to it
    MobileServiceTable<User> usersTable;

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if( result.hasResolution() ) {
            try {
                result.startResolutionForResult(this, REQUEST_CODE_RESOLVE_ERR);
            } catch(IntentSender.SendIntentException ex) {
                // There was an error with the resolution intent. Try again.
                Globals.googleApiClient.connect();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_register);

        if (savedInstanceState != null) {
            String name = savedInstanceState.getString(PENDING_ACTION_BUNDLE_KEY);
            if( name != null && name.isEmpty() )
                pendingAction = PendingAction.valueOf(name);
        }

        setContentView(R.layout.activity_register);
        Toolbar toolbar = (Toolbar) findViewById(R.id.fastride_toolbar);
        toolbar.setTitleTextColor(getResources().getColor(R.color.white));
        toolbar.setTitle(getString(R.string.title_activity_register));

//        if( !isSimPresent() ) {
//            findViewById(R.id.fb_login).setEnabled(false);
//            findViewById(R.id.google_login).setEnabled(false);
//
//            findViewById(R.id.txtNoSIM).setVisibility(View.VISIBLE);
//
//            return;
//        }

        final ContentResolver contentResolver = this.getContentResolver();
        mAndroidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID);

        // See list of supported devices here:
        // https://support.google.com/googleplay/answer/1727131
        mDeviceModel = Build.BRAND + ";" + Build.MODEL;

        // Twitter stuff
        mTwitterloginButton = (TwitterLoginButton) findViewById(R.id.twitter_login);
        mTwitterloginButton.setCallback(new Callback<TwitterSession>() {
            @Override
            public void success(Result<TwitterSession> result) {

                TwitterSession session = Twitter.getSessionManager().getActiveSession();

                mAccessToken = result.data.getAuthToken().token;
                mAccessTokenSecret = result.data.getAuthToken().secret;

                TwitterApiClient twitterApiClient = TwitterCore.getInstance().getApiClient();
                twitterApiClient.getAccountService().verifyCredentials(false, false, new Callback<com.twitter.sdk.android.core.models.User>() {

                    @Override
                    public void success(Result<com.twitter.sdk.android.core.models.User> userResult) {

                        mNewUser = new User();
                        String userID = userResult.data.idStr;
                        mNewUser.setRegistrationId(Globals.TWITTER_PROVIDER + Globals.BT_DELIMITER + userID);
                        String userName = userResult.data.name;
                        String[] unTokens = userName.split(" ");
                        mNewUser.setFirstName(unTokens[0]);
                        mNewUser.setLastName(unTokens[1]);

                        mNewUser.setDeviceId(mAndroidId);
                        mNewUser.setDeviceModel(mDeviceModel);

                        mNewUser.setPictureURL(userResult.data.profileImageUrl.replace("_normal", "_bigger"));

                        saveProviderAccessToken(Globals.TWITTER_PROVIDER, userID);
                    }

                    @Override
                    public void failure(TwitterException e) {

                    }
                });

                TwitterAuthClient authClient = new TwitterAuthClient();
                authClient.requestEmail(session, new Callback<String>() {
                    @Override
                    public void success(Result<String> result) {
                        mNewUser.setEmail(result.data);
                        mNewUser.save(getApplicationContext());

                        verifyAccount("???"); // what is Twitter regId?
                    }

                    @Override
                    public void failure(TwitterException error) {
                        Toast.makeText(RegisterActivity.this, error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void failure(TwitterException exception) {
                // Do something on failure
            }
        });

        // Google+ stuff
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.server_client_id))
                .requestServerAuthCode(getString(R.string.server_client_id),
                        true) // force code for refresh token
                .requestEmail()
                .build();

        CredentialRequest mCredentialRequest = new CredentialRequest.Builder()
                .setPasswordLoginSupported(true)
                .setAccountTypes(IdentityProviders.GOOGLE, IdentityProviders.TWITTER)
                .build();

        Globals.googleApiClient = new GoogleApiClient.Builder(this)
                                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                                //.addApi(Auth.CREDENTIALS_API)
                                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                                .build();

        SignInButton googleSignInButton = (SignInButton)findViewById(R.id.google_login);
        googleSignInButton.setColorScheme(SignInButton.COLOR_AUTO);
        googleSignInButton.setSize(SignInButton.SIZE_WIDE);
        googleSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(Globals.googleApiClient);
                startActivityForResult(signInIntent, RC_SIGN_IN);
            }
        });
        mGoogleProgressDialog = new MaterialDialog.Builder(this)
                .title("Connecting to Google API")
                .content("Please wait")
                .progress(true, 0)
                .build();

        // FB stuff
        mFBCallbackManager = CallbackManager.Factory.create();

        mFBLoginButton = (LoginButton) findViewById(R.id.fb_login);
        mFBLoginButton.setReadPermissions("email");

        // Callback registration
        mFBLoginButton.registerCallback(mFBCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(final LoginResult loginResult) {

                mAccessToken = loginResult.getAccessToken().getToken();

                mNewUser = new User();

                if (Profile.getCurrentProfile() == null) {

                    mFbProfileTracker = new ProfileTracker() {
                        @Override
                        protected void onCurrentProfileChanged(Profile oldProfile, final Profile profile) {

                            mNewUser.setRegistrationId(Globals.FB_PROVIDER + Globals.BT_DELIMITER + profile.getId());
                            mNewUser.setFirstName(profile.getFirstName());
                            mNewUser.setLastName(profile.getLastName());
                            String pictureURI = profile.getProfilePictureUri(100, 100).toString();
                            mNewUser.setPictureURL(pictureURI);

                            mNewUser.setDeviceId(mAndroidId);
                            mNewUser.setDeviceModel(mDeviceModel);
                            mNewUser.setPlatform(Globals.PLATFORM);

                            completeFBRegistration(loginResult.getAccessToken(), profile.getId());
                            mFbProfileTracker.stopTracking();
                        }
                    };
                    mFbProfileTracker.startTracking();
                } else {
                    Profile profile = Profile.getCurrentProfile();

                    mNewUser.setRegistrationId(Globals.FB_PROVIDER + Globals.BT_DELIMITER + profile.getId());
                    mNewUser.setFirstName(profile.getFirstName());
                    mNewUser.setLastName(profile.getLastName());
                    String pictureURI = profile.getProfilePictureUri(100, 100).toString();
                    mNewUser.setPictureURL(pictureURI);

                    mNewUser.setDeviceId(mAndroidId);
                    mNewUser.setDeviceModel(mDeviceModel);
                    mNewUser.setPlatform(Globals.PLATFORM);

                    completeFBRegistration(loginResult.getAccessToken(), profile.getId());
                }
            }

            @Override
            public void onCancel() {
                // App code
            }

            @Override
            public void onError(FacebookException exception) {
                String msg = getResources().getString(R.string.fb_error_msg)
                        + exception.getMessage().trim();

                new AlertDialog.Builder(RegisterActivity.this)
                        .setTitle(getResources().getString(R.string.fb_error))
                        .setMessage(msg)
                        .setPositiveButton("OK", null)
                        .show();
            }
        });

        usersTable = Globals.getMobileServiceClient()
                    .getTable("users", User.class);

    }

    private boolean isSimPresent() {
        TelephonyManager telMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        return !(TelephonyManager.SIM_STATE_ABSENT == telMgr.getSimState()) ;
    }

    private Boolean googleHandleSignInResult(GoogleSignInResult result) {
        if ( !result.isSuccess()) {
            Status status = result.getStatus();
            String msg = status.getStatusMessage();

            new MaterialDialog.Builder(RegisterActivity.this)
                    .title(R.string.registration_account_validation_failure)
                    .iconRes(R.drawable.ic_exclamation)
                    .content(msg)
                    .positiveText(android.R.string.ok)
                    .autoDismiss(true)
                    .show();

            return false;
        }

        mNewUser = new User();

        GoogleSignInAccount acct = result.getSignInAccount();
        if( acct == null )
            return false;

        mAccessToken = acct.getIdToken();
        mAuthorizationCode = acct.getServerAuthCode();
        String regId = acct.getId();
        mNewUser.setRegistrationId(Globals.GOOGLE_PROVIDER + Globals.BT_DELIMITER + regId);
        saveProviderAccessToken(Globals.GOOGLE_PROVIDER, regId);

        mNewUser.setFullName(acct.getDisplayName());
        mNewUser.setEmail(acct.getEmail());
        if (acct.getPhotoUrl() != null)
            mNewUser.setPictureURL(acct.getPhotoUrl().toString());

        mNewUser.setDeviceId(mAndroidId);
        mNewUser.setDeviceModel(mDeviceModel);
        mNewUser.setPlatform(Globals.PLATFORM);

        verifyAccount(regId);
        return true;
    }

    private void verifyAccount(String regId) {

        ListenableFuture<MobileServiceList<User> > usersFuture =
                usersTable.where()
                        .field("registration_id").eq(regId).execute();
        Futures.addCallback(usersFuture, new FutureCallback<MobileServiceList<User>>() {
            @Override
            public void onSuccess(MobileServiceList<User> users) {
                if (users.size() > 0) {
                    final User user = users.get(0);

                    if( user.compare(mNewUser) ) {

                        new MaterialDialog.Builder(RegisterActivity.this)
                                .title(R.string.registration_account_validation_failure)
                                .iconRes(R.drawable.ic_exclamation)
                                .content(R.string.registration_exists)
                                .positiveText(android.R.string.yes)
                                .negativeText(android.R.string.no)
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                                        ListenableFuture<Void> deleteUserFuture = usersTable.delete(user);
                                        Futures.addCallback(deleteUserFuture, new FutureCallback<Void>() {
                                            @Override
                                            public void onSuccess(Void result) {
                                                continueRegistration();
                                            }

                                            @Override
                                            public void onFailure(Throwable t) {

                                                Globals.__logException(t);
                                             }
                                        });

                                    }
                                })
                                .onNegative(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                                    }
                                })
                                .autoDismiss(true)
                                .show();
                    } else {
                        continueRegistration();
                    }
                } else {
                    continueRegistration();
                }
            }

            @Override
            public void onFailure(Throwable t) {

                Globals.__logException(t);

                Toast.makeText(RegisterActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();

            }
        });
    }

    private void completeFBRegistration(AccessToken accessToken, final String regId){
        GraphRequest request = GraphRequest.newMeRequest(
                accessToken,
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(
                            JSONObject object,
                            GraphResponse response) {

                        try {
                            JSONObject gUser = response.getJSONObject();
                            String email = gUser.getString("email");
                            mNewUser.setEmail(email);

                            String regID = Globals.FB_PROVIDER + Globals.BT_DELIMITER + regId;
                            saveProviderAccessToken(Globals.FB_PROVIDER, regID);

                            verifyAccount(regID);

                        } catch (JSONException ex) {
                            Globals.__logException(ex);
                        }

                    }
                });
        Bundle parameters = new Bundle();
        parameters.putString("fields", "id,name,email");
        request.setParameters(parameters);
        request.executeAsync();
    }

    @Override
    protected void onStart() {
        super.onStart();

//        if( !Globals.googleApiClient.isConnected() )
//            Globals.googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if( Globals.googleApiClient != null && Globals.googleApiClient.isConnected() )
            Globals.googleApiClient.disconnect();
    }

    @Override
    public void onResume() {
        super.onResume();
//        uiHelper.onResume();

        // Call the 'activateApp' method to log an app event for use in analytics and advertising reporting.  Do so in
        // the onResume methods of the primary Activities that an app may be launched into.
        //AppEventsLogger.activateApp(this);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if( FacebookSdk.isFacebookRequestCode(requestCode) )
            mFBCallbackManager.onActivityResult(requestCode, resultCode, data);
        else if( requestCode == RC_SIGN_IN ) { // Google

            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            googleHandleSignInResult(result);

        } else if( requestCode == REQUEST_CODE_RESOLVE_ERR && resultCode == RESULT_OK ) {
            // Make sure the app is not already connected or attempting to connect
            if (!Globals.googleApiClient.isConnecting() &&
                    !Globals.googleApiClient.isConnected())
            Globals.googleApiClient.connect();
        }
        else if( requestCode == TwitterAuthConfig.DEFAULT_AUTH_REQUEST_CODE ) {
            mTwitterloginButton.onActivityResult(requestCode, resultCode, data);
        }

    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if( mFbProfileTracker != null && mFbProfileTracker.isTracking() )
            mFbProfileTracker.stopTracking();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private void continueRegistration() {
        LinearLayout loginLayout = (LinearLayout) findViewById(R.id.login_form);
        if (loginLayout != null)
            loginLayout.setVisibility(View.GONE);

        showRegistrationForm();
    }

    private void saveProviderAccessToken(String provider, String userID) {

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPrefs.edit();

        editor.putString(Globals.REG_PROVIDER_PREF, provider);
        editor.putString(Globals.USERIDPREF, userID);
        editor.putString(Globals.TOKEN_PREF, mAccessToken);
        if( mAccessTokenSecret != null && !mAccessTokenSecret.isEmpty() )
            editor.putString(Globals.TOKENSECRET_PREF, mAccessTokenSecret);

        if( mAuthorizationCode != null && !mAuthorizationCode.isEmpty() )
           editor.putString(Globals.AUTHORIZATION_CODE_PREF, mAuthorizationCode);

        editor.apply();
    }

    private void handlePendingAction() {
        pendingAction = PendingAction.NONE;
    }

    private void showRegistrationForm() {
        LinearLayout form = (LinearLayout)findViewById(R.id.register_form);
        form.setVisibility(View.VISIBLE);
        View buttonNext = findViewById(R.id.btnRegistrationNext);
        buttonNext.setVisibility(View.VISIBLE);
    }

    private void hideRegistrationForm() {
        LinearLayout form = (LinearLayout)findViewById(R.id.register_form);
        form.setVisibility(View.GONE);
    }

    private void showError(String errorMessage) {

        new MaterialDialog.Builder(this)
                .title(R.string.registration_account_validation_failure)
                .iconRes(R.drawable.ic_exclamation)
                .content(errorMessage)
                .positiveText(android.R.string.ok)
                .autoDismiss(true)
                .show();
    }

    boolean bCarsFragmentDisplayed = false;
    boolean bConfirmFragmentDisplayed = false;

    private boolean isValidPhoneNumber(String phoneNumber) {
        if( phoneNumber == null ||
                phoneNumber.isEmpty() )
            return false;

        if( phoneNumber.length() < 10 )
            return false;

        String formattedPhoneNumber = PhoneNumberUtils.formatNumber(phoneNumber);
        Log.d(LOG_TAG, "Formatted phone number: " + formattedPhoneNumber);

        return Patterns.PHONE.matcher(formattedPhoneNumber).matches();
    }

    public void onRegisterNext(final View v){

        if( !bConfirmFragmentDisplayed ) {

            // Validate entered phone number
            final EditText txtPhoneNumber = (EditText) findViewById(R.id.phone);
            if( !isValidPhoneNumber(txtPhoneNumber.getText().toString()) ) {

                String noPhoneNumber = getResources().getString(R.string.no_phone_number);
                txtPhoneNumber.setError(noPhoneNumber);
                return;
            }

            mNewUser.setPhone(txtPhoneNumber.getText().toString());
            mNewUser.save(RegisterActivity.this);

            if( mAddNewUser ) {

                ListenableFuture<User> newUserFuture = usersTable.insert(mNewUser);
                ListenableFuture<String> sendSMSFuture = Futures.transform(newUserFuture, new Function<User, String>() {
                    @Override
                    public String apply(User _user){

                        return _user.getPhone();
                    }
                });

                Futures.addCallback(sendSMSFuture, new FutureCallback<String>() {
                    @Override
                    public void onSuccess(String userPhoneNumber) {

                        List<Pair<String, String>> params = new ArrayList<Pair<String, String>>();
                        params.add(new Pair<>("phoneNumber", userPhoneNumber));

                        ListenableFuture<JsonElement> apiFuture = Globals.getMobileServiceClient()
                                .invokeApi("sendSMS", HttpConstants.PostMethod, params);

                        Futures.addCallback(apiFuture, new FutureCallback<JsonElement>() {
                            @Override
                            public void onSuccess(JsonElement result) {

                                hideRegistrationForm();
                                bConfirmFragmentDisplayed = true;
                                onRegisterNext(v);
                            }

                            @Override
                            public void onFailure(Throwable t) {

                                showError(t.getMessage());
                            }
                        });
                    }

                    @Override
                    public void onFailure(Throwable t) {

                        showError(t.getMessage());
                    }
                });
            } else {

                ListenableFuture<MobileServiceList<User>> getUserFuture =
                        usersTable
                            .where()
                            .field("registration_id")
                            .eq(mNewUser.getRegistrationId())
                            .execute();

                ListenableFuture<User> existingUserFuture =
                        Futures.transform(getUserFuture, new Function<MobileServiceList<User>, User>() {
                        @Override
                        public User apply(MobileServiceList<User> users){
                            User user = users.get(0);

                            if( user != null ) {
                                user.setDeviceId(mAndroidId);
                                user.setDeviceModel(mDeviceModel);

                                user.setPhone(txtPhoneNumber.getText().toString());
                            }

                            return user;
                        }
                });

                Futures.addCallback(existingUserFuture, new FutureCallback<User>() {
                    @Override
                    public void onSuccess(User user) {

                        ListenableFuture<User> updateUserFuture = usersTable.update(user);
                        Futures.addCallback(updateUserFuture, new FutureCallback<User>() {
                            @Override
                            public void onSuccess(User result) {

                                hideRegistrationForm();

                                // Skip SMS confirmation for existing users
                                bConfirmFragmentDisplayed = true;

                                onRegisterNext(v);
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                showError(t.getMessage());
                            }
                        });
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        showError(t.getMessage());
                    }
                });
            }
        } else if( !bCarsFragmentDisplayed ) {

            RegisterCarsFragment fragment = new RegisterCarsFragment();

            FragmentTransaction transaction = getFragmentManager().beginTransaction();

            // Replace whatever is in fragment placeholder view with this fragment,
            // and add the transaction to the back stack so the user can navigate back.
            transaction.replace(R.id.register_wizard_placeholder, fragment);
            transaction.addToBackStack(null);

            transaction.commit();

            bCarsFragmentDisplayed = true;
            Button btnNext = (Button)findViewById(R.id.btnRegistrationNext);
            btnNext.setVisibility(View.VISIBLE);
            btnNext.setText(R.string.registration_finish);

        }
        else { // Finish

            final View view = findViewById(R.id.register_wizard_placeholder);

            new AsyncTask<Void, String, Void>() {

                Exception mEx;

                ProgressDialogFragment progressDialog;
                @Override
                protected void onPreExecute() {

                    super.onPreExecute();

                    progressDialog = ProgressDialogFragment.newInstance(
                                            getString(R.string.download_geofences_desc));
                    progressDialog.show(RegisterActivity.this.getFragmentManager(), "dialog");

//                    progressDialog = ProgressDialog.show(RegisterActivity.this,
//                            getString(R.string.download_data),
//                            getString(R.string.download_geofences_desc));
                }

                @Override
                protected void onPostExecute(Void result){

                    if( progressDialog != null && progressDialog.isVisible() ) {
                        progressDialog.dismiss();
                        progressDialog = null;
                    }

                    if( mEx == null ) {

                        if( Fabric.isInitialized() && Answers.getInstance() != null ) {
                            CustomEvent regEvent = new CustomEvent(getString(R.string.registration_answer_name));
                            regEvent.putCustomAttribute("User", mNewUser.getFullName());
                            Answers.getInstance().logCustom(regEvent);
                        }

                        NotificationsManager.handleNotifications(getApplicationContext(),
                                Globals.SENDER_ID,
                                GCMHandler.class);

                        // Register with Notification Hubs
                        if (checkPlayServices()) {
                            // Start IntentService to register this application with FCM.
                            Intent intent = new Intent(RegisterActivity.this,
                                                       AzureRegistrationIntentService.class);
                            startService(intent);
                        }


                        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();

                    } else {
                        Snackbar snackbar =
                                Snackbar.make(view, mEx.getMessage(), Snackbar.LENGTH_LONG);
                        snackbar.setActionTextColor(getResources().getColor(R.color.white));
                        //snackbar.setDuration(8000);
                        snackbar.show();
                    }
                }

                @Override
                protected void onProgressUpdate(String... progress) {
                    RegisterActivity.this.getSupportFragmentManager().executePendingTransactions();
                    progressDialog.setMessage(progress[0]);
                }

                @Override
                protected Void doInBackground(Void... voids) {

                    try {
                        mEx = null;

                        MobileServiceSyncTable<GeoFence> gFencesSyncTable = Globals.getMobileServiceClient().getSyncTable("geofences",
                                GeoFence.class);
                        wamsUtils.sync(Globals.getMobileServiceClient(), "geofences");

                        Query pullQuery = Globals.getMobileServiceClient().getTable(GeoFence.class).where();
                        gFencesSyncTable.purge(pullQuery);
                        gFencesSyncTable.pull(pullQuery).get();

                        publishProgress( getString(R.string.download_classifiers_desc) );

                        // Download cascade(s)
                        URL url = new URL(Globals.CASCADE_URL);
                        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                        urlConnection.setRequestMethod("GET");
                        urlConnection.connect();

                        String cascadeName = Uri.parse(Globals.CASCADE_URL).getLastPathSegment();

                        //set the path where we want to save the file
                        File file = new File(getFilesDir(), cascadeName);
                        FileOutputStream fileOutput = new FileOutputStream(file);

                        InputStream inputStream = urlConnection.getInputStream();

                        byte[] buffer = new byte[1024];
                        int bufferLength;

                        while ( (bufferLength = inputStream.read(buffer)) > 0 ) {
                            fileOutput.write(buffer, 0, bufferLength);
                        }
                        fileOutput.close();

                        Globals.setCascadePath(file.getAbsolutePath());

                    } catch(InterruptedException | ExecutionException | IOException ex ) {
                        mEx = ex;
                        Globals.__logException(ex);
                    }

                    return null;
                }
            }.execute();
        }
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
                apiAvailability.getErrorDialog(this, resultCode,
                                                Globals.PLAY_SERVICES_RESOLUTION_REQUEST)
                                                .show();
            } else {
                Globals.__log(LOG_TAG, getString(R.string.no_playservices));
            }

            return false;
        } else
            return true;
    }

    // Dialogs

    public static class ProgressDialogFragment extends DialogFragment {

        private static final String MESSAGE_TAG = "message";
        private static String mMessage;

        public static ProgressDialogFragment newInstance(String message) {
            ProgressDialogFragment fragment = new ProgressDialogFragment();

            Bundle args = new Bundle();
            args.putString(MESSAGE_TAG, message);
            fragment.setArguments(args);

            return fragment;
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putString(Globals.PARCELABLE_KEY_DIALOG_MESSAGE, mMessage);
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            if( savedInstanceState != null ) {
                mMessage = savedInstanceState.getString(Globals.PARCELABLE_KEY_DIALOG_MESSAGE);
            }
            else {
                mMessage = getArguments().getString(MESSAGE_TAG);
            }

            final ProgressDialog progressDialog = new ProgressDialog(getActivity());
            progressDialog.setTitle(getString(R.string.download_data));
            progressDialog.setMessage(mMessage);

            return progressDialog;
        }

        public void setMessage(String message) {
            ProgressDialog dialog = (ProgressDialog)getDialog();
            if( dialog != null )
                dialog.setMessage(message);
        }
    }
}
