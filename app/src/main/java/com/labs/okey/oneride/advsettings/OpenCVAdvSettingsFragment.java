package com.labs.okey.oneride.advsettings;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.labs.okey.oneride.R;

/**
 * @author Oleg Kleiman
 * created 16-Jul-16.
 */
public class OpenCVAdvSettingsFragment extends Fragment {

    private final String LOG_TAG = getClass().getSimpleName();

    private static OpenCVAdvSettingsFragment FragmentInstance;

    public static OpenCVAdvSettingsFragment getInstance() {

        if (FragmentInstance == null ) {
            FragmentInstance = new OpenCVAdvSettingsFragment();
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
        View rootView = inflater.inflate(R.layout.fragment_opencv_settings, container, false);

        return rootView;
    }
}
