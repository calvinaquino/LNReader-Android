package com.erakk.lnreader.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.InflateException;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.ui.fragment.BookmarkFragment;
import com.erakk.lnreader.ui.fragment.DownloadFragment;
import com.erakk.lnreader.ui.fragment.SearchFragment;
import com.erakk.lnreader.ui.fragment.UpdateInfoFragment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;


public class BaseActivity extends AppCompatActivity {
    private static final String TAG = BaseActivity.class.toString();
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    protected Fragment mContent = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestAppPermissions();

        //Restore the fragment's instance
        if (savedInstanceState != null) {
            mContent = getSupportFragmentManager().getFragment(savedInstanceState, "mContent");
        }

        setupExceptionHandler();
        UIHelper.setLanguage(this);
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        ActionBar t = getSupportActionBar();
        if (t != null)
            t.setTitle(title);
    }

    @Override
    public void setTitle(@StringRes int titleId) {
        super.setTitle(titleId);
        ActionBar t = getSupportActionBar();
        if (t != null)
            t.setTitle(titleId);
    }

    @Override
    public void onResume() {
        super.onResume();
        UIHelper.CheckScreenRotation(this);
        UIHelper.CheckKeepAwake(this);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        if (mDrawerToggle != null)
            mDrawerToggle.syncState();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //Save the fragment's instance
        if (mContent != null)
            try {
                getSupportFragmentManager().putFragment(outState, "mContent", mContent);
            } catch (Exception ex) {
                // TODO: Proper handling required
                Log.e(TAG, ex.getMessage());
            }
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
        mDrawerLayout.closeDrawers();
        mContent = null;
        UIHelper.openNovelList(this);
    }

    public void openWatchList(MenuItem item) {
        mDrawerLayout.closeDrawers();
        mContent = null;
        UIHelper.openWatchList(this);
    }

    public void openLastRead(MenuItem item) {
        mDrawerLayout.closeDrawers();
        mContent = null;
        UIHelper.openLastRead(this);
    }

    public void openAltNovelList(MenuItem item) {
        mDrawerLayout.closeDrawers();
        mContent = null;
        UIHelper.selectAlternativeLanguage(this);
    }

    public void openSettings(MenuItem view) {
        mDrawerLayout.closeDrawers();
        Intent intent = new Intent(this, DisplaySettingsActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
        // FOR TESTING
        // resetFirstRun();
    }

    public void openDownloadsList(MenuItem view) {
        mDrawerLayout.closeDrawers();
        View f = findViewById(R.id.mainFrame);
        if (f != null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            mContent = new DownloadFragment();
            transaction.replace(getLeftFrame(), mContent, DownloadFragment.class.toString())
                    .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_left)
                    .addToBackStack(DownloadFragment.class.toString())
                    .commit();
        } else {
            Intent i = new Intent(this, MainActivity.class);
            i.putExtra(Constants.EXTRA_INITIAL_FRAGMENT, DownloadFragment.class.toString());
            startActivity(i);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
        }
    }

    public void openUpdatesList(MenuItem view) {
        mDrawerLayout.closeDrawers();
        View f = findViewById(R.id.mainFrame);
        if (f != null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            mContent = new UpdateInfoFragment();
            transaction.replace(getLeftFrame(), mContent, UpdateInfoFragment.class.toString())
                    .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_left)
                    .addToBackStack(UpdateInfoFragment.class.toString())
                    .commit();
        } else {
            Intent i = new Intent(this, MainActivity.class);
            i.putExtra(Constants.EXTRA_INITIAL_FRAGMENT, UpdateInfoFragment.class.toString());
            startActivity(i);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
        }
    }

    public void openBookmarks(MenuItem view) {
        mDrawerLayout.closeDrawers();
        View f = findViewById(R.id.mainFrame);
        if (f != null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            mContent = new BookmarkFragment();
            transaction.replace(getLeftFrame(), mContent, BookmarkFragment.class.toString())
                    .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_left)
                    .addToBackStack(BookmarkFragment.class.toString())
                    .commit();
        } else {
            Intent i = new Intent(this, MainActivity.class);
            i.putExtra(Constants.EXTRA_INITIAL_FRAGMENT, BookmarkFragment.class.toString());
            startActivity(i);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
        }
    }

    public void openSearch(MenuItem view) {
        mDrawerLayout.closeDrawers();
        View f = findViewById(R.id.mainFrame);
        if (f != null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            mContent = new SearchFragment();
            transaction.replace(getLeftFrame(), mContent, SearchFragment.class.toString())
                    .setCustomAnimations(R.anim.abc_fade_in, R.anim.slide_out_left, R.anim.abc_fade_in, R.anim.slide_out_left)
                    .addToBackStack(SearchFragment.class.toString())
                    .commit();
        } else {
            Intent i = new Intent(this, MainActivity.class);
            i.putExtra(Constants.EXTRA_INITIAL_FRAGMENT, SearchFragment.class.toString());
            startActivity(i);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
        }
    }

    // endregion

    protected int getLeftFrame() {
        View v = findViewById(R.id.rightFragment);
        if (v != null)
            return R.id.rightFragment;
        else return R.id.mainFrame;
    }

    @Override
    public void setContentView(@LayoutRes int layout) {
        try {
            super.setContentView(layout);


            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            if (toolbar != null) {
                toolbar.setTitle(R.string.app_name);
                setSupportActionBar(toolbar);

                mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
                mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.open_chapter_title, R.string.open_chapter_title) {

                    /**
                     * Called when a drawer has settled in a completely closed state.
                     */
                    public void onDrawerClosed(View view) {
                        super.onDrawerClosed(view);
                        //getActionBar().setTitle(mTitle);
                        //invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()

                        ActionBar bar = getSupportActionBar();
                        if (bar != null) {
                            bar.setDisplayHomeAsUpEnabled(true);
                            bar.setDisplayShowHomeEnabled(true);
                        }
                    }

                    /**
                     * Called when a drawer has settled in a completely open state.
                     */
                    public void onDrawerOpened(View drawerView) {
                        super.onDrawerOpened(drawerView);
                        //getActionBar().setTitle(mDrawerTitle);
                        //invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                    }

                };
                ActionBar bar = getSupportActionBar();
                if (bar != null) {
                    bar.setDisplayHomeAsUpEnabled(true);
                    bar.setDisplayShowHomeEnabled(true);
                }
            } else {
                Log.w(TAG, "No toolbar detected!");
            }
        } catch (InflateException ex) {
            Toast.makeText(this, "Unable to load application, looks like your device is not supported: " + ex.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, ex.getMessage(), ex);
        }
    }

    /**
     * http://stackoverflow.com/a/26560727
     */
    private void setupExceptionHandler() {
        final Thread.UncaughtExceptionHandler oldHandler =
                Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler(
                new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread paramThread, Throwable paramThrowable) {
                        //Do your own error handling here

                        // try to write log file to the image path.
                        writeException(paramThread, paramThrowable);

                        if (oldHandler != null)
                            oldHandler.uncaughtException(
                                    paramThread,
                                    paramThrowable
                            ); //Delegates to Android's error handling
                        else
                            System.exit(2); //Prevents the service/app from freezing
                    }
                });
    }

    private void writeException(Thread paramThread, Throwable paramThrowable) {
        BufferedWriter writer = null;
        String rootPath = UIHelper.getImageRoot(this);
        try {
            // create a temporary file
            String timeLog = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
            File logFile = new File(rootPath + "/Error_" + timeLog + ".log");

            // This will output the full path where the file will be written to...
            Log.d(TAG, "Writing to: " + logFile.getCanonicalPath());

            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile), "UTF-8"));

            writer.write("Thread Name: " + paramThread.getName());
            writer.newLine();

            writer.write(paramThrowable.getMessage());
            writer.newLine();

            StringWriter sw = new StringWriter();
            paramThrowable.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            writer.write(exceptionAsString);
            writer.newLine();

            writer.flush();

            writer.write("-=EOL=-");
        } catch (Exception e) {
            Log.e(TAG, "Failed to write log file.", e);
        } finally {
            try {
                // Close the writer regardless of what happens...
                if (writer != null)
                    writer.close();
            } catch (Exception e) {
                Log.e(TAG, "Failed when closing writer.", e);
            }
        }
    }

    // region permission issue #277
    private int grantResults[];

    private void requestAppPermissions() {
        int requestId = 101;
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }

        if (hasReadPermissions() && hasWritePermissions()) {
            return;
        }

        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.INTERNET,
                        Manifest.permission.ACCESS_NETWORK_STATE
                }, requestId); // your request code

        onRequestPermissionsResult(requestId, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, grantResults);
    }

    private boolean hasReadPermissions() {
        return (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
    }

    private boolean hasWritePermissions() {
        return (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
    }

    @Override // android recommended class to handle permissions
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Log.d(TAG, "Permission granted");
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.uujm
                    Toast.makeText(this, "Permission denied to read/write your External storage", Toast.LENGTH_SHORT).show();


                }
                break;
            }

            // other 'case' line to check fosr other
            // permissions this app might request
        }
    }
    // endregion
}
