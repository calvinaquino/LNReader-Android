/**
 * 
 */
package com.erakk.lnreader.helper;

import java.io.InputStream;
import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;


/**
 * @author nandaka
 *
 */
public class DownloadImageTask extends
		AsyncTask<URL, Void, AsyncTaskResult<Bitmap>> {

	@Override
	protected AsyncTaskResult<Bitmap> doInBackground(URL... arg0) {
		try{
			Log.d("DownloadImage", "Starting to download: " + arg0[0].toString());
			Bitmap bitmap = BitmapFactory.decodeStream((InputStream) arg0[0].getContent());
			Log.d("DownloadImage", "Complete: " + arg0[0].toString());
			return new AsyncTaskResult<Bitmap>(bitmap); 
		}catch(Exception e){
			return new AsyncTaskResult<Bitmap>(e);
		}
	}
}
