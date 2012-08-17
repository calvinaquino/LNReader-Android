package com.erakk.lnreader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.zip.Inflater;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.LayoutInflater;
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
        //String novel = intent.getStringExtra(DisplayLightNovelsActivity.EXTRA_MESSAGE);
        PageModel page = new PageModel(); 
        page.setPage(intent.getStringExtra(DisplayLightNovelsActivity.EXTRA_PAGE));
        page.setTitle(intent.getStringExtra(DisplayLightNovelsActivity.EXTRA_TITLE));
        
        setContentView(R.layout.activity_light_novel_chapters);
        
        
        getActionBar().setDisplayHomeAsUpEnabled(true);
        
        //View NovelView = findViewById(R.id.ligh_novel_chapter_screen);
        
//        LayoutInflater layoutInflater = (LayoutInflater)LightNovelChaptersActivity.this.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
//        Log.d("LayoutInflater", "Gone through1");
//        RelativeLayout headerLayout = (RelativeLayout)layoutInflater.inflate(R.id.ligh_novel_chapter_screen_header ,null, false);
//        Log.d("LayoutInflater", "Gone through2");
//        ((ExpandableListView) NovelView).addHeaderView( headerLayout );
//        Log.d("LayoutInflater", "Gone through3");
        
        // get the textView
        
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean invertColors = sharedPrefs.getBoolean("invert_colors", false);
        
        if (invertColors == true) {
        	/*textViewSynopsys.setBackgroundColor(Color.TRANSPARENT);
        	textViewSynopsys.setTextColor(Color.WHITE);
        	textViewTitle.setBackgroundColor(Color.TRANSPARENT);
        	textViewTitle.setTextColor(Color.WHITE);
        	NovelView.setBackgroundColor(Color.BLACK);*/
        	
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

			novelCol = result.getResult();
			if(novelCol != null) {
				
				//Clear progressBar and string
				TextView tv = (TextView) findViewById(R.id.loading);
				tv.setVisibility(TextView.GONE);
				pb.setActivated(false);
				pb.setVisibility(ProgressBar.GONE);
				
				ExpandList = (ExpandableListView) findViewById(R.id.chapter_list);
				
				// Prepare header
				LayoutInflater layoutInflater = getLayoutInflater();
				View synopsis = layoutInflater.inflate(R.layout.activity_display_synopsis, null);
				synopsis.findViewById(R.id.loading).setVisibility(TextView.GONE);
				synopsis.findViewById(R.id.progressBar2).setVisibility(ProgressBar.GONE);
				
				TextView textViewTitle = (TextView) synopsis.findViewById(R.id.title);
				TextView textViewSynopsis = (TextView) synopsis.findViewById(R.id.synopsys);
				textViewTitle.setTextSize(20);
		        textViewSynopsis.setTextSize(16);         
		        
		        textViewTitle.setText(novelCol.getPageModel().getTitle());
		        textViewSynopsis.setText(novelCol.getSynopsis());
		        
		        ImageView ImageViewCover = (ImageView) synopsis.findViewById(R.id.cover);
		        if (novelCol.getCoverBitmap() == null) {
					// IN app test, is returning empty bitmap
					Toast tst = Toast.makeText(getApplicationContext(), "Bitmap empty", Toast.LENGTH_LONG);
					tst.show();
				}
				else {
					ImageViewCover.setImageBitmap(novelCol.getCoverBitmap());
				}
		        
		        ExpandList.addHeaderView(synopsis);
				
				
				try {
					ArrayList<ExpandListGroup> list = new ArrayList<ExpandListGroup>();
					ArrayList<ExpandListChild> list2 = new ArrayList<ExpandListChild>();
					
							/*
							 * Verify this iterator bookModel (probably pageModel, its getting all
							 * chapters of the novel for each book/group
							 */
					for(Iterator<BookModel> i = novelCol.getBookCollections().iterator(); i.hasNext();) {
						BookModel book = i.next();
						ExpandListGroup volume = new ExpandListGroup();
						volume.setName(book.getTitle());
						list.add(volume);
		        		for(Iterator<PageModel> i2 = book.getChapterCollection().iterator(); i2.hasNext();){
		        			PageModel chapter = i2.next();
		        			ExpandListChild chapter_page = new ExpandListChild();
		        			chapter_page.setName(chapter.getTitle());
//		        			chapter_page.setName(chapter.getTitle() + " (" + chapter.getPage() + ")");
		        			chapter_page.setTag(null);
		        			list2.add(chapter_page);
		        		}
						volume.setItems(list2);
					}       
					ExpListItems = list;
		        	ExpAdapter = new ExpandListAdapter(LightNovelChaptersActivity.this, ExpListItems);
		        	ExpandList.setAdapter(ExpAdapter);
		        	
		        	
		        	
				} catch (Exception e) {
					e.getStackTrace();
					Toast t = Toast.makeText(LightNovelChaptersActivity.this, e.getClass().toString() +": " + e.getMessage(), Toast.LENGTH_SHORT);
					t.show();					
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
