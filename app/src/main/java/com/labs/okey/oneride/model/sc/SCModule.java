package com.labs.okey.oneride.model.sc;

import com.labs.okey.oneride.utils.Globals;

/**
 * @author Oleg Kleiman
 * created 10-Jul-16.
 */

//@Module
public class SCModule {

    //@Provides
    public SCUser getUser(String authProvider, String userId){

        SCUser scUser = null;

        if(Globals.GOOGLE_PROVIDER.equalsIgnoreCase(authProvider) ) {
            scUser = new SCFacebookUser(userId);
        } else if( Globals.FB_PROVIDER.equalsIgnoreCase(authProvider)){
            scUser = new SCFacebookUser(userId);
        }

        return scUser;
    }
}
