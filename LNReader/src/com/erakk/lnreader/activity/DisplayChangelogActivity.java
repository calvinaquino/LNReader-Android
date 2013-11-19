package com.erakk.lnreader.activity;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;

public class DisplayChangelogActivity extends SherlockActivity {
	private static final String TAG = DisplayChangelogActivity.class.toString();
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		UIHelper.setLanguage(this);
		UIHelper.SetTheme(this, R.layout.activity_display_changelog);
		UIHelper.SetActionBarDisplayHomeAsUp(this, true);

		TextView txtChangelog = (TextView) findViewById(R.id.txtChangelog);
		//txtChangelog.setTextSize(20);
		txtChangelog.setText(readrawchangelog());
		txtChangelog.setMovementMethod(LinkMovementMethod.getInstance());
	}

	private String readrawchangelog(){
		InputStream inputStream = getResources().openRawResource(R.raw.changelog);
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		int i;
		try {
			i = inputStream.read();
			while (i != -1)
			{
				byteArrayOutputStream.write(i);
				i = inputStream.read();
			}
			inputStream.close();
		} catch (Exception e) {
			Log.e(TAG,"Failed to Read raw file:changelog.txt:"+e);
		}
		return byteArrayOutputStream.toString();
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
