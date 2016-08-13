package com.labs.okey.oneride.fragments;

import android.annotation.TargetApi;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.labs.okey.oneride.R;

/**
 * @author eli max
 * created 24/01/2016.
 */

public class TermsFragment extends Fragment {

    private static TermsFragment FragmentInstance;

    public static TermsFragment getInstance() {

        if (FragmentInstance == null ) {
            FragmentInstance = new TermsFragment();
        }
        return FragmentInstance;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }


    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {


        View rootView = inflater.inflate(R.layout.fragment_about_terms, container, false);

        WebView webView = (WebView) rootView.findViewById(R.id.webViewTerms);

//        webView.getSettings().setJavaScriptEnabled(true);
//        webView.getSettings().setLoadWithOverviewMode(true);
//        webView.getSettings().setUseWideViewPort(true);
        webView.setWebViewClient(new WebViewClient(){

            @TargetApi(Build.VERSION_CODES.N)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                final Uri uri = request.getUrl();
                view.loadUrl(uri.toString());

                return true;
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {

                view.loadUrl(url);

                return true;
            }

            @Override
            public void onPageFinished(WebView view, final String url) {

            }
        });

        webView.loadUrl("https://oneride.azurewebsites.net/terms_of_service.html");
        return  rootView;
    }
}
