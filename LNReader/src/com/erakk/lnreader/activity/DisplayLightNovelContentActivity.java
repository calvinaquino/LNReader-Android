package com.erakk.lnreader.activity;

import java.lang.reflect.Method;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Picture;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebView.PictureListener;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
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
import com.erakk.lnreader.task.IAsyncTaskOwner;
import com.erakk.lnreader.task.LoadNovelContentTask;

public class DisplayLightNovelContentActivity extends Activity implements IAsyncTaskOwner{
	private static final String TAG = DisplayLightNovelContentActivity.class.toString();
	private final DisplayLightNovelContentActivity activity = this;
	private NovelsDao dao = NovelsDao.getInstance(this);
	public NovelContentModel content;
	private NovelCollectionModel novelDetails;
	private LoadNovelContentTask task;
	private PageModel pageModel;
	private String volume;
	//private boolean refresh = false;
	private AlertDialog tocMenu = null;
	private PageModelAdapter jumpAdapter = null;
	private ProgressDialog dialog;
	private WebView webView;
	private boolean restored;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        UIHelper.SetTheme(this, R.layout.activity_display_light_novel_content);		
        UIHelper.SetActionBarDisplayHomeAsUp(this, true);

		try {
			PageModel tempPage = new PageModel();
			tempPage.setPage(getIntent().getStringExtra(Constants.EXTRA_PAGE));
			pageModel = dao.getPageModel(tempPage, null);
		} catch (Exception e) {
			Log.e(TAG, "Failed to get the PageModel for content: " + getIntent().getStringExtra(Constants.EXTRA_PAGE), e);
		}
				
		// compatibility search box
		final EditText searchText = (EditText) findViewById(R.id.searchText);
		searchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
		    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		        search(searchText.getText().toString());
		        return false;
		    }
		});
		
		webView = (WebView) findViewById(R.id.webView1);
		Log.d(TAG, "OnCreate Completed: " + pageModel.getPage());
		
		restored = false;
	}
	
	protected void onStart() {
		super.onStart();
		Log.d(TAG, "onStart Completed");
	}

	protected void onRestart() {
		super.onRestart();
		Log.d(TAG, "onRestart Completed");
	}
	
	@Override
	public void onResume() {
		super.onResume();
		// moved page loading here rather than onCreate
		// to avoid only the first page loaded when resume from sleep
		// when the user navigate using next/prev/jumpTo
		if(!restored) executeTask(pageModel, false);
		Log.d(TAG, "onResume Completed");
	}

	@Override
	public void onPause() {
		super.onPause();
		setLastReadState();
		Log.d(TAG, "onPause Completed");
	}
	
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		try {
			if(content == null)
				savedInstanceState.putString(Constants.EXTRA_PAGE, getIntent().getStringExtra(Constants.EXTRA_PAGE));
			else
				savedInstanceState.putString(Constants.EXTRA_PAGE, content.getPageModel().getPage());
			savedInstanceState.putInt(Constants.EXTRA_SCROLL_X, webView.getScrollX());
			savedInstanceState.putInt(Constants.EXTRA_SCROLL_Y, webView.getScrollY());
		} catch (Exception e) {
			Log.e(TAG, "Error when saving instance", e);
		}
		Log.d(TAG, "onSaveInstanceState Completed");
	}

	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		String restoredPage = savedInstanceState.getString(Constants.EXTRA_PAGE);
		if(!restoredPage.equalsIgnoreCase(getIntent().getStringExtra(Constants.EXTRA_PAGE))) {
			try {			
				// replace the current pageModel with the saved instance if have different page
				PageModel tempPage = new PageModel();
				tempPage.setPage(restoredPage);
				pageModel = dao.getPageModel(tempPage, null);
				executeTask(pageModel, false);
				webView.scrollTo(savedInstanceState.getInt(Constants.EXTRA_SCROLL_X), savedInstanceState.getInt(Constants.EXTRA_SCROLL_Y));
			} catch (Exception e) {
				Log.e(TAG, "Error when restoring instance", e);
			}
		}
		setWebViewSettings();
		restored = true;
		Log.d(TAG, "onRestoreInstanceState Completed");
	}
	
	@Override
	public void onStop() {
		super.onStop();
//		don't cancel, so can get the result after closing the activity		
//		if(task.getStatus() != Status.FINISHED) {
//			task.cancel(true);
//		}		
//		
		Log.d(TAG, "onStop Completed");
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
			//refresh = true;
			executeTask(pageModel, true);
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
			showSearchBox();
			return true;
		case android.R.id.home:
			tocMenu.show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@SuppressLint("NewApi")
	private void showSearchBox() {
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB )
			webView.showFindDialog("", true);
		else {
			RelativeLayout searchBox = (RelativeLayout) findViewById(R.id.searchBox);
			searchBox.setVisibility(View.VISIBLE);
		}
	}

	// Compatibility search method for older android version
	@SuppressWarnings("deprecation")
	private void search(String string) {
		if (string != null && string.length() > 0)
			webView.findAll(string);
		
		try {
			Method m = WebView.class.getMethod("setFindIsUp", Boolean.TYPE);
			m.invoke(webView, true);
		} catch (Exception ignored) { }
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
	// end of Compatibility search method for older android version
	
	public void jumpTo(PageModel page){
		setLastReadState();
		pageModel = page;
		executeTask(page, false);
	}	
	
	public void setMessageDialog(String message) {
		if(dialog.isShowing()) 
			dialog.setMessage(message);
	}
	
	public void toggleProgressBar(boolean show) {
		synchronized (this) {
			if(show) {
				dialog = ProgressDialog.show(this, "Novel Content", "Loading. Please wait...", true);
				dialog.getWindow().setGravity(Gravity.CENTER);
				dialog.setCanceledOnTouchOutside(true);
				dialog.setOnCancelListener(new OnCancelListener() {
					
					public void onCancel(DialogInterface dialog) {
						WebView webView = (WebView) findViewById(R.id.webView1);
						webView.loadData("<p>Task still loading...</p>", "text/html", "utf-8");
					}
				});
			} 
			else {
				dialog.dismiss();
			}
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

	public void setLastReadState() {
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
	private void executeTask(PageModel pageModel, boolean refresh) {
		task = new LoadNovelContentTask(refresh);
		task.owner = this;
		String key = TAG + ":" + pageModel.getPage();
		boolean isAdded = LNReaderApplication.getInstance().addTask(key, task);
		if(isAdded) {
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
				task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new PageModel[] {pageModel});
			else
				task.execute(new PageModel[] {pageModel});
		}
		else {
			WebView webView = (WebView) findViewById(R.id.webView1);
			webView.loadData("<p>Background task still loading...</p>", "text/html", "utf-8");
			LoadNovelContentTask tempTask = (LoadNovelContentTask) LNReaderApplication.getInstance().getTask(key);
			if(tempTask != null) {
				task = tempTask;
				task.owner = this;
			}
		}
	}
	
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	public void setContent(NovelContentModel loadedContent) {
		this.content = loadedContent;
		if(content.getLastUpdate().getTime() != pageModel.getLastUpdate().getTime())
			Toast.makeText(getApplicationContext(), "Content might be updated: " + content.getLastUpdate().toString() + " != " + pageModel.getLastUpdate().toString(), Toast.LENGTH_LONG).show();
		
		// load the contents here
		final WebView wv = (WebView) findViewById(R.id.webView1);
		setWebViewSettings();

		// custom link handler
		BakaTsukiWebViewClient client = new BakaTsukiWebViewClient(activity);
		wv.setWebViewClient(client);
		
		int styleId = -1;
		if(getColorPreferences()) {
			styleId = R.raw.style_dark;
			//Log.d("CSS", "CSS = dark");					
		}
		else {
			styleId = R.raw.style;
			//Log.d("CSS", "CSS = normal");
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
		Log.d(TAG, "Loaded: " + content.getPage());
	}

	private void setWebViewSettings() {
		WebView wv = (WebView) findViewById(R.id.webView1);
		wv.getSettings().setAllowFileAccess(true);
		
		wv.getSettings().setSupportZoom(getZoomPreferences());
		wv.getSettings().setBuiltInZoomControls(getZoomPreferences());

		wv.getSettings().setLoadWithOverviewMode(true);
		//wv.getSettings().setUseWideViewPort(true);
		wv.getSettings().setLoadsImagesAutomatically(getShowImagesPreferences());
		wv.setBackgroundColor(0);
		//wv.setBackgroundColor(Color.TRANSPARENT);
	}
	
	public void getResult(AsyncTaskResult<?> result) {
		Exception e = result.getError();
		if(e == null) {
			NovelContentModel loadedContent = (NovelContentModel) result.getResult();
			setContent(loadedContent);
		}
		else {
			Log.e(TAG, "Error when loading novel content: " + e.getMessage(), e);
			Toast.makeText(getApplicationContext(), e.getClass().toString() + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
		}			
		toggleProgressBar(false);
		//refresh = false;
	}

	private boolean getShowImagesPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(Constants.PREF_SHOW_IMAGE, false);
	}
	
	private boolean getColorPreferences(){
    	return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_INVERT_COLOR, false);
	}
	
	private boolean getZoomPreferences(){
    	return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_ZOOM_ENABLED, false);
	}
}
