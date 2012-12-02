package com.erakk.lnreader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.service.UpdateService;

/*
 * http://www.devahead.com/blog/2011/06/extending-the-android-application-class-and-dealing-with-singleton/
 */
public class LNReaderApplication extends Application {
	private static final String TAG = LNReaderApplication.class.toString();
	private static NovelsDao novelsDao = null;
	private static UpdateService service = null;
	private static LNReaderApplication instance;
	private static Hashtable<String, AsyncTask<?, ?, ?>> runningTasks;
	
	@Override
	public void onCreate()
	{
		super.onCreate();

		// Initialise the singletons so their instances
		// are bound to the application process.
		initSingletons();
		instance = this;
		
		doBindService();
		Log.d(TAG, "Application created.");
	}
	
	public static LNReaderApplication getInstance() {
		return instance;
	}
	
	protected void initSingletons()
	{
		if(novelsDao == null) novelsDao = NovelsDao.getInstance(this);
		if(runningTasks == null) runningTasks = new Hashtable<String, AsyncTask<?, ?, ?>>();
	}
	
	/*
	 * AsyncTask listing method
	 */
	public static Hashtable<String, AsyncTask<?, ?, ?>> getTaskList() {
		return runningTasks;
	}
	
	public AsyncTask<?, ?, ?> getTask(String key) {
		return runningTasks.get(key);
	}
	
	public boolean addTask(String key, AsyncTask<?, ?, ?> task) {
		if(runningTasks.containsKey(key)) {
			AsyncTask<?, ?, ?> tempTask = runningTasks.get(key);
			if(tempTask != null && tempTask.getStatus() != Status.FINISHED) return false;
		}
		runningTasks.put(key, task);
		return true;
	}
	
	public boolean removeTask(String key) {
		if(!runningTasks.containsKey(key)) return false;
		runningTasks.remove(key);
		return true;
	}
	
	public boolean isOnline() {
	    ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo netInfo = cm.getActiveNetworkInfo();
	    if (netInfo != null && netInfo.isConnectedOrConnecting()) {
	        return true;
	    }
	    return false;
	}

	/*
	 * UpdateService method
	 */
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
	
	public void setUpdateServiceListener(ICallbackNotifier notifier){
		if(service != null){
			service.notifier = notifier;
		}
	}
	
	public void runUpdateService(boolean force, ICallbackNotifier notifier) {
		if(service == null){
			doBindService();
			service.force = force;
			service.notifier = notifier;
		}
		else
			service.force = force;
			service.notifier = notifier;
			service.onStartCommand(null, BIND_AUTO_CREATE, (int)(new Date().getTime()/1000));
	}
	
	@Override
	public void onLowMemory () {
		Log.w(TAG, "Low Memory, unbind service...");
		unbindService(mConnection);
		super.onLowMemory();		
	}
	
	/*
	 * CSS caching method.
	 * Also used for caching javascript.
	 */
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
				Log.e(TAG, "Error reading asset: " + e.getMessage(), e);
			}
			cssCache.put(styleId, contents.toString());
		}
		return cssCache.get(styleId);
	}
	
	// Helper method
	public static String join(Collection<?> s, String delimiter) {
	     StringBuilder builder = new StringBuilder();
	     Iterator<?> iter = s.iterator();
	     while (iter.hasNext()) {
	         builder.append(iter.next());
	         if (!iter.hasNext()) {
	           break;                  
	         }
	         builder.append(delimiter);
	     }
	     return builder.toString();
	 }
	
	/**
	 * http://stackoverflow.com/questions/6350158/check-arraylist-for-instance-of-object
	 * @param arrayList
	 * @param clazz
	 * @return
	 */
	public static boolean isInstanceOf(Collection<?> arrayList, Class<?> clazz)
	{
	    for(Object o : arrayList)
	    {
	        if (o != null && o.getClass() == clazz)
	        {
	            return true;
	        }
	    }

	    return false;
	}
}
