package com.labs.okey.oneride.utils;

import com.google.common.util.concurrent.ListenableFuture;
import com.microsoft.windowsazure.mobileservices.MobileServiceException;
import com.microsoft.windowsazure.mobileservices.http.NextServiceFilterCallback;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilter;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterRequest;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse;

import java.util.concurrent.ExecutionException;

/**
 * @author Oleg Kleiman
 * created 05-Aug-16.
 */

public class RefreshTokenCacheFilter implements ServiceFilter {
    @Override
    public ListenableFuture<ServiceFilterResponse> handleRequest(ServiceFilterRequest request,
                                                                 NextServiceFilterCallback nextServiceFilterCallback) {
        ListenableFuture<ServiceFilterResponse> future = null;
        ServiceFilterResponse response = null;
        int responseCode = 401;

        future = nextServiceFilterCallback.onNext(request);

        try {
            response = future.get();
        } catch(InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            if (e.getCause().getClass() == MobileServiceException.class) {
                MobileServiceException mEx = (MobileServiceException) e.getCause();
                //responseCode = mEx.getResponse().getStatus().getStatusCode();
            }
        }

        return null;
    }
}
