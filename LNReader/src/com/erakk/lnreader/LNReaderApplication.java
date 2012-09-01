package com.erakk.lnreader;

import com.erakk.lnreader.dao.NovelsDao;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

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
		initSingletons();
		instance = this;
	}

	protected void initSingletons()
	{
		novelsDao = NovelsDao.getInstance(getApplicationContext());
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
}
