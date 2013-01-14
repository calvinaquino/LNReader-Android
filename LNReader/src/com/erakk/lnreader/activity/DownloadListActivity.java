package com.erakk.lnreader.activity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.R.layout;
import com.erakk.lnreader.R.menu;
import com.erakk.lnreader.adapter.DownloadListAdapter;
import com.erakk.lnreader.adapter.PageModelAdapter;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.AsyncTaskResult;
import com.erakk.lnreader.model.DownloadModel;
import com.erakk.lnreader.task.IAsyncTaskOwner;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.AsyncTask.Status;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Menu;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class DownloadListActivity extends Activity {
	private static final String TAG = DownloadListActivity.class.toString();
	ArrayList<DownloadModel> downloadList;
	ListView downliadListView;
	DownloadListAdapter adapter;
	private static DownloadListActivity instance;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		UIHelper.SetTheme(this, R.layout.activity_display_light_novel_list);
		UIHelper.SetActionBarDisplayHomeAsUp(this, true);
		setContentView(R.layout.activity_download_list);
		instance = this;
		downliadListView = (ListView) findViewById(R.id.download_list);
		downloadList = LNReaderApplication.getInstance().getDownloadList();
		updateContent();

	}
	public static DownloadListActivity getInstance() {
		return instance;
	}
	
	public void updateContent () {
		try {
		int resourceId = R.layout.download_list_item;
		adapter = new DownloadListAdapter(getApplicationContext(), resourceId, downloadList);
		downliadListView.setAdapter(adapter);
		} catch (Exception e) {
			Log.e(TAG, e.getMessage(), e);
			Toast.makeText(this, "Error when updating: " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
}
