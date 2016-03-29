package com.labs.okey.oneride;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.labs.okey.oneride.adapters.AboutTabAdapter;
import com.labs.okey.oneride.utils.WAMSVersionTable;
import com.labs.okey.oneride.views.SlidingTabLayout;

public class AboutActivity extends BaseActivity
        implements WAMSVersionTable.IVersionMismatchListener{

    private final String    LOG_TAG = getClass().getSimpleName();
    AboutTabAdapter         mTabAdapter;
    private String          titles[];
    ViewPager               mViewPager;
    SlidingTabLayout        slidingTabLayout;

    @Override
    @CallSuper
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        String title = getResources().getString(R.string.version_title);

        try{
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            String packageVersionName = info.versionName;
            title = title.concat(" ");
            title = title.concat(packageVersionName);
        }catch(PackageManager.NameNotFoundException ex) {
            Log.e(LOG_TAG, ex.getMessage());
            title = title.concat("<Unknown>");
        } finally {
            setupUI(title, "");
        }

        titles = getResources().getStringArray(R.array.about_titles);
        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        slidingTabLayout = (SlidingTabLayout) findViewById(R.id.sliding_tabs);

        mTabAdapter= new AboutTabAdapter(getSupportFragmentManager(),
                titles);
        mViewPager.setAdapter(mTabAdapter);

        slidingTabLayout.setViewPager(mViewPager);
        slidingTabLayout.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {
            @Override
            public int getIndicatorColor(int position) {
                return Color.WHITE;
            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_about, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    @UiThread
    public void mismatch(int majorLast, int minorLast, final String url) {
        new MaterialDialog.Builder(this)
                .title(getString(R.string.new_version_title))
                .content(getString(R.string.new_version_conent))
                .positiveText(android.R.string.yes)
                .negativeText(android.R.string.no)
                .onPositive(new MaterialDialog.SingleButtonCallback(){
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog,
                                        @NonNull DialogAction which) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(url));
                        //intent.setDataAndType(Uri.parse(url), "application/vnd.android.package-archive");
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                })
                .show();
    }

    @Override
    public void connectionFailure(Exception ex) {

    }

    @Override
    public void match() {
        View v = findViewById(R.id.drawer_layout);
        String message = getString(R.string.latest_version);
        Snackbar.make(v, message, Snackbar.LENGTH_LONG).show();

    }
}
