//package com.nandaka.bakareaderclone.helper;
package com.erakk.lnreader.task;

import android.os.AsyncTask;
import android.util.Log;

import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.URL;

public class DownloadPageTask extends AsyncTask<URL, Void, AsyncTaskResult<Document>> {
	@Override
	protected AsyncTaskResult<Document> doInBackground(URL... arg0) {
		try {
			Log.d("DownloadPageTask", "Downloading: " + arg0[0].toString());
			Response response = Jsoup.connect(arg0[0].toString())
					.timeout(7000)
					.execute();
			Log.d("DownloadPageTask", "Complete: " + arg0[0].toString());
			return new AsyncTaskResult<Document>(response.parse(), Document.class);
		} catch (Exception e) {
			return new AsyncTaskResult<Document>(null, Document.class, e);
		}
	}
}
