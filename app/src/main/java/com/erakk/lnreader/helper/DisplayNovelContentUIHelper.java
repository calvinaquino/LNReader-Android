package com.erakk.lnreader.helper;

import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.erakk.lnreader.R;
import com.erakk.lnreader.activity.DisplayLightNovelContentActivity;

import java.lang.reflect.Method;

public class DisplayNovelContentUIHelper {
    private static final String TAG = DisplayNovelContentUIHelper.class.toString();

    private final DisplayLightNovelContentActivity parent;

    private final Handler mHandler = new Handler();
    private ImageButton goTop;
    private ImageButton goBottom;
    private Runnable hideBottom;
    private Runnable hideTop;


    public DisplayNovelContentUIHelper(DisplayLightNovelContentActivity parent) {
        this.parent = parent;
    }

    // Compatibility search method for older android version
    public void prepareCompatSearchBox(final WebView webView) {

        final EditText searchText = (EditText) parent.findViewById(R.id.searchText);
        searchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                search(webView, searchText.getText().toString());
                return false;
            }
        });
    }

    @SuppressWarnings("deprecation")
    private void search(WebView webView, String string) {
        if (string != null && string.length() > 0)
            webView.findAll(string);

        try {
            Method m = NonLeakingWebView.class.getMethod("setFindIsUp", Boolean.TYPE);
            m.invoke(webView, true);
        } catch (NoSuchMethodException me) {
        } catch (Exception e) {
            Log.e(TAG, "Error when searching", e);
        }
    }

    public void closeSearchBox(WebView webView) {
        RelativeLayout searchBox = (RelativeLayout) parent.findViewById(R.id.searchBox);
        searchBox.setVisibility(View.GONE);
        webView.clearMatches();
    }

    // end of Compatibility search method for older android version

    public void prepareTopDownButton() {
        goTop = (ImageButton) parent.findViewById(R.id.webview_go_top);
        goBottom = (ImageButton) parent.findViewById(R.id.webview_go_bottom);

        // Hide button after a certain time being shown
        hideBottom = new Runnable() {

            @Override
            public void run() {
                goBottom.setVisibility(ImageButton.GONE);
            }
        };
        hideTop = new Runnable() {

            @Override
            public void run() {
                goTop.setVisibility(ImageButton.GONE);
            }
        };
    }

    public void toggleTopButton(boolean enable) {
        if (enable) {
            goTop.setVisibility(ImageButton.VISIBLE);
            mHandler.removeCallbacks(hideTop);
            mHandler.postDelayed(hideTop, 1000);
        } else
            goTop.setVisibility(ImageButton.GONE);
    }

    public void toggleBottomButton(boolean enable) {
        if (enable) {
            goBottom.setVisibility(ImageButton.VISIBLE);
            mHandler.removeCallbacks(hideBottom);
            mHandler.postDelayed(hideBottom, 1000);
        } else
            goBottom.setVisibility(ImageButton.GONE);
    }
}
