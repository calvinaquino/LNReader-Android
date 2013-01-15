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

public class LoadOriginalsTask extends AsyncTask<Void, ICallbackEventData, AsyncTaskResult<ArrayList<PageModel>>>  implements ICallbackNotifier {
	private static final String TAG = LoadOriginalsTask.class.toString();
	private boolean refreshOnly = false;
	private boolean alphOrder = false;
	public volatile IAsyncTaskOwner owner;
	
	public LoadOriginalsTask(IAsyncTaskOwner owner, boolean refreshOnly, boolean alphOrder) {
		this.refreshOnly = refreshOnly;
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
			if(refreshOnly) {
				publishProgress(new CallbackEventData("Refreshing Original List"));
				return new AsyncTaskResult<ArrayList<PageModel>>(NovelsDao.getInstance().getOriginalFromInternet(this));
			}
			else {
				publishProgress(new CallbackEventData("Loading Original List"));
				return new AsyncTaskResult<ArrayList<PageModel>>(NovelsDao.getInstance().getOriginal(this, alphOrder));
			}
		} catch (Exception e) {
			Log.e(TAG, "Error when getting original list: " + e.getMessage(), e);
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