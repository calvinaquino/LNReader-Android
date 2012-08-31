package com.erakk.lnreader;

import com.erakk.lnreader.dao.NovelsDao;

import android.app.Application;

/*
 * http://www.devahead.com/blog/2011/06/extending-the-android-application-class-and-dealing-with-singleton/
 */
public class LNReaderApplication extends Application {
	private NovelsDao novelsDao;
	@Override
	public void onCreate()
	{
		super.onCreate();

		// Initialize the singletons so their instances
		// are bound to the application process.
		initSingletons();
	}

	protected void initSingletons()
	{
		novelsDao = NovelsDao.getInstance(getApplicationContext());
	}
}
