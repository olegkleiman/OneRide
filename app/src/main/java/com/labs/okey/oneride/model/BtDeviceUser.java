package com.labs.okey.oneride.model;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author Oleg Kleiman
 * created 09-Jul-16.
 */
public class BtDeviceUser implements Parcelable {

    private BluetoothDevice mBluetoothDevice;

    private String _userName;
    public String get_UserName() {
        return _userName;
    }
    public void set_UserName(String value) {
        _userName = value;
    }

    private String _authProvider;
    public String get_authProvider() {
        return _authProvider;
    }
    public void set_authProvider(String value){
        _authProvider = value;
    }

    private String _userId;
    public String get_UserId() {
        return _userId;
    }
    public void set_UserId(String value) {
        _userId = value;
    }

    private String _rideCode;
    public String get_RideCode() { return  _rideCode; }
    public void set_RideCode(String value) { _rideCode = value; }

    private int _rssi;
    public int get_Rssi() {
        return _rssi;
    }
    public void set_Rssi(int value){
        _rssi = value;
    }

    public BluetoothDevice getDevice() {
        return mBluetoothDevice;
    }

    public int getStatus() {
        return mBluetoothDevice.getBondState();
    }

    public BtDeviceUser(BluetoothDevice device) {
        mBluetoothDevice = device;
    }

    @Override
    public boolean equals(Object object) {

        if( object != null && object instanceof BtDeviceUser ) {
            BtDeviceUser that = (BtDeviceUser)object;
            if( that.get_UserId().equalsIgnoreCase(this.get_UserId()))
                return true;
        }

        return false;
    }

    // Parcelable implementation
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {

    }

    private BtDeviceUser(Parcel in) {
        set_UserName(in.readString());
        set_authProvider(in.readString());
        set_UserId(in.readString());
        set_RideCode(in.readString());
    }

    public static final Creator<BtDeviceUser> CREATOR = new Creator<BtDeviceUser>() {
        public BtDeviceUser createFromParcel(Parcel in) {
            return new BtDeviceUser(in);
        }

        public BtDeviceUser[] newArray(int size) {
            return new BtDeviceUser[size];
        }
    };
}
