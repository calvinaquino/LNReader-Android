package com.erakk.lnreader.task;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.callback.CallbackEventData;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.callback.IExtendedCallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.PageModel;

public class LoadNovelDetailsTask extends AsyncTask<Void, ICallbackEventData, AsyncTaskResult<NovelCollectionModel>> implements ICallbackNotifier {
	private static final String TAG = LoadNovelDetailsTask.class.toString();
	private boolean refresh = false;
	public volatile IExtendedCallbackNotifier<AsyncTaskResult<?>> owner;
	private final String source;
	private final PageModel pageModel;

	public LoadNovelDetailsTask(PageModel p, boolean refresh, IExtendedCallbackNotifier<AsyncTaskResult<?>> owner) {
		super();
		this.owner = owner;
		this.refresh = refresh;
		this.pageModel = p;
		this.source = TAG + ":" + p.getPage();
	}

	@Override
	public void onProgressCallback(ICallbackEventData message) {
		publishProgress(message);
	}

	@Override
	protected void onPreExecute() {
		// executed on UI thread.
		owner.onProgressCallback(new CallbackEventData("", source));
	}

	@Override
	protected AsyncTaskResult<NovelCollectionModel> doInBackground(Void... arg0) {
		Context ctx = LNReaderApplication.getInstance().getApplicationContext();
		try {
			if (refresh) {
				publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.load_novel_detail_task_refreshing), source));
				NovelCollectionModel novelCol = NovelsDao.getInstance().getNovelDetailsFromInternet(pageModel, this);
				return new AsyncTaskResult<NovelCollectionModel>(novelCol);
			}
			else {
				publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.load_novel_detail_task_loading), source));
				NovelCollectionModel novelCol = NovelsDao.getInstance().getNovelDetails(pageModel, this);
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
		owner.onProgressCallback(values[0]);
	}

	@Override
	protected void onPostExecute(AsyncTaskResult<NovelCollectionModel> result) {
		Context ctx = LNReaderApplication.getInstance().getApplicationContext();
		CallbackEventData message = new CallbackEventData(ctx.getResources().getString(R.string.load_novel_detail_task_complete), source);
		owner.onCompleteCallback(message, result);
	}
}