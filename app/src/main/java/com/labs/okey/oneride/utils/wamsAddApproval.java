package com.labs.okey.oneride.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Display;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.labs.okey.oneride.R;
import com.labs.okey.oneride.model.Approval;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;

import net.steamcrafted.loadtoast.LoadToast;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;

import io.fabric.sdk.android.Fabric;

/**
 * @author eli max
 * created 23/10/2015.
 */

public class wamsAddApproval extends AsyncTask<File, Void, Void> {

    private final String    LOG_TAG = getClass().getSimpleName();

    URI                                 publishedUri;
    Exception                           error;
    String                              mRideID;
    String                              mDriverID;
    String                              mDriverName;
    int                                 mEmojiId;
    Context                             mContext;
    String                              mContainerName;
    IUploader                           mUploader;
    Approval                            mCurrentApproval;

    LoadToast lt;

    public static final String storageConnectionString =
            "DefaultEndpointsProtocol=https;AccountName=oneride;" +
                    "AccountKey=bdNIAOimN48pj29UmMQRgo5UK5a29cyJ3HnTM5Ikc4HzI7/DUOpxclfedehnQ/D7uSFEm8YOtcUyxUiSKpDqvw==";

    public wamsAddApproval(Context ctx,
                           String driverName,
                           String containerName,
                           String rideID,
                           String driverID,
                           int emojiId){

        mContainerName = containerName;
        mRideID = rideID;
        mDriverID = driverID;
        mDriverName = driverName;
        mEmojiId = emojiId;

        mContext = ctx;
        if( ctx instanceof IUploader )
            mUploader = (IUploader)ctx;
    }

    @Override
    protected void onPreExecute() {
        lt = new LoadToast(mContext);
        lt.setText(mContext.getString(R.string.processing));

        Display display = ((Activity)mContext).getWindow().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        lt.setTranslationY(size.y / 2);
        lt.show();
    }


    @Override
    protected void onPostExecute(Void result) {

        if( error == null ) {

            lt.success();

            CustomEvent requestEvent = new CustomEvent(mContext.getString(R.string.approval_answer_name));
            requestEvent.putCustomAttribute("User", mDriverName);

            if( Fabric.isInitialized() )
             Answers.getInstance().logCustom(requestEvent);

            new MaterialDialog.Builder(mContext)
                    .title(mContext.getString(R.string.approval_send_title))
                    .content(mContext.getString(R.string.approval_send_success))
                    .iconRes(R.drawable.ic_info)
                    .positiveText(android.R.string.ok)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog,
                                            @NonNull DialogAction which) {
                            mUploader.finished(Globals.APPROVAL_UPLOAD_TASK_TAG, true);
                        }
                    })
                    .show();
        }
        else {
            lt.error();

            if( ((Activity)mContext).hasWindowFocus() ) { // User may leave parent window for a meanwhile

                new MaterialDialog.Builder(mContext)
                        .title(mContext.getString(R.string.approval_nosend_title))
                        .content(error.getMessage())
                        .positiveText(mContext.getString(R.string.approval_another_picture))
                        .onPositive(new MaterialDialog.SingleButtonCallback(){
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog,
                                                @NonNull DialogAction which) {
                                // TODO
                            }
                        })
                        .iconRes(R.drawable.ic_exclamation)
                        .show();
            }
        }

    }

    @Override
    protected Void doInBackground(File... params) {

        File photoFile = params[0];

        try {
            CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
            CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
            CloudBlobContainer container = blobClient.getContainerReference(mContainerName );

            String fileName = photoFile.getName();

            CloudBlockBlob blob = container.getBlockBlobReference(fileName);
            blob.upload(new FileInputStream(photoFile), photoFile.length());

            publishedUri = blob.getQualifiedUri();

            Approval approval = new Approval();
            approval.setRideId(mRideID);
            approval.setPictureUrl(publishedUri.toString());
            approval.setDriverId(mDriverID);
            if( mEmojiId != 0 )
                approval.setEmojiId(mEmojiId);

            MobileServiceTable<Approval> wamsApprovalsTable = Globals.getMobileServiceClient().getTable("approvals", Approval.class);
            mCurrentApproval = wamsApprovalsTable.insert(approval).get();

        } catch (Exception e) {
            error = e;
            Log.e(LOG_TAG, e.getMessage());
        }

        return null;
    }

}
