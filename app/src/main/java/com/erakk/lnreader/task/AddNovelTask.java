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

public class AddNovelTask extends AsyncTask<PageModel, ICallbackEventData, AsyncTaskResult<NovelCollectionModel>> implements ICallbackNotifier {
	public volatile IExtendedCallbackNotifier<AsyncTaskResult<?>> owner;
	private String source;

	public AddNovelTask(IExtendedCallbackNotifier<AsyncTaskResult<?>> displayLightNovelListActivity, String source) {
		this.owner = displayLightNovelListActivity;
		this.source = source;
	}

	@Override
	public void onProgressCallback(ICallbackEventData message) {
		onProgressUpdate(message);
	}

	@Override
	protected AsyncTaskResult<NovelCollectionModel> doInBackground(PageModel... params) {
		Context ctx = LNReaderApplication.getInstance();
		PageModel page = params[0];
		try {
			publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.add_novel_task_check, page.getPage()), source));
			page = NovelsDao.getInstance().getUpdateInfo(page, this);
			if (page.isMissing()) {
				return new AsyncTaskResult<NovelCollectionModel>(null, NovelCollectionModel.class, new Exception(ctx.getResources().getString(R.string.add_novel_task_missing, page.getPage())));
			}

			NovelCollectionModel novelCol = NovelsDao.getInstance().getNovelDetailsFromInternet(page, this);
			Log.d("AddNovelTask", "Downloaded: " + novelCol.getPage());
			return new AsyncTaskResult<NovelCollectionModel>(novelCol, novelCol.getClass());
		} catch (Exception e) {
			Log.e("AddNovelTask", e.getClass().toString() + ": " + e.getMessage(), e);
			publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.add_novel_task_error, page.getPage(), e.getMessage()), source));
			return new AsyncTaskResult<NovelCollectionModel>(null, NovelCollectionModel.class, e);
		}
	}

	@Override
	protected void onProgressUpdate(ICallbackEventData... values) {
		// executed on UI thread.
		owner.onProgressCallback(values[0]);
	}

	@Override
	protected void onPostExecute(AsyncTaskResult<NovelCollectionModel> result) {
		Context ctx = LNReaderApplication.getInstance();
		owner.onCompleteCallback(new CallbackEventData(ctx.getResources().getString(R.string.add_novel_task_complete), source), result);
	}
}
