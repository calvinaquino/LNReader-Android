package com.erakk.lnreader.activity;

import android.app.Activity;
import android.app.LocalActivityManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UI.activity.BaseActivity;
import com.erakk.lnreader.UIHelper;

@SuppressWarnings("deprecation")
public class DisplayNovelPagerActivity extends BaseActivity {
	// TabSpec Names
	private static final String MAIN_SPEC = "Main";
	private static final String TEASER_SPEC = "Teaser";
	private static final String ORIGINAL_SPEC = "Original";
	private static final String TAG = DisplayNovelPagerActivity.class.toString();
	static TabHost tabHost;
	private boolean isInverted;
	LocalActivityManager lam;
	private Activity currentActivity = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			UIHelper.SetTheme(this, R.layout.activity_display_novel_pager);
			UIHelper.SetActionBarDisplayHomeAsUp(this, true);
			setContentView(R.layout.activity_display_novel_pager);
		} else {
			UIHelper.SetTheme(this, R.layout.activity_display_novel_pager_fix);
			UIHelper.SetActionBarDisplayHomeAsUp(this, true);
			setContentView(R.layout.activity_display_novel_pager_fix);
		}
		tabHost = (TabHost) findViewById(android.R.id.tabhost);
		lam = new LocalActivityManager(this, false);
		lam.dispatchCreate(savedInstanceState);
		tabHost.setup(lam);
		isInverted = UIHelper.getColorPreferences(this);

		// First Tab - Normal Novels
		TabSpec firstSpec = tabHost.newTabSpec(MAIN_SPEC);
		firstSpec.setIndicator(MAIN_SPEC);
		Intent firstIntent = new Intent(this, DisplayLightNovelListActivity.class);
		firstIntent.putExtra(Constants.EXTRA_ONLY_WATCHED, false);
		firstIntent.putExtra(Constants.EXTRA_NOVEL_LIST_MODE, Constants.EXTRA_NOVEL_LIST_MODE_MAIN);
		firstSpec.setContent(firstIntent);

		// Second Tab - Teasers
		TabSpec secondSpec = tabHost.newTabSpec(TEASER_SPEC);
		secondSpec.setIndicator(TEASER_SPEC);
		Intent secondIntent = new Intent(this, DisplayLightNovelListActivity.class);
		secondIntent.putExtra(Constants.EXTRA_NOVEL_LIST_MODE, Constants.EXTRA_NOVEL_LIST_MODE_TEASER);
		secondSpec.setContent(secondIntent);

		// Third Tab - Original
		TabSpec thirdSpec = tabHost.newTabSpec(ORIGINAL_SPEC);
		thirdSpec.setIndicator(ORIGINAL_SPEC);
		Intent thirdIntent = new Intent(this, DisplayLightNovelListActivity.class);
		thirdIntent.putExtra(Constants.EXTRA_NOVEL_LIST_MODE, Constants.EXTRA_NOVEL_LIST_MODE_ORIGINAL);
		thirdSpec.setContent(thirdIntent);

		// Adding all TabSpec to TabHost
		tabHost.addTab(firstSpec); // Adding First tab
		tabHost.addTab(secondSpec); // Adding Second tab
		tabHost.addTab(thirdSpec); // Adding third tab
		// setTabColor();

		tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
			@Override
			public void onTabChanged(String tabId) {
				// setTabColor();
				currentActivity = lam.getActivity(tabId);
			}
		});

		// Cheap preload list hack.
		tabHost.setCurrentTabByTag(TEASER_SPEC);
		tabHost.setCurrentTabByTag(ORIGINAL_SPEC);
		tabHost.setCurrentTabByTag(MAIN_SPEC);

		Log.d(TAG, "Created");
	}

	@Override
	protected void onPause() {
		super.onPause();
		lam.dispatchPause(isFinishing());
	}

	@Override
	protected void onResume() {
		super.onResume();
		lam.dispatchResume();
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		if (isInverted != UIHelper.getColorPreferences(this)) {
			UIHelper.Recreate(this);
		}
	}

	// public static void setTabColor() {
	// for (int i = 0; i < tabHost.getTabWidget().getChildCount(); i++) {
	// // tabHost.getTabWidget().getChildAt(i).setBackgroundColor(Color.parseColor("#2D5A9C")); //unselected
	// tabHost.getTabWidget().getChildAt(i).setBackgroundColor(Color.parseColor("#000000")); // unselected
	// }
	// // tabHost.getTabWidget().getChildAt(tabHost.getCurrentTab()).setBackgroundColor(Color.parseColor("#234B7E"));
	// // // selected
	// tabHost.getTabWidget().getChildAt(tabHost.getCurrentTab()).setBackgroundColor(Color.parseColor("#708090")); //
	// selected
	// }

	public static TabHost getMainTabHost() {
		return tabHost;
	}

	// Option Menu related
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_display_light_novel_list, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Activity activity = currentActivity;

		switch (item.getItemId()) {
		case R.id.menu_settings:
			Intent launchNewIntent = new Intent(this, DisplaySettingsActivity.class);
			startActivity(launchNewIntent);
			return true;
		case R.id.menu_refresh_novel_list:
			if (activity instanceof INovelListHelper)
				((INovelListHelper) activity).refreshList();
			return true;
		case R.id.invert_colors:
			UIHelper.ToggleColorPref(this);
			UIHelper.Recreate(this);
			return true;
		case R.id.menu_manual_add:
			if (activity instanceof INovelListHelper)
				((INovelListHelper) activity).manualAdd();
			return true;
		case R.id.menu_download_all_info:
			if (activity instanceof INovelListHelper)
				((INovelListHelper) activity).downloadAllNovelInfo();
			return true;
		case R.id.menu_bookmarks:
			Intent bookmarkIntent = new Intent(this, DisplayBookmarkActivity.class);
			startActivity(bookmarkIntent);
			return true;
		case R.id.menu_downloads_list:
			Intent downloadsItent = new Intent(this, DownloadListActivity.class);
			startActivity(downloadsItent);
			return true;
		case android.R.id.home:
			super.onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}