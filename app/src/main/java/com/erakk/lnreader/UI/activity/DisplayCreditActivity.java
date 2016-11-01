package com.erakk.lnreader.UI.activity;

import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.widget.TextView;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.R;

public class DisplayCreditActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_credit);

        TextView txtCredit = (TextView) findViewById(R.id.txtCredits);
        txtCredit.setTextSize(20);
        // limited support on html tag, see: http://stackoverflow.com/a/3150456
        txtCredit.setText(Html.fromHtml("<h1>Credits</h1><h2>UI and Parsers</h2>" +
                "- erakk<br />" +
                "- <a href=\"https://nandaka.devnull.zone/\">nandaka</a><br />" +
                "- Thatdot7<br />" +
                "- <a href=\"http://nstranslation.blogspot.com\">freedomofkeima</a><br />" +
                "<h2>UI Translations</h2>" +
                "- English : erakk & nandaka<br />" +
                "- Indonesian : freedomofkeima<br />" +
                "- French : Lery<br />" +
                "<br />" +
                "And other people contributing through <a href=\""+ Constants.ROOT_HTTPS + Constants.ROOT_URL + "/forums/\">baka-tsuki forum</a> :D<br />"));
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
