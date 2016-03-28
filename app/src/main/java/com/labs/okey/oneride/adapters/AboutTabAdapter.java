package com.labs.okey.oneride.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.labs.okey.oneride.fragments.AboutFragment;
import com.labs.okey.oneride.fragments.PrivacyFragment;
import com.labs.okey.oneride.fragments.TermsFragment;

/**
 * Created by eli max on 24/01/2016.
 */
public class AboutTabAdapter extends FragmentPagerAdapter {
    private String titles[];

    public AboutTabAdapter(FragmentManager fm, String[] titles) {
        super(fm);
        this.titles = titles;
    }



    @Override
    public Fragment getItem(int position) {

        switch( position) {
            case 0:
                return AboutFragment.getInstance();

            case 1:
                return TermsFragment.getInstance();

            case 2:
                return PrivacyFragment.getInstance();
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
}