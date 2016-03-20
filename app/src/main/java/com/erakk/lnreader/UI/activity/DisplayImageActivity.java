package com.erakk.lnreader.UI.activity;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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

    private LoadImageTask task;
    private String url;
    private String imageUrl;
    private String parent;

    private ArrayList<String> images;
    private int currentImageIndex = 0;
    private Menu _menu;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_image);

        setupWebView();

        Intent intent = getIntent();
        url = intent.getStringExtra(Constants.EXTRA_IMAGE_URL);
        parent = intent.getStringExtra(Constants.EXTRA_PAGE);

        images = intent.getStringArrayListExtra("image_list");
        if (images != null) {
            currentImageIndex = images.indexOf(url);
        }
    }

    public void setupWebView() {
        NonLeakingWebView imgWebView = (NonLeakingWebView) findViewById(R.id.webViewImage);
        imgWebView.getSettings().setAllowFileAccess(true);
        imgWebView.getSettings().setLoadWithOverviewMode(true);
        imgWebView.getSettings().setUseWideViewPort(true);
        imgWebView.setBackgroundColor(0);

        imgWebView.getSettings().setBuiltInZoomControls(UIHelper.getZoomPreferences(this));
        imgWebView.setDisplayZoomControl(UIHelper.getZoomControlPreferences(this));
    }

    @Override
    public void onResume() {
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
        NonLeakingWebView imgWebView = (NonLeakingWebView) findViewById(R.id.webViewImage);
        if (imgWebView != null) {
            RelativeLayout rootView = (RelativeLayout) findViewById(R.id.rootView);
            rootView.removeView(imgWebView);
            imgWebView.removeAllViews();
            imgWebView.destroy();
        }
    }

    @SuppressLint("NewApi")
    private void executeTask(String url, boolean refresh) {
        NonLeakingWebView imgWebView = (NonLeakingWebView) findViewById(R.id.webViewImage);
        task = new LoadImageTask(url, parent, refresh, this);
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
            case R.id.menu_refresh_image:
            /*
             * Implement code to refresh image content
			 */
                executeTask(url, true);
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
            case R.id.menu_download_image:
                if( !Util.isStringNullOrEmpty(imageUrl)) {
                    String temp[] = url.split("/");
                    String filename = temp[temp.length - 1];
                    filename = filename.replace("index.php?title=File:", "");

                    if(imageUrl.startsWith("file://")) {
                        String dest = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + filename;
                        try{
                            Util.copyFile(imageUrl.toString().replace("file://", ""), dest);
                            Toast.makeText(this, "Image saved to: " + dest, Toast.LENGTH_SHORT).show();
                        }
                        catch (Exception ex) {
                            Log.e(TAG, ex.getMessage(), ex);
                            Toast.makeText(this, "Failed to save image to: " + dest, Toast.LENGTH_LONG).show();
                        }
                    }
                    else {
                        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(imageUrl));
                        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
                        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                        dm.enqueue(request);
                    }
                }
                return true;
            case android.R.id.home:
                super.onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void toggleProgressBar(boolean show) {
        NonLeakingWebView imgWebView = (NonLeakingWebView) findViewById(R.id.webViewImage);
        TextView loadingText = (TextView) findViewById(R.id.emptyList);
        ProgressBar loadingBar = (ProgressBar) findViewById(R.id.loadProgress);
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
                    imageUrl = "file:///" + Util.sanitizeFilename(imageModel.getPath());
                    imageUrl = imageUrl.replace("file:////", "file:///");
                    NonLeakingWebView imgWebView = (NonLeakingWebView) findViewById(R.id.webViewImage);
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
                    //Toast.makeText(this, String.format("Loaded: %s", imageUrl), Toast.LENGTH_SHORT).show();
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

        TextView loadingText = (TextView) findViewById(R.id.emptyList);
        ProgressBar loadingBar = (ProgressBar) findViewById(R.id.loadProgress);

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
