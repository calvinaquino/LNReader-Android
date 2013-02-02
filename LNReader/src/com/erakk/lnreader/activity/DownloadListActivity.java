package com.erakk.lnreader.activity;

import java.util.ArrayList;

import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.adapter.DownloadListAdapter;
import com.erakk.lnreader.model.DownloadModel;

public class DownloadListActivity extends SherlockActivity {
	private static final String TAG = DownloadListActivity.class.toString();
	ArrayList<DownloadModel> downloadList;
	ListView downloadListView;
	DownloadListAdapter adapter;
	private static DownloadListActivity instance;
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		UIHelper.SetTheme(this, R.layout.activity_display_light_novel_list);
		UIHelper.SetActionBarDisplayHomeAsUp(this, true);
		setContentView(R.layout.activity_download_list);
		instance = this;
		downloadListView = (ListView) findViewById(R.id.download_list);
		downloadList = LNReaderApplication.getInstance().getDownloadList();
		updateContent();

	}
	public static DownloadListActivity getInstance() {
		return instance;
	}
	
	public int getDownloadListCount() {
		return downloadList.size();
	}
	
	public void updateContent () {
		try {
			int resourceId = R.layout.download_list_item;
			adapter = new DownloadListAdapter(getApplicationContext(), resourceId, downloadList);
			downloadListView.setAdapter(adapter);
		} catch (Exception e) {
			Log.e(TAG, e.getMessage(), e);
			Toast.makeText(this, "Error when updating: " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
}
