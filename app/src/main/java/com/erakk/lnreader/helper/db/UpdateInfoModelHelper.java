package com.erakk.lnreader.helper.db;

import java.util.ArrayList;
import java.util.Date;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.DBHelper;
import com.erakk.lnreader.model.UpdateInfoModel;
import com.erakk.lnreader.model.UpdateType;

public class UpdateInfoModelHelper {
	private static final String TAG = UpdateInfoModelHelper.class.toString();
	private static DBHelper helper = NovelsDao.getInstance().getDBHelper();

	// New column should be appended as the last column
	public static final String DATABASE_CREATE_UPDATE_HISTORY = "create table if not exists "
			  + DBHelper.TABLE_UPDATE_HISTORY + "(" + DBHelper.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "	// 0
			  						+ DBHelper.COLUMN_PAGE + " text not null, "							// 1
			  						+ DBHelper.COLUMN_UPDATE_TITLE + " text not null, "					// 2
			  						+ DBHelper.COLUMN_UPDATE_TYPE + " integer not null, "				// 3
			  						+ DBHelper.COLUMN_LAST_UPDATE + " integer);";						// 4

	public static UpdateInfoModel cursorToUpdateInfoModel(Cursor cursor) {
		UpdateInfoModel update = new UpdateInfoModel();
		update.setId(cursor.getInt(0));
		update.setUpdatePage(cursor.getString(1));
		update.setUpdateTitle(cursor.getString(2));
		int type = cursor.getInt(3);
		update.setUpdateType(UpdateType.values()[type]);
		update.setUpdateDate(new Date(cursor.getLong(4)*1000));
		return update;
	}

	/*
	 * Query Stuff
	 */

	public static ArrayList<UpdateInfoModel> getAllUpdateHistory(SQLiteDatabase db) {
		ArrayList<UpdateInfoModel> updates = new ArrayList<UpdateInfoModel>();

		Cursor cursor = helper.rawQuery(db, "select * from " + DBHelper.TABLE_UPDATE_HISTORY
					                   + " order by case "
					                   + "   when " + DBHelper.COLUMN_UPDATE_TYPE + " = " + UpdateType.UpdateTos.ordinal() + " then 0 "
					                   + "   when " + DBHelper.COLUMN_UPDATE_TYPE + " = " + UpdateType.NewNovel.ordinal() + " then 1 "
					                   + "   when " + DBHelper.COLUMN_UPDATE_TYPE + " = " + UpdateType.New.ordinal() + " then 2 "
					                   + "   when " + DBHelper.COLUMN_UPDATE_TYPE + " = " + UpdateType.Updated.ordinal() + " then 3 "
					                   + "   else 4 end "
					                   + ", " + DBHelper.COLUMN_LAST_UPDATE + " desc "
					                   + ", " + DBHelper.COLUMN_PAGE, null);
		try {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				UpdateInfoModel update = cursorToUpdateInfoModel(cursor);
				updates.add(update);
				cursor.moveToNext();
			}
		} finally{
			if(cursor != null) cursor.close();
		}

		return updates;
	}

	public static UpdateInfoModel getUpdateHistory(SQLiteDatabase db, UpdateInfoModel update) {
		UpdateInfoModel result = null;

		Cursor cursor = helper.rawQuery(db, "select * from " + DBHelper.TABLE_UPDATE_HISTORY
										 + " where " + DBHelper.COLUMN_PAGE + " = ? "
				                   , new String[] {update.getUpdatePage()});
		try {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				result = cursorToUpdateInfoModel(cursor);
				break;
			}
		} finally{
			if(cursor != null) cursor.close();
		}

		return result;
	}

	/*
	 * Insert Stuff
	 */

	public static int insertUpdateHistory(SQLiteDatabase db, UpdateInfoModel update) {
		UpdateInfoModel tempUpdate = getUpdateHistory(db, update);

		ContentValues cv = new ContentValues();
		cv.put(DBHelper.COLUMN_PAGE, update.getUpdatePage());
		cv.put(DBHelper.COLUMN_UPDATE_TITLE, update.getUpdateTitle());
		cv.put(DBHelper.COLUMN_UPDATE_TYPE, update.getUpdateType().ordinal());
		cv.put(DBHelper.COLUMN_LAST_UPDATE, (int) (update.getUpdateDate().getTime() / 1000));

		if(tempUpdate == null) {
			return (int) helper.insertOrThrow(db, DBHelper.TABLE_UPDATE_HISTORY, null, cv);
		}
		else {
			return helper.update(db, DBHelper.TABLE_UPDATE_HISTORY, cv, DBHelper.COLUMN_ID + " = ? ", new String[] {"" + tempUpdate.getId()});
		}
	}

	/*
	 * Delete Stuff
	 */

	public static void deleteAllUpdateHistory(SQLiteDatabase db) {
		db.execSQL("DROP TABLE IF EXISTS " + DBHelper.TABLE_UPDATE_HISTORY);
		db.execSQL(DATABASE_CREATE_UPDATE_HISTORY);
		Log.d(TAG, "Recreate " + DBHelper.TABLE_UPDATE_HISTORY);
	}

	public static int deleteUpdateHistory(SQLiteDatabase db, UpdateInfoModel updateInfo) {
		Log.d(TAG, "Deleting UpdateInfoModel id: " + updateInfo.getId());
		return helper.delete(db, DBHelper.TABLE_UPDATE_HISTORY, DBHelper.COLUMN_ID + " = ? "
				 , new String[] {updateInfo.getId() + ""});
	}
}
