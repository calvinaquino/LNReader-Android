package com.erakk.lnreader.helper;

import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;

import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.activity.DisplayLightNovelContentActivity;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.model.BookmarkModel;

import java.lang.ref.WeakReference;

public class BakaTsukiWebChromeClient extends WebChromeClient {
	private static final String TAG = BakaTsukiWebChromeClient.class.toString();
	private static final String HIGHLIGHT_EVENT = "HIGHLIGHT_EVENT";
	private static final String ADD = "highlighted";
	private static final String REMOVE = "clear";
	private static final String SCROLL_EVENT = "SCROLL_EVENT";
	private static final String LOAD_COMPLETE_EVENT = "LOAD_COMPLETE_EVENT";
	private static final String SPEAK_EVENT = "SPEAK_EVENT";
	private int oldScrollY = 0;
	protected WeakReference<DisplayLightNovelContentActivity> activityRef;
	private static String lastLog = "";

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
						NovelsDao.getInstance().addBookmark(bookmark);
					} else if (data[2].equalsIgnoreCase(REMOVE)) {
						NovelsDao.getInstance().deleteBookmark(bookmark);
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
					String message = "" + data[0] + " " + data[1];
					// suppress log message if the same
					if (!lastLog.equalsIgnoreCase(message)) {
						Log.d(TAG, message);
						lastLog = message;
					}
					int newScrollY = Integer.parseInt(data[1]);

					if (UIHelper.getDynamicButtonsPreferences(caller)) {
						if (oldScrollY < newScrollY) {
							caller.toggleTopButton(false);
							caller.toggleBottomButton(true);
						} else if (oldScrollY > newScrollY) {
							caller.toggleBottomButton(false);
							caller.toggleTopButton(true);
						}
					}
					oldScrollY = newScrollY;
				}
			} else if (consoleMessage.message().startsWith(LOAD_COMPLETE_EVENT)) {
				Log.d(TAG, "Console: " + consoleMessage.lineNumber() + ":" + consoleMessage.message());
				caller.notifyLoadComplete();
			} else if (consoleMessage.message().startsWith(SPEAK_EVENT)) {
				String data[] = consoleMessage.message().split(":", 2);
				caller.sendHtmlForSpeak(data[1]);
			} else {
				Log.w(TAG, "Console: " + consoleMessage.lineNumber() + ":" + consoleMessage.message());
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

	private int scrollY;
	private boolean requireScroll = false;
	public void setScrollY(int y) {
		this.scrollY = y;
		this.requireScroll = true;
	}

	@Override
	public void onProgressChanged(WebView view, int progress) {
		Log.d(TAG, "Progress: " + progress);
		final DisplayLightNovelContentActivity caller = activityRef.get();
		if (caller != null) {
			ProgressBar progressBar = (ProgressBar) caller.findViewById(R.id.loadProgress);
			if (progressBar != null) {
				if (progress < 100) {
					progressBar.setVisibility(View.VISIBLE);
					progressBar.setProgress(progress);
				} else {
					progressBar.setVisibility(View.GONE);
					if(this.requireScroll) {
						view.scrollTo(0, this.scrollY);
						this.requireScroll = false;
					}
				}
			}
		}
	}
}
