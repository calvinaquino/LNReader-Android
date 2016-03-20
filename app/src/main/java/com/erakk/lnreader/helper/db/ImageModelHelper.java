package com.erakk.lnreader.helper.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.erakk.lnreader.helper.DBHelper;
import com.erakk.lnreader.helper.Util;
import com.erakk.lnreader.model.ImageModel;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;

public class ImageModelHelper {
    // New column should be appended as the last column
    public static final String DATABASE_CREATE_IMAGES = "create table if not exists " + DBHelper.TABLE_IMAGE + "(" + DBHelper.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " // 0
            + DBHelper.COLUMN_IMAGE_NAME + " text unique not null, " // 1
            + DBHelper.COLUMN_FILEPATH + " text not null, " // 2
            + DBHelper.COLUMN_URL + " text not null, " // 3
            + DBHelper.COLUMN_REFERER + " text, " // 4
            + DBHelper.COLUMN_LAST_UPDATE + " integer, " // 5
            + DBHelper.COLUMN_LAST_CHECK + " integer, " // 6
            + DBHelper.COLUMN_IS_BIG_IMAGE + " boolean, " // 7
            + DBHelper.COLUMN_PARENT + " text);"; // 8
    private static final String TAG = ImageModelHelper.class.toString();

    public static ImageModel cursorToImage(Cursor cursor) {
        ImageModel image = new ImageModel();
        image.setId(cursor.getInt(0));
        image.setName(cursor.getString(1));
        image.setPath(cursor.getString(2));
        try {
            image.setUrl(new URL(cursor.getString(3)));
        } catch (MalformedURLException ex) {
            Log.e(TAG, "Invalid URL: " + cursor.getString(3), ex);
        }
        image.setReferer(cursor.getString(4));
        image.setLastUpdate(new Date(cursor.getInt(5) * 1000));
        image.setLastCheck(new Date(cursor.getInt(6) * 1000));

        image.setBigImage(cursor.getInt(7) == 1);
        image.setParent(cursor.getString(8));
        return image;
    }

	/*
     * Query Stuff
	 */

    public static ImageModel getImageByReferer(DBHelper helper, SQLiteDatabase db, ImageModel image) {
        return getImageByReferer(helper, db, image.getReferer());
    }

    public static ImageModel getImageByReferer(DBHelper helper, SQLiteDatabase db, String url) {
        // Log.d(TAG, "Selecting Image by Referer: " + url);
        ImageModel image = null;

        String nameAlt = "";
        if (url.startsWith("https")) {
            nameAlt = url.replace("https://", "http://");
        } else {
            nameAlt = url.replace("http://", "https://");
        }

        Cursor cursor = helper.rawQuery(db, "select * from " + DBHelper.TABLE_IMAGE + " where " + DBHelper.COLUMN_REFERER + " = ? or " + DBHelper.COLUMN_REFERER + " = ? ", new String[]{url, nameAlt});
        try {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                image = cursorToImage(cursor);
                Log.d(TAG, "Found by Ref: " + image.getReferer() + " name: " + image.getName() + " id: " + image.getId());
                break;
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }

        if (image == null) {
            Log.w(TAG, "Not Found Image by Referer: " + url);
        }
        return image;
    }

    public static ImageModel getImage(DBHelper helper, SQLiteDatabase db, ImageModel image) {
        return getImage(helper, db, image.getName());
    }

    public static ImageModel getImage(DBHelper helper, SQLiteDatabase db, String name) {
        // Log.d(TAG, "Selecting Image: " + name);
        ImageModel image = null;

        String nameAlt = "";
        if (name.startsWith("https")) {
            nameAlt = name.replace("https://", "http://");
        } else {
            nameAlt = name.replace("http://", "https://");
        }

        Cursor cursor = helper.rawQuery(db, "select * from " + DBHelper.TABLE_IMAGE + " where " + DBHelper.COLUMN_IMAGE_NAME + " = ? or " + DBHelper.COLUMN_IMAGE_NAME + " = ? ", new String[]{name, nameAlt});
        try {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                image = cursorToImage(cursor);
                // Log.d(TAG, "Found: " + image.getName() + " id: " + image.getId());
                break;
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }

        if (image == null) {
            Log.w(TAG, "Not Found Image: " + name);
        }
        return image;
    }

	/*
	 * Insert Stuff
	 */

    public static ImageModel insertImage(DBHelper helper, SQLiteDatabase db, ImageModel image) {
        ImageModel temp = getImage(helper, db, image.getName());

        ContentValues cv = new ContentValues();
        cv.put(DBHelper.COLUMN_IMAGE_NAME, image.getName());
        cv.put(DBHelper.COLUMN_FILEPATH, image.getPath());
        cv.put(DBHelper.COLUMN_URL, image.getUrl().toString());
        cv.put(DBHelper.COLUMN_REFERER, image.getReferer());
        cv.put(DBHelper.COLUMN_IS_BIG_IMAGE, image.isBigImage());
        cv.put(DBHelper.COLUMN_PARENT, image.getParent());
        if (temp == null) {
            cv.put(DBHelper.COLUMN_LAST_UPDATE, "" + (int) (new Date().getTime() / 1000));
            cv.put(DBHelper.COLUMN_LAST_CHECK, "" + (int) (new Date().getTime() / 1000));
            helper.insertOrThrow(db, DBHelper.TABLE_IMAGE, null, cv);
            Log.i(TAG, "Complete Insert Images: " + image.getName() + " Ref: " + image.getReferer());
        } else {
            cv.put(DBHelper.COLUMN_LAST_UPDATE, "" + (int) (temp.getLastUpdate().getTime() / 1000));
            cv.put(DBHelper.COLUMN_LAST_CHECK, "" + (int) (new Date().getTime() / 1000));
            helper.update(db, DBHelper.TABLE_IMAGE, cv, DBHelper.COLUMN_ID + " = ?", new String[]{"" + temp.getId()});
            Log.i(TAG, "Complete Update Images: " + image.getName() + " Ref: " + image.getReferer());
        }
        // get updated data
        image = getImage(helper, db, image.getName());

        // Log.d(TAG, "Complete Insert Images: " + image.getName() + " id: " + image.getId());

        return image;
    }

    public static ArrayList<ImageModel> getAllImages(DBHelper helper, SQLiteDatabase db) {
        ArrayList<ImageModel> result = new ArrayList<ImageModel>();
        Cursor cursor = helper.rawQuery(db, "select * from " + DBHelper.TABLE_IMAGE, null);
        try {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                ImageModel image = cursorToImage(cursor);
                result.add(image);
                cursor.moveToNext();
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return result;
    }

    public static ArrayList<ImageModel> getAllImagesByParent(DBHelper helper, SQLiteDatabase db, String parent) {
        ArrayList<ImageModel> result = new ArrayList<ImageModel>();
        Cursor cursor = helper.rawQuery(db, "select * from " + DBHelper.TABLE_IMAGE + " where " + DBHelper.COLUMN_PARENT + " = ? " , new String[] {parent} );
        try {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                ImageModel image = cursorToImage(cursor);
                result.add(image);
                cursor.moveToNext();
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return result;
    }
	/*
	 * Delete Stuff
	 */

    public static int deleteImageByParent(DBHelper helper, SQLiteDatabase db, String parent) {
        Log.d(TAG, "Start deleting images for parent: " + parent);
        int result = 0;
        if (!Util.isStringNullOrEmpty(parent)) {
            // get the files to delete
            ArrayList<ImageModel> imagesToDelete = getAllImagesByParent(helper, db, parent);
            for( ImageModel image : imagesToDelete) {
                String path = image.getPath().replace("?", "_");
                File f = new File(path);
                try {
                    if(f.exists()) {
                        boolean del = f.delete();
                        if (!del)
                            del = f.getCanonicalFile().delete();
                        if (!del) {
                            Log.w(TAG, "Failed to delete image file: " + path);
                        } else {
                            Log.i(TAG, "Deleted: " + path);
                        }
                    }
                    else {
                        Log.w(TAG, "File doesn't exists: " + path);
                    }
                }catch (IOException ex) {
                    Log.e(TAG, ex.getMessage(), ex);
                }
            }

            // delete from DB
            result = helper.delete(db, DBHelper.TABLE_IMAGE, DBHelper.COLUMN_PARENT + " = ?", new String[]{parent});
        }
        Log.w(TAG, "Images Deleted: " + result);
        return result;
    }

}
