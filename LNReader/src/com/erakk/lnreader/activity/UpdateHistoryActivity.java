package com.erakk.lnreader.activity;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.adapter.UpdateInfoModelAdapter;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.model.UpdateInfoModel;

public class UpdateHistoryActivity extends Activity {
	private static final String TAG = UpdateHistoryActivity.class.toString();
	ArrayList<UpdateInfoModel> updateList;
	ListView updateListView;
	UpdateInfoModelAdapter adapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		UIHelper.SetTheme(this, R.layout.activity_display_light_novel_list);
		UIHelper.SetActionBarDisplayHomeAsUp(this, true);
		setContentView(R.layout.activity_update_history);
		updateListView = (ListView) findViewById(R.id.update_list);
		updateListView.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				UpdateInfoModel item = updateList.get(arg2);
				openChapter(item);
			}
		});
		updateContent();
	}
	
	private void openChapter(UpdateInfoModel item) {
		Intent intent = new Intent(getApplicationContext(), DisplayLightNovelContentActivity.class);
        intent.putExtra(Constants.EXTRA_PAGE, item.getUpdatePage());
        startActivity(intent);
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_update_history, menu);
        return true;
    }
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        	case R.id.menu_settings:
        		Intent settingsIntent = new Intent(this, DisplaySettingsActivity.class);
            	startActivity(settingsIntent);
    			return true; 
        	case R.id.menu_clear_all:
        		NovelsDao.getInstance(this).deleteAllUpdateHistory();
        		updateContent();
    			return true; 
            case android.R.id.home:
    			super.onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
	
	public void updateContent () {
		try {
			updateList = NovelsDao.getInstance(this).getAllUpdateHistory();
			int resourceId = R.layout.update_list_item;
			adapter = new UpdateInfoModelAdapter(this, resourceId, updateList);
			updateListView.setAdapter(adapter);
			
		} catch (Exception e) {
			Log.e(TAG, e.getMessage(), e);
			Toast.makeText(this, "Error when updating: " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
}
