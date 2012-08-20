package com.erakk.lnreader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Picture;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebView.PictureListener;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.AsyncTaskResult;
import com.erakk.lnreader.helper.BakaTsukiWebViewClient;
import com.erakk.lnreader.helper.ICallbackNotifier;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.NovelContentModel;
import com.erakk.lnreader.model.PageModel;

@SuppressWarnings("deprecation")
@SuppressLint("NewApi")
public class DisplayLightNovelContentActivity extends Activity {
	private static final String TAG = DisplayLightNovelContentActivity.class.toString();
	private NovelsDao dao = new NovelsDao(this);
	private NovelContentModel content;
	private NovelCollectionModel novelDetails;
	private LoadNovelContentTask task;
	private PageModel pageModel;
	private String volume;
	private String parentPage;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_display_light_novel_content);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		Intent intent = getIntent();
		//PageModel page = new PageModel(); 
		//page.setPage(intent.getStringExtra(Constants.EXTRA_PAGE));
		//page.setTitle(intent.getStringExtra(Constants.EXTRA_TITLE));
		//volume = intent.getStringExtra(Constants.EXTRA_VOLUME);
		parentPage = intent.getStringExtra(Constants.EXTRA_NOVEL);
		
		try {
			pageModel = dao.getPageModel(intent.getStringExtra(Constants.EXTRA_PAGE), null);
			ToggleProgressBar(true);
			task = new LoadNovelContentTask();
			task.execute(new PageModel[] {pageModel});	
		} catch (Exception e) {
			e.printStackTrace();
			Log.d(TAG, "Failed to get the PageModel for content: " + content.getPage());
		}
		Log.d(TAG, "onCreate called");
	}
	
	@Override
	public void onPause() {
		if(content!= null) {
			// save last position and zoom
			WebView wv = (WebView) findViewById(R.id.webView1);
			content.setLastXScroll(wv.getScrollX());
			content.setLastYScroll(wv.getScrollY());
			content.setLastZoom(wv.getScale());
			content = dao.updateNovelContent(content);
			
			if(wv.getContentHeight() <=  wv.getScrollY() + wv.getBottom()) {
				try{
					PageModel page = content.getPageModel();
					page.setFinishedRead(true);
					page = dao.updatePageModel(page);
					Log.d(TAG, "Update Content: " + content.getLastXScroll() + " " + content.getLastYScroll() +  " " + content.getLastZoom());			
				}catch(Exception ex) {
					ex.printStackTrace();
					Log.d(TAG, "Error updating PageModel for Content: " + content.getPage());
				}
			}
		}
		Log.d(TAG, "Pausing activity");
		super.onPause();
	}
	
	@Override
	public void onStop() {
		if(task.getStatus() != Status.FINISHED) {
			task.cancel(true);
		}
		Log.d(TAG, "Stopping activity");
		super.onStop();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_display_novel_content, menu);
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
			
			Toast.makeText(getApplicationContext(), "Refreshing", Toast.LENGTH_SHORT).show();
			return true;
		case R.id.invert_colors:
			
			/*
			 * Implement code to invert colors
			 */
			
			Toast.makeText(getApplicationContext(), "Colors inverted", Toast.LENGTH_SHORT).show();
			return true;
		case R.id.menu_chapter_previous:
			
			/*
			 * Implement code to move to previous chapter
			 */
			PageModel prev = novelDetails.getPrev(pageModel.getPage());
			if(prev!= null) {
				pageModel = prev;
				task = new LoadNovelContentTask();
				task.execute(prev);
				//Toast.makeText(getApplicationContext(), "Go previous: " + prev.getTitle(), Toast.LENGTH_SHORT).show();
			}
			else {
				Toast.makeText(getApplicationContext(), "First available chapter.", Toast.LENGTH_SHORT).show();
			}
			return true;
		case R.id.menu_chapter_next:
			
			/*
			 * Implement code to move to next chapter
			 */
			PageModel next = novelDetails.getNext(pageModel.getPage());
			if(next!= null) {
				pageModel = next;
				task = new LoadNovelContentTask();
				task.execute(next);
				//Toast.makeText(getApplicationContext(), "Go next: " + next.getTitle(), Toast.LENGTH_SHORT).show();
			}
			else {
				Toast.makeText(getApplicationContext(), "Last available chapter.", Toast.LENGTH_SHORT).show();
			}
			
			return true;
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
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
	
	public void setContent(NovelContentModel content) {
		this.content = content;
	}
	
	public class LoadNovelContentTask extends AsyncTask<PageModel, String, AsyncTaskResult<NovelContentModel>> implements ICallbackNotifier{
    	public void onCallback(String message) {
    		publishProgress(message);
    	}
    	
		@Override
		protected void onPreExecute (){
			// executed on UI thread.
			ToggleProgressBar(true);
		}
		
		@Override
		protected AsyncTaskResult<NovelContentModel> doInBackground(PageModel... params) {
			try{
				PageModel p = params[0];
				NovelContentModel content = dao.getNovelContent(p, this);
				return new AsyncTaskResult<NovelContentModel>(content);
			}catch(Exception e) {
				return new AsyncTaskResult<NovelContentModel>(e);
			}
		}

		protected void onPostExecute(AsyncTaskResult<NovelContentModel> result) {
			Exception e = result.getError();
			
			if(e == null) {
				content = result.getResult();
				setContent(content);

				// load the contents here
				final WebView wv = (WebView) findViewById(R.id.webView1);
				wv.getSettings().setAllowFileAccess(true);
				wv.getSettings().setSupportZoom(true);
				wv.getSettings().setBuiltInZoomControls(true);
				wv.getSettings().setLoadWithOverviewMode(true);
				//wv.getSettings().setUseWideViewPort(true);
				
				// custom link handler
				BakaTsukiWebViewClient client = new BakaTsukiWebViewClient();
				wv.setWebViewClient(client);
				
				String html = Constants.WIKI_CSS_STYLE + "<body>" + content.getContent() + "</body></html>" ;
				wv.loadDataWithBaseURL(Constants.BASE_URL, html, "text/html", "utf-8", "");
				
				Log.d("LoadNovelContentTask", content.getPage());
				
				wv.setInitialScale((int) (content.getLastZoom() * 100));
				
				wv.setPictureListener(new PictureListener(){
					boolean needScroll = true;
					@Override
					@Deprecated
					public void onNewPicture(WebView arg0, Picture arg1) {
						Log.d(TAG, "Content Height: " + wv.getContentHeight() + " : " + content.getLastYScroll());
						if(needScroll && wv.getContentHeight() * content.getLastZoom() > content.getLastYScroll()) {
							//wv.scrollTo(content.getLastXScroll(), content.getLastYScroll());
							//wv.setScrollX(value)
							wv.setScrollY(content.getLastYScroll());
							needScroll = false;
						}						
					}					
				});
				try{
					novelDetails = dao.getNovelDetails(pageModel.getParentPageModel(), null);
					
					volume = pageModel.getParent().replace(pageModel.getParentPageModel().getPage() + Constants.NOVEL_BOOK_DIVIDER, "");
					
					setTitle(pageModel.getParentPageModel().getTitle() + ": " + volume + " "+ pageModel.getTitle());
				} catch (Exception ex) {
				
				}
				Log.d(TAG, "Load Content: " + content.getLastXScroll() + " " + content.getLastYScroll() +  " " + content.getLastZoom());
			}
			else {
				e.printStackTrace();
				Toast.makeText(getApplicationContext(), e.getClass().toString() + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
			}
			ToggleProgressBar(false);
		}
	}

}
