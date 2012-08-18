package com.erakk.lnreader;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean invertColors = sharedPrefs.getBoolean("invert_colors", false);

        View MainView = findViewById(R.id.main_screen);
        Button Button1 = (Button)findViewById(R.id.button1);
        Button Button2 = (Button)findViewById(R.id.button2);
        Button Button3 = (Button)findViewById(R.id.button3);
        
        if (invertColors == true) {
        	MainView.setBackgroundColor(Color.BLACK);
        	Button1.setTextColor(Color.WHITE);
        	Button1.setBackgroundColor(Color.DKGRAY);
        	Button2.setTextColor(Color.WHITE);
        	Button2.setBackgroundColor(Color.DKGRAY);
        	Button3.setTextColor(Color.WHITE);
        	Button3.setBackgroundColor(Color.DKGRAY);
        	
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
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
    			
    			/*
    			 * Implement code to invert colors
    			 */
    			
    			Toast.makeText(getApplicationContext(), "Colors inverted", Toast.LENGTH_SHORT).show();
    			return true;
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    public void openNovelList(View view) {
    	Intent intent = new Intent(this, DisplayLightNovelsActivity.class);
    	intent.putExtra(Constants.EXTRA_ONLY_WATCHED, false);
    	startActivity(intent);
    }
    
    public void openWatchList(View view) {
    	Intent intent = new Intent(this, DisplayLightNovelsActivity.class);
    	intent.putExtra(Constants.EXTRA_ONLY_WATCHED, true);
    	startActivity(intent);
    }
    
    public void openSettings(View view) {
    	Intent intent = new Intent(this, DisplaySettingsActivity.class);
    	startActivity(intent);
    }
}
