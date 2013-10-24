package com.erakk.lnreader.task;

import android.os.AsyncTask;

import com.erakk.lnreader.callback.CallbackEventData;
import com.erakk.lnreader.callback.DownloadCallbackEventData;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.AsyncTaskResult;
import com.erakk.lnreader.model.ImageModel;

public class LoadImageTask extends AsyncTask<String, ICallbackEventData, AsyncTaskResult<ImageModel>> implements ICallbackNotifier {
	public volatile IAsyncTaskOwner owner;
	private String url = "";
	private final boolean refresh;
	private final String taskId;

	public LoadImageTask(boolean refresh, IAsyncTaskOwner owner) {
		this.owner = owner;
		this.refresh = refresh;
		this.taskId = this.toString();
	}

	@Override
	public void onCallback(ICallbackEventData message) {
		publishProgress(message);
	}

	@Override
	protected void onPreExecute() {
		// executed on UI thread.
		owner.toggleProgressBar(true);
		CallbackEventData message = new CallbackEventData();
		if (this.refresh) {
			message.setMessage("Refreshing...");
		}
		else {
			message.setMessage("Loading...");
		}
		owner.setMessageDialog(message);
	}

	@Override
	protected AsyncTaskResult<ImageModel> doInBackground(String... params) {
		this.url = params[0];
		ImageModel image = new ImageModel();
		image.setName(url);
		try {
			if (refresh) {
				return new AsyncTaskResult<ImageModel>(NovelsDao.getInstance().getImageModelFromInternet(image, this));
			}
			else {
				return new AsyncTaskResult<ImageModel>(NovelsDao.getInstance().getImageModel(image, this));
			}
		} catch (Exception e) {
			return new AsyncTaskResult<ImageModel>(e);
		}
	}

	@Override
	protected void onProgressUpdate(ICallbackEventData... values) {
		// executed on UI thread.
		owner.setMessageDialog(values[0]);
		if (values[0].getClass() == DownloadCallbackEventData.class) {
			DownloadCallbackEventData data = (DownloadCallbackEventData) values[0];
			owner.updateProgress(this.taskId, data.getPercentage(), 100, data.getMessage());
		}
	}

	@Override
	protected void onPostExecute(AsyncTaskResult<ImageModel> result) {
		owner.getResult(result, ImageModel.class);
		owner.downloadListSetup(this.taskId, null, 2, result.getError() != null ? true : false);
	}
}