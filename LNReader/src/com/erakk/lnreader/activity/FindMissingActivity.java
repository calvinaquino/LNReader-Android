package com.erakk.lnreader.activity;

import java.util.ArrayList;

import android.os.Bundle;

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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		UIHelper.SetTheme(this, R.layout.activity_find_missing);
		UIHelper.SetActionBarDisplayHomeAsUp(this, true);

		isInverted = UIHelper.getColorPreferences(this);
		setContentView(R.layout.activity_find_missing);

		String extra = getIntent().getStringExtra(Constants.EXTRA_FIND_MISSING_MODE);
		getItems(extra);
	}

	private void getItems(String extra) {
		int resourceId = R.layout.find_missing_list_item;
		models = NovelsDao.getInstance(this).getMissingItems(extra);
		adapter = new FindMissingAdapter(this, resourceId, models);
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
		getSupportMenuInflater().inflate(R.menu.find_missing, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			super.onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
