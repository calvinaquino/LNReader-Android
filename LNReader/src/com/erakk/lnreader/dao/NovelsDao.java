/**
 * 
 */
package com.erakk.lnreader.dao;

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import org.jsoup.nodes.Document;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.helper.AsyncTaskResult;
import com.erakk.lnreader.helper.DBHelper;
import com.erakk.lnreader.helper.DownloadPageTask;
import com.erakk.lnreader.model.PageModel;
import com.erakk.lnreader.parser.BakaTsukiParser;


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
			page.setLastCheck(new Date());
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
		
		list = BakaTsukiParser.ParseNovelList(doc);
		
		Log.d(TAG, "Found: "+list.size()+" Novels");
		return list;
	}

}
