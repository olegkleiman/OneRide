package com.labs.okey.oneride.model;

import android.databinding.BaseObservable;
import android.databinding.Bindable;

import com.labs.okey.oneride.BR;

/**
 * @author Oleg Kleiman
 * created 05-Apr-17.
 */

public class PassengerState extends BaseObservable {

    private String text;
    @Bindable
    public String getText() {
        return text;
    }

    public void setText(String _text) {
        this.text = _text;
        notifyPropertyChanged(BR.text);
    }

    public void resetText() {
        this.text = "";
    }

    public Boolean hasText() {
        return this.text != null && !this.text.isEmpty();
    }
}
