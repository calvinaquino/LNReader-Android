package com.erakk.lnreader.activity;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.dao.NovelsDao;

public class DisplaySettingsActivity extends PreferenceActivity {
	//private static final String TAG = DisplayLightNovelsActivity.class.toString();
	private Activity activity;
	
	@SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
		// must be set before create any view when there is no layout
    	UIHelper.SetTheme(this, null);
		UIHelper.SetActionBarDisplayHomeAsUp(this, true);
    	
		super.onCreate(savedInstanceState);
    	activity = this;
        
        //This man is deprecated but but we may want to be bale to run on older API
        addPreferencesFromResource(R.xml.preferences);
        
//        updateViewColor();
        
        Preference lockHorizontal = (Preference)  findPreference("lock_horizontal");
        lockHorizontal.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
        	@Override
            public boolean onPreferenceClick(Preference p) {
        		if(p.getSharedPreferences().getBoolean("lock_horizontal", false)){
        			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
	        		Toast.makeText(getApplicationContext(), "Orientation Locked" , Toast.LENGTH_SHORT).show();
        		}
        		else {
        			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
	        		Toast.makeText(getApplicationContext(), "Orientation Unlocked" , Toast.LENGTH_SHORT).show();
        		}        		
        		return true;
            }
        });
        
        Preference invertColors = (Preference)  findPreference("invert_colors");
        invertColors.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
        	@Override
            public boolean onPreferenceClick(Preference p) {
        		UIHelper.Recreate(activity);
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
    
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//    	menu.add(Menu.NONE, 0, 0, "Show current settings");
//    	return super.onCreateOptionsMenu(menu);
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//    	switch (item.getItemId()) {
//    		case 0:
//    			startActivity(new Intent(this, DisplaySettingsActivity.class));
//    			return true;
//    	}
//    	return false;
//    }

	public boolean onPreferenceChange(Preference preference, Object newValue) {
		// TODO Auto-generated method stub
		return false;
	}
}
