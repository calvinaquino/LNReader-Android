package com.erakk.lnreader;

import java.util.ArrayList;
import java.util.Iterator;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
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
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
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
        
//        LayoutInflater layoutInflater = (LayoutInflater)LightNovelChaptersActivity.this.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
//        Log.d("LayoutInflater", "Gone through1");
//        RelativeLayout headerLayout = (RelativeLayout)layoutInflater.inflate(R.id.ligh_novel_chapter_screen_header ,null, false);
//        Log.d("LayoutInflater", "Gone through2");
//        ((ExpandableListView) NovelView).addHeaderView( headerLayout );
//        Log.d("LayoutInflater", "Gone through3");
        
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
    
//    public ArrayList<ExpandListGroup> SetStandardGroups() {
//    	novelCol = result.getResult();
//		if(novelCol != null) {
//			Log.d("TRY", "SetStandardGroup");
//			ArrayList<ExpandListGroup> list = new ArrayList<ExpandListGroup>();
//			Log.d("TRY", "Set list1");
//			ArrayList<ExpandListChild> list2 = new ArrayList<ExpandListChild>();
//			Log.d("TRY", "Set list2");
//			//Error here
//			for(Iterator<BookModel> i = novelCol.getBookCollections().iterator(); i.hasNext();) {
//				Log.d("TRY", "iterator books/volume");
//				BookModel book = i.next();
//				Log.d("TRY", "set next");
//				ExpandListGroup volume = new ExpandListGroup();
//				Log.d("TRY", "alloc volume");
//				volume.setName(book.getTitle());
//				Log.d("TRY", "setName");
//				list.add(volume);
//        		Log.d("TRY", "add to list1");
//        		for(Iterator<PageModel> i2 = book.getChapterCollection().iterator(); i2.hasNext();){
//        			Log.d("TRY", "iterator chapters");
//        			PageModel chapter = i2.next();
//        			Log.d("TRY", "set next");
//        			ExpandListChild chapter_page = new ExpandListChild();
//        			Log.d("TRY", "alloc chapter");
//        			chapter_page.setName(chapter.getTitle() + " (" + chapter.getPage() + ")");
//        			Log.d("TRY", "setName+page");
//        			chapter_page.setTag(null);
//        			Log.d("TRY", "setTag");
//        			list2.add(chapter_page);
//        			Log.d("TRY", "add to list2");
//        		}
//				volume.setItems(list2);
//			}      
//			Log.d("TRY", "return list");  
//			return list;
//		}
//    }

    
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
				
				try {      
					Log.d("TRY", "list START");
		        	ExpandList = (ExpandableListView) findViewById(R.id.chapter_list);
					ArrayList<ExpandListGroup> list = new ArrayList<ExpandListGroup>();
					ArrayList<ExpandListChild> list2 = new ArrayList<ExpandListChild>();
					//Error here
					for(Iterator<BookModel> i = novelCol.getBookCollections().iterator(); i.hasNext();) {
	        			Log.d("TRY", "book "+i);
						BookModel book = i.next();
						ExpandListGroup volume = new ExpandListGroup();
						volume.setName(book.getTitle());
						list.add(volume);
		        		for(Iterator<PageModel> i2 = book.getChapterCollection().iterator(); i2.hasNext();){
		        			Log.d("TRY", "chapter "+i);
		        			PageModel chapter = i2.next();
		        			ExpandListChild chapter_page = new ExpandListChild();
		        			chapter_page.setName(chapter.getTitle() + " (" + chapter.getPage() + ")");
		        			chapter_page.setTag(null);
		        			list2.add(chapter_page);
		        		}
						volume.setItems(list2);
					}      
					Log.d("TRY", "list END");  
					ExpListItems = list;
		        	
		        	//ExpListItems = SetStandardGroups();
		        	ExpAdapter = new ExpandListAdapter(LightNovelChaptersActivity.this, ExpListItems);
		        	ExpandList.setAdapter(ExpAdapter);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					Toast t = Toast.makeText(LightNovelChaptersActivity.this, e.getClass().toString() +": " + e.getMessage(), Toast.LENGTH_SHORT);
					//Toast t = Toast.makeText(this, "expandable list error simple", Toast.LENGTH_SHORT);
					t.show();					
				}
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
