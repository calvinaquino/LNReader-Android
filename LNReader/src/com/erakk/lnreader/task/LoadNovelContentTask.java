package com.erakk.lnreader.task;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.erakk.lnreader.R;
import com.erakk.lnreader.callback.CallbackEventData;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.model.NovelContentModel;
import com.erakk.lnreader.model.PageModel;

public class LoadNovelContentTask extends AsyncTask<PageModel, ICallbackEventData, AsyncTaskResult<NovelContentModel>> implements ICallbackNotifier {
	private static final String TAG = LoadNovelContentTask.class.toString();
	public volatile IAsyncTaskOwner owner;
	private final boolean refresh;
	private String source;

	public LoadNovelContentTask(boolean isRefresh, IAsyncTaskOwner owner) {
		super();
		this.refresh = isRefresh;
		this.owner = owner;
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
	protected AsyncTaskResult<NovelContentModel> doInBackground(PageModel... params) {
		Context ctx = owner.getContext();
		try {
			PageModel p = params[0];
			if (refresh) {
				publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.load_novel_content_task_refreshing), source));
				return new AsyncTaskResult<NovelContentModel>(NovelsDao.getInstance().getNovelContentFromInternet(p, this));
			}
			else {
				publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.load_novel_content_task_loading), source));
				return new AsyncTaskResult<NovelContentModel>(NovelsDao.getInstance().getNovelContent(p, true, this));
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
		owner.setMessageDialog(values[0]);
	}

	@Override
	protected void onPostExecute(AsyncTaskResult<NovelContentModel> result) {
		owner.setMessageDialog(new CallbackEventData(owner.getContext().getResources().getString(R.string.load_novel_content_task_complete), source));
		owner.onGetResult(result, NovelContentModel.class);
	}
}