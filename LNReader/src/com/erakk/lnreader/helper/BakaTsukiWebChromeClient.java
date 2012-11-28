package com.erakk.lnreader.helper;

import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.erakk.lnreader.activity.DisplayLightNovelContentActivity;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.model.NovelBookmark;

public class BakaTsukiWebChromeClient extends WebChromeClient {
	private static final String TAG = BakaTsukiWebChromeClient.class.toString();
	private static final String HIGHLIGHT_EVENT = "HIGHLIGHT_EVENT";
	private static final String ADD = "highlighted";
	private static final String REMOVE = "clear";
	private DisplayLightNovelContentActivity caller;
	
	public BakaTsukiWebChromeClient(DisplayLightNovelContentActivity caller) {
		super();
		this.caller = caller;
	}
	
	@Override
	public boolean onConsoleMessage (ConsoleMessage consoleMessage) {
		if(consoleMessage.message().startsWith(HIGHLIGHT_EVENT)) {
			String data[] = consoleMessage.message().split(":");
			NovelBookmark bookmark = new NovelBookmark();
			bookmark.setPage(caller.content.getPage());
			bookmark.setpIndex(Integer.parseInt(data[1]));
			if(data[2].equalsIgnoreCase(ADD)) {
				NovelsDao.getInstance(caller).addBookmark(bookmark);
			}
			else if(data[2].equalsIgnoreCase(REMOVE)) {
				NovelsDao.getInstance(caller).deleteBookmark(bookmark);
			}
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
