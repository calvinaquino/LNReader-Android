package com.erakk.lnreader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class LightNovelChaptersActivity extends Activity {

    @SuppressLint({ "NewApi", "NewApi" })
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Get intent and message
        Intent intent = getIntent();
        String novel = intent.getStringExtra(DisplayLightNovelsActivity.EXTRA_MESSAGE) + " Chapters";
        
        setContentView(R.layout.activity_light_novel_chapters);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        
     // Create the text view
        TextView textView = new TextView(this);
        textView.setTextSize(20);
        textView.setText(novel);

        // Set the text view as the activity layout
        setContentView(textView);
        
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
