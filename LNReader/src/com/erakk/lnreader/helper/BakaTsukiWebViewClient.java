package com.erakk.lnreader.helper;

import java.lang.ref.WeakReference;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.activity.DisplayImageActivity;
import com.erakk.lnreader.activity.DisplayLightNovelContentActivity;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.model.PageModel;

@TargetApi(11)
public class BakaTsukiWebViewClient extends WebViewClient {
	private static final String TAG = BakaTsukiWebViewClient.class.toString();
	protected WeakReference<DisplayLightNovelContentActivity> activityRef;

	public BakaTsukiWebViewClient(DisplayLightNovelContentActivity caller) {
		super();
		this.activityRef = new WeakReference<DisplayLightNovelContentActivity>(caller);
	}

	@Override
	public boolean shouldOverrideUrlLoading(WebView view, String url) {
		final DisplayLightNovelContentActivity caller = activityRef.get();
		if (caller == null)
			return false;

		caller.setLastReadState();
		Log.d(TAG, "Handling: " + url);

		// if image file
		if (url.contains("title=File:")) {
			Intent intent = new Intent(caller, DisplayImageActivity.class);
			intent.putExtra(Constants.EXTRA_IMAGE_URL, url);
			caller.startActivity(intent);
		} else {
			// get the title from url
			boolean isInternalPages = false;
			if (url.contains("/project/index.php?title=")) {
				String titles[] = url.split("title=", 2);
				if (titles.length == 2 && !(titles[1].length() == 0)) {
					// check if have inside db
					NovelsDao dao = NovelsDao.getInstance(caller);
					try {
						// split anchor text
						String[] titles2 = titles[1].split("#", 2);

						// check if load different page.
						synchronized (caller.content) {
							String currentPage = caller.content.getPage(); // caller.getIntent().getStringExtra(Constants.EXTRA_PAGE);
							if (!currentPage.equalsIgnoreCase(titles2[0])) {
								PageModel tempPage = new PageModel();
								tempPage.setPage(titles2[0]);
								PageModel pageModel = dao.getPageModel(tempPage, null);
								caller.jumpTo(pageModel);
								Log.d(TAG, "Loading : " + pageModel.getPage());
								// Toast.makeText(context, "Loading: " + titles[1], Toast.LENGTH_SHORT).show();
							} else {
								Log.d(TAG, "Already loaded");
							}
							// navigate to the anchor if exist.
							if (titles2.length == 2) {
								view.loadUrl("#" + titles2[1]);
							}
						}

						isInternalPages = true;
					} catch (Exception e) {
						Log.e(TAG, "Failed to load: " + titles[1], e);
					}
				}
			}

			if (!isInternalPages) {
				boolean useInternalWebView = PreferenceManager.getDefaultSharedPreferences(caller.getApplicationContext()).getBoolean(Constants.PREF_USE_INTERNAL_WEBVIEW, false);
				if (useInternalWebView) {
					PageModel pageModel = new PageModel();
					pageModel.setPage(url);
					PageModel temp = pageModel;
					try {
						temp = NovelsDao.getInstance().getExistingPageModel(pageModel, null);
					} catch (Exception e) {
						Log.e(TAG, "Failed to get pageModel: " + url, e);
					}
					if (temp != null)
						pageModel = temp;
					caller.loadExternalUrl(pageModel);

				} else {
					// use default handler.
					caller.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
				}
			}
		}
		return true;
	}
}
