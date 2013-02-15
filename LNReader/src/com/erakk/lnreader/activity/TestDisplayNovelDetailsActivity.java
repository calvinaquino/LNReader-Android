package com.erakk.lnreader.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.fragment.DisplayLightNovelDetailsFragment;
import com.erakk.lnreader.fragment.DisplaySynopsisFragment;

public class TestDisplayNovelDetailsActivity extends SherlockFragmentActivity {

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {		
		super.onCreate(null);	// This is to destroy the savedInstanceState so that the fragments don't get created twice
        UIHelper.SetTheme(this, R.layout.fragactivity_framework);
        UIHelper.SetActionBarDisplayHomeAsUp(this, true);
        setContentView(R.layout.fragactivity_framework);
		
        Bundle fromPevIntent = getIntent().getExtras();
        
        if(findViewById(R.id.rightFragment) != null) {	
        	fromPevIntent.putBoolean("show_list_child", false);
        	
        	Fragment synopsis_panel = new DisplaySynopsisFragment();
        	synopsis_panel.setArguments(fromPevIntent);
        	
        	Fragment list = new DisplayLightNovelDetailsFragment();
        	list.setArguments(fromPevIntent);
        	
        	getSupportFragmentManager().beginTransaction().replace(R.id.leftFragment, synopsis_panel).commit();
        	getSupportFragmentManager().beginTransaction().replace(R.id.rightFragment, list).commit();
        	
        } else {
        	fromPevIntent.putBoolean("show_list_child", true);
        	
        	Fragment list = new DisplayLightNovelDetailsFragment();
        	list.setArguments(fromPevIntent);
        	
        	getSupportFragmentManager().beginTransaction().replace(R.id.mainFrame, list).disallowAddToBackStack().commit();
        }
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.fragactivity_display_novel_list, menu);
		return true;
	}
	
	@Override
	public void finish() {
		super.finish();
		overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			Intent launchNewIntent = new Intent(this, DisplaySettingsActivity.class);
			startActivity(launchNewIntent);
			return true;
		case R.id.invert_colors:			
			UIHelper.ToggleColorPref(this);
			UIHelper.Recreate(this);
			return true;
		case R.id.menu_bookmarks:
    		Intent bookmarkIntent = new Intent(this, DisplayBookmarkActivity.class);
        	startActivity(bookmarkIntent);
			return true;    
		case R.id.menu_downloads:
    		Intent downloadsItent = new Intent(this, DownloadListActivity.class);
        	startActivity(downloadsItent);;
			return true; 
		case android.R.id.home:
			super.onBackPressed();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}		
	}
}
