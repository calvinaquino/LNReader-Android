package com.erakk.lnreader.task;

import java.util.ArrayList;

import android.os.AsyncTask;
import android.util.Log;

import com.erakk.lnreader.callback.CallbackEventData;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.AsyncTaskResult;
import com.erakk.lnreader.model.PageModel;

public class LoadTeasersTask extends AsyncTask<Void, ICallbackEventData, AsyncTaskResult<PageModel[]>> implements ICallbackNotifier {
	private static final String TAG = LoadTeasersTask.class.toString();
	private boolean refreshOnly = false;
	private boolean alphOrder = false;
	public volatile IAsyncTaskOwner owner;

	public LoadTeasersTask(IAsyncTaskOwner owner, boolean refreshOnly, boolean alphOrder) {
		this.refreshOnly = refreshOnly;
		this.alphOrder = alphOrder;
		this.owner = owner;
	}

	@Override
	public void onCallback(ICallbackEventData message) {
		publishProgress(message);
	}

	@Override
	protected void onPreExecute() {
		// executed on UI thread.
		owner.toggleProgressBar(true);
	}

	@Override
	protected AsyncTaskResult<PageModel[]> doInBackground(Void... arg0) {
		// different thread from UI
		try {
			ArrayList<PageModel> novels = new ArrayList<PageModel>();
			if (refreshOnly) {
				publishProgress(new CallbackEventData("Refreshing Teaser List"));
				novels = NovelsDao.getInstance().getTeaserFromInternet(this);
			}
			else {
				publishProgress(new CallbackEventData("Loading Teaser List"));
				novels = NovelsDao.getInstance().getTeaser(this, alphOrder);
			}
			return new AsyncTaskResult<PageModel[]>(novels.toArray(new PageModel[novels.size()]));
		} catch (Exception e) {
			Log.e(TAG, "Error when getting teaser list: " + e.getMessage(), e);
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
		owner.getResult(result, PageModel[].class);
	}
}