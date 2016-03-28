package com.labs.okey.oneride;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.labs.okey.oneride.services.GeofenceErrorMessages;
import com.labs.okey.oneride.utils.Globals;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Oleg Kleiman on 31-May-15.
 */
public class GeofenceTransitionsIntentService extends IntentService {

    private static String LOG_TAG = "FR.geofence-service";

    public GeofenceTransitionsIntentService() {
        // Use the TAG to name the worker thread.
        super(LOG_TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    /**
     * Handles incoming intents.
     * @param intent sent by Location Services. This Intent is provided to Location
     *               Services (inside a PendingIntent) when addGeofences() is called.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    geofencingEvent.getErrorCode());
            Log.e(LOG_TAG, errorMessage);
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        String geofenceTransitionString = getTransitionString(geofenceTransition);

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            // Get the geofences that were triggered. A single event can trigger multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            // Get the transition details as a String.
            String geofenceName = getGeofenceTransitionDetails(
                    this,
                    geofenceTransition,
                    triggeringGeofences
            );
            Globals.set_CurrentGeoFenceName(geofenceName);
            geofenceTransitionString += " " + geofenceName;
            Globals.setMonitorStatus(geofenceTransitionString);

            if( geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                Globals.setInGeofenceArea(true);
            } else // if( geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT ){
                Globals.setInGeofenceArea(false);


            // Send notification and log the transition details.
            if( Globals.getRemindGeofenceEntrance() ) {
                sendNotification(geofenceTransitionString);
                Globals.clearRemindGeofenceEntrance();
            }

            Log.i(LOG_TAG, geofenceTransitionString);
        } else if( geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL ) {
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            // Get the transition details as a String.
            String geofenceName = getGeofenceTransitionDetails(
                    this,
                    geofenceTransition,
                    triggeringGeofences
            );
            Globals.set_CurrentGeoFenceName(geofenceName);
            geofenceTransitionString += " " + geofenceName;
            Globals.setMonitorStatus(geofenceTransitionString);
        } else {
            // Log the error.
            Log.e(LOG_TAG, getString(R.string.geofence_transition_invalid_type, geofenceTransition));
        }

    }

    /**
     * Gets transition details and returns them as a formatted string.
     *
     * @param context               The app context.
     * @param geofenceTransition    The ID of the geofence transition.
     * @param triggeringGeofences   The geofence(s) triggered.
     * @return                      The transition details formatted as String.
     */
    private String getGeofenceTransitionDetails(
            Context context,
            int geofenceTransition,
            List<Geofence> triggeringGeofences) {

        // Get the Ids of each geofence that was triggered.
        List<String> triggeringGeofencesIdsList = new ArrayList<String>();
        for (Geofence geofence : triggeringGeofences) {
            String requestId = geofence.getRequestId();
            String[] tokens = requestId.split(":");

            // Add if not already added
            if( !triggeringGeofencesIdsList.contains(tokens[0]) )
                triggeringGeofencesIdsList.add(tokens[0]);
        }
        return TextUtils.join(", ", triggeringGeofencesIdsList);

    }

    /**
     * Posts a notification in the notification bar when a transition is detected.
     * If the user clicks the notification, control goes to the MainActivity.
     */
    private void sendNotification(String notificationDetails) {
        // Create an explicit content Intent that starts the main Activity.
        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);

        // This stack builder object will contain an artificial back stack for the PassengerRoleActivity
        // This ensures that navigating backward from it leads out of the application to the Start screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        // Add the main Activity to the task stack as the parent.
        stackBuilder.addParentStack(MainActivity.class);

        // Push the content Intent onto the stack.
        stackBuilder.addNextIntent(notificationIntent);

        // Get a PendingIntent containing the entire back stack.
        PendingIntent notificationPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        // Get a notification builder that's compatible with platform versions >= 4
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        String title = getString(R.string.app_label);
        // Define the notification settings.
        builder.setSmallIcon(R.mipmap.ic_launcher2)
                // In a real app, you may want to use a library like Volley
                // to decode the Bitmap.
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        R.mipmap.ic_launcher2))
                .setColor(Color.RED)
                .setContentTitle(title)
                .setContentText(notificationDetails)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(title))
                .setContentIntent(notificationPendingIntent);

        // Dismiss notification once the user touches it.
        builder.setAutoCancel(true);

        // Get an instance of the Notification manager
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Issue the notification
        mNotificationManager.notify(0, builder.build());

    }

    /**
     * Maps geofence transition types to their human-readable equivalents.
     *
     * @param transitionType    A transition type constant defined in Geofence
     * @return                  A String indicating the type of transition
     */
    private String getTransitionString(int transitionType) {
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return getString(R.string.geofence_transition_entered);
            case Geofence.GEOFENCE_TRANSITION_DWELL:
                return getString(R.string.geofence_transition_dwell);
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return getString(R.string.geofence_transition_exited);
            default:
                return getString(R.string.unknown_geofence_transition);
        }
    }
}
