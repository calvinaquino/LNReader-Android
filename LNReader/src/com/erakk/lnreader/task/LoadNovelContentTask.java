package com.erakk.lnreader.task;

import android.os.AsyncTask;

import com.erakk.lnreader.callback.CallbackEventData;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.AsyncTaskResult;
import com.erakk.lnreader.model.NovelContentModel;
import com.erakk.lnreader.model.PageModel;

public class LoadNovelContentTask extends AsyncTask<PageModel, ICallbackEventData, AsyncTaskResult<NovelContentModel>> implements ICallbackNotifier{
	public volatile IAsyncTaskOwner owner;
	private boolean refresh;
	
	public LoadNovelContentTask(boolean isRefresh) {
		super();
		this.refresh = isRefresh;
	}
	
	public void onCallback(ICallbackEventData message) {
		publishProgress(message);
	}
	
	@Override
	protected void onPreExecute (){
		// executed on UI thread.
		owner.toggleProgressBar(true);
		if(this.refresh) {
			owner.setMessageDialog(new CallbackEventData("Refreshing..."));
		}
		else {
			owner.setMessageDialog(new CallbackEventData("Loading..."));
		}		
	}
	
	@Override
	protected AsyncTaskResult<NovelContentModel> doInBackground(PageModel... params) {
		try{
			PageModel p = params[0];
			if(refresh) {
				return new AsyncTaskResult<NovelContentModel>(NovelsDao.getInstance().getNovelContentFromInternet(p, this));
			}
			else {
				return new AsyncTaskResult<NovelContentModel>(NovelsDao.getInstance().getNovelContent(p, true, this));
			}
		}catch(Exception e) {
			return new AsyncTaskResult<NovelContentModel>(e);
		}
	}
	
	@Override
	protected void onProgressUpdate (ICallbackEventData... values){
		//executed on UI thread.
		owner.setMessageDialog(values[0]);
	}
			
	protected void onPostExecute(AsyncTaskResult<NovelContentModel> result) {
		owner.getResult(result);			
	}
}