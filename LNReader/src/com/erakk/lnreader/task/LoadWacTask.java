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
import com.erakk.lnreader.helper.AsyncTaskResult;
import com.erakk.lnreader.helper.WebArchiveReader;

public class LoadWacTask extends AsyncTask<Void, ICallbackEventData, AsyncTaskResult<String>> implements ICallbackNotifier {
	private static final String TAG = LoadWacTask.class.toString();
	private final WebView wv;
	private final WebViewClient client;
	private final String wacName;
	private final IAsyncTaskOwner owner;

	public LoadWacTask(IAsyncTaskOwner owner, WebView wv, String wacName, WebViewClient client) {
		this.wv = wv;
		this.wacName = wacName;
		this.client = client;
		this.owner = owner;
	}

	@Override
	protected AsyncTaskResult<String> doInBackground(Void... arg0) {
		loadFromWac(this.wacName);
		return new AsyncTaskResult<String>("Completed");
	}

	@Override
	protected void onProgressUpdate(ICallbackEventData... values) {
		owner.setMessageDialog(values[0]);
	}

	private void loadFromWac(String wacName) {
		Log.d(TAG, "Loading from WAC: " + wacName);
		publishProgress(new CallbackEventData("Loading from WAC: " + wacName));
		try {
			WebArchiveReader wr = new WebArchiveReader() {

				@Override
				protected void onFinished(WebView webView) {
					webView.setWebViewClient(client);
					Log.d(TAG, "WAC loaded");
				}
			};

			FileInputStream is;

			is = new FileInputStream(wacName);

			if (wr.readWebArchive(is)) {
				wr.loadToWebView(wv);
			}
		} catch (FileNotFoundException e) {
			Log.e(TAG, "Failed to load saved web archive: " + wacName, e);
		}
	}

	@Override
	protected void onPostExecute(AsyncTaskResult<String> result) {
		owner.setMessageDialog(new CallbackEventData(result.getResult()));
	}

	@Override
	public void onCallback(ICallbackEventData message) {
		publishProgress(message);
	}
}
