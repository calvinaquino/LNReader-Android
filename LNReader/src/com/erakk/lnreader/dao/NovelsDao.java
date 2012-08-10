/**
 * 
 */
package com.erakk.lnreader.dao;

//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
//import java.util.concurrent.ExecutionException;

//import org.apache.http.Header;
//import org.apache.http.HeaderIterator;
//import org.apache.http.HttpResponse;
//import org.apache.http.ProtocolVersion;
//import org.apache.http.RequestLine;
//import org.apache.http.client.HttpClient;
//import org.apache.http.client.methods.HttpGet;
//import org.apache.http.client.methods.HttpUriRequest;
//import org.apache.http.impl.client.DefaultHttpClient;
//import org.apache.http.params.HttpParams;
//import org.apache.http.protocol.BasicHttpContext;
//import org.apache.http.protocol.HttpContext;
//import org.jsoup.Connection.Response;
//import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.Context;
//import android.database.SQLException;
//import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
//import android.os.Handler;
import android.util.Log;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.helper.AsyncTaskResult;
import com.erakk.lnreader.helper.DBHelper;
import com.erakk.lnreader.helper.DownloadPageTask;
import com.erakk.lnreader.model.PageModel;

/**
 * @author Nandaka
 * 
 */
public class NovelsDao {
	private static final String TAG = NovelsDao.class.toString();
	
	private ArrayList<PageModel> list;
	private static DBHelper dbh;
	
	public NovelsDao(Context context) {
		if(dbh == null)
			dbh = new DBHelper(context);	
	}
	  
	public ArrayList<PageModel> getNovels() throws Exception{
		boolean refresh = false;
		PageModel page = dbh.selectFirstBy(DBHelper.COLUMN_PAGE, "Main_Page");
		if(page == null) {
			refresh = true;
		}
		else {
			// get last updated page revision from internet
			
			// compare if less than x day
		}
		
		if(refresh){
			list = getNovelsFromInternet();
			page = new PageModel();
			page.setPage("Main_Page");
			page.setTitle("Main Page");
			page.setType(PageModel.TYPE_OTHER);
			page.setLastUpdate(new Date());
			dbh.insertOrUpdate(page);
			
			for(Iterator<PageModel> i = list.iterator(); i.hasNext();){
				PageModel p = i.next();
				dbh.insertOrUpdate(p);
			}			
		}
		else {
			list = dbh.selectAllByColumn(DBHelper.COLUMN_TYPE, PageModel.TYPE_NOVEL);
		}
		return list;

	}
	
	private ArrayList<PageModel> getNovelsFromInternet() throws Exception {
		list = new ArrayList<PageModel>();
		Log.d(TAG, "Downloading: " + Constants.BaseURL);
		
		URL url = new URL(Constants.BaseURL);
		AsyncTask<URL, Void, AsyncTaskResult<Document>> task = new DownloadPageTask().execute(new URL[] {url});
		
		AsyncTaskResult<Document> result = task.get();
		
		if(result.getError() != null) {
			throw result.getError();
		}
		
		Document doc = result.getResult();
		Log.d(TAG, "Completed: " + Constants.BaseURL);
		
		Element stage = doc.select("#p-Light_Novels").first();
		if (stage != null) {
			Log.d(TAG, "Found: #p-Light_Novels");
			Elements novels = stage.select("li");
			for (Iterator<Element> i = novels.iterator(); i
					.hasNext();) {
				Element novel = i.next();
				Element link = novel.select("a").first();
				PageModel page = new PageModel();
				page.setPage(link.attr("href").replace("/project/index.php?title=", ""));
				page.setType(PageModel.TYPE_NOVEL);
				page.setTitle(link.text());
				page.setLastUpdate(new Date());
				list.add(page);
				Log.d(TAG, "Add: "+ link.text());
			}
		}
		Log.d(TAG, "Found: "+list.size()+" Novels");
		return list;
	}

}
