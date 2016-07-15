package com.labs.okey.oneride;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.iid.InstanceIDListenerService;

/**
 * @author Oleg Kleiman
 * created 15-Jul-16.
 */
public class AzureInstanceIDService extends InstanceIDListenerService {

    private static final String TAG = "AzureInstanceIDService";

    @Override
    public void onTokenRefresh() {

        Log.i(TAG, "Refreshing GCM Registration Token");

        Intent intent = new Intent(this, AzureRegistrationIntentService.class);
        startService(intent);
    }
}
