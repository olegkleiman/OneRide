package com.labs.okey.oneride.services;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.labs.okey.oneride.model.BtDeviceUser;
import com.labs.okey.oneride.utils.Globals;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PassengerCIService extends Service {

    private final String                LOG_TAG = getClass().getSimpleName();
    private SharedPreferences           mSharedPrefs = null;
    private BluetoothAdapter            mBluetoothAdapter;
    private String                      mDriverId;
    private String                      mPassengerID;
    private String                      mRideCode;

    private final BroadcastReceiver mBtReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            int currentRssiLevel = Globals.DEFAULT_RSSI_LEVEL;

            if( mSharedPrefs!= null )
                currentRssiLevel = mSharedPrefs.getInt(Globals.PREF_RSSI_LEVEL, Globals.DEFAULT_RSSI_LEVEL);

            if( BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action) ) {
                renewBtDiscovery();
            }

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                BtDeviceUser btDeviceUser = btAnalyzeDevice(device);
                if( btDeviceUser != null ) {

                    int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                    btDeviceUser.set_Rssi(rssi);

                    if( rssi >= -(currentRssiLevel) ) { // these are negative values

                        if (btDeviceUser.get_UserId().equals(mDriverId)) {
                            FirebaseDatabase database = FirebaseDatabase.getInstance();
                            DatabaseReference ridesRef = database.getReference("rides");

                            DatabaseReference passengerRef = ridesRef.child(mRideCode)
                                    .child("passengers")
                                    .child(mPassengerID);
                            SimpleDateFormat simpleDate = new SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.US);
                            String dt = simpleDate.format(new Date());
                            Map<String, Object> lastSeenMap = new HashMap<>();
                            lastSeenMap.put("last_seen", dt);
                            passengerRef.updateChildren(lastSeenMap);
                        }
                    }
                }
            }
        }
    };

    private BtDeviceUser btAnalyzeDevice(BluetoothDevice device){
        if( device == null )
            return null;

        String deviceName = device.getName();
        if( deviceName == null || deviceName.isEmpty() ) {
            Globals.__log(LOG_TAG, "Device name is empty");
            return null;
        }

        BluetoothClass bluetoothClass = device.getBluetoothClass();
        if( bluetoothClass == null )
            return null;

        int deviceClass = bluetoothClass.getMajorDeviceClass();

        if( deviceClass != BluetoothClass.Device.Major.PHONE
                && deviceClass != BluetoothClass.Device.Major.COMPUTER) {
            Globals.__log(LOG_TAG, "Device " + deviceName + "is not PHONE nor COMPUTER");
            return null;
        }

        String[] tokens = deviceName.split(Globals.BT_DELIMITER);
        if( tokens.length != 3)
            return null;

        String authProvider = tokens[0];
        if( !Globals.GOOGLE_PROVIDER.equalsIgnoreCase(authProvider) &&
                !Globals.FB_PROVIDER.equalsIgnoreCase(authProvider)){
            Globals.__log(LOG_TAG, "Unrecognized provider: " + authProvider);
            return null;
        } else
            Globals.__log(LOG_TAG, "Provider: " + authProvider);

        String userId = tokens[1];
        Globals.__log(LOG_TAG, "User registration id: " + userId);

        String rideCode = tokens[2];
        Globals.__log(LOG_TAG, "Ride code: " + rideCode);

        BtDeviceUser btDeviceUser = new BtDeviceUser(device);
        btDeviceUser.set_authProvider(authProvider);
        btDeviceUser.set_UserId(userId);
        btDeviceUser.set_RideCode(rideCode);

        return btDeviceUser;
    }

    public PassengerCIService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void renewBtDiscovery() {

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if( !mBluetoothAdapter.isDiscovering() )
            mBluetoothAdapter.startDiscovery ();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startid) {

        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        mDriverId = intent.getStringExtra("driverId");
        mPassengerID = intent.getStringExtra("passengerId");
        mRideCode = intent.getStringExtra("rideCode");

        // Register the BroadcastReceiver for BT intents
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        registerReceiver(mBtReceiver, filter);

        renewBtDiscovery();

        return START_STICKY;
    }
}
