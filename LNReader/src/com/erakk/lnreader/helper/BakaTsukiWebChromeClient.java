package com.erakk.lnreader.helper;

import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Toast;

import com.erakk.lnreader.activity.DisplayLightNovelContentActivity;

public class BakaTsukiWebChromeClient extends WebChromeClient {
	private static final String TAG = BakaTsukiWebChromeClient.class.toString();
	private static final String HIGHLIGHT_EVENT = "HIGHLIGHT_EVENT";
	private DisplayLightNovelContentActivity caller;
	
	public BakaTsukiWebChromeClient(DisplayLightNovelContentActivity caller) {
		super();
		this.caller = caller;
	}
	
	@Override
	public boolean onConsoleMessage (ConsoleMessage consoleMessage) {
		if(consoleMessage.message().startsWith(HIGHLIGHT_EVENT)) {
			String data[] = consoleMessage.message().split(":");
			Toast.makeText(caller, "[Highlight event] para:" + data[1] + " mode:"+ data[2], Toast.LENGTH_SHORT).show();
		}
		else {
			Log.d(TAG, "Console: " + consoleMessage.lineNumber() + ":" + consoleMessage.message());
		}
		return true;
	}
	
	@Override
	public boolean onJsAlert (WebView view, String url, String message, JsResult result) {
		Log.d(TAG, "JSAlert: " + message);
		return true;
	}
}
