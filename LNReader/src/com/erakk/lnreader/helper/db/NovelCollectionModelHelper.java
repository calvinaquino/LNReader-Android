/*
 * NovelCollectionModel ==> Details + Chapter list
 * Nested Object:
 * - ArrayList<BookModel>
 *   - ArrayList<PageModel>
 */
package com.erakk.lnreader.helper.db;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.DBHelper;
import com.erakk.lnreader.model.BookModel;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.PageModel;

public class NovelCollectionModelHelper {
	private static final String TAG = NovelCollectionModelHelper.class.toString();
	private static DBHelper helper = NovelsDao.getInstance().getDBHelper();

	// New column should be appended as the last column
	public static final String DATABASE_CREATE_NOVEL_DETAILS = "create table if not exists "
			+ DBHelper.TABLE_NOVEL_DETAILS + "(" + DBHelper.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ DBHelper.COLUMN_PAGE + " text unique not null, "
			+ DBHelper.COLUMN_SYNOPSIS + " text not null, "
			+ DBHelper.COLUMN_IMAGE_NAME + " text not null, "
			+ DBHelper.COLUMN_LAST_UPDATE + " integer, "
			+ DBHelper.COLUMN_LAST_CHECK + " integer);";

	public static NovelCollectionModel cursorToNovelCollection(Cursor cursor) {
		NovelCollectionModel novelDetails = new NovelCollectionModel();
		novelDetails.setId(cursor.getInt(0));
		novelDetails.setPage(cursor.getString(1));
		novelDetails.setSynopsis(cursor.getString(2));
		novelDetails.setCover(cursor.getString(3));
		novelDetails.setLastUpdate(new Date(cursor.getInt(4) * 1000));
		novelDetails.setLastCheck(new Date(cursor.getInt(5) * 1000));
		return novelDetails;
	}

	/*
	 * Query Stuff
	 */
	public static NovelCollectionModel getNovelDetails(SQLiteDatabase db, String page) {
		// Log.d(TAG, "Selecting Novel Details: " + page);
		NovelCollectionModel novelDetails = getNovelDetailsOnly(db, page);

		if (novelDetails != null) {
			novelDetails.setPageModel(PageModelHelper.getPageModel(db, page));

			// get the books
			ArrayList<BookModel> bookCollection = BookModelHelper.getBookCollectionOnly(db, page, novelDetails);

			// get the chapters
			for (Iterator<BookModel> iBook = bookCollection.iterator(); iBook.hasNext();) {
				BookModel book = iBook.next();
				ArrayList<PageModel> chapters = getChapterCollection(db, novelDetails.getPage() + Constants.NOVEL_BOOK_DIVIDER + book.getTitle(), book);
				book.setChapterCollection(chapters);
			}
			novelDetails.setBookCollections(bookCollection);
		}
		else {
			Log.w(TAG, "No Data for Novel Details: " + page);
		}

		// Log.d(TAG, "Complete Selecting Novel Details: " + page);
		return novelDetails;
	}

	public static NovelCollectionModel getNovelDetailsOnly(SQLiteDatabase db, String page) {
		NovelCollectionModel novelDetails = null;
		Cursor cursor = helper.rawQuery(db, "select * from " + DBHelper.TABLE_NOVEL_DETAILS
				+ " where " + DBHelper.COLUMN_PAGE + " = ? ", new String[] { page });
		try {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				novelDetails = cursorToNovelCollection(cursor);
				// Log.d(TAG, "Found: " + novelDetails.toString());
				break;
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return novelDetails;
	}

	public static ArrayList<PageModel> getChapterCollection(SQLiteDatabase db, String parent, BookModel book) {
		ArrayList<PageModel> chapters = new ArrayList<PageModel>();
		Cursor cursor = helper.rawQuery(db, "select * from " + DBHelper.TABLE_PAGE
				+ " where " + DBHelper.COLUMN_PARENT + " = ? order by " + DBHelper.COLUMN_ORDER, new String[] { parent });
		try {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				PageModel chapter = PageModelHelper.cursorToPageModel(cursor);
				chapter.setBook(book);
				chapters.add(chapter);
				// Log.d(TAG, "Found: " + chapter.toString());
				cursor.moveToNext();
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return chapters;
	}

	/*
	 * Insert Stuff
	 */
	public static NovelCollectionModel insertNovelDetails(SQLiteDatabase db, NovelCollectionModel novelDetails) {
		ContentValues cv = new ContentValues();
		cv.put(DBHelper.COLUMN_PAGE, novelDetails.getPage());
		cv.put(DBHelper.COLUMN_SYNOPSIS, novelDetails.getSynopsis());
		if(novelDetails.getCover() == null)
			cv.put(DBHelper.COLUMN_IMAGE_NAME, "");
		else
			cv.put(DBHelper.COLUMN_IMAGE_NAME, novelDetails.getCover());
		cv.put(DBHelper.COLUMN_LAST_CHECK, "" + (int) (new Date().getTime() / 1000));

		// check if exist
		NovelCollectionModel temp = getNovelDetailsOnly(db, novelDetails.getPage());
		if (temp == null) {
			// Log.d(TAG, "Inserting Novel Details: " + novelDetails.getPage());
			if (novelDetails.getLastUpdate() == null)
				cv.put(DBHelper.COLUMN_LAST_UPDATE, 0);
			else
				cv.put(DBHelper.COLUMN_LAST_UPDATE, "" + (int) (novelDetails.getLastUpdate().getTime() / 1000));
			helper.insertOrThrow(db, DBHelper.TABLE_NOVEL_DETAILS, null, cv);
		}
		else {
			// Log.d(TAG, "Updating Novel Details: " + novelDetails.getPage() + " id: " + temp.getId());
			cv.put(DBHelper.COLUMN_LAST_UPDATE, "" + (int) (temp.getLastUpdate().getTime() / 1000));
			helper.update(db, DBHelper.TABLE_NOVEL_DETAILS, cv, DBHelper.COLUMN_ID + " = ?", new String[] { "" + temp.getId() });
		}

		// insert book
		for (BookModel book : novelDetails.getBookCollections()) {
			book.setPage(novelDetails.getPage());
			book.setLastUpdate(novelDetails.getLastUpdate());
			book = BookModelHelper.insertBookModel(db, book);
		}

		// insert chapter
		for (Iterator<BookModel> iBooks = novelDetails.getBookCollections().iterator(); iBooks.hasNext();) {
			BookModel book = iBooks.next();
			for (PageModel page : book.getChapterCollection()) {
				PageModelHelper.insertOrUpdatePageModel(db, page, false);
			}
		}

		// Log.d(TAG, "Complete Insert Novel Details: " + novelDetails.toString());

		// get updated data
		novelDetails = getNovelDetails(db, novelDetails.getPage());
		return novelDetails;
	}

	/*
	 * Delete Stuff
	 */
	public static void deleteNovelDetails(SQLiteDatabase db, NovelCollectionModel details) {
		int result = helper.delete(db, DBHelper.TABLE_NOVEL_DETAILS, DBHelper.COLUMN_ID + " = ?", new String[] { "" + details.getId() });
		Log.w(TAG, "NovelDetails Deleted: " + result);
	}

	public static int deleteNovel(SQLiteDatabase db, PageModel novel) {
		try {
			// TODO: delete images

			// delete chapter and books
			NovelCollectionModel details = getNovelDetails(db, novel.getPage());
			if (details != null) {
				ArrayList<BookModel> books = details.getBookCollections();
				for (BookModel bookModel : books) {
					BookModelHelper.deleteBookModel(db, bookModel);
				}
				deleteNovelDetails(db, details);
			}
			// delete page model;
			return PageModelHelper.deletePageModel(db, novel);
		} catch (Exception ex) {
			Log.e(TAG, "Error when deleting: " + novel.getPage(), ex);
			return 0;
		}
	}

}
