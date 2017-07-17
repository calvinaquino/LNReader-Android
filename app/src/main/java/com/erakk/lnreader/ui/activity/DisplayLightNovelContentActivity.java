package com.erakk.lnreader.ui.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import com.erakk.lnreader.model.NovelContentUserModel;
import com.erakk.lnreader.model.PageModel;
import com.erakk.lnreader.task.AsyncTaskResult;
import com.erakk.lnreader.task.LoadNovelContentTask;
import com.erakk.lnreader.task.LoadWacTask;

import java.io.File;
import java.util.ArrayList;

public class DisplayLightNovelContentActivity extends BaseActivity implements IExtendedCallbackNotifier<AsyncTaskResult<?>>, OnInitListener, OnCompleteListener {
    private static final String TAG = DisplayLightNovelContentActivity.class.toString();
    public NovelContentModel content;
    public NovelContentUserModel contentUserData;
    private PageModel currPageModel = null;

    private NovelCollectionModel novelDetails;
    private LoadNovelContentTask task;

    private boolean restored;
    private boolean isFullscreen;
    private boolean isPageLoaded = false;

    private AlertDialog bookmarkMenu = null;
    private AlertDialog tocMenu = null;

    private Menu _menu;

    // region private helpers, init in onCreate()
    private DisplayNovelContentTTSHelper _tts;
    private DisplayNovelContentUIHelper _uih;
    // endregion


    public void updateCurrentPageModel(PageModel reference, NovelContentModel refContent, NovelContentUserModel refContentUser) {
        String pageModelStr = "";
        String contentPageStr = "";
        String contentUserPageStr = "";

        if (reference != null) {
            this.currPageModel = reference;
            pageModelStr = reference.getPage();
        } else if (currPageModel != null) {
            pageModelStr = currPageModel.getPage();
        }

        if (refContent != null) {
            this.content = refContent;
            contentPageStr = refContent.getPage();
        } else if (content != null) {
            contentPageStr = content.getPage();
        }

        if (refContentUser != null) {
            this.contentUserData = refContentUser;
            contentUserPageStr = refContentUser.getPage();
        } else if (contentUserData != null) {
            contentUserPageStr = contentUserData.getPage();
        }

        final String message = String.format("PageModel: %s\nContent Page: %s\nContent User Page: %s", pageModelStr, contentPageStr, contentUserPageStr);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView txtDebug = (TextView) findViewById(R.id.txtDebug);
                try {
                    PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                    if (!pInfo.versionName.contains("beta"))
                        txtDebug.setMaxHeight(0);
                    txtDebug.setText(message);
                } catch (Exception ex) {
                    txtDebug.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        restored = false;
        setContentView(R.layout.activity_display_light_novel_content);

        // custom link handler
        NonLeakingWebView webView = (NonLeakingWebView) findViewById(R.id.webViewContent);
        BakaTsukiWebViewClient client = new BakaTsukiWebViewClient(this);
        webView.setWebViewClient(client);
        BakaTsukiWebChromeClient chromeClient = new BakaTsukiWebChromeClient(this);
        webView.setWebChromeClient(chromeClient);

        // UI Helper
        _uih = new DisplayNovelContentUIHelper(this);
        _uih.prepareCompatSearchBox(webView);
        _uih.prepareTopDownButton();

        isFullscreen = getFullscreenPreferences();
        _uih.prepareFullscreenHandler(webView);
        _uih.toggleFullscreen(isFullscreen);

        _tts = new DisplayNovelContentTTSHelper(this);

        Log.d(TAG, "OnCreate Completed.");
    }

    @Override
    protected void onDestroy() {
        NonLeakingWebView webView = (NonLeakingWebView) findViewById(R.id.webViewContent);
        if (webView != null) {
            RelativeLayout rootView = (RelativeLayout) findViewById(R.id.rootView);
            rootView.removeView(webView);
            webView.removeAllViews();
            webView.destroy();
        }
        _tts.unbindTtsService();

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

//        // re-enter immersive mode on restart
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && getFullscreenPreferences()) {
//            UIHelper.Recreate(this);
//        }

        restored = true;
        Log.d(TAG, "onRestart Completed");
    }

    @Override
    public void onResume() {
        super.onResume();

        // compare the settings from OnCreate and after user resume.
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
            String page = getIntent().getStringExtra(Constants.EXTRA_PAGE);
            PageModel pageModel = new PageModel(page);
            try {
                pageModel = NovelsDao.getInstance().getExistingPageModel(pageModel, null);
                if (pageModel == null) {
                    Toast.makeText(this, getResources().getString(R.string.bookmark_content_load_error), Toast.LENGTH_LONG).show();
                    Log.w(TAG, "Missing page: " + page);
                    onBackPressed();
                } else {
                    updateCurrentPageModel(pageModel, null, null);
                    this.getIntent().putExtra(Constants.EXTRA_PAGE_IS_EXTERNAL, currPageModel.isExternal());
                    executeTask(pageModel, false);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to get the PageModel for content: " + getIntent().getStringExtra(Constants.EXTRA_PAGE), e);
            }
        }
        setWebViewSettings();
        if (UIHelper.isTTSEnabled(this))
            _tts.setupTtsService();

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
                _menu.findItem(R.id.menu_browser_back).setVisible(false);
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                    _menu.findItem(R.id.menu_save_external).setVisible(true);
                _menu.findItem(R.id.menu_browser_back).setVisible(true);

                NonLeakingWebView webView = (NonLeakingWebView) findViewById(R.id.webViewContent);
                if (webView != null)
                    _menu.findItem(R.id.menu_browser_back).setEnabled(webView.canGoBack());
            }
        } catch (Exception e) {
            Log.w(TAG, "Cannot get current page model");
        }

        return true;
    }

    @Override
    public void onPause() {
        super.onPause();

        setLastReadState();
        if (getTtsStopOnPause()) {
            _tts.stop();
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

            if (currPageModel == null)
                savedInstanceState.putBoolean(Constants.EXTRA_PAGE_IS_EXTERNAL, false);
            else
                savedInstanceState.putBoolean(Constants.EXTRA_PAGE_IS_EXTERNAL, currPageModel.isExternal());

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
            updateCurrentPageModel(pageModel, null, null);
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
        getMenuInflater().inflate(R.menu.activity_display_light_novel_content, menu);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (getFullscreenPreferences()) {
                menu.findItem(R.id.menu_chapter_next).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                menu.findItem(R.id.menu_chapter_previous).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            }
        }

        // disable invert option if using custom color or css
        MenuItem invertColor = menu.findItem(R.id.invert_colors);
        if (invertColor != null) {
            if (DisplayNovelContentHtmlHelper.getUseCustomCSS(this) || UIHelper.getCssUseCustomColorPreferences(this)) {
                invertColor.setEnabled(false);
            } else {
                invertColor.setEnabled(true);
            }
        }

        _tts.setupTTSMenu(menu);
        _menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        NonLeakingWebView webView = (NonLeakingWebView) findViewById(R.id.webViewContent);
        switch (item.getItemId()) {
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
            case R.id.menu_speak:
                _tts.start(webView, contentUserData.getLastYScroll());
                return true;
            case R.id.menu_pause_tts:
                _tts.pause();
                return true;
            case R.id.menu_save_external:
                // save based on current intent page name.
                String url = getIntent().getStringExtra(Constants.EXTRA_PAGE);
                if (!url.startsWith("http")) {
                    url = getTitle().toString();
                    Log.w(TAG, "Current page is not started with http, resolve from current webView url: " + url);
                }
                if (webView != null && !Util.isStringNullOrEmpty(url))
                    webView.saveMyWebArchive(url);
                return true;
            case R.id.menu_browser_back:
                if (webView != null && webView.canGoBack()) {
                    // only good for android 4.4++
                    webView.goBack();
                }
                return true;
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_fullscreen:
                isFullscreen = !isFullscreen;
                SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(this).edit();
                edit.putBoolean(Constants.PREF_FULSCREEN, isFullscreen);
                edit.commit();
                _uih.toggleFullscreen(isFullscreen);

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

    // region Volume key scrolling

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean useVolumeRocker = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_USE_VOLUME_FOR_SCROLL, false);
        if (useVolumeRocker) {
            int scrollSize = UIHelper.getIntFromPreferences(Constants.PREF_SCROLL_SIZE, 5) * 100;

            boolean invertScroll = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_INVERT_SCROLL, false);
            if (invertScroll)
                scrollSize = scrollSize * -1;

            NonLeakingWebView webView = (NonLeakingWebView) findViewById(R.id.webViewContent);
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

    // endregion

    /**
     * Update last read chapter and the position.
     * Run async
     */
    @SuppressWarnings("deprecation")
    public void setLastReadState() {
        final NonLeakingWebView wv = (NonLeakingWebView) findViewById(R.id.webViewContent);

        // get the values from UI Thread due to Android restriction.
        final float currentScale = wv.getScale();
        final int lastY = wv.getScrollY() + wv.getBottom();
        final int contentHeight = wv.getContentHeight();

        // bug handling for Issue#213
        // for some reason, the setLastReadState is called twice in landscape mode.
        if (contentHeight == 0)
            return;

        new Thread(new Runnable() {

            @Override
            public void run() {
                if (currPageModel == null)
                    return;

                if (contentUserData == null) {
                    contentUserData = new NovelContentUserModel();
                    contentUserData.setPage(currPageModel.getPage());
                    updateCurrentPageModel(null, null, contentUserData);
                }
                updateCurrentPageModel(null, null, contentUserData);

                checkLastYAndScale();
                checkIsReadComplete();

                try {
                    NovelsDao.getInstance().updateNovelContentUserModel(contentUserData, null);
                    Log.d(TAG, "Update Content:X=" + contentUserData.getLastXScroll() + ":Y=" + contentUserData.getLastYScroll() + ":Z=" + contentUserData.getLastZoom());
                } catch (Exception ex) {
                    Log.e(TAG, ex.getMessage(), ex);
                }

                String lastPage = saveLastReadChapter();

                Log.i(TAG, "Last Read State Update complete: " + lastPage);
            }

            private void checkLastYAndScale() {
                contentUserData.setLastZoom(currentScale);

                // save zoom level, position is updated from updateLastLine()
                // for external page, use the px
                if (content == null) {
                    contentUserData.setLastYScroll(wv.getScrollY());
                }
            }

            private String saveLastReadChapter() {
                // save for jump to last read.
                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(LNReaderApplication.getInstance());
                SharedPreferences.Editor editor = sharedPrefs.edit();
                String lastPage;

                if (currPageModel.isExternal()) {
                    lastPage = currPageModel.getPage();
                } else if (content != null) {
                    lastPage = content.getPage();
                } else {
                    lastPage = getIntent().getStringExtra(Constants.EXTRA_PAGE);
                }
                editor.putString(Constants.PREF_LAST_READ, lastPage);
                editor.commit();
                return lastPage;
            }

            private void checkIsReadComplete() {
                // pixel, round to the nearest
                double isReadThreshold = (contentHeight * currentScale) - currentScale;

                try {

                    if (isReadThreshold <= lastY && !currPageModel.getPage().endsWith("&action=edit&redlink=1")) {
                        currPageModel.setFinishedRead(true);
                    }
                    Log.i(TAG, "Complete Read PageModel for Content: " + currPageModel.getPage() + " check value=" + isReadThreshold + " <= YPix=" + lastY + " ==> " + currPageModel.isFinishedRead());
                    NovelsDao.getInstance().updatePageModel(currPageModel);
                } catch (Exception ex) {
                    Log.e(TAG, "Error updating PageModel for Content: " + currPageModel.getPage(), ex);
                }
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
                if (getColorPreferences(this))
                    webView.loadData("<p style='background: black; color: white;'>" + getResources().getString(R.string.background_task_load) + "</p>", "text/html", "utf-8");
                else
                    webView.loadData("<p style='background: white; color: black;'>" + getResources().getString(R.string.background_task_load) + "</p>", "text/html", "utf-8");
                LoadNovelContentTask tempTask = (LoadNovelContentTask) LNReaderApplication.getInstance().getTask(key);
                if (tempTask != null) {
                    task = tempTask;
                    task.owner = this;
                }
                toggleProgressBar(true, "");
            }
        }
        setPrevNextButtonState(pageModel);
        updateCurrentPageModel(pageModel, null, null);
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
            final BakaTsukiWebViewClient client = (BakaTsukiWebViewClient) wv.getWebViewClient();

            // available
            if (!Util.isStringNullOrEmpty(wacName) && !refresh) {
                client.setExternalNeedSave(false);
                String[] urlParts = url.split("#", 2);
                if (urlParts.length == 2) {
                    executeLoadWacTask(wacName, urlParts[1], url);
                } else
                    executeLoadWacTask(wacName, "", url);
            } else {

                // delete if refresh
                if (refresh) {
                    Toast.makeText(this, "Refreshing WAC: " + wacName, Toast.LENGTH_SHORT).show();
                    // delete the WAC file
                    File f = new File(wacName);
                    if (f.exists())
                        f.delete();

                    Log.i(TAG, "Refreshing WAC: " + wacName);
                } else {
                    Log.w(TAG, "WAC not available: " + wacName);
                }

                client.setExternalNeedSave(true);

                setWebViewSettings();
                wv.loadUrl(url);

                Intent currIntent = this.getIntent();
                currIntent.putExtra(Constants.EXTRA_PAGE, Util.SanitizeBaseUrl(url, false));
                currIntent.putExtra(Constants.EXTRA_PAGE_IS_EXTERNAL, true);

                // sanitize here after redirect
                pageModel.setPage(Util.SanitizeBaseUrl(url, false));
            }
            setChapterTitle(pageModel);
            buildTOCMenu(pageModel);
            content = null;
            contentUserData = getContentUserData(pageModel.getPage());

            updateCurrentPageModel(pageModel, content, null);
        } catch (Exception ex) {
            Log.e(TAG, "Cannot load external content: " + pageModel.getPage(), ex);
        }
    }

    /**
     * Load saved external chapter from wac/mht
     *
     * @param wacName
     */
    @SuppressLint({"InlinedApi", "NewApi"})
    private void executeLoadWacTask(String wacName, String anchorLink, String historyUrl) {
        final NonLeakingWebView webView = (NonLeakingWebView) findViewById(R.id.webViewContent);
        final BakaTsukiWebViewClient client = (BakaTsukiWebViewClient) webView.getWebViewClient();
        LoadWacTask task = new LoadWacTask(this, webView, wacName, client, anchorLink, historyUrl);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        else
            task.execute();
    }

    /**
     * Setup the chapter from DB, Internal page only
     * Including JS for Bookmark highlighting, last read position, and CSS
     *
     * @param loadedContent
     */
    @SuppressLint("NewApi")
    private void setContent(NovelContentModel loadedContent) {
        this.content = loadedContent;
        try {
            PageModel pageModel = content.getPageModel();
            contentUserData = getContentUserData(content.getPage());

            updateCurrentPageModel(pageModel, content, contentUserData);

            if (content.getLastUpdate().getTime() < pageModel.getLastUpdate().getTime())
                Toast.makeText(this, getResources().getString(R.string.content_may_updated, content.getLastUpdate().toString(), pageModel.getLastUpdate().toString()), Toast.LENGTH_LONG).show();

            // load the contents here
            final NonLeakingWebView wv = (NonLeakingWebView) findViewById(R.id.webViewContent);
            setWebViewSettings();

            int lastPos = contentUserData.getLastYScroll();
            int pIndex = getIntent().getIntExtra(Constants.EXTRA_P_INDEX, -1);
            if (pIndex > 0)
                lastPos = pIndex;

            if (contentUserData.getLastZoom() > 0) {
                wv.setInitialScale((int) (contentUserData.getLastZoom() * 100));
            } else {
                wv.setInitialScale(this.getResources().getInteger(R.integer.default_zoom));
            }

            StringBuilder html = new StringBuilder();
            html.append("<html><head>");
            html.append(DisplayNovelContentHtmlHelper.getCSSSheet());
            html.append(DisplayNovelContentHtmlHelper.getViewPortMeta());
            html.append(DisplayNovelContentHtmlHelper.prepareJavaScript(lastPos, content.getBookmarks(), getBookmarkPreferences()));
            html.append("</head><body onclick='toogleHighlight(this, event);' onload='setup();'>");
            html.append(content.getContent());
            html.append("</body></html>");

            wv.loadDataWithBaseURL(UIHelper.getBaseUrl(this), html.toString(), "text/html", "utf-8", NonLeakingWebView.PREFIX_PAGEMODEL + content.getPage());
            setChapterTitle(pageModel);
            Log.d(TAG, "Load Content:X=" + contentUserData.getLastXScroll() + ":Y=" + contentUserData.getLastYScroll() + ":Z=" + contentUserData.getLastZoom());

            buildTOCMenu(pageModel);
            buildBookmarkMenu();

            invalidateOptionsMenu();

            Log.d(TAG, "Loaded: " + content.getPage());

            Intent currIntent = this.getIntent();
            currIntent.putExtra(Constants.EXTRA_PAGE, content.getPage());
            currIntent.putExtra(Constants.EXTRA_PAGE_IS_EXTERNAL, false);

        } catch (Exception e) {
            Log.e(TAG, "Cannot load content.", e);
        }
    }

    // region webView related method

    /**
     * Setup webView
     */
    @SuppressLint({"NewApi", "SetJavaScriptEnabled"})
    private void setWebViewSettings() {
        NonLeakingWebView wv = (NonLeakingWebView) findViewById(R.id.webViewContent);

        wv.getSettings().setAllowFileAccess(true);

        wv.getSettings().setSupportZoom(UIHelper.getZoomPreferences(this));
        wv.getSettings().setBuiltInZoomControls(UIHelper.getZoomPreferences(this));

        wv.setDisplayZoomControl(UIHelper.getZoomControlPreferences(this));

        wv.getSettings().setLoadWithOverviewMode(true);
        // wv.getSettings().setUseWideViewPort(true);
        wv.getSettings().setLoadsImagesAutomatically(getShowImagesPreferences());
        if (getColorPreferences(this))
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
                synchronized (this) {
                    try {
                        loadedContent.refreshPageModel(); // ensuring pageModel to be refreshed
                        setContent(loadedContent);
                    } catch (Exception e1) {
                        Log.e(TAG, "Cannot load content.", e);
                    }
                }
            } else if (result.getResultType() == Boolean.class) {
                // Load WAC
                Toast.makeText(this, message.getMessage(), Toast.LENGTH_SHORT).show();
                boolean res = (Boolean) result.getResult();
                if (!res) {
                    String page = getIntent().getStringExtra(Constants.EXTRA_PAGE);
                    PageModel p = new PageModel(page);
                    try {
                        contentUserData = getContentUserData(page);
                        updateCurrentPageModel(null, null, contentUserData);
                    } catch (Exception ex) {
                        Log.e(TAG, ex.getMessage(), ex);
                    }
                    loadExternalUrl(p, true);
                } else {
                    try {
                        contentUserData = getContentUserData(currPageModel.getPage());
                        updateCurrentPageModel(null, null, contentUserData);
                        final NonLeakingWebView webView = (NonLeakingWebView) findViewById(R.id.webViewContent);
                        final BakaTsukiWebChromeClient chromeClient = (BakaTsukiWebChromeClient) webView.getWebChromeClient();
                        chromeClient.setScrollY(contentUserData.getLastYScroll());
                    } catch (Exception ex) {
                        Log.e(TAG, ex.getMessage(), ex);
                    }
                }

            } else {
                Log.w(TAG, "Unexpected result: " + result.getResultType().getName());
            }
        } else {
            Log.e(TAG, "Error when loading novel content: " + e.getMessage(), e);
            Toast.makeText(this, e.getClass().toString() + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        toggleProgressBar(false, null);

    }

    private NovelContentUserModel getContentUserData(String page) throws Exception {
        NovelContentUserModel temp = NovelsDao.getInstance().getNovelContentUserModel(page, null);
        if (temp == null) {
            temp = new NovelContentUserModel();
            temp.setPage(page);
        }
        return temp;
    }

    /**
     * Used by ChromeClient to receive js update event for y-scrolling
     *
     * @param pIndex
     */
    public void updateLastLine(int pIndex) {
        try {
            if (contentUserData == null)
                contentUserData = getContentUserData(currPageModel.getPage());
            contentUserData.setLastYScroll(pIndex);
            updateCurrentPageModel(null, null, contentUserData);
        } catch (Exception ex) {
            Log.e(TAG, "updateLastLine(): " + ex.getMessage(), ex);
        }
    }

    /**
     * Used to move to the last read position upon receiving load complete event from webView client
     */
    public void notifyLoadComplete() {
        NonLeakingWebView webView = (NonLeakingWebView) findViewById(R.id.webViewContent);
        isPageLoaded = true;
        if (webView != null && content != null) {
            final NonLeakingWebView _webView = webView;
            // move to last read paragraph, delay after webView load the pages.
            _webView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        int y = getIntent().getIntExtra(Constants.EXTRA_P_INDEX, contentUserData.getLastYScroll());
                        Log.d(TAG, "notifyLoadComplete(): Move to the saved pos: " + y);
                        _webView.loadUrl("javascript:goToParagraph(" + y + ")");
                    } catch (NullPointerException ex) {
                        Log.i(TAG, "Failed to load the content");
                    }
                }
            }, UIHelper.getIntFromPreferences(Constants.PREF_KITKAT_WEBVIEW_FIX_DELAY, 500) + 100);
        }
    }

    // endregion

    @Override
    public boolean downloadListSetup(String id, String toastText, int type, boolean hasError) {
        Log.d(TAG, "Setup of " + id + ": " + toastText + " (type: " + type + ")" + "hasError: " + hasError);
        return false;
    }

    // region PREFERENCES
    private boolean getShowImagesPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_SHOW_IMAGE, true);
    }

    public boolean getFullscreenPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_FULSCREEN, false);
    }

    private boolean getBookmarkPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_ENABLE_BOOKMARK, true);
    }

    private boolean getHandleExternalLinkPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_USE_INTERNAL_WEBVIEW, false);
    }
    // endregion

    // region Progress bar related
    @Override
    public void onProgressCallback(ICallbackEventData message) {
        toggleProgressBar(true, message.getMessage());
    }

    public void toggleProgressBar(boolean show, String message) {
        NonLeakingWebView webView = (NonLeakingWebView) findViewById(R.id.webViewContent);
        TextView loadingText = (TextView) findViewById(R.id.emptyList);
        ProgressBar loadingBar = (ProgressBar) findViewById(R.id.loadProgress);
        if (webView == null || loadingBar == null || loadingText == null)
            return;
        synchronized (this) {
            if (show) {
                loadingText.setVisibility(TextView.VISIBLE);
                loadingText.setText(message);
                loadingBar.setVisibility(ProgressBar.VISIBLE);
                webView.setVisibility(ListView.GONE);
            } else {
                loadingText.setVisibility(TextView.GONE);
                loadingBar.setVisibility(ProgressBar.GONE);
                webView.setVisibility(ListView.VISIBLE);
            }
        }
    }
    // endregion

    // region Search box
    @SuppressWarnings("deprecation")
    private void showSearchBox() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            NonLeakingWebView webView = (NonLeakingWebView) findViewById(R.id.webViewContent);
            webView.showFindDialog("", true);
        } else {
            RelativeLayout searchBox = (RelativeLayout) findViewById(R.id.searchBox);
            searchBox.setVisibility(View.VISIBLE);
        }
    }

    public void searchNext(View view) {
        NonLeakingWebView webView = (NonLeakingWebView) findViewById(R.id.webViewContent);
        webView.findNext(true);
    }

    public void searchPrev(View view) {
        NonLeakingWebView webView = (NonLeakingWebView) findViewById(R.id.webViewContent);
        webView.findNext(false);
    }

    public void closeSearchBox(View view) {
        NonLeakingWebView webView = (NonLeakingWebView) findViewById(R.id.webViewContent);
        _uih.closeSearchBox(webView);
    }
    // endregion

    // region Top-Down button
    public void toggleTopButton(boolean enable) {
        _uih.toggleTopButton(enable);
    }

    public void toggleBottomButton(boolean enable) {
        _uih.toggleBottomButton(enable);
    }

    public void goTop(View view) {

        NonLeakingWebView webView = (NonLeakingWebView) findViewById(R.id.webViewContent);
        webView.pageUp(true);
    }

    public void goBottom(View view) {
        NonLeakingWebView webView = (NonLeakingWebView) findViewById(R.id.webViewContent);
        webView.pageDown(true);
    }
    // endregion

    // region TTS methods
    private boolean getTtsStopOnPause() {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_TTS_TTS_STOP_ON_LOST_FOCUS, true);
    }

    @Override
    public void onInit(int status) {
        invalidateOptionsMenu();
    }

    @Override
    public void onComplete(Object i, Class<?> source) {
        Log.d(TAG, "Data: " + i + " from: " + source.getCanonicalName());
        if (i != null && source == TtsHelper.class) {
            NonLeakingWebView webView = (NonLeakingWebView) findViewById(R.id.webViewContent);
            _tts.autoScroll(webView, i.toString());
        }
    }

    public void sendHtmlForSpeak(String html) {
        _tts.start(html, contentUserData.getLastYScroll());
    }
    // endregion

    // region private methods

    // region bookmark handler

    /**
     * Build Bookmarks-on-Chapter menu
     */
    public void buildBookmarkMenu() {
        if (content != null) {
            try {
                int resourceId = R.layout.item_bookmark;
                if (UIHelper.isSmallScreen(this)) {
                    resourceId = R.layout.item_bookmark_small;
                }

                final BookmarkModelAdapter bookmarkAdapter = new BookmarkModelAdapter(this, resourceId, content.getBookmarks(), content.getPageModel());
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
     * Update Bookmark-on-Chapter data upon receiving event from webView client
     */
    public void refreshBookmarkData() {
        if (bookmarkMenu != null) {
            BookmarkModelAdapter bookmarkAdapter = (BookmarkModelAdapter) bookmarkMenu.getListView().getAdapter();
            if (bookmarkAdapter != null) {
                bookmarkAdapter.refreshData();
            }
        }
    }

    // endregion

    // region TOC handler

    /**
     * Move between chapters
     *
     * @param page
     */
    public void jumpTo(PageModel page) {
        setLastReadState();
        _tts.stop();
        Intent currIntent = this.getIntent();
        currIntent.putExtra(Constants.EXTRA_PAGE, page.getPage());
        currIntent.putExtra(Constants.EXTRA_PAGE_IS_EXTERNAL, page.isExternal());

        // open external page as Intent to open browser
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
    private void buildTOCMenu(final PageModel referencePageModel) {
        Log.d(TAG, "Trying to create TOC");
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

                final PageModelAdapter jumpAdapter = new PageModelAdapter(this, R.layout.item_jump_to, chapters);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getResources().getString(R.string.content_toc));
                builder.setAdapter(jumpAdapter, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PageModel page = jumpAdapter.getItem(which);
                        jumpTo(page);
                    }
                });
                builder.setNegativeButton(R.string.cancel, null);
                builder.setPositiveButton(R.string.back_to_index, new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        backToIndex(referencePageModel);
                    }
                });
                tocMenu = builder.create();
            }
        } catch (Exception e) {
            Log.e(TAG, "Cannot get current page for TOC menu.", e);
        }
    }

    /**
     * Back to Novel Details
     */
    public void backToIndex(PageModel referencePageModel) {
        try {
            PageModel pageModel = NovelsDao.getInstance().getExistingPageModel(referencePageModel, null).getParentPageModel();

            Intent intent = new Intent(this, NovelListContainerActivity.class);
            intent.putExtra(Constants.EXTRA_ONLY_WATCHED, false);
            intent.putExtra(Constants.EXTRA_PAGE, pageModel.getPage());
            this.startActivity(intent);

            finish();
        } catch (Exception e) {
            Log.e(TAG, "Failed to get parent page model", e);
        }
    }

    // endregion

    /**
     * Used for floating button on fullscreen mode to open the menu.
     *
     * @param view
     */
    public void openMenu(View view) {
        invalidateOptionsMenu();
        openOptionsMenu();
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
     * Set Previous and Next button state.
     *
     * @param pageModel
     */
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


    /**
     * Get invert color preferences
     *
     * @param ctx
     * @return true if dark theme.
     */
    public static boolean getColorPreferences(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(Constants.PREF_INVERT_COLOR, true);
    }
    // endregion
}
