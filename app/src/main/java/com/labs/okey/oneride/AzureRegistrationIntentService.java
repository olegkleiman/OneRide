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
        String storedToken = null;

        try {

            String FCM_token  = FirebaseInstanceId.getInstance().getToken();
            Log.d(LOG_TAG, "Got FCM Registration Token: " + FCM_token );

            if (((regID=sharedPreferences.getString(Globals.NH_REGISTRATION_ID_PREF, null)) == null)){

                NotificationHub hub = new NotificationHub(Globals.AZURE_HUB_NAME,
                        Globals.AZURE_HUB_CONNECTION_STRING, this);
                //regID = hub.register(token).getRegistrationId();

                // If you want to use tags...
                // Refer to : https://azure.microsoft.com/en-us/documentation/articles/notification-hubs-routing-tag-expressions/
                User user = User.load(getApplicationContext());
                String tag = user.getRegistrationId();
                regID = hub.register(FCM_token, tag).getRegistrationId();

                resultString = "Registered Successfully - RegId : " + regID;
                Log.d(LOG_TAG, resultString);

                sharedPreferences.edit().putString(Globals.NH_REGISTRATION_ID_PREF, regID).apply();
                sharedPreferences.edit().putString(Globals.FCM_TOKEN_PREF, FCM_token).apply();
            }
            else if ((storedToken=sharedPreferences.getString(Globals.FCM_TOKEN_PREF, "")) != FCM_token) {

                NotificationHub hub = new NotificationHub(Globals.AZURE_HUB_NAME,
                                                          Globals.AZURE_HUB_CONNECTION_STRING, this);
                Log.d(LOG_TAG, "NH Registration refreshing with token : " + FCM_token);
                User user = User.load(getApplicationContext());
                String tag = user.getRegistrationId();
                regID = hub.register(FCM_token, tag).getRegistrationId();

                sharedPreferences.edit().putString(Globals.NH_REGISTRATION_ID_PREF, regID ).apply();
                sharedPreferences.edit().putString(Globals.FCM_TOKEN_PREF, FCM_token ).apply();
            }


        } catch(Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }
}
