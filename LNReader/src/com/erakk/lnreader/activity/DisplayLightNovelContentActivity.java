package com.erakk.lnreader.activity;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.AsyncTaskResult;
import com.erakk.lnreader.helper.BakaTsukiWebChromeClient;
import com.erakk.lnreader.helper.BakaTsukiWebViewClient;
import com.erakk.lnreader.model.BookModel;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.NovelContentModel;
import com.erakk.lnreader.model.PageModel;
import com.erakk.lnreader.task.IAsyncTaskOwner;
import com.erakk.lnreader.task.LoadNovelContentTask;

public class DisplayLightNovelContentActivity extends Activity implements IAsyncTaskOwner{
	private static final String TAG = DisplayLightNovelContentActivity.class.toString();
	public NovelContentModel content;
	private NovelCollectionModel novelDetails;
	private LoadNovelContentTask task;
	private AlertDialog tocMenu = null;
	private PageModelAdapter jumpAdapter = null;
	private ProgressDialog dialog;
	private WebView webView;
	private BakaTsukiWebViewClient client;
	private boolean restored;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        UIHelper.SetTheme(this, R.layout.activity_display_light_novel_content);		
        UIHelper.SetActionBarDisplayHomeAsUp(this, true);

		// compatibility search box
		final EditText searchText = (EditText) findViewById(R.id.searchText);
		searchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
		    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		        search(searchText.getText().toString());
		        return false;
		    }
		});
		
		webView = (WebView) findViewById(R.id.webView1);
		
		// custom link handler
		client = new BakaTsukiWebViewClient(this);
		webView.setWebViewClient(client);
		webView.setWebChromeClient(new BakaTsukiWebChromeClient(this));
		
		restored = false;
		
		Log.d(TAG, "OnCreate Completed.");
	}
	
	protected void onStart() {
		super.onStart();
		Log.d(TAG, "onStart Completed");
	}

	protected void onRestart() {
		super.onRestart();
		restored = true;
		Log.d(TAG, "onRestart Completed");
	}
	
	@Override
	public void onResume() {
		super.onResume();
		// moved page loading here rather than onCreate
		// to avoid only the first page loaded when resume from sleep 
		// (activity destroyed, onCreate called again)
		// when the user navigate using next/prev/jumpTo
		if(!restored) {
			PageModel pageModel = new PageModel();
	        pageModel.setPage(getIntent().getStringExtra(Constants.EXTRA_PAGE));
			try {
				pageModel = NovelsDao.getInstance(this).getPageModel(pageModel, null);
				executeTask(pageModel, false);
			} catch (Exception e) {
				Log.e(TAG, "Failed to get the PageModel for content: " + getIntent().getStringExtra(Constants.EXTRA_PAGE), e);
			}
		}
		setWebViewSettings();
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
		} catch (Exception e) {
			Log.e(TAG, "Error when saving instance", e);
		}
		Log.d(TAG, "onSaveInstanceState Completed");
	}

	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		String restoredPage = savedInstanceState.getString(Constants.EXTRA_PAGE);
		try {			
			// replace the current pageModel with the saved instance if have different page
			PageModel pageModel = new PageModel();
			pageModel.setPage(restoredPage);
			pageModel = NovelsDao.getInstance(this).getPageModel(pageModel, null);
			executeTask(pageModel, false);
		} catch (Exception e) {
			Log.e(TAG, "Error when restoring instance", e);
		}
		
		// flag that this activity is restored from pause
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
			if(content != null) {
				try {
					executeTask(content.getPageModel(), true);
					//Toast.makeText(getApplicationContext(), "Refreshing", Toast.LENGTH_SHORT).show();
				} catch (Exception e) {
					Log.e(TAG, "Cannot get current chapter.", e);
				}
			}
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
			if(content != null) {
				try {
					PageModel prev = novelDetails.getPrev(content.getPageModel().getPage());
					if(prev!= null) {
						jumpTo(prev);
					}
					else {
						Toast.makeText(getApplicationContext(), "First available chapter.", Toast.LENGTH_SHORT).show();
					}
				} catch (Exception e) {
					Log.e(TAG, "Cannot get previous chapter.", e);
				}
			}
			return true;
		case R.id.menu_chapter_next:
			
			/*
			 * Implement code to move to next chapter
			 */
			if(content != null) {
				try {
					PageModel next = novelDetails.getNext(content.getPageModel().getPage());
					if(next!= null) {
						jumpTo(next);
					}
					else {
						Toast.makeText(getApplicationContext(), "Last available chapter.", Toast.LENGTH_SHORT).show();
					}
				} catch (Exception e) {
					Log.e(TAG, "Cannot get next chapter.", e);
				}
			}
			return true;
		case R.id.menu_chapter_toc:
			if(tocMenu != null) tocMenu.show();
			return true;
		case R.id.menu_search:
			showSearchBox();
			return true;
		case android.R.id.home:
			if(tocMenu != null) tocMenu.show();
			else finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onBackPressed() {
		if(isTaskRoot()) {
			startActivity(new Intent(this, MainActivity.class));
			finish();
		}
		else {
			super.onBackPressed();
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		boolean useVolumeRocker = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_USE_VOLUME_FOR_SCROLL, false);
		if(useVolumeRocker) {
			int scrollSize = UIHelper.GetIntFromPreferences(Constants.PREF_SCROLL_SIZE, 500);
			
			boolean invertScroll = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_INVERT_SCROLL, false);
			if(invertScroll) scrollSize = scrollSize * -1;
			
			if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
				webView.flingScroll(0, scrollSize);
				return true;
			} else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
				webView.flingScroll(0, -scrollSize);
				return true;
			}
			else return super.onKeyDown(keyCode, event);
		}
		return super.onKeyDown(keyCode, event);
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
		executeTask(page, false);
	}
	
	private void buildTOCMenu() {
		if(novelDetails != null && content != null) {
			try {
				PageModel pageModel = content.getPageModel();
				BookModel book = pageModel.getBook();
				if(book != null) {
					ArrayList<PageModel> chapters = book.getChapterCollection();
					for (PageModel chapter : chapters) {
						if(chapter.getPage().contentEquals(pageModel.getPage())) {
							chapter.setHighlighted(true);
						}
						else chapter.setHighlighted(false);
					}
					
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
			} catch (Exception e) {
				Log.e(TAG, "Cannot get current page for menu.", e);
			}
		}
	}

	public void setLastReadState() {
		if(content!= null) {
			// save last position and zoom
			WebView wv = (WebView) findViewById(R.id.webView1);
			content.setLastXScroll(wv.getScrollX());
			content.setLastYScroll(wv.getScrollY());
			//content.setLastZoom(client.getScale());
			content.setLastZoom(wv.getScale());
			try{
				content = NovelsDao.getInstance(this).updateNovelContent(content);
			}catch(Exception ex) {
				Log.e(TAG, "Error when saving state: " + ex.getMessage(), ex);
			}
			
			// check if complete read.
			if(wv.getContentHeight() <=  wv.getScrollY() + wv.getBottom()) {
				try{
					PageModel page = content.getPageModel();
					page.setFinishedRead(true);
					page = NovelsDao.getInstance(this).updatePageModel(page);
				}catch(Exception ex) {
					Log.e(TAG, "Error updating PageModel for Content: " + content.getPage(), ex);
				}
			}
			
			// save for jump to last read.
			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
	    	SharedPreferences.Editor editor = sharedPrefs.edit();
	    	editor.putString(Constants.PREF_LAST_READ, content.getPage());
	    	editor.commit();
	    	
			Log.d(TAG, "Update Content: " + content.getLastXScroll() + " " + content.getLastYScroll() +  " " + content.getLastZoom());
		}
	}

	@SuppressLint("NewApi")
	private void executeTask(PageModel pageModel, boolean refresh) {
		task = new LoadNovelContentTask(refresh, this);
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
			webView.loadData("<p style='background: black; color: white;'>Background task still loading...</p>", "text/html", "utf-8");
			LoadNovelContentTask tempTask = (LoadNovelContentTask) LNReaderApplication.getInstance().getTask(key);
			if(tempTask != null) {
				task = tempTask;
				task.owner = this;
			}
			toggleProgressBar(true);
		}
	}
	
	//@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	public void setContent(NovelContentModel loadedContent) {
		this.content = loadedContent;
		try {
			PageModel pageModel = content.getPageModel();

			if(content.getLastUpdate().getTime() != pageModel.getLastUpdate().getTime())
				Toast.makeText(getApplicationContext(), "Content might be updated: " + content.getLastUpdate().toString() + " != " + pageModel.getLastUpdate().toString(), Toast.LENGTH_LONG).show();
			
			// load the contents here
			final WebView wv = (WebView) findViewById(R.id.webView1);
			setWebViewSettings();
			
			int styleId = -1;
			if(getColorPreferences()) {
				styleId = R.raw.style_dark;
				//Log.d("CSS", "CSS = dark");					
			}
			else {
				styleId = R.raw.style;
				//Log.d("CSS", "CSS = normal");
			}
			ArrayList<Integer> bookmarks = new ArrayList<Integer>();
			bookmarks.add(1);
			bookmarks.add(3);
			bookmarks.add(5);
			
			LNReaderApplication app = (LNReaderApplication) getApplication();
			String html = "<html><head><style type=\"text/css\">"
						+ app.ReadCss(styleId) 
						+ "</style>"
						+ prepareJavaScript(0, bookmarks)
						+ "</head><body onclick='toogleHighlight(this, event);' onload='setup();'>" 
						+ content.getContent() 
						+ "</body></html>";
			wv.loadDataWithBaseURL(Constants.BASE_URL, html, "text/html", "utf-8", "");
			wv.setInitialScale((int) (content.getLastZoom() * 100));
			
			wv.setPictureListener(new PictureListener(){
				boolean needScroll = true;
				@Deprecated
				public void onNewPicture(WebView arg0, Picture arg1) {
					if(needScroll && wv.getContentHeight() * content.getLastZoom() > content.getLastYScroll()) {
						Log.d(TAG, "Content Height: " + wv.getContentHeight() + " : " + content.getLastYScroll());
						wv.scrollTo(0, content.getLastYScroll());
						setLastReadState();
						needScroll = false;
					}						
				}					
			});
			try{
				novelDetails = NovelsDao.getInstance(this).getNovelDetails(pageModel.getParentPageModel(), null);
				
				String volume = pageModel.getParent().replace(pageModel.getParentPageModel().getPage() + Constants.NOVEL_BOOK_DIVIDER, "");
				
				setTitle(pageModel.getTitle() + " (" + volume + ")");
			} catch (Exception ex) {
				Log.e(TAG, "Error when setting title: " + ex.getMessage(), ex);
			}
			Log.d(TAG, "Load Content: " + content.getLastXScroll() + " " + content.getLastYScroll() +  " " + content.getLastZoom());
			
			buildTOCMenu();
			Log.d(TAG, "Loaded: " + content.getPage());
		} catch (Exception e) {
			Log.e(TAG, "Cannot load content.", e);
		}
	}
	
	private String prepareJavaScript(int lastPos, Collection<Integer> bookmarks) {
		String script ="<script type='text/javascript'>";
		String js = LNReaderApplication.getInstance().ReadCss(R.raw.content_script);
		js = "var bookmarkCol = [%bookmarks%];" + js;
		js = "var lastPos = %lastpos%;" + js;
		if(bookmarks != null && bookmarks.size() > 0) {
			js = js.replace("%bookmarks%", LNReaderApplication.join(bookmarks, ","));
		}
		else {
			js = js.replace("%bookmarks%", "");
		}
		if(lastPos > 0) {
			js = js.replace("%lastpos%", "" + lastPos);
		}
		else {
			js = js.replace("%lastpos%", "-1");
		}
		script += js;
		script += "</script>";
		Log.d(TAG, script);
		return script;
	}
	
	@SuppressLint({ "NewApi", "SetJavaScriptEnabled" })
	private void setWebViewSettings() {
		WebView wv = (WebView) findViewById(R.id.webView1);
		
		wv.getSettings().setAllowFileAccess(true);
		
		wv.getSettings().setSupportZoom(getZoomPreferences());
		wv.getSettings().setBuiltInZoomControls(getZoomPreferences());
		
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ) {
			wv.getSettings().setDisplayZoomControls(getZoomControlPreferences());
		}

		wv.getSettings().setLoadWithOverviewMode(true);
		//wv.getSettings().setUseWideViewPort(true);
		wv.getSettings().setLoadsImagesAutomatically(getShowImagesPreferences());
		wv.setBackgroundColor(0);
		wv.getSettings().setJavaScriptEnabled(true);
		//wv.setBackgroundColor(Color.TRANSPARENT);
	}
	
	public void setMessageDialog(ICallbackEventData message) {
		if(dialog.isShowing()) 
			dialog.setMessage(message.getMessage());
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
						webView.loadData("<p style='background: black; color: white;'>Task still loading...</p>", "text/html", "utf-8");
					}
				});
			} 
			else {
				dialog.dismiss();
			}
		}
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
	}

	private boolean getShowImagesPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(Constants.PREF_SHOW_IMAGE, true);
	}
	
	private boolean getColorPreferences(){
    	return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_INVERT_COLOR, true);
	}
	
	private boolean getZoomPreferences(){
    	return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_ZOOM_ENABLED, false);
	}
		
	private boolean getZoomControlPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_SHOW_ZOOM_CONTROL, false);
	}

}
