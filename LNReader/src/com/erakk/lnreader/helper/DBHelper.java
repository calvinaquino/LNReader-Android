//package com.nandaka.bakareaderclone.helper;
package com.erakk.lnreader.helper;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.model.BookModel;
import com.erakk.lnreader.model.ImageModel;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.NovelContentModel;
import com.erakk.lnreader.model.PageModel;

public class DBHelper extends SQLiteOpenHelper {
	public static final String TAG = DBHelper.class.toString();
	public static final String TABLE_PAGE = "pages";
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_PAGE = "page";
	public static final String COLUMN_LAST_UPDATE = "last_update";
	public static final String COLUMN_LAST_CHECK = "last_check";
	public static final String COLUMN_TITLE = "title";	
	public static final String COLUMN_TYPE = "type";
	public static final String COLUMN_PARENT = "parent";
	public static final String COLUMN_IS_WATCHED = "is_watched";
	public static final String COLUMN_IS_FINISHED_READ = "is_finished_read";
	public static final String COLUMN_IS_DOWNLOADED = "is_downloaded";
	public static final String COLUMN_ORDER = "_index";
	public static final String COLUMN_STATUS = "status";
	
	public static final String TABLE_IMAGE = "images";
	public static final String COLUMN_IMAGE = "name";
	public static final String COLUMN_FILEPATH = "filepath";
	public static final String COLUMN_URL = "url";
	public static final String COLUMN_REFERER = "referer";
	
	public static final String TABLE_NOVEL_DETAILS = "novel_details";
	public static final String COLUMN_SYNOPSIS = "synopsis";
	
	public static final String TABLE_NOVEL_BOOK = "novel_books";
	
	public static final String TABLE_NOVEL_CONTENT = "novel_books_content";
	public static final String COLUMN_CONTENT = "content";
	public static final String COLUMN_LAST_X = "lastXScroll";
	public static final String COLUMN_LAST_Y = "lastYScroll";
	public static final String COLUMN_ZOOM = "lastZoom";

	public static final String DATABASE_NAME = "pages.db";
	public static final int DATABASE_VERSION = 19;

	// Database creation SQL statement
	private static final String DATABASE_CREATE_PAGES = "create table "
	      + TABLE_PAGE + "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "	// 0
							 + COLUMN_PAGE + " text unique not null, "				// 1
			  				 + COLUMN_TITLE + " text not null, "					// 2
			  				 + COLUMN_TYPE + " text, "								// 3
			  				 + COLUMN_PARENT + " text, "							// 4
			  				 + COLUMN_LAST_UPDATE + " integer, "					// 5
			  				 + COLUMN_LAST_CHECK + " integer, "						// 6
			  				 + COLUMN_IS_WATCHED + " boolean, "						// 7
			  				 + COLUMN_IS_FINISHED_READ + " boolean, "				// 8
			  				 + COLUMN_IS_DOWNLOADED + " boolean, "					// 9
			  				 + COLUMN_ORDER + " integer, "							// 10
			  				 + COLUMN_STATUS + " text );";							// 11
	
	private static final String DATABASE_CREATE_IMAGES = "create table "
		      + TABLE_IMAGE + "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				 				  + COLUMN_IMAGE + " text unique not null, "
				  				  + COLUMN_FILEPATH + " text not null, "
				  				  + COLUMN_URL + " text not null, "
				  				  + COLUMN_REFERER + " text, "
				  				  + COLUMN_LAST_UPDATE + " integer, "
				  				  + COLUMN_LAST_CHECK + " integer);";
	
	private static final String DATABASE_CREATE_NOVEL_DETAILS = "create table "
		      + TABLE_NOVEL_DETAILS + "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				 				  + COLUMN_PAGE + " text unique not null, "
				  				  + COLUMN_SYNOPSIS + " text not null, "
				  				  + COLUMN_IMAGE + " text not null, "
				  				  + COLUMN_LAST_UPDATE + " integer, "
				  				  + COLUMN_LAST_CHECK + " integer);";

	// COLUMN_PAGE is not unique because being used for reference to the novel page. 
	private static final String DATABASE_CREATE_NOVEL_BOOKS = "create table "
		      + TABLE_NOVEL_BOOK + "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "	// 0
				 				  + COLUMN_PAGE + " text not null, "						// 1
				  				  + COLUMN_TITLE + " text not null, "						// 2
				  				  + COLUMN_LAST_UPDATE + " integer, "						// 3
				  				  + COLUMN_LAST_CHECK + " integer, "						// 4
				  				  + COLUMN_ORDER + " integer);";							// 5

	private static final String DATABASE_CREATE_NOVEL_CONTENT = "create table "
		      + TABLE_NOVEL_CONTENT + "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "	// 0
				 				    + COLUMN_CONTENT + " text not null, "						// 1
		      						+ COLUMN_PAGE + " text unique not null, "					// 2
				  				    + COLUMN_LAST_X + " integer, "								// 3
				  				    + COLUMN_LAST_Y + " integer, "								// 4
				  				    + COLUMN_ZOOM + " double, "									// 5
				  				    + COLUMN_LAST_UPDATE + " integer, "							// 6
				  				    + COLUMN_LAST_CHECK + " integer);";							// 7
	
	public static final String DB_ROOT_SD = Environment.getExternalStorageDirectory().getAbsolutePath().toString() + "/Android/data/" + Constants.class.getPackage().getName() + "/databases";
	
	private static String getDbPath(){
		File path = new File(DB_ROOT_SD);
		path.mkdirs();		
		return DB_ROOT_SD + "/" + DATABASE_NAME;
	}
	
	public DBHelper(Context context) {
		super(context, getDbPath(), null, DATABASE_VERSION);
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
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		onUpgrade(db, oldVersion, newVersion);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(DBHelper.class.getName(),
		        "Upgrading db from version " + oldVersion + " to "
		            + newVersion + ", which will destroy all old data");
//	    db.execSQL("DROP TABLE IF EXISTS " + TABLE_PAGE);
//	    db.execSQL("DROP TABLE IF EXISTS " + TABLE_IMAGE);
//	    db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOVEL_DETAILS);
//	    db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOVEL_BOOK);
//	    db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOVEL_CONTENT);
//		onCreate(db);
		if(oldVersion == 18) {
			db.execSQL("ALTER TABLE " + TABLE_PAGE + " ADD COLUMN " + COLUMN_STATUS + " text" );
			oldVersion = 19;
		}	    
	}
	
	public void deletePagesDB(SQLiteDatabase db) {
		// use drop because it is faster and can avoid free row fragmentation
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_PAGE);
	    db.execSQL("DROP TABLE IF EXISTS " + TABLE_IMAGE);
	    db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOVEL_DETAILS);
	    db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOVEL_BOOK);
	    db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOVEL_CONTENT);
	    onCreate(db);
		Log.w(TAG,"Database Deleted.");
	}
	
	public PageModel getMainPage(SQLiteDatabase db) {
		//Log.d(TAG, "Select Main_Page");
		PageModel page = getPageModel(db, "Main_Page");
		return page;
	}
	
	public PageModel getTeaserPage(SQLiteDatabase db) {
		PageModel page = getPageModel(db, "Category:Teasers");
		return page;
	}

	public ArrayList<PageModel> insertAllNovel(SQLiteDatabase db, ArrayList<PageModel> list) {
		ArrayList<PageModel> updatedList = new ArrayList<PageModel>();
		for(Iterator<PageModel> i = list.iterator(); i.hasNext();){
			PageModel p = i.next();
			p = insertOrUpdatePageModel(db, p);
			updatedList.add(p);
		}
		return updatedList;
	}
	
	public PageModel getPageModel(SQLiteDatabase db, String page) {
		//Log.d(TAG, "Select Page: " + page);
		PageModel pageModel = null;
		
		Cursor cursor = rawQuery(db, "select * from " + TABLE_PAGE + " where " + COLUMN_PAGE + " = ? ", new String[] {page});
		cursor.moveToFirst();
	    while (!cursor.isAfterLast()) {
	    	pageModel = cursorTopage(cursor);
	    	//Log.d(TAG, "Found Page: " + pageModel.toString());
	    	break;
	    }
	    cursor.close();
	    
	    // check again for case insensitive	    
	    if(pageModel == null) {
		    cursor = rawQuery(db, "select * from " + TABLE_PAGE + " where lower(" + COLUMN_PAGE + ") = lower(?) ", new String[] {page});
			cursor.moveToFirst();
		    while (!cursor.isAfterLast()) {
		    	pageModel = cursorTopage(cursor);
		    	//Log.d(TAG, "Found Page: " + pageModel.toString());
		    	break;
		    }
		    cursor.close();
	    }
		return pageModel;
	}
	
	public ArrayList<PageModel> doSearch(SQLiteDatabase db, String searchStr) {
		ArrayList<PageModel> result = new ArrayList<PageModel>();
		Cursor cursor = rawQuery(db, "select * from " + TABLE_PAGE + " where "
				+ COLUMN_PAGE + " LIKE ? OR " + COLUMN_TITLE + " LIKE ? " 
				+ " ORDER BY CASE " + COLUMN_TYPE
				+ "   WHEN '" + PageModel.TYPE_NOVEL   + "' THEN 1 "
				+ "   WHEN '" + PageModel.TYPE_CONTENT + "' THEN 2 "
				+ "   ELSE 3 END, "
				+ COLUMN_PARENT + ", "
				+ COLUMN_ORDER + ", "
				+ COLUMN_TITLE 
				+ " LIMIT 100 "
				, new String[] { "%" + searchStr + "%", "%" + searchStr + "%" });
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			PageModel page = cursorTopage(cursor);
			result.add(page);
			cursor.moveToNext();
		}
		cursor.close();
		return result;
	}

	public void deletePageModel(SQLiteDatabase db, PageModel tempPage) {
		int result = delete(db, TABLE_PAGE, COLUMN_ID + " = ?", new String[]{"" + tempPage.getId()});
		Log.w(TAG, "PageModel Deleted: " + result);
	}
	
	public boolean deleteNovel(SQLiteDatabase db, PageModel novel) {
		try{
			// TODO: delete images
			
			// delete chapter and books
			NovelCollectionModel details = getNovelDetails(db, novel.getPage());
			if(details != null) {
				ArrayList<BookModel> books = details.getBookCollections();
				for (BookModel bookModel : books) {
					deleteBookModel(db, bookModel);
				}
				deleteNovelDetails(db, details);
			}
			// delete page model;
			deletePageModel(db, novel);
			return true;
		} catch(Exception ex) {
			Log.e(TAG, "Error when deleting: " + novel.getPage(), ex);
			return false;
		}
	}
	
	public void deleteNovelDetails(SQLiteDatabase db, NovelCollectionModel details) {
		int result = delete(db, TABLE_NOVEL_DETAILS, COLUMN_ID + " = ?", new String[]{"" + details.getId()});
		Log.w(TAG, "NovelDetails Deleted: " + result);
	}
	
	public PageModel getPageModel(SQLiteDatabase db, int id) {
		PageModel pageModel = null;		
		Cursor cursor = rawQuery(db, "select * from " + TABLE_PAGE + " where " + COLUMN_ID + " = ? ", new String[] {"" + id});
		cursor.moveToFirst();
	    while (!cursor.isAfterLast()) {
	    	pageModel = cursorTopage(cursor);
	    	//Log.d(TAG, "Found Page: " + pageModel.toString());
	    	break;
	    }
	    cursor.close();
	    return pageModel;
	}
	
	public ArrayList<PageModel> getAllNovels(SQLiteDatabase db) {
		ArrayList<PageModel> pages = new ArrayList<PageModel>();
		
		Cursor cursor = rawQuery(db, "select * from " + TABLE_PAGE + " where " + COLUMN_PARENT + " = ? " + " ORDER BY " + COLUMN_IS_WATCHED + " DESC, " + COLUMN_TITLE, 
									 new String[] {"Main_Page"});
		cursor.moveToFirst();
	    while (!cursor.isAfterLast()) {
	    	PageModel page = cursorTopage(cursor);
	    	pages.add(page);
	    	cursor.moveToNext();
	    }
	    cursor.close();
		return pages;
	}
	
	public ArrayList<PageModel> getAllTeaser(SQLiteDatabase db) {
		ArrayList<PageModel> pages = new ArrayList<PageModel>();
		
		Cursor cursor = rawQuery(db, "select * from " + TABLE_PAGE + " where " + COLUMN_PARENT + " = ? " + " ORDER BY " + COLUMN_IS_WATCHED + " DESC, " + COLUMN_TITLE, 
									 new String[] {"Category:Teasers"});
		cursor.moveToFirst();
	    while (!cursor.isAfterLast()) {
	    	PageModel page = cursorTopage(cursor);
	    	pages.add(page);
	    	cursor.moveToNext();
	    }
	    cursor.close();
		return pages;
	}
	
	public ArrayList<PageModel> selectAllByColumn(SQLiteDatabase db, String whereQuery, String[] values) {
		return selectAllByColumn(db, whereQuery, values, null);
	}
	
	public ArrayList<PageModel> selectAllByColumn(SQLiteDatabase db, String whereQuery, String[] values, String orderQuery) {
		ArrayList<PageModel> pages = new ArrayList<PageModel>();
		
		String sql = "select * from " + TABLE_PAGE + " where " + whereQuery;
		if(orderQuery != null && orderQuery.length() > 0) {
			sql += " order by " + orderQuery; 
		}
		
		Cursor cursor = rawQuery(db, sql, values);
		cursor.moveToFirst();
	    while (!cursor.isAfterLast()) {
	    	PageModel page = cursorTopage(cursor);
	    	pages.add(page);
	    	cursor.moveToNext();
	    }
	    cursor.close();		
		return pages;
	}
	
	public PageModel selectFirstBy(SQLiteDatabase db, String column, String value){
		//Log.d(TAG, "Select First: Column = " + column + " Value = " + value);
		PageModel page = null;
		
		Cursor cursor = rawQuery(db, "select * from " + TABLE_PAGE + " where " + column + " = ? ", new String[] {value});
		cursor.moveToFirst();
	    while (!cursor.isAfterLast()) {
	    	page = cursorTopage(cursor);
	    	//Log.d(TAG, "Found: " + page.toString());
	    	break;
	    }
	    cursor.close();
		return page;
	}
		
	public PageModel insertOrUpdatePageModel(SQLiteDatabase db, PageModel page){
		//Log.d(TAG, page.toString());
			
		PageModel temp = selectFirstBy(db, COLUMN_PAGE, page.getPage());
				
		ContentValues cv = new ContentValues();
		cv.put(COLUMN_PAGE, page.getPage());
		cv.put(COLUMN_TITLE, page.getTitle());
		cv.put(COLUMN_ORDER, page.getOrder());
		cv.put(COLUMN_PARENT, page.getParent());
		cv.put(COLUMN_TYPE, page.getType());
		cv.put(COLUMN_IS_WATCHED, page.isWatched());
		cv.put(COLUMN_IS_FINISHED_READ, page.isFinishedRead());
		cv.put(COLUMN_IS_DOWNLOADED, page.isDownloaded());
		cv.put(COLUMN_STATUS, page.getStatus());
		
		if(temp == null) {
			//Log.d(TAG, "Inserting: " + page.toString());
			if(page.getLastUpdate() == null) 
				cv.put(COLUMN_LAST_UPDATE, 0);
			else 
				cv.put(COLUMN_LAST_UPDATE, "" + (int) (page.getLastUpdate().getTime() / 1000));
			if(page.getLastCheck() == null)
				cv.put(COLUMN_LAST_CHECK, "" + (int) (new Date().getTime() / 1000));
			else 
				cv.put(COLUMN_LAST_CHECK, "" + (int) (page.getLastCheck().getTime() / 1000));

			long id = insertOrThrow(db, TABLE_PAGE, null, cv);
			Log.i(TAG, "Page Model Inserted, New Id: " + id);
		}
		else {
			//Log.d(TAG, "Updating: " + temp.toString());			
			if(page.getLastUpdate() == null) 
				cv.put(COLUMN_LAST_UPDATE, "" + (int) (temp.getLastUpdate().getTime() / 1000));
			else 
				cv.put(COLUMN_LAST_UPDATE, "" + (int) (page.getLastUpdate().getTime() / 1000));
			if(page.getLastCheck() == null)
				cv.put(COLUMN_LAST_CHECK, "" + (int) (temp.getLastCheck().getTime() / 1000));
			else
				cv.put(COLUMN_LAST_CHECK, "" + (int) (page.getLastCheck().getTime() / 1000));
			
			int result = update(db, TABLE_PAGE, cv, COLUMN_ID + " = ?", new String[] {"" + temp.getId()});
			Log.i(TAG, "Page Model: " + page.getPage() + " Updated, Affected Row: " + result);
		}
		
		// get the updated data.
		page = getPageModel(db, page.getPage());
		return page;
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
		page.setFinishedRead(cursor.getInt(8) == 1 ? true : false);
		page.setDownloaded(cursor.getInt(9) == 1 ? true : false);
		page.setOrder(cursor.getInt(10));
		page.setStatus(cursor.getString(11));
	    return page;
	}
	
	/*
	 * NovelCollectionModel
	 * Nested Object: 
	 * - ArrayList<BookModel>
	 *   - ArrayList<PageModel>
	 */	
	public NovelCollectionModel insertNovelDetails(SQLiteDatabase db, NovelCollectionModel novelDetails){
		ContentValues cv = new ContentValues();
		cv.put(COLUMN_PAGE, novelDetails.getPage());
		cv.put(COLUMN_SYNOPSIS, novelDetails.getSynopsis());
		cv.put(COLUMN_IMAGE, novelDetails.getCover());
		cv.put(COLUMN_LAST_CHECK, "" + (int) (new Date().getTime() / 1000));
		
		// check if exist
		NovelCollectionModel temp = getNovelDetailsOnly(db, novelDetails.getPage());
		if(temp == null) {
			//Log.d(TAG, "Inserting Novel Details: " + novelDetails.getPage());
			if(novelDetails.getLastUpdate() == null)
				cv.put(COLUMN_LAST_UPDATE, 0);
			else
				cv.put(COLUMN_LAST_UPDATE, "" + (int) (novelDetails.getLastUpdate().getTime() / 1000));
			insertOrThrow(db, TABLE_NOVEL_DETAILS, null, cv);
		}
		else {
			//Log.d(TAG, "Updating Novel Details: " + novelDetails.getPage() + " id: " + temp.getId());
			cv.put(COLUMN_LAST_UPDATE, "" + (int) (temp.getLastUpdate().getTime() / 1000));
			update(db, TABLE_NOVEL_DETAILS, cv, COLUMN_ID + " = ?", new String[] {"" + temp.getId()});
		}

		for(Iterator<BookModel> iBooks = novelDetails.getBookCollections().iterator(); iBooks.hasNext();){
			BookModel book = iBooks.next();
			ContentValues cv2 = new ContentValues();
			cv2.put(COLUMN_PAGE, novelDetails.getPage());
			cv2.put(COLUMN_TITLE , book.getTitle());
			cv2.put(COLUMN_ORDER , book.getOrder());
			cv2.put(COLUMN_LAST_CHECK, "" + (int) (new Date().getTime() / 1000));
			
			BookModel tempBook = getBookModel(db, book.getId());
			if(tempBook == null) tempBook = getBookModel(db, novelDetails.getPage(), book.getTitle());
			if(tempBook == null) {
				//Log.d(TAG, "Inserting Novel Book: " + novelDetails.getPage() + Constants.NOVEL_BOOK_DIVIDER + book.getTitle());
				if(novelDetails.getLastUpdate() == null)
					cv2.put(COLUMN_LAST_UPDATE, 0);
				else
					cv2.put(COLUMN_LAST_UPDATE, "" + (int) (novelDetails.getLastUpdate().getTime() / 1000));
				insertOrThrow(db, TABLE_NOVEL_BOOK, null, cv2);
			}
			else {
				//Log.d(TAG, "Updating Novel Book: " + tempBook.getPage() + Constants.NOVEL_BOOK_DIVIDER + tempBook.getTitle() + " id: " + tempBook.getId());
				cv2.put(COLUMN_LAST_UPDATE, "" + (int) (tempBook.getLastUpdate().getTime() / 1000));
				update(db, TABLE_NOVEL_BOOK, cv2, COLUMN_ID + " = ?", new String[] {"" + tempBook.getId()});
			}
		}
		
		for(Iterator<BookModel> iBooks = novelDetails.getBookCollections().iterator(); iBooks.hasNext();){
			BookModel book = iBooks.next();
			for(Iterator<PageModel> iPage = book.getChapterCollection().iterator(); iPage.hasNext();) {
				PageModel page = iPage.next();
				ContentValues cv3 = new ContentValues();
				cv3.put(COLUMN_PAGE, page.getPage());
				cv3.put(COLUMN_TITLE, page.getTitle());
				cv3.put(COLUMN_TYPE, page.getType());
				cv3.put(COLUMN_PARENT, page.getParent());
				cv3.put(COLUMN_ORDER, page.getOrder());
				
				cv3.put(COLUMN_LAST_CHECK, "" + (int) (new Date().getTime() / 1000));
				cv3.put(COLUMN_IS_WATCHED, false);
				
				PageModel tempPage = getPageModel(db, page.getPage());
				if(tempPage == null) {
					//Log.d(TAG, "Inserting Novel Chapter: " + page.getPage());
					if(page.getLastUpdate() == null)
						cv3.put(COLUMN_LAST_UPDATE, 0);
					else 
						cv3.put(COLUMN_LAST_UPDATE, "" + (int) (page.getLastUpdate().getTime() / 1000));
					insertOrThrow(db, TABLE_PAGE, null, cv3);
				}
				else {
					cv3.put(COLUMN_LAST_UPDATE, "" + (int) (tempPage.getLastUpdate().getTime() / 1000));
					//Log.d(TAG, "Updating Novel Chapter: " + page.getPage() + " id: " +tempPage.getId());
					update(db, TABLE_PAGE, cv3, COLUMN_ID + " = ?", new String[] {"" + tempPage.getId()});
				}
			}
		}

		//Log.d(TAG, "Complete Insert Novel Details: " + novelDetails.toString());

		// get updated data
		novelDetails = getNovelDetails(db, novelDetails.getPage());
		return novelDetails;
	}
	
	private NovelCollectionModel getNovelDetailsOnly(SQLiteDatabase db, String page) {
		NovelCollectionModel novelDetails = null;
		Cursor cursor = rawQuery(db, "select * from " + TABLE_NOVEL_DETAILS + " where " + COLUMN_PAGE + " = ? ", new String[] {page});
		cursor.moveToFirst();
	    while (!cursor.isAfterLast()) {
	    	novelDetails = cursorToNovelCollection(cursor);
	    	//Log.d(TAG, "Found: " + novelDetails.toString());
	    	break;
	    }
	    cursor.close();
	    return novelDetails;
	}
	
	public BookModel getBookModel(SQLiteDatabase db, int id) {
		BookModel book = null;
		Cursor cursor = rawQuery(db, "select * from " + TABLE_NOVEL_BOOK + " where " + COLUMN_ID + " = ? ", new String[] {"" + id});
		cursor.moveToFirst();
	    while (!cursor.isAfterLast()) {
	    	book = cursorToBookModel(cursor);
	    	//Log.d(TAG, "Found: " + book.getPage() + Constants.NOVEL_BOOK_DIVIDER + book.getTitle());
	    	break;
	    }
	    cursor.close();
	    return book;
	}
	
	public BookModel getBookModel(SQLiteDatabase db, String page, String title) {
		BookModel book = null;
		Cursor cursor = rawQuery(db, "select * from " + TABLE_NOVEL_BOOK + " where " + COLUMN_PAGE + " = ? and " + COLUMN_TITLE + " = ?", new String[] {page, title});
		cursor.moveToFirst();
	    while (!cursor.isAfterLast()) {
	    	book = cursorToBookModel(cursor);
	    	//Log.d(TAG, "Found: " + book.getPage() + Constants.NOVEL_BOOK_DIVIDER + book.getTitle());
	    	break;
	    }
	    cursor.close();
	    return book;
	}
	
	public void deleteBookModel(SQLiteDatabase db, BookModel book) {
		int chaptersCount = 0;
		ArrayList<PageModel> chapters = book.getChapterCollection();
		if(chapters != null && chapters.size() > 0) {
			for(Iterator<PageModel> i = chapters.iterator(); i.hasNext();) {
				PageModel page = i.next();
				chaptersCount += delete(db, TABLE_PAGE, COLUMN_ID + " = ? ", new String[] {"" + page.getId()});
			}
			Log.w(TAG, "Deleted PageModel: " + chaptersCount);
		}
		int bookCount = delete(db, TABLE_NOVEL_BOOK, COLUMN_ID + " = ? ", new String[] { "" + book.getId() });
		Log.w(TAG, "Deleted BookModel: " + bookCount);
	}

	public ArrayList<BookModel> getBookCollectionOnly(SQLiteDatabase db, String page, NovelCollectionModel novelDetails) {
		// get the books
	    ArrayList<BookModel> bookCollection = new ArrayList<BookModel>(); 
	    Cursor cursor = rawQuery(db, "select * from " + TABLE_NOVEL_BOOK + " where " + COLUMN_PAGE + " = ? order by " + COLUMN_ORDER, new String[] {page});
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			BookModel book = cursorToBookModel(cursor);
			book.setParent(novelDetails);
			bookCollection.add(book);
	    	//Log.d(TAG, "Found: " + book.toString());
	    	cursor.moveToNext();
	    }
	    cursor.close();
		return bookCollection;
	}
	
	public ArrayList<PageModel> getChapterCollection(SQLiteDatabase db, String parent, BookModel book) {
		ArrayList<PageModel> chapters = new ArrayList<PageModel>();
		Cursor cursor = rawQuery(db, "select * from " + TABLE_PAGE + " where " + COLUMN_PARENT + " = ? order by " + COLUMN_ORDER, new String[] {parent});
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			PageModel chapter = cursorTopage(cursor);
			chapter.setBook(book);
			chapters.add(chapter);
	    	//Log.d(TAG, "Found: " + chapter.toString());
	    	cursor.moveToNext();
	    }
	    cursor.close();
		return chapters;
	}
	
	public NovelCollectionModel getNovelDetails(SQLiteDatabase db, String page) {
		//Log.d(TAG, "Selecting Novel Details: " + page);
		NovelCollectionModel novelDetails = getNovelDetailsOnly(db, page);
		
	    if(novelDetails != null) {
	    	novelDetails.setPageModel(getPageModel(db, page));
			
		    // get the books
		    ArrayList<BookModel> bookCollection = getBookCollectionOnly(db, page, novelDetails);	
			// get the chapters
			for(Iterator<BookModel> iBook = bookCollection.iterator(); iBook.hasNext();) {
				BookModel book = iBook.next();
				ArrayList<PageModel> chapters = getChapterCollection(db, novelDetails.getPage() + Constants.NOVEL_BOOK_DIVIDER + book.getTitle(), book);
				book.setChapterCollection(chapters);
			}		
			novelDetails.setBookCollections(bookCollection);
	    }
	    else {
	    	Log.w(TAG, "No Data for Novel Details: " + page);
	    }
		
		//Log.d(TAG, "Complete Selecting Novel Details: " + page);
		return novelDetails;
	}
	
	private BookModel cursorToBookModel(Cursor cursor) {
		BookModel book = new BookModel();
		book.setId(cursor.getInt(0));
		book.setPage(cursor.getString(1));
		book.setTitle(cursor.getString(2));
		book.setLastUpdate(new Date(cursor.getInt(3)*1000));
		book.setLastCheck(new Date(cursor.getInt(4)*1000));
		book.setOrder(cursor.getInt(5));
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

	/*
	 * ImageModel
	 * No Nested Object
	 */	
	public ImageModel insertImage(SQLiteDatabase db, ImageModel image){
		ImageModel temp = getImage(db, image.getName());
		
		ContentValues cv = new ContentValues();
		cv.put(COLUMN_IMAGE, image.getName());
		cv.put(COLUMN_FILEPATH, image.getPath());
		cv.put(COLUMN_URL, image.getUrl().toString());
		cv.put(COLUMN_REFERER, image.getReferer());
		if(temp == null) {
			cv.put(COLUMN_LAST_UPDATE, "" + (int) (new Date().getTime() / 1000));
			cv.put(COLUMN_LAST_CHECK, "" + (int) (new Date().getTime() / 1000));
			insertOrThrow(db, TABLE_IMAGE, null, cv);
			Log.i(TAG, "Complete Insert Images: " + image.getName() + " Ref: " +  image.getReferer());
		}
		else {			
			cv.put(COLUMN_LAST_UPDATE, "" + (int) (temp.getLastUpdate().getTime() / 1000));
			cv.put(COLUMN_LAST_CHECK, "" + (int) (new Date().getTime() / 1000));
			update(db, TABLE_IMAGE, cv, COLUMN_ID + " = ?", new String[] {"" + temp.getId()});
			Log.i(TAG, "Complete Update Images: " + image.getName() + " Ref: " +  image.getReferer());
		}
		// get updated data
		image = getImage(db, image.getName());
		
		//Log.d(TAG, "Complete Insert Images: " + image.getName() + " id: " + image.getId());
		
		return image;
	}
	
	public ImageModel getImageByReferer(SQLiteDatabase db, String url) {
		//Log.d(TAG, "Selecting Image by Referer: " + url);
		ImageModel image = null;
		
		Cursor cursor = rawQuery(db, "select * from " + TABLE_IMAGE + " where " + COLUMN_REFERER + " = ? ", new String[] {url});
		cursor.moveToFirst();
	    while (!cursor.isAfterLast()) {
	    	image = cursorToImage(cursor);
	    	//Log.d(TAG, "Found: " + image.getName() + " id: " + image.getId());
	    	break;
	    }
	    cursor.close();
		
		if(image == null) {
			Log.w(TAG, "Not Found Image by Referer: " + url);
		}
		return image;
	}
	
	public ImageModel getImage(SQLiteDatabase db, String name) {
		//Log.d(TAG, "Selecting Image: " + name);
		ImageModel image = null;
		
		Cursor cursor = rawQuery(db, "select * from " + TABLE_IMAGE + " where " + COLUMN_IMAGE + " = ? ", new String[] {name});
		cursor.moveToFirst();
	    while (!cursor.isAfterLast()) {
	    	image = cursorToImage(cursor);
	    	//Log.d(TAG, "Found: " + image.getName() + " id: " + image.getId());
	    	break;
	    }
	    cursor.close();

		if(image == null) {
			Log.w(TAG, "Not Found Image: " + name);
		}
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
		image.setReferer(cursor.getString(4));
		image.setLastUpdate(new Date(cursor.getInt(5)*1000));
		image.setLastCheck(new Date(cursor.getInt(6)*1000));
		return image;
	}
	
	/*
	 * NovelContentModel
	 * Nested object : PageModel, lazy loading via NovelsDao
	 */	
	public NovelContentModel insertNovelContent(SQLiteDatabase db, NovelContentModel content) throws Exception {
		ContentValues cv = new ContentValues();
		cv.put(COLUMN_CONTENT, content.getContent());
		cv.put(COLUMN_PAGE, content.getPage());
		cv.put(COLUMN_LAST_X, "" + content.getLastXScroll());
		cv.put(COLUMN_LAST_Y, "" + content.getLastYScroll());
		cv.put(COLUMN_ZOOM, "" + content.getLastZoom());
		cv.put(COLUMN_LAST_CHECK, "" + (int) (new Date().getTime() / 1000));
		
		NovelContentModel temp = getNovelContent(db, content.getPage());
		if(temp == null){
			//Log.d(TAG, "Inserting Novel Content: " + content.getPage());
			if(content.getLastUpdate() == null)
				cv.put(COLUMN_LAST_UPDATE, 0);
			else
				cv.put(COLUMN_LAST_UPDATE, "" + (int) (content.getLastUpdate().getTime() / 1000));			
			long id = insertOrThrow(db, TABLE_NOVEL_CONTENT, null, cv);
			Log.i(TAG, "Novel Content Inserted, New id: "  + id);
		}
		else {
			//Log.d(TAG, "Updating Novel Content: " + content.getPage() + " id: " + temp.getId());
			if(content.getLastUpdate() == null)
				cv.put(COLUMN_LAST_UPDATE, "" + (int) (temp.getLastUpdate().getTime() / 1000));
			else
				cv.put(COLUMN_LAST_UPDATE, "" + (int) (content.getLastUpdate().getTime() / 1000));		
			int result = update(db, TABLE_NOVEL_CONTENT, cv, COLUMN_ID + " = ? ", new String[] {"" + temp.getId()});
			Log.i(TAG, "Novel Content:" + content.getPage() + " Updated, Affected Row: "  + result);
		}
		
		// update the pageModel
		PageModel pageModel = content.getPageModel();
		if(pageModel != null) {
			pageModel.setDownloaded(true);
			pageModel = insertOrUpdatePageModel(db, pageModel);
		}
				
		content = getNovelContent(db, content.getPage());
		return content;
	}
	
	public NovelContentModel getNovelContent(SQLiteDatabase db, String page) {
		//Log.d(TAG, "Selecting Novel Content: " + page);
		NovelContentModel content = null;
		
		Cursor cursor = rawQuery(db, "select * from " + TABLE_NOVEL_CONTENT + " where " + COLUMN_PAGE + " = ? ", new String[] {page});
		cursor.moveToFirst();
	    while (!cursor.isAfterLast()) {
	    	content = cursorToNovelContent(cursor);
	    	//Log.d(TAG, "Found: " + content.getPage() + " id: " + content.getId());
	    	break;
	    }
	    cursor.close();
		if(content == null) {
			Log.w(TAG, "Not Found Novel Content: " + page);
		}		
		return content;
	}

	private NovelContentModel cursorToNovelContent(Cursor cursor) {
		NovelContentModel content = new NovelContentModel();
		content.setId(cursor.getInt(0));
		content.setContent(cursor.getString(1));
		content.setPage(cursor.getString(2));
		content.setLastXScroll(cursor.getInt(3));
		content.setLastYScroll(cursor.getInt(4));
		content.setLastZoom(cursor.getDouble(5));
		content.setLastUpdate(new Date(cursor.getLong(6)*1000));
		content.setLastCheck(new Date(cursor.getLong(7)*1000));
		return content;
	}
	
	/*
	 * To avoid android.database.sqlite.SQLiteException: unable to close due to unfinalised statements
	 */
	private Cursor rawQuery(SQLiteDatabase db, String sql, String[] values){
		if(!db.isOpen()) 
			db = getReadableDatabase();	
		return db.rawQuery(sql, values);
	}
	
	private int update(SQLiteDatabase db, String table, ContentValues cv, String whereClause, String[] whereParams){
		if (!db.isOpen())
			db = getWritableDatabase();
		return db.update(table, cv, whereClause, whereParams);
	}
	
	private long insertOrThrow(SQLiteDatabase db, String table, String nullColumnHack, ContentValues cv){
		if (!db.isOpen())
			db = getWritableDatabase();
		return db.insertOrThrow(table, nullColumnHack, cv);
	}
	
	private int delete(SQLiteDatabase db, String table, String whereClause, String[] whereParams) {
		if (!db.isOpen())
			db = getWritableDatabase();
		return db.delete(table, whereClause, whereParams);
	}


}
