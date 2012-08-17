package com.erakk.lnreader;

import com.erakk.lnreader.helper.AsyncTaskResult;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.PageModel;

import android.os.AsyncTask;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.support.v4.app.NavUtils;

@SuppressLint("NewApi")
public class DisplayNovelContentActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_novel_content);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        
        Intent intent = getIntent();
        PageModel page = new PageModel(); 
        page.setPage(intent.getStringExtra(Constants.EXTRA_PAGE));
        page.setTitle(intent.getStringExtra(Constants.EXTRA_TITLE));
        
        new LoadNovelContentTask().execute(new PageModel[] {page});
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_display_novel_content, menu);
        return true;
    }

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    public class LoadNovelContentTask extends AsyncTask<PageModel, ProgressBar, AsyncTaskResult<?>> {

		@Override
		protected AsyncTaskResult<?> doInBackground(PageModel... params) {
			try{
				PageModel p = params[0];
				return new AsyncTaskResult<PageModel>(p);
			}catch(Exception e) {
				return new AsyncTaskResult<PageModel>(e);
			}
		}
    	
		protected void onPostExecute(AsyncTaskResult<?> result) {
			Exception e = result.getError();
			if(e == null) {
				PageModel page = (PageModel) result.getResult();
		        
		        // test
		        WebView wv = (WebView) findViewById(R.id.webView1);
		        String url = Constants.BaseURL + "/index.php?title=" + page.getPage();
		        wv.loadUrl(url);
		        Log.d("LoadNovelContentTask", url);
			}
			else {				
				Toast t = Toast.makeText(getApplicationContext(), e.getClass().toString() + ": " + e.getMessage(), Toast.LENGTH_SHORT);
		        t.show();
			}
		}
    }

}
