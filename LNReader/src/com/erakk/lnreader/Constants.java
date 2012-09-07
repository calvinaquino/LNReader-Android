package com.erakk.lnreader;

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
	
	public static final String IMAGE_ROOT = Environment.getExternalStorageDirectory().getAbsolutePath().toString() + "/lnreader_cache";
	
	public static final int IMAGE_DOWNLOAD_RETRY = 3;
	public static final int PAGE_DOWNLOAD_RETRY = 3;
	
	public static final String NOVEL_BOOK_DIVIDER = "%";
	public static final int CHECK_INTERVAL = 7;
	
	public static final int TIMEOUT = 60000;
	
	public static final String TAG = "LNReader";
}
