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
import com.erakk.lnreader.model.NovelContentModel;
import com.erakk.lnreader.model.PageModel;

public class DownloadNovelContentTask extends AsyncTask<Void, ICallbackEventData, AsyncTaskResult<NovelContentModel[]>> implements ICallbackNotifier {
	private static final String TAG = DownloadNovelContentTask.class.toString();
	private final PageModel[] chapters;
	public volatile IAsyncTaskOwner owner;
	private int currentChapter = 0;
	private final String taskId;

	public DownloadNovelContentTask(PageModel[] chapters, IAsyncTaskOwner owner) {
		this.chapters = chapters;
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
	public void onProgressCallback(ICallbackEventData message) {
		publishProgress(message);
	}

	@Override
	protected AsyncTaskResult<NovelContentModel[]> doInBackground(Void... params) {
		Context ctx = owner.getContext();
		ArrayList<Exception> exceptionList = new ArrayList<Exception>();
		try {
			NovelContentModel[] contents = new NovelContentModel[chapters.length];
			for (int i = 0; i < chapters.length; ++i) {
				currentChapter++;
				NovelContentModel oldContent = NovelsDao.getInstance().getNovelContent(chapters[i], false, null);
				String message = ctx.getResources().getString(R.string.download_novel_content_task_progress, chapters[i].getTitle());
				if (oldContent != null) {
					message = ctx.getResources().getString(R.string.download_novel_content_task_update, chapters[i].getTitle());
				}
				Log.i(TAG, message);
				publishProgress(new CallbackEventData(message, this.taskId));

				try {
					NovelContentModel temp = NovelsDao.getInstance().getNovelContentFromInternet(chapters[i], this);
					contents[i] = temp;
				} catch (Exception e) {
					Log.e(TAG, String.format("Error when downloading: %s", chapters[i].getTitle()), e);
					publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.download_novel_content_task_error, chapters[i].getTitle(), e.getMessage()), this.taskId));
					exceptionList.add(e);
				}

			}
			if (exceptionList.size() > 0) {
				return new AsyncTaskResult<NovelContentModel[]>(exceptionList.get(exceptionList.size() - 1));
			}
			return new AsyncTaskResult<NovelContentModel[]>(contents);
		} catch (Exception e) {
			Log.e(TAG, String.format("Error when downloading: %s", chapters[currentChapter - 1].getPage(), e));
			publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.download_novel_content_task_error, chapters[currentChapter - 1].getPage(), e.getMessage()), this.taskId));
			return new AsyncTaskResult<NovelContentModel[]>(e);
		}
	}

	@Override
	protected void onProgressUpdate(ICallbackEventData... values) {
		// executed on UI thread.
		owner.setMessageDialog(values[0]);
		owner.updateProgress(this.taskId, currentChapter, chapters.length, values[0].getMessage());
	}

	@Override
	protected void onPostExecute(AsyncTaskResult<NovelContentModel[]> result) {
		owner.setMessageDialog(new CallbackEventData(owner.getContext().getResources().getString(R.string.download_novel_content_task_complete), this.taskId));
		owner.onGetResult(result, NovelContentModel[].class);
		owner.downloadListSetup(this.taskId, null, 2, result.getError() != null ? true : false);
	}
}