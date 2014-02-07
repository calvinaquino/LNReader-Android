package com.erakk.lnreader.task;

import java.util.List;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.callback.CallbackEventData;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.callback.ICompleteCallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.model.FindMissingModel;

public class DeleteMissingTask extends AsyncTask<Void, ICallbackEventData, Integer>{

	private static final String TAG = DeleteMissingTask.class.toString();
	private ICallbackNotifier callback;
	private ICompleteCallbackNotifier<Integer> completeCallback;
	private String source;
	private final List<FindMissingModel> items;
	private final String mode;
	private boolean hasError;

	public DeleteMissingTask(List<FindMissingModel> items, String mode, ICallbackNotifier callback, String source ) {
		this.items = items;
		this.mode = mode;
		this.setCallback(callback, source);
	}

	public void setCallback(ICallbackNotifier callback, String source) {
		this.callback = callback;

		if(callback instanceof ICompleteCallbackNotifier<?>) {
			// TODO merge ICallbackNotifier with ICompleteCallbackNotifier
			this.completeCallback = (ICompleteCallbackNotifier<Integer>) callback;
		}
		this.source = source;
	}

	@Override
	protected Integer doInBackground(Void... arg0) {
		Context ctx = LNReaderApplication.getInstance().getApplicationContext();
		try{
			int count = 0;
			if (items != null) {
				for (FindMissingModel missing : items) {
					count += NovelsDao.getInstance().deleteMissingItem(missing, mode);
					publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.task_delete_progress, count, items.size())));
				}
			}
			return count;
		}catch(Exception ex) {
			Log.e(TAG, "Failed to delete missing item.", ex);
			publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.task_delete_error, ex.getMessage())));
			hasError = true;
			return 0;
		}
	}

	@Override
	protected void onProgressUpdate(ICallbackEventData... values) {
		Log.d(TAG, values[0].getMessage());
		if (callback != null)
			callback.onProgressCallback(new CallbackEventData(values[0].getMessage(), source));
	}

	@Override
	protected void onPostExecute(Integer result) {
		if (!hasError) {
			String message = LNReaderApplication.getInstance().getApplicationContext().getResources().getString(R.string.task_delete_complete, items.size());
			Log.d(TAG, message);
			if (completeCallback != null)
				completeCallback.onCompleteCallback(new CallbackEventData(message, source), result);
		}
	}
}
