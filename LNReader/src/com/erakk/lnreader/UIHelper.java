package com.erakk.lnreader;

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
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

/*
 * Class for handling all the UI with API Warning ==> @SuppressLint("NewApi")
 */
public class UIHelper {

	private static final String TAG = UIHelper.class.toString();

	private static void CheckScreenRotation(Activity activity)
	{
		if(PreferenceManager.getDefaultSharedPreferences(LNReaderApplication.getInstance().getApplicationContext()).getBoolean(Constants.PREF_LOCK_HORIZONTAL, false)) {
			activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    	}
    	else {
    		activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    	}
	}

	/**
	 * Set action bar behaviour, only for API Level 11 and up.
	 * @param activity target activity
	 * @param enable enable up behaviour
	 */
	@SuppressLint("NewApi")
	public static void SetActionBarDisplayHomeAsUp(Activity activity, boolean enable) {
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			ActionBar actionBar = activity.getActionBar();
			if(actionBar != null) actionBar.setDisplayHomeAsUpEnabled(enable);
		}
		CheckScreenRotation(activity);
		CheckKeepAwake(activity);
	}

	/**
	 * Recreate the activity
	 * @param activity target activity
	 */
	@SuppressLint("NewApi")
	public static void Recreate(Activity activity) {
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB )
			activity.recreate();
		else{
			activity.finish();
			activity.startActivity(activity.getIntent());
		}
		CheckScreenRotation(activity);
		CheckKeepAwake(activity);
	}

	/**
	 * Set up the application theme based on Preferences:Constants.PREF_INVERT_COLOR
	 * @param activity target activity
	 * @param layoutId layout to use
	 */
	public static void SetTheme(Activity activity, Integer layoutId) {
    	if(PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(Constants.PREF_INVERT_COLOR, true)) {
    		activity.setTheme(R.style.AppTheme2);
    	}
    	else {
    		activity.setTheme(R.style.AppTheme);
    	}
    	if(layoutId != null) {
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
	 * @param activity target activity
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
	public static void ToggleFullscreen(Activity activity, boolean fullscreen) {
		if(fullscreen) {
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				ActionBar actionBar = activity.getActionBar();
				if(actionBar != null) actionBar.hide();
			}
			else {
				activity.requestWindowFeature(Window.FEATURE_NO_TITLE);
			}

			//activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
			//						  WindowManager.LayoutParams.FLAG_FULLSCREEN);
			activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		}
		else {
			activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
	    }
	}

	@SuppressLint("NewApi")
	public static void ToggleActionBar(Activity activity, boolean show) {
		if(!show) {
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				ActionBar actionBar = activity.getActionBar();
				if(actionBar != null) actionBar.hide();
			}
			else {
				activity.requestWindowFeature(Window.FEATURE_NO_TITLE);
			}
		}
	}

	/**
	 * Toggle the Preferences:Constants.PREF_INVERT_COLOR
	 * @param activity target activity
	 */
	public static void ToggleColorPref(Activity activity) {
    	SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
    	SharedPreferences.Editor editor = sharedPrefs.edit();
    	if (sharedPrefs.getBoolean(Constants.PREF_INVERT_COLOR, true)) {
    		editor.putBoolean(Constants.PREF_INVERT_COLOR, false);
    	}
    	else {
    		editor.putBoolean(Constants.PREF_INVERT_COLOR, true);
    	}
    	editor.commit();
    }

	public static int GetIntFromPreferences(String key, int defaultValue) {
		String value = PreferenceManager.getDefaultSharedPreferences(
				LNReaderApplication.getInstance().getApplicationContext())
				.getString(key, "");
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException ex) {
			return defaultValue;
		}
	}

	/**
	 * Create Yes/No Alert Dialog
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
	 * @param targetIv
	 */
	public static ImageView setColorFilter(ImageView targetIv) {
		if (PreferenceManager.getDefaultSharedPreferences(LNReaderApplication.getInstance().getApplicationContext()).getBoolean(Constants.PREF_INVERT_COLOR, true)) {
			targetIv.setColorFilter(Constants.COLOR_UNREAD);
		}
		else {
			targetIv.setColorFilter(Constants.COLOR_UNREAD_DARK);
		}
		return targetIv;
	}

	public static Drawable setColorFilter(Drawable targetIv) {
		if (PreferenceManager.getDefaultSharedPreferences(LNReaderApplication.getInstance().getApplicationContext()).getBoolean(Constants.PREF_INVERT_COLOR, true)) {
			targetIv.setColorFilter(Constants.COLOR_UNREAD, Mode.SRC_ATOP);
		}
		else {
			targetIv.setColorFilter(Constants.COLOR_UNREAD_DARK, Mode.SRC_ATOP);
		}
		return targetIv;
	}

	public static void setLanguage(Context activity, int langIdx) {
		String lang = "en";

		/* Add system locale / your values folder name here */
		if      (langIdx == 0) lang = "en";
		else if (langIdx == 1) lang = "in";

		/* Changing configuration to user's choice */
		Locale myLocale = new Locale(lang);
		Log.d(TAG, "Locale: " + lang);
		Resources res = activity.getResources();
		DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = myLocale;
        /* update resources */
        res.updateConfiguration(conf, dm);
	}

	public static void setLanguage(Context activity) {
		/* Set starting language */
		int langIdx = GetIntFromPreferences(Constants.PREF_LANGUAGE, 0);
		setLanguage(activity, langIdx);
	}
}
