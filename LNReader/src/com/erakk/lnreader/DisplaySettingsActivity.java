package com.erakk.lnreader;

import java.io.File;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.erakk.lnreader.dao.NovelsDao;

public class DisplaySettingsActivity extends PreferenceActivity {
	//private static final String TAG = DisplayLightNovelsActivity.class.toString();
	
	@SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {    	
        super.onCreate(savedInstanceState);
      //This man is deprecated but but we may want to be bale to run on older API
        addPreferencesFromResource(R.xml.preferences);
        
        updateViewColor();
        
        Preference invertColors = (Preference)  findPreference("invert_colors");
        invertColors.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
        	@Override
            public boolean onPreferenceClick(Preference p) {
                // TODO stuff              
        		
        		updateViewColor();
        		
        		Toast t = Toast.makeText(getApplicationContext(), "Color Inverted", Toast.LENGTH_LONG);
    			t.show();		
        		
                return true;
            }
        });
        Preference clearDatabase = (Preference)  findPreference("clear_database");
        clearDatabase.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
        	@Override
            public boolean onPreferenceClick(Preference p) {
                // TODO stuff              
        		
        		/*
        		 * CODE TO CLEAR DATABASE HERE
        		 */
        		NovelsDao dao = new NovelsDao(getApplicationContext());
        		dao.deleteDB();
        		Toast t = Toast.makeText(getApplicationContext(), "Database cleared!", Toast.LENGTH_LONG);
    			t.show();		
        		
                return true;
            }
        });
        Preference clearImages = (Preference)  findPreference("clear_image_cache");
        clearImages.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
        	@Override
            public boolean onPreferenceClick(Preference p) {
                // TODO stuff              
        		
        		/*
        		 * CODE TO CLEAR IMAGE CACHE HERE
        		 */
        		
        		DeleteRecursive(new File(Constants.IMAGE_ROOT));
        		
        		Toast t = Toast.makeText(getApplicationContext(), "Image cache cleared!", Toast.LENGTH_LONG);
    			t.show();		
        		
                return true;
            }
        });
        
    }
    
	@SuppressWarnings("deprecation")
	private void updateViewColor() {
    	SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    	boolean invertColors = sharedPrefs.getBoolean("invert_colors", false);
    	
    	/*
    	 * Why i am not using styles? because they can't be set on any view cycle, only on create. 
    	 * So this is going to be big.
    	 */
    	
    	// Views to be changed
    	// update category
    	PreferenceCategory preferenceUpdateCategory = (PreferenceCategory) findPreference("update_category");
    	Spannable titleUpdateCategory = new SpannableString ( preferenceUpdateCategory.toString() );
    	// perform updates
    	Preference preferencePerformUpdates = (Preference) findPreference("perform_updates");
    	Spannable titlePerformUpdates = new SpannableString ( preferencePerformUpdates.getTitle() );
    	Spannable summaryPerformUpdates = new SpannableString ( preferencePerformUpdates.getSummary() );
    	// updates interval
    	Preference preferenceUpdateInterval = (Preference) findPreference("perform_updates");
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
    	
        // it is considered white background and black text to be the standard
        // so we change to black background and white text if true
//    	Preference preference1 = (Preference) findViewById(R.id.);
        if (invertColors == true) {
        	getListView().setBackgroundColor(Color.BLACK);
        	int col = Color.WHITE;
        	// =====
        	// update category
        	// =====
        	titleUpdateCategory.setSpan(new ForegroundColorSpan( col ), 0, titleUpdateCategory.length(), 0);
        	// perform updates
        	summaryPerformUpdates.setSpan( new ForegroundColorSpan( col ), 0, summaryPerformUpdates.length(), 0 );
        	titlePerformUpdates.setSpan( new ForegroundColorSpan( col ), 0, titlePerformUpdates.length(), 0 );
        	// updates interval
        	summaryUpdateInterval.setSpan( new ForegroundColorSpan( col ), 0, summaryUpdateInterval.length(), 0 );
        	titleUpdateInterval.setSpan( new ForegroundColorSpan( col ), 0, titleUpdateInterval.length(), 0 );
        	// =====
        	// layout category
        	// =====
        	titleLayoutCategory.setSpan( new ForegroundColorSpan( col ), 0, titleLayoutCategory.length(), 0 );
        	// invert colors
        	summaryInvertColors.setSpan( new ForegroundColorSpan( col ), 0, summaryInvertColors.length(), 0 );
        	titleInvertColors.setSpan( new ForegroundColorSpan( col ), 0, titleInvertColors.length(), 0 );
        	// lock horizontal
        	summaryLockHorizontal.setSpan( new ForegroundColorSpan( col ), 0, summaryLockHorizontal.length(), 0 );
        	titleLockHorizontal.setSpan( new ForegroundColorSpan( col ), 0, titleLockHorizontal.length(), 0 );
        	// show images
        	summaryShowImages.setSpan( new ForegroundColorSpan( col ), 0, summaryShowImages.length(), 0 );
        	titleShowImages.setSpan( new ForegroundColorSpan( col ), 0, titleShowImages.length(), 0 );
        	// =====
        	// storage category
        	// =====
        	titleStorageCategory.setSpan( new ForegroundColorSpan( col ), 0, titleStorageCategory.length(), 0 );
        	// clear database
        	summaryClearDatabase.setSpan( new ForegroundColorSpan( col ), 0, summaryClearDatabase.length(), 0 );
        	titleClearDatabase.setSpan( new ForegroundColorSpan( col ), 0, titleClearDatabase.length(), 0 );
        	// clear image cache
        	summaryClearImageCache.setSpan( new ForegroundColorSpan( col ), 0, summaryClearImageCache.length(), 0 );
        	titleClearImageCache.setSpan( new ForegroundColorSpan( col ), 0, titleClearImageCache.length(), 0 );
        }
        else {
        	getListView().setBackgroundColor(Color.TRANSPARENT);
        	int col = Color.BLACK;
        	// =====
        	// update category
        	// =====
        	titleUpdateCategory.setSpan(new ForegroundColorSpan( col ), 0, titleUpdateCategory.length(), 0);
        	// perform updates
        	summaryPerformUpdates.setSpan( new ForegroundColorSpan( col ), 0, summaryPerformUpdates.length(), 0 );
        	titlePerformUpdates.setSpan( new ForegroundColorSpan( col ), 0, titlePerformUpdates.length(), 0 );
        	// updates interval
        	summaryUpdateInterval.setSpan( new ForegroundColorSpan( col ), 0, summaryUpdateInterval.length(), 0 );
        	titleUpdateInterval.setSpan( new ForegroundColorSpan( col ), 0, titleUpdateInterval.length(), 0 );
        	// =====
        	// layout category
        	// =====
        	titleLayoutCategory.setSpan( new ForegroundColorSpan( col ), 0, titleLayoutCategory.length(), 0 );
        	// invert colors
        	summaryInvertColors.setSpan( new ForegroundColorSpan( col ), 0, summaryInvertColors.length(), 0 );
        	titleInvertColors.setSpan( new ForegroundColorSpan( col ), 0, titleInvertColors.length(), 0 );
        	// lock horizontal
        	summaryLockHorizontal.setSpan( new ForegroundColorSpan( col ), 0, summaryLockHorizontal.length(), 0 );
        	titleLockHorizontal.setSpan( new ForegroundColorSpan( col ), 0, titleLockHorizontal.length(), 0 );
        	// show images
        	summaryShowImages.setSpan( new ForegroundColorSpan( col ), 0, summaryShowImages.length(), 0 );
        	titleShowImages.setSpan( new ForegroundColorSpan( col ), 0, titleShowImages.length(), 0 );
        	// =====
        	// storage category
        	// =====
        	titleStorageCategory.setSpan( new ForegroundColorSpan( col ), 0, titleStorageCategory.length(), 0 );
        	// clear database
        	summaryClearDatabase.setSpan( new ForegroundColorSpan( col ), 0, summaryClearDatabase.length(), 0 );
        	titleClearDatabase.setSpan( new ForegroundColorSpan( col ), 0, titleClearDatabase.length(), 0 );
        	// clear image cache
        	summaryClearImageCache.setSpan( new ForegroundColorSpan( col ), 0, summaryClearImageCache.length(), 0 );
        	titleClearImageCache.setSpan( new ForegroundColorSpan( col ), 0, titleClearImageCache.length(), 0 );
        }
    	// =====
    	// update category
    	// =====
        preferenceUpdateCategory.setTitle( titleUpdateCategory );
    	// perform updates
        preferencePerformUpdates.setSummary( summaryPerformUpdates );
        preferencePerformUpdates.setTitle( titlePerformUpdates );
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
	
	void DeleteRecursive(File fileOrDirectory) {
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
