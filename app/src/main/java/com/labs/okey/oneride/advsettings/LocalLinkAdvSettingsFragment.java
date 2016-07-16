package com.labs.okey.oneride.advsettings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.widget.TextViewCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.labs.okey.oneride.R;
import com.labs.okey.oneride.utils.Globals;

import java.util.Locale;

/**
 * @author Oleg Kleiman
 * created 16-Jul-16.
 */
public class LocalLinkAdvSettingsFragment extends Fragment {

    private final String LOG_TAG = getClass().getSimpleName();

    private static LocalLinkAdvSettingsFragment FragmentInstance;

    private int mRssiCurrentValue;

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

        final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this.getActivity());

        final TextView txtRssiLevel = (TextView)rootView.findViewById(R.id.current_rssi_level);
        mRssiCurrentValue = sharedPrefs.getInt(Globals.PREF_RSSI_LEVEL, Globals.DEFAULT_RSSI_LEVEL);
        txtRssiLevel.setText(String.format(Locale.getDefault(), "%d dBm", mRssiCurrentValue));

        SeekBar rssiSeekBar = (SeekBar)rootView.findViewById(R.id.rssiSeekBar);
        rssiSeekBar.setProgress(mRssiCurrentValue);
        final int minValue = 20;
        rssiSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar,
                                          int progresValue,
                                          boolean fromUser) {
                mRssiCurrentValue = minValue + progresValue;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putInt(Globals.PREF_RSSI_LEVEL, mRssiCurrentValue);
                editor.apply();

                txtRssiLevel.setText(String.format(Locale.getDefault(), "%d dBm", mRssiCurrentValue));
            }
        });


        return rootView;
    }
}
