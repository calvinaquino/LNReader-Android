package com.erakk.lnreader.activity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.erakk.lnreader.Constants;
import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.callback.DownloadCallbackEventData;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.helper.AsyncTaskResult;
import com.erakk.lnreader.helper.NonLeakingWebView;
import com.erakk.lnreader.helper.Util;
import com.erakk.lnreader.model.ImageModel;
import com.erakk.lnreader.task.IAsyncTaskOwner;
import com.erakk.lnreader.task.LoadImageTask;

public class DisplayImageActivity extends SherlockActivity implements IAsyncTaskOwner {
	private static final String TAG = DisplayImageActivity.class.toString();
	private NonLeakingWebView imgWebView;
	private LoadImageTask task;
	private String url;
	private ProgressDialog dialog;

	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		UIHelper.SetTheme(this, R.layout.activity_display_image);
		UIHelper.SetActionBarDisplayHomeAsUp(this, true);

		imgWebView = (NonLeakingWebView) findViewById(R.id.webViewImage);
		imgWebView.getSettings().setAllowFileAccess(true);
		imgWebView.getSettings().setLoadWithOverviewMode(true);
		imgWebView.getSettings().setUseWideViewPort(true);
		imgWebView.setBackgroundColor(0);

		imgWebView.getSettings().setBuiltInZoomControls(getZoomPreferences());

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			imgWebView.getSettings().setDisplayZoomControls(getZoomControlPreferences());
		}

		Intent intent = getIntent();
		url = intent.getStringExtra(Constants.EXTRA_IMAGE_URL);
		executeTask(url, false);
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
		task = new LoadImageTask(refresh, this);
		String key = TAG + ":" + url;
		if (LNReaderApplication.getInstance().addTask(key, task)) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
				task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new String[] { url });
			else
				task.execute(new String[] { url });
		}
	}

	@Override
	protected void onStop() {
		// check running task
		if (task != null) {
			if (!(task.getStatus() == Status.FINISHED)) {
				Toast.makeText(this, getResources().getString(R.string.cancel_task) + task.toString(), Toast.LENGTH_SHORT).show();
				task.cancel(true);
			}
		}
		super.onStop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.activity_display_image, menu);
		return true;
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
			// refresh = true;
			executeTask(url, true);
			return true;
		case R.id.menu_downloads_list:
			Intent downloadsItent = new Intent(this, DownloadListActivity.class);
			startActivity(downloadsItent);
			return true;
		case android.R.id.home:
			super.onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void toggleProgressBar(boolean show) {
		if (show) {
			dialog = ProgressDialog.show(this, getResources().getString(R.string.display_image), getResources().getString(R.string.loading_image), false);
			dialog.getWindow().setGravity(Gravity.CENTER);
			dialog.setCanceledOnTouchOutside(true);
		} else {
			dialog.dismiss();
		}
	}

	@Override
	public void setMessageDialog(ICallbackEventData message) {
		if (dialog != null && dialog.isShowing()) {
			ICallbackEventData data = message;
			dialog.setMessage(data.getMessage());

			if (data.getClass() == DownloadCallbackEventData.class) {
				DownloadCallbackEventData downloadData = (DownloadCallbackEventData) data;
				int percent = downloadData.getPercentage();
				synchronized (dialog) {
					if (percent > -1) {
						// somehow doesn't works....
						dialog.setIndeterminate(false);
						dialog.setSecondaryProgress(percent);
						dialog.setMax(100);
						dialog.setProgress(percent);
						dialog.setMessage(data.getMessage());
					} else {
						dialog.setIndeterminate(true);
						dialog.setMessage(data.getMessage());
					}
				}
			}
		}
	}

	@Override
	public void getResult(AsyncTaskResult<?> result) {
		if (result == null)
			return;

		Exception e = result.getError();
		if (e == null) {
			ImageModel imageModel = (ImageModel) result.getResult();
			if (!Util.isStringNullOrEmpty(imageModel.getPath())) {
				imgWebView = (NonLeakingWebView) findViewById(R.id.webViewImage);
				String imageUrl = "file:///" + Util.sanitizeFilename(imageModel.getPath());
				imageUrl = imageUrl.replace("file:////", "file:///");
				imgWebView.loadUrl(imageUrl);
				String title = imageModel.getName();
				setTitle(title.substring(title.lastIndexOf("/")));
				Toast.makeText(this, String.format("Loaded: %s", imageUrl), Toast.LENGTH_SHORT).show();
				Log.d("LoadImageTask", "Loaded: " + imageUrl);
			} else {
				Log.e(TAG, "Cannot get the image path.");
				Toast.makeText(getApplicationContext(), "Cannot load the image.", Toast.LENGTH_SHORT).show();
			}
		} else {
			Log.e(TAG, "Cannot load image.", e);
			Toast.makeText(getApplicationContext(), e.getClass() + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
		}
		// LNReaderApplication.getInstance().removeTask(TAG + ":" + url);
	}

	@Override
	public void updateProgress(String id, int current, int total, String messString) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean downloadListSetup(String id, String toastText, int type, boolean hasError) {
		// TODO Auto-generated method stub
		return false;
	}

	private boolean getZoomPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_ZOOM_ENABLED, false);
	}

	private boolean getZoomControlPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_SHOW_ZOOM_CONTROL, false);
	}
}
