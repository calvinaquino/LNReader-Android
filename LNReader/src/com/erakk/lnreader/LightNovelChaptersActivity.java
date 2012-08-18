package com.erakk.lnreader;

import java.util.ArrayList;
import java.util.Iterator;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
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
        PageModel page = new PageModel(); 
        page.setPage(intent.getStringExtra(Constants.EXTRA_PAGE));
        page.setTitle(intent.getStringExtra(Constants.EXTRA_TITLE));
        setContentView(R.layout.activity_light_novel_chapters);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        
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
      
        // setup listener
        ExpandList = (ExpandableListView) findViewById(R.id.chapter_list);
//        ExpandList.setLongClickable(true);
//        ExpandList.setOnLongClickListener(new OnLongClickListener() {
//			
//			@Override
//			public boolean onLongClick(View v) {
//				// TODO Auto-generated method stub
//				Toast t = Toast.makeText(LightNovelChaptersActivity.this, "longClick", Toast.LENGTH_SHORT);
//				t.show();
//				return false;
//			}
//		});
        ExpandList.setOnChildClickListener(new OnChildClickListener() {
			
			@Override
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
				if(novelCol != null) {
					PageModel p = novelCol.getBookCollections().get(groupPosition).getChapterCollection().get(childPosition);
					
					Intent intent = new Intent(getApplicationContext(), DisplayNovelContentActivity.class);
			        intent.putExtra(Constants.EXTRA_PAGE, p.getPage());
			        intent.putExtra(Constants.EXTRA_TITLE, p.getTitle());
			        
			        startActivity(intent);
				}
				return false;
			}
		});
    	
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
    	case R.id.menu_refresh_chapter_list:
			
			/*
			 * Implement code to refresh chapter/synopsis list
			 */
			
			Toast.makeText(getApplicationContext(), "Refreshing", Toast.LENGTH_SHORT).show();
			return true;
		case R.id.invert_colors:
			
			/*
			 * Implement code to invert colors
			 */
			
			Toast.makeText(getApplicationContext(), "Colors inverted", Toast.LENGTH_SHORT).show();
			return true;
        case android.R.id.home:
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		Toast.makeText(LightNovelChaptersActivity.this, "onCreateContextMenu", Toast.LENGTH_SHORT).show();
		ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;
		int type = ExpandableListView.getPackedPositionType(info.packedPosition);
		if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
//		if (v == findViewById(R.layout.expandvolume_list_item)) {
			inflater.inflate(R.menu.synopsys_volume_context_menu, menu);
		} else if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
//		} else if (v == findViewById(R.layout.expandchapter_list_item)) {
			inflater.inflate(R.menu.synopsys_chapter_context_menu, menu);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		//String[] names = getResources().getStringArray(R.array.novel_context_menu);
		switch(item.getItemId()) {
		//Volume cases
		case R.id.download_volume:
			
			/*
			 * Implement code to download this volume
			 */
			
			Toast.makeText(this, "Download this Volume",
					Toast.LENGTH_SHORT).show();
			return true;
		case R.id.clear_volume:
			
			/*
			 * Implement code to clear this volume cache
			 */
			
			Toast.makeText(this, "Clear this Volume",
					Toast.LENGTH_SHORT).show();
			return true;
		case R.id.mark_volume:
			
			/*
			 * Implement code to mark entire volume as read
			 */
			
			Toast.makeText(this, "Mark Volume as Read",
					Toast.LENGTH_SHORT).show();
			return true;
		//Chapter cases
		case R.id.download_chapter:
			
			/*
			 * Implement code to download this chapter
			 */
			
			Toast.makeText(this, "Download this chapter",
					Toast.LENGTH_SHORT).show();
			return true;
		case R.id.clear_chapter:
			
			/*
			 * Implement code to clear this chapter cache
			 */
			
			Toast.makeText(this, "Clear this Chapter",
					Toast.LENGTH_SHORT).show();
			return true;
		case R.id.mark_read:
			
			/*
			 * Implement code to mark this chapter read
			 */
			
			Toast.makeText(this, "Mark as Read",
					Toast.LENGTH_SHORT).show();
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}
    
	@SuppressLint("NewApi")
    public class LoadNovelDetailsTask extends AsyncTask<PageModel, ProgressBar, AsyncTaskResult<NovelCollectionModel>> {

		@Override
		protected void onPreExecute (){
			// executed on UI thread.
			ProgressBar pb = (ProgressBar) findViewById(R.id.progressBar2);
	    	pb.setIndeterminate(true);
	    	pb.setActivated(true);
	    	pb.animate();
		}
		
		@Override
		protected AsyncTaskResult<NovelCollectionModel> doInBackground(PageModel... arg0) {
			PageModel page = arg0[0];
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
		
		@Override
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
				
				// now add the volume and chapter list.
				try {
					ArrayList<ExpandListGroup> list = new ArrayList<ExpandListGroup>();
					for(Iterator<BookModel> i = novelCol.getBookCollections().iterator(); i.hasNext();) {
						BookModel book = i.next();
						ExpandListGroup volume = new ExpandListGroup();
						volume.setName(book.getTitle());
						list.add(volume);
						ArrayList<ExpandListChild> list2 = new ArrayList<ExpandListChild>();
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
