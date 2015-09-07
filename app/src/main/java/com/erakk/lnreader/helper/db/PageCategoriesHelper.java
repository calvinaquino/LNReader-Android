package com.erakk.lnreader.helper.db;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.erakk.lnreader.helper.DBHelper;

import java.util.ArrayList;

public class PageCategoriesHelper {

    public static final String DATABASE_CREATE_PAGE_CATEGORY =
            "create table if not exists " + DBHelper.TABLE_PAGE_CATEGORIES + "("
                    + DBHelper.COLUMN_PAGE + " text unique not null, " // 0
                    + DBHelper.COLUMN_CATEGORY + " text not null );"; // 1
    private static final String TAG = PageCategoriesHelper.class.toString();

    public static ArrayList<String> getCategoriesByPage(DBHelper helper, SQLiteDatabase db, String page) {
        ArrayList<String> temp = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = helper.rawQuery(db, "select * from " + DBHelper.TABLE_PAGE_CATEGORIES + " where " + DBHelper.COLUMN_PAGE + " = ? ", new String[]{page});
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                temp.add(cursor.getString(1));
                break;
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return temp;
    }

    public static int insertCategoryByPage(DBHelper helper, SQLiteDatabase db, String page, ArrayList<String> categories) {
        int result = 0;

        db.beginTransaction();
        deleteCategoriesByPage(helper, db, page);
        for (String category : categories) {
            ContentValues cv = new ContentValues();
            cv.put(DBHelper.COLUMN_PAGE, page);
            cv.put(DBHelper.COLUMN_CATEGORY, category);
            result += helper.insertOrThrow(db, DBHelper.TABLE_PAGE_CATEGORIES, "null", cv);
        }
        db.endTransaction();

        Log.w(TAG, "Categories Inserted: " + result);
        return result;
    }

    public static int deleteCategoriesByPage(DBHelper helper, SQLiteDatabase db, String page) {
        int result = 0;
        result = helper.delete(db, DBHelper.TABLE_PAGE_CATEGORIES, DBHelper.COLUMN_PAGE + " = ?", new String[]{page});
        Log.w(TAG, "Categories Deleted: " + result);
        return result;
    }
}
