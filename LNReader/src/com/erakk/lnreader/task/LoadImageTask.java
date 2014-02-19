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
import com.erakk.lnreader.model.ImageModel;

public class LoadImageTask extends AsyncTask<Void, ICallbackEventData, AsyncTaskResult<ImageModel>> implements ICallbackNotifier {
	private static final String TAG = LoadImageTask.class.toString();
	public volatile IExtendedCallbackNotifier<AsyncTaskResult<ImageModel>> callback;
	private String url = "";
	private final boolean refresh;
	private final String taskId;

	public LoadImageTask(String url, boolean refresh, IExtendedCallbackNotifier<AsyncTaskResult<ImageModel>> callback) {
		this.callback = callback;
		this.refresh = refresh;
		this.taskId = this.toString();
		this.url = url;
	}

	@Override
	public void onProgressCallback(ICallbackEventData message) {
		publishProgress(message);
	}

	@Override
	protected void onPreExecute() {
		// executed on UI thread.
		callback.onProgressCallback(new CallbackEventData("Starting Download Image", this.taskId));
	}

	@Override
	protected AsyncTaskResult<ImageModel> doInBackground(Void... params) {
		Context ctx = LNReaderApplication.getInstance().getApplicationContext();
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
		callback.onProgressCallback(values[0]);
	}

	@Override
	protected void onPostExecute(AsyncTaskResult<ImageModel> result) {
		Context ctx = LNReaderApplication.getInstance().getApplicationContext();
		CallbackEventData message = new CallbackEventData(ctx.getResources().getString(R.string.load_image_task_complete), this.taskId);
		callback.onCompleteCallback(message, result);
		callback.downloadListSetup(this.taskId, null, 2, result.getError() != null ? true : false);
	}
}