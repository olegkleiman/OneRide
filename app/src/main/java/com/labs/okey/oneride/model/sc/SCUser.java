package com.labs.okey.oneride.model.sc;

import com.google.common.util.concurrent.ListenableFuture;
import com.labs.okey.oneride.model.PropertyHolder;

import java.util.concurrent.Callable;

/**
 * Created by Oleg on 10-Jul-16.
 */
public interface SCUser {

    public String get_PictureURL();
    public String get_FirstName();
    public String get_LastName();
    public void get_FullName(PropertyHolder<String> callback);
}

