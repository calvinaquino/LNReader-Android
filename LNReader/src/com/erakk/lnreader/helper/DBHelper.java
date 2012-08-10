//package com.nandaka.bakareaderclone.helper;
package com.erakk.lnreader.helper;

import java.util.ArrayList;
import java.util.Date;

import com.erakk.lnreader.model.PageModel;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {
	public static final String TAG = DBHelper.class.toString();
	public static final String TABLE_PAGE = "pages";
	public static final String COLUMN_PAGE = "page";
	public static final String COLUMN_LAST_UPDATE = "last_update";
	public static final String COLUMN_TITLE = "title";	
	public static final String COLUMN_TYPE = "type";
	
	private static final String[] ALL_COLUMS = new String[] {COLUMN_PAGE,
															 COLUMN_TITLE,
															 COLUMN_TYPE, 
															 COLUMN_LAST_UPDATE};
	private static final String DATABASE_NAME = "pages.db";
	private static final int DATABASE_VERSION = 2;
	
	private SQLiteDatabase database;
	
	// Database creation sql statement
	private static final String DATABASE_CREATE_PAGES = "create table "
	      + TABLE_PAGE + "(" + COLUMN_PAGE + " text primary key not null, "
			  				 + COLUMN_TITLE + " text not null, "
			  				 + COLUMN_TYPE + " text not null, "
			  				 + COLUMN_LAST_UPDATE + " integer);";

	public DBHelper(Context context) {
	    super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	@Override
	public void onCreate(SQLiteDatabase db) {
		 db.execSQL(DATABASE_CREATE_PAGES);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(DBHelper.class.getName(),
		        "Upgrading database from version " + oldVersion + " to "
		            + newVersion + ", which will destroy all old data");
	    db.execSQL("DROP TABLE IF EXISTS " + TABLE_PAGE);
	    onCreate(db);
	}
	
	public ArrayList<PageModel> selectAll() {
		return selectAllByColumn("1", "1");
	}
	
	public ArrayList<PageModel> selectAllByColumn(String column, String value) {
		ArrayList<PageModel> pages = new ArrayList<PageModel>();
		database = this.getWritableDatabase();
		
		//Cursor cursor = database.query(TABLE_PAGE, ALL_COLUMS, 
		//		column + "=" + value, null, null, null, null);
		Cursor cursor = database.rawQuery("select * from " + TABLE_PAGE + " where " + column + " = ?", new String[] {value});
		cursor.moveToFirst();
	    while (!cursor.isAfterLast()) {
	    	PageModel page = cursorTopage(cursor);
	    	pages.add(page);
	    	cursor.moveToNext();
	    }		
		database.close();
		return pages;
	}
	
	public PageModel selectFirstBy(String column, String value){
		Log.d(TAG, "Select First: Column = " + column + " Value = " + value);
		PageModel page = null;
		database = this.getWritableDatabase();
		
		Cursor cursor = database.rawQuery("select * from " + TABLE_PAGE + " where " + column + " = ?", new String[] {value});
		//Cursor cursor = database.query(TABLE_PAGE, ALL_COLUMS, 
		//		COLUMN_PAGE + "= '" + value + "'", null, null, null, null);
		cursor.moveToFirst();
	    while (!cursor.isAfterLast()) {
	    	page = cursorTopage(cursor);
	    	Log.d(TAG, "Found: " + page.toString());
	    	break;
	    }		
		database.close();
		return page;
	}
	
	public String insertOrUpdate(PageModel page){
		Log.d(TAG, page.toString());
		
		PageModel temp = selectFirstBy(COLUMN_PAGE, page.getTitle());
		
		database = this.getWritableDatabase();
		if(temp == null) {
			Log.d(TAG, "Inserting: " + page.toString());
			ContentValues cv = new ContentValues();
			cv.put(COLUMN_PAGE, page.getPage());
			cv.put(COLUMN_TITLE, page.getTitle());
			cv.put(COLUMN_TYPE, page.getType());
			cv.put(COLUMN_LAST_UPDATE, "" + page.getLastUpdate().getSeconds());
			database.insertOrThrow(TABLE_PAGE, null, cv);
		}
		else {
			database.rawQuery("update " + TABLE_PAGE + " set " + COLUMN_TITLE + " = ?, " +
					   											 COLUMN_TYPE + " = ?, " +
					   											 COLUMN_LAST_UPDATE + " = ?" +
					   		  " where " + COLUMN_PAGE + " = ?",
					   		  new String[] {page.getTitle(), page.getType(), "" + page.getLastUpdate().getSeconds(), temp.getPage()});
			Log.d(TAG, "Updating: " + page.toString());
		}
		database.close();
		return page.getPage();
	}
	
	private PageModel cursorTopage(Cursor cursor) {
		PageModel page = new PageModel();
		page.setPage(cursor.getString(0));
		page.setTitle(cursor.getString(1));
		page.setType(cursor.getString(2));
		page.setLastUpdate(new Date(cursor.getLong(3)*1000));
	    return page;
	}
}
