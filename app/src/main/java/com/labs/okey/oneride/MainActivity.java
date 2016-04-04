package com.labs.okey.oneride;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.Cache;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.LoginEvent;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.JsonObject;
import com.labs.okey.oneride.adapters.ModesPeersAdapter;
import com.labs.okey.oneride.gcm.GCMHandler;
import com.labs.okey.oneride.model.FRMode;
import com.labs.okey.oneride.model.GeoFence;
import com.labs.okey.oneride.model.User;
import com.labs.okey.oneride.utils.Globals;
import com.labs.okey.oneride.utils.IRecyclerClickListener;
import com.labs.okey.oneride.utils.WAMSVersionTable;
import com.labs.okey.oneride.utils.wamsUtils;
import com.microsoft.windowsazure.mobileservices.MobileServiceList;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceAuthenticationProvider;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceUser;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.microsoft.windowsazure.mobileservices.table.query.Query;
import com.microsoft.windowsazure.mobileservices.table.sync.MobileServiceSyncTable;
import com.microsoft.windowsazure.notifications.NotificationsManager;
import com.pkmmte.view.CircularImageView;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends BaseActivity
        implements WAMSVersionTable.IVersionMismatchListener,
        IRecyclerClickListener {

    static final int            REGISTER_USER_REQUEST = 1;
    private final String        LOG_TAG = getClass().getSimpleName();
    private boolean             mWAMSLogedIn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        try {
            // Needed to detect HashCode for FB registration
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(),
                    PackageManager.GET_SIGNATURES);
            Signature[] signs = packageInfo.signatures;
            for (Signature signature : signs) {
                MessageDigest md;
                md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                String strSignature = new String(Base64.encode(md.digest(), 0));
                Log.d(LOG_TAG, strSignature);
            }
        }
        catch (PackageManager.NameNotFoundException | NoSuchAlgorithmException ex) {
            Log.e(LOG_TAG, ex.toString());
        }

//        PassengerFace pf1 = new PassengerFace("1");
//        PassengerFace pf2 = new PassengerFace("2");
//        PassengerFace pf3 = new PassengerFace("3");
//        PassengerFace pf4 = new PassengerFace("4");
//        Globals.passengerFaces.add(pf1);
//        Globals.passengerFaces.add(pf2);
//        Globals.passengerFaces.add(pf3);
//        Globals.passengerFaces.add(pf4);
//
//        int mDepth = Globals.passengerFaces.size();
//        faceapiUtils.dumpVerificationMatrix(mDepth);
//
//        for(int i = 0; i < mDepth; i++ ) {
//            for (int j = i; j < mDepth; j++) {
//
//                if (i == j)
//                    continue;
//
//                PassengerFace _pf1 = Globals.passengerFaces.get(i);
//                PassengerFace _pf2 = Globals.passengerFaces.get(j);
//
//                float matValue = Globals.verificationMat.get(i, j);
//                if( matValue == 0.0f) {
//                    Globals.verificationMat.set(i, j, 2.0f);
//                    Globals.verificationMat.set(j, i, 2.0f);
//                }
//
//            }
//        }
//
//        faceapiUtils.dumpVerificationMatrix(mDepth);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String accessToken = sharedPrefs.getString(Globals.TOKENPREF, "");
        String accessTokenSecret =  sharedPrefs.getString(Globals.TOKENSECRETPREF, "");

        // Don't confuse with BaseActivity.wamsInit();
        if( !wamsInit() ) {
            login(accessToken, accessTokenSecret);

            NotificationsManager.handleNotifications(this, Globals.SENDER_ID,
                                                    GCMHandler.class);
        }

        setupUI(getString(R.string.title_activity_main), "");

        if( Crashlytics.getInstance() != null)
            Crashlytics.log(Log.VERBOSE, LOG_TAG, getString(R.string.log_start));

        new AsyncTask<Void, Void, Void>() {

            MobileServiceSyncTable<GeoFence> gFencesSyncTable;

            @Override
            protected void onPreExecute() {
                try {
                    gFencesSyncTable = Globals.getMobileServiceClient().getSyncTable("geofences", GeoFence.class);
                }
                catch(Exception ex){
                    Log.e(LOG_TAG, ex.getMessage());
                }
            }

            @Override
            protected Void doInBackground(Void... voids) {

                try {

                    wamsUtils.sync(Globals.getMobileServiceClient(), "geofences");

                    Query query = Globals.getMobileServiceClient().getTable(GeoFence.class).where();

                    MobileServiceList<GeoFence> geoFences = gFencesSyncTable.read(query).get();
                    if(geoFences.getTotalCount() == 0 ) {
                        query = Globals.getMobileServiceClient().getTable(GeoFence.class).where().field("isactive").ne(false);

                        gFencesSyncTable.purge(query);
                        gFencesSyncTable.pull(query).get();

                        Crashlytics.log(Log.VERBOSE, LOG_TAG, getString(R.string.log_gf_updated));
                    } else
                        Crashlytics.log(Log.VERBOSE, LOG_TAG, getString(R.string.log_gf_uptodate));


                } catch(ExecutionException | InterruptedException ex) {
                    Log.e(LOG_TAG, ex.getMessage());
                }

                return null;
            }
        }.execute();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy");

        //NotificationsManager.stopHandlingNotifications(this);
    }

    public Boolean wamsInit(){

        if( mWAMSLogedIn )
            return true;

        if( !wamsUtils.loadUserTokenCache(Globals.getMobileServiceClient(), this) )
            return false;

        return true;
    }

    //
    // Implementation of IVersionMismatchListener
    //
    public void mismatch(int major, int minor, final String url){
        try {

            new MaterialDialog.Builder(this)
                    .title(getString(R.string.new_version_title))
                    .content(getString(R.string.new_version_conent))
                    .iconRes(R.drawable.ic_info)
                    .positiveText(android.R.string.yes)
                    .negativeText(android.R.string.no)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog,
                                            @NonNull DialogAction which) {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse(url));
                            //intent.setDataAndType(Uri.parse(url), "application/vnd.android.package-archive");
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    })
                    .show();
        } catch (Exception e) {
            if( Crashlytics.getInstance() != null)
                Crashlytics.logException(e);

            // better that catch the exception here would be use handle to send events the activity
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    public void match() {

    }

    public void connectionFailure(Exception ex) {

        if( ex != null ) {

            View v = findViewById(R.id.drawer_layout);
            Snackbar.make(v, ex.getMessage(), Snackbar.LENGTH_LONG);
        }

    }

    //
    // Implementation of IRecyclerClickListener
    //
    @Override
    public void clicked(View view, int position) {
        switch( position ) {
            case 1:
                onDriverClicked(view);
                break;

            case 2:
                onPassengerClicked(view);
                break;
        }
    }

    public void onDriverClicked(View v) {
        Globals.clearPassengerFaces();

        Intent intent = new Intent(this, DriverRoleActivity.class);
        startActivity(intent);
    }

    public void onPassengerClicked(View v) {
        Intent intent = new Intent(this, PassengerRoleActivity.class);
        startActivity(intent);
    }

    private void login(String accessToken, String accessTokenSecret) {
        final MobileServiceAuthenticationProvider tokenProvider = getTokenProvider();
        if (tokenProvider == null)
            throw new AssertionError("Token provider cannot be null");

        final JsonObject body = new JsonObject();
        if (tokenProvider == MobileServiceAuthenticationProvider.MicrosoftAccount) {
            body.addProperty("authenticationToken", accessToken);
        } else if (tokenProvider == MobileServiceAuthenticationProvider.Google) {
            body.addProperty("id_token", accessToken);
        } else {
            body.addProperty("access_token", accessToken);
            if (!accessTokenSecret.isEmpty())
                body.addProperty("access_token_secret", accessTokenSecret);
        }

        ListenableFuture<MobileServiceUser> loginFuture =
                Globals.getMobileServiceClient().login(tokenProvider, body);

        Futures.addCallback(loginFuture, new FutureCallback<MobileServiceUser>() {
            @Override
            public void onSuccess(MobileServiceUser mobileServiceUser) {
                cacheUserToken(mobileServiceUser);

                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                String userRegistrationId = sharedPrefs.getString(Globals.USERIDPREF, "");
                updateUserRegistration(userRegistrationId, mobileServiceUser.getUserId());

                mWAMSLogedIn = true;

                if (Answers.getInstance() != null)
                    Answers.getInstance().logLogin(new LoginEvent()
                            .putMethod(tokenProvider.toString())
                            .putSuccess(true));

            }

            @Override
            public void onFailure(Throwable t) {
                Throwable cause = t.getCause();
                String msg = t.getMessage();
                if (cause != null) {
                    msg = cause.getMessage();
                }
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });

    }

    private void cacheUserToken(MobileServiceUser mobileServiceUser) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPrefs.edit();

        editor.putString(Globals.WAMSTOKENPREF, mobileServiceUser.getAuthenticationToken());
        //editor.putString(Globals.USERIDPREF, mobileServiceUser.getUserId());

        editor.apply();
    }

    private void updateUserRegistration(final String registrationId, final String userId) {

        final MobileServiceAuthenticationProvider tokenProvider = getTokenProvider();
        assert (tokenProvider != null);

        if (tokenProvider == MobileServiceAuthenticationProvider.MicrosoftAccount) {

            final MobileServiceTable<User> usersTable = Globals.getMobileServiceClient().getTable("users", User.class);

            Callable<Void> updateUserRegistrationTask = new Callable<Void>() {
                @Override
                public Void call() throws Exception {

                    MobileServiceList<User> _users =
                            usersTable.where()
                                    .field("registration_id").eq(Globals.MICROSOFT_PROVIDER_FOR_STORE + registrationId)
                                    .execute().get();
                    if (_users.size() >= 1) {
                        User _user = _users.get(0);
                        _user.setRegistrationId(userId);

                        usersTable.update(_user);
                    }

                    return null;
                }
            };

            ExecutorService service = Executors.newFixedThreadPool(1);
            ListeningExecutorService executor = MoreExecutors.listeningDecorator(service);
            executor.submit(updateUserRegistrationTask);
        }
    }

    protected void setupUI(String title, String subTitle) {
        super.setupUI(title, subTitle);

        RecyclerView recycler = (RecyclerView)findViewById(R.id.recyclerViewModes);
        if( recycler != null ) {
            recycler.setHasFixedSize(true);
            recycler.setLayoutManager(new LinearLayoutManager(this));
            recycler.setItemAnimator(new DefaultItemAnimator());

            List<FRMode> modes = new ArrayList<>();
            FRMode mode1 = new FRMode();
            mode1.setName(getString(R.string.mode_name_driver));
            mode1.setImageId(R.drawable.driver64);
            modes.add(mode1);
            FRMode mode2 = new FRMode();
            mode2.setName(getString(R.string.mode_name_passenger));
            mode2.setImageId(R.drawable.passenger64);
            modes.add(mode2);

            ModesPeersAdapter adapter = new ModesPeersAdapter(this, modes);
            recycler.setAdapter(adapter);
        }

        try {
            User user = User.load(this);

            final CircularImageView imageAvatar = (CircularImageView) findViewById(R.id.userAvatarView);
            if( imageAvatar == null )
                return;

            // Retrieves an image through Volley
            String pictureURL = user.getPictureURL();
            if( pictureURL.isEmpty() )
                return;

            if( !pictureURL.contains("https") )
                pictureURL = pictureURL.replace("http", "https");
            Cache cache = Globals.volley.getRequestQueue().getCache();
            Cache.Entry entry = cache.get(pictureURL);
            if( entry != null ) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(entry.data, 0, entry.data.length);
                imageAvatar.setImageBitmap(bitmap);
            } else {
                final ImageLoader imageLoader = Globals.volley.getImageLoader();
                imageLoader.get(pictureURL,
                        new ImageLoader.ImageListener() {
                            @Override
                            public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {

                                Bitmap bitmap = response.getBitmap();
                                if (bitmap != null) {
                                    int height = imageAvatar.getHeight();
                                    int width = imageAvatar.getWidth();
                                    imageAvatar.setImageBitmap(bitmap);
                                }
                            }

                            @Override
                            public void onErrorResponse(VolleyError error) {
                                //NetworkResponse response = error.networkResponse;
                                Log.e(LOG_TAG, error.toString());
                            }
                        });
            }

        } catch (Exception e) {
            if( Crashlytics.getInstance() != null)
                Crashlytics.logException(e);

            Log.e(LOG_TAG, e.getMessage());
        }


    }
}
