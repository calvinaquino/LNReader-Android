package com.erakk.lnreader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.preference.PreferenceManager;

/*
 * Class for handling all the UI with API Warning ==> @SuppressLint("NewApi")
 */
public class UIHelper {

	private static void CheckScreenRotation(Activity activity)
	{
		if(PreferenceManager.getDefaultSharedPreferences(LNReaderApplication.getInstance().getApplicationContext()).getBoolean("lock_horizontal", false)) {
			activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    	}
    	else {
    		activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
    	}
	}
		
	@SuppressLint("NewApi")
	public static void SetActionBarDisplayHomeAsUp(Activity activity, boolean enable) {
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB )
			activity.getActionBar().setDisplayHomeAsUpEnabled(enable);
		CheckScreenRotation(activity);
	}
	
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
	
	public static void SetTheme(Activity activity, Integer layoutId) {
		// set before create any view
    	if(PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("invert_colors", false)) {    		
    		activity.setTheme(R.style.AppTheme2);
    	}
    	else {
    		activity.setTheme(R.style.AppTheme);
    	}
    	if(layoutId != null) {
    		activity.setContentView(layoutId);
    	}    	
	}
}
