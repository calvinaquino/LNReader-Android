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

public class LoadTeasersTask extends AsyncTask<Void, ICallbackEventData, AsyncTaskResult<ArrayList<PageModel>>>  implements ICallbackNotifier {
	private static final String TAG = LoadTeasersTask.class.toString();
	private boolean refreshOnly = false;
	public volatile IAsyncTaskOwner owner;
	
	public LoadTeasersTask(IAsyncTaskOwner owner, boolean refreshOnly) {
		this.refreshOnly = refreshOnly;
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
				publishProgress(new CallbackEventData("Refreshing Teaser List"));
				return new AsyncTaskResult<ArrayList<PageModel>>(NovelsDao.getInstance().getTeaserFromInternet(this));
			}
			else {
				publishProgress(new CallbackEventData("Loading Teaser List"));
				return new AsyncTaskResult<ArrayList<PageModel>>(NovelsDao.getInstance().getTeaser(this));
			}
		} catch (Exception e) {
			Log.e(TAG, "Error when getting teaser list: " + e.getMessage(), e);
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