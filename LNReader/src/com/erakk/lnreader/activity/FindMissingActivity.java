package com.erakk.lnreader.activity;

import java.util.ArrayList;
import java.util.List;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.erakk.lnreader.Constants;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.adapter.FindMissingAdapter;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.model.FindMissingModel;

public class FindMissingActivity extends SherlockListActivity {

	private boolean isInverted;
	private ArrayList<FindMissingModel> models = null;
	private FindMissingAdapter adapter = null;
	private String mode;
	private boolean dowloadSelected = false;
	private boolean elseSelected = true;

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
		models = NovelsDao.getInstance(this).getMissingItems(extra);
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
			if (adapter != null) {
				int count = 0;
				List<FindMissingModel> items = adapter.getItems();
				if (items != null) {
					for (FindMissingModel missing : items) {
						count += NovelsDao.getInstance(this).deleteMissingItem(missing, mode);
					}
				}
				Toast.makeText(this, getString(R.string.toast_show_deleted_count, count), Toast.LENGTH_SHORT).show();
				setItems(mode);
			}
			return true;
		case R.id.menu_clear_selected:
			if (adapter != null) {
				List<FindMissingModel> items = adapter.getItems();
				int count = 0;
				if (items != null) {
					for (FindMissingModel missing : items) {
						if (missing.isSelected()) {
							count += NovelsDao.getInstance(this).deleteMissingItem(missing, mode);
						}
					}
				}
				Toast.makeText(this, getString(R.string.toast_show_deleted_count, count), Toast.LENGTH_SHORT).show();
				setItems(mode);
			}
			return true;
		case android.R.id.home:
			super.onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
