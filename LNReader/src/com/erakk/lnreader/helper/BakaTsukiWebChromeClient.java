package com.erakk.lnreader.helper;

import java.lang.ref.WeakReference;

import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;

import com.erakk.lnreader.R;
import com.erakk.lnreader.activity.DisplayLightNovelContentActivity;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.model.BookmarkModel;

public class BakaTsukiWebChromeClient extends WebChromeClient {
	private static final String TAG = BakaTsukiWebChromeClient.class.toString();
	private static final String HIGHLIGHT_EVENT = "HIGHLIGHT_EVENT";
	private static final String ADD = "highlighted";
	private static final String REMOVE = "clear";
	private static final String SCROLL_EVENT = "SCROLL_EVENT";
	private static final String LOAD_COMPLETE_EVENT = "LOAD_COMPLETE_EVENT";
	private int oldScrollY = 0;
	protected WeakReference<DisplayLightNovelContentActivity> activityRef;

	public BakaTsukiWebChromeClient(DisplayLightNovelContentActivity caller) {
		super();
		this.activityRef = new WeakReference<DisplayLightNovelContentActivity>(caller);
	}

	@Override
	public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
		final DisplayLightNovelContentActivity caller = activityRef.get();
		if (caller == null)
			return false;

		try {
			if (consoleMessage.message().startsWith(HIGHLIGHT_EVENT)) {
				Log.d(TAG, "Highlight Event");
				String data[] = consoleMessage.message().split(":");
				try {
					int pIndex = Integer.parseInt(data[1]);
					BookmarkModel bookmark = new BookmarkModel();
					bookmark.setPage(caller.content.getPage());
					bookmark.setpIndex(pIndex);
					if (data[2].equalsIgnoreCase(ADD)) {
						bookmark.setExcerpt(data[3].trim());
						NovelsDao.getInstance(caller).addBookmark(bookmark);
					} else if (data[2].equalsIgnoreCase(REMOVE)) {
						NovelsDao.getInstance(caller).deleteBookmark(bookmark);
					}
					caller.refreshBookmarkData();
				} catch (NumberFormatException ex) {
					Log.e(TAG, "Error when parsing pIndex: " + ex.getMessage(), ex);
				}
			} else if (consoleMessage.message().startsWith(SCROLL_EVENT)) {
				// Log.d(TAG, "Scroll Event");
				String data[] = consoleMessage.message().split(":");
				if (data.length > 1 && data[1] != null) {
					caller.updateLastLine(Integer.parseInt(data[1]));
					Log.d("SCROLL", "" + data[0] + " " + data[1]);
					int newScrollY = Integer.parseInt(data[1]);

					if (caller.getDynamicButtons()) {
						if (oldScrollY < newScrollY) {
							caller.toggleTopButton(false);
							caller.toggleBottomButton(true);
						} else if (oldScrollY > newScrollY) {
							caller.toggleBottomButton(false);
							caller.toggleTopButton(true);
						}
					}
					oldScrollY = newScrollY;
				} else if (consoleMessage.message().startsWith(LOAD_COMPLETE_EVENT)) {
					caller.notifyLoadComplete();
				}
			} else {
				Log.d(TAG, "Console: " + consoleMessage.lineNumber() + ":" + consoleMessage.message());
			}
		} catch (Exception ex2) {
			Log.e(TAG, "Unknown error when parsing data: " + consoleMessage.message(), ex2);
		}
		return true;
	}

	@Override
	public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
		Log.d(TAG, "JSAlert: " + message);
		return true;
	}

	@Override
	public void onProgressChanged(WebView view, int progress) {
		Log.d(TAG, "Progress: " + progress);
		final DisplayLightNovelContentActivity caller = activityRef.get();
		if (caller != null) {
			ProgressBar progressBar = (ProgressBar) caller.findViewById(R.id.progressBar1);
			if (progressBar != null) {
				if (progress < 100) {
					progressBar.setVisibility(View.VISIBLE);
					progressBar.setProgress(progress);
				} else {
					progressBar.setVisibility(View.GONE);
				}
			}
		}
	}
}
