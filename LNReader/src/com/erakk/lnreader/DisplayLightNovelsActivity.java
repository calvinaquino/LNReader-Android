package com.erakk.lnreader;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
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

public class DisplayLightNovelsActivity extends ListActivity{
	ArrayList<PageModel> listItems = new ArrayList<PageModel>();
	PageModelAdapter adapter;
	NovelsDao dao = new NovelsDao(this);

	boolean onlyWatched = false;

	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_display_light_novels);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		registerForContextMenu(getListView());

		Intent intent = getIntent();
		onlyWatched = intent.getExtras().getBoolean(Constants.EXTRA_ONLY_WATCHED);

		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		boolean invertColors = sharedPrefs.getBoolean("invert_colors", false);

		//View NovelView = findViewById(R.id.light_novel_list_screen);

		if (invertColors == true) {
			//NovelList.setBackgroundColor(Color.TRANSPARENT);
			//ListText.setTextColor(Color.WHITE);
			//isWatched
			//NovelView.setBackgroundColor(Color.BLACK);
		}
		try {
			adapter = new PageModelAdapter(this, R.layout.novel_list_item, listItems);
			new LoadNovelsTask().execute(new boolean[] {false});
			setListAdapter(adapter);
		} catch (Exception e) {
			e.printStackTrace();
			Toast t = Toast.makeText(this, e.getClass().toString() +": " + e.getMessage(), Toast.LENGTH_SHORT);
			t.show();					
		}
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
		Intent intent = new Intent(this, LightNovelChaptersActivity.class);
		intent.putExtra(Constants.EXTRA_MESSAGE, novel);
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
			
			Toast.makeText(getApplicationContext(), "Refreshing", Toast.LENGTH_SHORT).show();
			return true;
		case R.id.invert_colors:
			
			/*
			 * Implement code to invert colors
			 */
			
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
		//AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		//String[] names = getResources().getStringArray(R.array.novel_context_menu);
		switch(item.getItemId()) {
		case R.id.add_to_watch:
			
			/*
			 * Implement code to toggle watch of this novel
			 */
			
			Toast.makeText(this, "Added to Watch List",
					Toast.LENGTH_SHORT).show();
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
			tv.setVisibility(TextView.GONE);
		}
	}

	@SuppressLint("NewApi")
	public class LoadNovelsTask extends AsyncTask<boolean[], String, AsyncTaskResult<ArrayList<PageModel>>> {

		@Override
		protected void onPreExecute (){
			// executed on UI thread.
			ToggleProgressBar(true);
		}
		
		@Override
		protected AsyncTaskResult<ArrayList<PageModel>> doInBackground(boolean[]... arg0) {
			// different thread from UI
			boolean refresh = arg0[0][0];
			try {

				if (onlyWatched) {
					publishProgress("Loading Watched List");
					return new AsyncTaskResult<ArrayList<PageModel>>(dao.getWatchedNovel());
				}
				else {
					publishProgress("Loading Novel List");
					return new AsyncTaskResult<ArrayList<PageModel>>(dao.getNovels(refresh));
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
			if(list != null) adapter.addAll(list);
			if(result.getError() != null) {
				Exception e = result.getError();
				Toast.makeText(getApplicationContext(), e.getClass().toString() + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
				
				Log.e(this.getClass().toString(), e.getClass().toString() + ": " + e.getMessage());
			}
			
			ToggleProgressBar(false);
		}    	 
	}
}

