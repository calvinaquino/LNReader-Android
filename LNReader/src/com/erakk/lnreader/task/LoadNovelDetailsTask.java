package com.erakk.lnreader.task;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.callback.CallbackEventData;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.PageModel;

public class LoadNovelDetailsTask extends AsyncTask<PageModel, ICallbackEventData, AsyncTaskResult<NovelCollectionModel>> implements ICallbackNotifier {
	private static final String TAG = LoadNovelDetailsTask.class.toString();
	private boolean refresh = false;
	public volatile IAsyncTaskOwner owner;
	private String source;

	public LoadNovelDetailsTask(boolean refresh, IAsyncTaskOwner owner) {
		super();
		this.owner = owner;
		this.refresh = refresh;
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
	protected AsyncTaskResult<NovelCollectionModel> doInBackground(PageModel... arg0) {
		Context ctx = LNReaderApplication.getInstance().getApplicationContext();
		PageModel page = arg0[0];
		try {
			if (refresh) {
				publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.load_novel_detail_task_refreshing), source));
				NovelCollectionModel novelCol = NovelsDao.getInstance().getNovelDetailsFromInternet(page, this);
				return new AsyncTaskResult<NovelCollectionModel>(novelCol);
			}
			else {
				publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.load_novel_detail_task_loading), source));
				NovelCollectionModel novelCol = NovelsDao.getInstance().getNovelDetails(page, this);
				return new AsyncTaskResult<NovelCollectionModel>(novelCol);
			}
		} catch (Exception e) {
			Log.e(TAG, e.getClass().toString() + ": " + e.getMessage(), e);
			publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.load_novel_detail_task_error, e.getMessage()), source));
			return new AsyncTaskResult<NovelCollectionModel>(e);
		}
	}

	@Override
	protected void onProgressUpdate(ICallbackEventData... values) {
		owner.setMessageDialog(values[0]);
	}

	@Override
	protected void onPostExecute(AsyncTaskResult<NovelCollectionModel> result) {
		owner.setMessageDialog(new CallbackEventData(owner.getContext().getResources().getString(R.string.load_novel_detail_task_complete), source));
		owner.onGetResult(result, NovelCollectionModel.class);
	}
}