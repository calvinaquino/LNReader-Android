package com.erakk.lnreader.task;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import android.os.AsyncTask;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.erakk.lnreader.callback.CallbackEventData;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.helper.AsyncTaskResult;
import com.erakk.lnreader.helper.WebArchiveReader;

public class LoadWacTask extends AsyncTask<Void, ICallbackEventData, AsyncTaskResult<Boolean>> implements ICallbackNotifier {
	private static final String TAG = LoadWacTask.class.toString();
	private final WebView wv;
	private final String wacName;
	private final IAsyncTaskOwner owner;
	private final WebArchiveReader wr;

	public LoadWacTask(IAsyncTaskOwner owner, WebView wv, String wacName, final WebViewClient client) {
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
	protected AsyncTaskResult<Boolean> doInBackground(Void... arg0) {
		return new AsyncTaskResult<Boolean>(loadFromWac(this.wacName));
	}

	@Override
	protected void onProgressUpdate(ICallbackEventData... values) {
		owner.setMessageDialog(values[0]);
	}

	private boolean loadFromWac(String wacName) {
		Log.d(TAG, "Loading from WAC: " + wacName);
		publishProgress(new CallbackEventData("Loading from WAC: " + wacName));
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
			message = "Failed";
		}
		owner.setMessageDialog(new CallbackEventData(message));
		Toast.makeText(owner.getContext(), message, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onCallback(ICallbackEventData message) {
		publishProgress(message);
	}
}
