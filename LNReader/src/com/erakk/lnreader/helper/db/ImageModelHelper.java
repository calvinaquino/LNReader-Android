package com.erakk.lnreader.helper.db;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.DBHelper;
import com.erakk.lnreader.model.ImageModel;

public class ImageModelHelper {
	private static final String TAG = ImageModelHelper.class.toString();
	private static DBHelper helper = NovelsDao.getInstance().getDBHelper();

	// New column should be appended as the last column
	public static final String DATABASE_CREATE_IMAGES = "create table if not exists " + DBHelper.TABLE_IMAGE + "(" + DBHelper.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " // 0
			+ DBHelper.COLUMN_IMAGE_NAME + " text unique not null, " // 1
			+ DBHelper.COLUMN_FILEPATH + " text not null, " // 2
			+ DBHelper.COLUMN_URL + " text not null, " // 3
			+ DBHelper.COLUMN_REFERER + " text, " // 4
			+ DBHelper.COLUMN_LAST_UPDATE + " integer, " // 5
			+ DBHelper.COLUMN_LAST_CHECK + " integer, " // 6
			+ DBHelper.COLUMN_IS_BIG_IMAGE + " boolean);"; // 7

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

		image.setBigImage(cursor.getInt(7) == 1 ? true : false);
		return image;
	}

	/*
	 * Query Stuff
	 */

	public static ImageModel getImageByReferer(SQLiteDatabase db, ImageModel image) {
		return getImageByReferer(db, image.getReferer());
	}

	public static ImageModel getImageByReferer(SQLiteDatabase db, String url) {
		// Log.d(TAG, "Selecting Image by Referer: " + url);
		ImageModel image = null;

		String nameAlt = "";
		if (url.startsWith("https")) {
			nameAlt = url.replace("https://", "http://");
		} else {
			nameAlt = url.replace("http://", "https://");
		}

		Cursor cursor = helper.rawQuery(db, "select * from " + DBHelper.TABLE_IMAGE + " where " + DBHelper.COLUMN_REFERER + " = ? or " + DBHelper.COLUMN_REFERER + " = ? ", new String[] { url, nameAlt });
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

	public static ImageModel getImage(SQLiteDatabase db, ImageModel image) {
		return getImage(db, image.getName());
	}

	public static ImageModel getImage(SQLiteDatabase db, String name) {
		// Log.d(TAG, "Selecting Image: " + name);
		ImageModel image = null;

		String nameAlt = "";
		if (name.startsWith("https")) {
			nameAlt = name.replace("https://", "http://");
		} else {
			nameAlt = name.replace("http://", "https://");
		}

		Cursor cursor = helper.rawQuery(db, "select * from " + DBHelper.TABLE_IMAGE + " where " + DBHelper.COLUMN_IMAGE_NAME + " = ? or " + DBHelper.COLUMN_IMAGE_NAME + " = ? ", new String[] { name, nameAlt });
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

	public static ImageModel insertImage(SQLiteDatabase db, ImageModel image) {
		ImageModel temp = getImage(db, image.getName());

		ContentValues cv = new ContentValues();
		cv.put(DBHelper.COLUMN_IMAGE_NAME, image.getName());
		cv.put(DBHelper.COLUMN_FILEPATH, image.getPath());
		cv.put(DBHelper.COLUMN_URL, image.getUrl().toString());
		cv.put(DBHelper.COLUMN_REFERER, image.getReferer());
		cv.put(DBHelper.COLUMN_IS_BIG_IMAGE, image.isBigImage());
		if (temp == null) {
			cv.put(DBHelper.COLUMN_LAST_UPDATE, "" + (int) (new Date().getTime() / 1000));
			cv.put(DBHelper.COLUMN_LAST_CHECK, "" + (int) (new Date().getTime() / 1000));
			helper.insertOrThrow(db, DBHelper.TABLE_IMAGE, null, cv);
			Log.i(TAG, "Complete Insert Images: " + image.getName() + " Ref: " + image.getReferer());
		} else {
			cv.put(DBHelper.COLUMN_LAST_UPDATE, "" + (int) (temp.getLastUpdate().getTime() / 1000));
			cv.put(DBHelper.COLUMN_LAST_CHECK, "" + (int) (new Date().getTime() / 1000));
			helper.update(db, DBHelper.TABLE_IMAGE, cv, DBHelper.COLUMN_ID + " = ?", new String[] { "" + temp.getId() });
			Log.i(TAG, "Complete Update Images: " + image.getName() + " Ref: " + image.getReferer());
		}
		// get updated data
		image = getImage(db, image.getName());

		// Log.d(TAG, "Complete Insert Images: " + image.getName() + " id: " + image.getId());

		return image;
	}

	/*
	 * Delete Stuff
	 */
	// TODO: only have bulk delete by dropping the table...
}
