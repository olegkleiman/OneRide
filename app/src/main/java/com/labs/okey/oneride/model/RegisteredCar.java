package com.labs.okey.oneride.model;

import android.databinding.BaseObservable;

/**
 * @author Oleg Kleiman
 * created 17-Jun-15
 */
public class RegisteredCar extends BaseObservable {

    private String carNumber;
    public String getCarNumber() {
        return carNumber;
    }
    public void setCarNumber(String value) {
        carNumber = value;
    }

    private String carNick;
    public String getCarNick() {
        return carNick;
    }
    public void setCarNick(String value) {
        carNick = value;
    }

}
