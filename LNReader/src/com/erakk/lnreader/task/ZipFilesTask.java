package com.erakk.lnreader.task;

import java.io.File;
import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.callback.CallbackEventData;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.helper.Util;

public class ZipFilesTask extends AsyncTask<Void, ICallbackEventData, Void> implements ICallbackNotifier {
	private static final String TAG = ZipFilesTask.class.toString();
	private final String zipName;
	private final String rootPath;
	private ICallbackNotifier callback;
	private String source;
	private boolean hasError = false;

	public static ZipFilesTask instance;

	public static ZipFilesTask getInstance() {
		return instance;
	}

	public static ZipFilesTask getInstance(String zipName, String rootPath, ICallbackNotifier callback, String source) {
		if (instance == null || instance.getStatus() == Status.FINISHED) {
			instance = new ZipFilesTask(zipName, rootPath, callback, source);
		}
		else {
			instance.setCallback(callback, source);
		}
		return instance;
	}

	public void setCallback(ICallbackNotifier callback, String source) {
		this.callback = callback;
		this.source = source;
	}

	private ZipFilesTask(String zipName, String rootPath, ICallbackNotifier callback, String source) {
		this.zipName = zipName;
		this.rootPath = rootPath;
		this.callback = callback;
		this.source = source;
	}

	@Override
	public void onProgressCallback(ICallbackEventData message) {
		publishProgress(message);
	}

	@Override
	protected Void doInBackground(Void... params) {
		Context ctx = LNReaderApplication.getInstance().getApplicationContext();
		// get thumb images
		publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.zip_files_task_get_files, rootPath)));

		// Java ref cheating using array
		Long totalSize[] = { 0L };
		List<File> filenames = Util.getListFiles(new File(rootPath), totalSize, this);

		// check the total file size
		long freeSpaceInBytes = Util.getFreeSpace(new File(zipName));
		Log.i(TAG, "Total File Size = " + totalSize[0] + ". Free Space: " + freeSpaceInBytes);
		if (freeSpaceInBytes < totalSize[0]) {
			String errorMessage = String.format("Not enough free space in %s (%s > %s)", rootPath, Util.humanReadableByteCount(freeSpaceInBytes, true), Util.humanReadableByteCount(totalSize[0], true));
			Log.e(TAG, errorMessage);
			publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.zip_files_task_error, errorMessage)));
			hasError = true;
			return null;
		}

		// zip the files
		try {
			publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.zip_files_task_zipping_files)));
			Util.zipFiles(filenames, zipName, rootPath, this);
		} catch (IOException e) {
			Log.e(TAG, "Failed to zip files.", e);
			publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.zip_files_task_error, e.getMessage())));
			hasError = true;
		}
		return null;
	}

	@Override
	protected void onProgressUpdate(ICallbackEventData... values) {
		Log.d(TAG, values[0].getMessage());
		if (callback != null)
			callback.onProgressCallback(new CallbackEventData(values[0].getMessage(), source));
	}

	@Override
	protected void onPostExecute(Void result) {
		if (!hasError) {
			String message = LNReaderApplication.getInstance().getApplicationContext().getResources().getString(R.string.zip_files_task_complete, rootPath, zipName);
			Log.d(TAG, message);
			if (callback != null)
				callback.onProgressCallback(new CallbackEventData(message, source));
		}
	}
}
