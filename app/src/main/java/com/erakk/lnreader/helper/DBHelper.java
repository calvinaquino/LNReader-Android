//package com.nandaka.bakareaderclone.helper;
package com.erakk.lnreader.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.erakk.lnreader.AlternativeLanguageInfo;
import com.erakk.lnreader.Constants;
import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.helper.db.BookModelHelper;
import com.erakk.lnreader.helper.db.BookmarkModelHelper;
import com.erakk.lnreader.helper.db.ImageModelHelper;
import com.erakk.lnreader.helper.db.NovelCollectionModelHelper;
import com.erakk.lnreader.helper.db.NovelContentModelHelper;
import com.erakk.lnreader.helper.db.NovelContentUserHelperModel;
import com.erakk.lnreader.helper.db.PageCategoriesHelper;
import com.erakk.lnreader.helper.db.PageModelHelper;
import com.erakk.lnreader.helper.db.UpdateInfoModelHelper;
import com.erakk.lnreader.model.PageModel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

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
    public static final String COLUMN_IS_COMPLETED = "is_completed";

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
	public static final String TABLE_NOVEL_CONTENT_USER = "novel_books_content_user";

	public static final String TABLE_NOVEL_BOOKMARK = "novel_bookmark";
	public static final String COLUMN_PARAGRAPH_INDEX = "p_index";
	public static final String COLUMN_EXCERPT = "excerpt";
	public static final String COLUMN_CREATE_DATE = "create_date";

	public static final String TABLE_UPDATE_HISTORY = "update_history";
	public static final String COLUMN_UPDATE_TITLE = "update_title";
	public static final String COLUMN_UPDATE_TYPE = "update_type";

    public static final String TABLE_PAGE_CATEGORIES = "page_categories";
    public static final String COLUMN_CATEGORY = "category";

	public static final String DATABASE_NAME = "pages.db";
    public static final int DATABASE_VERSION = 31;

	// Use /files/database to standardize with newer android.
	public static final String DB_ROOT_SD = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/" + Constants.class.getPackage().getName() + "/files/databases";

	private final Object lock = new Object();

	public DBHelper(Context context) {
        super(context, getDbPath(context), null, DATABASE_VERSION);
    }

	public static String getDbPath(final Context context) {
		String dbPath;
		File path = context.getExternalFilesDir(null);
		if (path != null)
			dbPath = path.getAbsolutePath() + "/databases/" + DATABASE_NAME;
		else {
			path = new File(DB_ROOT_SD);
			if (!(path.mkdirs() || path.isDirectory()))
				Log.e(TAG, "DB Path doesn't exists/failed to create.");

			// create new handler due, because ui might not yet created.
			Handler h = new Handler(Looper.getMainLooper());
			h.post(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(context, String.format("Failed to create/open db directory %s", DB_ROOT_SD), Toast.LENGTH_LONG).show();
				}
			});
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
		// new table @ v28
		db.execSQL(NovelContentUserHelperModel.DATABASE_CREATE_NOVEL_CONTENT_USER);
        // new table @ v29
        db.execSQL(PageCategoriesHelper.DATABASE_CREATE_PAGE_CATEGORY);
        // new index @ v30
        db.execSQL(PageModelHelper.TABLE_PAGES_CREATE_INDEX_BY_PARENT);
    }

	@Override
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		onUpgrade(db, oldVersion, newVersion);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Context ctx = LNReaderApplication.getInstance().getApplicationContext();
        String filename = UIHelper.getBackupRoot(ctx) + "/backup_" + oldVersion + "_to_" + newVersion + ".db";
        String str = ctx.getResources().getString(R.string.db_upgrade_backup_notification, filename);
        try {
            ctx.getMainLooper().prepare();
            Toast.makeText(ctx, str, Toast.LENGTH_SHORT).show();
            copyDB(ctx, true, filename);
		} catch (Exception ex) {
            Log.i(TAG, str);
        }
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
		if (oldVersion == 26) {
			db.execSQL("UPDATE " + TABLE_PAGE + " SET " + COLUMN_PARENT + " = 'Category:Original_novel' WHERE " + COLUMN_PARENT + " = 'Category:Original'");
			db.execSQL("UPDATE " + TABLE_PAGE + " SET " + COLUMN_PARENT + " = 'Category:Light_novel_(English)' WHERE " + COLUMN_PARENT + " = 'Main_Page'");
            oldVersion = 27;
        }
		if (oldVersion == 27) {
			db.execSQL(NovelContentUserHelperModel.DATABASE_CREATE_NOVEL_CONTENT_USER);

			// move out the current user content settings to new table.
			db.execSQL("insert into " + TABLE_NOVEL_CONTENT_USER +
					" select rowid, o." + COLUMN_PAGE + ", o." + COLUMN_LAST_X + ", o." + COLUMN_LAST_Y + ", o." + COLUMN_ZOOM + ", o." + COLUMN_LAST_UPDATE + ", o." + COLUMN_LAST_CHECK +
					" from " + TABLE_NOVEL_CONTENT + " o ");
            oldVersion = 28;
        }
        if (oldVersion == 28) {
            db.execSQL(PageCategoriesHelper.DATABASE_CREATE_PAGE_CATEGORY);
            oldVersion = 29;
        }
        if (oldVersion == 29) {
            db.execSQL(PageModelHelper.TABLE_PAGES_CREATE_INDEX_BY_PARENT);
            oldVersion = 30;
        }
		if (oldVersion == 30) {
            db.execSQL("ALTER TABLE " + TABLE_IMAGE + " ADD COLUMN " + COLUMN_PARENT + " text");
			oldVersion = 31;
		}

        // ensure all table are created
        // ensure the sql use 'if not exists' clause
        onCreate(db);

        Log.i(TAG, "Upgrade DB Complete: " + oldVersion + " to " + newVersion);
	}

	public void deletePagesDB(SQLiteDatabase db) {
		// use drop because it is faster and can avoid free row fragmentation
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_PAGE);
		Log.d(TAG, TABLE_PAGE + " deleted");
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_IMAGE);
		Log.d(TAG, TABLE_IMAGE + " deleted");
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOVEL_DETAILS);
		Log.d(TAG, TABLE_NOVEL_DETAILS + " deleted");
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOVEL_BOOK);
		Log.d(TAG, TABLE_NOVEL_BOOK + " deleted");
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOVEL_CONTENT);
		Log.d(TAG, TABLE_NOVEL_CONTENT + " deleted");
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_UPDATE_HISTORY);
		Log.d(TAG, TABLE_UPDATE_HISTORY + " deleted");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PAGE_CATEGORIES);
        Log.d(TAG, TABLE_PAGE_CATEGORIES + " deleted");
        onCreate(db);
		Log.w(TAG, "Database Deleted.");
	}

	public String copyDB(Context context, boolean makeBackup, String filename) throws IOException {
		if (Util.isStringNullOrEmpty(filename)) {
			filename = UIHelper.getBackupRoot(context) + "/Backup_pages.db";
		}
		File srcPath;
		File dstPath;
		String dbPath = getDbPath(context);
		if (makeBackup) {
			Log.d(TAG, "Creating database backup");
			srcPath = new File(dbPath);
			dstPath = new File(filename);
		} else {
			Log.d(TAG, "Restoring database backup");
			dstPath = new File(dbPath);
			srcPath = new File(filename);
		}
		Log.d(TAG, "source file: " + srcPath.getAbsolutePath());
		Log.d(TAG, "destination file: " + dstPath.getAbsolutePath());
		if (srcPath.exists()) {
            if(!dstPath.exists()) {
				File dir = new File(dbPath.substring(0, dbPath.lastIndexOf("/")));
				dir.mkdirs();
                dstPath.createNewFile();
            }
			Util.copyFile(srcPath, dstPath);
			Log.d(TAG, "copy success");
			return dstPath.getPath();
		} else {
			Log.d(TAG, "source file does not exist");
			return "null";
		}
	}

	/*
	 * Cross Table Operation
	 */

	public boolean isContentUpdated(SQLiteDatabase db, PageModel page) {
		// Log.d(TAG, "isContentUpdated is called by: " + page.getPage());
		String sql = "select case when " + TABLE_PAGE + "." + COLUMN_LAST_UPDATE + " > " + TABLE_NOVEL_CONTENT + "." + COLUMN_LAST_UPDATE +
				"       then 1 else 0 end " + " from " + TABLE_PAGE +
				" join " + TABLE_NOVEL_CONTENT + " using (" + COLUMN_PAGE + ") " +
				" where " + COLUMN_PAGE + " = ? " +
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

	public ArrayList<PageModel> getAllNovelsByCategory(SQLiteDatabase db, boolean alphOrder, boolean quick, String[] categories) {
		ArrayList<PageModel> pages = new ArrayList<PageModel>();
		Cursor cursor = rawQuery(db, getNovelListQuery(alphOrder, quick), categories);
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

	public ArrayList<PageModel> getAllAlternative(SQLiteDatabase db, boolean alphOrder, boolean quick, String language) {
		ArrayList<PageModel> pages = new ArrayList<PageModel>();
		if (language != null) {
			Cursor cursor = rawQuery(db, getNovelListQuery(alphOrder, quick)
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

	private String getNovelListQuery(boolean alphOrder, boolean isQuickLoad) {
		String sql = "";
		if (isQuickLoad) {
            sql = "SELECT p.* " +
                    " , CASE WHEN group_concat(c.category) like '%Category:Completed Project%' THEN 1 " +
                    "   ELSE 0 END as is_completed " +
                    " , COUNT(b.page) as volumes " +
                    " , group_concat(c.category) as categories " +
                    " FROM " + TABLE_PAGE + " p " +
                    " LEFT JOIN " + TABLE_PAGE_CATEGORIES + " c ON p." + COLUMN_PAGE + " = c." + COLUMN_PAGE +
                    " LEFT JOIN novel_books b ON p.page = b.page " +
                    " WHERE " + COLUMN_PARENT + " = ? ";
        }
		else {
            sql = "SELECT p.* " +
                    " , CASE WHEN group_concat(c.category) like '%Category:Completed Project%' THEN 1 " +
                    "   ELSE 0 END as is_completed " +
                    " , COUNT(b.page) as volumes " +
                    " , group_concat(c.category) as categories " +
                    " , r.* " +
                    " FROM " + TABLE_PAGE + " p " +
                    " LEFT JOIN " + TABLE_PAGE_CATEGORIES + " c ON p." + COLUMN_PAGE + " = c." + COLUMN_PAGE +
                    " LEFT JOIN novel_books b ON p.page = b.page " +
                    " LEFT JOIN ( SELECT " + COLUMN_PAGE + ", SUM(UPDATESCOUNT) " +
                    "             FROM ( SELECT d." + COLUMN_PAGE +
                    "                         , CASE WHEN p2." + COLUMN_LAST_UPDATE + " > ct." + COLUMN_LAST_UPDATE +
                    "                                THEN 1 ELSE 0 END AS UPDATESCOUNT " +
                    "                    FROM " + TABLE_NOVEL_DETAILS + " d " +
                    "                    JOIN " + TABLE_NOVEL_BOOK + " b ON d." + COLUMN_PAGE + " = b." + COLUMN_PAGE +
                    "                    JOIN " + TABLE_PAGE + " p2 ON p2." + COLUMN_PARENT + " = d." + COLUMN_PAGE + " || '" + Constants.NOVEL_BOOK_DIVIDER + "' || b." + COLUMN_TITLE +
                    "                    JOIN " + TABLE_NOVEL_CONTENT + " ct ON ct." + COLUMN_PAGE + " = p2." + COLUMN_PAGE +
                    "             ) GROUP BY " + COLUMN_PAGE +
                    " ) r ON p." + COLUMN_PAGE + " = r." + COLUMN_PAGE +
                    " WHERE p." + COLUMN_PARENT + " = ? ";
        }
        sql += " GROUP BY p.page ";
		if (alphOrder)
			sql += " ORDER BY " + COLUMN_TITLE;
		else
			sql += " ORDER BY " + COLUMN_IS_WATCHED + " DESC, " + COLUMN_TITLE;

		Log.d(TAG, sql);

		return sql;
	}

	public ArrayList<PageModel> getAllWatchedNovel(SQLiteDatabase db, boolean alphOrder, boolean isQuickLoad) {
		ArrayList<PageModel> pages = new ArrayList<PageModel>();

		ArrayList<String> parents = new ArrayList<String>();
		parents.add("'" + Constants.ROOT_NOVEL_ENGLISH + "'");
		parents.add("'" + Constants.ROOT_TEASER + "'");
		parents.add("'" + Constants.ROOT_ORIGINAL + "'");
        parents.add("'" + Constants.ROOT_WEB + "'");
        for (AlternativeLanguageInfo info : AlternativeLanguageInfo.getAlternativeLanguageInfo().values()) {
			parents.add("'" + info.getCategoryInfo() + "'");
		}

		String sql = "";
		if (isQuickLoad) {
            sql = "SELECT p.* " +
                    " , CASE WHEN group_concat(c.category) like '%Category:Completed Project%' THEN 1 " +
                    "   ELSE 0 END as is_completed " +
                    " , COUNT(b.page) as volumes " +
                    " , group_concat(c.category) as categories " +
                    " FROM " + TABLE_PAGE + " p " +
                    " LEFT JOIN " + TABLE_PAGE_CATEGORIES + " c ON p." + COLUMN_PAGE + " = c." + COLUMN_PAGE +
                    " LEFT JOIN novel_books b ON p.page = b.page " +
                    " WHERE p. " + COLUMN_PARENT + " IN (" + Util.join(parents, ", ") + ") " +
                    "   AND  p." + COLUMN_IS_WATCHED + " = ? ";
        }
		else {
            sql = "SELECT p.* " +
                    " , CASE group_concat(c.category) like '%Category:Completed Project%' THEN 1 " +
                    "   ELSE 0 END as is_completed " +
                    " , COUNT(b.page) as volumes " +
                    " , group_concat(c.category) as categories " +
                    " , r.* " +
                    " FROM " + TABLE_PAGE + " p " +
					" LEFT JOIN " + TABLE_PAGE_CATEGORIES + " c ON p." + COLUMN_PAGE + " = c." + COLUMN_PAGE +
                    " LEFT JOIN novel_books b ON p.page = b.page " +
                    " LEFT JOIN ( SELECT " + COLUMN_PAGE + ", SUM(UPDATESCOUNT) " +
                    "             FROM ( SELECT d." + COLUMN_PAGE +
                    "                         , case when p2." + COLUMN_LAST_UPDATE + " > ct." + COLUMN_LAST_UPDATE +
                    "                           then 1 else 0 end as UPDATESCOUNT " +
                    "                    FROM " + TABLE_NOVEL_DETAILS + " d " +
                    "                    JOIN " + TABLE_NOVEL_BOOK + " b ON d." + COLUMN_PAGE + " = b." + COLUMN_PAGE +
                    "                    JOIN " + TABLE_PAGE + " p2 ON p2." + COLUMN_PARENT + " = d." + COLUMN_PAGE + " || '" + Constants.NOVEL_BOOK_DIVIDER + "' || b." + COLUMN_TITLE +
                    "                                           AND p2." + COLUMN_IS_MISSING + " != 1 " +
                    "                    JOIN " + TABLE_NOVEL_CONTENT + " ct ON ct." + COLUMN_PAGE + " = p2." + COLUMN_PAGE +
                    "             ) GROUP BY " + COLUMN_PAGE +
                    " ) r ON p." + COLUMN_PAGE + " = r." + COLUMN_PAGE +
                    " WHERE " + COLUMN_PARENT + " IN (" + Util.join(parents, ", ") + ") " +
                    "   AND p." + COLUMN_IS_WATCHED + " = ? ";
        }

        sql += " GROUP BY p.page ";
		if (alphOrder)
			sql += " ORDER BY " + COLUMN_TITLE;
		else
			sql += " ORDER BY " + COLUMN_IS_WATCHED + " DESC, " + COLUMN_TITLE;


        Log.d(TAG, sql);

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
		// synchronized (lock) {
		if (!db.isOpen())
			db = getReadableDatabase();
		return db.rawQuery(sql, values);
		// }
	}

	public int update(SQLiteDatabase db, String table, ContentValues cv, String whereClause, String[] whereParams) {
		synchronized (lock) {
			if (!db.isOpen())
				db = getWritableDatabase();
			int affectedRows = db.updateWithOnConflict(table, cv, whereClause, whereParams, SQLiteDatabase.CONFLICT_IGNORE);
			// Log.d(TAG, "Affected Rows: " + affectedRows);
			return affectedRows;
			// return db.update(table, cv, whereClause, whereParams);
		}
	}

	public long insertOrThrow(SQLiteDatabase db, String table, String nullColumnHack, ContentValues cv) {
		synchronized (lock) {
			if (!db.isOpen())
				db = getWritableDatabase();
			long affectedRows = db.insertOrThrow(table, nullColumnHack, cv);
			// Log.d(TAG, "Affected Rows: " + affectedRows);
			return affectedRows;
		}
	}

	public int delete(SQLiteDatabase db, String table, String whereClause, String[] whereParams) {
		synchronized (lock) {
			if (!db.isOpen())
				db = getWritableDatabase();
			int affectedRows = db.delete(table, whereClause, whereParams);
			// Log.d(TAG, "Affected Rows: " + affectedRows);
			return affectedRows;
		}
	}

	public String checkDB(SQLiteDatabase db) {
		try {
			String sqlQuery = "pragma integrity_check";
			db.rawQuery(sqlQuery, null);

            // ensure all table are there
            onCreate(db);

			return "DB OK";
		} catch (Exception ex) {
			Log.e(TAG, "DB Check failed.", ex);
			return ex.getMessage();
		}
	}
}
