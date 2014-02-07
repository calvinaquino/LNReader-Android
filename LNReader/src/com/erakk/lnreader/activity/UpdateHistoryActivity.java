package com.erakk.lnreader.activity;

import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.erakk.lnreader.Constants;
import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.adapter.UpdateInfoModelAdapter;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.model.UpdateInfoModel;
import com.erakk.lnreader.model.UpdateType;
import com.erakk.lnreader.service.UpdateService;

public class UpdateHistoryActivity extends SherlockActivity implements ICallbackNotifier{
	private static final String TAG = UpdateHistoryActivity.class.toString();
	private ArrayList<UpdateInfoModel> updateList;
	private ListView updateListView;
	private UpdateInfoModelAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		UIHelper.SetTheme(this, R.layout.activity_display_light_novel_list);
		UIHelper.SetActionBarDisplayHomeAsUp(this, true);
		setContentView(R.layout.activity_update_history);
		updateListView = (ListView) findViewById(R.id.update_list);
		updateListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				UpdateInfoModel item = updateList.get(arg2);
				openChapter(item);
			}
		});
		updateContent();
		LNReaderApplication.getInstance().setUpdateServiceListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();

		// relisting all handler
		LNReaderApplication.getInstance().setUpdateServiceListener(this);
	}

	private void openChapter(UpdateInfoModel item) {
		Intent intent = null;
		if (item.getUpdateType() == UpdateType.NewNovel) {
			intent = new Intent(this, DisplayLightNovelDetailsActivity.class);
			intent.putExtra(Constants.EXTRA_PAGE, item.getUpdatePage());
		}
		else if (item.getUpdateType() == UpdateType.New ||
				item.getUpdateType() == UpdateType.Updated ||
				item.getUpdateType() == UpdateType.UpdateTos) {
			intent = new Intent(this, DisplayLightNovelContentActivity.class);
			intent.putExtra(Constants.EXTRA_PAGE, item.getUpdatePage());
		}

		if (intent != null)
			startActivity(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.activity_update_history, menu);
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
		case R.id.menu_clear_selected:
			for (UpdateInfoModel updateInfo : updateList) {
				if (updateInfo.isSelected())
					NovelsDao.getInstance().deleteUpdateHistory(updateInfo);
			}
			updateContent();
			return true;
		case R.id.menu_show_updated:
			if (adapter != null) {
				item.setChecked(!item.isChecked());
				adapter.filterUpdated(item.isChecked());
			}
			return true;
		case R.id.menu_show_new:
			if (adapter != null) {
				item.setChecked(!item.isChecked());
				adapter.filterNew(item.isChecked());
			}
			return true;
		case R.id.menu_show_deleted:
			if (adapter != null) {
				item.setChecked(!item.isChecked());
				adapter.filterDeleted(item.isChecked());
			}
			return true;
		case R.id.menu_run_update:
			runUpdate();
			return true;
		case android.R.id.home:
			Intent intent = getIntent();
			String caller = intent.getStringExtra(Constants.EXTRA_CALLER_ACTIVITY);
			if (caller != null && caller.equalsIgnoreCase(UpdateService.class.toString())) {
				Intent mainIntent = new Intent(this, MainActivity.class);
				startActivity(mainIntent);
				finish();
			}
			else
				super.onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void runUpdate() {
		LinearLayout panel = (LinearLayout) findViewById(R.id.layout_update_status);
		if (panel != null) {
			panel.setVisibility(View.VISIBLE);
			LNReaderApplication.getInstance().runUpdateService(true, this);
			TextView txtUpdate = (TextView) findViewById(R.id.txtUpdate);
			txtUpdate.setText("Update Status: " + getResources().getString(R.string.running));
			ProgressBar progress = (ProgressBar) findViewById(R.id.download_progress_bar);
			progress.setIndeterminate(true);
		}
	}

	public void updateContent() {
		try {
			updateList = NovelsDao.getInstance(this).getAllUpdateHistory();
			int resourceId = R.layout.update_list_item;
			adapter = new UpdateInfoModelAdapter(this, resourceId, updateList);
			updateListView.setAdapter(adapter);

		} catch (Exception e) {
			Log.e(TAG, e.getMessage(), e);
			Toast.makeText(this, getResources().getString(R.string.error_update) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onProgressCallback(ICallbackEventData message) {
		LinearLayout panel = (LinearLayout) findViewById(R.id.layout_update_status);
		if (panel != null) {
			panel.setVisibility(View.VISIBLE);

			TextView txtUpdate = (TextView) findViewById(R.id.txtUpdate);
			txtUpdate.setText("Update Status: " + message.getMessage());

			ProgressBar progress = (ProgressBar) findViewById(R.id.download_progress_bar);
			if (message.getPercentage() < 100) {
				progress.setIndeterminate(false);
				progress.setMax(100);
				progress.setProgress(message.getPercentage());
				progress.setProgress(0);
				progress.setProgress(message.getPercentage());
				progress.setMax(100);
			}
			else {
				panel.setVisibility(View.GONE);
				updateContent();
			}
		}
	}
}
