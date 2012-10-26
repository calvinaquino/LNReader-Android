package com.erakk.lnreader;

import java.util.Date;

import android.os.Environment;

public class Constants {

	//public static final String BaseURL = "http://www.baka-tsuki.org/project/";
	public static final String BASE_URL = "http://www.baka-tsuki.org";
	
	// Intent parameter list
	public static final String EXTRA_NOVEL = "com.erakk.lnreader.NOVEL";
	public static final String EXTRA_PAGE = "com.erakk.lnreader.page";
	public static final String EXTRA_TITLE = "com.erakk.lnreader.title";
	public static final String EXTRA_VOLUME = "com.erakk.lnreader.volume";
	public static final String EXTRA_ONLY_WATCHED = "com.erakk.lnreader.ONLY_WATCHED";
	public static final String EXTRA_IMAGE_URL = "com.erakk.lnreader.IMAGE_URL";
	public static final String EXTRA_SCROLL_X = "com.erakk.lnreader.SCROLL_X";
	public static final String EXTRA_SCROLL_Y = "com.erakk.lnreader.SCROLL_Y";
	
	public static final String IMAGE_ROOT = Environment.getExternalStorageDirectory().getAbsolutePath().toString() + "/Android/data/" + Constants.class.getPackage().getName() + "/files";
	
	public static final int IMAGE_DOWNLOAD_RETRY = 3;
	public static final int PAGE_DOWNLOAD_RETRY = 3;
	
	public static final String NOVEL_BOOK_DIVIDER = "%";
	public static final int CHECK_INTERVAL = 7;
	
	public static final int TIMEOUT = 60000;
	
	public static final String TAG = "LNReader";
	
	// Shared Preferences keys
	public static final String PREF_INVERT_COLOR = "invert_colors";
	public static final String PREF_LOCK_HORIZONTAL = "lock_horizontal";
	public static final String PREF_LAST_READ = "last_read";
	public static final String PREF_UPDATE_INTERVAL = "updates_interval";
	public static final String PREF_RUN_UPDATES = "run_update";
	public static final String PREF_RUN_UPDATES_STATUS = "run_update_status";
	public static final String PREF_DOWLOAD_BIG_IMAGE = "download_big_image";
	public static final String PREF_ZOOM_ENABLED = "enable_zoom";
	public static final String PREF_SHOW_IMAGE = "show_images";
	public static final String PREF_SHOW_ZOOM_CONTROL = "show_zoom_control";
	public static final String PREF_UPDATE_RING = "update_use_sound";
	public static final String PREF_UPDATE_VIBRATE = "update_use_vibration";
	
	public static final float DISPLAY_SCALE = LNReaderApplication.getInstance().getResources().getDisplayMetrics().density ;

	@SuppressWarnings("deprecation")
	public static final int NOTIFIER_ID = (int)(new Date().getTime() - new Date(2012, 1, 1).getTime());
}
