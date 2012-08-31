package com.erakk.lnreader;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.erakk.lnreader.adapter.PageModelAdapter;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.AsyncTaskResult;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.PageModel;

/*
 * Author: Nandaka
 * Copy from: NovelsActivity.java
 */

public class DisplayLightNovelListActivity extends ListActivity{
	private static final String TAG = DisplayLightNovelListActivity.class.toString();
	private ArrayList<PageModel> listItems = new ArrayList<PageModel>();
	private PageModelAdapter adapter;
	private NovelsDao dao = NovelsDao.getInstance(this);
	private LoadNovelsTask task = null;
	private DownloadNovelDetailsTask downloadTask = null;
	private ProgressDialog dialog;

	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
    	Log.d("MainActivity", "onCreate");
    	// set before create any view
    	if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("invert_colors", false)) {    		
    		setTheme(R.style.AppTheme2);
    	}
    	else {
    		setTheme(R.style.AppTheme);
    	}
    	
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_display_light_novel_list);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		registerForContextMenu(getListView());
		boolean onlyWatched = getIntent().getBooleanExtra(Constants.EXTRA_ONLY_WATCHED, false);//intent.getExtras().getBoolean(Constants.EXTRA_ONLY_WATCHED);

		//Encapsulated in updateContent
		updateContent(false, onlyWatched);
		
		if(onlyWatched){
			setTitle("Watched Light Novels");
		}
		else {
			setTitle("Light Novels");
		}
		registerForContextMenu(getListView());
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		// Get the item that was clicked
		PageModel o = adapter.getItem(position);
		String novel = o.toString();
		//Create new intent
		Intent intent = new Intent(this, DisplayLightNovelDetailsActivity.class);
		intent.putExtra(Constants.EXTRA_NOVEL, novel);
		intent.putExtra(Constants.EXTRA_PAGE, o.getPage());
		intent.putExtra(Constants.EXTRA_TITLE, o.getTitle());
		intent.putExtra(Constants.EXTRA_ONLY_WATCHED, getIntent().getStringExtra(Constants.EXTRA_ONLY_WATCHED));
		startActivity(intent);
		Log.d("DisplayLightNovelsActivity", o.getPage() + " (" + o.getTitle() + ")");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_display_light_novel_list, menu);
		return true;
	}
	
	@Override
	protected void onStop() {
		// cancel running task
		if(task != null) {
			if(!(task.getStatus() == Status.FINISHED)) {
				task.cancel(true);
				Log.d(TAG, "Stopping running task.");
			}
		}
		if(downloadTask != null) {
			if(!(downloadTask.getStatus() == Status.FINISHED)) {
				downloadTask.cancel(true);
				Log.d(TAG, "Stopping running download task.");
			}
		}
		super.onStop();
	}
	
    @SuppressLint("NewApi")
	@Override
    protected void onRestart() {
        super.onRestart();
        recreate();
    }

	@SuppressLint("NewApi")
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			Intent launchNewIntent = new Intent(this, DisplaySettingsActivity.class);
			startActivity(launchNewIntent);
			return true;
		case R.id.menu_refresh_novel_list:			
			/*
			 * Implement code to refresh novel list
			 */
			boolean onlyWatched = getIntent().getBooleanExtra(Constants.EXTRA_ONLY_WATCHED, false);
			updateContent(true, onlyWatched);			
			Toast.makeText(getApplicationContext(), "Refreshing", Toast.LENGTH_SHORT).show();
			return true;
		case R.id.invert_colors:			
			toggleColorPref();
			recreate();			
			Toast.makeText(getApplicationContext(), "Colors inverted", Toast.LENGTH_SHORT).show();
			return true;
		case android.R.id.home:
			//NavUtils.navigateUpFromSameTask(this);
			super.onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.novel_context_menu, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.add_to_watch:			
			/*
			 * Implement code to toggle watch of this novel
			 */
	        CheckBox checkBox = (CheckBox) findViewById(R.id.novel_is_watched);
	        if (checkBox.isChecked()) {
	        	checkBox.setChecked(false);
	        	Toast.makeText(this, "Removed from Watch List", Toast.LENGTH_SHORT).show();
	        }
	        else {
	        	checkBox.setChecked(true);
	        	Toast.makeText(this, "Added to Watch List", Toast.LENGTH_SHORT).show();
	        }
			return true;
		case R.id.download_novel:			
			/*
			 * Implement code to download entire novel
			 */
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
			if(info.position > -1) {
				ToggleProgressBar(true);
				PageModel novel = listItems.get(info.position);
				downloadTask = new DownloadNovelDetailsTask();
				downloadTask.execute(new PageModel[] {novel});
				Toast.makeText(this, "Downloading Novel: " + novel.getTitle(), Toast.LENGTH_SHORT).show();
			}
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}
	
	private void updateContent (boolean isRefresh, boolean onlyWatched) {
		try {
			if (adapter != null) {
				adapter.setResourceId(R.layout.novel_list_item);
			} else {
				adapter = new PageModelAdapter(this, R.layout.novel_list_item, listItems);
			}
			new LoadNovelsTask().execute(new Boolean[] {isRefresh, onlyWatched});
			setListAdapter(adapter);
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this, e.getClass().toString() +": " + e.getMessage(), Toast.LENGTH_SHORT).show();					
		}
	}
	
	@SuppressLint("NewApi")
	private void ToggleProgressBar(boolean show) {
		if(show) {
			dialog = ProgressDialog.show(this, "", "Loading. Please wait...", true);
		}
		else {
			dialog.dismiss();
		}
	}
	
	private void toggleColorPref () { 
    	SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    	SharedPreferences.Editor editor = sharedPrefs.edit();
    	if (sharedPrefs.getBoolean("invert_colors", false)) {
    		editor.putBoolean("invert_colors", false);
    	}
    	else {
    		editor.putBoolean("invert_colors", true);
    	}
    	editor.commit();
    }
    	
	@SuppressLint("NewApi")
	public class LoadNovelsTask extends AsyncTask<Boolean, String, AsyncTaskResult<ArrayList<PageModel>>>  implements ICallbackNotifier {
    	private boolean refreshOnly = false;
    	private boolean onlyWatched = false;
		
		public void onCallback(ICallbackEventData message) {
    		publishProgress(message.getMessage());
    	}

		@Override
		protected void onPreExecute (){
			// executed on UI thread.
			ToggleProgressBar(true);
		}
		
		@Override
		protected AsyncTaskResult<ArrayList<PageModel>> doInBackground(Boolean... arg0) {
			// different thread from UI
			this.refreshOnly = arg0[0];
			this.onlyWatched = arg0[1];
			try {
				if (onlyWatched) {
					publishProgress("Loading Watched List");
					return new AsyncTaskResult<ArrayList<PageModel>>(dao.getWatchedNovel());
				}
				else {
					if(refreshOnly) {
						publishProgress("Refreshing Novel List");
						return new AsyncTaskResult<ArrayList<PageModel>>(dao.getNovelsFromInternet(this));
					}
					else {
						publishProgress("Loading Novel List");
						return new AsyncTaskResult<ArrayList<PageModel>>(dao.getNovels(this));
					}
				}
				//return new AsyncTaskResult<ArrayList<PageModel>>(listItems);
			} catch (Exception e) {
				e.printStackTrace();
				return new AsyncTaskResult<ArrayList<PageModel>>(e);
			}
		}
		
		@Override
		protected void onProgressUpdate (String... values){
			dialog.setTitle(values[0]);
		}
		
		@Override
		protected void onPostExecute(AsyncTaskResult<ArrayList<PageModel>> result) {
			//executed on UI thread.
			ArrayList<PageModel> list = result.getResult();
			if(list != null) {
				if (refreshOnly) {
					adapter.clear();
					refreshOnly = false;
				}
				
				adapter.addAll(list);
				ToggleProgressBar(false);

				// Show message if watch list is empty
				if (list.size() == 0 && onlyWatched) {
					TextView tv = (TextView) findViewById(R.id.emptyList);
					tv.setVisibility(TextView.VISIBLE);
					tv.setText("Watch List is empty.");
				}
			}
			if(result.getError() != null) {
				Exception e = result.getError();
				Toast.makeText(getApplicationContext(), e.getClass().toString() + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
				Log.e(this.getClass().toString(), e.getClass().toString() + ": " + e.getMessage());
			}
		}    	 
	}
	
	public class DownloadNovelDetailsTask extends AsyncTask<PageModel, String, AsyncTaskResult<NovelCollectionModel>> implements ICallbackNotifier {

		@Override
		public void onCallback(ICallbackEventData message) {
			publishProgress(message.getMessage());
		}

		@Override
		protected AsyncTaskResult<NovelCollectionModel> doInBackground(PageModel... params) {
			PageModel page = params[0];
			try {
				publishProgress("Downloading chapter list...");
				NovelCollectionModel novelCol = dao.getNovelDetails(page, this);
				Log.d("DownloadNovelDetailsTask", "Downloaded: " + novelCol.getPage());				
				return new AsyncTaskResult<NovelCollectionModel>(novelCol);
			} catch (Exception e) {
				e.printStackTrace();
				Log.e("DownloadNovelDetailsTask", e.getClass().toString() + ": " + e.getMessage());
				return new AsyncTaskResult<NovelCollectionModel>(e);
			}
		}
		
		@Override
		protected void onProgressUpdate (String... values){
			//executed on UI thread.
			dialog.setMessage(values[0]);
		}
		
		@Override
		protected void onPostExecute(AsyncTaskResult<NovelCollectionModel> result) {
			Exception e = result.getError();
			if(e == null) {
				dialog.setMessage("Download complete.");
			}
			else {
				e.printStackTrace();
				Toast.makeText(getApplicationContext(), e.getClass().toString() + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
				Log.e(this.getClass().toString(), e.getClass().toString() + ": " + e.getMessage());
			}
			ToggleProgressBar(false);
		}
	}
}

