package com.erakk.lnreader.UI.activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.erakk.lnreader.R;
import com.erakk.lnreader.UI.fragment.BookmarkFragment;
import com.erakk.lnreader.UI.fragment.DownloadFragment;
import com.erakk.lnreader.UI.fragment.SearchFragment;
import com.erakk.lnreader.UI.fragment.UpdateInfoFragment;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.activity.DisplaySettingsActivity;


public class BaseActivity extends AppCompatActivity {
    private static final String TAG = BaseActivity.class.toString();
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragactivity_framework);
        UIHelper.setLanguage(this);
    }

    /**
     * Call this after onCreate to setup the toolbar.
     */
    protected void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(R.string.app_name);
            setSupportActionBar(toolbar);

            mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
            mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.open_chapter_title, R.string.open_chapter_title) {

                /** Called when a drawer has settled in a completely closed state. */
                public void onDrawerClosed(View view) {
                    super.onDrawerClosed(view);
                    //getActionBar().setTitle(mTitle);
                    //invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    getSupportActionBar().setDisplayShowHomeEnabled(true);
                }

                /** Called when a drawer has settled in a completely open state. */
                public void onDrawerOpened(View drawerView) {
                    super.onDrawerOpened(drawerView);
                    //getActionBar().setTitle(mDrawerTitle);
                    //invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                }

            };

            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        } else {
            Log.w(TAG, "No toolbar detected!");
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        if (mDrawerToggle != null)
            mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggle
        if (mDrawerToggle != null)
            mDrawerToggle.onConfigurationChanged(newConfig);
    }

    // region drawer onClick Handler

    public void openNovelList(MenuItem view) {
        UIHelper.openNovelList(this);
    }

    public void openWatchList(MenuItem item) {
        UIHelper.openWatchList(this);
    }

    public void openLastRead(MenuItem item) {
        UIHelper.openLastRead(this);
    }

    public void openAltNovelList(MenuItem item) {
        UIHelper.selectAlternativeLanguage(this);
    }

    public void openSettings(MenuItem view) {
        Intent intent = new Intent(this, DisplaySettingsActivity.class);
        startActivity(intent);
        // FOR TESTING
        // resetFirstRun();
    }

    public void openDownloadsList(MenuItem view) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.mainFrame, new DownloadFragment()).commit();
        mDrawerLayout.closeDrawers();
    }

    public void openUpdatesList(MenuItem view) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.mainFrame, new UpdateInfoFragment()).commit();
        mDrawerLayout.closeDrawers();
    }

    public void openBookmarks(MenuItem view) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.mainFrame, new BookmarkFragment()).commit();
        mDrawerLayout.closeDrawers();
    }

    public void openSearch(MenuItem view) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.mainFrame, new SearchFragment()).commit();
        mDrawerLayout.closeDrawers();
    }

    // endregion
}
