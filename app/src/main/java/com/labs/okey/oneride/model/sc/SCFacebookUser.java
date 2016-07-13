package com.labs.okey.oneride.model.sc;

import android.os.Bundle;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.labs.okey.oneride.model.PropertyHolder;

import org.json.JSONObject;

/**
 * @author Oleg Kleiman
 * created 10-Jul-16.
 */
public class SCFacebookUser implements SCUser {

    private final String    LOG_TAG = getClass().getSimpleName();

    private String          mUserId;

    public SCFacebookUser(String userId) {
        this.mUserId = userId;
    }

    @Override
    public String get_PictureURL() {
        return "https://graph.facebook.com/" + mUserId + "/picture?type=normal";
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
    public void get_FullName(final PropertyHolder<String> callback) {

        if( callback == null )
            return;

        AccessToken fbAccessToken = AccessToken.getCurrentAccessToken();
        final GraphRequest request = GraphRequest.newGraphPathRequest(
                fbAccessToken,
                mUserId,
                new GraphRequest.Callback(){
                    @Override
                    public void onCompleted(GraphResponse response) {

                        try {

                            JSONObject object = response.getJSONObject();
                            if (response.getError() == null) {
                                callback.property = (String) object.get("name");
                                callback.call();
                            } else {
                                if (Crashlytics.getInstance() != null)
                                    Crashlytics.log(response.getError().getErrorMessage());

                                Log.e(LOG_TAG, response.getError().getErrorMessage());
                            }
                        } catch(Exception e) {
                            Log.e(LOG_TAG, e.getLocalizedMessage());
                        }
                    }
                });

        Bundle parameters = new Bundle();
        parameters.putString("fields", "id,name");
        request.setParameters(parameters);
        request.executeAsync();
    }
}
