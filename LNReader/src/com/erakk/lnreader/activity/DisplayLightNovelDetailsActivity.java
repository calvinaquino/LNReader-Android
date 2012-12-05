package com.erakk.lnreader.activity;

import java.util.ArrayList;
import java.util.Iterator;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
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
import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.adapter.BookModelAdapter;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.AsyncTaskResult;
import com.erakk.lnreader.model.BookModel;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.NovelContentModel;
import com.erakk.lnreader.model.PageModel;
import com.erakk.lnreader.task.DownloadNovelContentTask;
import com.erakk.lnreader.task.IAsyncTaskOwner;
import com.erakk.lnreader.task.LoadNovelDetailsTask;

public class DisplayLightNovelDetailsActivity extends Activity implements IAsyncTaskOwner {
	public static final String TAG = DisplayLightNovelDetailsActivity.class.toString();
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
					PageModel chapter = novelCol.getBookCollections().get(groupPosition).getChapterCollection().get(childPosition);
					loadChapter(chapter);					
				}
				return false;
			}
		});
    	
        setTitle(page.getTitle());
        isInverted = getColorPreferences();
    }
    
	private void loadChapter(PageModel chapter) {
		if(chapter.isExternal()) {
			try{
			Uri url = Uri.parse(chapter.getPage());
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, url);
			startActivity(browserIntent);
			}catch(Exception ex) {
				String message = "Error when parsing url: " + chapter.getPage();
				Log.e(TAG, message , ex);
				Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
			}
		}
		else {
			Intent intent = new Intent(getApplicationContext(), DisplayLightNovelContentActivity.class);
	        intent.putExtra(Constants.EXTRA_PAGE, chapter.getPage());
	        startActivity(intent);
		}
	}
	
	@Override
    protected void onRestart() {
        super.onRestart();
        if(isInverted != getColorPreferences()) {
        	UIHelper.Recreate(this);
        }
        if(bookModelAdapter != null) {
        	bookModelAdapter.notifyDataSetChanged();
        }
    }
    
	protected void onResume(){
		super.onResume();
		Log.d(TAG, "OnResume: " + task.getStatus().toString());
	}
	
    public void onStop(){
    	// check running task
    	// disable canceling, so it can continue to show the status
//    	if(task != null && !(task.getStatus() == Status.FINISHED)) {
//    		task.cancel(true);
//    	}
//    	if(downloadTask != null && !(downloadTask.getStatus() == Status.FINISHED)) {
//    		downloadTask.cancel(true);
//    	}
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
				if(!temp.isDownloaded()  												// add to list if not downloaded 
				   || (temp.isDownloaded() 
					   && NovelsDao.getInstance(this).isContentUpdated(temp))) // or the update available.
				{
					notDownloadedChapters.add(temp);
				}
			}
			executeDownloadTask(notDownloadedChapters, true);
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
					// add to list if the update available.
					if(NovelsDao.getInstance(this).isContentUpdated(temp)) {
						downloadingChapters.add(temp);
					}
				}
				else {
					downloadingChapters.add(temp);
				}
			}
			executeDownloadTask(downloadingChapters, false);
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
			downloadTask = new DownloadNovelContentTask(new PageModel[] { chapter}, this);
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
		task = new LoadNovelDetailsTask(willRefresh, this);
		String key = TAG + ":" + pageModel.getPage();
		boolean isAdded = LNReaderApplication.getInstance().addTask(key, task);
		if(isAdded) {
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
				task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new PageModel[] {pageModel});
			else
				task.execute(new PageModel[] {pageModel});
		}
		else {
			Log.i(TAG, "Continue execute task: " + key);
			LoadNovelDetailsTask tempTask = (LoadNovelDetailsTask) LNReaderApplication.getInstance().getTask(key);
			if(tempTask != null) {
				task = tempTask;
				task.owner = this;
			}
			toggleProgressBar(true);
		}
	}
	
	@SuppressLint("NewApi")
	private void executeDownloadTask(ArrayList<PageModel> chapters, boolean isAll) {
		if(page != null) {
			downloadTask = new DownloadNovelContentTask((PageModel[]) chapters.toArray(new PageModel[chapters.size()]), this);
			String key = TAG + ":DownloadChapters:" + page.getPage();
			if(isAll) {
				key = TAG + ":DownloadChaptersAll:" + page.getPage();
			}
			boolean isAdded = LNReaderApplication.getInstance().addTask(key, task);
			if(isAdded) {
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
					downloadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				else
					downloadTask.execute();
			}
			else {
				Log.i(TAG, "Continue download task: " + key);
				DownloadNovelContentTask tempTask = (DownloadNovelContentTask) LNReaderApplication.getInstance().getTask(key);
				if(tempTask != null) {
					downloadTask = tempTask;
					downloadTask.owner = this;
				}
				toggleProgressBar(true);
			}
		}		
	}
    
	public void toggleProgressBar(boolean show) {
		if(show) {
			dialog = ProgressDialog.show(this, "Novel Details", "Loading. Please wait...", true);
			dialog.getWindow().setGravity(Gravity.CENTER);
			dialog.setCanceledOnTouchOutside(true);
			dialog.setOnCancelListener(new OnCancelListener() {
				
				public void onCancel(DialogInterface dialog) {
					if(novelCol == null) {
						TextView txtLoading = (TextView) findViewById(R.id.txtLoading);
						txtLoading.setVisibility(View.VISIBLE);
					}
				}
			});
		}
		else {
			dialog.dismiss();
		}
	}

	public void setMessageDialog(ICallbackEventData message) {
		if(dialog.isShowing())
			dialog.setMessage(message.getMessage());
	}

	public void getResult(AsyncTaskResult<?> result) {
		Exception e = result.getError();
		
		if(e == null) {
			// from DownloadNovelContentTask
			if(result.getResult() instanceof NovelContentModel[]) {
				NovelContentModel[] content = (NovelContentModel[]) result.getResult();
				if(content != null) {
					for(Iterator<BookModel> iBook = novelCol.getBookCollections().iterator(); iBook.hasNext();) {
						BookModel book = iBook.next();
						for(Iterator<PageModel> iPage = book.getChapterCollection().iterator(); iPage.hasNext();) {
							PageModel temp = iPage.next();
							for(int i = 0; i < content.length; ++i) {
								if(temp.getPage() == content[i].getPage()) {
									temp.setDownloaded(true);
								}
							}
						}
					}
					bookModelAdapter.notifyDataSetChanged();
				}
			}
			// from LoadNovelDetailsTask
			else if(result.getResult() instanceof NovelCollectionModel) {
				novelCol = (NovelCollectionModel) result.getResult();
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
						String title = page.getTitle();
						if(page.isTeaser()) {
							title += " (Teaser Project)";
						}
						if(page.isStalled()) {
							title += "\nStatus: Project Stalled";
						}
						if(page.isAbandoned()) {
							title += "\nStatus: Project Abandoned";
						}
						if(page.isPending()) {
							title += "\nStatus: Project Pending Authorization";
						}
												
						textViewTitle.setText(title);
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
				Log.d(TAG, "Loaded: " + novelCol.getPage());
			}
		}
		else {
			Log.e(TAG, e.getClass().toString() + ": " + e.getMessage(), e);
			Toast.makeText(getApplicationContext(), e.getClass().toString() + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
		}
		
		TextView txtLoading = (TextView) findViewById(R.id.txtLoading);
		txtLoading.setVisibility(View.GONE);
	}
	
	private boolean getColorPreferences(){
    	return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_INVERT_COLOR, true);
	}
}
