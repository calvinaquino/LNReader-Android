package com.erakk.lnreader.helper;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.DisplayImageActivity;

public class BakaTsukiWebViewClient extends WebViewClient {
	@Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
//        if (Uri.parse(url).getHost().equals("www.example.com")) {
//            // This is my web site, so do not override; let my WebView load the page
//            return false;
//        }

		Log.d("shouldOverrideUrlLoading", url);
		
		Context context = view.getContext();
		
		// if image file
		if(url.contains("title=File:")) {
			Intent intent = new Intent(context, DisplayImageActivity.class);
			intent.putExtra(Constants.EXTRA_IMAGE_URL, url);
			context.startActivity(intent);
		}
		else {
			//Toast.makeText(context, "Url: " + url, Toast.LENGTH_SHORT).show();
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			context.startActivity(browserIntent);
			return true;
		}
        return true;
    }	
}
