package com.erakk.lnreader.activity;

import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;

public class DisplayCreditActivity extends SherlockActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		UIHelper.setLanguage(this);
		UIHelper.SetTheme(this, R.layout.activity_display_credit);
		UIHelper.SetActionBarDisplayHomeAsUp(this, true);

		TextView txtCredit = (TextView) findViewById(R.id.txtCredits);
		txtCredit.setTextSize(20);
		// limited support on html tag, see: http://stackoverflow.com/a/3150456
		txtCredit.setText(Html.fromHtml("<h1>Credits</h1><h2>UI and Parsers</h2>" +
				"- erakk<br />" +
				"- <a href=\"http://nandaka.wordpress.com\">nandaka</a><br />" +
				"- Thatdot7<br />" +
				"- freedomofkeima<br />" +
				"<h2>UI Translations</h2>" +
				"- English : erakk & nandaka<br />" +
				"- Indonesian : freedomofkeima<br />" +
				"- French : Lery<br />" +
				"<br />" +
				"And other people contributing through <a href=\"http://baka-tsuki.org/forums/\">baka-tsuki forum</a> :D<br />"));
		// allow link to be clickable, see: http://stackoverflow.com/a/8722574
		txtCredit.setMovementMethod(LinkMovementMethod.getInstance());
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
