package com.erakk.lnreader.activity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.erakk.lnreader.Constants;
import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.adapter.BookmarkModelAdapter;
import com.erakk.lnreader.adapter.PageModelAdapter;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.AsyncTaskResult;
import com.erakk.lnreader.helper.BakaTsukiWebChromeClient;
import com.erakk.lnreader.helper.BakaTsukiWebViewClient;
import com.erakk.lnreader.helper.NonLeakingWebView;
import com.erakk.lnreader.helper.Util;
import com.erakk.lnreader.model.BookModel;
import com.erakk.lnreader.model.BookmarkModel;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.NovelContentModel;
import com.erakk.lnreader.model.PageModel;
import com.erakk.lnreader.task.IAsyncTaskOwner;
import com.erakk.lnreader.task.LoadNovelContentTask;

public class DisplayLightNovelContentActivity extends SherlockActivity implements IAsyncTaskOwner {
	private static final String TAG = DisplayLightNovelContentActivity.class.toString();
	public NovelContentModel content;
	private NovelCollectionModel novelDetails;
	private LoadNovelContentTask task;
	private AlertDialog tocMenu = null;
	private PageModelAdapter jumpAdapter = null;
	private BookmarkModelAdapter bookmarkAdapter = null;
	private ProgressDialog dialog;
	private NonLeakingWebView webView;
	private ImageButton goTop;
	private ImageButton goBottom;
	private BakaTsukiWebViewClient client;
	private boolean restored;
	private AlertDialog bookmarkMenu = null;
	boolean isFullscreen;

	boolean dynamicButtonsEnabled;
	Runnable hideBottom;
	Runnable hideTop;
	Handler mHandler = new Handler();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		isFullscreen = getFullscreenPreferences();
		UIHelper.ToggleFullscreen(this, isFullscreen);
		UIHelper.SetTheme(this, R.layout.activity_display_light_novel_content);
		UIHelper.SetActionBarDisplayHomeAsUp(this, true);

		// compatibility search box
		final EditText searchText = (EditText) findViewById(R.id.searchText);
		searchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				search(searchText.getText().toString());
				return false;
			}
		});

		webView = (NonLeakingWebView) findViewById(R.id.webViewContent);
		goTop = (ImageButton) findViewById(R.id.webview_go_top);
		goBottom = (ImageButton) findViewById(R.id.webview_go_bottom);

		// custom link handler
		client = new BakaTsukiWebViewClient(this);
		webView.setWebViewClient(client);
		webView.setWebChromeClient(new BakaTsukiWebChromeClient(this));

		restored = false;
		Log.d(TAG, "OnCreate Completed.");

		// Hide button after a certain time being shown
		hideBottom = new Runnable() {

			@Override
			public void run() {
				goBottom.setVisibility(ImageButton.GONE);
			}
		};
		hideTop = new Runnable() {

			@Override
			public void run() {
				goTop.setVisibility(ImageButton.GONE);
			}
		};
		// Android Studio Config Commit Test
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (webView != null) {
			RelativeLayout rootView = (RelativeLayout) findViewById(R.id.rootView);
			rootView.removeView(webView);
			webView.removeAllViews();
			webView.destroy();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(TAG, "onStart Completed");
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		restored = true;
		Log.d(TAG, "onRestart Completed");
	}

	@Override
	public void onResume() {
		super.onResume();

		dynamicButtonsEnabled = getDynamicButtonsPreferences();

		if (isFullscreen != getFullscreenPreferences()) {
			UIHelper.Recreate(this);
		}

		// show/hide option button
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			Button btnOption = (Button) findViewById(R.id.btnMenu);
			if (getFullscreenPreferences()) {
				btnOption.setVisibility(View.VISIBLE);
			} else {
				btnOption.setVisibility(View.GONE);
			}
		}

		// moved page loading here rather than onCreate
		// to avoid only the first page loaded when resume from sleep
		// (activity destroyed, onCreate called again)
		// when the user navigate using next/prev/jumpTo
		if (!restored) {
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
		if (content != null) {
			NonLeakingWebView wv = (NonLeakingWebView) findViewById(R.id.webViewContent);
			int pos = content.getLastYScroll();
			if (pos > 0)
				pos = pos - 1;
			wv.loadUrl("javascript:goToParagraph(" + pos + ")");
		}
		Log.d(TAG, "onResume Completed");
	}

	@Override
	public void onPause() {
		super.onPause();

		setLastReadState();
		Log.d(TAG, "onPause Completed");
	}

	public void toggleTopButton(boolean enable) {
		if (enable) {
			goTop.setVisibility(ImageButton.VISIBLE);
			mHandler.removeCallbacks(hideTop);
			mHandler.postDelayed(hideTop, 1000);
		} else
			goTop.setVisibility(ImageButton.GONE);
	}

	public void toggleBottomButton(boolean enable) {
		if (enable) {
			goBottom.setVisibility(ImageButton.VISIBLE);
			mHandler.removeCallbacks(hideBottom);
			mHandler.postDelayed(hideBottom, 1000);
		} else
			goBottom.setVisibility(ImageButton.GONE);
	}

	public void goTop(View view) {
		webView.pageUp(true);
	}

	public void goBottom(View view) {
		webView.pageDown(true);
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		try {
			if (content == null)
				savedInstanceState.putString(Constants.EXTRA_PAGE, getIntent().getStringExtra(Constants.EXTRA_PAGE));
			else
				savedInstanceState.putString(Constants.EXTRA_PAGE, content.getPageModel().getPage());
		} catch (Exception e) {
			Log.e(TAG, "Error when saving instance", e);
		}
		Log.d(TAG, "onSaveInstanceState Completed");
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		String restoredPage = savedInstanceState.getString(Constants.EXTRA_PAGE);
		try {
			// replace the current pageModel with the saved instance if have
			// different page
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
		// don't cancel, so can get the result after closing the activity
		// if(task.getStatus() != Status.FINISHED) {
		// task.cancel(true);
		// }
		//
		Log.d(TAG, "onStop Completed");
	}

	@Override
	@SuppressLint("NewApi")
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.activity_display_light_novel_content, menu);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			if (getFullscreenPreferences()) {
				menu.findItem(R.id.menu_chapter_next).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
				menu.findItem(R.id.menu_chapter_previous).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
			}
		}
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
			// refresh = true;
			if (content != null) {
				try {
					executeTask(content.getPageModel(), true);
					// Toast.makeText(getApplicationContext(), "Refreshing",
					// Toast.LENGTH_SHORT).show();
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
			if (content != null) {
				try {
					PageModel prev = novelDetails.getPrev(content.getPageModel().getPage());
					if (prev != null) {
						jumpTo(prev);
					} else {
						Toast.makeText(getApplicationContext(), getResources().getString(R.string.first_available_chapter), Toast.LENGTH_SHORT).show();
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
			if (content != null) {
				try {
					PageModel next = novelDetails.getNext(content.getPageModel().getPage());
					if (next != null) {
						jumpTo(next);
					} else {
						Toast.makeText(getApplicationContext(), getResources().getString(R.string.last_available_chapter), Toast.LENGTH_SHORT).show();
					}
				} catch (Exception e) {
					Log.e(TAG, "Cannot get next chapter.", e);
				}
			}
			return true;
		case R.id.menu_chapter_toc:
			if (tocMenu != null)
				tocMenu.show();
			return true;
		case R.id.menu_search:
			showSearchBox();
			return true;
		case R.id.menu_bookmarks_here:
			if (bookmarkMenu != null)
				bookmarkMenu.show();
			return true;
		case R.id.menu_bookmarks:
			Intent bookmarkIntent = new Intent(this, DisplayBookmarkActivity.class);
			startActivity(bookmarkIntent);
			return true;
		case R.id.menu_downloads_list:
			Intent downloadsItent = new Intent(this, DownloadListActivity.class);
			startActivity(downloadsItent);
			;
			return true;
		case android.R.id.home:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onBackPressed() {
		if (isTaskRoot()) {
			startActivity(new Intent(this, MainActivity.class));
			finish();
		} else {
			super.onBackPressed();
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		boolean useVolumeRocker = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_USE_VOLUME_FOR_SCROLL, false);
		if (useVolumeRocker) {
			int scrollSize = UIHelper.GetIntFromPreferences(Constants.PREF_SCROLL_SIZE, 5) * 100;

			boolean invertScroll = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_INVERT_SCROLL, false);
			if (invertScroll)
				scrollSize = scrollSize * -1;

			if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
				webView.flingScroll(0, -scrollSize);
				Log.d("Volume", "Up Pressed");
				return true;
			} else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
				webView.flingScroll(0, +scrollSize);
				Log.d("Volume", "Down Pressed");
				return true;
			} else
				return super.onKeyDown(keyCode, event);
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP) || (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
			boolean useVolumeRocker = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_USE_VOLUME_FOR_SCROLL, false);
			if (!useVolumeRocker) {
				return false;
			}
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	@SuppressLint("NewApi")
	private void showSearchBox() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
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
			Method m = NonLeakingWebView.class.getMethod("setFindIsUp", Boolean.TYPE);
			m.invoke(webView, true);
		} catch (Exception ignored) {
		}
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

	public void jumpTo(PageModel page) {
		setLastReadState();
		if (page.isExternal() && !getHandleExternalLinkPreferences()) {
			try {
				Uri url = Uri.parse(page.getPage());
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, url);
				startActivity(browserIntent);
			} catch (Exception ex) {
				String message = getResources().getString(R.string.error_parsing_url) + ": " + page.getPage();
				Log.e(TAG, message, ex);
				Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
			}
		} else
			executeTask(page, false);
	}

	private void buildTOCMenu(PageModel referencePageModel) {
		Log.d(TAG, "Trying to create TOC");
		// if(novelDetails != null) {
		try {
			BookModel book = referencePageModel.getBook();
			if (book != null) {
				ArrayList<PageModel> chapters = book.getChapterCollection();
				for (PageModel chapter : chapters) {
					if (chapter.getPage().contentEquals(referencePageModel.getPage())) {
						chapter.setHighlighted(true);
					} else
						chapter.setHighlighted(false);
				}
				Log.d(TAG, "TOC Found: " + chapters.size());

				int resourceId = R.layout.jumpto_list_item;
				if (UIHelper.IsSmallScreen(this)) {
					resourceId = R.layout.jumpto_list_item;
				}
				jumpAdapter = new PageModelAdapter(this, resourceId, chapters);
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(getResources().getString(R.string.content_toc));
				builder.setAdapter(jumpAdapter, new OnClickListener() {
					@Override
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
		// }
	}

	public void buildBookmarkMenu() {
		if (content != null) {
			try {
				int resourceId = R.layout.bookmark_list_item;
				if (UIHelper.IsSmallScreen(this)) {
					resourceId = R.layout.bookmark_list_item_small;
				}
				bookmarkAdapter = new BookmarkModelAdapter(this, resourceId, content.getBookmarks(), content.getPageModel());
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(getResources().getString(R.string.bookmarks));
				builder.setAdapter(bookmarkAdapter, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						BookmarkModel bookmark = bookmarkAdapter.getItem(which);
						NonLeakingWebView wv = (NonLeakingWebView) findViewById(R.id.webViewContent);
						wv.loadUrl("javascript:goToParagraph(" + bookmark.getpIndex() + ")");
					}
				});
				bookmarkMenu = builder.create();
			} catch (Exception e) {
				Log.e(TAG, "Error getting pageModel: " + e.getMessage(), e);
			}
		}
	}

	@SuppressWarnings("deprecation")
	public void setLastReadState() {
		if (content != null) {
			// save last position and zoom
			NonLeakingWebView wv = (NonLeakingWebView) findViewById(R.id.webViewContent);
			// content.setLastXScroll(wv.getScrollX());
			// content.setLastYScroll(wv.getScrollY());
			content.setLastZoom(wv.getScale());
			try {
				content = NovelsDao.getInstance(this).updateNovelContent(content);
			} catch (Exception ex) {
				Log.e(TAG, "Error when saving state: " + ex.getMessage(), ex);
			}

			// check if complete read.
			if (wv.getContentHeight() <= wv.getScrollY() + wv.getBottom()) {
				try {
					PageModel page = content.getPageModel();
					page.setFinishedRead(true);
					page = NovelsDao.getInstance(this).updatePageModel(page);
				} catch (Exception ex) {
					Log.e(TAG, "Error updating PageModel for Content: " + content.getPage(), ex);
				}
			}

			// save for jump to last read.
			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
			SharedPreferences.Editor editor = sharedPrefs.edit();
			editor.putString(Constants.PREF_LAST_READ, content.getPage());
			editor.commit();

			Log.d(TAG, "Update Content: " + content.getLastXScroll() + " " + content.getLastYScroll() + " " + content.getLastZoom());
		}
	}

	@SuppressLint("NewApi")
	private void executeTask(PageModel pageModel, boolean refresh) {
		NonLeakingWebView webView = (NonLeakingWebView) findViewById(R.id.webViewContent);
		if (pageModel.isExternal()) {
			loadExternalUrl(pageModel);
		} else {
			isLoaded = false;
			task = new LoadNovelContentTask(refresh, this);
			String key = TAG + ":" + pageModel.getPage();
			boolean isAdded = LNReaderApplication.getInstance().addTask(key, task);
			if (isAdded) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
					task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new PageModel[] { pageModel });
				else
					task.execute(new PageModel[] { pageModel });
			} else {
				webView.loadData("<p style='background: black; color: white;'>" + getResources().getString(R.string.background_task_load) + "</p>", "text/html", "utf-8");
				LoadNovelContentTask tempTask = (LoadNovelContentTask) LNReaderApplication.getInstance().getTask(key);
				if (tempTask != null) {
					task = tempTask;
					task.owner = this;
				}
				toggleProgressBar(true);
			}
		}
	}

	public void loadExternalUrl(PageModel pageModel) {
		try {
			final NonLeakingWebView wv = (NonLeakingWebView) findViewById(R.id.webViewContent);
			setWebViewSettings();
			wv.loadUrl(pageModel.getPage());
			setChapterTitle(pageModel);
			buildTOCMenu(pageModel);
			content = null;
		} catch (Exception ex) {
			Log.e(TAG, "Cannot load external content: " + pageModel.getPage(), ex);
		}
	}

	public void setContent(NovelContentModel loadedContent) {
		this.content = loadedContent;
		try {
			PageModel pageModel = content.getPageModel();

			if (content.getLastUpdate().getTime() != pageModel.getLastUpdate().getTime())
				Toast.makeText(getApplicationContext(), getResources().getString(R.string.content_may_updated) + ": " + content.getLastUpdate().toString() + " != " + pageModel.getLastUpdate().toString(), Toast.LENGTH_LONG).show();

			// load the contents here
			final NonLeakingWebView wv = (NonLeakingWebView) findViewById(R.id.webViewContent);
			setWebViewSettings();

			int lastPos = content.getLastYScroll();
			int pIndex = getIntent().getIntExtra(Constants.EXTRA_P_INDEX, -1);
			if (pIndex > 0)
				lastPos = pIndex;

			String html = "<html><head><style type=\"text/css\">" + getCSSSheet() + "</style>" + "<meta name='viewport' content='width=device-width, minimum-scale=0.5, maximum-scale=5' />" + prepareJavaScript(lastPos, content.getBookmarks()) + "</head><body onclick='toogleHighlight(this, event);' onload='setup();'>" + content.getContent() + "</body></html>";
			wv.loadDataWithBaseURL(Constants.BASE_URL, html, "text/html", "utf-8", "");
			if (content.getLastZoom() > 0) {
				wv.setInitialScale((int) (content.getLastZoom() * 100));
			} else {
				wv.setInitialScale(100);
			}

			setChapterTitle(pageModel);
			Log.d(TAG, "Load Content: " + content.getLastXScroll() + " " + content.getLastYScroll() + " " + content.getLastZoom());

			buildTOCMenu(pageModel);
			buildBookmarkMenu();

			Log.d(TAG, "Loaded: " + content.getPage());
		} catch (Exception e) {
			Log.e(TAG, "Cannot load content.", e);
		}
	}

	private void setChapterTitle(PageModel pageModel) {
		try {
			Log.d(TAG, "Parent Page: " + pageModel.getParent());
			novelDetails = NovelsDao.getInstance(this).getNovelDetails(pageModel.getParentPageModel(), null);

			String volume = pageModel.getParent().replace(pageModel.getParentPageModel().getPage() + Constants.NOVEL_BOOK_DIVIDER, "");

			setTitle(pageModel.getTitle() + " (" + volume + ")");
		} catch (Exception ex) {
			Log.e(TAG, "Error when setting title: " + ex.getMessage(), ex);
			setTitle(pageModel.getPage());
		}
	}

	private String prepareJavaScript(int lastPos, ArrayList<BookmarkModel> bookmarks) {
		String script = "<script type='text/javascript'>";
		String js = LNReaderApplication.getInstance().ReadCss(R.raw.content_script);
		js = "var bookmarkCol = [%bookmarks%];" + js;
		js = "var lastPos = %lastpos%;" + js;
		js = "var isBookmarkEnabled = " + getBookmarkPreferences() + ";" + js;

		if (bookmarks != null && bookmarks.size() > 0) {
			ArrayList<Integer> list = new ArrayList<Integer>();
			for (BookmarkModel bookmark : bookmarks) {
				list.add(bookmark.getpIndex());
			}
			js = js.replace("%bookmarks%", Util.join(list, ","));
		} else {
			js = js.replace("%bookmarks%", "");
		}
		if (lastPos > 0) {
			js = js.replace("%lastpos%", "" + lastPos);
			Log.d(TAG, "Last Position: " + lastPos);
		} else {
			js = js.replace("%lastpos%", "-1");
		}
		script += js;
		script += "</script>";

		return script;
	}

	@SuppressLint({ "NewApi", "SetJavaScriptEnabled" })
	private void setWebViewSettings() {
		NonLeakingWebView wv = (NonLeakingWebView) findViewById(R.id.webViewContent);

		wv.getSettings().setAllowFileAccess(true);

		wv.getSettings().setSupportZoom(getZoomPreferences());
		wv.getSettings().setBuiltInZoomControls(getZoomPreferences());

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			wv.getSettings().setDisplayZoomControls(getZoomControlPreferences());
		}

		wv.getSettings().setLoadWithOverviewMode(true);
		// wv.getSettings().setUseWideViewPort(true);
		wv.getSettings().setLoadsImagesAutomatically(getShowImagesPreferences());
		wv.setBackgroundColor(0);
		wv.getSettings().setJavaScriptEnabled(true);

		if (isLoaded)
			wv.loadUrl("javascript:toogleEnableBookmark(" + getBookmarkPreferences() + ")");
	}

	@Override
	public void setMessageDialog(ICallbackEventData message) {
		if (dialog.isShowing())
			dialog.setMessage(message.getMessage());
	}

	@Override
	public void toggleProgressBar(boolean show) {
		synchronized (this) {
			if (show) {
				dialog = ProgressDialog.show(this, getResources().getString(R.string.title_activity_display_novel_content), "Loading. Please wait...", true);
				dialog.getWindow().setGravity(Gravity.CENTER);
				dialog.setCanceledOnTouchOutside(true);
				dialog.setOnCancelListener(new OnCancelListener() {

					@Override
					public void onCancel(DialogInterface dialog) {
						NonLeakingWebView webView = (NonLeakingWebView) findViewById(R.id.webViewContent);
						if (webView != null)
							webView.loadData("<p style='background: black; color: white;'>Task still loading...</p>", "text/html", "utf-8");
					}
				});
			} else {
				dialog.dismiss();
			}
		}
	}

	@Override
	public void getResult(AsyncTaskResult<?> result) {
		Exception e = result.getError();
		if (e == null) {
			NovelContentModel loadedContent = (NovelContentModel) result.getResult();
			setContent(loadedContent);
		} else {
			Log.e(TAG, "Error when loading novel content: " + e.getMessage(), e);
			Toast.makeText(getApplicationContext(), e.getClass().toString() + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
		}
		toggleProgressBar(false);
	}

	private boolean getShowImagesPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(Constants.PREF_SHOW_IMAGE, true);
	}

	private boolean getColorPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_INVERT_COLOR, true);
	}

	private boolean getZoomPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_ZOOM_ENABLED, false);
	}

	private boolean getZoomControlPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_SHOW_ZOOM_CONTROL, false);
	}

	private boolean getFullscreenPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_FULSCREEN, false);
	}

	private boolean getBookmarkPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_ENABLE_BOOKMARK, true);
	}

	private boolean getDynamicButtonsPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_ENABLE_WEBVIEW_BUTTONS, false);
	}

	public boolean getDynamicButtons() {
		return dynamicButtonsEnabled;
	}

	public void refreshBookmarkData() {
		if (bookmarkAdapter != null)
			bookmarkAdapter.refreshData();
	}

	public void updateLastLine(int pIndex) {
		if (content != null)
			content.setLastYScroll(pIndex);
	}

	@SuppressLint("NewApi")
	public void OpenMenu(View view) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			invalidateOptionsMenu();
		}
		openOptionsMenu();
	}

	@Override
	public void updateProgress(String id, int current, int total, String messString) {
		Log.d(TAG, "Progress of " + id + ": " + messString + " (" + current + "/" + total + ")");
	}

	@Override
	public boolean downloadListSetup(String id, String toastText, int type) {
		Log.d(TAG, "Setup of " + id + ": " + toastText + " (type: " + type + ")");
		return false;
	}

	boolean isLoaded = false;

	public void notifyLoadComplete() {
		isLoaded = true;
	}

	private boolean getUseCustomCSS() {
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_USE_CUSTOM_CSS, false);
	}

	private float getLineSpacingPreferences() {
		return Float.parseFloat(PreferenceManager.getDefaultSharedPreferences(this).getString(Constants.PREF_LINESPACING, "150"));
	}

	private boolean getHandleExternalLinkPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_USE_INTERNAL_WEBVIEW, false);
	}

	private boolean getUseJustifiedPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_FORCE_JUSTIFIED, false);
	}

	private float getMarginPreferences() {
		return Float.parseFloat(PreferenceManager.getDefaultSharedPreferences(this).getString(Constants.PREF_MARGINS, "5"));
	}

	/**
	 * getCSSSheet() method will put all the CSS data into the HTML header. At
	 * the current moment, it reads the external data line by line then applies
	 * it directly to the header.
	 * 
	 * @return
	 */
	private String getCSSSheet() {
		StringBuilder css = new StringBuilder();

		if (getUseCustomCSS()) {
			String cssPath = PreferenceManager.getDefaultSharedPreferences(this).getString(Constants.PREF_CUSTOM_CSS_PATH, Environment.getExternalStorageDirectory().getPath() + "/custom.css");
			if (!Util.isStringNullOrEmpty(cssPath)) {
				File cssFile = new File(cssPath);
				if (cssFile.exists()) {
					// read the file
					BufferedReader br = null;
					FileReader fr = null;
					try {
						try {
							fr = new FileReader(cssFile);
							br = new BufferedReader(fr);
							String line;

							while ((line = br.readLine()) != null) {
								css.append(line);
							}

							return css.toString();

						} catch (Exception e) {
							throw e;
						} finally {
							if (fr != null)
								fr.close();
							if (br != null)
								br.close();
						}
					} catch (Exception e) {
						Log.e(TAG, "Error when reading Custom CSS: " + cssPath, e);
					}
				} else {
					Toast.makeText(getApplicationContext(), getResources().getString(R.string.css_layout_not_exist), Toast.LENGTH_SHORT).show();
				}
			}
		}

		// Default CSS start here
		int styleId = -1;
		if (getColorPreferences()) {
			styleId = R.raw.style_dark;
			// Log.d("CSS", "CSS = dark");
		} else {
			styleId = R.raw.style;
			// Log.d("CSS", "CSS = normal");
		}
		LNReaderApplication app = (LNReaderApplication) getApplication();
		css.append(app.ReadCss(styleId));

		if (getUseJustifiedPreferences()) {
			css.append("\nbody { text-align: justify !important; }\n");
		}
		css.append("\np { line-height:" + getLineSpacingPreferences() + "% !important; }\n");
		css.append("\nbody {margin: " + getMarginPreferences() + "% !important;}\n");

		return css.toString();
	}
}
