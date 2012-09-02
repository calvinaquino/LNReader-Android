package com.erakk.lnreader;

import java.io.File;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.erakk.lnreader.dao.NovelsDao;

public class DisplaySettingsActivity extends PreferenceActivity {
	//private static final String TAG = DisplayLightNovelsActivity.class.toString();
	
	@SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
    	// set before create any view
    	if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("invert_colors", false)) {    		
    		setTheme(R.style.AppTheme2);
    	}
    	else {
    		setTheme(R.style.AppTheme);
    	}
    	
        super.onCreate(savedInstanceState);
        //This man is deprecated but but we may want to be bale to run on older API
        addPreferencesFromResource(R.xml.preferences);
        
        updateViewColor();
        
        Preference lockHorizontal = (Preference)  findPreference("lock_horizontal");
        lockHorizontal.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
        	@Override
            public boolean onPreferenceClick(Preference p) {
        		if(p.getSharedPreferences().getBoolean("lock_horizontal", false)){
        			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
	        		Toast.makeText(getApplicationContext(), "Orientation Locked" , Toast.LENGTH_LONG).show();
        		}
        		else {
        			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
	        		Toast.makeText(getApplicationContext(), "Orientation Unlocked" , Toast.LENGTH_LONG).show();
        		}        		
        		return true;
            }
        });
        
        Preference invertColors = (Preference)  findPreference("invert_colors");
        invertColors.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
        	@Override
            public boolean onPreferenceClick(Preference p) {
        		recreate();
        		Toast.makeText(getApplicationContext(), "Color Inverted", Toast.LENGTH_LONG).show();        		
                return true;
            }
        });
        Preference clearDatabase = (Preference)  findPreference("clear_database");
        clearDatabase.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
        	@Override
            public boolean onPreferenceClick(Preference p) {
        		/*
        		 * CODE TO CLEAR DATABASE HERE
        		 */
        		NovelsDao dao = NovelsDao.getInstance(getApplicationContext());
        		dao.deleteDB();
        		Toast.makeText(getApplicationContext(), "Database cleared!", Toast.LENGTH_LONG).show();	
        		return true;
            }
        });
        Preference clearImages = (Preference)  findPreference("clear_image_cache");
        clearImages.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
        	@Override
            public boolean onPreferenceClick(Preference p) {
        		/*
        		 * CODE TO CLEAR IMAGE CACHE HERE
        		 */        		
        		DeleteRecursive(new File(Constants.IMAGE_ROOT));
        		Toast.makeText(getApplicationContext(), "Image cache cleared!", Toast.LENGTH_LONG).show();	
        		
                return true;
            }
        });
        
    }
    
    @SuppressLint("NewApi")
	@Override
    protected void onRestart() {
        super.onRestart();
        // The activity has become visible (it is now "resumed").
        recreate();
    }
    
	@SuppressWarnings("deprecation")
	private void updateViewColor() {
    	//SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    	//boolean invertColors = sharedPrefs.getBoolean("invert_colors", false);
    	    	
    	// Views to be changed
    	// update category
    	PreferenceCategory preferenceUpdateCategory = (PreferenceCategory) findPreference("update_category");
    	Spannable titleUpdateCategory = new SpannableString ( preferenceUpdateCategory.toString() );
    	// updates interval
    	Preference preferenceUpdateInterval = (Preference) findPreference("updates_interval");
    	Spannable titleUpdateInterval = new SpannableString ( preferenceUpdateInterval.getTitle() );
    	Spannable summaryUpdateInterval = new SpannableString ( preferenceUpdateInterval.getSummary() );
    	// layout category
    	Preference preferenceLayoutCategory = (Preference) findPreference("layout_category");
    	Spannable titleLayoutCategory = new SpannableString ( preferenceLayoutCategory.getTitle() );
    	// invert colors
    	Preference preferenceInvertColors = (Preference) findPreference("invert_colors");
    	Spannable titleInvertColors = new SpannableString ( preferenceInvertColors.getTitle() );
    	Spannable summaryInvertColors = new SpannableString ( preferenceInvertColors.getSummary() );
    	// lock horizontal
    	Preference preferenceLockHorizontal = (Preference) findPreference("lock_horizontal");
    	Spannable titleLockHorizontal = new SpannableString ( preferenceLockHorizontal.getTitle() );
    	Spannable summaryLockHorizontal = new SpannableString ( preferenceLockHorizontal.getSummary() );
    	// show images
    	Preference preferenceShowImages = (Preference) findPreference("show_images");
    	Spannable titleShowImages = new SpannableString ( preferenceShowImages.getTitle() );
    	Spannable summaryShowImages = new SpannableString ( preferenceShowImages.getSummary() );
    	// storage category
    	Preference preferenceStorageCategory = (Preference) findPreference("storage_category");
    	Spannable titleStorageCategory = new SpannableString ( preferenceStorageCategory.getTitle() );
    	// clear database
    	Preference preferenceClearDatabase = (Preference) findPreference("clear_database");
    	Spannable titleClearDatabase = new SpannableString ( preferenceClearDatabase.getTitle() );
    	Spannable summaryClearDatabase = new SpannableString ( preferenceClearDatabase.getSummary() );
    	// clear image cache
    	Preference preferenceClearImageCache = (Preference) findPreference("clear_image_cache");
    	Spannable titleClearImageCache = new SpannableString ( preferenceClearImageCache.getTitle() );
    	Spannable summaryClearImageCache = new SpannableString ( preferenceClearImageCache.getSummary() );
    	
    	// =====
    	// update category
    	// =====
        preferenceUpdateCategory.setTitle( titleUpdateCategory );
    	// updates interval
        preferenceUpdateInterval.setSummary( summaryUpdateInterval );
        preferenceUpdateInterval.setTitle( titleUpdateInterval );
    	// =====
    	// layout category
    	// =====
        preferenceLayoutCategory.setTitle( titleLayoutCategory );
    	// invert colors
        preferenceInvertColors.setSummary( summaryInvertColors );
        preferenceInvertColors.setTitle( titleInvertColors );
    	// lock horizontal
        preferenceLockHorizontal.setSummary( summaryLockHorizontal );
        preferenceLockHorizontal.setTitle( titleLockHorizontal );

    	// show images
        preferenceShowImages.setSummary( summaryShowImages );
        preferenceShowImages.setTitle( titleShowImages );
    	// =====
    	// storage category
    	// =====
        preferenceStorageCategory.setTitle( titleStorageCategory );
    	// clear database
        preferenceClearDatabase.setSummary( summaryClearDatabase );
        preferenceClearDatabase.setTitle( titleClearDatabase );
    	// clear image cache
        preferenceClearImageCache.setSummary( summaryClearImageCache );
        preferenceClearImageCache.setTitle( titleClearImageCache );
    }
	
	private void DeleteRecursive(File fileOrDirectory) {
	    if (fileOrDirectory.isDirectory())
	        for (File child : fileOrDirectory.listFiles())
	            DeleteRecursive(child);
	    fileOrDirectory.delete();
	}
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	menu.add(Menu.NONE, 0, 0, "Show current settings");
    	return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    		case 0:
    			startActivity(new Intent(this, DisplaySettingsActivity.class));
    			return true;
    	}
    	return false;
    }

	public boolean onPreferenceChange(Preference preference, Object newValue) {
		// TODO Auto-generated method stub
		return false;
	}
}
