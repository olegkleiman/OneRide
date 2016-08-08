package com.labs.okey.oneride;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.labs.okey.oneride.utils.Globals;

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

        String fcmToken = FirebaseInstanceId.getInstance().getToken();
        Log.i(TAG, "Refreshed FCM Registration Token: " + fcmToken);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.edit().putString(Globals.FCM_TOKEN_PREF, fcmToken).apply();

    }
}
