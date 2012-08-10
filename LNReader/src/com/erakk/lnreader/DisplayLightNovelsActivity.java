//package com.nandaka.bakareaderclone;
package com.erakk.lnreader;


//import java.io.IOException;
//import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

//import org.jsoup.nodes.Document;
//import org.jsoup.nodes.Element;

//nadaka.bakareaderclone original
import com.erakk.lnreader.dao.NovelsDao;
//import com.erakk.lnreader.helper.DownloadPageTask;
import com.erakk.lnreader.model.PageModel;

import android.os.Bundle;
import android.annotation.SuppressLint;
//import android.app.Activity;
import android.app.ListActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
//import android.widget.ListView;
import android.widget.Toast;
import android.support.v4.app.NavUtils;

/*
 * Author: Nandaka
 * Copy from: NovelsActivity.java
 */

public class DisplayLightNovelsActivity extends ListActivity {

	ArrayList<String> listItems=new ArrayList<String>();
	ArrayAdapter<String> adapter;
	NovelsDao dao = new NovelsDao(this);

    @SuppressLint("NewApi")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_light_novels);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        
        try {
        	ArrayList<PageModel> pages = dao.getNovels();
        	for(Iterator<PageModel> i = pages.iterator(); i
					.hasNext();){
        		PageModel p = i.next();
        		listItems.add(p.getTitle());
        	}
        	
	        adapter=new ArrayAdapter<String>(this,
	        		android.R.layout.simple_list_item_1,
	        		listItems);
	    	setListAdapter(adapter);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Toast t = Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT);
			t.show();					
		}
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

