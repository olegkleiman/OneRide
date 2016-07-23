package com.labs.okey.oneride.gcm;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.labs.okey.oneride.DriverRoleActivity;
import com.labs.okey.oneride.R;
import com.labs.okey.oneride.model.User;
import com.labs.okey.oneride.utils.Globals;
import com.microsoft.windowsazure.mobileservices.MobileServiceList;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.microsoft.windowsazure.notifications.NotificationsHandler;

/**
 * @author Oleg Kleiman
 * created 11-Apr-15.
 */
public class GCMHandler extends NotificationsHandler {

    private final String LOG_TAG = getClass().getSimpleName();

    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onUnregistered(Context context, String gcmRegistrationId) {
        super.onUnregistered(context, gcmRegistrationId);
    }

    @Override
    public void onReceive(final Context context, Bundle bundle) {

        String message = bundle.getString("message");
        if( message == null || message.isEmpty() )
            return;

        String extras = bundle.getString("extras");
        if( extras == null || extras.isEmpty() ) {
            Log.e(LOG_TAG, "Failed to classify push notification");
            return;
        }

        if( extras.equalsIgnoreCase(Globals.PUSH_NOTIFICATION_JOIN) )
            processJoinNotification(context, message);
        else if( extras.equalsIgnoreCase(Globals.PUSH_NOTIFICATION_APPROVAL) )
            processApprovalNotification(context, message);
        else
            Log.e(LOG_TAG, "Unknown push notification");
    }

    private void processApprovalNotification(Context context, String ridecode) {
        String format = context.getString(R.string.push_approval_format);
        String message = String.format(format, ridecode);
        sendNotification(context, message, context.getString(R.string.app_name));
    }

    private void processJoinNotification(final Context context, final String user) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        final boolean allowSamePassengers = sharedPrefs.getBoolean(Globals.PREF_ALLOW_SAME_PASSENGERS, false);

        final MobileServiceTable<User> usersTable = Globals.getMobileServiceClient()
                                                        .getTable("users", User.class);
        ListenableFuture<MobileServiceList<User>> future =
                usersTable.where().field("registration_id").eq(user).execute();
        Futures.addCallback(future, new FutureCallback<MobileServiceList<User>>() {
            @Override
            public void onSuccess(MobileServiceList<User> users) {
                User passenger = users.get(0);

                DriverRoleActivity driverActivity = Globals.getDriverActivity();
                if( driverActivity != null ) {

                    boolean bIsAlreadyJoined = driverActivity.isPassengerJoined(passenger);
                    if( allowSamePassengers )
                        bIsAlreadyJoined = false;

                    if( !bIsAlreadyJoined ) {
                        driverActivity.addPassenger(passenger, allowSamePassengers);

                        String title = context.getResources().getString(R.string.app_label);
                        String format = context.getString(R.string.notification_message_format);
                        String _message = String.format(format, passenger.getFullName());
                        sendNotification(context, _message, title);
                    }
                }
            }
            @Override
            public void onFailure(Throwable t) {
                if( Crashlytics.getInstance() != null)
                    Crashlytics.logException(t);

                Log.e(LOG_TAG, t.getMessage() + " Cause: " + t.getCause());
            }
        });
    }

    private void sendNotification(Context ctx, String msg, String title) {

        NotificationManager mNotificationManager = (NotificationManager)
                ctx.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(ctx)
                        .setSmallIcon(R.mipmap.oneride_notification)
                        .setContentTitle(title)
                        .setAutoCancel(true)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
                        .setContentText(msg);

        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

}
