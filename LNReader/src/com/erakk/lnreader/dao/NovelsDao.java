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
import android.util.Log;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.helper.DBHelper;
import com.erakk.lnreader.helper.DownloadFileTask;
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
	
	public static Context context;
	private static DBHelper _dbh;
	private static DBHelper getDBHelper() {
		if(_dbh == null) {
			if(context == null) throw new NullPointerException("DB Context is not init!");
			_dbh = new DBHelper(context);
		}
		return _dbh;
	}
	
	public static ArrayList<PageModel> getNovels() throws Exception{
		ArrayList<PageModel> list = null;
		boolean refresh = false;
		PageModel page = getDBHelper().getMainPage();
		
		// check if have main page data
		if(page == null) {
			refresh = true;
			Log.d(TAG, "No Main_Page data!");
		}
		else {
			Log.d(TAG, "Found Main_Page (" + page.getLastUpdate().toString() + "), last check: " + page.getLastCheck().toString());
			// compare if less than 7 day
			Date today = new Date();			
			if(today.getTime() - page.getLastCheck().getTime() > (7 * 3600 * 1000)) {				
				refresh = true;
				Log.d(TAG, "Last check is over 7 days, checking online status");
			}
		}
		
		if(refresh){
			// get updated main page and novel list from internet
			list = getNovelsFromInternet();			
			Log.d(TAG, "Updated Novel List");
		}
		else {
			// get from db
			list = getDBHelper().selectAllByColumn(DBHelper.COLUMN_TYPE, PageModel.TYPE_NOVEL);
			Log.d(TAG, "Found: " + list.size());
		}
		return list;
	}
	
	public static PageModel getPageModel(String page) throws Exception {
		PageModel pageModel = getDBHelper().getPageModel(page);
		if (pageModel == null) {
			pageModel = getPageModelFromInternet(page);
		}
		return pageModel;
	}
	
	public static PageModel getPageModelFromInternet(String page) throws Exception {
		Response response = Jsoup.connect("http://www.baka-tsuki.org/project/api.php?action=query&prop=info&format=xml&titles=" + page)
				 .timeout(60000)
				 .execute();
		PageModel pageModel = BakaTsukiParser.parsePageAPI(page, response.parse());
		
		// save to db and get saved value
		pageModel = getDBHelper().insertOrUpdatePageModel(pageModel);
		
		return pageModel;
	}

	public static PageModel updatePageModel(PageModel page) {
		return getDBHelper().insertOrUpdatePageModel(page);		
	}
	
	public static ArrayList<PageModel> getWatchedNovel() {
		return getDBHelper().selectAllByColumn(DBHelper.COLUMN_IS_WATCHED, "1");
	}
	
	public static ArrayList<PageModel> getNovelsFromInternet() throws Exception {
		// get last updated page revision from internet
		PageModel mainPage = getPageModel("Main_Page");
		mainPage.setType(PageModel.TYPE_OTHER);
		mainPage.setParent("");
		getDBHelper().insertOrUpdatePageModel(mainPage);
		Log.d(TAG, "Updated Main_Page");
		
		// now get the list
		ArrayList<PageModel> list = new ArrayList<PageModel>();
		String url = Constants.BASE_URL + "/project";
		Response response = Jsoup.connect(url)
				 .timeout(60000)
				 .execute();
		Document doc = response.parse();
		
		list = BakaTsukiParser.ParseNovelList(doc);
		Log.d(TAG, "Found: "+list.size()+" Novels");
		
		// saved to db and get saved value
		list = getDBHelper().insertAllNovel(list);
		
		return list;
	}
	
	/*
	 * NovelCollectionModel
	 */

	public static NovelCollectionModel getNovelDetails(PageModel page) throws Exception {
		boolean refresh = false;
		NovelCollectionModel novel = getDBHelper().getNovelDetails(page.getPage());
		
		if(novel != null) {
			// TODO: add check to refresh
		}
		else{
			refresh = true;
		}
		
		if(refresh) {
			novel = getNovelDetailsFromInternet(page);
		}
		
		return novel;
	}
	
	public static NovelCollectionModel getNovelDetailsFromInternet(PageModel page) throws Exception {
		Log.d(TAG, "Getting Novel Details from internet: " + page.getPage());
		NovelCollectionModel novel = null;
		
		Response response = Jsoup.connect(Constants.BASE_URL + "/project/index.php?title=" + page.getPage())
				 .timeout(60000)
				 .execute();
		Document doc = response.parse();
		
		novel = BakaTsukiParser.ParseNovelDetails(doc, page);
		PageModel novelPage = getPageModelFromInternet(page.getPage());
		novel.setLastUpdate(novelPage.getLastUpdate());
		
		// insert to DB and get saved value
		novel = getDBHelper().insertNovelDetails(novel);
		
		// download cover image
		if(novel.getCoverUrl() != null) {
			DownloadFileTask task = new DownloadFileTask();
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
	
	public static NovelContentModel getNovelContent(PageModel page) throws Exception {
		NovelContentModel content = null;
		
		// get from db
		content = getDBHelper().getNovelContent(page.getPage());
		// get from Internet;
		if(content == null) {
			Log.d("getNovelContent", "Get from Internet: " + page.getPage());
			content = getNovelContentFromInternet(page);
		}
		
		return content;
	}

	public static NovelContentModel getNovelContentFromInternet(PageModel page) throws Exception {
		NovelContentModel content = new NovelContentModel();
		
		Response response = Jsoup.connect(Constants.BASE_URL + "/project/api.php?action=parse&format=xml&prop=text|images&page=" + page.getPage())
				 .timeout(60000)
				 .execute();
		Document doc = response.parse();
		
		content = BakaTsukiParser.ParseNovelContent(doc, page);
		content.setPageModel(page);
		
		// download all attached images
		DownloadFileTask task = new DownloadFileTask();
		for(Iterator<ImageModel> i = content.getImages().iterator(); i.hasNext();) {
			ImageModel image = i.next();
			image = task.downloadImage(image.getUrl());
			// TODO: need to save image to db?
		}
				
		// save to DB, and get the saved value
		content = getDBHelper().insertNovelContent(content);
				
		return content;
	}
	
	/*
	 *  ImageModel
	 */
	
	public static ImageModel getImageModel(String page) throws Exception {
		ImageModel image = null;
		
		image = getDBHelper().getImage(page);
		if(image == null) {
			Log.d(TAG, "Image not found, might need to check by referer: " + page);
			image = getDBHelper().getImageByReferer(page);
			
			if(image == null) {
				Log.d(TAG, "Image not found, getting data from internet: " + page);
				image = getImageModelFromInternet(page);				
			}
		}
		return image;
	}

	private static ImageModel getImageModelFromInternet(String page) throws Exception {
		Response response = Jsoup.connect(Constants.BASE_URL + page)
				 .timeout(60000)
				 .execute();
		Document doc = response.parse();
		ImageModel image = BakaTsukiParser.parseImagePage(doc); // only return the full image url
		
		DownloadFileTask downloader = new DownloadFileTask();
		image = downloader.downloadImage(image.getUrl());
		image.setReferer(page);
		
		// save to db and get the saved value
		image = getDBHelper().insertImage(image);
		
		return image;		
	}
}
