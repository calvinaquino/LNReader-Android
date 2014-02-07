package com.erakk.lnreader.task;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import android.os.AsyncTask;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.erakk.lnreader.callback.CallbackEventData;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.callback.IExtendedCallbackNotifier;
import com.erakk.lnreader.helper.WebArchiveReader;

public class LoadWacTask extends AsyncTask<Void, ICallbackEventData, AsyncTaskResult<Boolean>> implements ICallbackNotifier {
	private static final String TAG = LoadWacTask.class.toString();
	private final WebView wv;
	private final String wacName;
	private final IExtendedCallbackNotifier<AsyncTaskResult<?>> owner;
	private final WebArchiveReader wr;
	private String source;

	public LoadWacTask(IExtendedCallbackNotifier<AsyncTaskResult<?>> owner, WebView wv, String wacName, final WebViewClient client) {
		this.wv = wv;
		this.wacName = wacName;
		this.owner = owner;

		wr = new WebArchiveReader() {

			@Override
			protected void onFinished(WebView webView) {
				webView.setWebViewClient(client);
				Log.d(TAG, "WAC loaded");
			}
		};
	}

	@Override
	protected void onPreExecute() {
		// executed on UI thread.
		owner.downloadListSetup(wacName, wacName, 0, false);
		owner.onProgressCallback(new CallbackEventData("", source));
	}

	@Override
	protected AsyncTaskResult<Boolean> doInBackground(Void... arg0) {
		return new AsyncTaskResult<Boolean>(loadFromWac(this.wacName));
	}

	@Override
	protected void onProgressUpdate(ICallbackEventData... values) {
		owner.onProgressCallback(values[0]);
	}

	private boolean loadFromWac(String wacName) {
		Log.d(TAG, "Loading from WAC: " + wacName);
		publishProgress(new CallbackEventData("Loading from WAC: " + wacName, source));
		try {
			FileInputStream is;
			is = new FileInputStream(wacName);
			return wr.readWebArchive(is);
		} catch (FileNotFoundException e) {
			Log.e(TAG, "Failed to load saved web archive: " + wacName, e);
		}
		return false;
	}

	@Override
	protected void onPostExecute(AsyncTaskResult<Boolean> result) {
		String message = null;
		if (result.getResult()) {
			wr.loadToWebView(wv);
			message = "Load from: " + wacName;
		}
		else {
			message = "Load WAC Failed";
		}

		owner.onCompleteCallback(new CallbackEventData(message, source), result);
		owner.downloadListSetup(wacName, wacName, 2, result.getError() != null ? true : false);
	}

	@Override
	public void onProgressCallback(ICallbackEventData message) {
		publishProgress(message);
	}
}
