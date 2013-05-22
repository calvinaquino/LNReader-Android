//package com.nandaka.bakareaderclone.helper;
package com.erakk.lnreader.helper;

import java.io.File;
import java.io.IOException;
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
import com.erakk.lnreader.model.BookmarkModel;
import com.erakk.lnreader.model.ImageModel;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.NovelContentModel;
import com.erakk.lnreader.model.PageModel;
import com.erakk.lnreader.model.UpdateInfoModel;
import com.erakk.lnreader.model.UpdateType;

public class DBHelper extends SQLiteOpenHelper {
	public static final String TAG = DBHelper.class.toString();
	public static final String TABLE_PAGE = "pages";
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_PAGE = "page";
	public static final String COLUMN_LANGUAGE = "language";
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
	public static final String COLUMN_IS_MISSING = "is_missing";
	public static final String COLUMN_IS_EXTERNAL = "is_external";

	public static final String TABLE_IMAGE = "images";
	public static final String COLUMN_IMAGE_NAME = "name";
	public static final String COLUMN_FILEPATH = "filepath";
	public static final String COLUMN_URL = "url";
	public static final String COLUMN_REFERER = "referer";
	public static final String COLUMN_IS_BIG_IMAGE = "is_big_image";

	public static final String TABLE_NOVEL_DETAILS = "novel_details";
	public static final String COLUMN_SYNOPSIS = "synopsis";

	public static final String TABLE_NOVEL_BOOK = "novel_books";

	public static final String TABLE_NOVEL_CONTENT = "novel_books_content";
	public static final String COLUMN_CONTENT = "content";
	public static final String COLUMN_LAST_X = "lastXScroll";
	public static final String COLUMN_LAST_Y = "lastYScroll";
	public static final String COLUMN_ZOOM = "lastZoom";

	public static final String TABLE_NOVEL_BOOKMARK = "novel_bookmark";
	public static final String COLUMN_PARAGRAPH_INDEX = "p_index";
	public static final String COLUMN_EXCERPT = "excerpt";
	public static final String COLUMN_CREATE_DATE = "create_date";

	public static final String TABLE_UPDATE_HISTORY = "update_history";
	public static final String COLUMN_UPDATE_TITLE = "update_title";
	public static final String COLUMN_UPDATE_TYPE = "update_type";

	public static final String DATABASE_NAME = "pages.db";
	public static final int DATABASE_VERSION = 26;

	// Database creation SQL statement
	// New column should be appended as the last column
	private static final String DATABASE_CREATE_PAGES = "create table if not exists "
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
			  				 + COLUMN_STATUS + " text, "							// 11
			  				 + COLUMN_IS_MISSING + " boolean, "						// 12
			  				 + COLUMN_IS_EXTERNAL + " boolean, "					// 13
							 + COLUMN_LANGUAGE + " text not null default '" + Constants.LANG_ENGLISH+ "');";                // 14

	private static final String DATABASE_CREATE_IMAGES = "create table if not exists "
		      + TABLE_IMAGE + "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "	// 0
				 				  + COLUMN_IMAGE_NAME + " text unique not null, "		// 1
				  				  + COLUMN_FILEPATH + " text not null, "				// 2
				  				  + COLUMN_URL + " text not null, "						// 3
				  				  + COLUMN_REFERER + " text, "							// 4
				  				  + COLUMN_LAST_UPDATE + " integer, "					// 5
				  				  + COLUMN_LAST_CHECK + " integer, "					// 6
				  				  + COLUMN_IS_BIG_IMAGE + " boolean);";					// 7

	private static final String DATABASE_CREATE_NOVEL_DETAILS = "create table if not exists "
		      + TABLE_NOVEL_DETAILS + "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				 				  + COLUMN_PAGE + " text unique not null, "
				  				  + COLUMN_SYNOPSIS + " text not null, "
				  				  + COLUMN_IMAGE_NAME + " text not null, "
				  				  + COLUMN_LAST_UPDATE + " integer, "
				  				  + COLUMN_LAST_CHECK + " integer);";

	// COLUMN_PAGE is not unique because being used for reference to the novel page.
	private static final String DATABASE_CREATE_NOVEL_BOOKS = "create table if not exists "
		      + TABLE_NOVEL_BOOK + "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "	// 0
				 				  + COLUMN_PAGE + " text not null, "						// 1
				  				  + COLUMN_TITLE + " text not null, "						// 2
				  				  + COLUMN_LAST_UPDATE + " integer, "						// 3
				  				  + COLUMN_LAST_CHECK + " integer, "						// 4
				  				  + COLUMN_ORDER + " integer);";							// 5

	private static final String DATABASE_CREATE_NOVEL_CONTENT = "create table if not exists "
		      + TABLE_NOVEL_CONTENT + "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "	// 0
				 				    + COLUMN_CONTENT + " text not null, "						// 1
		      						+ COLUMN_PAGE + " text unique not null, "					// 2
				  				    + COLUMN_LAST_X + " integer, "								// 3
				  				    + COLUMN_LAST_Y + " integer, "								// 4
				  				    + COLUMN_ZOOM + " double, "									// 5
				  				    + COLUMN_LAST_UPDATE + " integer, "							// 6
				  				    + COLUMN_LAST_CHECK + " integer);";							// 7

	private static final String DATABASE_CREATE_NOVEL_BOOKMARK = "create table if not exists "
		      + TABLE_NOVEL_BOOKMARK + "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "	// 0
		      						+ COLUMN_PAGE + " text not null, "							// 1
				  				    + COLUMN_PARAGRAPH_INDEX + " integer, "						// 2
				  				    + COLUMN_EXCERPT + " text, "								// 3
				  				    + COLUMN_CREATE_DATE + " integer);";						// 4

	private static final String DATABASE_CREATE_UPDATE_HISTORY = "create table if not exists "
			  + TABLE_UPDATE_HISTORY + "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "	// 0
			  						+ COLUMN_PAGE + " text not null, "							// 1
			  						+ COLUMN_UPDATE_TITLE + " text not null, "					// 2
			  						+ COLUMN_UPDATE_TYPE + " integer not null, "				// 3
			  						+ COLUMN_LAST_UPDATE + " integer);";						// 4

	// Use /files/database to standarize with newer android.
	public static final String DB_ROOT_SD = Environment.getExternalStorageDirectory().getAbsolutePath().toString() + "/Android/data/" + Constants.class.getPackage().getName() + "/files/databases";

	public static String getDbPath(Context context) {
		String dbPath = null;
		File path = context.getExternalFilesDir(null);
		if(path != null)
			dbPath = path.getAbsolutePath() + "/databases/" + DATABASE_NAME;
		else {
			path = new File(DB_ROOT_SD);
			if(!( path.mkdirs() || path.isDirectory())) Log.e(TAG, "DB Path doesn't exists/failed to create.");
			// throw new Exception("Failed to create db directory: " + DB_ROOT_SD);
			dbPath = DB_ROOT_SD + "/" + DATABASE_NAME;
		}
		Log.d(TAG, "DB Path : " + dbPath);
		return dbPath;
	}

	public DBHelper(Context context) {
		super(context, getDbPath(context), null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		 db.execSQL(DATABASE_CREATE_PAGES);
		 db.execSQL(DATABASE_CREATE_IMAGES);
		 db.execSQL(DATABASE_CREATE_NOVEL_DETAILS);
		 db.execSQL(DATABASE_CREATE_NOVEL_BOOKS);
		 db.execSQL(DATABASE_CREATE_NOVEL_CONTENT);
		 db.execSQL(DATABASE_CREATE_NOVEL_BOOKMARK);
		 db.execSQL(DATABASE_CREATE_UPDATE_HISTORY);
	}

	@Override
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		onUpgrade(db, oldVersion, newVersion);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(TAG, "Upgrading db from version " + oldVersion + " to " + newVersion);
		if(oldVersion < 18) {
			Log.w(TAG, "DB version is less than 18, recreate DB");
		    db.execSQL("DROP TABLE IF EXISTS " + TABLE_PAGE);
		    db.execSQL("DROP TABLE IF EXISTS " + TABLE_IMAGE);
		    db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOVEL_DETAILS);
		    db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOVEL_BOOK);
		    db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOVEL_CONTENT);
		    db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOVEL_BOOKMARK);
			onCreate(db);
			return;
		}
		if(oldVersion == 18) {
			db.execSQL("ALTER TABLE " + TABLE_PAGE + " ADD COLUMN " + COLUMN_STATUS + " text" );
			oldVersion = 19;
		}
		if(oldVersion == 19) {
			db.execSQL(DATABASE_CREATE_NOVEL_BOOKMARK);
			oldVersion = 21; // skip the alter table
		}
		if(oldVersion == 20) {
			db.execSQL("ALTER TABLE " + TABLE_NOVEL_BOOKMARK + " ADD COLUMN " + COLUMN_EXCERPT + " text" );
			db.execSQL("ALTER TABLE " + TABLE_NOVEL_BOOKMARK + " ADD COLUMN " + COLUMN_CREATE_DATE + " integer" );
			oldVersion = 21;
		}
		if(oldVersion == 21) {
			db.execSQL("ALTER TABLE " + TABLE_PAGE + " ADD COLUMN " + COLUMN_IS_MISSING + " boolean" );
			oldVersion = 22;
		}
		if(oldVersion == 22) {
			db.execSQL("ALTER TABLE " + TABLE_PAGE + " ADD COLUMN " + COLUMN_IS_EXTERNAL + " boolean" );
			oldVersion = 23;
		}
		if(oldVersion == 23) {
			db.execSQL(DATABASE_CREATE_UPDATE_HISTORY);
			oldVersion = 24;
		}
		if(oldVersion == 24) {
			db.execSQL("ALTER TABLE " + TABLE_IMAGE + " ADD COLUMN " + COLUMN_IS_BIG_IMAGE + " boolean" );
			oldVersion = 25;
		}
		if(oldVersion == 25) {
			db.execSQL("ALTER TABLE " + TABLE_PAGE + " ADD COLUMN " + COLUMN_LANGUAGE + " text not null default '" + Constants.LANG_ENGLISH + "'");
			oldVersion = 26;
		}
	}

	public void deletePagesDB(SQLiteDatabase db) {
		// use drop because it is faster and can avoid free row fragmentation
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_PAGE);
	    db.execSQL("DROP TABLE IF EXISTS " + TABLE_IMAGE);
	    db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOVEL_DETAILS);
	    db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOVEL_BOOK);
	    db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOVEL_CONTENT);
	    db.execSQL("DROP TABLE IF EXISTS " + TABLE_UPDATE_HISTORY);
	    onCreate(db);
		Log.w(TAG,"Database Deleted.");
	}


	public String copyDB(SQLiteDatabase db, Context context, boolean makeBackup) throws IOException {
		Log.d("DatabaseManager", "creating database backup");
		File srcPath;
		File dstPath;
		if (makeBackup) {
			srcPath = new File(getDbPath(context));
			dstPath = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Backup_pages.db");
		}
		else {
			dstPath = new File(getDbPath(context));
			srcPath = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Backup_pages.db");
		}
		Log.d("DatabaseManager", "source file: "+ srcPath.getAbsolutePath());
		Log.d("DatabaseManager", "destination file: "+ dstPath.getAbsolutePath());
		if (srcPath.exists()) {
			Util.copyFile(srcPath, dstPath);
			Log.d("DatabaseManager", "copy success");
			return dstPath.getPath();
		}
		else {
			Log.d("DatabaseManager", "source file does not exist");
			return "null";
		}
	}

	public PageModel getMainPage(SQLiteDatabase db) {
		//Log.d(TAG, "Select Main_Page");
		PageModel page = getPageModel(db, Constants.ROOT_NOVEL);
		return page;
	}

	public PageModel getTeaserPage(SQLiteDatabase db) {
		PageModel page = getPageModel(db, "Category:Teasers");
		return page;
	}

	public PageModel getOriginalPage(SQLiteDatabase db) {
		PageModel page = getPageModel(db, "Category:Original");
		return page;
	}

	public PageModel getAlternativePage(SQLiteDatabase db, String language) {
		/* Return PageModel depends on language */
		PageModel page = null;
		if (language.equals(Constants.LANG_BAHASA_INDONESIA)) page = getPageModel(db, "Category:Indonesian");
		return page;
	}

	public ArrayList<PageModel> insertAllNovel(SQLiteDatabase db, ArrayList<PageModel> list) {
		ArrayList<PageModel> updatedList = new ArrayList<PageModel>();
		for(Iterator<PageModel> i = list.iterator(); i.hasNext();){
			PageModel p = i.next();
			p = insertOrUpdatePageModel(db, p, false);
			updatedList.add(p);
		}
		return updatedList;
	}

	public PageModel getPageModel(SQLiteDatabase db, String page) {
		//Log.d(TAG, "Select Page: " + page);
		PageModel pageModel = null;
		Cursor cursor = null;
		try{
			cursor = rawQuery(db, "select * from " + TABLE_PAGE + " where " + COLUMN_PAGE + " = ? ", new String[] {page});
			cursor.moveToFirst();
		    while (!cursor.isAfterLast()) {
		    	pageModel = cursorToPageModel(cursor);
		    	//Log.d(TAG, "Found Page: " + pageModel.toString());
		    	break;
		    }
		}
		finally{
			if(cursor != null) cursor.close();
		}

	    // check again for case insensitive
	    if(pageModel == null) {
	    	try{
		    cursor = rawQuery(db, "select * from " + TABLE_PAGE + " where lower(" + COLUMN_PAGE + ") = lower(?) ", new String[] {page});
			cursor.moveToFirst();
		    while (!cursor.isAfterLast()) {
		    	pageModel = cursorToPageModel(cursor);
		    	//Log.d(TAG, "Found Page: " + pageModel.toString());
		    	break;
		    }
	    	}
	    	finally{
	    		if(cursor != null) cursor.close();
	    	}
	    }
		return pageModel;
	}

	public boolean isContentUpdated(SQLiteDatabase db, PageModel page) {
		//Log.d(TAG, "isContentUpdated is called by: " + page.getPage());
		String sql = "select case when " + TABLE_PAGE + "." + COLUMN_LAST_UPDATE + " != " + TABLE_NOVEL_CONTENT + "." + COLUMN_LAST_UPDATE + " then 1 else 0 end "
					 + " from " + TABLE_PAGE + " join " + TABLE_NOVEL_CONTENT + " using (" + COLUMN_PAGE + ") "
					 + "where " + COLUMN_PAGE + " = ? ";
		Cursor cursor = rawQuery(db, sql,  new String[] {page.getPage()});
		try{
			cursor.moveToFirst();
		    while (!cursor.isAfterLast()) {
		    	return cursor.getInt(0) == 1 ? true : false;
		    }
		}
	    finally{
	    	if(cursor != null) cursor.close();
	    }

		return false;
	}

	public int isNovelUpdated(SQLiteDatabase db, PageModel novelPage) {
		String sql = "select r.page, sum(r.hasUpdates) " +
	                 "from ( select " + TABLE_NOVEL_DETAILS + "." + COLUMN_PAGE +
				     "            , case when " + TABLE_PAGE + "." + COLUMN_LAST_UPDATE + " != " + TABLE_NOVEL_CONTENT + "." + COLUMN_LAST_UPDATE +
				     "                   then 1 else 0 end as hasUpdates " +
				     "       from " + TABLE_NOVEL_DETAILS +
				     "       join " + TABLE_NOVEL_BOOK + " on " + TABLE_NOVEL_DETAILS + "." + COLUMN_PAGE + " = " + TABLE_NOVEL_BOOK + "." + COLUMN_PAGE +
				     "       join " + TABLE_PAGE       + " on " + TABLE_PAGE + "." + COLUMN_PARENT        + " = " + TABLE_NOVEL_DETAILS + "." + COLUMN_PAGE + " || '" + Constants.NOVEL_BOOK_DIVIDER + "' || " + TABLE_NOVEL_BOOK + "." + COLUMN_TITLE +
				     "       join " + TABLE_NOVEL_CONTENT + " on " + TABLE_NOVEL_CONTENT + "." + COLUMN_PAGE + " = " + TABLE_PAGE + "." + COLUMN_PAGE +
				     "       where " + TABLE_NOVEL_DETAILS + "." + COLUMN_PAGE + " = ? " +
				     ") r group by r.page ";
		Cursor cursor = rawQuery(db, sql,  new String[] {novelPage.getPage()});
		try {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				return cursor.getInt(1);
			}
		} finally{
			if(cursor != null) cursor.close();
		}

		return 0;
	}

	public ArrayList<PageModel> doSearch(SQLiteDatabase db, String searchStr, boolean isNovelOnly) {
		ArrayList<PageModel> result = new ArrayList<PageModel>();

		String sql = null;
		if(isNovelOnly) {
			sql = "select * from " + TABLE_PAGE + " WHERE "
		            + COLUMN_TYPE + " = '" + PageModel.TYPE_NOVEL + "' AND "
		            + COLUMN_LANGUAGE + " = 'English' AND ("
					+ COLUMN_PAGE + " LIKE ? OR " + COLUMN_TITLE + " LIKE ? )"
					+ " ORDER BY "
					+ COLUMN_PARENT + ", "
					+ COLUMN_ORDER + ", "
					+ COLUMN_TITLE
					+ " LIMIT 100 ";
			Log.d(TAG, "Novel Only");
		}
		else {
			sql = "select * from " + TABLE_PAGE + " WHERE "
		            + COLUMN_LANGUAGE + " = 'English' AND ("
					+ COLUMN_PAGE + " LIKE ? OR " + COLUMN_TITLE + " LIKE ? )"
					+ " ORDER BY CASE " + COLUMN_TYPE
					+ "   WHEN '" + PageModel.TYPE_NOVEL   + "' THEN 1 "
					+ "   WHEN '" + PageModel.TYPE_CONTENT + "' THEN 2 "
					+ "   ELSE 3 END, "
					+ COLUMN_PARENT + ", "
					+ COLUMN_ORDER + ", "
					+ COLUMN_TITLE
					+ " LIMIT 100 ";
			Log.d(TAG, "All Items");
		}
		Cursor cursor = rawQuery(db, sql , new String[] { "%" + searchStr + "%", "%" + searchStr + "%" });
		try {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				PageModel page = cursorToPageModel(cursor);
				result.add(page);
				cursor.moveToNext();
			}
		} finally{
			if(cursor != null) cursor.close();
		}
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
		try {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				pageModel = cursorToPageModel(cursor);
				//Log.d(TAG, "Found Page: " + pageModel.toString());
				break;
			}
		} finally{
			if(cursor != null) cursor.close();
		}
	    return pageModel;
	}

	public ArrayList<PageModel> getAllNovels(SQLiteDatabase db, boolean alphOrder) {
		ArrayList<PageModel> pages = new ArrayList<PageModel>();

		String sql = "select * from " + TABLE_PAGE +
                 " left join ( select " + COLUMN_PAGE + ", sum(UPDATESCOUNT) from ( select " + TABLE_NOVEL_DETAILS + "." + COLUMN_PAGE +
			     "            , case when " + TABLE_PAGE + "." + COLUMN_LAST_UPDATE + " != " + TABLE_NOVEL_CONTENT + "." + COLUMN_LAST_UPDATE +
			     "                   then 1 else 0 end as UPDATESCOUNT " +
			     "       from " + TABLE_NOVEL_DETAILS +
			     "       join " + TABLE_NOVEL_BOOK + " on " + TABLE_NOVEL_DETAILS + "." + COLUMN_PAGE + " = " + TABLE_NOVEL_BOOK + "." + COLUMN_PAGE +
			     "       join " + TABLE_PAGE       + " on " + TABLE_PAGE + "." + COLUMN_PARENT        + " = " + TABLE_NOVEL_DETAILS + "." + COLUMN_PAGE + " || '" + Constants.NOVEL_BOOK_DIVIDER + "' || " + TABLE_NOVEL_BOOK + "." + COLUMN_TITLE +
			     "       join " + TABLE_NOVEL_CONTENT + " on " + TABLE_NOVEL_CONTENT + "." + COLUMN_PAGE + " = " + TABLE_PAGE + "." + COLUMN_PAGE +
			     " ) group by " + COLUMN_PAGE + ") r on " + TABLE_PAGE + "." + COLUMN_PAGE + " = r." + COLUMN_PAGE +
                 " where " + COLUMN_PARENT + " = ? ";
		if(alphOrder) sql += " ORDER BY " + COLUMN_TITLE;
		else          sql += " ORDER BY " + COLUMN_IS_WATCHED + " DESC, " + COLUMN_TITLE;

		Cursor cursor = rawQuery(db, sql, new String[] {Constants.ROOT_NOVEL});
		try {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				PageModel page = cursorToPageModel(cursor);
				pages.add(page);
				cursor.moveToNext();
			}
		} finally{
			if(cursor != null) cursor.close();
		}
		return pages;
	}

	public ArrayList<PageModel> getAllTeaser(SQLiteDatabase db, boolean alphOrder) {
		ArrayList<PageModel> pages = new ArrayList<PageModel>();

		String sql = "select * from " + TABLE_PAGE +
                " left join ( select " + COLUMN_PAGE + ", sum(UPDATESCOUNT) from ( select " + TABLE_NOVEL_DETAILS + "." + COLUMN_PAGE +
			     "            , case when " + TABLE_PAGE + "." + COLUMN_LAST_UPDATE + " != " + TABLE_NOVEL_CONTENT + "." + COLUMN_LAST_UPDATE +
			     "                   then 1 else 0 end as UPDATESCOUNT " +
			     "       from " + TABLE_NOVEL_DETAILS +
			     "       join " + TABLE_NOVEL_BOOK + " on " + TABLE_NOVEL_DETAILS + "." + COLUMN_PAGE + " = " + TABLE_NOVEL_BOOK + "." + COLUMN_PAGE +
			     "       join " + TABLE_PAGE       + " on " + TABLE_PAGE + "." + COLUMN_PARENT        + " = " + TABLE_NOVEL_DETAILS + "." + COLUMN_PAGE + " || '" + Constants.NOVEL_BOOK_DIVIDER + "' || " + TABLE_NOVEL_BOOK + "." + COLUMN_TITLE +
			     "       join " + TABLE_NOVEL_CONTENT + " on " + TABLE_NOVEL_CONTENT + "." + COLUMN_PAGE + " = " + TABLE_PAGE + "." + COLUMN_PAGE +
			     " ) group by " + COLUMN_PAGE + ") r on " + TABLE_PAGE + "." + COLUMN_PAGE + " = r." + COLUMN_PAGE +
                " where " + COLUMN_PARENT + " = ? ";
		if(alphOrder) sql += " ORDER BY " + COLUMN_TITLE;
		else          sql += " ORDER BY " + COLUMN_IS_WATCHED + " DESC, " + COLUMN_TITLE;

		Cursor cursor = rawQuery(db, sql, new String[] {"Category:Teasers"});
		try {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				PageModel page = cursorToPageModel(cursor);
				pages.add(page);
				cursor.moveToNext();
			}
		} finally{
			if(cursor != null) cursor.close();
		}
		return pages;
	}

	public ArrayList<PageModel> getAllOriginal(SQLiteDatabase db, boolean alphOrder) {
		ArrayList<PageModel> pages = new ArrayList<PageModel>();

		String sql = "select * from " + TABLE_PAGE +
                " left join ( select " + COLUMN_PAGE + ", sum(UPDATESCOUNT) from ( select " + TABLE_NOVEL_DETAILS + "." + COLUMN_PAGE +
			     "            , case when " + TABLE_PAGE + "." + COLUMN_LAST_UPDATE + " != " + TABLE_NOVEL_CONTENT + "." + COLUMN_LAST_UPDATE +
			     "                   then 1 else 0 end as UPDATESCOUNT " +
			     "       from " + TABLE_NOVEL_DETAILS +
			     "       join " + TABLE_NOVEL_BOOK + " on " + TABLE_NOVEL_DETAILS + "." + COLUMN_PAGE + " = " + TABLE_NOVEL_BOOK + "." + COLUMN_PAGE +
			     "       join " + TABLE_PAGE       + " on " + TABLE_PAGE + "." + COLUMN_PARENT        + " = " + TABLE_NOVEL_DETAILS + "." + COLUMN_PAGE + " || '" + Constants.NOVEL_BOOK_DIVIDER + "' || " + TABLE_NOVEL_BOOK + "." + COLUMN_TITLE +
			     "       join " + TABLE_NOVEL_CONTENT + " on " + TABLE_NOVEL_CONTENT + "." + COLUMN_PAGE + " = " + TABLE_PAGE + "." + COLUMN_PAGE +
			     " ) group by " + COLUMN_PAGE + ") r on " + TABLE_PAGE + "." + COLUMN_PAGE + " = r." + COLUMN_PAGE +
                " where " + COLUMN_PARENT + " = ? ";
		if(alphOrder) sql += " ORDER BY " + COLUMN_TITLE;
		else          sql += " ORDER BY " + COLUMN_IS_WATCHED + " DESC, " + COLUMN_TITLE;

		Cursor cursor = rawQuery(db, sql, new String[] {"Category:Original"});
		try {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				PageModel page = cursorToPageModel(cursor);
				pages.add(page);
				cursor.moveToNext();
			}
		} finally{
			if(cursor != null) cursor.close();
		}
		return pages;
	}

	public ArrayList<PageModel> getAllAlternative(SQLiteDatabase db, boolean alphOrder, String language) {
		ArrayList<PageModel> pages = new ArrayList<PageModel>();

		String sql = "select * from " + TABLE_PAGE +
                " left join ( select " + COLUMN_PAGE + ", sum(UPDATESCOUNT) from ( select " + TABLE_NOVEL_DETAILS + "." + COLUMN_PAGE +
			     "            , case when " + TABLE_PAGE + "." + COLUMN_LAST_UPDATE + " != " + TABLE_NOVEL_CONTENT + "." + COLUMN_LAST_UPDATE +
			     "                   then 1 else 0 end as UPDATESCOUNT " +
			     "       from " + TABLE_NOVEL_DETAILS +
			     "       join " + TABLE_NOVEL_BOOK + " on " + TABLE_NOVEL_DETAILS + "." + COLUMN_PAGE + " = " + TABLE_NOVEL_BOOK + "." + COLUMN_PAGE +
			     "       join " + TABLE_PAGE       + " on " + TABLE_PAGE + "." + COLUMN_PARENT        + " = " + TABLE_NOVEL_DETAILS + "." + COLUMN_PAGE + " || '" + Constants.NOVEL_BOOK_DIVIDER + "' || " + TABLE_NOVEL_BOOK + "." + COLUMN_TITLE +
			     "       join " + TABLE_NOVEL_CONTENT + " on " + TABLE_NOVEL_CONTENT + "." + COLUMN_PAGE + " = " + TABLE_PAGE + "." + COLUMN_PAGE +
			     " ) group by " + COLUMN_PAGE + ") r on " + TABLE_PAGE + "." + COLUMN_PAGE + " = r." + COLUMN_PAGE +
                " where " + COLUMN_PARENT + " = ? ";
		if(alphOrder) sql += " ORDER BY " + COLUMN_TITLE;
		else          sql += " ORDER BY " + COLUMN_IS_WATCHED + " DESC, " + COLUMN_TITLE;
        Cursor cursor = null;
        if (language.equals(Constants.LANG_BAHASA_INDONESIA)) cursor = rawQuery(db, sql, new String[] {"Category:Indonesian"});
		try {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				PageModel page = cursorToPageModel(cursor);
				pages.add(page);
				cursor.moveToNext();
			}
		} finally{
			if(cursor != null) cursor.close();
		}
		return pages;
	}

	public ArrayList<PageModel> getAllWatchedNovel(SQLiteDatabase db, boolean alphOrder) {
		ArrayList<PageModel> pages = new ArrayList<PageModel>();

		String sql = "select * from " + TABLE_PAGE +
                " left join ( select " + COLUMN_PAGE + ", sum(UPDATESCOUNT) from ( select " + TABLE_NOVEL_DETAILS + "." + COLUMN_PAGE +
			     "            , case when " + TABLE_PAGE + "." + COLUMN_LAST_UPDATE + " != " + TABLE_NOVEL_CONTENT + "." + COLUMN_LAST_UPDATE +
			     "                   then 1 else 0 end as UPDATESCOUNT " +
			     "       from " + TABLE_NOVEL_DETAILS +
			     "       join " + TABLE_NOVEL_BOOK + " on " + TABLE_NOVEL_DETAILS + "." + COLUMN_PAGE + " = " + TABLE_NOVEL_BOOK + "." + COLUMN_PAGE +
			     "       join " + TABLE_PAGE       + " on " + TABLE_PAGE + "." + COLUMN_PARENT        + " = " + TABLE_NOVEL_DETAILS + "." + COLUMN_PAGE + " || '" + Constants.NOVEL_BOOK_DIVIDER + "' || " + TABLE_NOVEL_BOOK + "." + COLUMN_TITLE +
			     "       join " + TABLE_NOVEL_CONTENT + " on " + TABLE_NOVEL_CONTENT + "." + COLUMN_PAGE + " = " + TABLE_PAGE + "." + COLUMN_PAGE +
			     " ) group by " + COLUMN_PAGE + ") r on " + TABLE_PAGE + "." + COLUMN_PAGE + " = r." + COLUMN_PAGE +
                " where " + COLUMN_PARENT + " in ('" + Constants.ROOT_NOVEL + "', 'Category:Teasers', 'Category:Original', 'Category:Indonesian') and  " + TABLE_PAGE + "." + COLUMN_IS_WATCHED + " = ? ";
		if(alphOrder) sql += " ORDER BY " + COLUMN_TITLE;
		else          sql += " ORDER BY " + COLUMN_IS_WATCHED + " DESC, " + COLUMN_TITLE;

		Cursor cursor = rawQuery(db, sql, new String[] {"1"});
		try {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				PageModel page = cursorToPageModel(cursor);
				pages.add(page);
				cursor.moveToNext();
			}
		} finally{
			if(cursor != null) cursor.close();
		}
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
		try {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				PageModel page = cursorToPageModel(cursor);
				pages.add(page);
				cursor.moveToNext();
			}
		} finally{
			if(cursor != null) cursor.close();
		}
		return pages;
	}

	public PageModel selectFirstBy(SQLiteDatabase db, String column, String value){
		//Log.d(TAG, "Select First: Column = " + column + " Value = " + value);
		PageModel page = null;

		Cursor cursor = rawQuery(db, "select * from " + TABLE_PAGE + " where " + column + " = ? ", new String[] {value});
		try {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				page = cursorToPageModel(cursor);
				//Log.d(TAG, "Found: " + page.toString());
				break;
			}
		} finally{
			if(cursor != null) cursor.close();
		}
		return page;
	}

	public PageModel insertOrUpdatePageModel(SQLiteDatabase db, PageModel page, boolean updateStatus){
		//Log.d(TAG, page.toString());

		PageModel temp = selectFirstBy(db, COLUMN_PAGE, page.getPage());

		ContentValues cv = new ContentValues();
		cv.put(COLUMN_PAGE, page.getPage());
		cv.put(COLUMN_LANGUAGE, page.getLanguage());
		cv.put(COLUMN_TITLE, page.getTitle());
		cv.put(COLUMN_ORDER, page.getOrder());
		cv.put(COLUMN_PARENT, page.getParent());
		cv.put(COLUMN_TYPE, page.getType());
		cv.put(COLUMN_IS_WATCHED, page.isWatched());
		cv.put(COLUMN_IS_FINISHED_READ, page.isFinishedRead());
		cv.put(COLUMN_IS_DOWNLOADED, page.isDownloaded());
		if(updateStatus) cv.put(COLUMN_STATUS, page.getStatus());
		cv.put(COLUMN_IS_MISSING, page.isMissing());
		cv.put(COLUMN_IS_EXTERNAL, page.isExternal());

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

	private PageModel cursorToPageModel(Cursor cursor) {
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
		page.setMissing(cursor.getInt(12) == 1 ? true : false);
		page.setExternal(cursor.getInt(13) == 1 ? true : false);
		page.setLanguage(cursor.getString(14));

		if(cursor.getColumnCount() > 15) {
		  page.setUpdateCount(cursor.getInt(16));
		}
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
		cv.put(COLUMN_IMAGE_NAME, novelDetails.getCover());
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

		// insert book
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

		// insert chapter
		for(Iterator<BookModel> iBooks = novelDetails.getBookCollections().iterator(); iBooks.hasNext();){
			BookModel book = iBooks.next();
			for(Iterator<PageModel> iPage = book.getChapterCollection().iterator(); iPage.hasNext();) {
				PageModel page = iPage.next();
				ContentValues cv3 = new ContentValues();
				cv3.put(COLUMN_PAGE, page.getPage());
				cv3.put(COLUMN_LANGUAGE, page.getLanguage());
				cv3.put(COLUMN_TITLE, page.getTitle());
				cv3.put(COLUMN_TYPE, page.getType());
				cv3.put(COLUMN_PARENT, page.getParent());
				cv3.put(COLUMN_ORDER, page.getOrder());
				cv3.put(COLUMN_IS_EXTERNAL, page.isExternal());
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
		try {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				novelDetails = cursorToNovelCollection(cursor);
				//Log.d(TAG, "Found: " + novelDetails.toString());
				break;
			}
		} finally{
			if(cursor != null) cursor.close();
		}
	    return novelDetails;
	}

	public BookModel getBookModel(SQLiteDatabase db, int id) {
		BookModel book = null;
		Cursor cursor = rawQuery(db, "select * from " + TABLE_NOVEL_BOOK + " where " + COLUMN_ID + " = ? ", new String[] {"" + id});
		try {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				book = cursorToBookModel(cursor);
				//Log.d(TAG, "Found: " + book.getPage() + Constants.NOVEL_BOOK_DIVIDER + book.getTitle());
				break;
			}
		} finally{
			if(cursor != null) cursor.close();
		}
	    return book;
	}

	public BookModel getBookModel(SQLiteDatabase db, String page, String title) {
		BookModel book = null;
		Cursor cursor = rawQuery(db, "select * from " + TABLE_NOVEL_BOOK + " where " + COLUMN_PAGE + " = ? and " + COLUMN_TITLE + " = ?", new String[] {page, title});
		try {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				book = cursorToBookModel(cursor);
				//Log.d(TAG, "Found: " + book.getPage() + Constants.NOVEL_BOOK_DIVIDER + book.getTitle());
				break;
			}
		} finally{
			if(cursor != null) cursor.close();
		}
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
		try {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				BookModel book = cursorToBookModel(cursor);
				book.setParent(novelDetails);
				bookCollection.add(book);
				//Log.d(TAG, "Found: " + book.toString());
				cursor.moveToNext();
			}
		} finally{
			if(cursor != null) cursor.close();
		}
		return bookCollection;
	}

	public ArrayList<PageModel> getChapterCollection(SQLiteDatabase db, String parent, BookModel book) {
		ArrayList<PageModel> chapters = new ArrayList<PageModel>();
		Cursor cursor = rawQuery(db, "select * from " + TABLE_PAGE + " where " + COLUMN_PARENT + " = ? order by " + COLUMN_ORDER, new String[] {parent});
		try {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				PageModel chapter = cursorToPageModel(cursor);
				chapter.setBook(book);
				chapters.add(chapter);
				//Log.d(TAG, "Found: " + chapter.toString());
				cursor.moveToNext();
			}
		} finally{
			if(cursor != null) cursor.close();
		}
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
		cv.put(COLUMN_IMAGE_NAME, image.getName());
		cv.put(COLUMN_FILEPATH, image.getPath());
		cv.put(COLUMN_URL, image.getUrl().toString());
		cv.put(COLUMN_REFERER, image.getReferer());
		cv.put(COLUMN_IS_BIG_IMAGE, image.isBigImage());
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

	public ImageModel getImageByReferer(SQLiteDatabase db, ImageModel image) {
		return getImageByReferer(db, image.getReferer());
	}

	public ImageModel getImageByReferer(SQLiteDatabase db, String url) {
		//Log.d(TAG, "Selecting Image by Referer: " + url);
		ImageModel image = null;

		Cursor cursor = rawQuery(db, "select * from " + TABLE_IMAGE + " where " + COLUMN_REFERER + " = ? ", new String[] {url});
		try {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				image = cursorToImage(cursor);
				Log.d(TAG, "Found by Ref: " + image.getReferer() + " name: " + image.getName() + " id: " + image.getId());
				break;
			}
		} finally{
			if(cursor != null) cursor.close();
		}

		if(image == null) {
			Log.w(TAG, "Not Found Image by Referer: " + url);
		}
		return image;
	}


	public ImageModel getImage(SQLiteDatabase db, ImageModel image) {
		return getImage(db, image.getName());
	}

	public ImageModel getImage(SQLiteDatabase db, String name) {
		//Log.d(TAG, "Selecting Image: " + name);
		ImageModel image = null;

		Cursor cursor = rawQuery(db, "select * from " + TABLE_IMAGE + " where " + COLUMN_IMAGE_NAME + " = ? ", new String[] {name});
		try {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				image = cursorToImage(cursor);
				//Log.d(TAG, "Found: " + image.getName() + " id: " + image.getId());
				break;
			}
		} finally{
			if(cursor != null) cursor.close();
		}

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

		image.setBigImage(cursor.getInt(7) == 1 ? true : false);
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
		cv.put(COLUMN_ZOOM, "" + content.getLastZoom());
		cv.put(COLUMN_LAST_CHECK, "" + (int) (new Date().getTime() / 1000));

		NovelContentModel temp = getNovelContent(db, content.getPage());
		if(temp == null){
			cv.put(COLUMN_LAST_X, "" + content.getLastXScroll());
			cv.put(COLUMN_LAST_Y, "" + content.getLastYScroll());

			//Log.d(TAG, "Inserting Novel Content: " + content.getPage());
			if(content.getLastUpdate() == null)
				cv.put(COLUMN_LAST_UPDATE, 0);
			else
				cv.put(COLUMN_LAST_UPDATE, "" + (int) (content.getLastUpdate().getTime() / 1000));
			long id = insertOrThrow(db, TABLE_NOVEL_CONTENT, null, cv);
			Log.i(TAG, "Novel Content Inserted, New id: "  + id);
		}
		else {
			if(content.isUpdatingFromInternet()) {
				cv.put(COLUMN_LAST_X, "" + temp.getLastXScroll());
				cv.put(COLUMN_LAST_Y, "" + temp.getLastYScroll());
			}
			else {
				cv.put(COLUMN_LAST_X, "" + content.getLastXScroll());
				cv.put(COLUMN_LAST_Y, "" + content.getLastYScroll());
			}

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
			pageModel = insertOrUpdatePageModel(db, pageModel, false);
		}

		content = getNovelContent(db, content.getPage());
		return content;
	}

	public NovelContentModel getNovelContent(SQLiteDatabase db, String page) {
		//Log.d(TAG, "Selecting Novel Content: " + page);
		NovelContentModel content = null;

		Cursor cursor = rawQuery(db, "select * from " + TABLE_NOVEL_CONTENT + " where " + COLUMN_PAGE + " = ? ", new String[] {page});
		try {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				content = cursorToNovelContent(cursor);
				//Log.d(TAG, "Found: " + content.getPage() + " id: " + content.getId());
				break;
			}
		} finally{
			if(cursor != null) cursor.close();
		}
		if(content == null) {
			Log.w(TAG, "Not Found Novel Content: " + page);
		}
		return content;
	}

	public ArrayList<BookmarkModel> getAllBookmarks(SQLiteDatabase db) {
		ArrayList<BookmarkModel> bookmarks = new ArrayList<BookmarkModel>();

		Cursor cursor = rawQuery(db, "select * from " + TABLE_NOVEL_BOOKMARK
				                   + " order by " + COLUMN_PAGE
				                   + ", " + COLUMN_PARAGRAPH_INDEX, null);
		try {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				BookmarkModel bookmark = cursorToNovelBookmark(cursor);
				bookmarks.add(bookmark);
				cursor.moveToNext();
			}
		} finally{
			if(cursor != null) cursor.close();
		}

		return bookmarks;
	}

	public ArrayList<BookmarkModel> getBookmarks(SQLiteDatabase db, PageModel page) {
		ArrayList<BookmarkModel> bookmarks = new ArrayList<BookmarkModel>();

		Cursor cursor = rawQuery(db, "select * from " + TABLE_NOVEL_BOOKMARK + " where " + COLUMN_PAGE + " = ? ", new String[] {page.getPage()});
		try {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				BookmarkModel bookmark = cursorToNovelBookmark(cursor);
				bookmarks.add(bookmark);
				cursor.moveToNext();
			}
		} finally{
			if(cursor != null) cursor.close();
		}

		return bookmarks;
	}

	private BookmarkModel getBookmark(SQLiteDatabase db, String page, int pIndex) {
		BookmarkModel bookmark = null;

		Cursor cursor = rawQuery(db, "select * from " + TABLE_NOVEL_BOOKMARK + " where " + COLUMN_PAGE + " = ? and " + COLUMN_PARAGRAPH_INDEX + " = ? "
				                   , new String[] {page, "" + pIndex});
		try {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				bookmark = cursorToNovelBookmark(cursor);
				break;
			}
		} finally{
			if(cursor != null) cursor.close();
		}

		return bookmark;
	}

	public int insertBookmark(SQLiteDatabase db, BookmarkModel bookmark) {
		BookmarkModel tempBookmark = getBookmark(db, bookmark.getPage(), bookmark.getpIndex());

		if(tempBookmark == null) {
			ContentValues cv = new ContentValues();
			cv.put(COLUMN_PAGE, bookmark.getPage());
			cv.put(COLUMN_PARAGRAPH_INDEX, bookmark.getpIndex());
			String excerpt = bookmark.getExcerpt();
			if(excerpt.length() > 200) {
				excerpt = excerpt.substring(0, 197) + "...";
			}
			cv.put(COLUMN_EXCERPT, excerpt);
			cv.put(COLUMN_CREATE_DATE, (int) (new Date().getTime() / 1000));
			return (int) insertOrThrow(db, TABLE_NOVEL_BOOKMARK, null, cv);
		}

		return 0;
	}

	public int deleteBookmark(SQLiteDatabase db,BookmarkModel bookmark) {
		return delete(db, TABLE_NOVEL_BOOKMARK, COLUMN_PAGE + " = ? and " + COLUMN_PARAGRAPH_INDEX + " = ? "
				 , new String[] {bookmark.getPage(), "" + bookmark.getpIndex()});
	}

	private BookmarkModel cursorToNovelBookmark(Cursor cursor) {
		BookmarkModel bookmark = new BookmarkModel();
		bookmark.setId(cursor.getInt(0));
		bookmark.setPage(cursor.getString(1));
		bookmark.setpIndex(cursor.getInt(2));
		bookmark.setExcerpt(cursor.getString(3));
		bookmark.setCreationDate(new Date(cursor.getLong(4)*1000));
		return bookmark;
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

	public UpdateInfoModel getUpdateHistory(SQLiteDatabase db, UpdateInfoModel update) {
		UpdateInfoModel result = null;

		Cursor cursor = rawQuery(db, "select * from " + TABLE_UPDATE_HISTORY + " where " + COLUMN_PAGE + " = ? "
				                   , new String[] {update.getUpdatePage()});
		try {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				result = cursorToUpdateInfoModel(cursor);
				break;
			}
		} finally{
			if(cursor != null) cursor.close();
		}

		return result;
	}

	public int insertUpdateHistory(SQLiteDatabase db, UpdateInfoModel update) {
		UpdateInfoModel tempUpdate = getUpdateHistory(db, update);

		ContentValues cv = new ContentValues();
		cv.put(COLUMN_PAGE, update.getUpdatePage());
		cv.put(COLUMN_UPDATE_TITLE, update.getUpdateTitle());
		cv.put(COLUMN_UPDATE_TYPE, update.getUpdateType().ordinal());
		cv.put(COLUMN_LAST_UPDATE, (int) (update.getUpdateDate().getTime() / 1000));

		if(tempUpdate == null) {
			return (int) insertOrThrow(db, TABLE_UPDATE_HISTORY, null, cv);
		}
		else {
			return update(db, TABLE_UPDATE_HISTORY, cv, COLUMN_ID + " = ? ", new String[] {"" + tempUpdate.getId()});
		}
	}

	public ArrayList<UpdateInfoModel> getAllUpdateHistory(SQLiteDatabase db) {
		ArrayList<UpdateInfoModel> updates = new ArrayList<UpdateInfoModel>();

		Cursor cursor = rawQuery(db, "select * from " + TABLE_UPDATE_HISTORY
				                   + " order by case "
				                   + "   when " + COLUMN_UPDATE_TYPE + " = " + UpdateType.UpdateTos.ordinal() + " then 0 "
				                   + "   when " + COLUMN_UPDATE_TYPE + " = " + UpdateType.NewNovel.ordinal() + " then 1 "
				                   + "   when " + COLUMN_UPDATE_TYPE + " = " + UpdateType.New.ordinal() + " then 2 "
				                   + "   when " + COLUMN_UPDATE_TYPE + " = " + UpdateType.Updated.ordinal() + " then 3 "
				                   + "   else 4 end "
				                   + ", " + COLUMN_LAST_UPDATE + " desc "
				                   + ", " + COLUMN_PAGE, null);
		try {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				UpdateInfoModel update = cursorToUpdateInfoModel(cursor);
				updates.add(update);
				cursor.moveToNext();
			}
		} finally{
			if(cursor != null) cursor.close();
		}

		return updates;
	}

	public void deleteAllUpdateHistory(SQLiteDatabase db) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_UPDATE_HISTORY);
		db.execSQL(DATABASE_CREATE_UPDATE_HISTORY);
		Log.d(TAG, "Recreate " + TABLE_UPDATE_HISTORY);
	}

	public int deleteUpdateHistory(SQLiteDatabase db, UpdateInfoModel updateInfo) {
		Log.d(TAG, "Deleting UpdateInfoModel id: " + updateInfo.getId());
		return delete(db, TABLE_UPDATE_HISTORY, COLUMN_ID + " = ? "
				 , new String[] {updateInfo.getId() + ""});
	}

	private UpdateInfoModel cursorToUpdateInfoModel(Cursor cursor) {
		UpdateInfoModel update = new UpdateInfoModel();
		update.setId(cursor.getInt(0));
		update.setUpdatePage(cursor.getString(1));
		update.setUpdateTitle(cursor.getString(2));
		int type = cursor.getInt(3);
		update.setUpdateType(UpdateType.values()[type]);
		update.setUpdateDate(new Date(cursor.getLong(4)*1000));
		return update;
	}

	/*
	 * To avoid android.database.sqlite.SQLiteException: unable to close due to unfinalised statements
	 */
	private Cursor rawQuery(SQLiteDatabase db, String sql, String[] values){
		Object lock = new Object();
		synchronized (lock) {
			if(!db.isOpen())
				db = getReadableDatabase();
			return db.rawQuery(sql, values);
		}
	}

	private int update(SQLiteDatabase db, String table, ContentValues cv, String whereClause, String[] whereParams){
		Object lock = new Object();
		synchronized (lock) {
			if (!db.isOpen())
				db = getWritableDatabase();
			return db.update(table, cv, whereClause, whereParams);
		}
	}

	private long insertOrThrow(SQLiteDatabase db, String table, String nullColumnHack, ContentValues cv){
		Object lock = new Object();
		synchronized (lock) {
			if (!db.isOpen())
				db = getWritableDatabase();
			return db.insertOrThrow(table, nullColumnHack, cv);
		}
	}

	private int delete(SQLiteDatabase db, String table, String whereClause, String[] whereParams) {
		Object lock = new Object();
		synchronized (lock) {
			if (!db.isOpen())
				db = getWritableDatabase();
			return db.delete(table, whereClause, whereParams);
		}
	}
}
