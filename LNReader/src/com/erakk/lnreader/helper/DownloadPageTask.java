//package com.nandaka.bakareaderclone.helper;
package com.erakk.lnreader.helper;

import java.io.IOException;
import java.net.URL;
import org.jsoup.Jsoup;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;

import android.os.AsyncTask;

public class DownloadPageTask extends AsyncTask<URL, Void, AsyncTaskResult<Document>> {
	@Override
	protected AsyncTaskResult<Document> doInBackground(URL... arg0) {
		try {
			Response response = Jsoup.connect(arg0[0].toString())
									 .timeout(7000)
									 .execute();
			return new AsyncTaskResult<Document>(response.parse());
		} catch (IOException e) {
			return new AsyncTaskResult<Document>(e);
		}		
	}
}
