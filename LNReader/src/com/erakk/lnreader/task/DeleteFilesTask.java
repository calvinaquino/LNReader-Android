package com.erakk.lnreader.task;

import java.io.File;
import java.util.ArrayList;

import android.os.AsyncTask;
import android.util.Log;

import com.erakk.lnreader.callback.CallbackEventData;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.IExtendedCallbackNotifier;

public class DeleteFilesTask extends AsyncTask<Void, ICallbackEventData, AsyncTaskResult<Integer>> {
	private static final String TAG = DeleteFilesTask.class.toString();
	private final IExtendedCallbackNotifier<AsyncTaskResult<?>> owner;
	private final String source;
	private final String filename;

	public DeleteFilesTask(IExtendedCallbackNotifier<AsyncTaskResult<?>> owner, String filename) {
		this.owner = owner;
		source = TAG + ":" + filename;
		this.filename = filename;
	}

	@Override
	protected AsyncTaskResult<Integer> doInBackground(Void... params) {
		try{
			Log.d(TAG, "Start deleting: " + filename);
			File f = new File(filename);
			int count = 0;
			if(f.exists()) {
				count = delete(f, count);
			}

			return new AsyncTaskResult<Integer>(count);
		}catch(Exception e) {
			Log.e(TAG, "Failed to delete: " + filename, e);
			return new AsyncTaskResult<Integer>(e);
		}
	}

	private int delete(File f, int count) {
		ArrayList<File> list = new ArrayList<File>();
		ArrayList<File> dirs = new ArrayList<File>();

		if(f.isFile())
			list.add(f);
		else if(f.isDirectory()) {
			publishProgress(new CallbackEventData("Getting file list...", source));
			addAll(dirs, f.listFiles());
			for(int i = 0; i < dirs.size(); ++i) {
				File temp = dirs.get(i);
				if(temp.isDirectory()) {
					list.add(temp);
					addAll(dirs,temp.listFiles());	// dirs keep appeding until reach files
				}
				else
					list.add(temp);
			}
		}

		for (File file : list) {
			publishProgress(new CallbackEventData("Deleting: " + file.getName(), source));
			if(file.delete()) ++count;
		}
		return count;
	}

	private void addAll(ArrayList<File> dirs, File[] fs) {
		for (File file : fs) {
			dirs.add(file);
		}
	}

	@Override
	protected void onProgressUpdate(ICallbackEventData... values) {
		//Log.d(TAG, values[0].getMessage());
		if(owner != null)
			owner.onProgressCallback(values[0]);
	}

	@Override
	protected void onPostExecute(AsyncTaskResult<Integer> result) {
		if(owner != null){
			CallbackEventData message = new CallbackEventData("Delete completed", source);
			owner.onCompleteCallback(message, result);
		}
	}
}
