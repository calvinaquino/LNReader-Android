package com.erakk.lnreader.task;

import android.os.AsyncTask;
import android.util.Log;

import com.erakk.lnreader.callback.CallbackEventData;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.AsyncTaskResult;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.PageModel;

public class DownloadNovelDetailsTask extends AsyncTask<PageModel, ICallbackEventData, AsyncTaskResult<NovelCollectionModel>> implements ICallbackNotifier {
	public volatile IAsyncTaskOwner owner;
	
	public DownloadNovelDetailsTask(IAsyncTaskOwner owner) {
		this.owner = owner;
	}
	
	public void onCallback(ICallbackEventData message) {
		publishProgress(message);
	}

	@Override
	protected AsyncTaskResult<NovelCollectionModel> doInBackground(PageModel... params) {
		PageModel page = params[0];
		try {
			publishProgress(new CallbackEventData("Downloading chapter list..."));
			NovelCollectionModel novelCol = NovelsDao.getInstance().getNovelDetailsFromInternet(page, this);
			Log.d("DownloadNovelDetailsTask", "Downloaded: " + novelCol.getPage());				
			return new AsyncTaskResult<NovelCollectionModel>(novelCol);
		} catch (Exception e) {
			e.printStackTrace();
			Log.e("DownloadNovelDetailsTask", e.getClass().toString() + ": " + e.getMessage());
			return new AsyncTaskResult<NovelCollectionModel>(e);
		}
	}
	
	@Override
	protected void onProgressUpdate (ICallbackEventData... values){
		//executed on UI thread.
		owner.setMessageDialog(values[0]);
	}
	
	@Override
	protected void onPostExecute(AsyncTaskResult<NovelCollectionModel> result) {
		owner.getResult(result);
	}
}
