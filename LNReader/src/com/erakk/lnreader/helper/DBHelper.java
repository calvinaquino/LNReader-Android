//package com.nandaka.bakareaderclone.helper;
package com.erakk.lnreader.helper;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import com.erakk.lnreader.model.BookModel;
import com.erakk.lnreader.model.ImageModel;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.NovelContentModel;
import com.erakk.lnreader.model.PageModel;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {
	public static final String TAG = DBHelper.class.toString();
	public static final String TABLE_PAGE = "pages";
	public static final String COLUMN_ID = "id";
	public static final String COLUMN_PAGE = "page";
	public static final String COLUMN_LAST_UPDATE = "last_update";
	public static final String COLUMN_LAST_CHECK = "last_check";
	public static final String COLUMN_TITLE = "title";	
	public static final String COLUMN_TYPE = "type";
	public static final String COLUMN_PARENT = "parent";
	public static final String COLUMN_IS_WATCHED = "is_watched";
	
	public static final String TABLE_IMAGE = "images";
	public static final String COLUMN_IMAGE = "name";
	public static final String COLUMN_FILEPATH = "filepath";
	public static final String COLUMN_URL = "url";
	
	public static final String TABLE_NOVEL_DETAILS = "novel_details";
	public static final String COLUMN_SYNOPSIS = "synopsis";
	
	public static final String TABLE_NOVEL_BOOK = "novel_books";
	
	public static final String TABLE_NOVEL_CONTENT = "novel_books_content";
	public static final String COLUMN_CONTENT = "content";
	public static final String COLUMN_LAST_X = "lastXScroll";
	public static final String COLUMN_LAST_Y = "lastYScroll";
	public static final String COLUMN_ZOOM = "lastZoom";

	private static final String DATABASE_NAME = "pages.db";
	private static final int DATABASE_VERSION = 11;
	
	private SQLiteDatabase database;
	
	// Database creation SQL statement
	private static final String DATABASE_CREATE_PAGES = "create table "
	      + TABLE_PAGE + "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
							 + COLUMN_PAGE + " text not null, "
			  				 + COLUMN_TITLE + " text not null, "
			  				 + COLUMN_TYPE + " text not null, "
			  				 + COLUMN_PARENT + " text, "
			  				 + COLUMN_LAST_UPDATE + " integer, "
			  				 + COLUMN_LAST_CHECK + " integer, "
			  				 + COLUMN_IS_WATCHED + " boolean);";
	
	private static final String DATABASE_CREATE_IMAGES = "create table "
		      + TABLE_IMAGE + "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				 				  + COLUMN_IMAGE + " text not null, "
				  				  + COLUMN_FILEPATH + " text not null, "
				  				  + COLUMN_URL + " text not null, "
				  				  + COLUMN_LAST_UPDATE + " integer, "
				  				  + COLUMN_LAST_CHECK + " integer);";
	
	private static final String DATABASE_CREATE_NOVEL_DETAILS = "create table "
		      + TABLE_NOVEL_DETAILS + "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				 				  + COLUMN_PAGE + " text not null, "
				  				  + COLUMN_SYNOPSIS + " text not null, "
				  				  + COLUMN_IMAGE + " text not null, "
				  				  + COLUMN_LAST_UPDATE + " integer, "
				  				  + COLUMN_LAST_CHECK + " integer);";
	
	private static final String DATABASE_CREATE_NOVEL_BOOKS = "create table "
		      + TABLE_NOVEL_BOOK + "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				 				  + COLUMN_PAGE + " text not null, "
				  				  + COLUMN_TITLE + " text not null, "
				  				  + COLUMN_LAST_UPDATE + " integer, "
				  				  + COLUMN_LAST_CHECK + " integer);";

	private static final String DATABASE_CREATE_NOVEL_CONTENT = "create table "
		      + TABLE_NOVEL_CONTENT + "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				 				    + COLUMN_CONTENT + " text not null, "
		      						+ COLUMN_PAGE + " text not null, "
				  				    + COLUMN_LAST_X + " integer, "
				  				    + COLUMN_LAST_Y + " integer, "
				  				    + COLUMN_ZOOM + " double, "
				  				    + COLUMN_LAST_UPDATE + " integer, "
				  				    + COLUMN_LAST_CHECK + " integer);";
	
	public DBHelper(Context context) {
	    super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		 db.execSQL(DATABASE_CREATE_PAGES);
		 db.execSQL(DATABASE_CREATE_IMAGES);
		 db.execSQL(DATABASE_CREATE_NOVEL_DETAILS);
		 db.execSQL(DATABASE_CREATE_NOVEL_BOOKS);
		 db.execSQL(DATABASE_CREATE_NOVEL_CONTENT);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(DBHelper.class.getName(),
		        "Upgrading database from version " + oldVersion + " to "
		            + newVersion + ", which will destroy all old data");
	    db.execSQL("DROP TABLE IF EXISTS " + TABLE_PAGE);
	    db.execSQL("DROP TABLE IF EXISTS " + TABLE_IMAGE);
	    db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOVEL_DETAILS);
	    db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOVEL_BOOK);
	    db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOVEL_CONTENT);
	    onCreate(db);
	}
	
	public void deletePagesDB() {
		database = this.getWritableDatabase();
		database.execSQL("delete from " + TABLE_PAGE);
		database.execSQL("delete from " + TABLE_NOVEL_DETAILS);
		database.execSQL("delete from " + TABLE_IMAGE);
		database.execSQL("delete from " + TABLE_NOVEL_BOOK);
		database.execSQL("delete from " + TABLE_NOVEL_CONTENT);
		//database.close();
		Log.w(TAG,"Database Deleted.");
	}
	
	public PageModel getMainPage() {
		Log.d(TAG, "Select Main_Page");
		PageModel page = getPageModel("Main_Page");
		return page;
	}

	public void insertAllNovel(ArrayList<PageModel> list) {
		for(Iterator<PageModel> i = list.iterator(); i.hasNext();){
			PageModel p = i.next();
			insertOrUpdate(p);
		}	
	}
	
	public PageModel getPageModel(String page) {
		Log.d(TAG, "Select Page: " + page);
		PageModel pageModel = null;
		database = this.getWritableDatabase();
		
		Cursor cursor = database.rawQuery("select * from " + TABLE_PAGE + " where " + COLUMN_PAGE + " = ? ", new String[] {page});
		cursor.moveToFirst();
	    while (!cursor.isAfterLast()) {
	    	pageModel = cursorTopage(cursor);
	    	Log.d(TAG, "Found Page: " + pageModel.toString());
	    	break;
	    }
		return pageModel;
	}
	
	public ArrayList<PageModel> selectAllNovels() {
		ArrayList<PageModel> pages = new ArrayList<PageModel>();
		database = this.getWritableDatabase();

		Cursor cursor = database.rawQuery("select * from " + TABLE_PAGE + " where " + COLUMN_PARENT + " = ?", new String[] {"Main_Page"});
		cursor.moveToFirst();
	    while (!cursor.isAfterLast()) {
	    	PageModel page = cursorTopage(cursor);
	    	pages.add(page);
	    	cursor.moveToNext();
	    }		
		//database.close();
		return pages;
	}
	
	public ArrayList<PageModel> selectAllByColumn(String column, String value) {
		ArrayList<PageModel> pages = new ArrayList<PageModel>();
		database = this.getWritableDatabase();

		Cursor cursor = database.rawQuery("select * from " + TABLE_PAGE + " where " + column + " = ?", new String[] {value});
		cursor.moveToFirst();
	    while (!cursor.isAfterLast()) {
	    	PageModel page = cursorTopage(cursor);
	    	pages.add(page);
	    	cursor.moveToNext();
	    }		
		//database.close();
		return pages;
	}
	
	public PageModel selectFirstBy(String column, String value){
		Log.d(TAG, "Select First: Column = " + column + " Value = " + value);
		PageModel page = null;
		database = this.getWritableDatabase();
		
		Cursor cursor = database.rawQuery("select * from " + TABLE_PAGE + " where " + column + " = ? ", new String[] {value});
		//Cursor cursor = database.rawQuery("select * from " + TABLE_PAGE + " where " + column + " = '" + value + "'", null);
		cursor.moveToFirst();
	    while (!cursor.isAfterLast()) {
	    	page = cursorTopage(cursor);
	    	Log.d(TAG, "Found: " + page.toString());
	    	break;
	    }		
		//database.close();
		return page;
	}
		
	@SuppressWarnings("deprecation")
	public String insertOrUpdate(PageModel page){
		Log.d(TAG, page.toString());
		
		PageModel temp = selectFirstBy(COLUMN_PAGE, page.getPage());
		
		database = this.getWritableDatabase();
		if(temp == null) {
			Log.d(TAG, "Inserting: " + page.toString());
			ContentValues cv = new ContentValues();
			cv.put(COLUMN_PAGE, page.getPage());
			cv.put(COLUMN_TITLE, page.getTitle());
			cv.put(COLUMN_TYPE, page.getType());
			cv.put(COLUMN_PARENT, page.getParent());
			cv.put(COLUMN_LAST_UPDATE, "" + (int) (page.getLastUpdate().getTime()/ 1000));
			cv.put(COLUMN_LAST_CHECK, "" + (int) (new Date().getTime() / 1000));
			cv.put(COLUMN_IS_WATCHED, page.isWatched());
			database.insertOrThrow(TABLE_PAGE, null, cv);
		}
		else {
			ContentValues cv = new ContentValues();
			cv.put(COLUMN_PAGE, page.getPage());
			cv.put(COLUMN_TITLE, page.getTitle());
			cv.put(COLUMN_TYPE, page.getType());
			cv.put(COLUMN_PARENT, page.getParent());
			cv.put(COLUMN_LAST_UPDATE, "" + (int) (page.getLastUpdate().getTime() / 1000));
			cv.put(COLUMN_LAST_CHECK, "" + (int) (new Date().getTime() / 1000));
			cv.put(COLUMN_IS_WATCHED, page.isWatched());
			database.update(TABLE_PAGE, cv, "page = ?", new String[] {page.getPage()});
			Log.d(TAG, "Updating: " + page.toString());
		}
		//database.close();
		return page.getPage();
	}
	
	private PageModel cursorTopage(Cursor cursor) {
		PageModel page = new PageModel();
		page.setId(cursor.getInt(0));
		page.setPage(cursor.getString(1));
		page.setTitle(cursor.getString(2));
		page.setType(cursor.getString(3));
		page.setParent(cursor.getString(4));
		page.setLastUpdate(new Date(cursor.getLong(5)*1000));
		page.setLastCheck(new Date(cursor.getLong(6)*1000));
		page.setWatched(cursor.getInt(7) == 1 ? true : false);
	    return page;
	}
	
	@SuppressWarnings("deprecation")
	public void insertNovelDetails(NovelCollectionModel novelDetails){
		database = this.getWritableDatabase();
		Log.d(TAG, "Inserting Novel Details: " + novelDetails.toString());
		ContentValues cv = new ContentValues();
		cv.put(COLUMN_PAGE, novelDetails.getPage());
		cv.put(COLUMN_SYNOPSIS, novelDetails.getSynopsis());
		cv.put(COLUMN_IMAGE, novelDetails.getCover());
		cv.put(COLUMN_LAST_UPDATE, "" + (int) (novelDetails.getLastUpdate().getTime() / 1000));
		cv.put(COLUMN_LAST_CHECK, "" + (int) (new Date().getTime() / 1000));
		database.insertOrThrow(TABLE_NOVEL_DETAILS, null, cv);
		
		for(Iterator<BookModel> iBooks = novelDetails.getBookCollections().iterator(); iBooks.hasNext();){
			BookModel book = iBooks.next();
			Log.d(TAG, "Inserting Novel Details Books: " + book.toString());
			ContentValues cv2 = new ContentValues();
			cv2.put(COLUMN_PAGE, novelDetails.getPage());
			cv2.put(COLUMN_TITLE , book.getTitle());
			cv2.put(COLUMN_LAST_UPDATE, "" + (int) (novelDetails.getLastUpdate().getTime() / 1000));
			cv2.put(COLUMN_LAST_CHECK, "" + (int) (new Date().getTime() / 1000));
			database.insertOrThrow(TABLE_NOVEL_BOOK, null, cv2);
			
			for(Iterator<PageModel> iPage = book.getChapterCollection().iterator(); iPage.hasNext();) {
				PageModel page = iPage.next();
				Log.d(TAG, "Inserting Novel Details Chapter: " + page.toString());
				ContentValues cv3 = new ContentValues();
				cv3.put(COLUMN_PAGE, page.getPage());
				cv3.put(COLUMN_TITLE, page.getTitle());
				cv3.put(COLUMN_TYPE, page.getType());
				cv3.put(COLUMN_PARENT, page.getParent());
				cv3.put(COLUMN_LAST_UPDATE, "" + (int) (novelDetails.getLastUpdate().getTime() / 1000)); // TODO: get actual page revision
				cv3.put(COLUMN_LAST_CHECK, "" + (int) (new Date().getTime() / 1000));
				cv3.put(COLUMN_IS_WATCHED, false);
				database.insertOrThrow(TABLE_PAGE, null, cv3);
			}
		}
		
		Log.d(TAG, "Complete Insert Novel Details: " + novelDetails.toString());
		//database.close();
	}
	
	public NovelCollectionModel getNovelDetails(String page) {
		Log.d(TAG, "Selecting Novel Details: " + page);
		NovelCollectionModel novelDetails = null;
		database = this.getWritableDatabase();
		
		Cursor cursor = database.rawQuery("select * from " + TABLE_NOVEL_DETAILS + " where " + COLUMN_PAGE + " = ? ", new String[] {page});
		cursor.moveToFirst();
	    while (!cursor.isAfterLast()) {
	    	novelDetails = cursorToNovelCollection(cursor);
	    	Log.d(TAG, "Found: " + novelDetails.toString());
	    	break;
	    }
	    if(novelDetails != null) {
		    // get the books
		    ArrayList<BookModel> bookCollection = new ArrayList<BookModel>(); 
		    cursor = database.rawQuery("select * from " + TABLE_NOVEL_BOOK + " where " + COLUMN_PAGE + " = ? ", new String[] {page});
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				BookModel book = cursorToBookModel(cursor);
				bookCollection.add(book);
		    	Log.d(TAG, "Found: " + book.toString());
		    	cursor.moveToNext();
		    }		
			
			// get the page model
			novelDetails.setPageModel(getPageModel(novelDetails.getPage()));			
			
			// get the chapters
			for(Iterator<BookModel> iBook = bookCollection.iterator(); iBook.hasNext();) {
				BookModel book = iBook.next();
				ArrayList<PageModel> chapters = new ArrayList<PageModel>();
				cursor = database.rawQuery("select * from " + TABLE_PAGE + " where " + COLUMN_PARENT + " = ? ", new String[] {novelDetails.getPage() + "%" + book.getTitle()});
				cursor.moveToFirst();
				while (!cursor.isAfterLast()) {
					PageModel chapter = cursorTopage(cursor);
					chapters.add(chapter);
			    	Log.d(TAG, "Found: " + chapter.toString());
			    	cursor.moveToNext();
			    }
				book.setChapterCollection(chapters);
			}		
			novelDetails.setBookCollections(bookCollection);
	    }
	    else {
	    	Log.d(TAG, "No Data for Novel Details: " + page);
	    }
		//database.close();
		
		Log.d(TAG, "Complete Selecting Novel Details: " + page);
		return novelDetails;
	}
	
	private BookModel cursorToBookModel(Cursor cursor) {
		BookModel book = new BookModel();
		book.setId(cursor.getInt(0));
		book.setPage(cursor.getString(1));
		book.setTitle(cursor.getString(2));
		book.setLastUpdate(new Date(cursor.getInt(3)*1000));
		book.setLastCheck(new Date(cursor.getInt(4)*1000));
		return book;
	}

	private NovelCollectionModel cursorToNovelCollection(Cursor cursor) {
		NovelCollectionModel novelDetails = new NovelCollectionModel();
		novelDetails.setId(cursor.getInt(0));
		novelDetails.setPage(cursor.getString(1));
		novelDetails.setSynopsis(cursor.getString(2));
		novelDetails.setCover(cursor.getString(3));
		novelDetails.setLastUpdate(new Date(cursor.getInt(4)*1000));
		novelDetails.setLastCheck(new Date(cursor.getInt(5)*1000));
		return novelDetails;
	}

	@SuppressWarnings("deprecation")
	public void insertImage(ImageModel image){
		database = this.getWritableDatabase();
		Log.d(TAG, "Inserting Images: " + image.toString());
		ContentValues cv = new ContentValues();
		cv.put(COLUMN_IMAGE, image.getName());
		cv.put(COLUMN_FILEPATH, image.getPath());
		cv.put(COLUMN_URL, image.getUrl().toString());
		cv.put(COLUMN_LAST_UPDATE, "" + (int) (new Date().getTime() / 1000));
		cv.put(COLUMN_LAST_CHECK, "" + (int) (new Date().getTime() / 1000));
		database.insertOrThrow(TABLE_IMAGE, null, cv);
		Log.d(TAG, "Complete Insert Images: " + image.toString());
		//database.close();
	}
	
	public ImageModel getImage(String name) {
		Log.d(TAG, "Selecting: " + name);
		ImageModel image = null;
		database = this.getWritableDatabase();
		
		Cursor cursor = database.rawQuery("select * from " + TABLE_IMAGE + " where " + COLUMN_IMAGE + " = ? ", new String[] {name});
		cursor.moveToFirst();
	    while (!cursor.isAfterLast()) {
	    	image = cursorToImage(cursor);
	    	Log.d(TAG, "Found: " + image.toString());
	    	break;
	    }		
		//database.close();
		
		Log.d(TAG, "Complete Select: " + name);
		return image;
	}

	private ImageModel cursorToImage(Cursor cursor) {
		ImageModel image = new ImageModel();
		image.setId(cursor.getInt(0));
		image.setName(cursor.getString(1));
		image.setPath(cursor.getString(2));
		try {
			image.setUrl(new URL(cursor.getString(3)));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		image.setLastUpdate(new Date(cursor.getInt(4)*1000));
		image.setLastCheck(new Date(cursor.getInt(5)*1000));
		return image;
	}
	
	public void insertNovelContent(NovelContentModel content) {
		database = this.getWritableDatabase();
		Log.d(TAG, "Inserting Novel Content: " + content.getPage());
		ContentValues cv = new ContentValues();
		cv.put(COLUMN_CONTENT, content.getContent());
		cv.put(COLUMN_PAGE, content.getPage());
		cv.put(COLUMN_LAST_X, "" + content.getLastXScroll());
		cv.put(COLUMN_LAST_Y, "" + content.getLastYScroll());
		cv.put(COLUMN_ZOOM, "" + content.getLastZoom());
		cv.put(COLUMN_LAST_UPDATE, "" + (int) (new Date().getTime() / 1000));
		cv.put(COLUMN_LAST_CHECK, "" + (int) (new Date().getTime() / 1000));
		database.insertOrThrow(TABLE_NOVEL_CONTENT, null, cv);
		Log.d(TAG, "Complete Insert Novel Content:: " + content.getPage());
	}
	
	public NovelContentModel getNovelContent(String page) {
		Log.d(TAG, "Selecting Novel Content: " + page);
		NovelContentModel content = null;
		database = this.getWritableDatabase();
		
		Cursor cursor = database.rawQuery("select * from " + TABLE_NOVEL_CONTENT + " where " + COLUMN_PAGE + " = ? ", new String[] {page});
		cursor.moveToFirst();
	    while (!cursor.isAfterLast()) {
	    	content = cursorToNovelContent(cursor);
	    	Log.d(TAG, "Found: " + content.getPage());
	    	break;
	    }		
		//database.close();
		
		Log.d(TAG, "Complete Select: " + page);
		return content;
	}

	private NovelContentModel cursorToNovelContent(Cursor cursor) {
		NovelContentModel content = new NovelContentModel();
		content.setId(cursor.getInt(0));
		content.setContent(cursor.getString(1));
		content.setPage(cursor.getString(2));
		content.setPageModel(getPageModel(content.getPage()));
		content.setLastXScroll(cursor.getInt(3));
		content.setLastYScroll(cursor.getInt(4));
		content.setLastZoom(cursor.getDouble(5));
		content.setLastUpdate(new Date(cursor.getInt(6)*1000));
		content.setLastCheck(new Date(cursor.getInt(7)*1000));
		return content;
	}
}
