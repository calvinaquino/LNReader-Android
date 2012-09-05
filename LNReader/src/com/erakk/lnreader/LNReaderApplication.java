package com.erakk.lnreader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;

import com.erakk.lnreader.dao.NovelsDao;

/*
 * http://www.devahead.com/blog/2011/06/extending-the-android-application-class-and-dealing-with-singleton/
 */
public class LNReaderApplication extends Application {
	private NovelsDao novelsDao;
	private static LNReaderApplication instance;
	@Override
	public void onCreate()
	{
		super.onCreate();

		// Initialize the singletons so their instances
		// are bound to the application process.
//		CheckScreenRotation();
		initSingletons();
		instance = this;
	}
	
//	public void CheckScreenRotation()
//	{
//		if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("lock_horizontal", false)) {
//			
//		    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//    	}
//    	else {
//    		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
//    	}
//	}
	
	protected void initSingletons()
	{
		this.novelsDao = NovelsDao.getInstance(getApplicationContext());
	}
	
	public boolean isOnline() {
	    ConnectivityManager cm =
	        (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo netInfo = cm.getActiveNetworkInfo();
	    if (netInfo != null && netInfo.isConnectedOrConnecting()) {
	        return true;
	    }
	    return false;
	}
	
	public static LNReaderApplication getInstance() {
		return instance;
	}
	
	private Hashtable<Integer, String> cssCache = null;
	public String ReadCss(int styleId) {
		if(cssCache == null)
			cssCache = new Hashtable<Integer, String>();
		if(!cssCache.containsKey(styleId)) {
			StringBuilder contents = new StringBuilder();
			InputStream in =  getApplicationContext().getResources().openRawResource(styleId);
			InputStreamReader isr = new InputStreamReader(in);
			BufferedReader buff = new BufferedReader(isr);
			String temp = null;
			try {
				while((temp = buff.readLine()) != null){
					contents.append(temp);
				}
				buff.close();
				isr.close();
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			cssCache.put(styleId, contents.toString());
		}
		return cssCache.get(styleId);
	}	
}
