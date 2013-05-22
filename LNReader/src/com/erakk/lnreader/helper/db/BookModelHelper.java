package com.erakk.lnreader.helper.db;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.DBHelper;
import com.erakk.lnreader.model.BookModel;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.PageModel;

public class BookModelHelper {
	private static final String TAG = BookModelHelper.class.toString();
	private static DBHelper helper = NovelsDao.getInstance().getDBHelper();

	// New column should be appended as the last column
	// COLUMN_PAGE is not unique because being used for reference to the novel page.
	public static final String DATABASE_CREATE_NOVEL_BOOKS = "create table if not exists "
		      + DBHelper.TABLE_NOVEL_BOOK + "(" + DBHelper.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "	// 0
				 				  + DBHelper.COLUMN_PAGE + " text not null, "						// 1
				  				  + DBHelper.COLUMN_TITLE + " text not null, "						// 2
				  				  + DBHelper.COLUMN_LAST_UPDATE + " integer, "						// 3
				  				  + DBHelper.COLUMN_LAST_CHECK + " integer, "						// 4
				  				  + DBHelper.COLUMN_ORDER + " integer);";							// 5

	public static BookModel cursorToBookModel(Cursor cursor) {
		BookModel book = new BookModel();
		book.setId(cursor.getInt(0));
		book.setPage(cursor.getString(1));
		book.setTitle(cursor.getString(2));
		book.setLastUpdate(new Date(cursor.getInt(3)*1000));
		book.setLastCheck(new Date(cursor.getInt(4)*1000));
		book.setOrder(cursor.getInt(5));
		return book;
	}

	/*
	 * Query Stuff
	 */
	public static BookModel getBookModel(SQLiteDatabase db, int id) {
		BookModel book = null;
		Cursor cursor = helper.rawQuery(db, "select * from " + DBHelper.TABLE_NOVEL_BOOK
										 + " where " + DBHelper.COLUMN_ID + " = ? ", new String[] {"" + id});
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

	public static BookModel getBookModel(SQLiteDatabase db, String page, String title) {
		BookModel book = null;
		Cursor cursor = helper.rawQuery(db, "select * from " + DBHelper.TABLE_NOVEL_BOOK
										 + " where " + DBHelper.COLUMN_PAGE + " = ? and " + DBHelper.COLUMN_TITLE + " = ?", new String[] {page, title});
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

	public static ArrayList<BookModel> getBookCollectionOnly(SQLiteDatabase db, String page, NovelCollectionModel novelDetails) {
		// get the books
	    ArrayList<BookModel> bookCollection = new ArrayList<BookModel>();
	    Cursor cursor = helper.rawQuery(db, "select * from " + DBHelper.TABLE_NOVEL_BOOK
	    								 + " where " + DBHelper.COLUMN_PAGE + " = ? "
	    								 + " order by " + DBHelper.COLUMN_ORDER, new String[] {page});
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

	/*
	 * Insert Stuff
	 */

	/*
	 * Delete Stuff
	 */
	public static void deleteBookModel(SQLiteDatabase db, BookModel book) {
		int chaptersCount = 0;
		ArrayList<PageModel> chapters = book.getChapterCollection();
		if(chapters != null && chapters.size() > 0) {
			for(Iterator<PageModel> i = chapters.iterator(); i.hasNext();) {
				PageModel page = i.next();
				chaptersCount += helper.delete(db, DBHelper.TABLE_PAGE, DBHelper.COLUMN_ID + " = ? ", new String[] {"" + page.getId()});
			}
			Log.w(TAG, "Deleted PageModel: " + chaptersCount);
		}
		int bookCount = helper.delete(db, DBHelper.TABLE_NOVEL_BOOK, DBHelper.COLUMN_ID + " = ? ", new String[] { "" + book.getId() });
		Log.w(TAG, "Deleted BookModel: " + bookCount);
	}
}
