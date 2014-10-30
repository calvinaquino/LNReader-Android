package com.erakk.lnreader.activity;

import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
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
import com.erakk.lnreader.callback.IExtendedCallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.BakaTsukiWebChromeClient;
import com.erakk.lnreader.helper.BakaTsukiWebViewClient;
import com.erakk.lnreader.helper.DisplayNovelContentHtmlHelper;
import com.erakk.lnreader.helper.DisplayNovelContentTTSHelper;
import com.erakk.lnreader.helper.DisplayNovelContentUIHelper;
import com.erakk.lnreader.helper.NonLeakingWebView;
import com.erakk.lnreader.helper.OnCompleteListener;
import com.erakk.lnreader.helper.TtsHelper;
import com.erakk.lnreader.helper.Util;
import com.erakk.lnreader.model.BookModel;
import com.erakk.lnreader.model.BookmarkModel;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.NovelContentModel;
import com.erakk.lnreader.model.PageModel;
import com.erakk.lnreader.parser.CommonParser;
import com.erakk.lnreader.task.AsyncTaskResult;
import com.erakk.lnreader.task.LoadNovelContentTask;
import com.erakk.lnreader.task.LoadWacTask;

public class DisplayLightNovelContentActivity extends SherlockActivity implements IExtendedCallbackNotifier<AsyncTaskResult<?>>, OnInitListener, OnCompleteListener {
	private static final String TAG = DisplayLightNovelContentActivity.class.toString();
	public NovelContentModel content;
	private NovelCollectionModel novelDetails;
	private LoadNovelContentTask task;
	private AlertDialog tocMenu = null;
	private PageModelAdapter jumpAdapter = null;
	private BookmarkModelAdapter bookmarkAdapter = null;

	private NonLeakingWebView webView;
	private BakaTsukiWebViewClient client;
	private boolean restored;
	private AlertDialog bookmarkMenu = null;
	private boolean isFullscreen;
	private boolean isPageLoaded = false;

	private TextView loadingText;
	private ProgressBar loadingBar;

	private Menu _menu;
	public ArrayList<String> images;

	private DisplayNovelContentTTSHelper tts;
	private DisplayNovelContentUIHelper uih;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		isFullscreen = getFullscreenPreferences();
		UIHelper.ToggleFullscreen(this, isFullscreen);
		UIHelper.SetTheme(this, R.layout.activity_display_light_novel_content);
		UIHelper.SetActionBarDisplayHomeAsUp(this, true);

		uih = new DisplayNovelContentUIHelper(this);

		webView = (NonLeakingWebView) findViewById(R.id.webViewContent);

		uih.prepareCompatSearchBox(webView);
		uih.prepareTopDownButton();

		// custom link handler
		client = new BakaTsukiWebViewClient(this);
		webView.setWebViewClient(client);
		webView.setWebChromeClient(new BakaTsukiWebChromeClient(this));

		restored = false;

		loadingText = (TextView) findViewById(R.id.emptyList);
		loadingBar = (ProgressBar) findViewById(R.id.loadProgress);

		tts = new DisplayNovelContentTTSHelper(this);

		Log.d(TAG, "OnCreate Completed.");
	}

	@SuppressLint("NewApi")
	@Override
	public void onInit(int status) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			invalidateOptionsMenu();
		else
			supportInvalidateOptionsMenu();
	}

	@Override
	protected void onDestroy() {
		if (webView != null) {
			RelativeLayout rootView = (RelativeLayout) findViewById(R.id.rootView);
			rootView.removeView(webView);
			webView.removeAllViews();
			webView.destroy();
		}
		tts.unbindTtsService();

		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		super.onDestroy();
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(TAG, "onStart Completed");
	}

	@Override
	protected void onRestart() {
		super.onRestart();

		// re-enter immersive mode on restart
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && getFullscreenPreferences()) {
			UIHelper.Recreate(this);
		}

		restored = true;
		Log.d(TAG, "onRestart Completed");
	}

	@Override
	public void onResume() {
		super.onResume();

		if (isFullscreen != getFullscreenPreferences()) {
			UIHelper.Recreate(this);
		}

		// show/hide option button
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			Button btnOption = (Button) findViewById(R.id.btnMenu);
			// do not show option button for KitKat, immersive mode will show action bar
			if (getFullscreenPreferences() && Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
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
			PageModel pageModel = new PageModel(getIntent().getStringExtra(Constants.EXTRA_PAGE));
			try {
				pageModel = NovelsDao.getInstance().getPageModel(pageModel, null, false);
				if (pageModel == null) {
					Toast.makeText(this, getResources().getString(R.string.bookmark_content_load_error), Toast.LENGTH_LONG).show();
					onBackPressed();
				}
				else {
					executeTask(pageModel, false);
				}
			} catch (Exception e) {
				Log.e(TAG, "Failed to get the PageModel for content: " + getIntent().getStringExtra(Constants.EXTRA_PAGE), e);
			}
		}
		setWebViewSettings();
		if (UIHelper.isTTSEnabled(this))
			tts.setupTtsService();

		Log.d(TAG, "onResume Completed");
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		_menu = menu;

		try {
			if (content != null) {
				setPrevNextButtonState(content.getPageModel());
				_menu.findItem(R.id.menu_save_external).setVisible(false);
			}
			else {
				_menu.findItem(R.id.menu_save_external).setVisible(true);
			}

		} catch (Exception e) {
			Log.w(TAG, "Cannot get current pagemodel");
		}

		return true;
	}

	private void setPrevNextButtonState(PageModel pageModel) {
		if (_menu != null) {
			boolean isNextEnabled = false;
			boolean isPrevEnabled = false;

			try {
				PageModel prevPage = novelDetails.getPrev(pageModel.getPage(), UIHelper.getShowMissing(this),
						UIHelper.getShowRedlink(this));
				if (prevPage != null)
					isPrevEnabled = true;
			} catch (Exception ex) {
				Log.e(TAG, "Failed to get prev chapter: " + pageModel.getPage(), ex);
			}
			try {
				PageModel nextPage = novelDetails.getNext(pageModel.getPage(), UIHelper.getShowMissing(this),
						UIHelper.getShowRedlink(this));
				if (nextPage != null)
					isNextEnabled = true;
			} catch (Exception ex) {
				Log.e(TAG, "Failed to get next chapter: " + pageModel.getPage(), ex);
			}

			_menu.findItem(R.id.menu_chapter_next).setEnabled(isNextEnabled);
			_menu.findItem(R.id.menu_chapter_previous).setEnabled(isPrevEnabled);
		}
	}

	@Override
	public void onPause() {
		super.onPause();

		setLastReadState();
		if (getTtsStopOnPause()) {
			tts.stop();
		}
		Log.d(TAG, "onPause Completed");
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
			// replace the current pageModel with the saved instance
			// if have different page
			PageModel pageModel = new PageModel(restoredPage);
			pageModel = NovelsDao.getInstance().getPageModel(pageModel, null);
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

		tts.setupTTSMenu(menu);
		_menu = menu;
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
			PageModel page = null;
			if (content != null) {
				try {
					page = content.getPageModel();
				} catch (Exception e) {
					Log.e(TAG, "Cannot get current chapter.", e);
				}
			} else {
				String pageStr = getIntent().getStringExtra(Constants.EXTRA_PAGE);
				try {
					page = NovelsDao.getInstance().getExistingPageModel(new PageModel(pageStr), null);
					if (page == null) {
						// no page model, just url
						page = new PageModel(pageStr);
						page.setExternal(true);
					}
				} catch (Exception e) {
					Log.e(TAG, "Cannot get current chapter.", e);
				}
			}

			if (page != null) {
				executeTask(page, true);
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
			String currentPage = getIntent().getStringExtra(Constants.EXTRA_PAGE);
			try {
				if (novelDetails == null)
					novelDetails = NovelsDao.getInstance().getNovelDetails(content.getPageModel(), null, false);
				PageModel prev = novelDetails.getPrev(currentPage, UIHelper.getShowMissing(this), UIHelper.getShowRedlink(this));
				if (prev != null) {
					jumpTo(prev);
				} else {
					Toast.makeText(this, getResources().getString(R.string.first_available_chapter), Toast.LENGTH_SHORT).show();
				}
			} catch (Exception e) {
				Log.e(TAG, "Cannot get previous chapter.", e);
			}
			return true;
		case R.id.menu_chapter_next:

			/*
			 * Implement code to move to next chapter
			 */
			String currentPage2 = getIntent().getStringExtra(Constants.EXTRA_PAGE);
			try {
				if (novelDetails == null)
					novelDetails = NovelsDao.getInstance().getNovelDetails(content.getPageModel(), null, false);

				PageModel next = novelDetails.getNext(currentPage2, UIHelper.getShowMissing(this), UIHelper.getShowRedlink(this));
				if (next != null) {
					jumpTo(next);
				} else {
					Toast.makeText(this, getResources().getString(R.string.last_available_chapter), Toast.LENGTH_SHORT).show();
				}
			} catch (Exception e) {
				Log.e(TAG, "Cannot get next chapter.", e);
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
			return true;
		case R.id.menu_speak:
			tts.start(webView, content.getLastYScroll());
			return true;
		case R.id.menu_pause_tts:
			tts.pause();
			return true;
		case R.id.menu_save_external:
			// save based on current intent page name.
			String url = getIntent().getStringExtra(Constants.EXTRA_PAGE);
			NonLeakingWebView wv = (NonLeakingWebView) findViewById(R.id.webViewContent);
			if (!url.startsWith("http")) {
				url = getTitle().toString();
				Log.w(TAG, "Current page is not started with http, resolve from current webview url: " + url);
			}

			if (wv != null && !Util.isStringNullOrEmpty(url))
				wv.saveMyWebArchive(url);
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
			int scrollSize = UIHelper.getIntFromPreferences(Constants.PREF_SCROLL_SIZE, 5) * 100;

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

	/**
	 * Move between chapters
	 * 
	 * @param page
	 */
	public void jumpTo(PageModel page) {
		setLastReadState();
		tts.stop();
		this.getIntent().putExtra(Constants.EXTRA_PAGE, page.getPage());
		if (page.isExternal() && !getHandleExternalLinkPreferences()) {
			try {
				Uri url = Uri.parse(page.getPage());
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, url);
				startActivity(browserIntent);
			} catch (Exception ex) {
				String message = getResources().getString(R.string.error_parsing_url, page.getPage());
				Log.e(TAG, message, ex);
				Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
			}
		} else
			executeTask(page, false);
	}

	/**
	 * Build Table-of-Contents
	 * 
	 * @param referencePageModel
	 */
	private void buildTOCMenu(PageModel referencePageModel) {
		Log.d(TAG, "Trying to create TOC");
		// if(novelDetails != null) {
		try {
			BookModel book = referencePageModel.getBook(false);
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
				// if (UIHelper.IsSmallScreen(this)) {
				// resourceId = R.layout.jumpto_list_item;
				// }
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
				builder.setNegativeButton(R.string.back_to_index, new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						backToIndex();
					}
				});
				tocMenu = builder.create();
			}
		} catch (Exception e) {
			Log.e(TAG, "Cannot get current page for menu.", e);
		}
		// }
	}

	/**
	 * Back to Novel Details
	 */
	public void backToIndex() {
		String page = getIntent().getStringExtra(Constants.EXTRA_PAGE);
		PageModel pageModel = new PageModel(page);
		try {
			pageModel = NovelsDao.getInstance().getExistingPageModel(pageModel, null).getParentPageModel();

			Intent i = new Intent(this, DisplayLightNovelDetailsActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			i.putExtra(Constants.EXTRA_PAGE, pageModel.getPage());
			startActivity(i);
			finish();
		} catch (Exception e) {
			Log.e(TAG, "Failed to get parent page model", e);
		}
	}

	/**
	 * Build Bookmarks-on-Chapter menu
	 */
	public void buildBookmarkMenu() {
		if (content != null) {
			try {
				int resourceId = R.layout.bookmark_list_item;
				if (UIHelper.isSmallScreen(this)) {
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
				builder.setNegativeButton(R.string.cancel, null);
				builder.setPositiveButton(R.string.menu_show_clear_all, new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						int total = 0;
						for (BookmarkModel bookmark : content.getBookmarks()) {
							total += NovelsDao.getInstance().deleteBookmark(bookmark);
							NonLeakingWebView wv = (NonLeakingWebView) findViewById(R.id.webViewContent);
							wv.loadUrl("javascript:toogleHighlightById(" + bookmark.getpIndex() + ")");
						}
						Toast.makeText(getBaseContext(), getString(R.string.toast_show_deleted_count, total), Toast.LENGTH_SHORT).show();
					}
				});
				bookmarkMenu = builder.create();
			} catch (Exception e) {
				Log.e(TAG, "Error getting pageModel: " + e.getMessage(), e);
			}
		}
	}

	/**
	 * Update last read chapter and the position.
	 * Run async
	 */
	@SuppressWarnings("deprecation")
	public void setLastReadState() {
		NonLeakingWebView wv = (NonLeakingWebView) findViewById(R.id.webViewContent);
		final float currentScale = wv.getScale();
		final int lastY = wv.getScrollY() + wv.getBottom();
		final int contentHeight = wv.getContentHeight();
		new Thread(new Runnable() {

			@Override
			public void run() {
				// save last position and zoom
				if (content != null) {
					content.setLastZoom(currentScale);
					try {
						content = NovelsDao.getInstance().updateNovelContent(content, false);
					} catch (Exception ex) {
						Log.e(TAG, "Error when saving state: " + ex.getMessage(), ex);
					}

					// check if complete read.
					if (contentHeight <= lastY) {
						if (content != null) {
							try {
								PageModel page = content.getPageModel();
								if (!page.getPage().endsWith("&action=edit&redlink=1")) {
									page.setFinishedRead(true);
									page = NovelsDao.getInstance().updatePageModel(page);
								}
							} catch (Exception ex) {
								Log.e(TAG, "Error updating PageModel for Content: " + content.getPage(), ex);
							}
						}
					}
					Log.d(TAG, "Update Content:X=" + content.getLastXScroll() + ":Y=" + content.getLastYScroll() + ":Z=" + content.getLastZoom());
				}

				// save for jump to last read.
				SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(LNReaderApplication.getInstance());
				SharedPreferences.Editor editor = sharedPrefs.edit();
				String lastPage = "";
				if (content != null) {
					lastPage = content.getPage();
				} else {
					lastPage = getIntent().getStringExtra(Constants.EXTRA_PAGE);
				}
				editor.putString(Constants.PREF_LAST_READ, lastPage);
				editor.commit();
				Log.i(TAG, "Last Read State Update complete: " + lastPage);
			}
		}).start();
	}

	/**
	 * Load chapter from DB
	 * 
	 * @param pageModel
	 * @param refresh
	 */
	@SuppressLint("NewApi")
	private void executeTask(PageModel pageModel, boolean refresh) {
		NonLeakingWebView webView = (NonLeakingWebView) findViewById(R.id.webViewContent);
		if (pageModel.isExternal()) {
			loadExternalUrl(pageModel, refresh);
		} else {
			isPageLoaded = false;
			task = new LoadNovelContentTask(pageModel, refresh, this);
			String key = TAG + ":" + pageModel.getPage();
			boolean isAdded = LNReaderApplication.getInstance().addTask(key, task);
			if (isAdded) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
					task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				else
					task.execute();
			} else {
				if (UIHelper.getColorPreferences(this))
					webView.loadData("<p style='background: black; color: white;'>" + getResources().getString(R.string.background_task_load) + "</p>", "text/html", "utf-8");
				else
					webView.loadData("<p style='background: white; color: black;'>" + getResources().getString(R.string.background_task_load) + "</p>", "text/html", "utf-8");
				LoadNovelContentTask tempTask = (LoadNovelContentTask) LNReaderApplication.getInstance().getTask(key);
				if (tempTask != null) {
					task = tempTask;
					task.owner = this;
				}
				toggleProgressBar(true);
			}
		}
		setPrevNextButtonState(pageModel);
	}

	/**
	 * Load chapter for external url (not hosted in Baka-Tsuki).
	 * Used local cache if available (wac/mht).
	 * 
	 * @param pageModel
	 * @param refresh
	 */
	public void loadExternalUrl(PageModel pageModel, boolean refresh) {
		try {
			// check if .wac available
			String url = pageModel.getPage();
			String wacName = Util.getSavedWacName(url);
			final NonLeakingWebView wv = (NonLeakingWebView) findViewById(R.id.webViewContent);
			if (!Util.isStringNullOrEmpty(wacName) && !refresh) {
				client.setExternalNeedSave(false);
				String[] urlParts = url.split("#", 2);
				if (urlParts.length == 2) {
					executeLoadWacTask(wacName, urlParts[1]);
				}
				else
					executeLoadWacTask(wacName, "");
			}
			else {
				if (refresh) {
					Toast.makeText(this, "Refreshing WAC: " + wacName, Toast.LENGTH_SHORT).show();
					Log.i(TAG, "Refreshing WAC: " + wacName);
				} else
				{
					Log.w(TAG, "WAC not available: " + wacName);
				}

				client.setExternalNeedSave(true);

				setWebViewSettings();
				wv.loadUrl(url);
			}
			setChapterTitle(pageModel);
			buildTOCMenu(pageModel);
			content = null;
		} catch (Exception ex) {
			Log.e(TAG, "Cannot load external content: " + pageModel.getPage(), ex);
		}
	}

	/**
	 * Load saved external chapter from wac/mht
	 * 
	 * @param wacName
	 */
	@SuppressLint({ "InlinedApi", "NewApi" })
	private void executeLoadWacTask(String wacName, String anchorLink) {
		NonLeakingWebView webView = (NonLeakingWebView) findViewById(R.id.webViewContent);
		LoadWacTask task = new LoadWacTask(this, webView, wacName, client, anchorLink);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		else
			task.execute();
	}

	/**
	 * Setup the chapter from DB
	 * Including JS for Bookmark highlighting, last read position, and CSS
	 * 
	 * @param loadedContent
	 */
	@SuppressLint("NewApi")
	public void setContent(NovelContentModel loadedContent) {
		this.content = loadedContent;
		try {
			PageModel pageModel = content.getPageModel();

			if (content.getLastUpdate().getTime() < pageModel.getLastUpdate().getTime())
				Toast.makeText(this, getResources().getString(R.string.content_may_updated, content.getLastUpdate().toString(), pageModel.getLastUpdate().toString()), Toast.LENGTH_LONG).show();

			// load the contents here
			final NonLeakingWebView wv = (NonLeakingWebView) findViewById(R.id.webViewContent);
			setWebViewSettings();

			int lastPos = content.getLastYScroll();
			int pIndex = getIntent().getIntExtra(Constants.EXTRA_P_INDEX, -1);
			if (pIndex > 0)
				lastPos = pIndex;

			StringBuilder html = new StringBuilder();
			html.append("<html><head>");
			html.append(DisplayNovelContentHtmlHelper.getCSSSheet());
			html.append("<meta name='viewport' content='width=device-width, minimum-scale=0.5, maximum-scale=5' id='viewport-meta'/>");
			html.append(DisplayNovelContentHtmlHelper.prepareJavaScript(lastPos, content.getBookmarks(), getBookmarkPreferences()));
			html.append("</head><body onclick='toogleHighlight(this, event);' onload='setup();'>");
			html.append(content.getContent());
			html.append("</body></html>");

			wv.loadDataWithBaseURL(UIHelper.getBaseUrl(this), html.toString(), "text/html", "utf-8", "");
			client.currentUrl = pageModel.getPage();
			if (content.getLastZoom() > 0) {
				wv.setInitialScale((int) (content.getLastZoom() * 100));
			} else {
				wv.setInitialScale(100);
			}

			setChapterTitle(pageModel);
			Log.d(TAG, "Load Content: " + content.getLastXScroll() + " " + content.getLastYScroll() + " " + content.getLastZoom());

			buildTOCMenu(pageModel);
			buildBookmarkMenu();

			invalidateOptionsMenu();

			Log.d(TAG, "Loaded: " + content.getPage());
		} catch (Exception e) {
			Log.e(TAG, "Cannot load content.", e);
		}
	}

	/**
	 * Set activity title to current chapter title.
	 * 
	 * @param pageModel
	 */
	private void setChapterTitle(PageModel pageModel) {
		String title = pageModel.getPage();
		try {
			if (pageModel.getParent() != null) {
				Log.d(TAG, "Parent Page: " + pageModel.getParent());
				novelDetails = NovelsDao.getInstance().getNovelDetails(pageModel.getParentPageModel(), null, false);
				String volume = pageModel.getParent().replace(pageModel.getParentPageModel().getPage() + Constants.NOVEL_BOOK_DIVIDER, "");
				title = pageModel.getTitle() + " (" + volume + ")";
			}
		} catch (Exception ex) {
			Log.e(TAG, "Error when setting title: " + ex.getMessage(), ex);
		}
		setTitle(title);
	}

	/**
	 * Setup webview
	 */
	@SuppressLint({ "NewApi", "SetJavaScriptEnabled" })
	private void setWebViewSettings() {
		NonLeakingWebView wv = (NonLeakingWebView) findViewById(R.id.webViewContent);

		wv.getSettings().setAllowFileAccess(true);

		wv.getSettings().setSupportZoom(UIHelper.getZoomPreferences(this));
		wv.getSettings().setBuiltInZoomControls(UIHelper.getZoomPreferences(this));

		wv.setDisplayZoomControl(UIHelper.getZoomControlPreferences(this));

		wv.getSettings().setLoadWithOverviewMode(true);
		// wv.getSettings().setUseWideViewPort(true);
		wv.getSettings().setLoadsImagesAutomatically(getShowImagesPreferences());
		if (UIHelper.getColorPreferences(this))
			wv.setBackgroundColor(0);
		wv.getSettings().setJavaScriptEnabled(true);

		if (isPageLoaded)
			wv.loadUrl("javascript:toogleEnableBookmark(" + getBookmarkPreferences() + ")");
	}

	/**
	 * AsyncTask complete handler for:
	 * - Novel Content saved in DB
	 * - External Chapter
	 */
	@Override
	public void onCompleteCallback(ICallbackEventData message, AsyncTaskResult<?> result) {
		Exception e = result.getError();
		if (e == null) {
			if (result.getResultType() == NovelContentModel.class) {
				NovelContentModel loadedContent = (NovelContentModel) result.getResult();
				setContent(loadedContent);
				Document imageDoc = Jsoup.parse(loadedContent.getContent());
				images = CommonParser.parseImagesFromContentPage(imageDoc);
			}
			else if (result.getResultType() == Boolean.class) {
				// Load WAC
				Toast.makeText(this, message.getMessage(), Toast.LENGTH_SHORT).show();
				boolean res = (Boolean) result.getResult();
				if (!res) {
					PageModel p = new PageModel(getIntent().getStringExtra(Constants.EXTRA_PAGE));
					loadExternalUrl(p, true);
				}

			}
			else {
				Log.w(TAG, "Unexpected result: " + result.getResultType().getName());
			}
		} else {
			Log.e(TAG, "Error when loading novel content: " + e.getMessage(), e);
			Toast.makeText(this, e.getClass().toString() + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
		}
		toggleProgressBar(false);

	}

	/**
	 * Update Bookmark-on-Chapter data upon receiving event from webview client
	 */
	public void refreshBookmarkData() {
		if (bookmarkAdapter != null)
			bookmarkAdapter.refreshData();
	}

	/**
	 * Used by ChromeClient to receive js update event for y-scrolling
	 * 
	 * @param pIndex
	 */
	public void updateLastLine(int pIndex) {
		if (content != null)
			content.setLastYScroll(pIndex);
	}

	/**
	 * Used for floating button on fullscreen mode to open the menu.
	 * 
	 * @param view
	 */
	@SuppressLint("NewApi")
	private void openMenu(View view) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			invalidateOptionsMenu();
		}
		openOptionsMenu();
	}

	@Override
	public boolean downloadListSetup(String id, String toastText, int type, boolean hasError) {
		Log.d(TAG, "Setup of " + id + ": " + toastText + " (type: " + type + ")" + "hasError: " + hasError);
		return false;
	}

	/**
	 * Used to move to the last read position upon receiving load complete event from webview client
	 */
	public void notifyLoadComplete() {
		isPageLoaded = true;
		if (webView != null && content != null) {

			// move to last read paragraph, delay after webview load the pages.
			webView.postDelayed(new Runnable() {
				@Override
				public void run() {
					int y = getIntent().getIntExtra(Constants.EXTRA_P_INDEX, content.getLastYScroll());
					Log.d(TAG, "notifyLoadComplete(): Move to the saved pos: " + y);
					webView.loadUrl("javascript:goToParagraph(" + y + ")");
				}
			}, UIHelper.getIntFromPreferences(Constants.PREF_KITKAT_WEBVIEW_FIX_DELAY, 500) + 100);
		}
	}

	/* PREFERENCES */
	private boolean getShowImagesPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_SHOW_IMAGE, true);
	}

	private boolean getFullscreenPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_FULSCREEN, false);
	}

	private boolean getBookmarkPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_ENABLE_BOOKMARK, true);
	}

	private boolean getHandleExternalLinkPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_USE_INTERNAL_WEBVIEW, false);
	}

	/* Progress bar related */
	@Override
	public void onProgressCallback(ICallbackEventData message) {
		toggleProgressBar(true);
		loadingText.setText(message.getMessage());
	}

	public void toggleProgressBar(boolean show) {
		if (webView == null || loadingBar == null || loadingText == null)
			return;
		synchronized (this) {
			if (show) {
				loadingText.setVisibility(TextView.VISIBLE);
				loadingBar.setVisibility(ProgressBar.VISIBLE);
				webView.setVisibility(ListView.GONE);
			} else {
				loadingText.setVisibility(TextView.GONE);
				loadingBar.setVisibility(ProgressBar.GONE);
				webView.setVisibility(ListView.VISIBLE);
			}
		}
	}

	/* Search box */
	@SuppressWarnings("deprecation")
	private void showSearchBox() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			webView.showFindDialog("", true);
		else {
			RelativeLayout searchBox = (RelativeLayout) findViewById(R.id.searchBox);
			searchBox.setVisibility(View.VISIBLE);
		}
	}

	public void searchNext(View view) {
		webView.findNext(true);
	}

	public void searchPrev(View view) {
		webView.findNext(false);
	}

	public void closeSearchBox(View view) {
		uih.closeSearchBox(webView);
	}

	/* Top-Down button */
	public void toggleTopButton(boolean enable) {
		uih.toggleTopButton(enable);
	}

	public void toggleBottomButton(boolean enable) {
		uih.toggleBottomButton(enable);
	}

	public void goTop(View view) {
		webView.pageUp(true);
	}

	public void goBottom(View view) {
		webView.pageDown(true);
	}

	/* TTS */
	private boolean getTtsStopOnPause() {
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_TTS_TTS_STOP_ON_LOST_FOCUS, true);
	}

	@Override
	public void onComplete(Object i, Class<?> source) {
		Log.d(TAG, "Data: " + i + " from: " + source.getCanonicalName());
		if (i != null && source == TtsHelper.class) {
			tts.autoScroll(webView, i.toString());
		}
	}

	public void sendHtmlForSpeak(String html) {
		tts.start(html, content.getLastYScroll());
	}
}
