package com.labs.okey.oneride;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.Cache;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.crashlytics.android.Crashlytics;
import com.labs.okey.oneride.adapters.ModesPeersAdapter;
import com.labs.okey.oneride.model.FRMode;
import com.labs.okey.oneride.model.User;
import com.labs.okey.oneride.utils.Globals;
import com.labs.okey.oneride.utils.IRecyclerClickListener;
import com.labs.okey.oneride.utils.WAMSVersionTable;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.pkmmte.view.CircularImageView;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity
        implements WAMSVersionTable.IVersionMismatchListener,
        IRecyclerClickListener {

    static final int            REGISTER_USER_REQUEST = 1;
    private final String        LOG_TAG = getClass().getSimpleName();
    public static MobileServiceClient wamsClient;
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


        setupUI(getString(R.string.title_activity_main), "");

        Crashlytics.log(Log.VERBOSE, LOG_TAG, getString(R.string.log_start));
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
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
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

    }

    public void onPassengerClicked(View v) {

    }

    protected void setupUI(String title, String subTitle) {
        super.setupUI(title, subTitle);

        try {
            User user = User.load(this);

            final CircularImageView imageAvatar = (CircularImageView) findViewById(R.id.userAvatarView);

            // Retrieves an image thru Volley
            String pictureURL = user.getPictureURL();
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
                                if (bitmap != null)
                                    imageAvatar.setImageBitmap(bitmap);
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

        RecyclerView recycler = (RecyclerView)findViewById(R.id.recyclerViewModes);
        recycler.setHasFixedSize(true);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setItemAnimator(new DefaultItemAnimator());

        List<FRMode> modes = new ArrayList<>();
        FRMode mode1 = new FRMode();
        mode1.setName( getString(R.string.mode_name_driver));
        mode1.setImageId(R.drawable.driver64);
        modes.add(mode1);
        FRMode mode2 = new FRMode();
        mode2.setName(getString(R.string.mode_name_passenger));
        mode2.setImageId(R.drawable.passenger64);
        modes.add(mode2);

        ModesPeersAdapter adapter = new ModesPeersAdapter(this, modes);
        recycler.setAdapter(adapter);
    }
}
