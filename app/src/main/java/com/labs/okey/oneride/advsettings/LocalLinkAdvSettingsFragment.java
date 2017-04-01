package com.labs.okey.oneride.advsettings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.labs.okey.oneride.R;
import com.labs.okey.oneride.utils.Globals;

import java.util.Locale;

/**
 * @author Oleg Kleiman
 * created 16-Jul-16.
 */
public class LocalLinkAdvSettingsFragment extends Fragment
        implements CompoundButton.OnCheckedChangeListener,
        View.OnClickListener {

    private final String LOG_TAG = getClass().getSimpleName();

    private static LocalLinkAdvSettingsFragment FragmentInstance;

    private int                                 mRssiCurrentValue;
    private SharedPreferences                   mSharedPrefs;

    private EditText                            mDiscoveryPeriodEdit;
    private EditText                            mLastSeenEdit;

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

        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this.getActivity());

        final TextView txtRssiLevel = (TextView)rootView.findViewById(R.id.current_rssi_level);
        mRssiCurrentValue = mSharedPrefs.getInt(Globals.PREF_RSSI_LEVEL, Globals.DEFAULT_RSSI_LEVEL);
        txtRssiLevel.setText(String.format(Locale.getDefault(), "-%d dBm", mRssiCurrentValue));

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
                SharedPreferences.Editor editor = mSharedPrefs.edit();
                editor.putInt(Globals.PREF_RSSI_LEVEL, mRssiCurrentValue);
                editor.apply();

                txtRssiLevel.setText(String.format(Locale.getDefault(), "-%d dBm", mRssiCurrentValue));
            }
        });

        CheckBox cbPushNotification = (CheckBox)rootView.findViewById(R.id.cbPushNotificationTransport);
        cbPushNotification.setOnCheckedChangeListener(this);
        CheckBox cbRealtimeDBNotification = (CheckBox)rootView.findViewById(R.id.cbRealtimeDb);
        cbRealtimeDBNotification.setOnCheckedChangeListener(this);

        CheckBox cbAllowSamePassengers = (CheckBox)rootView.findViewById(R.id.cbAllowSamePassengers);
        boolean isSamePassengersAllowed = mSharedPrefs.getBoolean(Globals.PREF_ALLOW_SAME_PASSENGERS, false);
        cbAllowSamePassengers.setChecked(isSamePassengersAllowed);
        cbAllowSamePassengers.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                SharedPreferences.Editor editor = mSharedPrefs.edit();
                editor.putBoolean(Globals.PREF_ALLOW_SAME_PASSENGERS, isChecked);
                editor.apply();
            }
        });

        mLastSeenEdit = (EditText)rootView.findViewById(R.id.txtLastSeenBefore);
        int lastSeenInterval = mSharedPrefs.getInt(Globals.PREF_LAST_SEEN_BEFORE,
                                                   Globals.PREF_LAST_SEEN_DEFAULT_INTERVAL);
        mLastSeenEdit.setText(Long.toString(lastSeenInterval));

        mDiscoveryPeriodEdit = (EditText)rootView.findViewById(R.id.txtDiscoveryPeriod);
        int discoverableDuration = mSharedPrefs.getInt(Globals.PREF_DISCOVERABLE_DURATION,
                                        Globals.PREF_DISCOVERABLE_DURATION_DEFAULT);
        mDiscoveryPeriodEdit.setText(Integer.toString(discoverableDuration));

        Button btnDiscoveryApply = (Button)rootView.findViewById(R.id.btnDiscoveryPeriodApply);
        btnDiscoveryApply.setOnClickListener(this);

        return rootView;
    }

    @Override
    public void onClick(View view) {
        int discoverableDuration = Integer.parseInt(mDiscoveryPeriodEdit.getText().toString());
        SharedPreferences.Editor editor = mSharedPrefs.edit();
        editor.putInt(Globals.PREF_DISCOVERABLE_DURATION, discoverableDuration);
        editor.apply();
    }

    public void onLastSeenPeriodApply(View v) {

        Globals.LAST_SEEN_INTERVAL = Long.parseLong(mLastSeenEdit.getText().toString());
    }

    public void onDiscoveryPeriodApply(View v) {

    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {

        switch (compoundButton.getId() ) {
            case R.id.cbPushNotificationTransport: {
                Log.d(LOG_TAG, String.format("Push Notifications transport checked: %b", isChecked));

                SharedPreferences.Editor editor = mSharedPrefs.edit();
                editor.putBoolean(Globals.PREF_PUSH_MODE, isChecked);
                editor.apply();

                Globals.setPushNotificationsModeEnabled(isChecked);
            }
            break;

            case R.id.cbRealtimeDb: {
                Log.d(LOG_TAG, String.format("Realtime DB transport checked: %b", isChecked));

                SharedPreferences.Editor editor = mSharedPrefs.edit();
                editor.putBoolean(Globals.PREF_REALTIMEDB__MODE, isChecked);
                editor.apply();

                Globals.setRealtimeDbNotificationsMode(isChecked);
            }
            break;

        }
    }
}
