package com.labs.okey.oneride.services;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.labs.okey.oneride.DriverRoleActivity;
import com.labs.okey.oneride.R;
import com.labs.okey.oneride.model.User;
import com.labs.okey.oneride.utils.Globals;
import com.microsoft.windowsazure.mobileservices.MobileServiceList;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import io.fabric.sdk.android.Fabric;

public class ChildEventListenerService extends Service {

    private final String    LOG_TAG = getClass().getSimpleName();
    private static final int NOTIFICATION_ID = 1;

    public ChildEventListenerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startid) {
        String rideCode = intent.getStringExtra("ridecode");

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference rideRef = database.getReference("rides").child(rideCode);

        rideRef.child("passengers").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {
                String passengerId = dataSnapshot.getKey();
                Globals.__log(LOG_TAG, "passenger joined. UserId: " + passengerId);

                processJoinNotification(getApplicationContext(), passengerId);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String prevChildKey) {
                String passengerId = dataSnapshot.getKey();
                Map<String, Object> propsMap = (Map<String, Object>)dataSnapshot.getValue();
                Date lastSeen = null;
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.US);
                    String strLastSeen = (String) propsMap.get("last_seen");
                    lastSeen = sdf.parse(strLastSeen);
                } catch(ParseException ex) {
                    lastSeen = new Date();
                }
                updateJoin(passengerId, lastSeen);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Log.i(LOG_TAG, "child removed");
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                Log.i(LOG_TAG, "child moved");
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.i(LOG_TAG, databaseError.getMessage());
            }
        });

        return START_STICKY;
    }

    private void updateJoin(String userId, Date lastSeen) {
        DriverRoleActivity driverActivity = Globals.getDriverActivity();
        if( driverActivity != null ) {

            User passenger = new User();
            passenger.setRegistrationId(userId);
            passenger.setLastSeen(lastSeen);
            boolean bIsAlreadyJoined = driverActivity.isPassengerJoined(passenger);
            if( bIsAlreadyJoined ) {
                driverActivity.updatePassenger(passenger);
            }
        }
    }

    private void processJoinNotification(final Context context, final String userId) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        final boolean allowSamePassengers = sharedPrefs.getBoolean(Globals.PREF_ALLOW_SAME_PASSENGERS, false);

        final MobileServiceTable<User> usersTable = Globals.getMobileServiceClient()
                .getTable("users", User.class);
        ListenableFuture<MobileServiceList<User>> future =
                usersTable.where().field("registration_id").eq(userId).execute();
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
                if( Fabric.isInitialized() && Crashlytics.getInstance() != null)
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
