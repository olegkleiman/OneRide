package com.labs.okey.oneride;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.CallSuper;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.labs.okey.oneride.adapters.MyRidesAdapter;
import com.labs.okey.oneride.model.Ride;
import com.labs.okey.oneride.utils.Globals;
import com.labs.okey.oneride.utils.wamsUtils;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.MobileServiceList;
import com.microsoft.windowsazure.mobileservices.table.query.Query;
import com.microsoft.windowsazure.mobileservices.table.sync.MobileServiceSyncTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MyRidesActivity extends BaseActivity
        implements Handler.Callback {

    private final String                    LOG_TAG = getClass().getSimpleName();

    MyRidesAdapter                          mAdapter;
    private List<Ride>                      mRides;
    private MobileServiceSyncTable<Ride>    mRidesSyncTable;

    SwipeRefreshLayout                      mSwipeLayout;
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

        mRides = new ArrayList<>();

        mSwipeLayout = (SwipeRefreshLayout)findViewById(R.id.swipeRefreshLayout);
        mSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                refreshRides();
            }
        });

        RecyclerView recycler = (RecyclerView)findViewById(R.id.recyclerMyRides);
        recycler.setHasFixedSize(true);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.addItemDecoration(new DividerItemDecoration());
        recycler.setItemAnimator(new DefaultItemAnimator());

        mAdapter = new MyRidesAdapter(mRides);
        recycler.setAdapter(mAdapter);

    }

    @Override
    @MainThread
    @CallSuper
    public void onResume() {

        super.onResume();

        refreshRides();
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

        String myUserID = Globals.userID.toLowerCase();

        final Query pullQueryRides = Globals.getMobileServiceClient()
                                            .getTable(Ride.class)
                                            .where()
                                            .field("driverid")
                                            .eq(myUserID);

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

                                    mSwipeLayout.setRefreshing(false);

                                    if( result != null ) {
                                        Globals.__log(LOG_TAG,
                                                String.format(Locale.getDefault(),
                                                        "Pull rides got %d rides", result.getTotalCount()));

                                        mRides.clear();
                                        mRides.addAll(result);

                                        sort();
                                        mAdapter.notifyDataSetChanged();
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
                        mSwipeLayout.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onFailure(Throwable t) {
                Globals.__logException(t);
                mSwipeLayout.setRefreshing(false);
            }
        });

    }

    private void sort(){

        Collections.sort(mRides, Collections.reverseOrder(new Comparator<Ride>() {
            public int compare(Ride r1, Ride r2) {
                if( r1.getCreated() == null
                        || r2.getCreated() == null )
                    return 1;

                return r1.getCreated().compareTo(r2.getCreated());
            }}));

    }

    public void refreshRides() {

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

    /**
     * RecyclerView item decoration - give equal margin around grid item
     */
    public class DividerItemDecoration extends RecyclerView.ItemDecoration {

        private Drawable mDivider;

        public DividerItemDecoration() {

            mDivider = ResourcesCompat.getDrawable(getResources(), R.drawable.line_divider, null);

        }

        @Override
        public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
            int left = parent.getPaddingLeft();
            int right = parent.getWidth() - parent.getPaddingRight();

            int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = parent.getChildAt(i);

                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

                int top = child.getBottom() + params.bottomMargin;
                int bottom = top + mDivider.getIntrinsicHeight();

                mDivider.setBounds(left, top, right, bottom);
                mDivider.draw(c);
            }
        }

    }

}


