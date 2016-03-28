package com.labs.okey.oneride.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.labs.okey.oneride.model.Ride;
import com.labs.okey.oneride.myrides.GeneralMyRidesFragment;
import com.labs.okey.oneride.myrides.RejectedMyRidesFragment;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;


/**
 * Created by eli max on 18/06/2015.
 */
public class MyRideTabAdapter extends FragmentPagerAdapter {

    private String titles[];
    List<Ride> mRides;



    public MyRideTabAdapter(FragmentManager fm, String[] titles, List<Ride> rides) {
        super(fm);
        this.titles = titles;
        this.mRides = rides;
        GeneralMyRidesFragment.getInstance().setRides(rides);
        RejectedMyRidesFragment.getInstance().setAppeals(rides);
    }

    public void updateRides(List<Ride> rides) {
        this.mRides = rides;
        GeneralMyRidesFragment.getInstance().updateRides(rides);
        RejectedMyRidesFragment.getInstance().updateAppeals(rides);
    }

    @Override
    public Fragment getItem(int position) {

        switch( position) {
            case 0:
                return GeneralMyRidesFragment.getInstance();

            case 1:
                return RejectedMyRidesFragment.getInstance();
        }

        return  null;
    }

    @Override
    public int getCount() {
        return titles.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return titles[position];
    }

    private void sort(){

        Collections.sort(mRides, new Comparator<Ride>() {
            public int compare(Ride r1, Ride r2) {
                return r1.getCreated().compareTo(r2.getCreated());
            }
        });
    }
}

