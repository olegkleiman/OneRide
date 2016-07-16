package com.labs.okey.oneride;

import android.content.Intent;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceIdService;

/**
 * @author Oleg Kleiman
 * created 15-Jul-16.
 */
public class AzureInstanceIDService extends FirebaseInstanceIdService {

    private final String TAG = getClass().getSimpleName();

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is also called
     * when the InstanceID token is initially generated, so this is where
     * you retrieve the token.
     */
    // [START refresh_token]
    @Override
    public void onTokenRefresh() {

        Log.i(TAG, "Refreshing FCM Registration Token");

        Intent intent = new Intent(this, AzureRegistrationIntentService.class);
        startService(intent);
    }
}
