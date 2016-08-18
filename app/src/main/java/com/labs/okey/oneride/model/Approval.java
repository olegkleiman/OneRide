package com.labs.okey.oneride.model;

import com.google.gson.annotations.Expose;

import java.io.Serializable;

/**
 * @author Oleg Kleiman
 * created 23-Jul-16.
 */
public class Approval implements Serializable {

    @Expose
    @com.google.gson.annotations.SerializedName("id")
    public String Id;

    @Expose
    @com.google.gson.annotations.SerializedName("rideid")
    private String rideId;
    public String getRideId() {
        return rideId;
    }
    public void setRideId(String value) { rideId = value; }

    @Expose
    @com.google.gson.annotations.SerializedName("pictureurl")
    private String pictureUrl;
    public String getPictureUrl() {
        return pictureUrl;
    }
    public void setPictureUrl(String value) { pictureUrl = value; }

    @Expose
    @com.google.gson.annotations.SerializedName("driverid")
    private String driverid;
    public String getDriverId() {
        return driverid;
    }
    public void setDriverId(String value){
        driverid = value;
    }

    @Expose
    @com.google.gson.annotations.SerializedName("emoji_id")
    private Integer emojiId;
    public Integer getEmojiId() {
        return emojiId;
    }
    public void setEmojiId(Integer value) {
        emojiId = value;
    }
}
