package com.erakk.lnreader.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;


public class MainActivity extends Activity {
	private boolean isInverted;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UIHelper.SetTheme(this, R.layout.activity_main);
        UIHelper.SetActionBarDisplayHomeAsUp(this, false);
        isInverted = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_INVERT_COLOR, false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
	@Override
    protected void onRestart() {
        super.onRestart();
        if(isInverted != PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_INVERT_COLOR, false)) {
        	UIHelper.Recreate(this);
        }
    }
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        	case R.id.last_novel:        		
        		/*
        		 * Implement code to load last chapter read
        		 */
        		String lastReadPage = PreferenceManager.getDefaultSharedPreferences(this).getString(Constants.PREF_LAST_READ, "");
        		if(lastReadPage.length() > 0) {
        			Intent intent = new Intent(getApplicationContext(), DisplayLightNovelContentActivity.class);
			        intent.putExtra(Constants.EXTRA_PAGE, lastReadPage);
			        startActivity(intent);
        			//Toast.makeText(this, "Loading: " + lastReadPage, Toast.LENGTH_SHORT).show();
        		}
        		else{
        			Toast.makeText(this, "No last read novel.", Toast.LENGTH_SHORT).show();
        		}
        		return true;
        	case R.id.invert_colors:    			
        		UIHelper.ToggleColorPref(this);
        		UIHelper.Recreate(this);    			
    			return true;
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
	
    public void openNovelList(View view) {
    	Intent intent = new Intent(this, DisplayLightNovelListActivity.class);
    	intent.putExtra(Constants.EXTRA_ONLY_WATCHED, false);
    	startActivity(intent);
    }
    
    public void openTeaserList(View view) {
    	Intent intent = new Intent(this, DisplayTeaserListActivity.class);
    	startActivity(intent);
    }
    
    public void openWatchList(View view) {
    	Intent intent = new Intent(this, DisplayLightNovelListActivity.class);
    	intent.putExtra(Constants.EXTRA_ONLY_WATCHED, true);
    	startActivity(intent);
    }
    
    public void openSettings(View view) {
    	Intent intent = new Intent(this, DisplaySettingsActivity.class);
    	startActivity(intent);
    }

    public void openSearch(View view) {
    	Intent intent = new Intent(this, DisplaySearchActivity.class);
    	startActivity(intent);
    } 
}
