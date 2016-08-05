package com.labs.okey.oneride.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.Log;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.labs.okey.oneride.R;
import com.labs.okey.oneride.model.Join;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;

import java.util.Date;
import java.util.concurrent.ExecutionException;

/**
 * Created by Oleg on 18-Aug-15.
 */
public class wamsPictureURLUpdater extends AsyncTask<String, Void, Void> {

    private final String    LOG_TAG = getClass().getSimpleName();

    Context mContext;
    IUploader mUrlUpdater;

    MobileServiceTable<Join> mJoinsTable;
    Exception error;

    ProgressDialog mProgressDialog;

    private String mAndroidId;

    public wamsPictureURLUpdater(Context ctx) {
        mContext = ctx;

        if( ctx instanceof IUploader)
            mUrlUpdater = (IUploader)ctx;
    }

    @Override
    protected void onPreExecute() {


        mAndroidId = Settings.Secure.getString(mContext.getContentResolver() ,
                                               Settings.Secure.ANDROID_ID);

        mProgressDialog = ProgressDialog.show(mContext,
                mContext.getString(R.string.detection_update),
                mContext.getString(R.string.detection_wait));


        try {
            mJoinsTable = Globals.getMobileServiceClient().getTable("joins", Join.class);

        } catch (Exception e) {
            error = e;
        }
    }

    @Override
    protected void onPostExecute(Void result) {

        if( (mProgressDialog != null) && mProgressDialog.isShowing() )
            mProgressDialog.dismiss();

        if( error != null ) {
            new MaterialDialog.Builder(mContext)
                    .title(mContext.getString(R.string.save_error))
                    .content(error.getMessage())
                    .positiveText(android.R.string.ok)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog,
                                            @NonNull DialogAction which) {
                            if (mUrlUpdater != null)
                                mUrlUpdater.finished(0, false);
                        }
                    })
                    .show();
        } else {
            if (mUrlUpdater != null)
                mUrlUpdater.finished(0, true);
        }
    }

    @Override
    protected Void doInBackground(String... params) {

        String pictureURL = params[0];
        String rideCode = params[1];
        String faceId = params[2];

        try {

            Join _join = new Join();
            _join.setWhenJoined(new Date());
            _join.setRideCode(rideCode);
            _join.setDeviceId(mAndroidId);
            _join.setPictureURL(pictureURL);
            _join.setFaceId(faceId);

            mJoinsTable.insert(_join).get();

        } catch (InterruptedException | ExecutionException e) {
            Log.e(LOG_TAG, e.getMessage());
            error = e;
        }

        return null;
    }
}
