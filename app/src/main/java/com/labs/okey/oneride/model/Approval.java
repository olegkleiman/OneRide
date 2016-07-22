package com.labs.okey.oneride.model;

import java.io.Serializable;

/**
 * @author Oleg Kleiman
 * created 23-Jul-16.
 */
public class Approval implements Serializable {
    @com.google.gson.annotations.SerializedName("id")
    public String Id;

    @com.google.gson.annotations.SerializedName("rideid")
    private String rideId;
    public String getRideId() {
        return rideId;
    }
    public void setRideId(String value) { rideId = value; }

    @com.google.gson.annotations.SerializedName("pictureurl")
    private String pictureUrl;
    public String getPictureUrl() {
        return pictureUrl;
    }
    public void setPictureUrl(String value) { pictureUrl = value; }

    @com.google.gson.annotations.SerializedName("driverid")
    private String driverid;
    public String getDriverId() {
        return driverid;
    }
    public void setDriverId(String value){
        driverid = value;
    }
}
