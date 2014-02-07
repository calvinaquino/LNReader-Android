package com.erakk.lnreader.task;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.erakk.lnreader.R;
import com.erakk.lnreader.callback.CallbackEventData;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.PageModel;

public class AddNovelTask extends AsyncTask<PageModel, ICallbackEventData, AsyncTaskResult<NovelCollectionModel>> implements ICallbackNotifier {
	public volatile IAsyncTaskOwner owner;
	private String source;

	public AddNovelTask(IAsyncTaskOwner displayLightNovelListActivity) {
		this.owner = displayLightNovelListActivity;
	}

	@Override
	public void onProgressCallback(ICallbackEventData message) {
		onProgressUpdate(message);
	}

	@Override
	protected AsyncTaskResult<NovelCollectionModel> doInBackground(PageModel... params) {
		Context ctx = owner.getContext();
		PageModel page = params[0];
		try {
			publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.add_novel_task_check, page.getPage()), source));
			page = NovelsDao.getInstance().getUpdateInfo(page, this);
			if (page.isMissing()) {
				return new AsyncTaskResult<NovelCollectionModel>(new Exception(ctx.getResources().getString(R.string.add_novel_task_missing, page.getPage())));
			}

			NovelCollectionModel novelCol = NovelsDao.getInstance().getNovelDetailsFromInternet(page, this);
			Log.d("AddNovelTask", "Downloaded: " + novelCol.getPage());
			return new AsyncTaskResult<NovelCollectionModel>(novelCol);
		} catch (Exception e) {
			Log.e("AddNovelTask", e.getClass().toString() + ": " + e.getMessage(), e);
			publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.add_novel_task_error, page.getPage(), e.getMessage()), source));
			return new AsyncTaskResult<NovelCollectionModel>(e);
		}
	}

	@Override
	protected void onProgressUpdate(ICallbackEventData... values) {
		// executed on UI thread.
		owner.setMessageDialog(values[0]);
	}

	@Override
	protected void onPostExecute(AsyncTaskResult<NovelCollectionModel> result) {
		owner.setMessageDialog(new CallbackEventData(owner.getContext().getResources().getString(R.string.add_novel_task_complete), source));
		owner.onGetResult(result, NovelCollectionModel.class);
	}
}
