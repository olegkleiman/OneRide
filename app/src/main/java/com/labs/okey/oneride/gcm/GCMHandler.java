package com.labs.okey.oneride.gcm;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.WorkerThread;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.labs.okey.oneride.DriverRoleActivity;
import com.labs.okey.oneride.R;
import com.labs.okey.oneride.model.PassengerFace;
import com.labs.okey.oneride.model.User;
import com.labs.okey.oneride.model.sc.SCModule;
import com.labs.okey.oneride.model.sc.SCUser;
import com.labs.okey.oneride.utils.Globals;
import com.microsoft.windowsazure.mobileservices.MobileServiceList;
import com.microsoft.windowsazure.mobileservices.notifications.MobileServicePush;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.microsoft.windowsazure.notifications.NotificationsHandler;

import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * @author Oleg Kleiman
 * created 11-Apr-15.
 */
public class GCMHandler extends NotificationsHandler {

    private final String LOG_TAG = getClass().getSimpleName();

    private static final int NOTIFICATION_ID = 1;
    private static final String NOTIFICATIONS_HANDLER_CLASS = "WAMS_NotificationsHandlerClass";

//    @Override
//    public void onRegistered(final Context context,  final String gcmRegistrationId) {
//        super.onRegistered(context, gcmRegistrationId);
//
//        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
//        final String userID = sharedPrefs.getString(Globals.USERIDPREF, "");
//
//        new AsyncTask<Void, Void, Void>() {
//
//            @Override
//            protected Void doInBackground(Void... voids) {
//
//                try {
//                    String[] tags = {userID};
//
//                    final MobileServicePush msp = Globals.getMobileServiceClient().getPush();
//                    if( msp == null ) {
//                        if( Crashlytics.getInstance() != null )
//                            Crashlytics.log(Log.ERROR, LOG_TAG, "Unable to get PUSH");
//
//                        return null;
//                    }
//
//                    ListenableFuture<Void> lf = msp.register(gcmRegistrationId);
//
//                    final User user = User.load(context);
//                    final CustomEvent subscriptionEvent =
//                            new CustomEvent(context.getString(R.string.push_subscription_answer_name));
//
//                    Futures.addCallback(lf, new FutureCallback<Void>() {
//                        @Override
//                        public void onSuccess(Void result) {
//                            subscriptionEvent.putCustomAttribute(context.getString(R.string.push_subscription_attribute), "Success");
//                            if( user != null )
//                                subscriptionEvent.putCustomAttribute("User", user.getFullName());
//                            Answers.getInstance().logCustom(subscriptionEvent);
//
//                            Log.d(LOG_TAG, "Azure Notification Hub registration succeeded");
//                        }
//
//                        @Override
//                        public void onFailure(Throwable t) {
//
//                            try {
//
//                                subscriptionEvent.putCustomAttribute(context.getString(R.string.push_subscription_attribute), "Failed");
//                                if( user != null )
//                                    subscriptionEvent.putCustomAttribute("User", user.getFullName());
//                                Answers.getInstance().logCustom(subscriptionEvent);
//
//                                if( Crashlytics.getInstance() != null )
//                                    Crashlytics.logException(t);
//
//                                verifyStorageVersion(context);
//
//                                //msp.unregister().get();
//                                //msp.unregisterAll(gcmRegistrationId).get();
//                            } catch (Exception ex) {
//                                Log.e(LOG_TAG, ex.getLocalizedMessage());
//                            }
//                        }
//                    });
//
//                } catch (Exception e) {
//
//                    if( Crashlytics.getInstance() != null)
//                        Crashlytics.logException(e);
//
//                    String msg = e.getLocalizedMessage();
//                    Log.e(LOG_TAG, "Registration error: " + msg);
//
//                }
//
//                return null;
//            }
//        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//    }

    @Override
    public void onUnregistered(Context context, String gcmRegistrationId) {
        super.onUnregistered(context, gcmRegistrationId);
    }

    @Override
    public void onReceive(final Context context, Bundle bundle) {

        String message = bundle.getString("message");
        if( message == null || message.isEmpty() )
            return;

        final String user = message;
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        final boolean allowSamePassengers = sharedPrefs.getBoolean(Globals.PREF_ALLOW_SAME_PASSENGERS, false);

        final MobileServiceTable<User> usersTable = Globals.getMobileServiceClient()
                                                    .getTable("users", User.class);

        new AsyncTask<Void, Void, Void>() {

            @Override
            @WorkerThread
            protected Void doInBackground(Void... voids) {

                 try {
                     MobileServiceList<User> users =
                                    usersTable.where().field("registration_id").eq(user).execute().get();
                     if (users.size() > 0) {
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
                 } catch(ExecutionException | InterruptedException ex ){
                        if( Crashlytics.getInstance() != null)
                            Crashlytics.logException(ex);

                        Log.e(LOG_TAG, ex.getMessage() + " Cause: " + ex.getCause());
                 }

                 return null;
             }
        }.execute();
    }

    private void sendNotification(Context ctx, String msg, String title) {
        NotificationManager mNotificationManager = (NotificationManager)
                ctx.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(ctx)
                        .setSmallIcon(R.mipmap.oneride_notification)
                        .setContentTitle(title)
                        .setAutoCancel(true)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(msg))
                        .setContentText(msg);

        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

}
