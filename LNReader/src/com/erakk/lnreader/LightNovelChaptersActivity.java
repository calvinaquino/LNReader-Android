package com.erakk.lnreader;

import java.util.ArrayList;
import java.util.Iterator;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.model.BookModel;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.PageModel;

public class LightNovelChaptersActivity extends Activity {
	NovelsDao dao;
	
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
        
        // TODO: get image Cover and put in ImageView :)
        //ImageView ImageViewCover = (ImageView) findViewById(R.id.cover);
        //ImageViewCover.setImageBitmap( <TheImage> );
        
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
        NovelCollectionModel novelCol;
		try {
			// TODO: change to proper ui elements :)
			novelCol = dao.getNovelDetails(page);
			String details = "";
			details += novelCol.getSynopsis();
			
			// test only for listing books BookModelers
			details += "\n\nListing: "; 
			for(Iterator<BookModel> i = novelCol.getBookCollections().iterator(); i.hasNext();) {
				BookModel book = i.next();
				details += "\n" + book.getTitle();
				for(Iterator<PageModel> i2 = book.getChapterCollection().iterator(); i2.hasNext();){
					PageModel chapter = i2.next();
					details += "\n\t" + chapter.getTitle() + " (" + chapter.getPage() + ")";
				}
				details += "\n";
			}
			//details += "\n Cover Image:\n" + novelCol.getCover();
			
			textViewSynopsys.setText(details);
			
			// Removed the old way. was causing URL to URI conflict.
			ImageView ImageViewCover = (ImageView) findViewById(R.id.cover);
	        ImageViewCover.setImageBitmap(novelCol.getCoverBitmap() );
	        
	        if (novelCol.getCoverBitmap() == null) {
	        	// IN app test, is returning empty bitmap
	        	Toast tst = Toast.makeText(this, "Bitmap empty", Toast.LENGTH_LONG);
	        	tst.show();
	        }
	        
		} catch (Exception e) {
			e.printStackTrace();
			Log.e("NovelDetails", e.getClass().toString() + ": " + e.getMessage());
			Toast t = Toast.makeText(this, e.getClass().toString() + ": " + e.getMessage(), Toast.LENGTH_SHORT);
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
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
