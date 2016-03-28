package com.labs.okey.oneride.tutorials;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.labs.okey.oneride.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class IntroTutorialFragment extends Fragment {

    private static final String ARG_POSITION = "position";

    public static IntroTutorialFragment newInstance(int position) {
        IntroTutorialFragment f = new IntroTutorialFragment();
        Bundle b = new Bundle();
        b.putInt(ARG_POSITION, position);
        f.setArguments(b);
        return f;
    }


    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_intro, container, false);
    }


}
