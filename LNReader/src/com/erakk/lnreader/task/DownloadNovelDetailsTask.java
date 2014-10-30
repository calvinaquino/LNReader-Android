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
import com.erakk.lnreader.helper.BakaReaderException;
import com.erakk.lnreader.helper.Util;
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
		boolean exists = false;
		exists = owner.downloadListSetup(this.taskId, null, 0, false);
		if (exists)
			this.cancel(true);
	}

	@Override
	public void onProgressCallback(ICallbackEventData message) {
		publishProgress(message);
	}

	@Override
	protected AsyncTaskResult<NovelCollectionModel[]> doInBackground(PageModel... params) {
		Context ctx = owner.getContext();
		ArrayList<NovelCollectionModel> result = new ArrayList<NovelCollectionModel>();
		totalParts = params.length;

		ArrayList<Exception> exs = new ArrayList<Exception>();
		for (PageModel pageModel : params) {
			currentPart++;
			try {
				Log.i("DownloadNovelDetailsTask", "Downloading: " + pageModel.getPage());
				publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.download_novel_details_task_progress, pageModel.getTitle()), this.taskId));
				NovelCollectionModel novelCol = NovelsDao.getInstance().getNovelDetailsFromInternet(pageModel, this);
				result.add(novelCol);
			} catch (Exception e) {
				Log.e("DownloadNovelDetailsTask", "Failed to download novel details for " + pageModel.getPage() + ": " + e.getMessage(), e);
				publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.download_novel_details_task_error, pageModel.getPage(), e.getMessage()), this.taskId));
				exs.add(e);
			}
		}
		if (exs.size() == 1) {
			return new AsyncTaskResult<NovelCollectionModel[]>(new BakaReaderException(exs.get(0).getMessage(), BakaReaderException.DOWNLOADNOVELDETAIL_ERROR));
		}
		else if (exs.size() > 1) {
			String errors = Util.join(exs, "\n");
			return new AsyncTaskResult<NovelCollectionModel[]>(new BakaReaderException(errors, BakaReaderException.MULTIPLE_ERRORS));
		}
		return new AsyncTaskResult<NovelCollectionModel[]>(result.toArray(new NovelCollectionModel[result.size()]), NovelCollectionModel[].class);
	}

	@Override
	protected void onProgressUpdate(ICallbackEventData... values) {
		// executed on UI thread.
		owner.setMessageDialog(values[0]);
		owner.updateProgress(this.taskId, currentPart, totalParts, values[0].getMessage());
	}

	@Override
	protected void onPostExecute(AsyncTaskResult<NovelCollectionModel[]> result) {
		owner.setMessageDialog(new CallbackEventData(owner.getContext().getResources().getString(R.string.download_novel_details_task_complete), this.taskId));
		owner.onGetResult(result, NovelCollectionModel[].class);
		owner.downloadListSetup(this.taskId, null, 2, result.getError() != null ? true : false);
	}
}
