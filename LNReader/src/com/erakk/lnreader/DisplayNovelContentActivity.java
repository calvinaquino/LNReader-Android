package com.erakk.lnreader;

import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.AsyncTaskResult;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.NovelContentModel;
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
	public class LoadNovelContentTask extends AsyncTask<PageModel, ProgressBar, AsyncTaskResult<NovelContentModel>> {

		@Override
		protected AsyncTaskResult<NovelContentModel> doInBackground(PageModel... params) {
			try{
				PageModel p = params[0];
				NovelContentModel content = NovelsDao.getNovelContent(p);
				return new AsyncTaskResult<NovelContentModel>(content);
			}catch(Exception e) {
				return new AsyncTaskResult<NovelContentModel>(e);
			}
		}

		protected void onPostExecute(AsyncTaskResult<NovelContentModel> result) {
			Exception e = result.getError();
			if(e == null) {
				NovelContentModel content = result.getResult();

				// test
				WebView wv = (WebView) findViewById(R.id.webView1);
				wv.getSettings().setAllowFileAccess(true);
				String html = Constants.WIKI_CSS_STYLE + content.getContent();
				wv.loadDataWithBaseURL("", html, "text/html", "utf-8", "");
				
				Log.d("LoadNovelContentTask", content.getPageModel().getTitle());
			}
			else {				
				Toast t = Toast.makeText(getApplicationContext(), e.getClass().toString() + ": " + e.getMessage(), Toast.LENGTH_SHORT);
				t.show();
			}
		}
	}

}
