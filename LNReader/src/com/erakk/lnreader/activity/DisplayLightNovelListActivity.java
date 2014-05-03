package com.erakk.lnreader.activity;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.erakk.lnreader.Constants;
import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.adapter.PageModelAdapter;
import com.erakk.lnreader.callback.CallbackEventData;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.Util;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.PageModel;
import com.erakk.lnreader.task.AddNovelTask;
import com.erakk.lnreader.task.AsyncTaskResult;
import com.erakk.lnreader.task.DownloadNovelDetailsTask;
import com.erakk.lnreader.task.IAsyncTaskOwner;
import com.erakk.lnreader.task.LoadNovelsTask;

/*
 * Author: Nandaka
 * Copy from: NovelsActivity.java
 */

public class DisplayLightNovelListActivity extends SherlockListActivity implements IAsyncTaskOwner, INovelListHelper {
	private static final String TAG = DisplayLightNovelListActivity.class.toString();
	private final ArrayList<PageModel> listItems = new ArrayList<PageModel>();
	private PageModelAdapter adapter;
	private LoadNovelsTask task = null;
	private DownloadNovelDetailsTask downloadTask = null;
	private AddNovelTask addTask = null;
	private String mode;

	private boolean isInverted;
	private boolean onlyWatched = false;
	String touchedForDownload;

	private TextView loadingText;
	private ProgressBar loadingBar;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		UIHelper.SetTheme(this, R.layout.activity_display_light_novel_list);
		UIHelper.SetActionBarDisplayHomeAsUp(this, true);

		loadingText = (TextView) findViewById(R.id.emptyList);
		loadingBar = (ProgressBar) findViewById(R.id.empttListProgress);

		registerForContextMenu(getListView());

		mode = getIntent().getStringExtra(Constants.EXTRA_NOVEL_LIST_MODE);
		if (Util.isStringNullOrEmpty(mode))
			mode = Constants.EXTRA_NOVEL_LIST_MODE_MAIN;

		if (mode.equalsIgnoreCase(Constants.EXTRA_NOVEL_LIST_MODE_MAIN)) {
			onlyWatched = getIntent().getBooleanExtra(Constants.EXTRA_ONLY_WATCHED, false);

			if (onlyWatched) {
				setTitle(getResources().getString(R.string.watched_light_novels));
			} else {
				setTitle(getResources().getString(R.string.light_novels));
			}
		}
		else if (mode.equalsIgnoreCase(Constants.EXTRA_NOVEL_LIST_MODE_ORIGINAL)) {
			setTitle("Light Novels: Original");
		}
		else if (mode.equalsIgnoreCase(Constants.EXTRA_NOVEL_LIST_MODE_TEASER)) {
			setTitle("Light Novels: Teasers");
		}

		registerForContextMenu(getListView());
		isInverted = UIHelper.getColorPreferences(this);

		// Encapsulated in updateContent
		updateContent(false, onlyWatched);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		// Get the item that was clicked
		PageModel o = adapter.getItem(position);
		String novel = o.toString();
		// Create new intent
		Intent intent = new Intent(this, DisplayLightNovelDetailsActivity.class);
		intent.putExtra(Constants.EXTRA_NOVEL, novel);
		intent.putExtra(Constants.EXTRA_PAGE, o.getPage());
		intent.putExtra(Constants.EXTRA_TITLE, o.getTitle());
		intent.putExtra(Constants.EXTRA_ONLY_WATCHED, getIntent().getBooleanExtra(Constants.EXTRA_ONLY_WATCHED, false));
		startActivity(intent);
		Log.d("DisplayLightNovelsActivity", o.getPage() + " (" + o.getTitle() + ")");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.activity_display_light_novel_list, menu);
		return true;
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		if (isInverted != UIHelper.getColorPreferences(this)) {
			UIHelper.Recreate(this);
		}
		if (adapter != null)
			adapter.notifyDataSetChanged();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			Intent launchNewIntent = new Intent(this, DisplaySettingsActivity.class);
			startActivity(launchNewIntent);
			return true;
		case R.id.menu_refresh_novel_list:
			refreshList();
			return true;
		case R.id.invert_colors:
			UIHelper.ToggleColorPref(this);
			UIHelper.Recreate(this);
			return true;
		case R.id.menu_manual_add:
			manualAdd();
			return true;
		case R.id.menu_download_all_info:
			downloadAllNovelInfo();
			return true;
		case R.id.menu_bookmarks:
			Intent bookmarkIntent = new Intent(this, DisplayBookmarkActivity.class);
			startActivity(bookmarkIntent);
			return true;
		case R.id.menu_downloads_list:
			Intent downloadsItent = new Intent(this, DownloadListActivity.class);
			startActivity(downloadsItent);
			;
			return true;
		case android.R.id.home:
			super.onBackPressed();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void refreshList() {
		boolean onlyWatched = getIntent().getBooleanExtra(Constants.EXTRA_ONLY_WATCHED, false);
		updateContent(true, onlyWatched);
		Toast.makeText(this, "Refreshing", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void downloadAllNovelInfo() {
		if (mode.equalsIgnoreCase(Constants.EXTRA_NOVEL_LIST_MODE_MAIN)) {
			if (onlyWatched)
				touchedForDownload = "Watched Light Novels information";
			else
				touchedForDownload = "All Main Light Novels information";
		} else if (mode.equalsIgnoreCase(Constants.EXTRA_NOVEL_LIST_MODE_ORIGINAL)) {
			touchedForDownload = "All Original Light Novels information";
		}
		else if (mode.equalsIgnoreCase(Constants.EXTRA_NOVEL_LIST_MODE_TEASER)) {
			touchedForDownload = "All Teaser Light Novels information";
		}
		executeDownloadTask(listItems);
	}

	@Override
	public void manualAdd() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(getResources().getString(R.string.add_novel_main, mode));
		LayoutInflater factory = LayoutInflater.from(this);
		View inputView = factory.inflate(R.layout.layout_add_new_novel, null);
		final EditText inputName = (EditText) inputView.findViewById(R.id.page);
		final EditText inputTitle = (EditText) inputView.findViewById(R.id.title);
		alert.setView(inputView);
		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				if (whichButton == DialogInterface.BUTTON_POSITIVE) {
					handleOK(inputName, inputTitle);
				}
			}
		});
		alert.setNegativeButton(getResources().getString(R.string.cancel), null);
		alert.show();
	}

	private void handleOK(EditText input, EditText inputTitle) {
		String novel = input.getText().toString();
		String title = inputTitle.getText().toString();
		if (novel != null && novel.length() > 0 && inputTitle != null && inputTitle.length() > 0) {
			PageModel temp = new PageModel();
			temp.setPage(novel);
			temp.setTitle(title);
			temp.setType(PageModel.TYPE_NOVEL);
			if (mode.equalsIgnoreCase(Constants.EXTRA_NOVEL_LIST_MODE_MAIN)) {
				temp.setParent("Main_Page");
			}
			else if (mode.equalsIgnoreCase(Constants.EXTRA_NOVEL_LIST_MODE_ORIGINAL)) {
				temp.setParent(Constants.ROOT_ORIGINAL);
				temp.setStatus(Constants.STATUS_ORIGINAL);
			}
			else if (mode.equalsIgnoreCase(Constants.EXTRA_NOVEL_LIST_MODE_TEASER)) {
				temp.setParent(Constants.ROOT_TEASER);
				temp.setStatus(Constants.STATUS_TEASER);
			}
			executeAddTask(temp);
		} else {
			Toast.makeText(this, "Empty Input", Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.novel_context_menu, menu);
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
		case R.id.add_to_watch:
			/*
			 * Implement code to toggle watch of this novel
			 */
			if (info.position > -1) {
				PageModel novel = listItems.get(info.position);
				if (novel.isWatched()) {
					novel.setWatched(false);
					Toast.makeText(this, getResources().getString(R.string.removed_from_watchlist) + ": " + novel.getTitle(), Toast.LENGTH_SHORT).show();
				} else {
					novel.setWatched(true);
					Toast.makeText(this, getResources().getString(R.string.added_to_watchlist) + ": " + novel.getTitle(), Toast.LENGTH_SHORT).show();
				}
				NovelsDao.getInstance(this).updatePageModel(novel);
				adapter.notifyDataSetChanged();
			}
			return true;
		case R.id.download_novel:
			/*
			 * Implement code to download novel synopsis
			 */
			if (info.position > -1) {
				PageModel novel = listItems.get(info.position);
				ArrayList<PageModel> novels = new ArrayList<PageModel>();
				novels.add(novel);
				touchedForDownload = novel.getTitle() + "'s information";
				executeDownloadTask(novels);
			}
			return true;
		case R.id.delete_novel:
			if (info.position > -1) {
				toggleProgressBar(true);
				PageModel novel = listItems.get(info.position);
				Log.d(TAG, "Deleting novel: " + novel.getTitle());
				int result = NovelsDao.getInstance(this).deleteNovel(novel);
				if (result > 0) {
					listItems.remove(novel);
					adapter.notifyDataSetChanged();
				}
				toggleProgressBar(false);
			}
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	private void updateContent(boolean isRefresh, boolean onlyWatched) {
		try {
			// Check size
			int resourceId = R.layout.novel_list_item;
			if (UIHelper.IsSmallScreen(this)) {
				resourceId = R.layout.novel_list_item_small;
			}
			if (adapter != null) {
				adapter.setResourceId(resourceId);
			} else {
				adapter = new PageModelAdapter(this, resourceId, listItems);
			}
			boolean alphOrder = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_ALPH_ORDER, false);
			executeTask(isRefresh, onlyWatched, alphOrder);
			setListAdapter(adapter);
		} catch (Exception e) {
			Log.e(TAG, e.getMessage(), e);
			Toast.makeText(this, getResources().getString(R.string.error_update) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	@SuppressLint("NewApi")
	private void executeTask(boolean isRefresh, boolean onlyWatched, boolean alphOrder) {
		String key = null;
		if (mode.equalsIgnoreCase(Constants.EXTRA_NOVEL_LIST_MODE_MAIN)) {
			key = TAG + ":" + Constants.ROOT_NOVEL;
		}
		else if (mode.equalsIgnoreCase(Constants.EXTRA_NOVEL_LIST_MODE_ORIGINAL)) {
			key = TAG + ":" + Constants.ROOT_ORIGINAL;
		}
		else if (mode.equalsIgnoreCase(Constants.EXTRA_NOVEL_LIST_MODE_TEASER)) {
			key = TAG + ":" + Constants.ROOT_TEASER;
		}

		task = new LoadNovelsTask(this, isRefresh, onlyWatched, alphOrder, mode);

		boolean isAdded = LNReaderApplication.getInstance().addTask(key, task);
		if (isAdded) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
				task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			else
				task.execute();
		} else {
			Log.i(TAG, "Continue execute task: " + key);
			LoadNovelsTask tempTask = (LoadNovelsTask) LNReaderApplication.getInstance().getTask(key);
			if (tempTask != null) {
				task = tempTask;
				task.owner = this;
			}
		}
		toggleProgressBar(true);
	}

	@Override
	public boolean downloadListSetup(String id, String toastText, int type, boolean hasError) {
		return UIHelper.downloadListSetup(this, touchedForDownload, id, toastText, type, hasError);
	}

	@Override
	public void updateProgress(String id, int current, int total, String messString) {
		double cur = current;
		double tot = total;
		double result = (cur / tot) * 100;
		LNReaderApplication.getInstance().updateDownload(id, (int) result, messString);
		if (loadingBar != null && loadingBar.getVisibility() == View.VISIBLE) {
			loadingBar.setIndeterminate(false);
			loadingBar.setMax(total);
			loadingBar.setProgress(current);
			loadingBar.setProgress(0);
			loadingBar.setProgress(current);
			loadingBar.setMax(total);
		}
	}

	@SuppressLint("NewApi")
	private void executeDownloadTask(ArrayList<PageModel> novels) {
		downloadTask = new DownloadNovelDetailsTask(this);
		if (novels == null || novels.size() == 0) {
			Log.w(TAG, getResources().getString(R.string.error_empty_download_array));
			return;
		}
		String key = DisplayLightNovelDetailsActivity.TAG + ":" + novels.get(0).getPage();
		if (novels.size() > 1) {
			if (mode.equalsIgnoreCase(Constants.EXTRA_NOVEL_LIST_MODE_MAIN)) {
				key = DisplayLightNovelDetailsActivity.TAG + ":All_Novels";
			}
			else if (mode.equalsIgnoreCase(Constants.EXTRA_NOVEL_LIST_MODE_ORIGINAL)) {
				key = DisplayLightNovelDetailsActivity.TAG + ":All_Original";
			}
			else if (mode.equalsIgnoreCase(Constants.EXTRA_NOVEL_LIST_MODE_TEASER)) {
				key = DisplayLightNovelDetailsActivity.TAG + ":All_Teasers";
			}
		}
		boolean isAdded = LNReaderApplication.getInstance().addTask(key, downloadTask);
		if (isAdded) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
				downloadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, novels.toArray(new PageModel[novels.size()]));
			else
				downloadTask.execute(novels.toArray(new PageModel[novels.size()]));
		} else {
			Log.i(TAG, "Continue download task: " + key);
			DownloadNovelDetailsTask tempTask = (DownloadNovelDetailsTask) LNReaderApplication.getInstance().getTask(key);
			if (tempTask != null) {
				downloadTask = tempTask;
				downloadTask.owner = this;
			}
			toggleProgressBar(true);
		}
	}

	@SuppressLint("NewApi")
	private void executeAddTask(PageModel novel) {
		addTask = new AddNovelTask(this);
		String key = DisplayLightNovelDetailsActivity.TAG + ":Add:" + novel.getPage();
		boolean isAdded = LNReaderApplication.getInstance().addTask(key, addTask);
		if (isAdded) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
				addTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new PageModel[] { novel });
			else
				addTask.execute(new PageModel[] { novel });
		} else {
			Log.i(TAG, "Continue Add task: " + key);
			AddNovelTask tempTask = (AddNovelTask) LNReaderApplication.getInstance().getTask(key);
			if (tempTask != null) {
				addTask = tempTask;
				addTask.owner = this;
			}
			toggleProgressBar(true);
		}
	}

	@Override
	public void toggleProgressBar(boolean show) {
		if (show) {
			loadingText.setText("Loading List, please wait...");
			loadingText.setVisibility(TextView.VISIBLE);
			loadingBar.setVisibility(ProgressBar.VISIBLE);
			loadingBar.setIndeterminate(true);
			getListView().setVisibility(ListView.GONE);
		} else {
			loadingText.setVisibility(TextView.GONE);
			loadingBar.setVisibility(ProgressBar.GONE);
			getListView().setVisibility(ListView.VISIBLE);
		}
	}

	@Override
	public void setMessageDialog(ICallbackEventData message) {
		if (loadingText.getVisibility() == TextView.VISIBLE)
			loadingText.setText(message.getMessage());
	}

	@Override
	public void onGetResult(AsyncTaskResult<?> result, Class<?> t) {
		Exception e = result.getError();
		if (e == null) {
			// from LoadNovelsTask
			if (t == PageModel[].class) {
				PageModel[] list = (PageModel[]) result.getResult();
				toggleProgressBar(false);
				if (list != null && list.length > 0) {
					adapter.clear();
					adapter.addAll(list);

					// Show message if watch list is empty
					if (onlyWatched) {
						if (list.length == 0) {
							Log.d(TAG, "WatchList result set message empty");
							TextView tv = (TextView) findViewById(R.id.emptyList);
							tv.setVisibility(TextView.VISIBLE);
							tv.setText("Watch List is empty.");
						}
					}
				}
				else {
					TextView tv = (TextView) findViewById(R.id.emptyList);
					tv.setVisibility(TextView.VISIBLE);
					tv.setText("List is empty.");
					Log.w(TAG, "Empty ArrayList!");
				}
			}
			// from DownloadNovelDetailsTask
			else if (t == NovelCollectionModel[].class) {
				setMessageDialog(new CallbackEventData("Download complete.", "DownloadNovelDetailsTask"));
				NovelCollectionModel[] list = (NovelCollectionModel[]) result.getResult();
				if (list.length > 0) {
					for (NovelCollectionModel novelCol : list) {
						try {
							PageModel page = novelCol.getPageModel();
							boolean found = false;
							for (PageModel temp : adapter.data) {
								if (temp.getPage().equalsIgnoreCase(page.getPage())) {
									found = true;
									break;
								}
							}
							if (!found) {
								adapter.data.add(page);
							}
						} catch (Exception e1) {
							Log.e(TAG, e1.getClass().toString() + ": " + e1.getMessage(), e1);
						}
					}
				}
				adapter.notifyDataSetChanged();
				toggleProgressBar(false);
			} else {
				Log.e(TAG, "Unknown ResultType: " + t.getName());
			}
		} else {
			Log.e(TAG, e.getClass().toString() + ": " + e.getMessage(), e);
			Toast.makeText(this, e.getClass().toString() + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public Context getContext() {
		return this;
	}
}
