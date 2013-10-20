package com.erakk.lnreader.task;

import java.io.IOException;

import android.os.AsyncTask;
import android.util.Log;

import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.callback.CallbackEventData;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;

public class CopyDBTask extends AsyncTask<Void, ICallbackEventData, Void> implements ICallbackNotifier{

	private static final String TAG = CopyDBTask.class.toString();
	private final ICallbackNotifier callback;
	private final String source;
	private final boolean makeBackup;

	public CopyDBTask(boolean makeBackup, ICallbackNotifier callback, String source) {
		this.makeBackup = makeBackup;
		this.source = source;
		this.callback = callback;
	}

	@Override
	public void onCallback(ICallbackEventData message) {
		publishProgress(message);
	}

	@Override
	protected Void doInBackground(Void... params) {
		try {
			copyDB(makeBackup);
		} catch (IOException e) {
			String message;
			if(makeBackup) {
				message = "Error when backing up DB";
			}
			else {
				message = "Error when restoring DB";
			}
			publishProgress(new CallbackEventData(message));
			Log.e(TAG, message, e);
		}
		return null;
	}

	@Override
	protected void onProgressUpdate(ICallbackEventData... values) {
		Log.d(TAG, values[0].getMessage());
		if(callback != null)
			callback.onCallback(new CallbackEventData(values[0].getMessage(), source));
	}

	private void copyDB(boolean makeBackup) throws IOException {
		if (makeBackup)
			publishProgress(new CallbackEventData("Staring database backup, this might take some time..."));
		else
			publishProgress(new CallbackEventData("Starting database restore, this might take some time..."));

		String filePath = NovelsDao.getInstance().copyDB(makeBackup);
		if (filePath == "null") {
			publishProgress(new CallbackEventData(LNReaderApplication.getInstance().getApplicationContext().getResources().getString(R.string.database_not_found), source));
		} else {
			if (makeBackup)
				publishProgress(new CallbackEventData("Database backup created at " + filePath));
			else
				publishProgress(new CallbackEventData("Database backup restored!"));
		}
	}
}
