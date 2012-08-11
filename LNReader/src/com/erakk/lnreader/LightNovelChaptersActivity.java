package com.erakk.lnreader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.model.NovelCollectionModel;

public class LightNovelChaptersActivity extends Activity {
	NovelsDao dao;
	
    @SuppressLint({ "NewApi", "NewApi" })
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Get intent and message
        Intent intent = getIntent();
        String novel = intent.getStringExtra(DisplayLightNovelsActivity.EXTRA_MESSAGE) + " Chapters";
        String page = intent.getStringExtra(DisplayLightNovelsActivity.EXTRA_PAGE);
        
        setContentView(R.layout.activity_light_novel_chapters);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        
        // get the textView
        TextView textView = (TextView) findViewById(R.id.synopsis);
        textView.setTextSize(20);
        textView.setText(novel);        
        
        dao = new NovelsDao(this);
        NovelCollectionModel novelCol;
		try {
			// TODO: change to proper ui elements :)
			novelCol = dao.getNovelDetailsFromInternet(page);
			String details = "";
			details += "\n Synopsys:\n" + novelCol.getSynopsis();
			details += "\n Cover Image:\n" + novelCol.getCover();
			
			textView.setText(novel + details);
			
			ImageView img = (ImageView) findViewById(R.id.cover);
			img.setImageURI(novelCol.getCoverUri());
		} catch (Exception e) {
			//e.printStackTrace();
			Log.e("NovelDetails", e.getMessage());
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
