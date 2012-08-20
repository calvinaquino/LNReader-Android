package com.erakk.lnreader;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.erakk.lnreader.adapter.PageModelAdapter;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.AsyncTaskResult;
import com.erakk.lnreader.model.PageModel;

/*
 * Author: Nandaka
 * Copy from: NovelsActivity.java
 */

public class DisplayLightNovelListActivity extends ListActivity{
	private static final String TAG = DisplayLightNovelListActivity.class.toString();
	private ArrayList<PageModel> listItems = new ArrayList<PageModel>();
	private PageModelAdapter adapter;
	private NovelsDao dao = new NovelsDao(this);
	private boolean refreshOnly = false;
	private boolean onlyWatched = false;
	private LoadNovelsTask task = null;

	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_display_light_novel_list);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		registerForContextMenu(getListView());

		Intent intent = getIntent();
		onlyWatched = intent.getExtras().getBoolean(Constants.EXTRA_ONLY_WATCHED);

		//Encapsulated in updateContent
		updateContent();
		
		if(onlyWatched){
			setTitle("Watched Light Novels");
		}
		else {
			setTitle("Light Novels");
		}
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
		startActivity(intent);
		Log.d("DisplayLightNovelsActivity", o.getPage() + " (" + o.getTitle() + ")");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_display_light_novels, menu);
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
		super.onStop();
	}
	
	@Override
    protected void onResume() {
        super.onResume();
        // The activity has become visible (it is now "resumed").
        updateViewColor();
    }

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
			refreshOnly = true;
			updateContent();
			
			Toast.makeText(getApplicationContext(), "Refreshing", Toast.LENGTH_SHORT).show();
			return true;
		case R.id.invert_colors:
			
			toggleColorPref();
    		updateViewColor();
			updateContent();
			
			Toast.makeText(getApplicationContext(), "Colors inverted", Toast.LENGTH_SHORT).show();
			return true;
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
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
		//adapter.AdapterContextMenuInfo info = (adapter.AdapterContextMenuInfo) item.getMenuInfo();
		//String[] names = getResources().getStringArray(R.array.novel_context_menu);
		switch(item.getItemId()) {
		case R.id.add_to_watch:
			
			/*
			 * Implement code to toggle watch of this novel
			 */
	        CheckBox checkBox = (CheckBox) findViewById(R.id.novel_is_watched);
	        if (checkBox.isChecked()) {
	        	checkBox.setChecked(false);Toast.makeText(this, "Removed from Watch List",
						Toast.LENGTH_SHORT).show();
	        }
	        else {
	        	checkBox.setChecked(true);Toast.makeText(this, "Added to Watch List",
						Toast.LENGTH_SHORT).show();
	        }
			return true;
		case R.id.download_novel:
			
			/*
			 * Implement code to download entire novel
			 */
			
			Toast.makeText(this, "Downloading Novel",
					Toast.LENGTH_SHORT).show();
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}
	
	private void updateContent () {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    	boolean invertColors = sharedPrefs.getBoolean("invert_colors", false);
		try {
			if (adapter != null) { 
				if (invertColors)adapter.setResourceId(R.layout.novel_list_item_black);
				else adapter.setResourceId(R.layout.novel_list_item);
			} else {
				if (invertColors)adapter = new PageModelAdapter(this, R.layout.novel_list_item_black, listItems);
				else adapter = new PageModelAdapter(this, R.layout.novel_list_item, listItems);
			}
			new LoadNovelsTask().execute();
			setListAdapter(adapter);
		} catch (Exception e) {
			e.printStackTrace();
			Toast t = Toast.makeText(this, e.getClass().toString() +": " + e.getMessage(), Toast.LENGTH_SHORT);
			t.show();					
		}
	}
	
	@SuppressLint("NewApi")
	private void ToggleProgressBar(boolean show) {
		ProgressBar pb = (ProgressBar) findViewById(R.id.progressBar2);
		TextView tv = (TextView) findViewById(R.id.loading);
		
		if(show) {
			pb.setIndeterminate(true);
			pb.setActivated(true);
			pb.animate();
			pb.setVisibility(ProgressBar.VISIBLE);
		
			tv.setText("Loading...");
			tv.setVisibility(TextView.VISIBLE);
		}
		else {
			pb.setVisibility(ProgressBar.GONE);			
			tv.setVisibility(TextView.INVISIBLE);
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
    	//Toast.makeText(this, "", Toast.LENGTH_SHORT).show();
    }
    
    private void updateViewColor() {
    	SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    	boolean invertColors = sharedPrefs.getBoolean("invert_colors", false);
    	
    	// Views to be changed
        View MainView = findViewById(R.id.light_novel_list_screen);
        
        // it is considered white background and black text to be the standard
        // so we change to black background and white text if true
        if (invertColors == true) {
        	MainView.setBackgroundColor(Color.BLACK);
        	adapter = new PageModelAdapter(this, R.layout.novel_list_item_black, listItems);
//        	NovelNames.setTextColor(Color.WHITE);
        }
        else {
        	MainView.setBackgroundColor(Color.WHITE);
        	adapter = new PageModelAdapter(this, R.layout.novel_list_item, listItems);
//        	NovelNames.setTextColor(Color.BLACK);
        }
    }
	
	@SuppressLint("NewApi")
	public class LoadNovelsTask extends AsyncTask<Void, String, AsyncTaskResult<ArrayList<PageModel>>> {

		@Override
		protected void onPreExecute (){
			// executed on UI thread.
			ToggleProgressBar(true);
		}
		
		@Override
		protected AsyncTaskResult<ArrayList<PageModel>> doInBackground(Void... arg0) {
			// different thread from UI
			try {
				if (onlyWatched) {
					publishProgress("Loading Watched List");
					return new AsyncTaskResult<ArrayList<PageModel>>(dao.getWatchedNovel());
				}
				else {
					if(refreshOnly) {
						publishProgress("Refreshing Novel List");
						return new AsyncTaskResult<ArrayList<PageModel>>(dao.getNovelsFromInternet());
					}
					else {
						publishProgress("Loading Novel List");
						return new AsyncTaskResult<ArrayList<PageModel>>(dao.getNovels());
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
			//executed on UI thread.
			TextView tv = (TextView) findViewById(R.id.loading);
			tv.setText(values[0]);
		}
		
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
				
				if (list.size() == 0 && onlyWatched) {
					// Show message if watch list is empty
					TextView tv = (TextView) findViewById(R.id.loading);
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
}

