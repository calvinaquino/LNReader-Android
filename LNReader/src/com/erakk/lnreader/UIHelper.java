package com.erakk.lnreader;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.View.OnSystemUiVisibilityChangeListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.erakk.lnreader.helper.Util;

/*
 * Class for handling all the UI with API Warning ==> @SuppressLint("NewApi")
 */
public class UIHelper {

	private static final String TAG = UIHelper.class.toString();

	public static void CheckScreenRotation(Activity activity) {
		switch (getIntFromPreferences(Constants.PREF_ORIENTATION, 0)) {
		case 0:
			activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
			break;
		case 1:
			activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			break;
		case 2:
			activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			break;
		default:
			activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
			break;
		}
	}

	/**
	 * Set action bar behaviour, only for API Level 11 and up.
	 * 
	 * @param activity
	 *            target activity
	 * @param enable
	 *            enable up behaviour
	 */
	@SuppressLint("NewApi")
	public static void SetActionBarDisplayHomeAsUp(Activity activity, boolean enable) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			ActionBar actionBar = activity.getActionBar();
			if (actionBar != null)
				actionBar.setDisplayHomeAsUpEnabled(enable);
		}
		// CheckScreenRotation(activity);
		CheckKeepAwake(activity);
	}

	/**
	 * Recreate the activity
	 * 
	 * @param activity
	 *            target activity
	 */
	@SuppressLint("NewApi")
	public static void Recreate(Activity activity) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			activity.recreate();
		else {
			activity.finish();
			activity.startActivity(activity.getIntent());
		}
		CheckScreenRotation(activity);
		CheckKeepAwake(activity);
	}

	/**
	 * Set up the application theme based on Preferences:Constants.PREF_INVERT_COLOR
	 * 
	 * @param activity
	 *            target activity
	 * @param layoutId
	 *            layout to use
	 */
	public static void SetTheme(Activity activity, Integer layoutId) {
		CheckScreenRotation(activity);
		if (PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(Constants.PREF_INVERT_COLOR, true)) {
			activity.setTheme(R.style.AppTheme2);
		} else {
			activity.setTheme(R.style.AppTheme);
		}
		if (layoutId != null) {
			activity.setContentView(layoutId);
		}
	}

	public static boolean CheckKeepAwake(Activity activity) {
		boolean keep = PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(Constants.PREF_KEEP_AWAKE, false);
		if (keep) {
			activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
		return keep;

	}

	/**
	 * Check whether the screen width is less than 600dp
	 * 
	 * @param activity
	 *            target activity
	 * @return true if less than 600dp
	 */
	@SuppressWarnings("deprecation")
	public static boolean IsSmallScreen(Activity activity) {
		DisplayMetrics metrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		Display display = activity.getWindowManager().getDefaultDisplay();
		if (display.getWidth() < (600 * metrics.density)) {
			return true;
		}
		return false;
	}

	@SuppressWarnings("deprecation")
	public static int getScreenHeight(Activity activity) {
		DisplayMetrics metrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		Display display = activity.getWindowManager().getDefaultDisplay();
		return display.getWidth();
	}

	@SuppressLint("NewApi")
	public static void ToggleFullscreen(final Activity activity, boolean fullscreen) {
		if (fullscreen) {
			// Use Immersive Mode for KitKat
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
				// Hide system ui when activity opens
				hideSystemUi(activity);
				final Handler mHideHandler = new Handler();
				final Runnable mHideRunnable = new Runnable() {
					@Override
					public void run() {
						hideSystemUi(activity);
					}
				};
				// Hide system ui bars when not used after 2 seconds
				activity.getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(
						new OnSystemUiVisibilityChangeListener() {
							@Override
							public void onSystemUiVisibilityChange(int visibility) {
								if (visibility == 0) {
									// the navigation bar re-appears, let’s hide it after 2 seconds
									mHideHandler.postDelayed(mHideRunnable, 2000);
								}
							}
						});
			}
			// Hide action bar for supported versions
			else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				ActionBar actionBar = activity.getActionBar();
				if (actionBar != null)
					actionBar.hide();
				activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
				activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			} else {
				activity.requestWindowFeature(Window.FEATURE_NO_TITLE);
				activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
				activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			}
		} else {
			activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
	}

	@SuppressLint("NewApi")
	public static void hideSystemUi(Activity activity) {
		activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
				| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_FULLSCREEN
				| View.SYSTEM_UI_FLAG_IMMERSIVE);
	}

	@SuppressLint("NewApi")
	public static void ToggleActionBar(Activity activity, boolean show) {
		if (!show) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				ActionBar actionBar = activity.getActionBar();
				if (actionBar != null)
					actionBar.hide();
			} else {
				activity.requestWindowFeature(Window.FEATURE_NO_TITLE);
			}
		}
	}

	/**
	 * Toggle the Preferences:Constants.PREF_INVERT_COLOR
	 * 
	 * @param activity
	 *            target activity
	 */
	public static void ToggleColorPref(Activity activity) {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
		SharedPreferences.Editor editor = sharedPrefs.edit();
		if (sharedPrefs.getBoolean(Constants.PREF_INVERT_COLOR, true)) {
			editor.putBoolean(Constants.PREF_INVERT_COLOR, false);
		} else {
			editor.putBoolean(Constants.PREF_INVERT_COLOR, true);
		}
		editor.commit();
	}

	public static int getIntFromPreferences(String key, int defaultValue) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LNReaderApplication.getInstance().getApplicationContext());
		try {
			String value = prefs.getString(key, "");
			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException ex) {
				return defaultValue;
			}
		} catch (ClassCastException cex) {
			return prefs.getInt(key, defaultValue);
		}
	}

	public static float getFloatFromPreferences(String key, float defaultValue) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LNReaderApplication.getInstance().getApplicationContext());
		try {
			String value = prefs.getString(key, "");

			try {
				return Float.parseFloat(value);
			} catch (NumberFormatException ex) {
				return defaultValue;
			}
		} catch (ClassCastException cex) {
			return prefs.getFloat(key, defaultValue);
		}
	}

	/**
	 * Create Yes/No Alert Dialog
	 * 
	 * @param context
	 * @param message
	 * @param caption
	 * @param listener
	 * @return
	 */
	public static AlertDialog createYesNoDialog(Context context, String message, String caption, DialogInterface.OnClickListener listener) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(message);
		builder.setTitle(caption);
		builder.setPositiveButton("Yes", listener);
		builder.setNegativeButton("No", listener);
		return builder.create();
	}

	/**
	 * Change the color of image in ImageView, works nicely with single coloured images.
	 * 
	 * @param targetIv
	 */
	public static ImageView setColorFilter(ImageView targetIv) {
		if (PreferenceManager.getDefaultSharedPreferences(targetIv.getContext()).getBoolean(Constants.PREF_INVERT_COLOR, true)) {
			targetIv.setColorFilter(Constants.COLOR_UNREAD);
		} else {
			targetIv.setColorFilter(Constants.COLOR_UNREAD_DARK);
		}
		return targetIv;
	}

	public static Drawable setColorFilter(Drawable targetIv) {
		if (PreferenceManager.getDefaultSharedPreferences(LNReaderApplication.getInstance().getApplicationContext()).getBoolean(Constants.PREF_INVERT_COLOR, true)) {
			targetIv.setColorFilter(Constants.COLOR_UNREAD, Mode.SRC_ATOP);
		} else {
			targetIv.setColorFilter(Constants.COLOR_UNREAD_DARK, Mode.SRC_ATOP);
		}
		return targetIv;
	}

	public static void setLanguage(Context activity, String key) {
		try {
			/* Changing configuration to user's choice */
			Locale myLocale = new Locale(key);
			Log.d(TAG, "Locale: " + key);
			Resources res = activity.getResources();
			DisplayMetrics dm = res.getDisplayMetrics();
			Configuration conf = res.getConfiguration();
			conf.locale = myLocale;
			/* update resources */
			res.updateConfiguration(conf, dm);
		} catch (Exception ex) {
			Log.e(TAG, "Failed to set language: " + key, ex);
			setLanguage(activity, "en");
		}
	}

	public static void setLanguage(Context activity) {
		/* Set starting language */
		String locale = PreferenceManager.getDefaultSharedPreferences(activity).getString(Constants.PREF_LANGUAGE, "en");
		setLanguage(activity, locale);
	}

	public static void setAlternativeLanguagePreferences(Context activity, String lang, boolean val) {
		/* Set Alternative Language Novels preferences */
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);

		// write
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putBoolean(lang, val);
		editor.commit(); // save change
	}

	/**
	 * Get image root path, remove '/' from the last character.
	 * 
	 * @param activity
	 * @return
	 */
	public static String getImageRoot(Context activity) {
		String loc = PreferenceManager.getDefaultSharedPreferences(activity).getString(Constants.PREF_IMAGE_SAVE_LOC, "");
		if (Util.isStringNullOrEmpty(loc)) {
			Log.w(TAG, "Empty Path, use default path for image storage.");
			loc = Environment.getExternalStorageDirectory().getAbsolutePath().toString() + "/Android/data/" + Constants.class.getPackage().getName() + "/files";
		}
		if (loc.endsWith("/"))
			loc = loc.substring(0, loc.length() - 1);
		return loc;
	}

	/**
	 * Return HTTP or HTTPS based on pref.
	 * 
	 * @param activity
	 * @return
	 */
	public static String getBaseUrl(Context activity) {
		boolean useHttps = PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(Constants.PREF_USE_HTTPS, false);
		if (useHttps)
			return Constants.ROOT_HTTPS + Constants.ROOT_URL;
		else
			return Constants.ROOT_HTTP + Constants.ROOT_URL;
	}

	/**
	 * Read raw resources and return it as string
	 * 
	 * @param ctx
	 * @param resourceId
	 * @return
	 */
	public static String readRawStringResources(Context ctx, int resourceId) {
		InputStream in = ctx.getResources().openRawResource(resourceId);
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		String temp = null;

		int i;
		try {
			i = in.read();
			while (i != -1)
			{
				byteArrayOutputStream.write(i);
				i = in.read();
			}
			in.close();
			temp = byteArrayOutputStream.toString();
			byteArrayOutputStream.close();
		} catch (Exception e) {
			Log.e(TAG, "Failed to Read Asset: " + resourceId, e);
		}
		return temp;
	}

	/* PREFERENCES HELPER */
	public static boolean getColorPreferences(Context ctx) {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(Constants.PREF_INVERT_COLOR, true);
	}

	public static boolean getDownloadTouchPreference(Context ctx) {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(Constants.PREF_DOWNLOAD_TOUCH, false);
	}

	public static boolean getStrechCoverPreference(Context ctx) {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(Constants.PREF_STRETCH_COVER, false);
	}

	public static boolean getZoomPreferences(Context ctx) {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(Constants.PREF_ZOOM_ENABLED, false);
	}

	public static boolean getZoomControlPreferences(Context ctx) {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(Constants.PREF_SHOW_ZOOM_CONTROL, false);
	}

	public static boolean getDynamicButtonsPreferences(Context ctx) {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(Constants.PREF_ENABLE_WEBVIEW_BUTTONS, false);
	}

	public static boolean getAllBookmarkOrder(Context ctx) {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(Constants.PREF_BOOKMARK_ORDER, false);
	}

	public static boolean getKitKatWebViewFix(Context ctx) {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(Constants.PREF_KITKAT_WEBVIEW_FIX, false);
	}

	public static boolean getShowExternal(Context ctx) {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(Constants.PREF_SHOW_EXTERNAL, true);
	}

	public static boolean getShowMissing(Context ctx) {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(Constants.PREF_SHOW_MISSING, true);
	}

	public static boolean getShowRedlink(Context ctx) {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(Constants.PREF_SHOW_REDLINK, true);
	}

	public static String getBackupRoot(Context ctx) {
		String loc = PreferenceManager.getDefaultSharedPreferences(ctx).getString(Constants.PREF_BACKUP_LOCATION, "");
		if (Util.isStringNullOrEmpty(loc)) {
			Log.w(TAG, "Empty Path, use default path for backup storage.");
			loc = Environment.getExternalStorageDirectory().getAbsolutePath();
		}
		if (loc.endsWith("/"))
			loc = loc.substring(0, loc.length() - 1);
		return loc;
	}
}
