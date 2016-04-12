package com.labs.okey.oneride.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.labs.okey.oneride.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class PhoneConfirmFragment extends Fragment {

    private final String LOG_TAG = getClass().getSimpleName();

//    public PhoneConfirmFragment() {
//        // Required empty public constructor
//    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_phone_confirm, container, false);
    }

}
