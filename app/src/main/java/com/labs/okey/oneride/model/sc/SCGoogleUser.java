package com.labs.okey.oneride.model.sc;

import android.util.Log;

import com.labs.okey.oneride.model.PropertyHolder;

/**
 * @author Oleg Kleiman
 * created 11-Jul-16.
 */
public class SCGoogleUser implements SCUser {

    private final String    LOG_TAG = getClass().getSimpleName();

    private String          mUserId;

    public SCGoogleUser(String userId) {
        mUserId = userId;
    }

    @Override
    public String get_PictureURL() {
        return null;
    }

    @Override
    public String get_FirstName() {
        return null;
    }

    @Override
    public String get_LastName() {
        return null;
    }

    @Override
    public void get_FullName(PropertyHolder<String> callback) {
        if( callback == null )
            return;

        try {
            callback.call();
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getLocalizedMessage());
        }
    }
}
