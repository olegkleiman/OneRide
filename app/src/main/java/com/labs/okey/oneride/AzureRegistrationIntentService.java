package com.labs.okey.oneride;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.labs.okey.oneride.model.User;
import com.labs.okey.oneride.utils.Globals;
import com.microsoft.windowsazure.messaging.NotificationHub;

/**
 * @author Oleg Kleiman
 * created 15-Jul-16.
 */
public class AzureRegistrationIntentService extends IntentService {

    private final String        LOG_TAG = getClass().getSimpleName();
    private NotificationHub     hub;

    public AzureRegistrationIntentService() {
        super("AzureRegistrationIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        String resultString = null;
        String regID = null;

        try {

            String fcmToken  = sharedPreferences.getString(Globals.FCM_TOKEN_PREF, null);
            Log.d(LOG_TAG, "Read FCM Registration Token: " + fcmToken);

            if( fcmToken == null && fcmToken.isEmpty() ) {
                //FirebaseInstanceId.getInstance().deleteInstanceId();
                fcmToken = FirebaseInstanceId.getInstance().getToken();
                sharedPreferences.edit().putString(Globals.FCM_TOKEN_PREF, fcmToken).apply();
            }

            if (((regID=sharedPreferences.getString(Globals.NH_REGISTRATION_ID_PREF, null)) == null)){

                NotificationHub hub = new NotificationHub(Globals.AZURE_HUB_NAME,
                        Globals.AZURE_HUB_CONNECTION_STRING, this);
                //regID = hub.register(token).getRegistrationId();

                // If you want to use tags...
                // Refer to : https://azure.microsoft.com/en-us/documentation/articles/notification-hubs-routing-tag-expressions/
                User user = User.load(getApplicationContext());
                String tag = user.getRegistrationId();
                regID = hub.register(fcmToken, tag).getRegistrationId();

                resultString = "Registered Successfully - RegId : " + regID;
                Log.d(LOG_TAG, resultString);

                sharedPreferences.edit().putString(Globals.NH_REGISTRATION_ID_PREF, regID).apply();
            }

        } catch(Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }
}
