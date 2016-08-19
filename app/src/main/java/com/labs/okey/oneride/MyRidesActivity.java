package com.labs.okey.oneride;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.CallSuper;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.labs.okey.oneride.adapters.MyRideTabAdapter;
import com.labs.okey.oneride.model.Ride;
import com.labs.okey.oneride.utils.Globals;
import com.labs.okey.oneride.utils.wamsUtils;
import com.labs.okey.oneride.views.SlidingTabLayout;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.MobileServiceList;
import com.microsoft.windowsazure.mobileservices.table.query.Query;
import com.microsoft.windowsazure.mobileservices.table.sync.MobileServiceSyncTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MyRidesActivity extends BaseActivity
        implements ActionBar.TabListener,
                    Handler.Callback {

    private final String                    LOG_TAG = getClass().getSimpleName();

    private MyRideTabAdapter                mTabAdapter;
    private List<Ride>                      mRides;
    private MobileServiceSyncTable<Ride>    mRidesSyncTable;

    SlidingTabLayout                        slidingTabLayout;
    String[]                                titles;
    ViewPager                               mViewPager;
    MaterialDialog                          mOfflineDialog;

    private Handler handler = new Handler(this);
    public Handler getHandler() {
        return handler;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_rides);

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

        if (Globals.userID == null ||
                Globals.userID.isEmpty()) {
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            Globals.userID = sharedPrefs.getString(Globals.USERIDPREF, "");
        }

        MobileServiceClient wamsClient = Globals.getMobileServiceClient();
        if( wamsClient == null )
            Globals.initMobileServices(this);

        wamsUtils.sync(Globals.getMobileServiceClient(), "rides");
        mRidesSyncTable = Globals.getMobileServiceClient().getSyncTable("rides", Ride.class);

        setupUI(getString(R.string.subtitle_activity_my_rides), "");

        titles = getResources().getStringArray(R.array.my_rides_titles);

        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        slidingTabLayout = (SlidingTabLayout) findViewById(R.id.sliding_tabs);

        mTabAdapter = new MyRideTabAdapter(getSupportFragmentManager(),
                                          titles, mRides);
        mViewPager.setAdapter(mTabAdapter);

        slidingTabLayout.setViewPager(mViewPager);
        slidingTabLayout.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {
            @Override
            public int getIndicatorColor(int position) {
                return R.color.ColorAccent;
            }
        });

        mRides = new ArrayList<Ride>();

    }

    @Override
    @MainThread
    @CallSuper
    public void onResume() {

        super.onResume();

        if( !isConnectedToNetwork() ) {
            mOfflineDialog.show();
        }
        else {

            if (mOfflineDialog != null && mOfflineDialog.isShowing())
                mOfflineDialog.dismiss();

            updateHistory();
        }
    }

    //
    // Implementation of Handler.Callback
    //
    @Override
    public boolean handleMessage(Message msg) {
        return true;
    }

    private void setUpdateRequired(Boolean value) {
        Globals.myRides_update_required = value;
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putBoolean(Globals.UPDATE_MYRIDES_REQUIRED, Globals.myRides_update_required);
        editor.apply();
    }

    public void updateHistory(){

        final ProgressBar progressBar = (ProgressBar)findViewById(R.id.myrides_progress_refresh);
        progressBar.setVisibility(View.VISIBLE);

        String myUserID = Globals.userID.toLowerCase();

        final Query pullQueryRides = Globals.getMobileServiceClient()
                                            .getTable(Ride.class)
                                            .where()
                                            .field("driverid")
                                            .eq(myUserID)
                                            .top(50);

        ListenableFuture pullFuture = mRidesSyncTable.pull(pullQueryRides);
        Futures.addCallback(pullFuture, new FutureCallback<Object>() {
            @Override
            public void onSuccess(Object result) {

                setUpdateRequired(false);

                ListenableFuture<MobileServiceList<Ride>> readRidesFuture =
                            mRidesSyncTable.read(pullQueryRides);
                Futures.addCallback(readRidesFuture, new FutureCallback<MobileServiceList<Ride>>() {
                    @Override
                    public void onSuccess(final MobileServiceList<Ride> result) {

                          runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setVisibility(View.GONE);

                                    if( result != null ) {
                                        Globals.__log(LOG_TAG,
                                                String.format(Locale.getDefault(),
                                                        "Pull rides got %d rides", result.getTotalCount()));

                                        mRides = result;
                                        mTabAdapter.updateRides(mRides);
                                    }
                                    else {
                                        Globals.__log(LOG_TAG, "No results from pull rides");
                                    }
                                }
                          });
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        Globals.__logException(t);
                        progressBar.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onFailure(Throwable t) {
                Globals.__logException(t);
                progressBar.setVisibility(View.GONE);
            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_my_rides, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_refresh_history) {
            onRefresh();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onRefresh() {


        if( !isConnectedToNetwork() ) {
            mOfflineDialog.show();
        }
        else {

            if (mOfflineDialog != null && mOfflineDialog.isShowing())
                mOfflineDialog.dismiss();

            Globals.myRides_update_required = true;
            updateHistory();
        }

    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, android.support.v4.app.FragmentTransaction fragmentTransaction) {

    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, android.support.v4.app.FragmentTransaction fragmentTransaction) {

    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, android.support.v4.app.FragmentTransaction fragmentTransaction) {

    }

}


