package com.erakk.lnreader.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UI.activity.BaseActivity;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.IExtendedCallbackNotifier;
import com.erakk.lnreader.helper.DisplayNovelContentHtmlHelper;
import com.erakk.lnreader.helper.NonLeakingWebView;
import com.erakk.lnreader.helper.Util;
import com.erakk.lnreader.model.ImageModel;
import com.erakk.lnreader.task.AsyncTaskResult;
import com.erakk.lnreader.task.LoadImageTask;

import java.util.ArrayList;

public class DisplayImageActivity extends BaseActivity implements IExtendedCallbackNotifier<AsyncTaskResult<ImageModel>> {
    private static final String TAG = DisplayImageActivity.class.toString();
    private NonLeakingWebView imgWebView;
    private LoadImageTask task;
    private String url;

    private TextView loadingText;
    private ProgressBar loadingBar;

    private ArrayList<String> images;
    private int currentImageIndex = 0;
    private Menu _menu;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UIHelper.SetTheme(this, R.layout.activity_display_image);
        UIHelper.SetActionBarDisplayHomeAsUp(this, true);

        setupWebView();

        loadingText = (TextView) findViewById(R.id.emptyList);
        loadingBar = (ProgressBar) findViewById(R.id.loadProgress);

        Intent intent = getIntent();
        url = intent.getStringExtra(Constants.EXTRA_IMAGE_URL);

        images = intent.getStringArrayListExtra("image_list");
        if (images != null) {
            currentImageIndex = images.indexOf(url);
        }
    }

    public void setupWebView() {
        imgWebView = (NonLeakingWebView) findViewById(R.id.webViewImage);
        imgWebView.getSettings().setAllowFileAccess(true);
        imgWebView.getSettings().setLoadWithOverviewMode(true);
        imgWebView.getSettings().setUseWideViewPort(true);
        if (UIHelper.getColorPreferences(this))
            imgWebView.setBackgroundColor(0);

        imgWebView.getSettings().setBuiltInZoomControls(UIHelper.getZoomPreferences(this));
        imgWebView.setDisplayZoomControl(UIHelper.getZoomControlPreferences(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupWebView();

        executeTask(url, false);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt("currentImageIndex", currentImageIndex);
        savedInstanceState.putString(Constants.EXTRA_IMAGE_URL, url);
        savedInstanceState.putStringArrayList("image_list", images);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        currentImageIndex = savedInstanceState.getInt("currentImageIndex", 0);
        url = savedInstanceState.getString(Constants.EXTRA_IMAGE_URL);
        images = savedInstanceState.getStringArrayList("image_list");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (imgWebView != null) {
            RelativeLayout rootView = (RelativeLayout) findViewById(R.id.rootView);
            rootView.removeView(imgWebView);
            imgWebView.removeAllViews();
            imgWebView.destroy();
        }
    }

    @SuppressLint("NewApi")
    private void executeTask(String url, boolean refresh) {
        imgWebView = (NonLeakingWebView) findViewById(R.id.webViewImage);
        task = new LoadImageTask(url, refresh, this);
        String key = TAG + ":" + url;
        boolean isAdded = LNReaderApplication.getInstance().addTask(key, task);
        if (isAdded) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            else
                task.execute();
        } else {
            LoadImageTask tempTask = (LoadImageTask) LNReaderApplication.getInstance().getTask(key);
            if (tempTask != null) {
                task = tempTask;
                task.callback = this;
            }
            toggleProgressBar(true);
        }
        setPrevNextButtonState();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_display_image, menu);
        _menu = menu;
        setPrevNextButtonState();
        return true;
    }

    private void setPrevNextButtonState() {
        if (_menu != null) {
            boolean isNextEnabled = false;
            boolean isPrevEnabled = false;

            if (images != null && images.size() > 0) {
                Log.d(TAG, "Image Count: " + images.size());
                if (currentImageIndex > 0)
                    isPrevEnabled = true;
                if (images.size() != 1 && currentImageIndex < images.size() - 1)
                    isNextEnabled = true;
            }

            _menu.findItem(R.id.menu_chapter_next).setEnabled(isNextEnabled);
            _menu.findItem(R.id.menu_chapter_previous).setEnabled(isPrevEnabled);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                Intent launchNewIntent = new Intent(this, DisplaySettingsActivity.class);
                startActivity(launchNewIntent);
                return true;
            case R.id.menu_refresh_image:
            /*
             * Implement code to refresh image content
			 */
                executeTask(url, true);
                return true;
            case R.id.menu_downloads_list:
                Intent downloadsItent = new Intent(this, DownloadListActivity.class);
                startActivity(downloadsItent);
                return true;
            case R.id.menu_chapter_previous:
                currentImageIndex--;
                url = images.get(currentImageIndex);
                executeTask(url, false);
                return true;
            case R.id.menu_chapter_next:
                currentImageIndex++;
                url = images.get(currentImageIndex);
                executeTask(url, false);
                return true;
            case android.R.id.home:
                super.onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void toggleProgressBar(boolean show) {
        if (imgWebView == null || loadingBar == null || loadingText == null)
            return;
        if (show) {
            loadingText.setVisibility(TextView.VISIBLE);
            loadingBar.setVisibility(ProgressBar.VISIBLE);
            imgWebView.setVisibility(View.GONE);
        } else {
            loadingText.setVisibility(TextView.GONE);
            loadingBar.setVisibility(ProgressBar.GONE);
            imgWebView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean downloadListSetup(String id, String toastText, int type, boolean hasError) {
        Log.d(TAG, "Setup of " + id + ": " + toastText + " (type: " + type + ")" + "hasError: " + hasError);
        return false;
    }

    @Override
    public void onCompleteCallback(ICallbackEventData message, AsyncTaskResult<ImageModel> result) {
        if (result == null)
            return;

        Exception e = result.getError();
        if (e == null) {
            if (result.getResultType() == ImageModel.class) {
                ImageModel imageModel = result.getResult();
                if (!Util.isStringNullOrEmpty(imageModel.getPath())) {
                    String imageUrl = "file:///" + Util.sanitizeFilename(imageModel.getPath());
                    imageUrl = imageUrl.replace("file:////", "file:///");
                    imgWebView = (NonLeakingWebView) findViewById(R.id.webViewImage);
                    if (imgWebView != null) {
                        StringBuilder html = new StringBuilder();
                        html.append("<html><head>");
                        html.append(DisplayNovelContentHtmlHelper.getViewPortMeta());
                        html.append("</head><body>");
                        html.append("<img src=\"" + imageUrl + "\"></img>");
                        html.append("</body></html>");
                        //imgWebView.loadUrl(imageUrl);
                        imgWebView.loadDataWithBaseURL("file://", html.toString(), "text/html", "utf-8", null);
                    }

                    String title = imageModel.getName();
                    setTitle(title.substring(title.lastIndexOf("/")));
                    Toast.makeText(this, String.format("Loaded: %s", imageUrl), Toast.LENGTH_SHORT).show();
                    Log.d("LoadImageTask", "Loaded: " + imageUrl);
                } else {
                    Log.e(TAG, "Cannot get the image path.");
                    Toast.makeText(this, "Cannot load the image.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.w(TAG, "Getting unexpected class: " + result.getResultType().getName());
            }
        } else {
            Log.e(TAG, "Cannot load image.", e);
            Toast.makeText(this, e.getClass() + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        toggleProgressBar(false);

    }

    @Override
    public void onProgressCallback(ICallbackEventData message) {
        toggleProgressBar(true);

        loadingText.setText(message.getMessage());

        synchronized (this) {
            if (message.getPercentage() > -1) {
                // android progress bar bug
                // see: http://stackoverflow.com/a/4352073
                loadingBar.setIndeterminate(false);
                loadingBar.setMax(100);
                loadingBar.setProgress(message.getPercentage());
                loadingBar.setProgress(0);
                loadingBar.setProgress(message.getPercentage());
                loadingBar.setMax(100);
            } else {
                loadingBar.setIndeterminate(true);
            }
        }

    }
}
