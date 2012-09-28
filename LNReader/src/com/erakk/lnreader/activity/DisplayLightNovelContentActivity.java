package com.erakk.lnreader.activity;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Picture;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebView.PictureListener;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.adapter.PageModelAdapter;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.AsyncTaskResult;
import com.erakk.lnreader.helper.BakaTsukiWebViewClient;
import com.erakk.lnreader.model.BookModel;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.NovelContentModel;
import com.erakk.lnreader.model.PageModel;

public class DisplayLightNovelContentActivity extends Activity {
	private static final String TAG = DisplayLightNovelContentActivity.class.toString();
	private final Activity activity = this;
	private NovelsDao dao = NovelsDao.getInstance(this);
	private NovelContentModel content;
	private NovelCollectionModel novelDetails;
	private LoadNovelContentTask task;
	private PageModel pageModel;
	private String volume;
	private boolean refresh = false;
	private AlertDialog tocMenu = null;
	private PageModelAdapter jumpAdapter = null;
	private ProgressDialog dialog;
	private WebView webView;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        UIHelper.SetTheme(this, R.layout.activity_display_light_novel_content);		
        UIHelper.SetActionBarDisplayHomeAsUp(this, true);

		try {
			pageModel = dao.getPageModel(getIntent().getStringExtra(Constants.EXTRA_PAGE), null);
		} catch (Exception e) {
			Log.e(TAG, "Failed to get the PageModel for content: " + getIntent().getStringExtra(Constants.EXTRA_PAGE), e);
		}
				
		// search box
		EditText searchText = (EditText) findViewById(R.id.searchText);
		searchText.addTextChangedListener(new TextWatcher(){

			public void afterTextChanged(Editable arg0) { }

			public void beforeTextChanged(CharSequence s, int start, int count,	int after) { }

			public void onTextChanged(CharSequence s, int start, int before, int count) {
				search(s.toString());
			}						
		});
		
		webView = (WebView) findViewById(R.id.webView1);
		Log.d(TAG, "OnCreate Completed");
	}
	
	@SuppressWarnings("deprecation")
	private void search(String string) {
		if(string != null && string.length() > 0)
			webView.findAll(string);
	}
	
	public void searchNext(View view) {
		webView.findNext(true);
	}
	
	public void searchPrev(View view) {
		webView.findNext(false);
	}
	
	public void closeSearchBox(View view) {
		RelativeLayout searchBox = (RelativeLayout) findViewById(R.id.searchBox);
		searchBox.setVisibility(View.GONE);
		webView.clearMatches();
	}
	
	@Override
	public void onPause() {
		setLastReadState();
		Log.d(TAG, "Pausing activity");
		super.onPause();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "Resuming activity");
		// the actual loading
		executeTask(pageModel);
	}
	
	@Override
	public void onStop() {
		if(task.getStatus() != Status.FINISHED) {
			task.cancel(true);
		}
		Log.d(TAG, "Stopping activity");
		super.onStop();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_display_light_novel_content, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			Intent launchNewIntent = new Intent(this, DisplaySettingsActivity.class);
			startActivity(launchNewIntent);
			return true;
		case R.id.menu_refresh_chapter_content:
			
			/*
			 * Implement code to refresh chapter content
			 */
			refresh = true;
			executeTask(pageModel);
			Toast.makeText(getApplicationContext(), "Refreshing", Toast.LENGTH_SHORT).show();
			return true;
		case R.id.invert_colors:
			
			/*
			 * Implement code to invert colors
			 */
			UIHelper.ToggleColorPref(this);
			UIHelper.Recreate(this);
			return true;
		case R.id.menu_chapter_previous:
			
			/*
			 * Implement code to move to previous chapter
			 */
			PageModel prev = novelDetails.getPrev(pageModel.getPage());
			if(prev!= null) {
				jumpTo(prev);
			}
			else {
				Toast.makeText(getApplicationContext(), "First available chapter.", Toast.LENGTH_SHORT).show();
			}
			return true;
		case R.id.menu_chapter_next:
			
			/*
			 * Implement code to move to next chapter
			 */
			PageModel next = novelDetails.getNext(pageModel.getPage());
			if(next!= null) {
				jumpTo(next);
			}
			else {
				Toast.makeText(getApplicationContext(), "Last available chapter.", Toast.LENGTH_SHORT).show();
			}
			
			return true;
		case R.id.menu_chapter_toc:
			tocMenu.show();
			return true;
		case R.id.menu_search:
			RelativeLayout searchBox = (RelativeLayout) findViewById(R.id.searchBox);
			searchBox.setVisibility(View.VISIBLE);
			return true;
		case android.R.id.home:
			tocMenu.show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void jumpTo(PageModel page){
		setLastReadState();
		pageModel = page;
		executeTask(page);
	}	
	
	private void ToggleProgressBar(boolean show) {
		if(show) {
			dialog = ProgressDialog.show(this, "Novel Content", "Loading. Please wait...", true);
			dialog.getWindow().setGravity(Gravity.CENTER);
			dialog.setCanceledOnTouchOutside(true);
		} 
		else {
			dialog.dismiss();
		}
	}
	
	private void buildTOCMenu() {
		if(novelDetails != null) {
			BookModel book = pageModel.getBook();
			if(book != null) {
				ArrayList<PageModel> chapters = book.getChapterCollection();
				int resourceId = R.layout.novel_list_item;
				if(UIHelper.IsSmallScreen(this)) {
					resourceId = R.layout.novel_list_item_small; 
				}
				jumpAdapter = new PageModelAdapter(this, resourceId, chapters);
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("Jump To");
				builder.setAdapter(jumpAdapter, new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						PageModel page = jumpAdapter.getItem(which);
						jumpTo(page);
					}				
				});
				tocMenu = builder.create();
			}
		}
	}

	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		try {
			savedInstanceState.putString(Constants.EXTRA_PAGE, content.getPageModel().getPage());
		} catch (Exception e) {
			Log.e(TAG, "Error when saving instance", e);
		}
	}

	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		try {
			// replace the current pageModel with the saved instance
			pageModel = dao.getPageModel(savedInstanceState.getString(Constants.EXTRA_PAGE), null);
		} catch (Exception e) {
			Log.e(TAG, "Error when restoring instance", e);
		}
		Log.d(TAG, "onRestoreInstanceState Completed");
	}
	
	private void setLastReadState() {
		if(content!= null) {
			// save last position and zoom
			WebView wv = (WebView) findViewById(R.id.webView1);
			content.setLastXScroll(wv.getScrollX());
			content.setLastYScroll(wv.getScrollY());
			content.setLastZoom(wv.getScale());
			try{
				content = dao.updateNovelContent(content);
			}catch(Exception ex) {
				ex.printStackTrace();
			}
			if(wv.getContentHeight() <=  wv.getScrollY() + wv.getBottom()) {
				try{
					PageModel page = content.getPageModel();
					page.setFinishedRead(true);
					page = dao.updatePageModel(page);
					Log.d(TAG, "Update Content: " + content.getLastXScroll() + " " + content.getLastYScroll() +  " " + content.getLastZoom());
				}catch(Exception ex) {
					ex.printStackTrace();
					Log.d(TAG, "Error updating PageModel for Content: " + content.getPage());
				}
			}
			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
	    	SharedPreferences.Editor editor = sharedPrefs.edit();
	    	editor.putString(Constants.PREF_LAST_READ, content.getPage());
	    	editor.commit();
		}
	}
	
	@SuppressLint("NewApi")
	private void executeTask(PageModel pageModel) {
		task = new LoadNovelContentTask();
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new PageModel[] {pageModel});
		else
			task.execute(new PageModel[] {pageModel});
	}
	
	public void setContent(NovelContentModel content) {
		this.content = content;
	}
	
	public boolean getColorPreferences(){
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    	return sharedPrefs.getBoolean(Constants.PREF_INVERT_COLOR, false);
	}
	
	public class LoadNovelContentTask extends AsyncTask<PageModel, String, AsyncTaskResult<NovelContentModel>> implements ICallbackNotifier{
		public void onCallback(ICallbackEventData message) {
    		publishProgress(message.getMessage());
    	}
    	
		@Override
		protected void onPreExecute (){
			// executed on UI thread.
			ToggleProgressBar(true);
		}
		
		@Override
		protected AsyncTaskResult<NovelContentModel> doInBackground(PageModel... params) {
			try{
				PageModel p = params[0];
				if(refresh) {
					return new AsyncTaskResult<NovelContentModel>(dao.getNovelContentFromInternet(p, this));
				}
				else {
					return new AsyncTaskResult<NovelContentModel>(dao.getNovelContent(p, this));
				}
			}catch(Exception e) {
				return new AsyncTaskResult<NovelContentModel>(e);
			}
		}
		
		@Override
		protected void onProgressUpdate (String... values){
			//executed on UI thread.
			dialog.setMessage(values[0]);
		}
		
		@SuppressWarnings("deprecation")
		protected void onPostExecute(AsyncTaskResult<NovelContentModel> result) {
			Exception e = result.getError();
			
			if(e == null) {
				content = result.getResult();
				
				// load the contents here
				final WebView wv = (WebView) findViewById(R.id.webView1);
				wv.getSettings().setAllowFileAccess(true);
				wv.getSettings().setSupportZoom(true);
				wv.getSettings().setBuiltInZoomControls(true);
				wv.getSettings().setLoadWithOverviewMode(true);
				//wv.getSettings().setUseWideViewPort(true);
				wv.getSettings().setLoadsImagesAutomatically(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("show_images", false));
				//wv.setBackgroundColor(0);
				wv.setBackgroundColor(Color.TRANSPARENT);

				// custom link handler
				BakaTsukiWebViewClient client = new BakaTsukiWebViewClient(activity);
				wv.setWebViewClient(client);

				int styleId = -1;
				if(getColorPreferences()) {
					styleId = R.raw.style_dark;
					Log.d("CSS", "CSS = dark");					
				}
				else {
					styleId = R.raw.style;
					Log.d("CSS", "CSS = normal");
				}
				LNReaderApplication app = (LNReaderApplication) getApplication();
				String html = "<html><head><style type=\"text/css\">" + app.ReadCss(styleId) + "</style></head><body>" + content.getContent() + "</body></html>";
				wv.loadDataWithBaseURL(Constants.BASE_URL, html, "text/html", "utf-8", "");

				wv.setInitialScale((int) (content.getLastZoom() * 100));
				
				wv.setPictureListener(new PictureListener(){
					boolean needScroll = true;
					@Deprecated
					public void onNewPicture(WebView arg0, Picture arg1) {
						Log.d(TAG, "Content Height: " + wv.getContentHeight() + " : " + content.getLastYScroll());
						if(needScroll && wv.getContentHeight() * content.getLastZoom() > content.getLastYScroll()) {
							wv.scrollTo(0, content.getLastYScroll());
							needScroll = false;
						}						
					}					
				});
				try{
					novelDetails = dao.getNovelDetails(pageModel.getParentPageModel(), null);
					
					volume = pageModel.getParent().replace(pageModel.getParentPageModel().getPage() + Constants.NOVEL_BOOK_DIVIDER, "");
					
					setTitle(pageModel.getTitle() + " (" + volume + ")");
				} catch (Exception ex) {
					Log.e(TAG, "Error when setting title: " + ex.getMessage(), ex);
				}
				Log.d(TAG, "Load Content: " + content.getLastXScroll() + " " + content.getLastYScroll() +  " " + content.getLastZoom());
				
				buildTOCMenu();
			}
			else {
				Log.e(TAG, "Error when loading novel content: " + e.getMessage(), e);
				Toast.makeText(getApplicationContext(), e.getClass().toString() + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
			}			
			ToggleProgressBar(false);
			refresh = false;
		}
	}
}
