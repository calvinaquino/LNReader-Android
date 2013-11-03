package com.erakk.lnreader.task;

import java.util.ArrayList;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.erakk.lnreader.R;
import com.erakk.lnreader.callback.CallbackEventData;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.AsyncTaskResult;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.PageModel;

public class DownloadNovelDetailsTask extends AsyncTask<PageModel, ICallbackEventData, AsyncTaskResult<NovelCollectionModel[]>> implements ICallbackNotifier {
	public volatile IAsyncTaskOwner owner;
	private int currentPart = 0;
	private int totalParts = 0;
	private final String taskId;

	public DownloadNovelDetailsTask(IAsyncTaskOwner owner) {
		this.owner = owner;
		this.taskId = this.toString();
	}

	@Override
	protected void onPreExecute() {
		// executed on UI thread.
		// owner.toggleProgressBar(true);
		boolean exists = false;
		exists = owner.downloadListSetup(this.taskId, null, 0, false);
		if (exists)
			this.cancel(true);
	}

	@Override
	public void onCallback(ICallbackEventData message) {
		publishProgress(message);
	}

	@Override
	protected AsyncTaskResult<NovelCollectionModel[]> doInBackground(PageModel... params) {
		Context ctx = owner.getContext();
		ArrayList<NovelCollectionModel> result = new ArrayList<NovelCollectionModel>();
		totalParts = params.length;
		for (PageModel pageModel : params) {
			currentPart++;
			try {
				publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.download_novel_details_task_progress, pageModel.getTitle())));
				NovelCollectionModel novelCol = NovelsDao.getInstance().getNovelDetailsFromInternet(pageModel, this);
				Log.d("DownloadNovelDetailsTask", "Downloaded: " + novelCol.getPage());
				result.add(novelCol);
			} catch (Exception e) {
				Log.e("DownloadNovelDetailsTask", "Failed to download novel details for " + pageModel.getPage() + ": " + e.getMessage(), e);
				publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.download_novel_details_task_error, pageModel.getPage(), e.getMessage())));
				return new AsyncTaskResult<NovelCollectionModel[]>(e);
			}
		}
		return new AsyncTaskResult<NovelCollectionModel[]>(result.toArray(new NovelCollectionModel[result.size()]));
	}

	@Override
	protected void onProgressUpdate(ICallbackEventData... values) {
		// executed on UI thread.
		owner.setMessageDialog(values[0]);
		owner.updateProgress(this.taskId, currentPart, totalParts, values[0].getMessage());
	}

	@Override
	protected void onPostExecute(AsyncTaskResult<NovelCollectionModel[]> result) {
		owner.setMessageDialog(new CallbackEventData(owner.getContext().getResources().getString(R.string.download_novel_details_task_complete)));
		owner.onGetResult(result, NovelCollectionModel[].class);
		owner.downloadListSetup(this.taskId, null, 2, result.getError() != null ? true : false);
	}
}
