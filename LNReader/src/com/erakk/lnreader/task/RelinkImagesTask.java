package com.erakk.lnreader.task;

import java.io.File;
import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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
	private ICallbackNotifier callback;
	private String source;
	private final boolean hasError = false;

	public static RelinkImagesTask instance;

	public static RelinkImagesTask getInstance() {
		return instance;
	}

	public static RelinkImagesTask getInstance(String rootPath, ICallbackNotifier callback, String source) {
		if (instance == null || instance.getStatus() == Status.FINISHED) {
			instance = new RelinkImagesTask(rootPath, callback, source);
		}
		else {
			instance.setCallback(callback, source);
		}
		return instance;
	}

	public void setCallback(ICallbackNotifier callback, String source) {
		this.callback = callback;
		this.source = source;
	}

	private RelinkImagesTask(String rootPath, ICallbackNotifier callback, String source) {
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
					// content.setContent(content.getContent().replaceAll("file:///[\\w/\\.]+/project/images/thumb/",
					// "file:///" + rootPath + "/project/images/thumb/"));
					// NovelsDao.getInstance().updateNovelContent(content);

					Document doc = Jsoup.parse(content.getContent());
					Elements imageElements = doc.select("img");
					for (Element image : imageElements) {
						String imgUrl = image.attr("src");
						if (imgUrl.startsWith("file:///") && imgUrl.contains("/project/images/thumb/")) {
							String mntImgUrl = imgUrl.replace("file:///", "");
							Log.d(TAG, "Found image : " + imgUrl);
							if (!new File(mntImgUrl).exists()) {
								Log.d(TAG, "Old image doesn't exists/moved: " + mntImgUrl);
								String newUrl = imgUrl.replaceAll("file:///[\\w/\\./!$%^&*()_+|~\\={}\\[\\]:\";'<>?,-]+/project/images/thumb/", "file:///" + rootPath + "/project/images/thumb/");
								String mntNewUrl = newUrl.replace("file:///", "");
								Log.d(TAG, "Trying to replace with " + mntNewUrl);
								if (new File(mntNewUrl).exists()) {
									Log.d(TAG, "Replace image: " + imgUrl + " ==> " + newUrl);
									image.attr("src", newUrl);
								}
							}
						}
					}
					content.setContent(doc.html());
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
