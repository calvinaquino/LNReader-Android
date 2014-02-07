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
import com.erakk.lnreader.model.NovelContentModel;
import com.erakk.lnreader.model.PageModel;

public class LoadNovelContentTask extends AsyncTask<Void, ICallbackEventData, AsyncTaskResult<NovelContentModel>> implements ICallbackNotifier {
	private static final String TAG = LoadNovelContentTask.class.toString();
	public volatile IExtendedCallbackNotifier<AsyncTaskResult<?>> owner;
	private final boolean refresh;
	private final String source;
	private final PageModel pageModel;

	public LoadNovelContentTask(PageModel p, boolean isRefresh, IExtendedCallbackNotifier<AsyncTaskResult<?>> owner) {
		super();
		this.refresh = isRefresh;
		this.owner = owner;
		this.pageModel = p;
		this.source = p.getPage();
	}

	@Override
	public void onProgressCallback(ICallbackEventData message) {
		publishProgress(message);
	}

	@Override
	protected void onPreExecute() {
		// executed on UI thread.
		owner.downloadListSetup(pageModel.getPage(), pageModel.getPage(), 0, false);
		owner.onProgressCallback(new CallbackEventData("", source));
	}

	@Override
	protected AsyncTaskResult<NovelContentModel> doInBackground(Void... params) {
		Context ctx = LNReaderApplication.getInstance().getApplicationContext();
		try {
			if (refresh) {
				publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.load_novel_content_task_refreshing), source));
				return new AsyncTaskResult<NovelContentModel>(NovelsDao.getInstance().getNovelContentFromInternet(pageModel, this));
			}
			else {
				publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.load_novel_content_task_loading), source));
				return new AsyncTaskResult<NovelContentModel>(NovelsDao.getInstance().getNovelContent(pageModel, true, this));
			}
		} catch (Exception e) {
			Log.e(TAG, "Error when getting novel content: " + e.getMessage(), e);
			publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.load_novel_content_task_error, e.getMessage()), source));
			return new AsyncTaskResult<NovelContentModel>(e);
		}
	}

	@Override
	protected void onProgressUpdate(ICallbackEventData... values) {
		// executed on UI thread.
		owner.onProgressCallback(values[0]);
	}

	@Override
	protected void onPostExecute(AsyncTaskResult<NovelContentModel> result) {
		Context ctx = LNReaderApplication.getInstance().getApplicationContext();
		CallbackEventData message = new CallbackEventData(ctx.getResources().getString(R.string.load_novel_content_task_complete), source);
		owner.onCompleteCallback(message, result);
		owner.downloadListSetup(pageModel.getPage(), pageModel.getPage(), 2, result.getError() != null ? true : false);
	}
}