package com.erakk.lnreader.task;

import java.util.ArrayList;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.callback.CallbackEventData;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.AsyncTaskResult;
import com.erakk.lnreader.model.PageModel;

public class LoadOriginalsTask extends AsyncTask<Void, ICallbackEventData, AsyncTaskResult<PageModel[]>> implements ICallbackNotifier {
	private static final String TAG = LoadOriginalsTask.class.toString();
	private boolean refreshOnly = false;
	private boolean alphOrder = false;
	public volatile IAsyncTaskOwner owner;
	private String source;

	public LoadOriginalsTask(IAsyncTaskOwner owner, boolean refreshOnly, boolean alphOrder) {
		this.refreshOnly = refreshOnly;
		this.alphOrder = alphOrder;
		this.owner = owner;
	}

	@Override
	public void onProgressCallback(ICallbackEventData message) {
		publishProgress(message);
	}

	@Override
	protected void onPreExecute() {
		// executed on UI thread.
		owner.toggleProgressBar(true);
	}

	@Override
	protected AsyncTaskResult<PageModel[]> doInBackground(Void... arg0) {
		Context ctx = LNReaderApplication.getInstance().getApplicationContext();
		// different thread from UI
		try {
			ArrayList<PageModel> novels = new ArrayList<PageModel>();
			if (refreshOnly) {
				publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.load_original_task_refreshing), source));
				novels = NovelsDao.getInstance().getOriginalFromInternet(this);
			}
			else {
				publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.load_original_task_loading), source));
				novels = NovelsDao.getInstance().getOriginal(this, alphOrder);
			}
			return new AsyncTaskResult<PageModel[]>(novels.toArray(new PageModel[novels.size()]));
		} catch (Exception e) {
			Log.e(TAG, "Error when getting original list: " + e.getMessage(), e);
			publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.load_original_task_error, e.getMessage()), source));
			return new AsyncTaskResult<PageModel[]>(e);
		}
	}

	@Override
	protected void onProgressUpdate(ICallbackEventData... values) {
		owner.setMessageDialog(values[0]);
	}

	@Override
	protected void onPostExecute(AsyncTaskResult<PageModel[]> result) {
		// executed on UI thread.
		owner.setMessageDialog(new CallbackEventData(owner.getContext().getResources().getString(R.string.load_original_task_complete), source));
		owner.onGetResult(result, PageModel[].class);
	}
}