//package com.nandaka.bakareaderclone.helper;
package com.erakk.lnreader.helper;

import java.io.IOException;
import java.net.URL;
import org.jsoup.Jsoup;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;

import android.os.AsyncTask;

public class DownloadPageTask extends AsyncTask<URL, Void, Document> {

	@Override
	protected Document doInBackground(URL... arg0) {
		try {
			Response response = Jsoup.connect(arg0[0].toString())
									 .timeout(7000)
									 .execute();
			return response.parse();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
