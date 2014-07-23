package com.erakk.lnreader.task;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Date;

import javax.net.ssl.HttpsURLConnection;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.callback.CallbackEventData;
import com.erakk.lnreader.callback.DownloadCallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.helper.Util;
import com.erakk.lnreader.model.ImageModel;

public class DownloadFileTask extends AsyncTask<Void, Integer, AsyncTaskResult<ImageModel>> {
	private static final String TAG = DownloadFileTask.class.toString();
	private ICallbackNotifier notifier = null;
	private String source;
	private final URL url;

	public DownloadFileTask(URL url, ICallbackNotifier notifier) {
		this.notifier = notifier;
		this.url = url;
	}

	@Override
	protected AsyncTaskResult<ImageModel> doInBackground(Void... urls) {
		try {
			return new AsyncTaskResult<ImageModel>(downloadImage(), ImageModel.class);
		} catch (Exception e) {
			return new AsyncTaskResult<ImageModel>(e);
		}
	}

	public ImageModel downloadImage() throws Exception {
		return downloadImage(this.url);
	}

	@SuppressLint("DefaultLocale")
	public ImageModel downloadImage(URL imageUrl) throws Exception {
		if (imageUrl.getProtocol().equalsIgnoreCase("file"))
			return null;

		Log.d(TAG, "Start Downloading: " + imageUrl.toString());
		InputStream input = null;
		OutputStream output = null;
		String filepath = UIHelper.getImageRoot(LNReaderApplication.getInstance().getApplicationContext()) + imageUrl.getFile();
		@SuppressWarnings("deprecation")
		String decodedUrl = Util.sanitizeFilename(URLDecoder.decode(filepath));
		Log.d(TAG, "Saving to: " + decodedUrl);

		// create dir if not exist
		String path = decodedUrl.substring(0, decodedUrl.lastIndexOf("/"));
		File cacheDir = new File(path);

		if (cacheDir.mkdirs() || cacheDir.isDirectory()) {
			Log.d(TAG, "Path to: " + path);
		} else {
			Log.e(TAG, "Failed to create Path: " + path);
		}

		File tempFilename = new File(decodedUrl + ".!tmp");
		File decodedFile = new File(decodedUrl);

		Log.d(TAG, "Start downloading image: " + imageUrl);

		// remove temp file if exist
		if (tempFilename.exists()) {
			tempFilename.delete();
		}

		HttpURLConnection connection = null;
		if (imageUrl.getProtocol().toLowerCase().equals("https")) {
			if (UIHelper.getUseAppKeystore(LNReaderApplication.getInstance().getApplicationContext())) {
				HttpsURLConnection.setDefaultSSLSocketFactory(Util.initMySecureSSL());
				Log.w(TAG, "Using my https cert key store");

			}
			HttpsURLConnection https = (HttpsURLConnection) url.openConnection();
			https.setHostnameVerifier(Util.DO_NOT_VERIFY);
			connection = https;
		}
		else {
			connection = (HttpURLConnection) imageUrl.openConnection();
		}

		int timeout = UIHelper.getIntFromPreferences(Constants.PREF_TIMEOUT, 60) * 1000;
		connection.setConnectTimeout(timeout);
		connection.setReadTimeout(timeout);

		connection.connect();

		// this will be useful so that you can show a typical 0-100% progress bar
		// I'm not using it AT them moment, but don't remove, might be useful for real.
		int fileLength = connection.getContentLength();

		// check saved filesize if already downloaded
		boolean download = true;
		if (decodedFile.exists()) {
			if (decodedFile.length() == fileLength) {
				download = false;
				Log.d(TAG, "File exists: " + decodedUrl + " Size: " + fileLength);
			} else {
				decodedFile.delete();
				Log.d(TAG, "File exists but different size: " + decodedUrl + " " + decodedFile.length() + "!=" + fileLength);
			}
		}
		if (download) {
			for (int i = 0; i < UIHelper.getIntFromPreferences(Constants.PREF_RETRY, 3); ++i) {
				try {
					boolean increase_retry = PreferenceManager.getDefaultSharedPreferences(LNReaderApplication.getInstance().getApplicationContext()).getBoolean(Constants.PREF_INCREASE_RETRY, false);
					if (increase_retry) {
						timeout = timeout * (i + 1);
						connection.setConnectTimeout(timeout);
						connection.setReadTimeout(timeout);
					}

					// download the file
					input = new BufferedInputStream(imageUrl.openStream());
					output = new FileOutputStream(tempFilename);

					byte data[] = new byte[1024];
					long downloaded = 0;
					int count;
					while ((count = input.read(data)) != -1) {
						downloaded += count;
						// publishing the progress....
						int progress = (int) (downloaded * 100 / fileLength);
						publishProgress(progress);

						// via notifier, C# style :)
						if (notifier != null) {
							DownloadCallbackEventData message = new DownloadCallbackEventData(null, downloaded, fileLength, source);
							message.setUrl(imageUrl.toString());
							message.setFilePath(decodedUrl);
							notifier.onProgressCallback(message);
						}
						// Log.d(TAG, "Downloading: " + url + " " + progress + "%");
						output.write(data, 0, count);
					}
					Log.d(TAG, "Filesize: " + downloaded);
					if (downloaded > 0)
						break;
				} catch (Exception ex) {
					Log.e(TAG, ex.getMessage(), ex);
					if (i > UIHelper.getIntFromPreferences(Constants.PREF_RETRY, 3)) {
						Log.e(TAG, "Failed to download: " + imageUrl.toString(), ex);
						throw ex;
					} else {
						if (notifier != null) {
							notifier.onProgressCallback(new CallbackEventData("Downloading: " + imageUrl + "\nRetry: " + i + "x", source));
						}
					}
				} finally {
					if (output != null) {
						output.flush();
						output.close();
					}
					if (input != null) {
						input.close();
					}
				}
			}
			// Rename file
			tempFilename.renameTo(decodedFile);
			Log.d(TAG, "Downloading image complete, saved to: " + decodedUrl);
		}

		ImageModel image = new ImageModel();
		image.setName(imageUrl.getFile());
		image.setUrl(imageUrl);
		image.setPath(filepath);
		image.setLastCheck(new Date());
		image.setLastUpdate(new Date());
		Log.d(TAG, "Complete Downloading: " + imageUrl);
		return image;
	}
}
