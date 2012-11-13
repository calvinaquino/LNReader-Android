package com.erakk.lnreader;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.Display;

/*
 * Class for handling all the UI with API Warning ==> @SuppressLint("NewApi")
 */
public class UIHelper {

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
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB )
			activity.getActionBar().setDisplayHomeAsUpEnabled(enable);
		CheckScreenRotation(activity);
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
			activity.startActivity(activity.getIntent());
			activity.finish();
		}
		CheckScreenRotation(activity);
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
	
	/**
	 * Toggle the Preferences:Constants.PREF_INVERT_COLOR
	 * @param activity target activity
	 */
	public static void ToggleColorPref(Activity activity) { 
    	SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
    	SharedPreferences.Editor editor = sharedPrefs.edit();
    	if (sharedPrefs.getBoolean(Constants.PREF_INVERT_COLOR, false)) {
    		editor.putBoolean(Constants.PREF_INVERT_COLOR, false);
    	}
    	else {
    		editor.putBoolean(Constants.PREF_INVERT_COLOR, true);
    	}
    	editor.commit();
    }
	
	public static String UrlEncode(String param) throws UnsupportedEncodingException {
		if(!param.contains("%")) {
			param = URLEncoder.encode(param, "utf-8");
		}
		return param;
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
}
