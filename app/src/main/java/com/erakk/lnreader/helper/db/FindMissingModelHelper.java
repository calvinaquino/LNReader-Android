package com.erakk.lnreader.helper.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.helper.DBHelper;
import com.erakk.lnreader.model.FindMissingModel;
import com.erakk.lnreader.model.PageModel;

import java.util.ArrayList;

public class FindMissingModelHelper {

    public static ArrayList<FindMissingModel> getAllRedlinkChapter(DBHelper helper, SQLiteDatabase db) {
        ArrayList<FindMissingModel> list = new ArrayList<FindMissingModel>();

        String sql = "SELECT " + DBHelper.COLUMN_PAGE +
                ", " + DBHelper.COLUMN_TITLE +
                ", " + DBHelper.COLUMN_PARENT +
                ", " + DBHelper.COLUMN_IS_DOWNLOADED +
                ", " + DBHelper.COLUMN_ID +
                " FROM " + DBHelper.TABLE_PAGE +
                " WHERE " + DBHelper.COLUMN_TYPE + " = '" + PageModel.TYPE_CONTENT + "'" +
                "   AND " + DBHelper.COLUMN_PAGE + " LIKE '%redlink=1' " +
                " ORDER BY " + DBHelper.COLUMN_PAGE;

        Cursor cursor = helper.rawQuery(db, sql, null);
        try {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                FindMissingModel page = cursorToRedlinkFindMissing(cursor);
                list.add(page);
                cursor.moveToNext();
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return list;
    }

    private static FindMissingModel cursorToRedlinkFindMissing(Cursor cursor) {
        FindMissingModel model = new FindMissingModel();
        model.setPage(cursor.getString(0));
        model.setTitle(cursor.getString(1));

        String details = cursor.getString(2);
        if (details != null) {
            details = details.replace(Constants.NOVEL_BOOK_DIVIDER, " - ");
        }
        model.setDetails(details);

        model.setDownloaded(cursor.getInt(3) == 1);
        model.setId(cursor.getInt(4));

        return model;
    }

    public static ArrayList<FindMissingModel> getAllMissingChapter(DBHelper helper, SQLiteDatabase db) {
        ArrayList<FindMissingModel> list = new ArrayList<FindMissingModel>();

        String sql = "SELECT " + DBHelper.COLUMN_PAGE +
                ", " + DBHelper.COLUMN_TITLE +
                ", " + DBHelper.COLUMN_PARENT +
                ", " + DBHelper.COLUMN_IS_DOWNLOADED +
                ", " + DBHelper.COLUMN_ID +
                " FROM " + DBHelper.TABLE_PAGE +
                " WHERE " + DBHelper.COLUMN_TYPE + " = '" + PageModel.TYPE_CONTENT + "'" +
                "   AND " + DBHelper.COLUMN_IS_MISSING + " = 1 " +
                " ORDER BY " + DBHelper.COLUMN_PAGE;

        Cursor cursor = helper.rawQuery(db, sql, null);
        try {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                FindMissingModel page = cursorToRedlinkFindMissing(cursor);
                list.add(page);
                cursor.moveToNext();
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return list;
    }

    public static ArrayList<FindMissingModel> getAllEmptyBook(DBHelper helper, SQLiteDatabase db) {
        ArrayList<FindMissingModel> list = new ArrayList<FindMissingModel>();

        String sql = "SELECT b." + DBHelper.COLUMN_PAGE +
                ", ifnull(b." + DBHelper.COLUMN_PAGE + ", b." + DBHelper.COLUMN_TITLE + ") " +
                ", p." + DBHelper.COLUMN_PAGE +
                ", p." + DBHelper.COLUMN_IS_DOWNLOADED +
                ", b." + DBHelper.COLUMN_ID +
                " FROM " + DBHelper.TABLE_NOVEL_BOOK + " b " +
                " LEFT JOIN " + DBHelper.TABLE_PAGE + " p on p." + DBHelper.COLUMN_PARENT + " = b." + DBHelper.COLUMN_PAGE + "||'" + Constants.NOVEL_BOOK_DIVIDER + "'||b." + DBHelper.COLUMN_TITLE +
                " WHERE p." + DBHelper.COLUMN_PAGE + " IS NULL " +
                " ORDER BY b." + DBHelper.COLUMN_PAGE;

        Cursor cursor = helper.rawQuery(db, sql, null);
        try {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                FindMissingModel page = cursorToRedlinkFindMissing(cursor);
                list.add(page);
                cursor.moveToNext();
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return list;
    }

    public static ArrayList<FindMissingModel> getAllEmptyNovel(DBHelper helper, SQLiteDatabase db) {
        ArrayList<FindMissingModel> list = new ArrayList<FindMissingModel>();

        String sql = "SELECT p." + DBHelper.COLUMN_PAGE +
                ", p." + DBHelper.COLUMN_TITLE +
                ", p." + DBHelper.COLUMN_PARENT +
                ", case when d." + DBHelper.COLUMN_PAGE + " is null then 0 else 1 end as is_downloaded " +
                ", b." + DBHelper.COLUMN_TITLE +
                ", p." + DBHelper.COLUMN_ID +
                " FROM " + DBHelper.TABLE_PAGE + " p " +
                " LEFT JOIN " + DBHelper.TABLE_NOVEL_DETAILS + " d ON p." + DBHelper.COLUMN_PAGE + " = d." + DBHelper.COLUMN_PAGE +
                " LEFT JOIN " + DBHelper.TABLE_NOVEL_BOOK + " b ON p." + DBHelper.COLUMN_PAGE + " = b." + DBHelper.COLUMN_PAGE +
                " WHERE p." + DBHelper.COLUMN_TYPE + " = '" + PageModel.TYPE_NOVEL + "' " + "" +
                "   AND b." + DBHelper.COLUMN_TITLE + " IS NULL " +
                "   AND p." + DBHelper.COLUMN_PARENT + " IS NOT NULL" +
                " ORDER BY p." + DBHelper.COLUMN_PARENT + ", p." + DBHelper.COLUMN_PAGE;

        Cursor cursor = helper.rawQuery(db, sql, null);
        try {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                FindMissingModel page = cursorToRedlinkFindMissing(cursor);
                list.add(page);
                cursor.moveToNext();
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return list;
    }

}
