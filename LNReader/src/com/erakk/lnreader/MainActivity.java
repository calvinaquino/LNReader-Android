package com.erakk.lnreader;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;

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
