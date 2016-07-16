package com.labs.okey.oneride.advsettings;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.labs.okey.oneride.R;

/**
 * @author Oleg Kleiman
 * created 16-Jul-16.
 */
public class LocalLinkAdvSettingsFragment extends Fragment {

    private static LocalLinkAdvSettingsFragment FragmentInstance;

    public static LocalLinkAdvSettingsFragment getInstance() {

        if (FragmentInstance == null ) {
            FragmentInstance = new LocalLinkAdvSettingsFragment();
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
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_local_link_settings, container, false);

        return rootView;
    }
}
