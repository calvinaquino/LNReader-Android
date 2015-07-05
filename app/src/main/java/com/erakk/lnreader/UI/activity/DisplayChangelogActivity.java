package com.erakk.lnreader.UI.activity;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;

public class DisplayChangelogActivity extends BaseActivity {
	private static final String TAG = DisplayChangelogActivity.class.toString();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_display_changelog);

		TextView txtChangelog = (TextView) findViewById(R.id.txtChangelog);
		txtChangelog.setText(UIHelper.readRawStringResources(this, R.raw.changelog));
		txtChangelog.setMovementMethod(LinkMovementMethod.getInstance());
		Log.d(TAG, "created");
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

}
