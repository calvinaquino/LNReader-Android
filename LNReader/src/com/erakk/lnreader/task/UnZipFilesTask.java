package com.erakk.lnreader.task;

import java.io.IOException;

import android.os.AsyncTask;
import android.util.Log;

import com.erakk.lnreader.callback.CallbackEventData;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.helper.Util;

public class UnZipFilesTask extends AsyncTask<Void, ICallbackEventData, Void> implements ICallbackNotifier {
	private static final String TAG = UnZipFilesTask.class.toString();
	private final String zipName;
	private final String rootPath;
	private final ICallbackNotifier callback;
	private final String source;
	private boolean hasError = false;

	public UnZipFilesTask(String zipName, String rootPath, ICallbackNotifier callback, String source) {
		this.zipName = zipName;
		this.rootPath = rootPath;
		this.callback = callback;
		this.source = source;
	}

	@Override
	public void onCallback(ICallbackEventData message) {
		publishProgress(message);
	}

	@Override
	protected Void doInBackground(Void... arg0) {
		// unzip the files
		try {
			publishProgress(new CallbackEventData("UnZipping files..."));
			Util.unzipFiles(zipName, rootPath, this);
		} catch (IOException e) {
			Log.e(TAG, "Failed to unzip files.", e);
			publishProgress(new CallbackEventData("Failed to unzip files: " + e.getMessage()));
			hasError = true;
		}
		return null;
	}

	@Override
	protected void onProgressUpdate(ICallbackEventData... values) {
		Log.d(TAG, values[0].getMessage());
		if (callback != null)
			callback.onCallback(new CallbackEventData(values[0].getMessage(), source));
	}

	@Override
	protected void onPostExecute(Void result) {
		if (!hasError) {
			String message = "Completed unzipping " + zipName + " to: " + rootPath;
			Log.d(TAG, message);
			if (callback != null)
				callback.onCallback(new CallbackEventData(message, source));
		}
	}
}
