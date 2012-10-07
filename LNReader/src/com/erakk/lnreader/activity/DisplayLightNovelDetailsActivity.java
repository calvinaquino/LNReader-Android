package com.erakk.lnreader.activity;

import java.util.ArrayList;
import java.util.Iterator;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
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
	private boolean isInverted;
    
	@Override
     public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UIHelper.SetTheme(this, R.layout.activity_display_light_novel_details);
        UIHelper.SetActionBarDisplayHomeAsUp(this, true);
        
        //Get intent and message
        Intent intent = getIntent();
        page = new PageModel();
        page.setPage(intent.getStringExtra(Constants.EXTRA_PAGE));
        //page.setTitle(intent.getStringExtra(Constants.EXTRA_TITLE));
        try {
			page = NovelsDao.getInstance(this).getPageModel(page, null);
		} catch (Exception e) {
			Log.e(TAG, "Error when getting Page Model for " + page.getPage(), e);
		}                
        executeTask(page, false);
       
        // setup listener
        expandList = (ExpandableListView) findViewById(R.id.chapter_list);
        registerForContextMenu(expandList);
        expandList.setOnChildClickListener(new OnChildClickListener() {			
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
				if(novelCol != null) {
					PageModel p = novelCol.getBookCollections().get(groupPosition).getChapterCollection().get(childPosition);
					
					Intent intent = new Intent(getApplicationContext(), DisplayLightNovelContentActivity.class);
			        intent.putExtra(Constants.EXTRA_PAGE, p.getPage());
			        startActivity(intent);
				}
				return false;
			}
		});
    	
        setTitle(page.getTitle());
        isInverted = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_INVERT_COLOR, false);
    }
    
	@Override
    protected void onRestart() {
        super.onRestart();
        if(isInverted != PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_INVERT_COLOR, false)) {
        	UIHelper.Recreate(this);
        }
        bookModelAdapter.notifyDataSetChanged();
    }
    
	protected void onResume(){
		super.onResume();
		Log.d(TAG, "OnResume: " + task.getStatus().toString());
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
    
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case R.id.menu_settings:
    		Intent launchNewIntent = new Intent(this, DisplaySettingsActivity.class);
    		startActivity(launchNewIntent);
    		return true;
    	case R.id.menu_refresh_chapter_list:			
    		executeTask(page, true);
			Toast.makeText(getApplicationContext(), "Refreshing", Toast.LENGTH_SHORT).show();
			return true;
		case R.id.invert_colors:			
			UIHelper.ToggleColorPref(this);
			UIHelper.Recreate(this);
			return true;
		case R.id.menu_download_all:
			/*
			 * Download all chapters
			 */
			ArrayList<PageModel> availableChapters = novelCol.getFlattedChapterList();
			ArrayList<PageModel> notDownloadedChapters = new ArrayList<PageModel>(); 
			for(Iterator<PageModel> i = availableChapters.iterator(); i.hasNext();) {
				PageModel temp = i.next();
				if(!temp.isDownloaded()) notDownloadedChapters.add(temp);
			}
			executeDownloadTask(notDownloadedChapters);
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
				if(temp.isDownloaded()) {
					try {
						NovelContentModel content = dao.getNovelContent(temp, false, null);
						if(content != null) {
							// check if content is updated
							if(content.getLastUpdate().getTime() != temp.getLastUpdate().getTime()) {
								downloadingChapters.add(temp);
							}
						}
					} catch (Exception e) {
						Log.e(TAG, "Failed to get novel content", e);
					}
				}
				else {
					downloadingChapters.add(temp);
				}
			}
			executeDownloadTask(downloadingChapters);
			return true;
		case R.id.clear_volume:
			
			/*
			 * Implement code to clear this volume cache
			 */
			BookModel bookDel = novelCol.getBookCollections().get(groupPosition);
			Toast.makeText(this, "Clear this Volume: " + bookDel.getTitle(), Toast.LENGTH_SHORT).show();
			dao.deleteBooks(bookDel);
			novelCol.getBookCollections().remove(groupPosition);
			bookModelAdapter.notifyDataSetChanged();
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
			return true;
		case R.id.clear_chapter:
			
			/*
			 * Implement code to clear this chapter cache
			 */
			chapter = novelCol.getBookCollections().get(groupPosition).getChapterCollection().get(childPosition);
			Toast.makeText(this, "Clear this Chapter: " + chapter.getTitle(), Toast.LENGTH_SHORT).show();
			dao.deletePage(chapter);
			novelCol.getBookCollections().get(groupPosition).getChapterCollection().remove(childPosition);
			bookModelAdapter.notifyDataSetChanged();
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
    
	@SuppressLint("NewApi")
	private void executeTask(PageModel pageModel, boolean willRefresh) {
		task = new LoadNovelDetailsTask();
		task.refresh = willRefresh;
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new PageModel[] {pageModel});
		else
			task.execute(new PageModel[] {pageModel});
	}
	
	@SuppressLint("NewApi")
	private void executeDownloadTask(ArrayList<PageModel> chapters) {
		downloadTask = new DownloadNovelContentTask((PageModel[]) chapters.toArray(new PageModel[chapters.size()]));
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			downloadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		else
			downloadTask.execute();
	}
	
	private void ToggleProgressBar(boolean show) {
		if(show) {
			dialog = ProgressDialog.show(this, "Novel Details", "Loading. Please wait...", true);
			dialog.getWindow().setGravity(Gravity.CENTER);
			dialog.setCanceledOnTouchOutside(true);
		}
		else {
			dialog.dismiss();
		}
	}
    
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
					return new AsyncTaskResult<NovelCollectionModel>(novelCol);
				}
			} catch (Exception e) {
				Log.e(TAG, e.getClass().toString() + ": " + e.getMessage(), e);
				return new AsyncTaskResult<NovelCollectionModel>(e);
			}
		}
		
		@Override
		protected void onProgressUpdate (String... values){
			if(dialog.isShowing())
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
						page = novelCol.getPageModel();
						LayoutInflater layoutInflater = getLayoutInflater();
						View synopsis = layoutInflater.inflate(R.layout.activity_display_synopsis, null);
						TextView textViewTitle = (TextView) synopsis.findViewById(R.id.title);
						TextView textViewSynopsis = (TextView) synopsis.findViewById(R.id.synopsys);
						textViewTitle.setTextSize(20);
						textViewSynopsis.setTextSize(16); 
						textViewTitle.setText(page.getTitle());
						textViewSynopsis.setText(novelCol.getSynopsis());
						
						CheckBox isWatched = (CheckBox) synopsis.findViewById(R.id.isWatched);
						isWatched.setChecked(page.isWatched());
						isWatched.setOnCheckedChangeListener(new OnCheckedChangeListener() {

							public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
								if(isChecked){
									Toast.makeText(getApplicationContext(), "Added to watch list: " + page.getTitle(),	Toast.LENGTH_SHORT).show();
								}
								else {
									Toast.makeText(getApplicationContext(), "Removed from watch list: " + page.getTitle(),	Toast.LENGTH_SHORT).show();
								}
								// update the db!
								page.setWatched(isChecked);
								NovelsDao dao = NovelsDao.getInstance(getApplicationContext());
								dao.updatePageModel(page);
							}
						});

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
					Log.e(TAG, "Error when setting up chapter list: " + e2.getMessage(), e2);
					Toast.makeText(DisplayLightNovelDetailsActivity.this, e2.getClass().toString() +": " + e2.getMessage(), Toast.LENGTH_SHORT).show();
				}				
			}
			else {
				Log.e(TAG, e.getClass().toString() + ": " + e.getMessage(), e);
				Toast.makeText(getApplicationContext(), e.getClass().toString() + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
			}
			ToggleProgressBar(false);
			Log.d(TAG, "Loaded: " + novelCol.getPage());
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
		
		public void onCallback(ICallbackEventData message) {
    		publishProgress(message.getMessage());
    	}

		@Override
		protected AsyncTaskResult<NovelContentModel[]> doInBackground(Void... params) {
			try{
				NovelContentModel[] contents = new NovelContentModel[chapters.length];
				for(int i = 0; i < chapters.length; ++i) {
					NovelContentModel oldContent = dao.getNovelContent(chapters[i], false, null);
					if(oldContent == null) publishProgress("Downloading: " + chapters[i].getTitle());
					else publishProgress("Updating: " + chapters[i].getTitle());
					
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
			synchronized (dialog) {
				if(dialog.isShowing())
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
				}
			}
			else {
				Log.e(TAG, e.getClass().toString() + ": " + e.getMessage(), e);
				Toast.makeText(getApplicationContext(), e.getClass().toString() + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
			}
			ToggleProgressBar(false);
		}
	}
}
