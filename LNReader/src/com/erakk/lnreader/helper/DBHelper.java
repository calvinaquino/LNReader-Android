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
	
		
	@SuppressWarnings("unused")
	private static final String[] ALL_PAGE_COLUMS = new String[] {COLUMN_PAGE,
															 COLUMN_TITLE,
															 COLUMN_TYPE, 
															 COLUMN_PARENT, 
															 COLUMN_LAST_UPDATE,
															 COLUMN_LAST_CHECK,
															 COLUMN_IS_WATCHED};
	private static final String DATABASE_NAME = "pages.db";
	private static final int DATABASE_VERSION = 10;
	
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

	public DBHelper(Context context) {
	    super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		 db.execSQL(DATABASE_CREATE_PAGES);
		 db.execSQL(DATABASE_CREATE_IMAGES);
		 db.execSQL(DATABASE_CREATE_NOVEL_DETAILS);
		 db.execSQL(DATABASE_CREATE_NOVEL_BOOKS);
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
	    onCreate(db);
	}
	
	public void deletePagesDB() {
		database = this.getWritableDatabase();
		database.execSQL("delete from " + TABLE_PAGE);
		database.execSQL("delete from " + TABLE_NOVEL_DETAILS);
		database.execSQL("delete from " + TABLE_IMAGE);
		database.execSQL("delete from " + TABLE_NOVEL_BOOK);
		database.close();
		Log.w(TAG,"Database Deleted.");
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
		database.close();
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
		database.close();
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
		database.close();
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
			cv.put(COLUMN_LAST_UPDATE, "" + page.getLastUpdate().getSeconds());
			cv.put(COLUMN_LAST_CHECK, "" + new Date().getSeconds());
			cv.put(COLUMN_IS_WATCHED, page.isWatched());
			database.insertOrThrow(TABLE_PAGE, null, cv);
		}
		else {
			database.rawQuery("update " + TABLE_PAGE + " set " + COLUMN_TITLE + " = ?, " +
					   											 COLUMN_TYPE + " = ?, " +
					   											 COLUMN_PARENT + " = ?, " +
					   											 COLUMN_LAST_UPDATE + " = ?, " +
					   											 COLUMN_LAST_CHECK + " = ?, " +
					   											 COLUMN_IS_WATCHED + " = ? " +
					   		  " where " + COLUMN_PAGE + " = ?",
					   		  new String[] {page.getTitle(), 
					                        page.getType(),
					                        page.getParent(),
					                        "" + page.getLastUpdate().getSeconds(),
					                        "" + page.getLastCheck().getSeconds(),
					                        page.isWatched() ? "1" : "0", 
					                        temp.getPage()});
			Log.d(TAG, "Updating: " + page.toString());
		}
		database.close();
		return page.getPage();
	}
	
	private PageModel cursorTopage(Cursor cursor) {
		PageModel page = new PageModel();
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
		cv.put(COLUMN_LAST_UPDATE, "" + new Date().getSeconds());
		cv.put(COLUMN_LAST_CHECK, "" + new Date().getSeconds());
		database.insertOrThrow(TABLE_NOVEL_DETAILS, null, cv);		
		for(Iterator<BookModel> iBooks = novelDetails.getBookCollections().iterator(); iBooks.hasNext();){
			BookModel book = iBooks.next();
			Log.d(TAG, "Inserting Novel Details Books: " + book.toString());
			ContentValues cv2 = new ContentValues();
			cv2.put(COLUMN_PAGE, novelDetails.getPage());
			cv2.put(COLUMN_TITLE , book.getTitle());
			cv2.put(COLUMN_LAST_UPDATE, "" + new Date().getSeconds());
			cv2.put(COLUMN_LAST_CHECK, "" + new Date().getSeconds());
			database.insertOrThrow(TABLE_NOVEL_BOOK, null, cv2);
			
			for(Iterator<PageModel> iPage = book.getChapterCollection().iterator(); iPage.hasNext();) {
				PageModel page = iPage.next();
				Log.d(TAG, "Inserting Novel Details Chapter: " + page.toString());
				ContentValues cv3 = new ContentValues();
				cv3.put(COLUMN_PAGE, page.getPage());
				cv3.put(COLUMN_TITLE, page.getTitle());
				cv3.put(COLUMN_TYPE, page.getType());
				cv3.put(COLUMN_PARENT, page.getParent());
				cv3.put(COLUMN_LAST_UPDATE, "" + new Date().getSeconds());
				cv3.put(COLUMN_LAST_CHECK, "" + new Date().getSeconds());
				cv3.put(COLUMN_IS_WATCHED, false);
				database.insertOrThrow(TABLE_PAGE, null, cv3);
			}
		}
		
		Log.d(TAG, "Complete Insert Novel Details: " + novelDetails.toString());
		database.close();
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
		database.close();
		
		Log.d(TAG, "Complete Selecting Novel Details: " + page);
		return novelDetails;
	}
	
	private BookModel cursorToBookModel(Cursor cursor) {
		BookModel book = new BookModel();
		book.setTitle(cursor.getString(2));
		book.setLastUpdate(new Date(cursor.getInt(3)*1000));
		book.setLastCheck(new Date(cursor.getInt(4)*1000));
		return book;
	}

	private NovelCollectionModel cursorToNovelCollection(Cursor cursor) {
		NovelCollectionModel novelDetails = new NovelCollectionModel();
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
		cv.put(COLUMN_LAST_UPDATE, "" + new Date().getSeconds());
		cv.put(COLUMN_LAST_CHECK, "" + new Date().getSeconds());
		database.insertOrThrow(TABLE_IMAGE, null, cv);
		Log.d(TAG, "Complete Insert Images: " + image.toString());
		database.close();
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
		database.close();
		
		Log.d(TAG, "Complete Select: " + name);
		return image;
	}

	private ImageModel cursorToImage(Cursor cursor) {
		ImageModel image = new ImageModel();
		image.setName(cursor.getString(1));
		image.setPath(cursor.getString(21));
		try {
			image.setUrl(new URL(cursor.getString(3)));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		image.setLastUpdate(new Date(cursor.getInt(4)*1000));
		image.setLastCheck(new Date(cursor.getInt(5)*1000));
		return image;
	}
}
