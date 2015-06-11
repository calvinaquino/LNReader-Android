package com.erakk.lnreader.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UI.activity.BaseActivity;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.adapter.DownloadListAdapter;
import com.erakk.lnreader.model.DownloadModel;

import java.util.ArrayList;

public class DownloadListActivity extends BaseActivity {
	private static final String TAG = DownloadListActivity.class.toString();
	ArrayList<DownloadModel> downloadList;
	ListView downloadListView;
	DownloadListAdapter adapter;
	private static DownloadListActivity instance;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		UIHelper.SetTheme(this, R.layout.activity_download_list);
		UIHelper.SetActionBarDisplayHomeAsUp(this, true);
		// setContentView(R.layout.activity_download_list);
		instance = this;
		downloadListView = (ListView) findViewById(R.id.download_list);
		downloadList = LNReaderApplication.getInstance().getDownloadList();
		updateContent();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public static DownloadListActivity getInstance() {
		return instance;
	}

	public int getDownloadListCount() {
		return downloadList.size();
	}

	public void updateContent() {
		try {
			int resourceId = R.layout.download_list_item;
			adapter = new DownloadListAdapter(this, resourceId, downloadList);
			downloadListView.setAdapter(adapter);
		} catch (Exception e) {
			Log.e(TAG, e.getMessage(), e);
			Toast.makeText(this, getResources().getString(R.string.error_update) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
}
