//package com.nandaka.bakareaderclone;
package com.erakk.lnreader;


//import java.io.IOException;
//import java.net.URL;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.AsyncTaskResult;
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
	public static final String EXTRA_TITLE = "com.erakk.lnreader.title";
	ArrayList<PageModel> listItems=new ArrayList<PageModel>();
	ArrayAdapter<PageModel> adapter;
	NovelsDao dao = new NovelsDao(this);

    @SuppressLint("NewApi")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_light_novels);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean invertColors = sharedPrefs.getBoolean("invert_colors", false);

        View NovelView = findViewById(R.id.light_novel_list_screen);
        ListView NovelList = (ListView) findViewById(android.R.id.list);
        //CheckBox isWatched = (CheckBox) findViewById(R.id.novel_is_watched);
        //TextView ListText = (TextView) findViewById(R.id.novel_name);
        
        
        if (invertColors == true) {
        	//NovelList.setBackgroundColor(Color.TRANSPARENT);
        	//ListText.setTextColor(Color.WHITE);
        	//isWatched
        	NovelView.setBackgroundColor(Color.BLACK);
        	
        }
        //R.layout.list_black_text,R.id.list_content
        try {
        	adapter=new ArrayAdapter<PageModel>(this,
	        		R.layout.novel_list_item,R.id.novel_name,
	        		listItems);
//        	adapter=new ArrayAdapter<PageModel>(this,
//	        		R.layout.novel_list_item,
//	        		listItems);
//	    	listItems = new LoadNovelsTask().execute(adapter).get().getResult();
	        new LoadNovelsTask().execute(new Void[] {});
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
        //Create new intent
        Intent intent = new Intent(this, LightNovelChaptersActivity.class);
        intent.putExtra(EXTRA_MESSAGE, novel);
        intent.putExtra(EXTRA_PAGE, o.getPage());
        intent.putExtra(EXTRA_TITLE, o.getTitle());
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
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
        ContextMenuInfo menuInfo) {
      if (v.getId()==android.R.layout.activity_list_item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
        menu.setHeaderTitle("Quick Menu");
        String[] menuItems = getResources().getStringArray(R.array.novel_context_menu);
        for (int i = 0; i<menuItems.length; i++) {
          menu.add(Menu.NONE, i, i, menuItems[i]);
        }
      }
    }
    
     public class LoadNovelsTask extends AsyncTask<Void, ProgressBar, AsyncTaskResult<ArrayList<PageModel>>> {

		@SuppressLint("NewApi")
		@Override
		protected AsyncTaskResult<ArrayList<PageModel>> doInBackground(Void... arg0) {
			ProgressBar pb = (ProgressBar) findViewById(R.id.progressBar2);
	    	pb.setIndeterminate(true);
	    	pb.setActivated(true);
	    	pb.animate();
	    	
			try {
				listItems = dao.getNovels();
				return new AsyncTaskResult<ArrayList<PageModel>>(listItems);
			} catch (Exception e) {
				e.printStackTrace();
				return new AsyncTaskResult<ArrayList<PageModel>>(e);
			}
		}
		
		@SuppressLint("NewApi")
		protected void onPostExecute(AsyncTaskResult<ArrayList<PageModel>> result) {
	         ProgressBar pb = (ProgressBar) findViewById(R.id.progressBar2);
			 TextView tv = (TextView) findViewById(R.id.loading);
			 tv.setVisibility(TextView.GONE);
	         pb.setActivated(false);
	         pb.setVisibility(ProgressBar.GONE);
	         ArrayList<PageModel> list = result.getResult();
	         if(list != null) adapter.addAll(list);
	         if(result.getError() != null) {
        	 	Exception e = result.getError();
        	 	Toast t = Toast.makeText(getApplicationContext(), e.getClass().toString() + ": " + e.getMessage(), Toast.LENGTH_SHORT);
				t.show();
				Log.e(this.getClass().toString(), e.getClass().toString() + ": " + e.getMessage());
	         }
	    }
    	 
    }
    
}

