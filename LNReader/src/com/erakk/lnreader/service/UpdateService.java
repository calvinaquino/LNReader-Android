package com.erakk.lnreader.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.erakk.lnreader.activity.MainActivity;

public class UpdateService extends Service {
	private final IBinder mBinder = new MyBinder();

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d("DERVICE", "onStartCommand");
	    return Service.START_NOT_STICKY;
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		Log.d("DERVICE", "onBind");
	    return mBinder;
	}
	
	@Override
    public void onCreate() {
        // Display a notification about us starting.  We put an icon in the status bar.
		Log.d("DERVICE", "onCreate");
        sendNotification();
    }
	
	public class MyBinder extends Binder {
	    public UpdateService getService() {

			Log.d("DERVICE", "getService");
	    	return UpdateService.this;
	    }
	}
	
	@SuppressWarnings("deprecation")
	public void sendNotification() {
		Log.d("DERVICE", "sendNotification");
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
		
		int icon = android.R.drawable.arrow_up_float; //Just a placeholder
		CharSequence tickerText = "Hello";
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);
		
		Context context = getApplicationContext();
		CharSequence contentTitle = "Notification Title";
		CharSequence contentText = "Update test for LNReader";
		Intent notificationIntent = new Intent(this, MainActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
		
		final int NOTIF_ID = 1;

		mNotificationManager.notify(NOTIF_ID, notification);
	}

} 
