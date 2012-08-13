package com.erakk.lnreader;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
        		Toast last = Toast.makeText(this, "Last Novel read option not implemented yet.", Toast.LENGTH_SHORT);
        		last.show();
        		//Intent launchNewIntent = new Intent(this, DisplaySettingsActivity.class);
        		//startActivity(launchNewIntent);
        		return true;
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    public void openNovelList(View view) {
    	Intent intent = new Intent(this, DisplayLightNovelsActivity.class);
    	startActivity(intent);
    }
    
    public void openWatchList(View view) {
    	Intent intent = new Intent(this, DisplayWatchListActivity.class);
    	startActivity(intent);
    }
    
    public void openSettings(View view) {
    	Intent intent = new Intent(this, DisplaySettingsActivity.class);
    	startActivity(intent);
    }
}
