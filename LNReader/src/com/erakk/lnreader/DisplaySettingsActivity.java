package com.erakk.lnreader;

import com.erakk.lnreader.helper.DBHelper;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;




public class DisplaySettingsActivity extends PreferenceActivity {
	//private static final String TAG = DisplayLightNovelsActivity.class.toString();
	
	@SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {    	
        super.onCreate(savedInstanceState);
      //This man is deprecated but...
        addPreferencesFromResource(R.xml.preferences);
        
        Preference clearCache = (Preference)  findPreference("clear_cache");
        clearCache.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
        	@Override
            public boolean onPreferenceClick(Preference p) {
                // TODO stuff              
        		
        		/*
        		 * CODE TO CLEAR CACHE HERE
        		 */
        		
        		Toast t = Toast.makeText(getApplicationContext(), "Cache cleared!", Toast.LENGTH_LONG);
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
        		DBHelper helper = new DBHelper(getApplicationContext());
        		helper.deletePagesDB();
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
        		
        		Toast t = Toast.makeText(getApplicationContext(), "Image cache cleared!", Toast.LENGTH_LONG);
    			t.show();		
        		
                return true;
            }
        });
        
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
