package com.labs.okey.oneride.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.UUID;

/**
 * Created by Oleg Kleiman on 8/24/15.
 */
public class PassengerFace implements Parcelable {

    private String faceId;
    public void setFaceId(String value) {
        faceId = value;
    }
    public UUID getFaceId() {
       return  UUID.fromString(faceId);
    }

    String pictureUrl;
    public void setPictureUrl(String value) { pictureUrl = value; }
    public String getPictureUrl() { return pictureUrl; }

    public PassengerFace(String _faceId){
        this.faceId = _faceId;
    }

    public PassengerFace() {

    }

    public boolean isInitialized(){
        return ( faceId != null );
    }

    //
    // Implementation of Parcelable
    //

    private PassengerFace(Parcel in) {
        setFaceId( in.readString() );
        setPictureUrl(  in.readString() );
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(faceId);
        parcel.writeString(pictureUrl);
    }

    public static final Creator<PassengerFace> CREATOR = new Creator<PassengerFace>() {
        public PassengerFace createFromParcel(Parcel in) {
            return new PassengerFace(in);
        }

        public PassengerFace[] newArray(int size) {
            return new PassengerFace[size];
        }
    };
}
