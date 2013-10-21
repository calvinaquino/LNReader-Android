package com.erakk.lnreader.task;

import java.util.ArrayList;

import android.os.AsyncTask;
import android.util.Log;

import com.erakk.lnreader.callback.CallbackEventData;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.model.NovelContentModel;
import com.erakk.lnreader.model.PageModel;

public class RelinkImagesTask extends AsyncTask<Void, ICallbackEventData, Void> implements ICallbackNotifier {
	private static final String TAG = RelinkImagesTask.class.toString();
	private final String rootPath;
	private final ICallbackNotifier callback;
	private final String source;
	private final boolean hasError = false;

	public RelinkImagesTask(String rootPath, ICallbackNotifier callback, String source) {
		this.rootPath = rootPath;
		this.callback = callback;
		this.source = source;
	}

	@Override
	public void onCallback(ICallbackEventData message) {
		publishProgress(message);
	}

	@Override
	protected Void doInBackground(Void... params) {
		// get all contents
		ArrayList<PageModel> pages = NovelsDao.getInstance().getAllContentPageModel();
		int count = 1;
		for (PageModel page : pages) {
			String message = "Relink image in content: " + page.getPage() + " [" + count + " of " + pages.size() + "]";
			publishProgress(new CallbackEventData(message));

			try {
				// get the contents
				NovelContentModel content = NovelsDao.getInstance().getNovelContent(page, false, callback);

				if (content != null) {

					// replace the rootpath based on /project/
					// for now just replace the thumbs
					// file:///mnt/sdcard/test/project/images/thumb/c/c7/Accel_World_v01_262.jpg/84px-Accel_World_v01_262.jpg
					// file:///sdcard-ext/.bakareaderex/project/images/thumb/c/c7/Accel_World_v01_262.jpg/84px-Accel_World_v01_262.jpg
					content.setContent(content.getContent().replaceAll("file:///[\\w/\\.]+/project/images/thumb/", "file:///" + rootPath + "/project/images/thumb/"));
					NovelsDao.getInstance().updateNovelContent(content);
				}
			} catch (Exception e) {
				message = "Failed to relink image in content: " + page.getPage();
				Log.e(TAG, message, e);
				publishProgress(new CallbackEventData(message));
			}
			++count;
		}

		return null;
	}

	@Override
	protected void onProgressUpdate(ICallbackEventData... values) {
		Log.d(TAG, values[0].getMessage());
		if (callback != null)
			callback.onCallback(new CallbackEventData(values[0].getMessage(), source));
	}

	@Override
	protected void onPostExecute(Void result) {
		if (!hasError) {
			String message = "Completed relink images to: " + rootPath;
			Log.d(TAG, message);
			if (callback != null)
				callback.onCallback(new CallbackEventData(message, source));
		}
	}
}
