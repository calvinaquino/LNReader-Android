package com.erakk.lnreader.task;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.os.AsyncTask;
import android.util.Log;

import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.callback.CallbackEventData;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.callback.IExtendedCallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.Util;
import com.erakk.lnreader.model.ImageModel;
import com.erakk.lnreader.model.NovelContentModel;
import com.erakk.lnreader.model.PageModel;

public class RelinkImagesTask extends AsyncTask<Void, ICallbackEventData, Void> implements ICallbackNotifier {
	private static final String TAG = RelinkImagesTask.class.toString();
	private final String rootPath;
	private IExtendedCallbackNotifier<AsyncTaskResult<?>> callback;
	private String source;
	private final boolean hasError = false;
	private int updated;

	public static RelinkImagesTask instance;

	public static RelinkImagesTask getInstance() {
		return instance;
	}

	public static RelinkImagesTask getInstance(String rootPath, IExtendedCallbackNotifier<AsyncTaskResult<?>> callback, String source) {
		if (instance == null || instance.getStatus() == Status.FINISHED) {
			instance = new RelinkImagesTask(rootPath, callback, source);
		}
		else {
			instance.setCallback(callback, source);
		}
		return instance;
	}

	public void setCallback(IExtendedCallbackNotifier<AsyncTaskResult<?>> callback, String source) {
		this.callback = callback;
		this.source = source;
	}

	private RelinkImagesTask(String rootPath, IExtendedCallbackNotifier<AsyncTaskResult<?>> callback, String source) {
		this.rootPath = rootPath;
		this.callback = callback;
		this.source = source;
	}

	@Override
	public void onProgressCallback(ICallbackEventData message) {
		publishProgress(message);
	}

	@Override
	protected Void doInBackground(Void... params) {

		BufferedWriter writer = null;
		try {
			// create a temporary file
			String timeLog = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
			File logFile = new File(rootPath + "/RelinkImageTask_" + timeLog + ".log");

			// This will output the full path where the file will be written to...
			Log.d(TAG, "Writing to: " + logFile.getCanonicalPath());

			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile), "UTF-8"));

			writer.write("INF1. Using new image base path: " + rootPath);
			writer.newLine();
			writer.flush();

			processImageInContents(writer);
			processBigImage(writer);

			writer.write("-=EOL=-");
		} catch (Exception e) {
			Log.e(TAG, "Failed to write log file.", e);
		} finally {
			try {
				// Close the writer regardless of what happens...
				writer.close();
			} catch (Exception e) {
			}
		}
		return null;
	}

	private void writeLogFile(BufferedWriter writer, String key, RelinkImageData data) throws IOException {
		writer.write("Context: " + key);
		writer.newLine();
		writer.write("Type: " + data.Type);
		writer.newLine();
		writer.write("Original Name: " + data.OriginalName);
		writer.newLine();
		writer.write("Tested Duplicates: ");
		writer.newLine();
		for (String image : data.AlternateNames) {
			writer.write("\t" + image);
			writer.newLine();
		}
		writer.flush();
	}

	private void processBigImage(BufferedWriter writer) throws IOException {
		writer.write("BIG1. Start processing big images");
		ArrayList<ImageModel> images = NovelsDao.getInstance().getAllImages();

		int count = 1;
		int notReplacedCount = 0, replacedCount = 0, notFoundCount = 0;
		for (ImageModel image : images) {

			if (this.isCancelled()) {
				String message = LNReaderApplication.getInstance().getApplicationContext().getResources().getString(R.string.relink_task_cancelled);
				publishProgress(new CallbackEventData(message, source));
				return;
			}

			String message = LNReaderApplication.getInstance().getApplicationContext().getResources().getString(R.string.relink_task_progress2, image.getName(), count, images.size());
			publishProgress(new CallbackEventData(message, source));
			String oldPath = image.getPath();

			// skip if file exists
			if (new File(oldPath).exists()) {
				Log.d(TAG, "Skipping: " + oldPath);
				++notReplacedCount;
				continue;
			}

			String newPath = oldPath.replaceAll("[\\w/\\./!$%^&*()_+|~\\={}\\[\\]:\";'<>?,-]+/project/images/", rootPath + "/project/images/");
			Log.i(TAG, "Trying to update big image: " + oldPath + " => " + newPath);
			if (new File(newPath).exists()) {
				Log.i(TAG, "Updated: " + oldPath + " => " + newPath);
				image.setPath(newPath);
				NovelsDao.getInstance().insertImage(image);
				++updated;
				++replacedCount;
			}
			else {
				Log.w(TAG, "File doesn't exists: " + newPath);
				writeLogFile(writer, image.getName(), new RelinkImageData(oldPath, newPath, ImageType.Big));
				++notFoundCount;
			}
		}

		writer.write("BIG2. Big Image Summary: ");
		writer.newLine();
		writer.write("Total: " + images.size());
		writer.newLine();
		writer.write("Not replaced: " + notReplacedCount);
		writer.newLine();
		writer.write("Updated: " + replacedCount);
		writer.newLine();
		writer.write("Not Found: " + notFoundCount);
		writer.newLine();
		writer.newLine();
		writer.flush();

	}

	private void processImageInContents(BufferedWriter writer) throws IOException {
		writer.write("CTX1. Start processing images in contents");
		writer.newLine();
		// get all contents
		ArrayList<PageModel> pages = NovelsDao.getInstance().getAllContentPageModel();
		updated = 0;
		int count = 1;
		int notReplacedCount = 0, replacedCount = 0, notFoundCount = 0, totalImages = 0;
		for (PageModel page : pages) {
			if (this.isCancelled()) {
				String message = LNReaderApplication.getInstance().getApplicationContext().getResources().getString(R.string.relink_task_cancelled);
				publishProgress(new CallbackEventData(message, source));
				return;
			}

			String message = LNReaderApplication.getInstance().getApplicationContext().getResources().getString(R.string.relink_task_progress, page.getPage(), count, pages.size());
			publishProgress(new CallbackEventData(message, source));

			try {
				// get the contents
				NovelContentModel content = NovelsDao.getInstance().getNovelContent(page, false, this);
				boolean isModified = false;
				if (content != null) {

					// replace the rootpath based on /project/
					// for now just replace the thumbs
					// file:///mnt/sdcard/test/project/images/thumb/c/c7/Accel_World_v01_262.jpg/84px-Accel_World_v01_262.jpg
					// file:///sdcard-ext/.bakareaderex/project/images/thumb/c/c7/Accel_World_v01_262.jpg/84px-Accel_World_v01_262.jpg

					Document doc = Jsoup.parse(content.getContent());
					Elements imageElements = doc.select("img");
					for (Element image : imageElements) {
						++totalImages;
						String imgUrl = image.attr("src");
						if (imgUrl.startsWith("file:///") && imgUrl.contains("/project/images/thumb/")) {
							String mntImgUrl = imgUrl.replace("file:///", "");
							Log.d(TAG, "Found image in Content : " + imgUrl);

							if (!new File(mntImgUrl).exists()) {
								Log.d(TAG, "Old image doesn't exists/moved: " + mntImgUrl);
								String newUrl = imgUrl.replaceAll("file:///[\\w/\\./!$%^&*()_+|~\\={}\\[\\]:\";'<>?,-]+/project/images/thumb/", "file:///" + rootPath + "/project/images/thumb/");
								while (newUrl.startsWith("file:////")) {
									newUrl = newUrl.replace("file:////", "file:///");
								}

								List<String> newUrls = new ArrayList<String>();
								newUrls.add(newUrl);

								// check with encoded / decoded filename
								String oriFilename = newUrl.substring(newUrl.lastIndexOf("/project/images/thumb/") + 22);

								if (oriFilename.contains("%")) {
									String decodedFilename = java.net.URLDecoder.decode(oriFilename, "UTF-8");
									decodedFilename = newUrl.replace(oriFilename, decodedFilename);
									if (Util.isInList(decodedFilename, newUrls) == -1)
										newUrls.add(decodedFilename);

									String decodedFilenamePlus = decodedFilename.replace("+", "_");
									if (Util.isInList(decodedFilenamePlus, newUrls) == -1)
										newUrls.add(decodedFilenamePlus);
								}

								String encodedFilename = java.net.URLEncoder.encode(oriFilename, "UTF-8");
								encodedFilename = newUrl.replace(oriFilename, encodedFilename).replace("%2F", "/");
								if (Util.isInList(encodedFilename, newUrls) == -1)
									newUrls.add(encodedFilename);

								String encodedFilenamePlus = encodedFilename.replace("%2B", "_");
								if (Util.isInList(encodedFilenamePlus, newUrls) == -1)
									newUrls.add(encodedFilenamePlus);

								boolean isFound = false;
								for (String url : newUrls) {
									String mntNewUrl = url.replace("file:///", "");
									Log.d(TAG, "Trying to replace with " + mntNewUrl);

									if (new File(mntNewUrl).exists()) {
										Log.i(TAG, "Replace image: " + imgUrl + " ==> " + url);
										image.attr("src", url);
										++updated;
										++replacedCount;
										isFound = true;
										isModified = true;
										break;
									}
								}
								if (!isFound) {
									Log.w(TAG, "Image not found for " + imgUrl);
									++notFoundCount;
									writeLogFile(writer, content.getPage(), new RelinkImageData(imgUrl, newUrls, ImageType.Content));
								}
								String alt = image.attr("alt");
								if (Util.isStringNullOrEmpty(alt)) {
									image.attr("alt", image.attr("src"));
								}
							}
							else {
								Log.d(TAG, "Old Image Path exists: " + mntImgUrl);
								++notReplacedCount;
							}
						}
					}

					if (isModified) {
						content.setContent(doc.html());
						NovelsDao.getInstance().updateNovelContent(content, true);
					}

				}
			} catch (Exception e) {
				message = LNReaderApplication.getInstance().getApplicationContext().getResources().getString(R.string.relink_task_error, page.getPage());
				Log.e(TAG, message, e);
				publishProgress(new CallbackEventData(message, source));
			}
			++count;
		}

		writer.write("CTX2. Content Image Summary: ");
		writer.newLine();
		writer.write("Total Contents: " + pages.size());
		writer.newLine();
		writer.write("Total Images in content: " + totalImages);
		writer.newLine();
		writer.write("Not replaced: " + notReplacedCount);
		writer.newLine();
		writer.write("Updated: " + replacedCount);
		writer.newLine();
		writer.write("Not Found: " + notFoundCount);
		writer.newLine();
		writer.newLine();
		writer.flush();
	}

	@Override
	protected void onProgressUpdate(ICallbackEventData... values) {
		Log.d(TAG, values[0].getMessage());
		if (callback != null)
			callback.onProgressCallback(new CallbackEventData(values[0].getMessage(), source));
	}

	@Override
	protected void onPostExecute(Void result) {
		if (!hasError) {
			String message = LNReaderApplication.getInstance().getApplicationContext().getResources().getString(R.string.relink_task_complete, rootPath, updated);
			Log.i(TAG, message);
			if (callback != null)
				callback.onCompleteCallback(new CallbackEventData(message, source), null);
		}
	}

	public class RelinkImageData {
		public String OriginalName = "";
		public ArrayList<String> AlternateNames = new ArrayList<String>();
		public ImageType Type = ImageType.Big;

		public RelinkImageData(String original, String altName, ImageType type) {
			this.OriginalName = original;
			this.AlternateNames.add(altName);
			this.Type = type;
		}

		public RelinkImageData(String original, List<String> altNames, ImageType type) {
			this.OriginalName = original;
			for (String a : altNames) {
				AlternateNames.add(a);
			}
			this.Type = type;
		}
	}

	public enum ImageType {
		Big, Content
	}
}
