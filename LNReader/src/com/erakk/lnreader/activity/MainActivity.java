package com.erakk.lnreader.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;

import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.erakk.lnreader.Constants;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;

public class MainActivity extends SherlockActivity {
	private static final String TAG = MainActivity.class.toString();
	private boolean isInverted;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			UIHelper.SetTheme(this, R.layout.activity_main);
		else {
			UIHelper.SetTheme(this, R.layout.activity_main_no_tab);
		}
		UIHelper.SetActionBarDisplayHomeAsUp(this, false);
		isInverted = getColorPreferences();
		setIconColor();

		if (isFirstRun()) {
			// Show copyrights

			new AlertDialog.Builder(this).setTitle("Terms of Use").setMessage("Before using this application, keep in mind that we, the developers of BakaTsuki EX, are not responsible for the content displayed by the application in any way. Therefore, you must read and agree to the TLG Translation Common Agreement of Baka-Tsuki.org:\n\n" + getString(R.string.bakatsuki_copyrights) + "\n\nBy clicking \"I Agree\" below, you confirm that you have read the TLG Translation Common Agreement in it's entirety.").setPositiveButton("I Agree", new OnClickListener() {

				public void onClick(DialogInterface dialog, int which) {
					setFirstRun();
				}
			}).setNegativeButton("Exit App", new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			}).show();
		}
		Log.d(TAG, "Main created.");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		if (isInverted != getColorPreferences()) {
			UIHelper.Recreate(this);
			setIconColor();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.invert_colors:
			UIHelper.ToggleColorPref(this);
			UIHelper.Recreate(this);
			setIconColor();
			return true;
		case android.R.id.home:
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void openNovelList(View view) {
		String ui = PreferenceManager.getDefaultSharedPreferences(this).getString(Constants.PREF_UI_SELECTION, "0");
		if(ui.equalsIgnoreCase("0")) {
			Intent intent = new Intent(this, DisplayNovelPagerActivity.class);
			intent.putExtra(Constants.EXTRA_ONLY_WATCHED, false);
			startActivity(intent);
		}
		else if (ui.equalsIgnoreCase("1")) {
			Intent intent = new Intent(this, TestDisplayNovelActivity.class);
			intent.putExtra(Constants.EXTRA_ONLY_WATCHED, false);
			startActivity(intent);
		}
		else if (ui.equalsIgnoreCase("2")) {
			Intent intent = new Intent(this, TestDisplayNovelActivityTwo.class);
			intent.putExtra(Constants.EXTRA_ONLY_WATCHED, false);
			startActivity(intent);
		}
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
		// FOR TESTING
		// resetFirstRun();
	}

	public void jumpLastRead(View view) {
		String lastReadPage = PreferenceManager.getDefaultSharedPreferences(this).getString(Constants.PREF_LAST_READ, "");
		if (lastReadPage.length() > 0) {
			Intent intent = new Intent(getApplicationContext(), DisplayLightNovelContentActivity.class);
			intent.putExtra(Constants.EXTRA_PAGE, lastReadPage);
			startActivity(intent);
		} else {
			Toast.makeText(this, "You have no novel to resume reading.", Toast.LENGTH_SHORT).show();
		}
	}
	private void setIconColor() {
		LinearLayout rightMenu = (LinearLayout) findViewById(R.id.menu_right);
		int childCount = rightMenu.getChildCount();
		for (int i = 0; i < childCount; ++i) {
			ImageButton btn = (ImageButton) rightMenu.getChildAt(i);
			btn.setImageDrawable(UIHelper.setColorFilter(btn.getDrawable()));
		}
	}

	private boolean getColorPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_INVERT_COLOR, true);
	}

	private boolean isFirstRun() {
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_FIRST_RUN, true);
	}

	private void setFirstRun() {
		SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(this).edit();
		edit.putBoolean(Constants.PREF_FIRST_RUN, false);
		edit.commit();
	}
}
