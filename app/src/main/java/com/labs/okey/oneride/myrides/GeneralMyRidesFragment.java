package com.labs.okey.oneride.myrides;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.labs.okey.oneride.R;
import com.labs.okey.oneride.RideDetailsActivity;
import com.labs.okey.oneride.adapters.MyRidesAdapter;
import com.labs.okey.oneride.model.Ride;
import com.labs.okey.oneride.utils.IRecyclerClickListener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


/**
 * Created by eli max on 18/06/2015.
 */
public class GeneralMyRidesFragment extends Fragment {


    List<Ride> mRides = new ArrayList<>();
    private static final String ARG_POSITION = "position";
    private static GeneralMyRidesFragment FragmentInstance;
    MyRidesAdapter adapter;


    public static GeneralMyRidesFragment getInstance() {

        if (FragmentInstance == null ) {
            FragmentInstance = new GeneralMyRidesFragment();
        }
        return FragmentInstance;
    }

    public void setRides(List<Ride> rides) {

        if (rides == null || rides.isEmpty())
            return;

        mRides.clear();
        mRides.addAll(rides);
        sort();
    }

    public void updateRides(List<Ride> rides){

        if (rides == null || rides.isEmpty())
            return;

        mRides.clear();
        mRides.addAll(rides);

        sort();
        adapter.notifyDataSetChanged();
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
        View rootView = inflater.inflate(R.layout.fragment_myride_general, container, false);

//        if (mRides.isEmpty()) {
//
//            final ProgressBar progress_refresh = (ProgressBar) rootView.findViewById(R.id.progress_refresh);
//            progress_refresh.setVisibility(View.VISIBLE);
//        }

        RecyclerView recycler = (RecyclerView)rootView.findViewById(R.id.recyclerMyRides);
        recycler.setHasFixedSize(true);
        recycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        recycler.setItemAnimator(new DefaultItemAnimator());

        adapter = new MyRidesAdapter(mRides);
        adapter.setOnClickListener(new IRecyclerClickListener() {
            @Override
            public void clicked(View v, int position) {

                Ride currentRide = mRides.get(position);
                Intent intent = new Intent(getActivity(), RideDetailsActivity.class);

                intent.putExtra("ride", (Serializable)currentRide);
                startActivity(intent);
            }
        });
        recycler.setAdapter(adapter);

        return rootView;

    }

    private void sort(){

        Collections.sort(mRides, Collections.reverseOrder(new Comparator<Ride>() {
            public int compare(Ride r1, Ride r2) {
                if( r1.getCreated() == null
                        || r2.getCreated() == null )
                    return 1;

                return r1.getCreated().compareTo(r2.getCreated());
            }}));

    }
}