package com.erakk.lnreader.task;

import android.os.AsyncTask;
import android.util.Log;

import com.erakk.lnreader.callback.CallbackEventData;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.AsyncTaskResult;
import com.erakk.lnreader.model.NovelContentModel;
import com.erakk.lnreader.model.PageModel;

public class LoadNovelContentTask extends AsyncTask<PageModel, ICallbackEventData, AsyncTaskResult<NovelContentModel>> implements ICallbackNotifier {
	private static final String TAG = LoadNovelContentTask.class.toString();
	public volatile IAsyncTaskOwner owner;
	private final boolean refresh;

	public LoadNovelContentTask(boolean isRefresh, IAsyncTaskOwner owner) {
		super();
		this.refresh = isRefresh;
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
		if (this.refresh) {
			owner.setMessageDialog(new CallbackEventData("Refreshing..."));
		}
		else {
			owner.setMessageDialog(new CallbackEventData("Loading..."));
		}
	}

	@Override
	protected AsyncTaskResult<NovelContentModel> doInBackground(PageModel... params) {
		try {
			PageModel p = params[0];
			if (refresh) {
				return new AsyncTaskResult<NovelContentModel>(NovelsDao.getInstance().getNovelContentFromInternet(p, this));
			}
			else {
				return new AsyncTaskResult<NovelContentModel>(NovelsDao.getInstance().getNovelContent(p, true, this));
			}
		} catch (Exception e) {
			Log.e(TAG, "Error when getting novel content: " + e.getMessage(), e);
			return new AsyncTaskResult<NovelContentModel>(e);
		}
	}

	@Override
	protected void onProgressUpdate(ICallbackEventData... values) {
		// executed on UI thread.
		owner.setMessageDialog(values[0]);
	}

	@Override
	protected void onPostExecute(AsyncTaskResult<NovelContentModel> result) {
		owner.getResult(result, NovelContentModel.class);
	}
}