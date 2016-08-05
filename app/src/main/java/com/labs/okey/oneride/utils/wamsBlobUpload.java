package com.labs.okey.oneride.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.labs.okey.oneride.R;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;

/**
 * Created by Oleg Kleiman on 18-Aug-15.
 */
public class wamsBlobUpload extends AsyncTask<File, Void, Void> {

    private final String    LOG_TAG = getClass().getSimpleName();

    URI publishedUri;
    Exception error;

    Context mContext;
    String  mContainerName;
    IUploader mUrlUpdater;

    ProgressDialog mProgressDialog;


    public wamsBlobUpload(Context ctx, String containerName){

        mContainerName = containerName;

        mContext = ctx;
        if( ctx instanceof IUploader)
            mUrlUpdater = (IUploader)ctx;
    }

    @Override
    protected void onPreExecute() {
        mProgressDialog = ProgressDialog.show(mContext,
                                    mContext.getString(R.string.detection_store),
                                    mContext.getString(R.string.detection_wait));
    }


    @Override
    protected void onPostExecute(Void result) {

        if( (mProgressDialog != null) && mProgressDialog.isShowing() )
            mProgressDialog.dismiss();

        if( mUrlUpdater != null )
            mUrlUpdater.update(publishedUri.toString());
    }


    @Override
    protected Void doInBackground(File... params) {

        File photoFile = params[0];

        try {
            CloudStorageAccount storageAccount = CloudStorageAccount.parse(Globals.storageConnectionString);
            CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
            CloudBlobContainer container = blobClient.getContainerReference(mContainerName);

            String fileName = photoFile.getName();

            CloudBlockBlob blob = container.getBlockBlobReference(fileName);
            blob.upload(new FileInputStream(photoFile), photoFile.length());

            publishedUri = blob.getQualifiedUri();

        } catch (URISyntaxException | InvalidKeyException
                | IOException | StorageException e) {
            error = e;
            Log.e(LOG_TAG, e.getMessage());
        }

        return null;
    }
}
