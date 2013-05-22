//package com.nandaka.bakareaderclone.helper;
package com.erakk.lnreader.helper;

import java.io.File;
import java.io.IOException;
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
import com.erakk.lnreader.helper.db.BookmarkModelHelper;
import com.erakk.lnreader.helper.db.ImageModelHelper;
import com.erakk.lnreader.helper.db.NovelContentModelHelper;
import com.erakk.lnreader.helper.db.PageModelHelper;
import com.erakk.lnreader.helper.db.UpdateInfoModelHelper;
import com.erakk.lnreader.model.BookModel;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.PageModel;

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
		 db.execSQL(PageModelHelper.DATABASE_CREATE_PAGES);
		 db.execSQL(ImageModelHelper.DATABASE_CREATE_IMAGES);
		 db.execSQL(DATABASE_CREATE_NOVEL_DETAILS);
		 db.execSQL(DATABASE_CREATE_NOVEL_BOOKS);
		 db.execSQL(NovelContentModelHelper.DATABASE_CREATE_NOVEL_CONTENT);
		 db.execSQL(BookmarkModelHelper.DATABASE_CREATE_NOVEL_BOOKMARK);
		 db.execSQL(UpdateInfoModelHelper.DATABASE_CREATE_UPDATE_HISTORY);
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
			db.execSQL(BookmarkModelHelper.DATABASE_CREATE_NOVEL_BOOKMARK);
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
			db.execSQL(UpdateInfoModelHelper.DATABASE_CREATE_UPDATE_HISTORY);
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

	/*
	 * Cross Table Operation
	 */

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
				PageModel page = PageModelHelper.cursorToPageModel(cursor);
				result.add(page);
				cursor.moveToNext();
			}
		} finally{
			if(cursor != null) cursor.close();
		}
		return result;
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
			PageModelHelper.deletePageModel(db, novel);
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
				PageModel page = PageModelHelper.cursorToPageModel(cursor);
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
				PageModel page = PageModelHelper.cursorToPageModel(cursor);
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
				PageModel page = PageModelHelper.cursorToPageModel(cursor);
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
				PageModel page = PageModelHelper.cursorToPageModel(cursor);
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
				PageModel page = PageModelHelper.cursorToPageModel(cursor);
				pages.add(page);
				cursor.moveToNext();
			}
		} finally{
			if(cursor != null) cursor.close();
		}
		return pages;
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

				PageModel tempPage = PageModelHelper.getPageModel(db, page.getPage());
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
				PageModel chapter = PageModelHelper.cursorToPageModel(cursor);
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
	    	novelDetails.setPageModel(PageModelHelper.getPageModel(db, page));

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
	 * To avoid android.database.sqlite.SQLiteException: unable to close due to unfinalised statements
	 */
	public Cursor rawQuery(SQLiteDatabase db, String sql, String[] values){
		Object lock = new Object();
		synchronized (lock) {
			if(!db.isOpen())
				db = getReadableDatabase();
			return db.rawQuery(sql, values);
		}
	}

	public int update(SQLiteDatabase db, String table, ContentValues cv, String whereClause, String[] whereParams){
		Object lock = new Object();
		synchronized (lock) {
			if (!db.isOpen())
				db = getWritableDatabase();
			return db.update(table, cv, whereClause, whereParams);
		}
	}

	public long insertOrThrow(SQLiteDatabase db, String table, String nullColumnHack, ContentValues cv){
		Object lock = new Object();
		synchronized (lock) {
			if (!db.isOpen())
				db = getWritableDatabase();
			return db.insertOrThrow(table, nullColumnHack, cv);
		}
	}

	public int delete(SQLiteDatabase db, String table, String whereClause, String[] whereParams) {
		Object lock = new Object();
		synchronized (lock) {
			if (!db.isOpen())
				db = getWritableDatabase();
			return db.delete(table, whereClause, whereParams);
		}
	}
}
