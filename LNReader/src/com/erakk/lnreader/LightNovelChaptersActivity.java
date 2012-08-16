package com.erakk.lnreader;

import java.util.ArrayList;
import java.util.Iterator;

import android.annotation.SuppressLint;
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
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.erakk.lnreader.adapter.ExpandListAdapter;
import com.erakk.lnreader.classes.ExpandListChild;
import com.erakk.lnreader.classes.ExpandListGroup;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.AsyncTaskResult;
import com.erakk.lnreader.model.BookModel;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.PageModel;

public class LightNovelChaptersActivity extends Activity {
	NovelsDao dao;
	NovelCollectionModel novelCol;
    private ExpandListAdapter ExpAdapter;
    private ArrayList<ExpandListGroup> ExpListItems;
    private ExpandableListView ExpandList;
    
    @SuppressLint({ "NewApi", "NewApi" })
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        
        //Get intent and message
        Intent intent = getIntent();
        String novel = intent.getStringExtra(DisplayLightNovelsActivity.EXTRA_MESSAGE);
        PageModel page = new PageModel(); 
        page.setPage(intent.getStringExtra(DisplayLightNovelsActivity.EXTRA_PAGE));
        page.setTitle(intent.getStringExtra(DisplayLightNovelsActivity.EXTRA_TITLE));
        
        setContentView(R.layout.activity_light_novel_chapters);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        
        View NovelView = findViewById(R.id.ligh_novel_chapter_screen);
        
        // get the textView
        TextView textViewTitle = (TextView) findViewById(R.id.title);
        TextView textViewSynopsys = (TextView) findViewById(R.id.synopsys);
        
        textViewTitle.setTextSize(20);
        textViewSynopsys.setTextSize(16);             
        
        textViewTitle.setText(novel);
        
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean invertColors = sharedPrefs.getBoolean("invert_colors", false);
        
        if (invertColors == true) {
        	textViewSynopsys.setBackgroundColor(Color.TRANSPARENT);
        	textViewSynopsys.setTextColor(Color.WHITE);
        	textViewTitle.setBackgroundColor(Color.TRANSPARENT);
        	textViewTitle.setTextColor(Color.WHITE);
        	NovelView.setBackgroundColor(Color.BLACK);
        	
        }
        try {
        	ExpandList = (ExpandableListView) findViewById(R.id.chapter_list);
        	//Error on this line.. =S
        	ExpListItems = SetStandardGroups();
        	Log.d("TRY", "setGroupOK");
        	ExpAdapter = new ExpandListAdapter(LightNovelChaptersActivity.this, ExpListItems);
        	ExpandList.setAdapter(ExpAdapter);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Toast t = Toast.makeText(this, e.getClass().toString() +": " + e.getMessage(), Toast.LENGTH_SHORT);
			//Toast t = Toast.makeText(this, "expandable list error simple", Toast.LENGTH_SHORT);
			t.show();					
		}
        
        dao = new NovelsDao(this);
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
        getMenuInflater().inflate(R.menu.activity_light_novel_chapters, menu);
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
    
    public ArrayList<ExpandListGroup> SetStandardGroups() {
    	        ArrayList<ExpandListGroup> list = new ArrayList<ExpandListGroup>();
    	        ArrayList<ExpandListChild> list2 = new ArrayList<ExpandListChild>();
    	        
    	        for(Iterator<BookModel> i = novelCol.getBookCollections().iterator(); i.hasNext();) {
					BookModel book = i.next();
					ExpandListGroup volume = new ExpandListGroup();
					volume.setName(book.getTitle());
	    	        list.add(volume);
					for(Iterator<PageModel> i2 = book.getChapterCollection().iterator(); i2.hasNext();){
						PageModel chapter = i2.next();
		    	        ExpandListChild chapter_page = new ExpandListChild();
		    	        chapter_page.setName(chapter.getTitle() + " (" + chapter.getPage() + ")");
		    	        chapter_page.setTag(null);
		    	        list2.add(chapter_page);
					}
				}
    	        
    	        
    	        /*ExpandListGroup gru1 = new ExpandListGroup();
    	        gru1.setName("Comedy");
    	        ExpandListChild ch1_1 = new ExpandListChild();
    	        ch1_1.setName("A movie");
    	        ch1_1.setTag(null);
    	        list2.add(ch1_1);
    	        ExpandListChild ch1_2 = new ExpandListChild();
    	        ch1_2.setName("An other movie");
    	        ch1_2.setTag(null);
    	        list2.add(ch1_2);
    	        ExpandListChild ch1_3 = new ExpandListChild();
    	        ch1_3.setName("And an other movie");
    	        ch1_3.setTag(null);
    	        list2.add(ch1_3);
    	        gru1.setItems(list2);
    	        list2 = new ArrayList<ExpandListChild>();
    	        ExpandListGroup gru2 = new ExpandListGroup();
    	        gru2.setName("Action");
    	        ExpandListChild ch2_1 = new ExpandListChild();
    	        ch2_1.setName("A movie");
    	        ch2_1.setTag(null);
    	        list2.add(ch2_1);
    	        ExpandListChild ch2_2 = new ExpandListChild();
    	        ch2_2.setName("An other movie");
    	        ch2_2.setTag(null);
    	        list2.add(ch2_2);
    	        ExpandListChild ch2_3 = new ExpandListChild();
    	        ch2_3.setName("And an other movie");
    	        ch2_3.setTag(null);
    	        list2.add(ch2_3);
    	        gru2.setItems(list2);
    	        list.add(gru1);
    	        list.add(gru2);*/
    	        
    	        return list;
    	    }

    
    public class LoadNovelDetailsTask extends AsyncTask<PageModel, ProgressBar, AsyncTaskResult<NovelCollectionModel>> {

		@SuppressLint("NewApi")
		@Override
		protected AsyncTaskResult<NovelCollectionModel> doInBackground(PageModel... arg0) {
			PageModel page = arg0[0];
			ProgressBar pb = (ProgressBar) findViewById(R.id.progressBar2);
	    	pb.setIndeterminate(true);
	    	pb.setActivated(true);
	    	pb.animate();
				        
			try {
				NovelCollectionModel novelCol = dao.getNovelDetails(page);
				Log.d("LoadNovelDetailsTask", "Loaded: " + novelCol.getPage());				
		        return new AsyncTaskResult<NovelCollectionModel>(novelCol);
			} catch (Exception e) {
				e.printStackTrace();
				Log.e("NovelDetails", e.getClass().toString() + ": " + e.getMessage());
				return new AsyncTaskResult<NovelCollectionModel>(e);
			}
		}
		
		@SuppressLint("NewApi")
		protected void onPostExecute(AsyncTaskResult<NovelCollectionModel> result) {
			ProgressBar pb = (ProgressBar) findViewById(R.id.progressBar2);
			
			pb.setActivated(false);
			pb.setVisibility(ProgressBar.GONE);

			// TODO: get image Cover and put in ImageView :)
			//ImageView ImageViewCover = (ImageView) findViewById(R.id.cover);
			//ImageViewCover.setImageBitmap( <TheImage> );

			novelCol = result.getResult();
			if(novelCol != null) {
				
				//Clear progressBar and string
				 TextView tv = (TextView) findViewById(R.id.loading);
				 tv.setVisibility(TextView.GONE);
		         pb.setActivated(false);
		         pb.setVisibility(ProgressBar.GONE);
				
				String details = "";
				details += novelCol.getSynopsis();
				
				// TODO: change to proper ui elements :)
				// test only for listing books BookModelers
				/*details += "\n\nListing: "; 
				for(Iterator<BookModel> i = novelCol.getBookCollections().iterator(); i.hasNext();) {
					BookModel book = i.next();
					details += "\n" + book.getTitle();
					for(Iterator<PageModel> i2 = book.getChapterCollection().iterator(); i2.hasNext();){
						PageModel chapter = i2.next();
						details += "\n\t" + chapter.getTitle() + " (" + chapter.getPage() + ")";
					}
					details += "\n";
				}*/
				//details += "\n Cover Image:\n" + novelCol.getCover();
				
		        TextView textViewSynopsys = (TextView) findViewById(R.id.synopsys);
				textViewSynopsys.setText(details);
				
				// Removed the old way. was causing URL to URI conflict.
				ImageView ImageViewCover = (ImageView) findViewById(R.id.cover);
		        ImageViewCover.setImageBitmap(novelCol.getCoverBitmap() );
		        	
				if (novelCol.getCoverBitmap() == null) {
					// IN app test, is returning empty bitmap
					Toast tst = Toast.makeText(getApplicationContext(), "Bitmap empty", Toast.LENGTH_LONG);
					tst.show();
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
