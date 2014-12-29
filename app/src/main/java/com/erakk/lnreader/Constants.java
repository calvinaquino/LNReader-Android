package com.erakk.lnreader;

import java.util.Date;

import android.graphics.Color;

public class Constants {

    public static final String ROOT_URL = "//www.baka-tsuki.org";
    public static final String ROOT_HTTP = "http:";
    public static final String ROOT_HTTPS = "https:";

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
    public static final String EXTRA_PAGE_EXTERNAL = "com.erakk.lnreader.page.isExternal";

    public static final String EXTRA_FIND_MISSING_MODE = "find_missing_mode";
    public static final String PREF_MISSING_CHAPTER = "find_missing_chapter";
    public static final String PREF_REDLINK_CHAPTER = "find_redlink_chapter";
    public static final String PREF_EMPTY_BOOK = "find_empty_book";
    public static final String PREF_EMPTY_NOVEL = "find_empty_novel";
    public static final String PREF_SHOW_MAINT_WARNING = "maint_show_warning";

    public static final String EXTRA_NOVEL_LIST_MODE = "novel_list_mode";
    public static final String EXTRA_NOVEL_LIST_MODE_MAIN = "Main";
    public static final String EXTRA_NOVEL_LIST_MODE_ORIGINAL = "Original";
    public static final String EXTRA_NOVEL_LIST_MODE_TEASER = "Teaser";

    public static final String NOVEL_BOOK_DIVIDER = "%NOVEL_BOOK_DIVIDER%";
    public static final long CHECK_INTERVAL = 7 * 24 * 3600 * 1000;

    // Shared Preferences keys
    public static final String PREF_FIRST_RUN = "first_run";
    public static final String PREF_DOWNLOAD_TOUCH = "touch_download_chapter";
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
    public static final String PREF_TIMEOUT = "timeout";
    public static final String PREF_RETRY = "retry";
    public static final String PREF_INCREASE_RETRY = "increase_retry";
    public static final String PREF_IMAGE_SAVE_LOC = "save_location";
    public static final String PREF_AGGRESIVE_TITLE_CLEAN_UP = "aggresive_title_clean_up";
    public static final String PREF_USE_HTTPS = "use_https";
    public static final String PREF_HIDE_EMPTY_VOLUME = "hide_empty_volume";
    public static final String PREF_TTS_PITCH = "tts_pitch";
    public static final String PREF_TTS_SPEECH_RATE = "tts_reading_speed";
    public static final String PREF_TTS_DELAY = "tts_whitespace_delay";
    public static final String PREF_TTS_ENGINE = "tts_engine";
    public static final String PREF_TTS_TTS_STOP_ON_LOST_FOCUS = "tts_stop_on_lost_focus";
    public static final String PREF_AUTO_DOWNLOAD_UPDATED_CHAPTER = "auto_download_updated_chapter";
    public static final String PREF_BACKUP_THUMB_IMAGES = "backup_thumb_images";
    public static final String PREF_BACKUP_DB = "backup_database";
    public static final String PREF_RESTORE_DB = "restore_database";
    public static final String PREF_RESTORE_THUMB_IMAGES = "restore_thumb_images";
    public static final String PREF_RELINK_THUMB_IMAGES = "relink_images";
    public static final String PREF_BOOKMARK_ORDER = "bookmark_order";
    public static final String PREF_PROCESS_ALL_IMAGES = "process_all_images";
    public static final String PREF_SHOW_REDLINK = "show_redlink";
    public static final String PREF_KITKAT_WEBVIEW_FIX = "webview_kitkat_fix";
    public static final String PREF_KITKAT_WEBVIEW_FIX_DELAY = "webview_kitkat_fix_delay";
    public static final String PREF_LAST_AUTO_BACKUP_TIME = "last_auto_backup";
    public static final String PREF_AUTO_BACKUP_COUNT = "auto_backup_count";
    public static final String PREF_LAST_AUTO_BACKUP_INDEX = "last_auto_backup_index";
    public static final String PREF_AUTO_BACKUP_ENABLED = "auto_backup_enabled";
    public static final String PREF_BACKUP_LOCATION = "backup_location";
    public static final String PREF_UPDATE_INCLUDE_REDLINK = "update_include_redlink";
    public static final String PREF_USE_APP_KEYSTORE = "https_use_my_cert";
    public static final String PREF_SAVE_EXTERNAL_URL = "save_external_url";
    public static final String PREF_CLEAR_EXTERNAL_TEMP = "clear_external_temp";
    public static final String PREF_UPDATE_INCLUDE_EXTERNAL = "update_include_external";
    public static final String PREF_QUICK_LOAD = "quick_load";
    public static final String PREF_HEADING_FONT = "css_heading_fontface";
    public static final String PREF_CONTENT_FONT = "css_content_fontface";
    public static final String PREF_CSS_CUSTOM_COLOR = "css_use_custom_colors";
    public static final String PREF_CSS_BACKGROUND = "css_background";
    public static final String PREF_CSS_FOREGROUND = "css_foreground";
    public static final String PREF_CSS_LINK_COLOR = "css_link";
    public static final String PREF_CSS_TABLE_BORDER = "css_thumb-border";
    public static final String PREF_CSS_TABLE_BACKGROUND = "css_thumb-back";
    public static final String PREF_TTS_ENABLED = "tts_is_enabled";

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
    public static final int COLOR_REDLINK = Color.parseColor("#ff69b4");
    ;

    public static final String ROOT_NOVEL_ENGLISH = "Category:Light_novel_(English)";
    public static final String ROOT_ORIGINAL = "Category:Original_novel";
    public static final String ROOT_TEASER = "Category:Teasers";

    /* Section of Task Key */
    public static final String KEY_LOAD_CHAPTER = ":LoadChapter:";
    public static final String KEY_DOWNLOAD_CHAPTER = ":DownloadChapters:";
    public static final String KEY_DOWNLOAD_ALL_CHAPTER = ":DownloadChaptersAll:";

    /* Section of Language for novel parser */
    public static final String LANG_ENGLISH = "English";
    public static final String LANG_BAHASA_INDONESIA = "Bahasa Indonesia";
    public static final String LANG_FRENCH = "Français";
    public static final String LANG_POLISH = "Polish";

    /* You just need to add a new alternative language here and in xml -> parse_lang_info.xml */
    public static final String[] languageList = {Constants.LANG_ENGLISH, Constants.LANG_FRENCH, Constants.LANG_BAHASA_INDONESIA, Constants.LANG_POLISH};
    public static final int BUFFER = 1024;

    /* Pattern for accepting sub-category */
    public static final String[] categoryPattern = {"Teaser", "novel", "Novel", "project", "Project"};

    /**
     * URL used for wiki API to get the contents
     */
    public static final String API_URL_CONTENT = "%s/project/api.php?action=parse&format=xml&prop=text|images&redirects=yes&page=%s";
    public static final String API_REDLINK = "&action=edit&redlink=1";
    /**
     * URL used for wiki API to get the page info
     */
    public static final String API_URL_INFO = "%s/project/api.php?action=query&prop=info|revisions&format=xml&redirects=yes&titles=%s";

}
