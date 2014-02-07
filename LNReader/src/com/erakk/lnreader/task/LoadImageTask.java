package com.erakk.lnreader.task;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.erakk.lnreader.R;
import com.erakk.lnreader.callback.CallbackEventData;
import com.erakk.lnreader.callback.DownloadCallbackEventData;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.AsyncTaskResult;
import com.erakk.lnreader.model.ImageModel;

public class LoadImageTask extends AsyncTask<String, ICallbackEventData, AsyncTaskResult<ImageModel>> implements ICallbackNotifier {
	private static final String TAG = LoadImageTask.class.toString();
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
	public void onProgressCallback(ICallbackEventData message) {
		publishProgress(message);
	}

	@Override
	protected void onPreExecute() {
		// executed on UI thread.
		owner.toggleProgressBar(true);
	}

	@Override
	protected AsyncTaskResult<ImageModel> doInBackground(String... params) {
		Context ctx = owner.getContext();
		this.url = params[0];
		ImageModel image = new ImageModel();
		image.setName(url);
		try {
			if (refresh) {
				publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.load_image_task_refreshing), this.taskId));
				return new AsyncTaskResult<ImageModel>(NovelsDao.getInstance().getImageModelFromInternet(image, this));
			}
			else {
				publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.load_image_task_loading), this.taskId));
				return new AsyncTaskResult<ImageModel>(NovelsDao.getInstance().getImageModel(image, this));
			}
		} catch (Exception e) {
			Log.e(TAG, "Error when getting image: " + e.getMessage(), e);
			publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.load_image_task_error, e.getMessage()), this.taskId));
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
		else if (values[0].getClass() == CallbackEventData.class) {
			owner.setMessageDialog(values[0]);
		}
	}

	@Override
	protected void onPostExecute(AsyncTaskResult<ImageModel> result) {
		owner.onGetResult(result, ImageModel.class);
		owner.downloadListSetup(this.taskId, null, 2, result.getError() != null ? true : false);
		owner.setMessageDialog(new CallbackEventData(owner.getContext().getResources().getString(R.string.load_image_task_complete), this.taskId));
	}
}