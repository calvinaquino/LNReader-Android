//package com.nandaka.bakareaderclone.helper;
package com.erakk.lnreader.helper;

import java.net.URL;
import org.jsoup.Jsoup;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;

import android.os.AsyncTask;
import android.util.Log;

public class DownloadPageTask extends AsyncTask<URL, Void, AsyncTaskResult<Document>> {
	@Override
	protected AsyncTaskResult<Document> doInBackground(URL... arg0) {
		try {
			Log.d("DownloadPageTask", "Downloading: " + arg0[0].toString());
			Response response = Jsoup.connect(arg0[0].toString())
									 .timeout(7000)
									 .execute();
			Log.d("DownloadPageTask", "Complete: " + arg0[0].toString());
			return new AsyncTaskResult<Document>(response.parse());
		} catch (Exception e) {
			return new AsyncTaskResult<Document>(e);
		}		
	}
}
