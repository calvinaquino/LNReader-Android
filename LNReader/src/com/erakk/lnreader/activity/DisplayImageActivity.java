package com.erakk.lnreader.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.Toast;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.callback.DownloadCallbackEventData;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.AsyncTaskResult;
import com.erakk.lnreader.model.ImageModel;

public class DisplayImageActivity extends Activity {
	private NovelsDao dao = NovelsDao.getInstance(this);
	private WebView imgWebView;
	private LoadImageTask task;
	private boolean refresh = false;
	private String url;
	private ProgressDialog dialog;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
    	UIHelper.SetTheme(this, R.layout.activity_display_image);
        UIHelper.SetActionBarDisplayHomeAsUp(this, true);
        
        imgWebView = (WebView) findViewById(R.id.webView1);
        imgWebView.getSettings().setAllowFileAccess(true);
        imgWebView.getSettings().setBuiltInZoomControls(true);
        imgWebView.getSettings().setLoadWithOverviewMode(true);
        imgWebView.getSettings().setUseWideViewPort(true);
        
        Intent intent = getIntent();
        url = intent.getStringExtra(Constants.EXTRA_IMAGE_URL);
        executeTask(url);
    }
	
	@SuppressLint("NewApi")
	private void executeTask(String url) {        
		task = new LoadImageTask();        
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new String[] {url});
		else
			task.execute(new String[] {url});
	}
	
    @Override
    protected void onStop() {
    	// check running task
    	if(task != null){
    		if(!(task.getStatus() == Status.FINISHED)) {
    			Toast.makeText(this, "Canceling task: " + task.toString(), Toast.LENGTH_SHORT).show();
    			task.cancel(true);    			
    		}
    	}
    	super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_display_image, menu);
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
			refresh = true;
			executeTask(url);			
			return true;
		case android.R.id.home:
			super.onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
    
	private void ToggleProgressBar(boolean show) {
		if(show) {
			dialog = ProgressDialog.show(this, "Display Image", "Loading. Please wait...", false);
			dialog.getWindow().setGravity(Gravity.CENTER);
			dialog.setCanceledOnTouchOutside(true);
		
			if(refresh) {
				dialog.setMessage("Refreshing...");
			}
			else {
				dialog.setMessage("Loading...");
			}
		}
		else {
			dialog.dismiss();
		}
	}
    	
	public class LoadImageTask extends AsyncTask<String, ICallbackEventData, AsyncTaskResult<ImageModel>> implements ICallbackNotifier {
    	String url = "";
    	public String toString() {
    		return "LoadImageTask: " + url;
    	}
    	
    	public void onCallback(ICallbackEventData message) {
    		publishProgress(message);
    	}
    	
    	@Override
		protected void onPreExecute (){
			// executed on UI thread.
			ToggleProgressBar(true);
		}
    	
		@Override
		protected AsyncTaskResult<ImageModel> doInBackground(String... params) {
			this.url = params[0];			
			try{
				if(refresh) {
					return new AsyncTaskResult<ImageModel>(dao.getImageModelFromInternet(url, this));
				}
				else {
					return new AsyncTaskResult<ImageModel>(dao.getImageModel(url, this));
				}
			} catch (Exception e) {
				return new AsyncTaskResult<ImageModel>(e);
			}			
		}
		
		@Override
		protected void onProgressUpdate (ICallbackEventData... values){
			//executed on UI thread.
			ICallbackEventData data = values[0];
			dialog.setMessage(data.getMessage());

			if(data.getClass() == DownloadCallbackEventData.class) {
				DownloadCallbackEventData downloadData = (DownloadCallbackEventData) data;
				int percent = downloadData.getPercentage();
				synchronized (dialog) {
					if(percent > -1) {
						// somehow doesn't works....
						dialog.setIndeterminate(false);
						dialog.setSecondaryProgress(percent);
						dialog.setMax(100);
						dialog.setProgress(percent);
						dialog.setMessage(data.getMessage());
					}
					else {
						dialog.setIndeterminate(true);
						dialog.setMessage(data.getMessage());
					}
				}
			}
		}
		
		@Override
		protected void onPostExecute(AsyncTaskResult<ImageModel> result) {
			Exception e = result.getError();
			if(e == null) {
				imgWebView = (WebView) findViewById(R.id.webView1);
				String imageUrl = "file:///" + result.getResult().getPath(); 
				imgWebView.loadUrl(imageUrl);
				Log.d("LoadImageTask", "Loading: " + imageUrl);
			}
			else{
				e.printStackTrace();
				Toast.makeText(getApplicationContext(), e.getClass() + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
			}
			ToggleProgressBar(false);
		}
    }
}
