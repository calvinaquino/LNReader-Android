//package com.nandaka.bakareaderclone.helper;
package com.erakk.lnreader.helper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.erakk.lnreader.AlternativeLanguageInfo;
import com.erakk.lnreader.Constants;
import com.erakk.lnreader.helper.db.BookModelHelper;
import com.erakk.lnreader.helper.db.BookmarkModelHelper;
import com.erakk.lnreader.helper.db.ImageModelHelper;
import com.erakk.lnreader.helper.db.NovelCollectionModelHelper;
import com.erakk.lnreader.helper.db.NovelContentModelHelper;
import com.erakk.lnreader.helper.db.PageModelHelper;
import com.erakk.lnreader.helper.db.UpdateInfoModelHelper;
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

	// Use /files/database to standarize with newer android.
	public static final String DB_ROOT_SD = Environment.getExternalStorageDirectory().getAbsolutePath().toString() + "/Android/data/" + Constants.class.getPackage().getName() + "/files/databases";

	public DBHelper(Context context) {
		super(context, getDbPath(context), null, DATABASE_VERSION);
	}

	public static String getDbPath(Context context) {
		String dbPath = null;
		File path = context.getExternalFilesDir(null);
		if (path != null)
			dbPath = path.getAbsolutePath() + "/databases/" + DATABASE_NAME;
		else {
			path = new File(DB_ROOT_SD);
			if (!(path.mkdirs() || path.isDirectory()))
				Log.e(TAG, "DB Path doesn't exists/failed to create.");
			Toast.makeText(context, String.format("Failed to create/open db directory %s", DB_ROOT_SD), Toast.LENGTH_LONG).show();
			dbPath = DB_ROOT_SD + "/" + DATABASE_NAME;
		}
		Log.d(TAG, "DB Path : " + dbPath);
		return dbPath;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(PageModelHelper.DATABASE_CREATE_PAGES);
		db.execSQL(ImageModelHelper.DATABASE_CREATE_IMAGES);
		db.execSQL(NovelCollectionModelHelper.DATABASE_CREATE_NOVEL_DETAILS);
		db.execSQL(BookModelHelper.DATABASE_CREATE_NOVEL_BOOKS);
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
		if (oldVersion < 18) {
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
		if (oldVersion == 18) {
			db.execSQL("ALTER TABLE " + TABLE_PAGE + " ADD COLUMN " + COLUMN_STATUS + " text");
			oldVersion = 19;
		}
		if (oldVersion == 19) {
			db.execSQL(BookmarkModelHelper.DATABASE_CREATE_NOVEL_BOOKMARK);
			oldVersion = 21; // skip the alter table
		}
		if (oldVersion == 20) {
			db.execSQL("ALTER TABLE " + TABLE_NOVEL_BOOKMARK + " ADD COLUMN " + COLUMN_EXCERPT + " text");
			db.execSQL("ALTER TABLE " + TABLE_NOVEL_BOOKMARK + " ADD COLUMN " + COLUMN_CREATE_DATE + " integer");
			oldVersion = 21;
		}
		if (oldVersion == 21) {
			db.execSQL("ALTER TABLE " + TABLE_PAGE + " ADD COLUMN " + COLUMN_IS_MISSING + " boolean");
			oldVersion = 22;
		}
		if (oldVersion == 22) {
			db.execSQL("ALTER TABLE " + TABLE_PAGE + " ADD COLUMN " + COLUMN_IS_EXTERNAL + " boolean");
			oldVersion = 23;
		}
		if (oldVersion == 23) {
			db.execSQL(UpdateInfoModelHelper.DATABASE_CREATE_UPDATE_HISTORY);
			oldVersion = 24;
		}
		if (oldVersion == 24) {
			db.execSQL("ALTER TABLE " + TABLE_IMAGE + " ADD COLUMN " + COLUMN_IS_BIG_IMAGE + " boolean");
			oldVersion = 25;
		}
		if (oldVersion == 25) {
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
		Log.w(TAG, "Database Deleted.");
	}

	public String copyDB(SQLiteDatabase db, Context context, boolean makeBackup) throws IOException {
		Log.d("DatabaseManager", "creating database backup");
		File srcPath;
		File dstPath;
		if (makeBackup) {
			srcPath = new File(getDbPath(context));
			dstPath = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Backup_pages.db");
		} else {
			dstPath = new File(getDbPath(context));
			srcPath = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Backup_pages.db");
		}
		Log.d("DatabaseManager", "source file: " + srcPath.getAbsolutePath());
		Log.d("DatabaseManager", "destination file: " + dstPath.getAbsolutePath());
		if (srcPath.exists()) {
			Util.copyFile(srcPath, dstPath);
			Log.d("DatabaseManager", "copy success");
			return dstPath.getPath();
		} else {
			Log.d("DatabaseManager", "source file does not exist");
			return "null";
		}
	}

	/*
	 * Cross Table Operation
	 */

	public boolean isContentUpdated(SQLiteDatabase db, PageModel page) {
		// Log.d(TAG, "isContentUpdated is called by: " + page.getPage());
		String sql = "select case when " + TABLE_PAGE + "." + COLUMN_LAST_UPDATE + " != " + TABLE_NOVEL_CONTENT + "." + COLUMN_LAST_UPDATE +
				     "       then 1 else 0 end " + " from " + TABLE_PAGE +
				     " join " + TABLE_NOVEL_CONTENT + " using (" + COLUMN_PAGE + ") " +
				     " where " + COLUMN_PAGE + " = ? "+
			         "   and " + TABLE_PAGE + "." + COLUMN_IS_MISSING + " != 1 ";
		Cursor cursor = rawQuery(db, sql, new String[] { page.getPage() });
		try {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				return cursor.getInt(0) == 1 ? true : false;
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}

		return false;
	}

	public int isNovelUpdated(SQLiteDatabase db, PageModel novelPage) {
		String sql = "select r.page, sum(r.hasUpdates) " +
	                 "from ( select " + TABLE_NOVEL_DETAILS + "." + COLUMN_PAGE +
	                 "                , case when " + TABLE_PAGE + "." + COLUMN_LAST_UPDATE + " != " + TABLE_NOVEL_CONTENT + "." + COLUMN_LAST_UPDATE +
	                 "                  then 1 else 0 end as hasUpdates " +
	                 "       from " + TABLE_NOVEL_DETAILS +
	                 "       join " + TABLE_NOVEL_BOOK + " on " + TABLE_NOVEL_DETAILS + "." + COLUMN_PAGE + " = " + TABLE_NOVEL_BOOK + "." + COLUMN_PAGE +
	                 "       join " + TABLE_PAGE + " on " + TABLE_PAGE + "." + COLUMN_PARENT + " = " + TABLE_NOVEL_DETAILS + "." + COLUMN_PAGE + " || '" + Constants.NOVEL_BOOK_DIVIDER + "' || " + TABLE_NOVEL_BOOK + "." + COLUMN_TITLE +
	                 "       join " + TABLE_NOVEL_CONTENT + " on " + TABLE_NOVEL_CONTENT + "." + COLUMN_PAGE + " = " + TABLE_PAGE + "." + COLUMN_PAGE +
	                 "       where " + TABLE_NOVEL_DETAILS + "." + COLUMN_PAGE + " = ? " +
	                 "         and " + TABLE_PAGE + "." + COLUMN_IS_MISSING + " != 1 " +
	                 ") r group by r.page ";
		Cursor cursor = rawQuery(db, sql, new String[] { novelPage.getPage() });
		try {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				return cursor.getInt(1);
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}

		return 0;
	}

	public ArrayList<PageModel> doSearch(SQLiteDatabase db, String searchStr, boolean isNovelOnly, ArrayList<String> languageList) {
		ArrayList<PageModel> result = new ArrayList<PageModel>();

		String sql = null;
		String sqlLang = "'" + Util.join(languageList, "' ,'") + "'";

		if (isNovelOnly) {
			sql = "select * from " + TABLE_PAGE + " WHERE " + COLUMN_TYPE + " = '" + PageModel.TYPE_NOVEL + "' AND " + COLUMN_LANGUAGE + " IN (" + sqlLang + ") AND (" + COLUMN_PAGE + " LIKE ? OR " + COLUMN_TITLE + " LIKE ? )" + " ORDER BY " + COLUMN_PARENT + ", " + COLUMN_ORDER + ", " + COLUMN_TITLE + " LIMIT 100 ";
			Log.d(TAG, "Novel Only");
		} else {
			sql = "select * from " + TABLE_PAGE + " WHERE " + COLUMN_LANGUAGE + " IN (" + sqlLang + ") AND (" + COLUMN_PAGE + " LIKE ? OR " + COLUMN_TITLE + " LIKE ? )" + " ORDER BY CASE " + COLUMN_TYPE + "   WHEN '" + PageModel.TYPE_NOVEL + "' THEN 1 " + "   WHEN '" + PageModel.TYPE_CONTENT + "' THEN 2 " + "   ELSE 3 END, " + COLUMN_PARENT + ", " + COLUMN_ORDER + ", " + COLUMN_TITLE + " LIMIT 100 ";
			Log.d(TAG, "All Items");
		}
		Cursor cursor = rawQuery(db, sql, new String[] { "%" + searchStr + "%", "%" + searchStr + "%" });
		try {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				PageModel page = PageModelHelper.cursorToPageModel(cursor);
				result.add(page);
				cursor.moveToNext();
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return result;
	}

	public ArrayList<PageModel> getAllNovels(SQLiteDatabase db, boolean alphOrder) {
		ArrayList<PageModel> pages = new ArrayList<PageModel>();
		Cursor cursor = rawQuery(db, getNovelListQuery(alphOrder)
				                   , new String[] { Constants.ROOT_NOVEL });
		try {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				PageModel page = PageModelHelper.cursorToPageModel(cursor);
				pages.add(page);
				cursor.moveToNext();
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return pages;
	}

	public ArrayList<PageModel> getAllTeaser(SQLiteDatabase db, boolean alphOrder) {
		ArrayList<PageModel> pages = new ArrayList<PageModel>();
		Cursor cursor = rawQuery(db, getNovelListQuery(alphOrder)
				                   , new String[] { "Category:Teasers" });
		try {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				PageModel page = PageModelHelper.cursorToPageModel(cursor);
				pages.add(page);
				cursor.moveToNext();
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return pages;
	}

	public ArrayList<PageModel> getAllOriginal(SQLiteDatabase db, boolean alphOrder) {
		ArrayList<PageModel> pages = new ArrayList<PageModel>();
		Cursor cursor = rawQuery(db, getNovelListQuery(alphOrder)
				                   , new String[] { "Category:Original" });
		try {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				PageModel page = PageModelHelper.cursorToPageModel(cursor);
				pages.add(page);
				cursor.moveToNext();
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return pages;
	}

	public ArrayList<PageModel> getAllAlternative(SQLiteDatabase db, boolean alphOrder, String language) {
		ArrayList<PageModel> pages = new ArrayList<PageModel>();
		if (language != null) {
			Cursor cursor = rawQuery(db, getNovelListQuery(alphOrder)
					                   , new String[] { AlternativeLanguageInfo.getAlternativeLanguageInfo().get(language).getCategoryInfo() });

			try {
				cursor.moveToFirst();
				while (!cursor.isAfterLast()) {
					PageModel page = PageModelHelper.cursorToPageModel(cursor);
					pages.add(page);
					cursor.moveToNext();
				}
			} finally {
				if (cursor != null)
					cursor.close();
			}
		}
		return pages;
	}

	private String getNovelListQuery(boolean alphOrder) {
		String sql = "select * from " + TABLE_PAGE +
				     " left join ( select " + COLUMN_PAGE + ", sum(UPDATESCOUNT) " +
				     "             from ( select " + TABLE_NOVEL_DETAILS + "." + COLUMN_PAGE +
				     "                         , case when " + TABLE_PAGE + "." + COLUMN_LAST_UPDATE + " != " + TABLE_NOVEL_CONTENT + "." + COLUMN_LAST_UPDATE +
				     "                           then 1 else 0 end as UPDATESCOUNT " +
				     "                    from " + TABLE_NOVEL_DETAILS +
				     "                    join " + TABLE_NOVEL_BOOK + " on " + TABLE_NOVEL_DETAILS + "." + COLUMN_PAGE + " = " + TABLE_NOVEL_BOOK + "." + COLUMN_PAGE +
				     "                    join " + TABLE_PAGE + " on " + TABLE_PAGE + "." + COLUMN_PARENT + " = " + TABLE_NOVEL_DETAILS + "." + COLUMN_PAGE + " || '" + Constants.NOVEL_BOOK_DIVIDER + "' || " + TABLE_NOVEL_BOOK + "." + COLUMN_TITLE +
				     "                    join " + TABLE_NOVEL_CONTENT + " on " + TABLE_NOVEL_CONTENT + "." + COLUMN_PAGE + " = " + TABLE_PAGE + "." + COLUMN_PAGE + " " +
				     "             ) group by " + COLUMN_PAGE +
				     " ) r on " + TABLE_PAGE + "." + COLUMN_PAGE + " = r." + COLUMN_PAGE +
				     " where " + COLUMN_PARENT + " = ? ";
		if (alphOrder)
			sql += " ORDER BY " + COLUMN_TITLE;
		else
			sql += " ORDER BY " + COLUMN_IS_WATCHED + " DESC, " + COLUMN_TITLE;

		return sql;
	}

	public ArrayList<PageModel> getAllWatchedNovel(SQLiteDatabase db, boolean alphOrder) {
		ArrayList<PageModel> pages = new ArrayList<PageModel>();

		ArrayList<String> parents = new ArrayList<String>();
		parents.add("'" + Constants.ROOT_NOVEL + "'");
		parents.add("'Category:Teasers'");
		parents.add("'Category:Original'");
		for(AlternativeLanguageInfo info : AlternativeLanguageInfo.getAlternativeLanguageInfo().values()) {
			parents.add("'" + info.getCategoryInfo() + "'");
		}

		String sql = "select * " +
				     " from " + TABLE_PAGE +
				     " left join ( select " + COLUMN_PAGE +
				     "                  , sum(UPDATESCOUNT) " +
				     "             from ( select " + TABLE_NOVEL_DETAILS + "." + COLUMN_PAGE +
				     "                         , case when " + TABLE_PAGE + "." + COLUMN_LAST_UPDATE + " != " + TABLE_NOVEL_CONTENT + "." + COLUMN_LAST_UPDATE +
				     "                           then 1 else 0 end as UPDATESCOUNT " +
				     "                    from " + TABLE_NOVEL_DETAILS +
				     "                    join " + TABLE_NOVEL_BOOK + " on " + TABLE_NOVEL_DETAILS + "." + COLUMN_PAGE + " = " + TABLE_NOVEL_BOOK + "." + COLUMN_PAGE +
				     "                    join " + TABLE_PAGE + " on " + TABLE_PAGE + "." + COLUMN_PARENT + " = " + TABLE_NOVEL_DETAILS + "." + COLUMN_PAGE + " || '" + Constants.NOVEL_BOOK_DIVIDER + "' || " + TABLE_NOVEL_BOOK + "." + COLUMN_TITLE +
  				     "                                           and " + TABLE_PAGE + "." + COLUMN_IS_MISSING + " != 1 " +
				     "                    join " + TABLE_NOVEL_CONTENT + " on " + TABLE_NOVEL_CONTENT + "." + COLUMN_PAGE + " = " + TABLE_PAGE + "." + COLUMN_PAGE + " " +
				     "             ) group by " + COLUMN_PAGE +
				     " ) r on " + TABLE_PAGE + "." + COLUMN_PAGE + " = r." + COLUMN_PAGE +
				     " where " + COLUMN_PARENT + " in (" + Util.join(parents, ", ") + ") " +
				     "   and  " + TABLE_PAGE + "." + COLUMN_IS_WATCHED + " = ? ";
		if (alphOrder)
			sql += " ORDER BY " + COLUMN_TITLE;
		else
			sql += " ORDER BY " + COLUMN_IS_WATCHED + " DESC, " + COLUMN_TITLE;

		Cursor cursor = rawQuery(db, sql, new String[] { "1" });
		try {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				PageModel page = PageModelHelper.cursorToPageModel(cursor);
				pages.add(page);
				cursor.moveToNext();
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return pages;
	}

	/*
	 * To avoid android.database.sqlite.SQLiteException: unable to close due to unfinalized statements
	 */
	public Cursor rawQuery(SQLiteDatabase db, String sql, String[] values) {
		Object lock = new Object();
		synchronized (lock) {
			if (!db.isOpen())
				db = getReadableDatabase();
			return db.rawQuery(sql, values);
		}
	}

	public int update(SQLiteDatabase db, String table, ContentValues cv, String whereClause, String[] whereParams) {
		Object lock = new Object();
		synchronized (lock) {
			if (!db.isOpen())
				db = getWritableDatabase();
			return db.update(table, cv, whereClause, whereParams);
		}
	}

	public long insertOrThrow(SQLiteDatabase db, String table, String nullColumnHack, ContentValues cv) {
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
