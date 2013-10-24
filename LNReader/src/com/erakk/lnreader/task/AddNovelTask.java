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

public class AddNovelTask extends AsyncTask<PageModel, ICallbackEventData, AsyncTaskResult<NovelCollectionModel>> implements ICallbackNotifier {
	public volatile IAsyncTaskOwner owner;

	public AddNovelTask(IAsyncTaskOwner displayLightNovelListActivity) {
		this.owner = displayLightNovelListActivity;
	}

	@Override
	public void onCallback(ICallbackEventData message) {
		onProgressUpdate(message);
	}

	@Override
	protected AsyncTaskResult<NovelCollectionModel> doInBackground(PageModel... params) {
		PageModel page = params[0];
		try {
			publishProgress(new CallbackEventData("Checking Novel: " + page.getPage()));
			page = NovelsDao.getInstance().getUpdateInfo(page, this);
			if (page.isMissing())
				throw new Exception("Novel doesn't exists: " + page.getPage());

			NovelCollectionModel novelCol = NovelsDao.getInstance().getNovelDetailsFromInternet(page, this);
			Log.d("AddNovelTask", "Downloaded: " + novelCol.getPage());
			return new AsyncTaskResult<NovelCollectionModel>(novelCol);
		} catch (Exception e) {
			Log.e("AddNovelTask", e.getClass().toString() + ": " + e.getMessage(), e);
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
		owner.getResult(result, NovelCollectionModel.class);
	}
}
