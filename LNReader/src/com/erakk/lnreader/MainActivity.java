package com.erakk.lnreader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;


public class MainActivity extends Activity {

    @SuppressLint("NewApi")
	@Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.d("MainActivity", "onCreate");
    	// set before create any view
    	if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("invert_colors", false)) {    		
    		setTheme(R.style.AppTheme2);
    	}
    	else {
    		setTheme(R.style.AppTheme);
    	}
    	
        super.onCreate(savedInstanceState);
        
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB )
        	getActionBar().setDisplayHomeAsUpEnabled(false);
        
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    @SuppressLint("NewApi")
	@Override
    protected void onRestart() {
        super.onRestart();
        recreate();
    }
    
    @SuppressLint("NewApi")
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        	case R.id.last_novel:
        		
        		/*
        		 * Implement code to load last chapter read
        		 */        		
        		Toast last = Toast.makeText(this, "Last Novel read option not implemented yet.", Toast.LENGTH_SHORT);
        		last.show();
        		return true;
        	case R.id.invert_colors:
    			
        		toggleColorPref();
        		// recreate the view to use new theme
        		recreate();
    			
    			Toast.makeText(getApplicationContext(), "Colors inverted", Toast.LENGTH_SHORT).show();
    			return true;
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void toggleColorPref () { 
    	SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    	SharedPreferences.Editor editor = sharedPrefs.edit();
    	if (sharedPrefs.getBoolean("invert_colors", false)) {
    		editor.putBoolean("invert_colors", false);
    	}
    	else {
    		editor.putBoolean("invert_colors", true);
    	}
    	editor.commit();
    }
    
    public void openNovelList(View view) {
    	Intent intent = new Intent(this, DisplayLightNovelListActivity.class);
    	intent.putExtra(Constants.EXTRA_ONLY_WATCHED, false);
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
}
