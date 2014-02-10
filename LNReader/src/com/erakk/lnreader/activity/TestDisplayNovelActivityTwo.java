package com.erakk.lnreader.activity;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.erakk.lnreader.Constants;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.fragment.DisplayLightNovelListFragment;
import com.erakk.lnreader.fragment.DisplayNovelTabFragment;

public class TestDisplayNovelActivityTwo extends SherlockFragmentActivity implements
DisplayLightNovelListFragment.FragmentListener{

	private boolean isInverted;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(null);
		UIHelper.SetTheme(this, R.layout.fragactivity_framework);
		UIHelper.SetActionBarDisplayHomeAsUp(this, true);
		setContentView(R.layout.fragactivity_framework);

		isInverted = getColorPreferences();

		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

		if(findViewById(R.id.rightFragment) != null) {
			transaction.replace(R.id.rightFragment, new DisplayNovelTabFragment()).commit();
		} else {
			transaction.replace(R.id.mainFrame, new DisplayNovelTabFragment()).commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.fragactivity_display_novel_list, menu);
		return true;
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		if(isInverted != getColorPreferences()) {
			UIHelper.Recreate(this);
		}
		//if(adapter != null) adapter.notifyDataSetChanged();
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
		case R.id.menu_downloads_list:
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

	private boolean getColorPreferences(){
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_INVERT_COLOR, true);
	}

	@Override
	public void changeNextFragment(Bundle bundle) {
		Intent intent = new Intent(this, TestDisplayNovelDetailsActivity.class);
		intent.putExtras(bundle);

		startActivity(intent);
		overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
	}
}
