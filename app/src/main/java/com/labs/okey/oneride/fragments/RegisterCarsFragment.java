package com.labs.okey.oneride.fragments;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.labs.okey.oneride.R;
import com.labs.okey.oneride.adapters.CarsAdapter;
import com.labs.okey.oneride.model.RegisteredCar;
import com.labs.okey.oneride.utils.Globals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class RegisterCarsFragment extends Fragment {

    private final String LOG_TAG = getClass().getSimpleName();

    private EditText mCarInput;
    private EditText mCarNickInput;
    private CarsAdapter mCarsAdapter;
    List<RegisteredCar> mCars;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_register_cars, container, false);

        try {

            mCars = new ArrayList<>();
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            Set<String> carsSet = sharedPrefs.getStringSet(Globals.CARS_PREF, new HashSet<String>());
            if( carsSet != null ) {
                Iterator<String> iterator = carsSet.iterator();

                while (iterator.hasNext()) {
                    String strCar= iterator.next();
                    String[] tokens = strCar.split("~");
                    RegisteredCar car = new RegisteredCar();
                    car.setCarNumber(tokens[0]);
                    if( tokens.length > 1 )
                        car.setCarNick(tokens[1]);
                    mCars.add(car);
                }
            }

            RecyclerView recycler = (RecyclerView)v.findViewById(R.id.carsListView);
            recycler.setHasFixedSize(true);
            recycler.setLayoutManager(new LinearLayoutManager(getActivity()));
            recycler.setItemAnimator(new DefaultItemAnimator());
            mCarsAdapter = new CarsAdapter(getActivity(), mCars, null);
            recycler.setAdapter(mCarsAdapter);

            View addButton = v.findViewById(R.id.add_car_button);
            addButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                            .title(R.string.add_car_dialog_caption)
                            .customView(R.layout.dialog_add_car, true)
                            .positiveText(R.string.add_car_button_add)
                            .negativeText(android.R.string.cancel)
                            .autoDismiss(true)
                            .cancelable(true)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog,
                                                    @NonNull DialogAction which) {
                                    String carNumber = mCarInput.getText().toString();
                                    String carNick = (mCarNickInput.getText().toString().isEmpty()) ?
                                            "" : mCarNickInput.getText().toString();

                                    RegisteredCar car = new RegisteredCar();
                                    car.setCarNumber(carNumber);
                                    car.setCarNick(carNick);

                                    mCarsAdapter.add(car);
                                    mCarsAdapter.notifyDataSetChanged();

                                    saveCars();

                                }
                            })
                           .build();

                    mCarInput = (EditText) dialog.getCustomView().findViewById(R.id.txtCarNumber);
                    mCarNickInput = (EditText) dialog.getCustomView().findViewById(R.id.txtCarNick);
                    dialog.show();

                                    //Toast.makeText(getActivity(), "Not implemented yet", Toast.LENGTH_LONG).show();
                }
            });
        } catch(Exception ex) {
            Log.e(LOG_TAG, ex.getMessage());
        }
        return v;
   }

    private void saveCars() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = sharedPrefs.edit();
        Set<String> carsSet = new HashSet<String>();
        for (RegisteredCar car : mCars) {
            String s = car.getCarNumber() + "~" + car.getCarNick();
            carsSet.add(s);
        }

        editor.putStringSet(Globals.CARS_PREF, carsSet);
        editor.apply();
    }

}
