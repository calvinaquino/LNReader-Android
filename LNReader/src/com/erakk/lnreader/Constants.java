package com.erakk.lnreader;

import java.util.Date;

import android.graphics.Color;
import android.os.Environment;

public class Constants {

	// public static final String BaseURL = "http://www.baka-tsuki.org/project/";
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
	public static final String EXTRA_P_INDEX = "pIndex";
	public static final String EXTRA_CALLER_ACTIVITY = "caller_activity";

	public static final String IMAGE_ROOT = Environment.getExternalStorageDirectory().getAbsolutePath().toString() + "/Android/data/" + Constants.class.getPackage().getName() + "/files";

	public static final int IMAGE_DOWNLOAD_RETRY = 3;
	public static final int PAGE_DOWNLOAD_RETRY = 3;

	public static final String NOVEL_BOOK_DIVIDER = "%NOVEL_BOOK_DIVIDER%";
	public static final int CHECK_INTERVAL = 7;

	public static final int TIMEOUT = 60000;

	public static final String TAG = "LNReader";

	// Shared Preferences keys
	public static final String PREF_FIRST_RUN = "first_run";
	public static final String PREF_DOWNLOAD_TOUCH = "auto_download_chapter";
	public static final String PREF_INVERT_COLOR = "invert_colors";
	public static final String PREF_LANGUAGE = "language_selection";
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
	public static final String PREF_PERSIST_NOTIFICATION = "persist_notification";
	public static final String PREF_USE_VOLUME_FOR_SCROLL = "use_volume_to_scroll";
	public static final String PREF_SCROLL_SIZE = "scroll_size";
	public static final String PREF_IS_NOVEL_ONLY = "novel_only";
	public static final String PREF_INVERT_SCROLL = "invert_scroll";
	public static final String PREF_ALPH_ORDER = "alphabet_order";
	public static final String PREF_SHOW_MISSING = "show_missing";
	public static final String PREF_SHOW_EXTERNAL = "show_external";
	public static final String PREF_KEEP_AWAKE = "keep_awake";
	public static final String PREF_FULSCREEN = "fullscreen";
	public static final String PREF_ENABLE_BOOKMARK = "enable_bookmark";
	public static final String PREF_ENABLE_WEBVIEW_BUTTONS = "enable_webview_buttons";
	public static final String PREF_USE_INTERNAL_WEBVIEW = "use_internal_webview";
	public static final String PREF_CONSOLIDATE_NOTIFICATION = "consolidate_notification";
	public static final String PREF_FORCE_JUSTIFIED = "force_justified";
	public static final String PREF_STRETCH_COVER = "strech_detail_image";
	public static final String PREF_LINESPACING = "line_spacing";
	public static final String PREF_USE_CUSTOM_CSS = "use_custom_css";
	public static final String PREF_CUSTOM_CSS_PATH = "custom_css_path";
	public static final String PREF_MARGINS = "margin_space";
	public static final String PREF_UI_SELECTION = "ui_selection";
	public static final String PREF_ORIENTATION = "orientation_lock";
	public static final String PREF_LAST_UPDATE = "last_update";

	public static final float DISPLAY_SCALE = LNReaderApplication.getInstance().getResources().getDisplayMetrics().density;

	@SuppressWarnings("deprecation")
	public static final int NOTIFIER_ID = (int) (new Date().getTime() - new Date(2012, 1, 1).getTime());
	public static final int CONSOLIDATED_NOTIFIER_ID = 20130210;

	public static final String STATUS_TEASER = "teaser";
	public static final String STATUS_STALLED = "stalled";
	public static final String STATUS_ABANDONED = "abandoned";
	public static final String STATUS_PENDING = "pending";
	public static final String STATUS_ORIGINAL = "original";
	public static final String STATUS_BAHASA_INDONESIA = "indonesian";

	public static final int COLOR_READ = Color.parseColor("#888888");
	public static final int COLOR_UNREAD = Color.parseColor("#dddddd");
	public static final int COLOR_UNREAD_DARK = Color.parseColor("#222222");
	public static final int COLOR_MISSING = Color.parseColor("#ff0000");
	public static final int COLOR_EXTERNAL = Color.parseColor("#3333ff");

	public static final String ROOT_NOVEL = "Main_Page";

	/* Section of Task Key */
	public static final String KEY_LOAD_CHAPTER = ":LoadChapter:";
	public static final String KEY_DOWNLOAD_CHAPTER = ":DownloadChapters:";
	public static final String KEY_DOWNLOAD_ALL_CHAPTER = ":DownloadChaptersAll:";

	/* Section of Language for novel parser */
	public static final String LANG_ENGLISH = "English";
	public static final String LANG_BAHASA_INDONESIA = "Bahasa Indonesia";
	public static final String LANG_FRENCH = "Français";
	public static final String LANG_POLISH = "Polish";

	/* You just need to add a new alternative language here and in AlternativeLanguageInfo -> initSingleton */
	public static final String[] languageList = { Constants.LANG_ENGLISH, Constants.LANG_FRENCH, Constants.LANG_BAHASA_INDONESIA, Constants.LANG_POLISH };

}
