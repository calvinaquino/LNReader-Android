package com.erakk.lnreader.service;

import android.R;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.activity.DisplayLightNovelDetailsActivity;

public class UpdateService extends IntentService {

	String ns = Context.NOTIFICATION_SERVICE;
	NotificationManager NotificationManager = (NotificationManager) getSystemService(ns);
	
	public UpdateService() {
		super(Constants.TAG);
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
	    Log.d(Constants.TAG, "onHandleIntent for action: " + intent.getAction());
	    notifyUser();
	    
	}

	@SuppressWarnings({ "deprecation", "unused" })
	private void notifyUser() {
		int icon = R.drawable.arrow_down_float;
		CharSequence tickerText = "Hello";
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);
		
		Context context = getApplicationContext();
		CharSequence contentTitle = "LNReader Notification";
		CharSequence contentText = "Novel Update Test";
		Intent notificationIntent = new Intent(this, DisplayLightNovelDetailsActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
		
		final int HELLO_ID = 1;

		NotificationManager.notify(HELLO_ID, notification);
	}
}
