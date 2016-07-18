package com.labs.okey.oneride.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;

import com.labs.okey.oneride.utils.Globals;

/**
 * @author Oleg Kleiman
 * created 11-Apr-15.
 */
public class User implements Parcelable {

    @com.google.gson.annotations.SerializedName("id")
    public String Id;

    @com.google.gson.annotations.SerializedName("first_name")
    private String first_name;
    public String getFirstName() {
        return first_name;
    }
    public void setFirstName(String value) {
        first_name = value;
    }

    @com.google.gson.annotations.SerializedName("last_name")
    private String last_name;
    public String getLastName() {
        return last_name;
    }
    public void setLastName(String value){
        last_name = value;
    }

    public String getFullName() { return first_name + " " + last_name;}
    public void setFullName(String val) {
        String[] tokens = val.split(" ");
        first_name = tokens[0];
        if( tokens.length > 1) {
            last_name = tokens[1];
        }
    }

    @com.google.gson.annotations.SerializedName("registration_id")
    private String registration_id;
    public String getRegistrationId() { return registration_id; }
    public void setRegistrationId(String value) { registration_id = value; }

    @com.google.gson.annotations.SerializedName("picture_url")
    private String picture_url;
    public String getPictureURL() { return this.picture_url; }
    public void setPictureURL(String value) { this.picture_url = value; }

    @com.google.gson.annotations.SerializedName("email")
    private String email;
    public String getEmail() { return this.email; }
    public void setEmail(String value) { this.email = value; }

    @com.google.gson.annotations.SerializedName("phone")
    private String phone;
    public String getPhone() { return this.phone; }
    public void setPhone(String value) { this.phone = value; }

    @com.google.gson.annotations.SerializedName("use_phone")
    private Boolean usePhone = true;
    public Boolean getUsePhone() { return this.usePhone; }
    public void setUsePhone(Boolean value) { this.usePhone = value; }

    @com.google.gson.annotations.SerializedName("platform")
    private String platform;
    public String getPlatform() { return this.platform; }
    public void setPlatform(String value) { this.platform = value; }

    @com.google.gson.annotations.SerializedName("device_id")
    private String deviceId;
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String value) { this.deviceId = value; }

    // This is not persistent property
    private boolean _wasSelfPictured = false;
    public boolean wasSelfPictured() {
        return _wasSelfPictured;
    }
    public void setSelfPictured(boolean value){
        _wasSelfPictured = value;
    }

    @com.google.gson.annotations.SerializedName("confirmation_code")
    private String confirmationCode;
    public String getConfiramtionCode() { return confirmationCode; }
    public void setConfirmationCode(String code) { confirmationCode = code; }


    public boolean compare(User _user) {
        return this.registration_id.compareTo(_user.registration_id) == 0
                && this.deviceId.compareTo(_user.deviceId) == 0;
    }

    public User() {

    }

    public static User load(Context context) {

        User _user = new User();

        SharedPreferences sharedPrefs =
                PreferenceManager.getDefaultSharedPreferences(context);
        _user.setFirstName(sharedPrefs.getString(Globals.FIRST_NAME_PREF, ""));
        _user.setLastName(sharedPrefs.getString(Globals.LAST_NAME_PREF, ""));
        _user.setRegistrationId(sharedPrefs.getString(Globals.REG_ID_PREF, ""));
        _user.setPictureURL(sharedPrefs.getString(Globals.PICTURE_URL_PREF, ""));
        _user.setEmail(sharedPrefs.getString(Globals.EMAIL_PREF, ""));
        _user.setPhone(sharedPrefs.getString(Globals.PHONE_PREF, ""));
        _user.setUsePhone(sharedPrefs.getBoolean(Globals.USE_PHONE_PFER, false));

        return _user;
    }

    public boolean isLoaded() {
        return !this.getRegistrationId().isEmpty();
    }

    public void save(Context context) {
        SharedPreferences sharedPrefs =
                PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPrefs.edit();

        editor.putString(Globals.FIRST_NAME_PREF, this.getFirstName());
        editor.putString(Globals.LAST_NAME_PREF, this.getLastName());
        editor.putString(Globals.REG_ID_PREF, this.getRegistrationId());
        editor.putString(Globals.PICTURE_URL_PREF, this.getPictureURL());
        editor.putString(Globals.EMAIL_PREF, this.getEmail());
        editor.putString(Globals.PHONE_PREF, this.getPhone());
        editor.putBoolean(Globals.USE_PHONE_PFER, this.getUsePhone());

        editor.apply();
    }

    //
    // Implementation of Parcelable
    //

    private User(Parcel in) {
        setFirstName( in.readString() );
        setLastName(  in.readString() );
        setRegistrationId( in.readString() );
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(getFirstName());
        parcel.writeString(getLastName());
        parcel.writeString(getRegistrationId());
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        public User[] newArray(int size) {
            return new User[size];
        }
    };
}
