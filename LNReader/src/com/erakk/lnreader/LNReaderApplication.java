package com.erakk.lnreader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Hashtable;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.service.UpdateService;

/*
 * http://www.devahead.com/blog/2011/06/extending-the-android-application-class-and-dealing-with-singleton/
 */
public class LNReaderApplication extends Application {
	private NovelsDao novelsDao = null;
	private UpdateService service = null;
	private static LNReaderApplication instance;
	
	@Override
	public void onCreate()
	{
		super.onCreate();

		// Initialize the singletons so their instances
		// are bound to the application process.
		initSingletons();
		instance = this;
		
		doBindService();
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
	
	private ServiceConnection mConnection = new ServiceConnection() {

	    public void onServiceConnected(ComponentName className, IBinder binder) {
	    	service = ((UpdateService.MyBinder) binder).getService();
			Log.d(UpdateService.TAG, "onServiceConnected");
	      	Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
	    }

	    public void onServiceDisconnected(ComponentName className) {
	    	service = null;
			Log.d(UpdateService.TAG, "onServiceDisconnected");
	   	}
	};
	
	private void doBindService() {
	    bindService(new Intent(this, UpdateService.class), mConnection, Context.BIND_AUTO_CREATE);
		Log.d(UpdateService.TAG, "doBindService");
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
	
	@Override
	public void onLowMemory () {
		unbindService(mConnection);
		super.onLowMemory();		
	}
	
	public void runUpdateService() {
		if(service == null){
			doBindService();
		}
		else
			service.onStartCommand(null, BIND_AUTO_CREATE, (int)(new Date().getTime()/1000));
	}	
}
