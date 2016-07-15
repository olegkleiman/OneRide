package com.labs.okey.oneride;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.google.firebase.iid.FirebaseInstanceId;
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

        String resultString = null;
        String regID = null;

        try {

            FirebaseInstanceId instanceID = FirebaseInstanceId.getInstance();

            String token = instanceID.getToken(Globals.SENDER_ID,
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE);
            Log.i(LOG_TAG, "Got FCM Registration Token: " + token);

            NotificationHub hub = new NotificationHub(Globals.AZURE_HUB_NAME,
                                                      Globals.AZURE_HUB_CONNECTION_STRING, this);
            regID = hub.register(token).getRegistrationId();

            // If you want to use tags...
            // Refer to : https://azure.microsoft.com/en-us/documentation/articles/notification-hubs-routing-tag-expressions/
            // regID = hub.register(token, "tag1,tag2").getRegistrationId();

            resultString = "Registered Successfully - RegId : " + regID;
            Log.i(LOG_TAG, resultString);

        } catch(Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }
}
