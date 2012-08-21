/**
 * 
 */
package com.erakk.lnreader.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.helper.DBHelper;
import com.erakk.lnreader.helper.DownloadFileTask;
import com.erakk.lnreader.helper.ICallbackNotifier;
import com.erakk.lnreader.model.ImageModel;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.NovelContentModel;
import com.erakk.lnreader.model.PageModel;
import com.erakk.lnreader.parser.BakaTsukiParser;

/**
 * @author Nandaka
 * 
 */
public class NovelsDao {
	private static final String TAG = NovelsDao.class.toString();
	private Context context;
	private static DBHelper dbh;

	public NovelsDao(Context context) {
		if (dbh == null) {
			dbh = new DBHelper(context);
		}
		this.context = context;
	}

	public void deleteDB() {
		synchronized (dbh) {
			SQLiteDatabase db = dbh.getWritableDatabase();
			try{
				dbh.deletePagesDB(db);
			}finally{
				db.close();
			}
		}
	}

	public ArrayList<PageModel> getNovels(ICallbackNotifier notifier) throws Exception {
		ArrayList<PageModel> list = null;
		boolean refresh = false;
		PageModel page = null;
		SQLiteDatabase db = null;
		
		// check if main page exist
		synchronized (dbh) {
			try{
				db = dbh.getReadableDatabase();
				page = dbh.getMainPage(db);
			}finally{
				db.close();
			}
		}
		if (page == null) {
			refresh = true;
			Log.d(TAG, "No Main_Page data!");
		} else {
			Log.d(TAG, "Found Main_Page (" + page.getLastUpdate().toString() + "), last check: " + page.getLastCheck().toString());
			// compare if less than 7 day
			Date today = new Date();
			long diff = today.getTime() - page.getLastCheck().getTime();
			if (diff > (Constants.CHECK_INTERVAL * 24 * 3600 * 1000)) {
				refresh = true;
				Log.d(TAG, "Last check is over 7 days, checking online status");
			}
		}

		if (refresh) {
			// get updated main page and novel list from internet
			list = getNovelsFromInternet(notifier);
			Log.d(TAG, "Updated Novel List");
		} else {
			// get from db
			synchronized (dbh) {
				try{
					db = dbh.getReadableDatabase();
					list = dbh.getAllNovels(db);//dbh.selectAllByColumn(db, DBHelper.COLUMN_TYPE, PageModel.TYPE_NOVEL);
				}finally{
					db.close();
				}
			}
			Log.d(TAG, "Found: " + list.size());
		}

		return list;
	}

	public ArrayList<PageModel> getNovelsFromInternet(ICallbackNotifier notifier) throws Exception {
		if (notifier != null) {
			notifier.onCallback("Downloading novel list...");
		}
		// get last updated page revision from internet
		PageModel mainPage = getPageModel("Main_Page", notifier);
		mainPage.setType(PageModel.TYPE_OTHER);
		mainPage.setParent("");

		ArrayList<PageModel> list = null;
		synchronized (dbh) {
			SQLiteDatabase db = dbh.getWritableDatabase();
			try{
				db.beginTransaction();
				mainPage = dbh.insertOrUpdatePageModel(db, mainPage);
				Log.d(TAG, "Updated Main_Page");
	
				// now get the list
				list = new ArrayList<PageModel>();
				String url = Constants.BASE_URL + "/project";
				Response response = Jsoup.connect(url).timeout(60000).execute();
				Document doc = response.parse();
	
				list = BakaTsukiParser.ParseNovelList(doc, context);
				Log.d(TAG, "Found from internet: " + list.size() + " Novels");
	
				// saved to db and get saved value
				list = dbh.insertAllNovel(db, list);
				
				// now get the saved value
				list = dbh.getAllNovels(db);
				db.setTransactionSuccessful();
			}finally{
				db.endTransaction();
				db.close();
			}
		}
		return list;
	}

	public ArrayList<PageModel> getWatchedNovel() {
		ArrayList<PageModel> watchedNovel = null;
		synchronized (dbh) {
			SQLiteDatabase db = dbh.getReadableDatabase();
			try{
				watchedNovel = dbh.selectAllByColumn(db, DBHelper.COLUMN_IS_WATCHED, "1");
			}finally{
				db.close();
			}			
		}
		return watchedNovel;
	}

	public PageModel getPageModel(String page, ICallbackNotifier notifier) throws Exception {
		PageModel pageModel = null;
		synchronized (dbh) {
			SQLiteDatabase db = dbh.getReadableDatabase();
			try{
				pageModel = dbh.getPageModel(db, page);
			}finally{
				db.close();
			}
		}
		if (pageModel == null) {
			pageModel = getPageModelFromInternet(page, notifier);
		}
		return pageModel;
	}

	public PageModel getPageModelFromInternet(String page, ICallbackNotifier notifier) throws Exception {
		Response response = Jsoup.connect("http://www.baka-tsuki.org/project/api.php?action=query&prop=info&format=xml&titles=" + page).timeout(60000).execute();
		PageModel pageModel = BakaTsukiParser.parsePageAPI(page, response.parse(), context);

		synchronized (dbh) {
			// save to db and get saved value
			SQLiteDatabase db = dbh.getWritableDatabase();
			try{
				pageModel = dbh.insertOrUpdatePageModel(db, pageModel);
			}finally{
				db.close();
			}
		}
		return pageModel;
	}

	public PageModel updatePageModel(PageModel page) {
		PageModel pageModel = null;
		synchronized (dbh) {
			SQLiteDatabase db = dbh.getWritableDatabase();
			try{
				pageModel = dbh.insertOrUpdatePageModel(db, page);
			}
			finally{
				db.close();
			}
		}
		return pageModel;
	}
	
	/*
	 * NovelCollectionModel
	 */

	public NovelCollectionModel getNovelDetails(PageModel page, ICallbackNotifier notifier) throws Exception {
		boolean refresh = false;
		NovelCollectionModel novel = null;
		synchronized (dbh) {
			SQLiteDatabase db = dbh.getReadableDatabase();
			try{
				novel = dbh.getNovelDetails(db, page.getPage());
			}
			finally{
				db.close();
			}
		}
		if (novel != null) {
			// TODO: add check to refresh
		} else {
			refresh = true;
		}

		if (refresh) {
			novel = getNovelDetailsFromInternet(page, notifier);
		}

		return novel;
	}

	public NovelCollectionModel getNovelDetailsFromInternet(PageModel page, ICallbackNotifier notifier) throws Exception {
		Log.d(TAG, "Getting Novel Details from internet: " + page.getPage());
		NovelCollectionModel novel = null;

		Response response = Jsoup.connect(Constants.BASE_URL + "/project/index.php?title=" + page.getPage()).timeout(60000).execute();
		Document doc = response.parse();

		novel = BakaTsukiParser.ParseNovelDetails(doc, page, context);
		
		// check if page model exist
		PageModel novelPage = getPageModel(page.getPage(), notifier);
		page.setParent("Main_Page"); // insurance
		
		novel.setLastUpdate(novelPage.getLastUpdate());

		synchronized (dbh) {
			// insert to DB and get saved value
			SQLiteDatabase db = dbh.getWritableDatabase();
			try{
				db.beginTransaction();
				novel = dbh.insertNovelDetails(db, novel);
				db.setTransactionSuccessful();
			}
			finally{
				db.endTransaction();
				db.close();
			}
		}
		// download cover image
		if (novel.getCoverUrl() != null) {
			DownloadFileTask task = new DownloadFileTask(notifier);
			ImageModel image = task.downloadImage(novel.getCoverUrl());
			// TODO: need to save to db?
			Log.d("Image", image.toString());
		}

		Log.d(TAG, "Complete getting Novel Details from internet: " + page.getPage());
		return novel;
	}

	/*
	 * NovelContentModel
	 */

	public NovelContentModel getNovelContent(PageModel page, ICallbackNotifier notifier) throws Exception {
		NovelContentModel content = null;

		synchronized (dbh) {
			// get from db
			SQLiteDatabase db = dbh.getReadableDatabase();
			try{
				content = dbh.getNovelContent(db, page.getPage());
			}
			finally{
				db.close();
			}
		}
		// get from Internet;
		if (content == null) {
			Log.d("getNovelContent", "Get from Internet: " + page.getPage());
			content = getNovelContentFromInternet(page, notifier);
		}

		return content;
	}

	public NovelContentModel getNovelContentFromInternet(PageModel page, ICallbackNotifier notifier) throws Exception {
		NovelContentModel content = new NovelContentModel(context);

		Response response = Jsoup.connect(Constants.BASE_URL + "/project/api.php?action=parse&format=xml&prop=text|images&page=" + page.getPage()).timeout(60000).execute();
		Document doc = response.parse();

		content = BakaTsukiParser.ParseNovelContent(doc, page, context);

		// download all attached images
		DownloadFileTask task = new DownloadFileTask(notifier);
		for (Iterator<ImageModel> i = content.getImages().iterator(); i.hasNext();) {
			ImageModel image = i.next();
			
			if(notifier != null) {
				notifier.onCallback("Downloading: " + image.getUrl());
			}
			image = task.downloadImage(image.getUrl());
			// TODO: need to save image to db?
		}
		synchronized (dbh) {
			// save to DB, and get the saved value
			SQLiteDatabase db = dbh.getWritableDatabase();
			try{
				// TODO: somehow using transaction cause problem...
				db.beginTransaction();
				content = dbh.insertNovelContent(db, content);
				db.setTransactionSuccessful();
			}
			finally{
				db.endTransaction();
				db.close();
			}
		}
		return content;
	}

	public NovelContentModel updateNovelContent(NovelContentModel content) throws Exception {
		synchronized (dbh) {
			SQLiteDatabase db = dbh.getWritableDatabase();
			try{
				content = dbh.insertNovelContent(db, content);
			}
			finally{
				db.close();
			}
		}
		return content;
	}

	/*
	 * ImageModel
	 */

	public ImageModel getImageModel(String page, ICallbackNotifier notifier) throws Exception {
		ImageModel image = null;
		synchronized (dbh) {
			SQLiteDatabase db = dbh.getReadableDatabase();
			try{
				image = dbh.getImage(db, page);
	
				if (image == null) {
					Log.d(TAG, "Image not found, might need to check by referer: " + page);
					image = dbh.getImageByReferer(db, page);
				}
			}
			finally{
				db.close();
			}
		}
		if (image == null) {
			Log.d(TAG, "Image not found, getting data from internet: " + page);
			image = getImageModelFromInternet(page, notifier);
		}
		return image;
	}

	public ImageModel getImageModelFromInternet(String page, ICallbackNotifier notifier) throws Exception {
		String url = page;
		if (!url.startsWith("http"))
			url = Constants.BASE_URL + url;
		Response response = Jsoup.connect(url).timeout(60000).execute();
		Document doc = response.parse();
		// only return the full  image url
		ImageModel image = BakaTsukiParser.parseImagePage(doc); 

		DownloadFileTask downloader = new DownloadFileTask(notifier);
		image = downloader.downloadImage(image.getUrl());
		image.setReferer(page);

		synchronized (dbh) {
			// save to db and get the saved value
			SQLiteDatabase db = dbh.getWritableDatabase();
			try{
				image = dbh.insertImage(db, image);
			}
			finally{
				db.close();
			}
		}
		return image;
	}

}
