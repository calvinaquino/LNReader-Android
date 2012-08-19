package com.erakk.lnreader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.AsyncTaskResult;
import com.erakk.lnreader.model.ImageModel;

public class DisplayImageActivity extends Activity {
	NovelsDao dao = new NovelsDao(this);
	WebView imgWebView;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_image);
        
        Intent intent = getIntent();
        String url = intent.getStringExtra(Constants.EXTRA_IMAGE_URL);
        
        imgWebView = (WebView) findViewById(R.id.webView1);
        imgWebView.getSettings().setAllowFileAccess(true);
        imgWebView.getSettings().setBuiltInZoomControls(true);
        imgWebView.getSettings().setLoadWithOverviewMode(true);
        imgWebView.getSettings().setUseWideViewPort(true);
        
        new LoadImageTask().execute(new String[] {url});
        
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_display_image, menu);
        return true;
    }
        
    @SuppressLint("NewApi")
	private void ToggleProgressBar(boolean show) {
		ProgressBar pb = (ProgressBar) findViewById(R.id.progressBar2);
		TextView tv = (TextView) findViewById(R.id.loading);
		if(show) {
			pb.setIndeterminate(true);
			pb.setActivated(true);
			pb.animate();
			pb.setVisibility(ProgressBar.VISIBLE);
		
			tv.setText("Loading...");
			tv.setVisibility(TextView.VISIBLE);
		}
		else {
			pb.setVisibility(ProgressBar.GONE);			
			tv.setVisibility(TextView.GONE);
		}
	}
    
    @SuppressLint("NewApi")
	public class LoadImageTask extends AsyncTask<String, String, AsyncTaskResult<ImageModel>> {
    	@Override
		protected void onPreExecute (){
			// executed on UI thread.
			ToggleProgressBar(true);
		}
    	
		@Override
		protected AsyncTaskResult<ImageModel> doInBackground(String... params) {
			String url = params[0];
			
			try{
				return new AsyncTaskResult<ImageModel>(dao.getImageModel(url));
			} catch (Exception e) {
				return new AsyncTaskResult<ImageModel>(e);
			}
			
		}
    	
		protected void onPostExecute(AsyncTaskResult<ImageModel> result) {
			Exception e = result.getError();
			if(e == null) {
				imgWebView = (WebView) findViewById(R.id.webView1);
				String imageUrl = "file:///" + result.getResult().getPath(); 
				imgWebView.loadUrl(imageUrl);
				Log.d("LoadImageTask", "Loading: " + imageUrl);
			}
			else{
				Toast.makeText(getApplicationContext(), e.getClass() + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
			}
			ToggleProgressBar(false);
		}
    }
}
