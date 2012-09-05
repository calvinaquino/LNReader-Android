package com.erakk.lnreader.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.AsyncTaskResult;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.PageModel;

public class DisplaySynopsisActivity extends Activity {
	NovelsDao dao = NovelsDao.getInstance(this);
	NovelCollectionModel novelCol;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //Get intent and message
        Intent intent = getIntent();
        String novel = intent.getStringExtra(Constants.EXTRA_NOVEL);
        PageModel page = new PageModel(); 
        page.setPage(intent.getStringExtra(Constants.EXTRA_PAGE));
        page.setTitle(intent.getStringExtra(Constants.EXTRA_TITLE));
        
        setContentView(R.layout.activity_display_synopsis);
                
        UIHelper.SetActionBarDisplayHomeAsUp(this, true);
        
        View NovelView = findViewById(R.id.ligh_novel_synopsys_screen);

        // get the textView
        TextView textViewTitle = (TextView) findViewById(R.id.title);
        TextView textViewSynopsis = (TextView) findViewById(R.id.synopsys);
        
        textViewTitle.setTextSize(20);
        textViewSynopsis.setTextSize(16);         
        
        textViewTitle.setText(novel);
        
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean invertColors = sharedPrefs.getBoolean("invert_colors", false);
        
        if (invertColors == true) {
        	textViewSynopsis.setBackgroundColor(Color.TRANSPARENT);
        	textViewSynopsis.setTextColor(Color.WHITE);
        	textViewTitle.setBackgroundColor(Color.TRANSPARENT);
        	textViewTitle.setTextColor(Color.WHITE);
        	NovelView.setBackgroundColor(Color.BLACK);
        	
        }
        

        Log.d(null, "start Default");
        
        //dao = new NovelsDao(this);
        try {
        	new LoadNovelDetailsTask().execute(new PageModel[] {page});
        } catch (Exception e) {
			// TODO Auto-generated catch block
			Toast t = Toast.makeText(this, e.getClass().toString() +": " + e.getMessage(), Toast.LENGTH_SHORT);
			t.show();					
		}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_display_synopsys, menu);
        return true;
    }

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case R.id.menu_settings:
    		Intent launchNewIntent = new Intent(this, DisplaySettingsActivity.class);
    		startActivity(launchNewIntent);
    		return true;
        case android.R.id.home:
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
      
    public class LoadNovelDetailsTask extends AsyncTask<PageModel, ProgressBar, AsyncTaskResult<NovelCollectionModel>> {
    	public ICallbackNotifier notifier;
    	
		@Override
		protected AsyncTaskResult<NovelCollectionModel> doInBackground(PageModel... arg0) {
			PageModel page = arg0[0];
			try {
				NovelCollectionModel novelCol = dao.getNovelDetails(page, notifier);
				Log.d("LoadNovelDetailsTask", "Loaded: " + novelCol.getPage());				
		        return new AsyncTaskResult<NovelCollectionModel>(novelCol);
			} catch (Exception e) {
				e.printStackTrace();
				Log.e("NovelDetails", e.getClass().toString() + ": " + e.getMessage());
				return new AsyncTaskResult<NovelCollectionModel>(e);
			}
		}
		
		protected void onPostExecute(AsyncTaskResult<NovelCollectionModel> result) {
			novelCol = result.getResult();
			if(novelCol != null) {
				String details = "";
				details += novelCol.getSynopsis();
				
		        TextView textViewSynopsys = (TextView) findViewById(R.id.synopsys);
				textViewSynopsys.setText(details);
				
				// Removed the old way. was causing URL to URI conflict.
				ImageView ImageViewCover = (ImageView) findViewById(R.id.cover);
				if (novelCol.getCoverBitmap() == null) {
					// IN app test, is returning empty bitmap
					Toast tst = Toast.makeText(getApplicationContext(), "Bitmap empty", Toast.LENGTH_LONG);
					tst.show();
				}
				else {
					ImageViewCover.setImageBitmap(novelCol.getCoverBitmap());
				}
			}
			if(result.getError() != null) {
				Exception e = result.getError();
				Toast t = Toast.makeText(getApplicationContext(), e.getClass().toString() + ": " + e.getMessage(), Toast.LENGTH_SHORT);
				t.show();
				Log.e(this.getClass().toString(), e.getClass().toString() + ": " + e.getMessage());
			}
		}
    	 
    }
}
