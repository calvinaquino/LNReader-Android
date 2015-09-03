package com.erakk.lnreader.helper.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.helper.DBHelper;
import com.erakk.lnreader.helper.Util;
import com.erakk.lnreader.model.NovelContentUserModel;
import com.erakk.lnreader.model.PageModel;

import java.util.Date;

public class NovelContentUserHelperModel {
    // New column should be appended as the last column
    public static final String DATABASE_CREATE_NOVEL_CONTENT_USER = "create table if not exists " + DBHelper.TABLE_NOVEL_CONTENT_USER + "("
            + DBHelper.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " // 0
            + DBHelper.COLUMN_PAGE + " text unique not null, " // 1
            + DBHelper.COLUMN_LAST_X + " integer, " // 2
            + DBHelper.COLUMN_LAST_Y + " integer, " // 3
            + DBHelper.COLUMN_ZOOM + " double, " // 4
            + DBHelper.COLUMN_LAST_UPDATE + " integer, " // 5
            + DBHelper.COLUMN_LAST_CHECK + " integer);"; // 6
    private static final String TAG = NovelContentUserHelperModel.class.toString();

    public static NovelContentUserModel cursorToModel(Cursor cursor) {
        NovelContentUserModel model = new NovelContentUserModel();
        model.setId(cursor.getInt(0));
        model.setPage(cursor.getString(1));
        model.setLastXScroll(cursor.getInt(2));
        model.setLastYScroll(cursor.getInt(3));
        model.setLastZoom(cursor.getDouble(4));
        model.setLastUpdate(new Date(cursor.getLong(5) * 1000));
        model.setLastCheck(new Date(cursor.getLong(6) * 1000));
        return model;
    }

     /*
     * Query Stuff
	 */

    public static NovelContentUserModel getNovelContentUserModel(DBHelper helper, SQLiteDatabase db, String page) {
        NovelContentUserModel content = null;

        Cursor cursor = helper.rawQuery(db, "select * from " + DBHelper.TABLE_NOVEL_CONTENT_USER + " where " + DBHelper.COLUMN_PAGE + " = ? ", new String[]{page});
        try {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                content = cursorToModel(cursor);
                break;
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        if (content == null) {
            Log.w(TAG, "Not Found Novel Content User: " + page);
        }
        return content;
    }


	/*
     * Insert Stuff
	 */

    public static NovelContentUserModel insertModel(DBHelper helper, SQLiteDatabase db, NovelContentUserModel content) throws Exception {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.COLUMN_PAGE, content.getPage());
        cv.put(DBHelper.COLUMN_ZOOM, "" + content.getLastZoom());
        cv.put(DBHelper.COLUMN_LAST_CHECK, "" + (int) (new Date().getTime() / 1000));

        NovelContentUserModel temp = getNovelContentUserModel(helper, db, content.getPage());
        if (temp == null) {
            cv.put(DBHelper.COLUMN_LAST_X, "" + content.getLastXScroll());
            cv.put(DBHelper.COLUMN_LAST_Y, "" + content.getLastYScroll());

            if (content.getLastUpdate() == null)
                cv.put(DBHelper.COLUMN_LAST_UPDATE, 0);
            else
                cv.put(DBHelper.COLUMN_LAST_UPDATE, "" + (int) (content.getLastUpdate().getTime() / 1000));
            long id = helper.insertOrThrow(db, DBHelper.TABLE_NOVEL_CONTENT_USER, null, cv);
            Log.i(TAG, "Novel Content Inserted, New id: " + id);
        } else {
            cv.put(DBHelper.COLUMN_LAST_X, "" + content.getLastXScroll());
            cv.put(DBHelper.COLUMN_LAST_Y, "" + content.getLastYScroll());

            if (content.getLastUpdate() == null)
                cv.put(DBHelper.COLUMN_LAST_UPDATE, "" + (int) (temp.getLastUpdate().getTime() / 1000));
            else
                cv.put(DBHelper.COLUMN_LAST_UPDATE, "" + (int) (content.getLastUpdate().getTime() / 1000));
            int result = helper.update(db, DBHelper.TABLE_NOVEL_CONTENT_USER, cv, DBHelper.COLUMN_ID + " = ? ", new String[]{"" + temp.getId()});
            Log.i(TAG, "Novel Content:" + content.getPage() + " Updated, Affected Row: " + result);
        }

        content = getNovelContentUserModel(helper, db, content.getPage());
        return content;
    }

	/*
     * Delete Stuff
	 */

    public static boolean deleteNovelContentUser(DBHelper helper, SQLiteDatabase db, String page) {
        if (page != null) {
            int result = helper.delete(db, DBHelper.TABLE_NOVEL_CONTENT_USER, DBHelper.COLUMN_PAGE + " = ?", new String[]{"" + page});
            Log.w(TAG, "NovelContentUser Deleted: " + result);
            return result > 0 ? true : false;
        }
        return false;
    }

    public static int deleteNovelContentUser(DBHelper helper, SQLiteDatabase db, PageModel ref) {
        if (ref != null && !Util.isStringNullOrEmpty(ref.getPage())) {
            int result = helper.delete(db, DBHelper.TABLE_NOVEL_CONTENT_USER, DBHelper.COLUMN_PAGE + " = ?", new String[]{"" + ref.getPage()});
            Log.w(TAG, "NovelContentUser Deleted: " + result);
            return result;
        }
        return 0;
    }

    public static int resetZoomLevel(DBHelper helper, SQLiteDatabase db) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.COLUMN_ZOOM, LNReaderApplication.getInstance().getResources().getInteger(R.integer.default_zoom) / 100);
        int result = helper.update(db, DBHelper.TABLE_NOVEL_CONTENT_USER, cv, null, null);
        return result;
    }
}
