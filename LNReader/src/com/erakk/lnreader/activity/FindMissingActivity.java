package com.erakk.lnreader.activity;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.erakk.lnreader.Constants;
import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.adapter.FindMissingAdapter;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.IExtendedCallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.model.FindMissingModel;
import com.erakk.lnreader.task.DeleteMissingTask;

public class FindMissingActivity extends SherlockListActivity implements IExtendedCallbackNotifier<Integer> {

	private static final String TAG = FindMissingActivity.class.toString();
	private boolean isInverted;
	private ArrayList<FindMissingModel> models = null;
	private FindMissingAdapter adapter = null;
	private String mode;
	private boolean dowloadSelected = false;
	private boolean elseSelected = true;
	private DeleteMissingTask task;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		UIHelper.SetTheme(this, R.layout.activity_find_missing);
		UIHelper.SetActionBarDisplayHomeAsUp(this, true);

		isInverted = UIHelper.getColorPreferences(this);
		setContentView(R.layout.activity_find_missing);

		mode = getIntent().getStringExtra(Constants.EXTRA_FIND_MISSING_MODE);
		setItems(mode);
		setTitle("Maintenance: " + getString(getResources().getIdentifier(mode, "string", getPackageName())));

		checkWarning();
	}

	private void checkWarning() {
		if (UIHelper.getShowMaintWarning(this)) {
			UIHelper.createYesNoDialog(
					this
					, getResources().getString(R.string.maint_warning_text)
					, getResources().getString(R.string.maint_warning_title)
					, new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (which == DialogInterface.BUTTON_NEGATIVE) {
								finish();
							}
						}
					}).show();
		}
	}

	private void setItems(String extra) {
		int resourceId = R.layout.find_missing_list_item;
		models = NovelsDao.getInstance().getMissingItems(extra);
		adapter = new FindMissingAdapter(this, resourceId, models, extra, dowloadSelected, elseSelected);
		setListAdapter(adapter);
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		if (isInverted != UIHelper.getColorPreferences(this)) {
			UIHelper.Recreate(this);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getSupportMenuInflater().inflate(R.menu.activity_find_missing, menu);
		return true;
	}

	@SuppressLint("NewApi")
	private void executeDeleteTask(List<FindMissingModel> items, String mode) {
		task = new DeleteMissingTask(items, mode, this, TAG);
		String key = TAG + ":" + mode;
		boolean isAdded = LNReaderApplication.getInstance().addTask(key, task);
		if (isAdded) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
				task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			else
				task.execute();
		} else {
			Log.i(TAG, "Continue delete task: " + key);
			DeleteMissingTask tempTask = (DeleteMissingTask) LNReaderApplication.getInstance().getTask(key);
			if (tempTask != null) {
				task = tempTask;
				task.setCallback(this, TAG);
			}
			toggleProgressBar(true);
		}
	}

	public void toggleProgressBar(boolean show) {
		TextView loadingText = (TextView) findViewById(R.id.emptyList);
		if(loadingText != null) {
			if (show) {
				loadingText.setVisibility(TextView.VISIBLE);
				getListView().setVisibility(ListView.GONE);
			} else {
				loadingText.setVisibility(TextView.GONE);
				getListView().setVisibility(ListView.VISIBLE);
			}
		}
	}

	@Override
	public void onProgressCallback(ICallbackEventData message) {
		toggleProgressBar(true);
		TextView loadingText = (TextView) findViewById(R.id.emptyList);
		if(loadingText != null) {
			loadingText.setText(message.getMessage());
		}
	}

	@Override
	public void onCompleteCallback(ICallbackEventData message, Integer result) {
		Toast.makeText(this, getString(R.string.toast_show_deleted_count, result), Toast.LENGTH_SHORT).show();
		toggleProgressBar(false);
		setItems(mode);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_show_downloaded:
			if (adapter != null) {
				item.setChecked(!item.isChecked());
				adapter.filterDownloaded(item.isChecked());
				dowloadSelected = item.isChecked();
			}
			return true;
		case R.id.menu_show_everything_else:
			if (adapter != null) {
				item.setChecked(!item.isChecked());
				adapter.filterEverythingElse(item.isChecked());
				elseSelected = item.isChecked();
			}
			return true;
		case R.id.menu_clear_all:
			if(task != null && task.getStatus() !=Status.FINISHED) {
				Toast.makeText(this, getString(R.string.task_delete_running), Toast.LENGTH_SHORT).show();
				return true;
			}
			if (adapter != null) {
				List<FindMissingModel> items = adapter.getItems();
				if (items != null) {
					executeDeleteTask(items, mode);
				}
			}
			return true;
		case R.id.menu_clear_selected:
			if(task != null && task.getStatus() !=Status.FINISHED) {
				Toast.makeText(this, getString(R.string.task_delete_running), Toast.LENGTH_SHORT).show();
				return true;
			}
			if (adapter != null) {
				List<FindMissingModel> items = adapter.getItems();
				if (items != null) {
					List<FindMissingModel> selectedItems = new ArrayList<FindMissingModel>();
					for (FindMissingModel missing : items) {
						if (missing.isSelected()) {
							selectedItems.add(missing);
						}
					}
					executeDeleteTask(selectedItems, mode);
				}
			}
			return true;
		case android.R.id.home:
			super.onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean downloadListSetup(String taskId, String message, int setupType, boolean hasError) {
		return false;
	}
}
