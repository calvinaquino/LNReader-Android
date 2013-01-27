package com.erakk.lnreader.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
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
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB )
            UIHelper.SetTheme(this, R.layout.activity_main);
		else{
	        UIHelper.SetTheme(this, R.layout.activity_main_no_tab);
		}
        UIHelper.SetActionBarDisplayHomeAsUp(this, false);
        isInverted = getColorPreferences();


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
	@Override
    protected void onRestart() {
        super.onRestart();
        if(isInverted != getColorPreferences()) {
        	UIHelper.Recreate(this);
        }
    }
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        	case R.id.invert_colors:    			
        		UIHelper.ToggleColorPref(this);
        		UIHelper.Recreate(this);    			
    			return true;
        	case R.id.menu_search:    			
        		Intent intent = new Intent(this, DisplaySearchActivity.class);
            	startActivity(intent);  			
    			return true;
        	case R.id.menu_bookmarks:
        		Intent bookmarkIntent = new Intent(this, DisplayBookmarkActivity.class);
            	startActivity(bookmarkIntent);
    			return true;   
        	case R.id.menu_settings:
        		Intent settingsIntent = new Intent(this, DisplaySettingsActivity.class);
            	startActivity(settingsIntent);
    			return true; 
        	case R.id.menu_downloads:
        		Intent downloadsIntent = new Intent(this, DownloadListActivity.class);
            	startActivity(downloadsIntent);;
    			return true; 
        	case R.id.menu_update_history:
        		Intent updateHistoryIntent  = new Intent(this, UpdateHistoryActivity.class);
            	startActivity(updateHistoryIntent);;
    			return true;
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
	
    public void openNovelList(View view) {
//    	Intent intent = new Intent(this, DisplayLightNovelListActivity.class);
    	Intent intent = new Intent(this, DisplayNovelPagerActivity.class);
    	intent.putExtra(Constants.EXTRA_ONLY_WATCHED, false);
    	startActivity(intent);
    }
    public void openNovelListNoTab(View view) {
    	Intent intent = new Intent(this, DisplayLightNovelListActivity.class);
    	intent.putExtra(Constants.EXTRA_ONLY_WATCHED, false);
    	startActivity(intent);
    }
    
    public void openTeaserList(View view) {
    	Intent intent = new Intent(this, DisplayTeaserListActivity.class);
    	startActivity(intent);
    }
    
    public void openOriginalsList(View view) {
    	Intent intent = new Intent(this, DisplayOriginalListActivity.class);
    	startActivity(intent);
    }
    public void openDownloadsList(View view) {
    	Intent intent = new Intent(this, DownloadListActivity.class);
    	startActivity(intent);
    }
    public void openUpdatesList(View view) {
    	Intent intent = new Intent(this, UpdateHistoryActivity.class);
    	startActivity(intent);
    }
    public void openBookmarks(View view) {
    	Intent bookmarkIntent = new Intent(this, DisplayBookmarkActivity.class);
    	startActivity(bookmarkIntent);
    }
    public void openSearch(View view) {
    	Intent intent = new Intent(this, DisplaySearchActivity.class);
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
    
    public void jumpLastRead(View view) {
		String lastReadPage = PreferenceManager.getDefaultSharedPreferences(this).getString(Constants.PREF_LAST_READ, "");
		if(lastReadPage.length() > 0) {
			Intent intent = new Intent(getApplicationContext(), DisplayLightNovelContentActivity.class);
	        intent.putExtra(Constants.EXTRA_PAGE, lastReadPage);
	        startActivity(intent);
		}
		else{
			Toast.makeText(this, "You have no novel to resume reading.", Toast.LENGTH_SHORT).show();
		}
    }
    
//    public void openDownloads(View view) {
//    	Intent intent = new Intent(this, DownloadListActivity.class);
//    	startActivity(intent);
//    }

	private boolean getColorPreferences(){
    	return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_INVERT_COLOR, true);
	}
}
