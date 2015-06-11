package com.erakk.lnreader.activity;

import android.app.Activity;
import android.app.LocalActivityManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

import com.erakk.lnreader.AlternativeLanguageInfo;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UI.activity.BaseActivity;
import com.erakk.lnreader.UIHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

/*
 * Modified by : freedomofkeima
 */

@SuppressWarnings("deprecation")
public class DisplayAlternativeNovelPagerActivity extends BaseActivity {

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

		tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
			@Override
			public void onTabChanged(String tabId) {
				// setTabColor();
				currentActivity = lam.getActivity(tabId);
			}
		});

		/* Dynamically add Tabs */
		ArrayList<String> Choice = new ArrayList<String>();

		Iterator<Entry<String, AlternativeLanguageInfo>> it = AlternativeLanguageInfo.getAlternativeLanguageInfo().entrySet().iterator();
		while (it.hasNext()) {
			AlternativeLanguageInfo info = it.next().getValue();
			boolean isChosen = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(info.getLanguage(), true);
			if (isChosen) {
				Choice.add(info.getLanguage());
				Log.d("Language Added: ", info.getLanguage());
			}
			it.remove();
		}

		TabSpec[] allSpec = new TabSpec[Choice.size()];
		for (int i = 0; i < Choice.size(); i++) {
			allSpec[i] = tabHost.newTabSpec(Choice.get(i));
			allSpec[i].setIndicator(Choice.get(i));
			Intent firstIntent = new Intent(this, DisplayAlternativeNovelListActivity.class);
			firstIntent.putExtra("LANG", Choice.get(i));
			allSpec[i].setContent(firstIntent);

			// Adding all TabSpec to TabHost
			tabHost.addTab(allSpec[i]);

			// Cheap preload list hack.
			tabHost.setCurrentTabByTag(Choice.get(i));
		}

		// Tab color
		// setTabColor();
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
