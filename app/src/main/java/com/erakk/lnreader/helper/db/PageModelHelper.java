package com.erakk.lnreader.helper.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.erakk.lnreader.AlternativeLanguageInfo;
import com.erakk.lnreader.Constants;
import com.erakk.lnreader.helper.DBHelper;
import com.erakk.lnreader.helper.Util;
import com.erakk.lnreader.model.PageModel;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

public class PageModelHelper {

    // New column should be appended as the last column
    public static final String DATABASE_CREATE_PAGES = "create table if not exists " + DBHelper.TABLE_PAGE + "("
            + DBHelper.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " // 0
            + DBHelper.COLUMN_PAGE + " text unique not null, " // 1
            + DBHelper.COLUMN_TITLE + " text not null, " // 2
            + DBHelper.COLUMN_TYPE + " text, " // 3
            + DBHelper.COLUMN_PARENT + " text, " // 4
            + DBHelper.COLUMN_LAST_UPDATE + " integer, " // 5
            + DBHelper.COLUMN_LAST_CHECK + " integer, " // 6
            + DBHelper.COLUMN_IS_WATCHED + " boolean, " // 7
            + DBHelper.COLUMN_IS_FINISHED_READ + " boolean, " // 8
            + DBHelper.COLUMN_IS_DOWNLOADED + " boolean, " // 9
            + DBHelper.COLUMN_ORDER + " integer, " // 10
            + DBHelper.COLUMN_STATUS + " text, " // 11
            + DBHelper.COLUMN_IS_MISSING + " boolean, " // 12
            + DBHelper.COLUMN_IS_EXTERNAL + " boolean, " // 13
            + DBHelper.COLUMN_LANGUAGE + " text not null default '" + Constants.LANG_ENGLISH + "');"; // 14
    private static final String TAG = PageModelHelper.class.toString();

    public static PageModel cursorToPageModel(Cursor cursor) {
        PageModel page = new PageModel();
        page.setId(cursor.getInt(0));
        page.setPage(cursor.getString(1));
        page.setTitle(cursor.getString(2));
        page.setType(cursor.getString(3));
        page.setParent(cursor.getString(4));
        page.setLastUpdate(new Date(cursor.getLong(5) * 1000));
        page.setLastCheck(new Date(cursor.getLong(6) * 1000));
        page.setWatched(cursor.getInt(7) == 1 ? true : false);
        page.setFinishedRead(cursor.getInt(8) == 1 ? true : false);
        page.setDownloaded(cursor.getInt(9) == 1 ? true : false);
        page.setOrder(cursor.getInt(10));
        page.setStatus(cursor.getString(11));
        page.setMissing(cursor.getInt(12) == 1 ? true : false);
        page.setExternal(cursor.getInt(13) == 1 ? true : false);
        page.setLanguage(cursor.getString(14));

        if (cursor.getColumnCount() > 15) {
            page.setUpdateCount(cursor.getInt(16));
        }
        return page;
    }

	/*
     * Queries Stuff
	 */

    public static ArrayList<PageModel> getAllContentPageModel(DBHelper helper, SQLiteDatabase db) {
        ArrayList<PageModel> result = new ArrayList<PageModel>();

        String sql = "select a.* " +
                " from " + DBHelper.TABLE_PAGE + " a " +
                " join " + DBHelper.TABLE_NOVEL_CONTENT + " b " +
                "   on " + "a." + DBHelper.COLUMN_PAGE + " = b." + DBHelper.COLUMN_PAGE;
        Cursor cursor = helper.rawQuery(db, sql, null);
        try {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                PageModel page = cursorToPageModel(cursor);
                result.add(page);
                cursor.moveToNext();
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return result;
    }

    public static PageModel getAlternativePage(DBHelper helper, SQLiteDatabase db, String language) {
        /* Return PageModel depends on language */
        PageModel page = null;
        page = getPageModel(helper, db, AlternativeLanguageInfo.getAlternativeLanguageInfo().get(language).getCategoryInfo());
        return page;
    }

    public static PageModel getPageModel(DBHelper helper, SQLiteDatabase db, String page) {
        PageModel pageModel = null;
        Cursor cursor = null;
        try {
            cursor = helper.rawQuery(db, "select * from " + DBHelper.TABLE_PAGE + " where " + DBHelper.COLUMN_PAGE + " = ? ", new String[]{page});
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                pageModel = cursorToPageModel(cursor);
                // Log.d(TAG, "Found Page: " + pageModel.toString());
                break;
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }

        // check again for case insensitive
        if (pageModel == null) {
            try {
                cursor = helper.rawQuery(db, "select * from " + DBHelper.TABLE_PAGE + " where lower(" + DBHelper.COLUMN_PAGE + ") = lower(?) ", new String[]{page});
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    pageModel = cursorToPageModel(cursor);
                    // Log.d(TAG, "Found Page: " + pageModel.toString());
                    break;
                }
            } finally {
                if (cursor != null)
                    cursor.close();
            }
        }
        return pageModel;
    }

    public static PageModel getPageModel(DBHelper helper, SQLiteDatabase db, int id) {
        PageModel pageModel = null;
        Cursor cursor = helper.rawQuery(db, "select * from " + DBHelper.TABLE_PAGE + " where " + DBHelper.COLUMN_ID + " = ? ", new String[]{"" + id});
        try {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                pageModel = cursorToPageModel(cursor);
                // Log.d(TAG, "Found Page: " + pageModel.toString());
                break;
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return pageModel;
    }

    public static ArrayList<PageModel> selectAllByColumn(DBHelper helper, SQLiteDatabase db, String whereQuery, String[] values) {
        return selectAllByColumn(helper, db, whereQuery, values, null);
    }

    public static ArrayList<PageModel> selectAllByColumn(DBHelper helper, SQLiteDatabase db, String whereQuery, String[] values, String orderQuery) {
        ArrayList<PageModel> pages = new ArrayList<PageModel>();

        String sql = "select * from " + DBHelper.TABLE_PAGE + " where " + whereQuery;
        if (orderQuery != null && orderQuery.length() > 0) {
            sql += " order by " + orderQuery;
        }

        Cursor cursor = helper.rawQuery(db, sql, values);
        try {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                PageModel page = cursorToPageModel(cursor);
                pages.add(page);
                cursor.moveToNext();
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return pages;
    }

    public static PageModel selectFirstBy(DBHelper helper, SQLiteDatabase db, String column, String value) {
        // Log.d(TAG, "Select First: Column = " + column + " Value = " + value);
        PageModel page = null;

        Cursor cursor = helper.rawQuery(db, "select * from " + DBHelper.TABLE_PAGE + " where " + column + " = ? ", new String[]{value});
        try {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                page = cursorToPageModel(cursor);
                // Log.d(TAG, "Found: " + page.toString());
                break;
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return page;
    }

    /*
     * Insert Stuff
     */
    public static ArrayList<PageModel> insertAllNovel(DBHelper helper, SQLiteDatabase db, ArrayList<PageModel> list) {
        ArrayList<PageModel> updatedList = new ArrayList<PageModel>();
        for (Iterator<PageModel> i = list.iterator(); i.hasNext(); ) {
            PageModel p = i.next();
            p = insertOrUpdatePageModel(helper, db, p, false);
            updatedList.add(p);
        }
        return updatedList;
    }

    /**
     * Insert/Update Page Model, with note:
     * - isDownloaded flag is set from NovelContentModelHelper (insert/delete).
     * - if PageModel.id > 0, the content will be updated from input page.
     *
     * @param db
     * @param page
     * @param updateStatus
     * @return
     */
    public static PageModel insertOrUpdatePageModel(DBHelper helper, SQLiteDatabase db, PageModel page, boolean updateStatus) {
        // Log.d(TAG, page.toString());

        PageModel temp = selectFirstBy(helper, db, DBHelper.COLUMN_PAGE, page.getPage());

        ContentValues cv = new ContentValues();
        cv.put(DBHelper.COLUMN_PAGE, page.getPage());
        cv.put(DBHelper.COLUMN_LANGUAGE, page.getLanguage());
        cv.put(DBHelper.COLUMN_TITLE, page.getTitle());
        cv.put(DBHelper.COLUMN_ORDER, page.getOrder());
        cv.put(DBHelper.COLUMN_PARENT, page.getParent());
        cv.put(DBHelper.COLUMN_TYPE, page.getType());
        if (updateStatus)
            cv.put(DBHelper.COLUMN_STATUS, page.getStatus());
        cv.put(DBHelper.COLUMN_IS_MISSING, page.isMissing());
        cv.put(DBHelper.COLUMN_IS_EXTERNAL, page.isExternal());

        if (temp == null) {
            // Fresh Data
            // Log.d(TAG, "Inserting: " + page.toString());
            if (page.getLastUpdate() == null)
                cv.put(DBHelper.COLUMN_LAST_UPDATE, 0);
            else
                cv.put(DBHelper.COLUMN_LAST_UPDATE, "" + (int) (page.getLastUpdate().getTime() / 1000));
            if (page.getLastCheck() == null)
                cv.put(DBHelper.COLUMN_LAST_CHECK, "" + (int) (new Date().getTime() / 1000));
            else
                cv.put(DBHelper.COLUMN_LAST_CHECK, "" + (int) (page.getLastCheck().getTime() / 1000));

            cv.put(DBHelper.COLUMN_IS_WATCHED, page.isWatched());
            cv.put(DBHelper.COLUMN_IS_FINISHED_READ, page.isFinishedRead());
            cv.put(DBHelper.COLUMN_IS_DOWNLOADED, page.isDownloaded());

            long id = helper.insertOrThrow(db, DBHelper.TABLE_PAGE, null, cv);
            Log.i(TAG, "Page Model Inserted, New Id: " + id);
        } else {
            // Log.d(TAG, "Updating: " + temp.toString());
            if (page.getLastUpdate() == null)
                cv.put(DBHelper.COLUMN_LAST_UPDATE, "" + (int) (temp.getLastUpdate().getTime() / 1000));
            else
                cv.put(DBHelper.COLUMN_LAST_UPDATE, "" + (int) (page.getLastUpdate().getTime() / 1000));
            if (page.getLastCheck() == null)
                cv.put(DBHelper.COLUMN_LAST_CHECK, "" + (int) (temp.getLastCheck().getTime() / 1000));
            else
                cv.put(DBHelper.COLUMN_LAST_CHECK, "" + (int) (page.getLastCheck().getTime() / 1000));

            if (page.getId() > 0) {
                // new data model have an id, use the input value.
                cv.put(DBHelper.COLUMN_IS_WATCHED, page.isWatched());
                cv.put(DBHelper.COLUMN_IS_FINISHED_READ, page.isFinishedRead());
                cv.put(DBHelper.COLUMN_IS_DOWNLOADED, page.isDownloaded());
            } else {
                cv.put(DBHelper.COLUMN_IS_WATCHED, temp.isWatched());
                cv.put(DBHelper.COLUMN_IS_FINISHED_READ, temp.isFinishedRead());
                cv.put(DBHelper.COLUMN_IS_DOWNLOADED, temp.isDownloaded());
            }

            int result = helper.update(db, DBHelper.TABLE_PAGE, cv, DBHelper.COLUMN_ID + " = ?", new String[]{"" + temp.getId()});
            Log.d(TAG, "Page Model: " + page.getPage() + " Updated, Affected Row: " + result);
        }

        // get the updated data.
        page = getPageModel(helper, db, page.getPage());
        return page;
    }

    /*
     * Delete Stuff
     */
    public static int deletePageModel(DBHelper helper, SQLiteDatabase db, PageModel tempPage) {
        int result = 0;
        if (tempPage.getId() > 0) {
            result = helper.delete(db, DBHelper.TABLE_PAGE, DBHelper.COLUMN_ID + " = ?", new String[]{"" + tempPage.getId()});
        } else if (!Util.isStringNullOrEmpty(tempPage.getPage())) {
            result = helper.delete(db, DBHelper.TABLE_PAGE, DBHelper.COLUMN_PAGE + " = ?", new String[]{tempPage.getPage()});
        }
        Log.w(TAG, "PageModel Deleted: " + result);
        return result;
    }
}
