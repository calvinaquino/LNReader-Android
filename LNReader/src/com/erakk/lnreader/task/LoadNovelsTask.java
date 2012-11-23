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

public class LoadNovelsTask extends AsyncTask<Void, ICallbackEventData, AsyncTaskResult<ArrayList<PageModel>>>  implements ICallbackNotifier {
	private static final String TAG = LoadNovelsTask.class.toString();
	private boolean refreshOnly = false;
	private boolean onlyWatched = false;
	private boolean alphOrder = false;
	public volatile IAsyncTaskOwner owner;
	
	public LoadNovelsTask(IAsyncTaskOwner owner, boolean refreshOnly, boolean onlyWatched, boolean alphOrder) {
		this.refreshOnly = refreshOnly;
		this.onlyWatched = onlyWatched;
		this.alphOrder = alphOrder;
		this.owner = owner;
	}
	
	public void onCallback(ICallbackEventData message) {
		publishProgress(message);
	}

	@Override
	protected void onPreExecute (){
		// executed on UI thread.
		owner.toggleProgressBar(true);
	}
	
	@Override
	protected AsyncTaskResult<ArrayList<PageModel>> doInBackground(Void... arg0) {
		// different thread from UI
		try {
			if (onlyWatched) {
				publishProgress(new CallbackEventData("Loading Watched List"));
				return new AsyncTaskResult<ArrayList<PageModel>>(NovelsDao.getInstance().getWatchedNovel());
			}
			else {
				if(refreshOnly) {
					publishProgress(new CallbackEventData("Refreshing Novel List"));
					return new AsyncTaskResult<ArrayList<PageModel>>(NovelsDao.getInstance().getNovelsFromInternet(this));
				}
				else {
					publishProgress(new CallbackEventData("Loading Novel List"));
					return new AsyncTaskResult<ArrayList<PageModel>>(NovelsDao.getInstance().getNovels(this, alphOrder));
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "Error when getting novel list: " + e.getMessage(), e);
			return new AsyncTaskResult<ArrayList<PageModel>>(e);
		}
	}
	
	@Override
	protected void onProgressUpdate (ICallbackEventData... values){
		owner.setMessageDialog(values[0]);
	}
	
	@Override
	protected void onPostExecute(AsyncTaskResult<ArrayList<PageModel>> result) {
		//executed on UI thread.
		owner.getResult(result);
	}    	 
}