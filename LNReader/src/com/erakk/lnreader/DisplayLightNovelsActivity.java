//package com.nandaka.bakareaderclone;
package com.erakk.lnreader;


//import java.io.IOException;
//import java.net.URL;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.model.PageModel;
//import org.jsoup.nodes.Document;
//import org.jsoup.nodes.Element;
//nadaka.bakareaderclone original
//import com.erakk.lnreader.helper.DownloadPageTask;
//import android.app.Activity;
//import android.widget.ListView;

/*
 * Author: Nandaka
 * Copy from: NovelsActivity.java
 */

public class DisplayLightNovelsActivity extends ListActivity {

	public final static String EXTRA_MESSAGE = "com.erakk.lnreader.NOVEL";
	public final static String EXTRA_PAGE = "com.erakk.lnreader.page";
	ArrayList<PageModel> listItems=new ArrayList<PageModel>();
	ArrayAdapter<PageModel> adapter;
	NovelsDao dao = new NovelsDao(this);

    @SuppressLint("NewApi")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_light_novels);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        
        try {
        	listItems = dao.getNovels();
//        	for(Iterator<PageModel> i = pages.iterator(); i
//					.hasNext();){
//        		PageModel p = i.next();
//        		listItems.add(p.getTitle());
//        	}
        	
	        adapter=new ArrayAdapter<PageModel>(this,
	        		android.R.layout.simple_list_item_1,
	        		listItems);
	    	setListAdapter(adapter);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Toast t = Toast.makeText(this, e.getClass().toString() +": " + e.getMessage(), Toast.LENGTH_SHORT);
			t.show();					
		}
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        // Get the item that was clicked
        PageModel o = adapter.getItem(position);
        String novel = o.toString();
        //Toast just for testing
        Toast.makeText(this, "You selected: " + novel, Toast.LENGTH_LONG).show();
        //Create new intent
        Intent intent = new Intent(this, LightNovelChaptersActivity.class);
        intent.putExtra(EXTRA_MESSAGE, novel);
        intent.putExtra(EXTRA_PAGE, o.getPage());
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_display_light_novels, menu);
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

