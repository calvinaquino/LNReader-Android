package com.erakk.lnreader.activity;

import java.io.File;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.widget.Toast;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.service.MyScheduleReceiver;

public class DisplaySettingsActivity extends PreferenceActivity {
	//private static final String TAG = DisplayLightNovelsActivity.class.toString();
	private Activity activity;
	
	@SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
		UIHelper.SetTheme(this, null);
		super.onCreate(savedInstanceState);
		UIHelper.SetActionBarDisplayHomeAsUp(this, true);   	
		
    	activity = this;
        
        //This man is deprecated but but we may want to be able to run on older API
        addPreferencesFromResource(R.xml.preferences);
        
        // Screen Orientation
        Preference lockHorizontal = (Preference)  findPreference(Constants.PREF_LOCK_HORIZONTAL);
        lockHorizontal.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference p) {
        		if(p.getSharedPreferences().getBoolean(Constants.PREF_LOCK_HORIZONTAL, false)){
        			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        		}
        		else {
        			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        		}        		
        		return true;
            }
        });
        
        // Invert Color
        Preference invertColors = (Preference)  findPreference(Constants.PREF_INVERT_COLOR);
        invertColors.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference p) {
        		UIHelper.Recreate(activity);
                return true;
            }
        });
        
        Preference clearDatabase = (Preference)  findPreference("clear_database");
        clearDatabase.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
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
            public boolean onPreferenceClick(Preference p) {
        		/*
        		 * CODE TO CLEAR IMAGE CACHE HERE
        		 */        		
        		DeleteRecursive(new File(Constants.IMAGE_ROOT));
        		Toast.makeText(getApplicationContext(), "Image cache cleared!", Toast.LENGTH_LONG).show();	
        		
                return true;
            }
        });
        
        final Preference updatesInterval = (Preference)  findPreference(Constants.PREF_UPDATE_INTERVAL);
        updatesInterval.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				int updatesIntervalInt = Integer.parseInt(newValue.toString());	
				MyScheduleReceiver.reschedule(getApplicationContext(), updatesIntervalInt);
                return true;
			}
		});
        
    }
    
	@Override
    protected void onRestart() {
        super.onRestart();
        UIHelper.Recreate(this);
    }
	
	private void DeleteRecursive(File fileOrDirectory) {
	    if (fileOrDirectory.isDirectory())
	        for (File child : fileOrDirectory.listFiles())
	            DeleteRecursive(child);
	    fileOrDirectory.delete();
	}
}
