package com.erakk.lnreader.helper;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.Date;

import android.os.AsyncTask;
import android.util.Log;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.model.ImageModel;

public class DownloadFileTask extends AsyncTask<URL, Integer, AsyncTaskResult<ImageModel>> {
	private static final String TAG = DownloadFileTask.class.toString();

	@Override
	protected AsyncTaskResult<ImageModel> doInBackground(URL... urls) {
		try{
			ImageModel image = downloadImage(urls[0]);
			return new AsyncTaskResult<ImageModel>(image);	         
		} catch (Exception e) {
			return new AsyncTaskResult<ImageModel>(e);
		}
	}

	public ImageModel downloadImage(URL url) throws Exception{
		Log.d(TAG, "Start Downloading: " + url.toString());
		InputStream input = null;
		OutputStream output = null;
		String filepath = Constants.IMAGE_ROOT + url.getFile();
		@SuppressWarnings("deprecation")
		String decodedUrl = URLDecoder.decode(filepath);
		Log.d(TAG, "Saving to: " + decodedUrl);

		// create dir if not exist
		String path = decodedUrl.substring(0, decodedUrl.lastIndexOf("/"));
		File cacheDir = new File(path);
		cacheDir.mkdirs();
		Log.d(TAG, "Path to: " + path);
		
		// check if file already downloaded
		if(new File(decodedUrl).exists()) {
			Log.d(TAG, "File exists: " + decodedUrl);
		}
		else {
			Log.d(TAG, "Start downloading image: " + url);
			URLConnection connection = url.openConnection();
			connection.connect();
			// this will be useful so that you can show a typical 0-100% progress bar
			// I'm not using it AT them moment, but don't remove, might be useful for real.
			int fileLength = connection.getContentLength();
	
			// download the file
			input = new BufferedInputStream(url.openStream());
			output = new FileOutputStream(decodedUrl);
	
			byte data[] = new byte[1024];
			long total = 0;
			int count;
			while ((count = input.read(data)) != -1) {
				total += count;
				// publishing the progress....
				publishProgress((int) (total * 100 / fileLength));
				output.write(data, 0, count);
			}
			output.flush();
			output.close();
			input.close();
			Log.d(TAG, "Downloading image complete, saved to: " + decodedUrl);
		}
		
		ImageModel image = new ImageModel();
		image.setName(url.getFile());
		image.setUrl(url);
		image.setPath(filepath);
		image.setLastCheck(new Date());
		image.setLastUpdate(new Date());        
		Log.d(TAG, "Complete Downloading: " + url.toString());
		return image;
	}
}


