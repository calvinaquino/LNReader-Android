package com.erakk.lnreader.activity;

import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.erakk.lnreader.Constants;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.adapter.BookmarkModelAdapter;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.model.BookmarkModel;

public class DisplayBookmarkActivity extends SherlockListActivity {
	private boolean isInverted;
	private BookmarkModelAdapter adapter = null;
	private ArrayList<BookmarkModel> bookmarks = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		UIHelper.SetTheme(this, R.layout.activity_display_bookmark);
		UIHelper.SetActionBarDisplayHomeAsUp(this, true);

		isInverted = getColorPreferences();
		setTitle(getResources().getString(R.string.bookmarks));
		getBookmarks();
	}

	private void getBookmarks() {
		int resourceId = R.layout.bookmark_list_item;
		if (UIHelper.IsSmallScreen(this)) {
			resourceId = R.layout.bookmark_list_item;
		}
		bookmarks = NovelsDao.getInstance(this).getAllBookmarks();
		adapter = new BookmarkModelAdapter(this, resourceId, bookmarks, null);
		adapter.showPage = true;
		adapter.showCheckBox = true;
		setListAdapter(adapter);
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		if (isInverted != getColorPreferences()) {
			UIHelper.Recreate(this);
		}
		if (adapter != null)
			adapter.notifyDataSetChanged();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getSupportMenuInflater().inflate(R.menu.activity_display_bookmark, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.invert_colors:
			UIHelper.ToggleColorPref(this);
			UIHelper.Recreate(this);
			return true;
		case R.id.menu_downloads_list:
			Intent downloadsItent = new Intent(this, DownloadListActivity.class);
			startActivity(downloadsItent);
			return true;
		case R.id.menu_bookmark_delete_selected:
			if (bookmarks != null) {
				for (BookmarkModel bookmark : bookmarks) {
					if (bookmark.isSelected())
						NovelsDao.getInstance(this).deleteBookmark(bookmark);
				}
				if (adapter != null)
					adapter.refreshData();
			}
			return true;
		case android.R.id.home:
			super.onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		// Get the item that was clicked
		BookmarkModel page = adapter.getItem(position);
		// Create new intent
		Intent intent = new Intent(getApplicationContext(), DisplayLightNovelContentActivity.class);
		intent.putExtra(Constants.EXTRA_PAGE, page.getPage());
		intent.putExtra(Constants.EXTRA_P_INDEX, page.getpIndex());
		startActivity(intent);
	}

	private boolean getColorPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_INVERT_COLOR, true);
	}
}
