package com.erakk.lnreader.helper.db;

import java.util.ArrayList;
import java.util.Date;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.DBHelper;
import com.erakk.lnreader.model.BookmarkModel;
import com.erakk.lnreader.model.PageModel;

public class BookmarkModelHelper {
	private static final String TAG = BookmarkModelHelper.class.toString();
	private static DBHelper helper = NovelsDao.getInstance().getDBHelper();

	// New column should be appended as the last column
	public static final String DATABASE_CREATE_NOVEL_BOOKMARK = "create table if not exists "
		      + DBHelper.TABLE_NOVEL_BOOKMARK + "(" + DBHelper.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "	// 0
						      						+ DBHelper.COLUMN_PAGE + " text not null, "						// 1
								  				    + DBHelper.COLUMN_PARAGRAPH_INDEX + " integer, "				// 2
								  				    + DBHelper.COLUMN_EXCERPT + " text, "							// 3
								  				    + DBHelper.COLUMN_CREATE_DATE + " integer);";					// 4

	public static BookmarkModel cursorToNovelBookmark(Cursor cursor) {
		BookmarkModel bookmark = new BookmarkModel();
		bookmark.setId(cursor.getInt(0));
		bookmark.setPage(cursor.getString(1));
		bookmark.setpIndex(cursor.getInt(2));
		bookmark.setExcerpt(cursor.getString(3));
		bookmark.setCreationDate(new Date(cursor.getLong(4)*1000));
		return bookmark;
	}

	/*
	 * Query Stuff
	 */

	public static ArrayList<BookmarkModel> getAllBookmarks(SQLiteDatabase db) {
		ArrayList<BookmarkModel> bookmarks = new ArrayList<BookmarkModel>();

		Cursor cursor = helper.rawQuery(db, "select * from " + DBHelper.TABLE_NOVEL_BOOKMARK
				                   + " order by " + DBHelper.COLUMN_PAGE
				                   + ", " + DBHelper.COLUMN_PARAGRAPH_INDEX, null);
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

	public static ArrayList<BookmarkModel> getBookmarks(SQLiteDatabase db, PageModel page) {
		ArrayList<BookmarkModel> bookmarks = new ArrayList<BookmarkModel>();

		Cursor cursor = helper.rawQuery(db, "select * from " + DBHelper.TABLE_NOVEL_BOOKMARK
										 + " where " + DBHelper.COLUMN_PAGE + " = ? ", new String[] {page.getPage()});
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

	private static BookmarkModel getBookmark(SQLiteDatabase db, String page, int pIndex) {
		BookmarkModel bookmark = null;

		Cursor cursor = helper.rawQuery(db, "select * from " + DBHelper.TABLE_NOVEL_BOOKMARK
										 + " where " + DBHelper.COLUMN_PAGE + " = ? and " + DBHelper.COLUMN_PARAGRAPH_INDEX + " = ? "
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

	/*
	 * Insert Stuff
	 */

	public static int insertBookmark(SQLiteDatabase db, BookmarkModel bookmark) {
		BookmarkModel tempBookmark = getBookmark(db, bookmark.getPage(), bookmark.getpIndex());

		if(tempBookmark == null) {
			ContentValues cv = new ContentValues();
			cv.put(DBHelper.COLUMN_PAGE, bookmark.getPage());
			cv.put(DBHelper.COLUMN_PARAGRAPH_INDEX, bookmark.getpIndex());
			String excerpt = bookmark.getExcerpt();
			if(excerpt.length() > 200) {
				excerpt = excerpt.substring(0, 197) + "...";
			}
			cv.put(DBHelper.COLUMN_EXCERPT, excerpt);
			cv.put(DBHelper.COLUMN_CREATE_DATE, (int) (new Date().getTime() / 1000));
			return (int) helper.insertOrThrow(db, DBHelper.TABLE_NOVEL_BOOKMARK, null, cv);
		}
		else {
			Log.d(TAG, "Bookmark already created.");
			return 0;
		}

	}

	/*
	 * Delete Stuff
	 */

	public static int deleteBookmark(SQLiteDatabase db,BookmarkModel bookmark) {
		return helper.delete(db, DBHelper.TABLE_NOVEL_BOOKMARK, DBHelper.COLUMN_PAGE + " = ? and " + DBHelper.COLUMN_PARAGRAPH_INDEX + " = ? "
				 , new String[] {bookmark.getPage(), "" + bookmark.getpIndex()});
	}
}
