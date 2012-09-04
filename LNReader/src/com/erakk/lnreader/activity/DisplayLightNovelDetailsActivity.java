package com.erakk.lnreader.activity;

import java.util.ArrayList;
import java.util.Iterator;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.R;
import com.erakk.lnreader.adapter.BookModelAdapter;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.AsyncTaskResult;
import com.erakk.lnreader.model.BookModel;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.NovelContentModel;
import com.erakk.lnreader.model.PageModel;

public class DisplayLightNovelDetailsActivity extends Activity {
	private static final String TAG = DisplayLightNovelDetailsActivity.class.toString();
	private PageModel page;
	private NovelCollectionModel novelCol;
	private NovelsDao dao = NovelsDao.getInstance(this);
	
    private BookModelAdapter bookModelAdapter;
    private ExpandableListView expandList;
    
    private DownloadNovelContentTask downloadTask = null;
    private LoadNovelDetailsTask task = null;
    
	private ProgressDialog dialog;
    
	@SuppressLint("NewApi")
	@Override
     public void onCreate(Bundle savedInstanceState) {
    	// set before create any view
    	if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("invert_colors", false)) {    		
    		setTheme(R.style.AppTheme2);
    	}
    	else {
    		setTheme(R.style.AppTheme);
    	}
    	
        super.onCreate(savedInstanceState);
        CheckScreenRotation();
        setContentView(R.layout.activity_display_light_novel_details);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB )
        	getActionBar().setDisplayHomeAsUpEnabled(true);
        
        //Get intent and message
        Intent intent = getIntent();
        page = new PageModel(); 
        page.setPage(intent.getStringExtra(Constants.EXTRA_PAGE));
        page.setTitle(intent.getStringExtra(Constants.EXTRA_TITLE));
                
        updateContent(false);
       
        // setup listener
        expandList = (ExpandableListView) findViewById(R.id.chapter_list);
        registerForContextMenu(expandList);
        expandList.setOnChildClickListener(new OnChildClickListener() {			
			@Override
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
				if(novelCol != null) {
					PageModel p = novelCol.getBookCollections().get(groupPosition).getChapterCollection().get(childPosition);
					
					Intent intent = new Intent(getApplicationContext(), DisplayLightNovelContentActivity.class);
			        intent.putExtra(Constants.EXTRA_PAGE, p.getPage());
//			        intent.putExtra(Constants.EXTRA_TITLE, p.getTitle());
//			        intent.putExtra(Constants.EXTRA_NOVEL, novelCol.getPage());
//			        intent.putExtra(Constants.EXTRA_VOLUME, novelCol.getBookCollections().get(groupPosition).getTitle());
			        startActivity(intent);
				}
				return false;
			}
		});
    	
        setTitle(page.getTitle());
    }
    
    @SuppressLint("NewApi")
	@Override
    protected void onRestart() {
        super.onRestart();
        recreate();
    }
    
    public void onStop(){
    	// check running task
    	if(task != null && !(task.getStatus() == Status.FINISHED)) {
    		task.cancel(true);
    	}
    	if(downloadTask != null && !(downloadTask.getStatus() == Status.FINISHED)) {
    		downloadTask.cancel(true);
    	}
    	super.onStop();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_display_light_novel_details, menu);
        return true;
    }
    
    @SuppressLint("NewApi")
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case R.id.menu_settings:
    		Intent launchNewIntent = new Intent(this, DisplaySettingsActivity.class);
    		startActivity(launchNewIntent);
    		return true;
    	case R.id.menu_refresh_chapter_list:			
			updateContent(true);
			Toast.makeText(getApplicationContext(), "Refreshing", Toast.LENGTH_SHORT).show();
			return true;
		case R.id.invert_colors:			
			toggleColorPref();
			recreate();			
			Toast.makeText(getApplicationContext(), "Colors inverted", Toast.LENGTH_SHORT).show();
			return true;
        case android.R.id.home:
        	super.onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, v, menuInfo);
    	ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;
	
    	MenuInflater inflater = getMenuInflater();
    	int type = ExpandableListView.getPackedPositionType(info.packedPosition);    	
    	if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
    		inflater.inflate(R.menu.novel_details_volume_context_menu, menu);
    	} else if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
    		inflater.inflate(R.menu.novel_details_chapter_context_menu, menu);
    	}
    }

	@Override
	public boolean onContextItemSelected(MenuItem item) {
    	ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo) item.getMenuInfo();
    	// unpacking
    	int groupPosition = ExpandableListView.getPackedPositionGroup(info.packedPosition);
    	int childPosition = ExpandableListView.getPackedPositionChild(info.packedPosition);
    	
    	PageModel chapter = null;
    	
		switch(item.getItemId()) {
		//Volume cases
		case R.id.download_volume:
			
			/*
			 * Implement code to download this volume
			 */
			BookModel book = novelCol.getBookCollections().get(groupPosition);
			// get the chapter which not downloaded yet
			ArrayList<PageModel> downloadingChapters = new ArrayList<PageModel>();
			for(Iterator<PageModel> i = book.getChapterCollection().iterator(); i.hasNext();) {
				PageModel temp = i.next();
				if(!temp.isDownloaded()) downloadingChapters.add(temp);
			}
			
			downloadTask = new DownloadNovelContentTask((PageModel[]) downloadingChapters.toArray(new PageModel[downloadingChapters.size()]));
			downloadTask.execute();
			
			Toast.makeText(this, "Download this Volume: " + book.getTitle(),
					Toast.LENGTH_SHORT).show();
			return true;
		case R.id.clear_volume:
			
			/*
			 * Implement code to clear this volume cache
			 */
			
			Toast.makeText(this, "Clear this Volume",
					Toast.LENGTH_SHORT).show();
			return true;
		case R.id.mark_volume:
			
			/*
			 * Implement code to mark entire volume as read
			 */
			
			Toast.makeText(this, "Mark Volume as Read",	Toast.LENGTH_SHORT).show();
			BookModel book2 = novelCol.getBookCollections().get(groupPosition);
			for(Iterator<PageModel> iPage = book2.getChapterCollection().iterator(); iPage.hasNext();) {
				PageModel page = iPage.next();
				page.setFinishedRead(true);
				dao.updatePageModel(page);
			}
			bookModelAdapter.notifyDataSetChanged();
			return true;
		//Chapter cases
		case R.id.download_chapter:
			
			/*
			 * Implement code to download this chapter
			 */
			chapter = novelCol.getBookCollections().get(groupPosition).getChapterCollection().get(childPosition);
			downloadTask = new DownloadNovelContentTask(new PageModel[] { chapter});
			downloadTask.execute();
			Toast.makeText(this, "Download this chapter: " + chapter.getTitle(), Toast.LENGTH_SHORT).show();
			return true;
		case R.id.clear_chapter:
			
			/*
			 * Implement code to clear this chapter cache
			 */
			
			Toast.makeText(this, "Clear this Chapter", Toast.LENGTH_SHORT).show();
			return true;
		case R.id.mark_read:
			
			/*
			 * Implement code to mark this chapter read
			 * >> change to toggle
			 */
			chapter = novelCol.getBookCollections().get(groupPosition).getChapterCollection().get(childPosition);
			chapter.setFinishedRead(!chapter.isFinishedRead());
			dao.updatePageModel(chapter);
			bookModelAdapter.notifyDataSetChanged();
			Toast.makeText(this, "Toggle Read", Toast.LENGTH_SHORT).show();
			return true;
		default:
			return super.onContextItemSelected(item);
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
	
	private void CheckScreenRotation()
	{
		if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("lock_horizontal", false)) {
			switch (this.getResources().getConfiguration().orientation)
		    {
		    	case Configuration.ORIENTATION_PORTRAIT:
		    		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		    		break;
		    	case Configuration.ORIENTATION_LANDSCAPE:
		    		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		    		break;
		    }
    	}
    	else {
    		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    	}
	}
    	
    private void updateContent ( boolean willRefresh) {
		task = new LoadNovelDetailsTask();
		task.refresh = willRefresh;
		task.execute(page);
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
    
	@SuppressLint("NewApi")
    public class LoadNovelDetailsTask extends AsyncTask<PageModel, String, AsyncTaskResult<NovelCollectionModel>> implements ICallbackNotifier {
		public boolean refresh = false;

    	public void onCallback(ICallbackEventData message) {
    		publishProgress(message.getMessage());
    	}
    	
		@Override
		protected void onPreExecute (){
			// executed on UI thread.
			ToggleProgressBar(true);
		}
		
		@Override
		protected AsyncTaskResult<NovelCollectionModel> doInBackground(PageModel... arg0) {
			PageModel page = arg0[0];
			try {
				if(refresh) {
					publishProgress("Refreshing chapter list...");
					NovelCollectionModel novelCol = dao.getNovelDetailsFromInternet(page, this);
					return new AsyncTaskResult<NovelCollectionModel>(novelCol);
				}
				else {
					publishProgress("Loading chapter list...");
					NovelCollectionModel novelCol = dao.getNovelDetails(page, this);
					Log.d(TAG, "Loaded: " + novelCol.getPage());				
					return new AsyncTaskResult<NovelCollectionModel>(novelCol);
				}
			} catch (Exception e) {
				e.printStackTrace();
				Log.e(TAG, e.getClass().toString() + ": " + e.getMessage());
				return new AsyncTaskResult<NovelCollectionModel>(e);
			}
		}
		
		@Override
		protected void onProgressUpdate (String... values){
			dialog.setMessage(values[0]);
		}
		
		@Override
		protected void onPostExecute(AsyncTaskResult<NovelCollectionModel> result) {
			Exception e = result.getError();
			if(e == null) {
				novelCol = result.getResult();
				expandList = (ExpandableListView) findViewById(R.id.chapter_list);
				// now add the volume and chapter list.
				try {
					// Prepare header
					if(expandList.getHeaderViewsCount() == 0) {  
						LayoutInflater layoutInflater = getLayoutInflater();
						View synopsis = layoutInflater.inflate(R.layout.activity_display_synopsis, null);
						TextView textViewTitle = (TextView) synopsis.findViewById(R.id.title);
						TextView textViewSynopsis = (TextView) synopsis.findViewById(R.id.synopsys);
						textViewTitle.setTextSize(20);
						textViewSynopsis.setTextSize(16); 
						textViewTitle.setText(novelCol.getPageModel().getTitle());
						textViewSynopsis.setText(novelCol.getSynopsis());

						ImageView ImageViewCover = (ImageView) synopsis.findViewById(R.id.cover);
						if (novelCol.getCoverBitmap() == null) {
							// IN app test, is returning empty bitmap
							Toast.makeText(getApplicationContext(), "Bitmap empty", Toast.LENGTH_LONG).show();
						}
						else {
							ImageViewCover.setImageBitmap(novelCol.getCoverBitmap());
						}

						expandList.addHeaderView(synopsis);
					}
		        	bookModelAdapter = new BookModelAdapter(DisplayLightNovelDetailsActivity.this, novelCol.getBookCollections());
		        	expandList.setAdapter(bookModelAdapter);
				} catch (Exception e2) {
					e2.getStackTrace();
					Toast.makeText(DisplayLightNovelDetailsActivity.this, e2.getClass().toString() +": " + e2.getMessage(), Toast.LENGTH_SHORT).show();
				}				
			}
			else {
				e.printStackTrace();
				Toast.makeText(getApplicationContext(), e.getClass().toString() + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
				Log.e(TAG, e.getClass().toString() + ": " + e.getMessage());
			}
			ToggleProgressBar(false);
	        //updateViewColor();
		}		
    }

	public class DownloadNovelContentTask extends AsyncTask<Void, String, AsyncTaskResult<NovelContentModel[]>> implements ICallbackNotifier{
		private PageModel[] chapters;
		
		public DownloadNovelContentTask(PageModel[] chapters) {
			super();
			this.chapters = chapters;
		}
		
		@Override
		protected void onPreExecute (){
			// executed on UI thread.
			ToggleProgressBar(true);
		}
		
		@Override
		public void onCallback(ICallbackEventData message) {
    		publishProgress(message.getMessage());
    	}

		@Override
		protected AsyncTaskResult<NovelContentModel[]> doInBackground(Void... params) {
			try{
				NovelContentModel[] contents = new NovelContentModel[chapters.length];
				for(int i = 0; i < chapters.length; ++i) {
					publishProgress("Downloading: " + chapters[i].getTitle());
					NovelContentModel temp = dao.getNovelContentFromInternet(chapters[i], this);
					contents[i] = temp;
				}
				return new AsyncTaskResult<NovelContentModel[]>(contents);
			}catch(Exception e) {
				return new AsyncTaskResult<NovelContentModel[]>(e);
			}
		}
		
		@Override
		protected void onProgressUpdate (String... values){
			//executed on UI thread.
			//TextView tv = (TextView) findViewById(R.id.loading);
			//tv.setText(values[0]);
			synchronized (dialog) {
				dialog.setMessage(values[0]);
			}			
		}
		
		@Override
		protected void onPostExecute(AsyncTaskResult<NovelContentModel[]> result) {
			Exception e = result.getError();
			if(e == null) {
				NovelContentModel[] content = result.getResult();
				if(content != null) {
					for(Iterator<BookModel> iBook = novelCol.getBookCollections().iterator(); iBook.hasNext();) {
						BookModel book = iBook.next();
						for(Iterator<PageModel> iPage = book.getChapterCollection().iterator(); iPage.hasNext();) {
							PageModel temp = iPage.next();
							for(int i = 0; i < chapters.length; ++i) {
								if(temp.getPage() == chapters[i].getPage()) {
									temp.setDownloaded(true);
									chapters[i].setDownloaded(true);
								}
							}
						}
					}
					bookModelAdapter.notifyDataSetChanged();
//					updateContent(false);
				}
			}
			else {
				e.printStackTrace();
				Toast.makeText(getApplicationContext(), e.getClass().toString() + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
			}
			ToggleProgressBar(false);
		}
	}
}
