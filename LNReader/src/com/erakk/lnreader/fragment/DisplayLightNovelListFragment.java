package com.erakk.lnreader.fragment;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;
import com.erakk.lnreader.Constants;
import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.activity.DisplayLightNovelDetailsActivity;
import com.erakk.lnreader.activity.INovelListHelper;
import com.erakk.lnreader.adapter.PageModelAdapter;
import com.erakk.lnreader.callback.CallbackEventData;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.AsyncTaskResult;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.PageModel;
import com.erakk.lnreader.task.AddNovelTask;
import com.erakk.lnreader.task.DownloadNovelDetailsTask;
import com.erakk.lnreader.task.IAsyncTaskOwner;
import com.erakk.lnreader.task.LoadNovelsTask;

/*
 * Author: Nandaka
 * Copy from: NovelsActivity.java
 */

public class DisplayLightNovelListFragment extends SherlockListFragment implements IAsyncTaskOwner, INovelListHelper {
	private static final String TAG = DisplayLightNovelListFragment.class.toString();
	private final ArrayList<PageModel> listItems = new ArrayList<PageModel>();
	private PageModelAdapter adapter;
	private LoadNovelsTask task = null;
	private DownloadNovelDetailsTask downloadTask = null;
	private AddNovelTask addTask = null;
	private boolean onlyWatched = false;
	String touchedForDownload;
	ListView listView = null;

	private TextView loadingText;
	private ProgressBar loadingBar;
	FragmentListener mFragListener;

	public interface FragmentListener {
		public void changeNextFragment(Bundle bundle);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mFragListener = (FragmentListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement FragListener");
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.activity_display_light_novel_list, container, false);

		loadingText = (TextView) view.findViewById(R.id.emptyList);
		loadingBar = (ProgressBar) view.findViewById(R.id.empttListProgress);
		getSherlockActivity().setTitle("Light Novels");

		return view;
	}

	@Override
	public void onStart() {
		super.onStart();

		/************************************************
		 * These lines of code require the ListView to already be created before
		 * they are used, hence, put in the onStart() method
		 ****************************************************/

		onlyWatched = getSherlockActivity().getIntent().getBooleanExtra(Constants.EXTRA_ONLY_WATCHED, false);

		registerForContextMenu(getListView());
		listView = getListView();

		// Encapsulated in updateContent
		updateContent(false, onlyWatched);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		// Get the item that was clicked
		PageModel o = adapter.getItem(position);
		String novel = o.toString();

		// Create a bundle containing information about the novel that is clicked
		Bundle bundle = new Bundle();
		bundle.putString(Constants.EXTRA_NOVEL, novel);
		bundle.putString(Constants.EXTRA_PAGE, o.getPage());
		bundle.putString(Constants.EXTRA_TITLE, o.getTitle());
		bundle.putBoolean(Constants.EXTRA_ONLY_WATCHED, getSherlockActivity().getIntent().getBooleanExtra(Constants.EXTRA_ONLY_WATCHED, false));

		mFragListener.changeNextFragment(bundle);

		Log.d("DisplayLightNovelsActivity", o.getPage() + " (" + o.getTitle() + ")");

		// Need to send it through
	}

	@Override
	public void refreshList() {
		boolean onlyWatched = getSherlockActivity().getIntent().getBooleanExtra(Constants.EXTRA_ONLY_WATCHED, false);
		updateContent(true, onlyWatched);
		Toast.makeText(getSherlockActivity(), "Refreshing Main Novel...", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void downloadAllNovelInfo() {
		if (onlyWatched)
			touchedForDownload = "Watched Light Novels information";
		else
			touchedForDownload = "All Main Light Novels information";
		executeDownloadTask(listItems);
	}

	@Override
	public void manualAdd() {
		AlertDialog.Builder alert = new AlertDialog.Builder(getSherlockActivity());
		alert.setTitle("Add Novel (Main)");

		LayoutInflater factory = LayoutInflater.from(getSherlockActivity());
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
		alert.setNegativeButton("Cancel", null);
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
			temp.setParent("Main_Page");
			executeAddTask(temp);
		} else {
			Toast.makeText(getSherlockActivity(), "Empty Input", Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getSherlockActivity().getMenuInflater();
		inflater.inflate(R.menu.novel_context_menu, menu);
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		if (!(item.getMenuInfo() instanceof AdapterContextMenuInfo))
			return super.onContextItemSelected(item);
		Log.d(TAG, "Context menu called");
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
					Toast.makeText(getSherlockActivity(), "Removed from watch list: " + novel.getTitle(), Toast.LENGTH_SHORT).show();
				} else {
					novel.setWatched(true);
					Toast.makeText(getSherlockActivity(), "Added to watch list: " + novel.getTitle(), Toast.LENGTH_SHORT).show();
				}
				NovelsDao.getInstance(getSherlockActivity()).updatePageModel(novel);
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
				boolean result = NovelsDao.getInstance(getSherlockActivity()).deleteNovel(novel);
				if (result) {
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
			if (UIHelper.IsSmallScreen(getSherlockActivity())) {
				resourceId = R.layout.novel_list_item_small;
			}
			if (adapter != null) {
				adapter.setResourceId(resourceId);
			} else {
				adapter = new PageModelAdapter(getSherlockActivity(), resourceId, listItems);
			}
			boolean alphOrder = PreferenceManager.getDefaultSharedPreferences(getSherlockActivity()).getBoolean(Constants.PREF_ALPH_ORDER, false);
			executeTask(isRefresh, onlyWatched, alphOrder);
			setListAdapter(adapter);
		} catch (Exception e) {
			Log.e(TAG, e.getMessage(), e);
			Toast.makeText(getSherlockActivity(), "Error when updating: " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	@SuppressLint("NewApi")
	private void executeTask(boolean isRefresh, boolean onlyWatched, boolean alphOrder) {
		task = new LoadNovelsTask(this, isRefresh, onlyWatched, alphOrder);
		String key = TAG + ":Main+Page";
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
			// This
			toggleProgressBar(true);
		}
	}

	@Override
	public boolean downloadListSetup(String id, String toastText, int type, boolean hasError) {
		boolean exists = false;
		if (!this.isAdded() || this.isDetached())
			return exists;

		String name = touchedForDownload;
		if (type == 0) {
			if (LNReaderApplication.getInstance().checkIfDownloadExists(name)) {
				exists = true;
				Toast.makeText(getSherlockActivity(), "Download already on queue.", Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(getSherlockActivity(), "Downloading " + name + ".", Toast.LENGTH_SHORT).show();
				LNReaderApplication.getInstance().addDownload(id, name);
			}
		} else if (type == 1) {
			Toast.makeText(getSherlockActivity(), toastText, Toast.LENGTH_SHORT).show();
		} else if (type == 2) {
			String message = String.format("%s's download finished!", LNReaderApplication.getInstance().getDownloadDescription(id));
			if (hasError)
				message = String.format("%s's download finished with error(s)!", LNReaderApplication.getInstance().getDownloadDescription(id));
			Toast.makeText(getSherlockActivity(), message, Toast.LENGTH_SHORT).show();
			LNReaderApplication.getInstance().removeDownload(id);
		}
		return exists;
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
		if (novels == null || novels.size() == 0)
			return;
		String key = DisplayLightNovelDetailsActivity.TAG + ":" + novels.get(0).getPage();
		if (novels.size() > 1) {
			key = DisplayLightNovelDetailsActivity.TAG + ":All_Novels";
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
			if (listView != null)
				listView.setVisibility(ListView.GONE);
		} else {
			loadingText.setVisibility(TextView.GONE);
			loadingBar.setVisibility(ProgressBar.GONE);
			if (listView != null)
				listView.setVisibility(ListView.VISIBLE);
		}
	}

	@Override
	public void setMessageDialog(ICallbackEventData message) {
		// if(dialog.isShowing())
		// dialog.setMessage(message.getMessage());
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
				Log.d(TAG, "LoadNovelsTask result ok");
				if (list != null && list.length > 0) {
					adapter.clear();
					adapter.addAll(list);
					toggleProgressBar(false);

					// Show message if watch list is empty
					if (list.length == 0 && onlyWatched) {
						Log.d(TAG, "WatchList result set message empty");
						loadingText.setVisibility(TextView.VISIBLE);
						loadingText.setText("Watch List is empty.");
					}
				} else {
					toggleProgressBar(false);
					loadingText.setVisibility(TextView.VISIBLE);
					loadingText.setText("List is empty.");
					Log.w(TAG, "Empty ArrayList!");
				}
			}
			// from DownloadNovelDetailsTask
			else if (t == NovelCollectionModel[].class) {
				setMessageDialog(new CallbackEventData("Download complete."));
				NovelCollectionModel[] list = (NovelCollectionModel[]) result.getResult();
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
				adapter.notifyDataSetChanged();
				toggleProgressBar(false);
			} else {
				Log.e(TAG, "Unknown ResultType: " + t.getName());
			}
		} else {
			Log.e(TAG, e.getClass().toString() + ": " + e.getMessage(), e);
			Toast.makeText(getSherlockActivity(), e.getClass().toString() + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public Context getContext() {
		Context ctx = this.getSherlockActivity();
		if (ctx == null)
			return LNReaderApplication.getInstance().getApplicationContext();
		return ctx;
	}
}
