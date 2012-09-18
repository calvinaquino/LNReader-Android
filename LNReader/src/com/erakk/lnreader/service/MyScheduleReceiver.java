package com.erakk.lnreader.service;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class MyScheduleReceiver extends BroadcastReceiver {

  // Restart service every 30 seconds
	//private static final long REPEAT_TIME = 1000 * 30;
	@Override
	public void onReceive(Context context, Intent intent) {
		
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		int updatesInterval = preferences.getInt("updates_interval", 0);	
		long repeatTime = 0;
		switch (updatesInterval) {
		case 1:
			repeatTime = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
			break;
		case 2:
			repeatTime = AlarmManager.INTERVAL_HALF_HOUR;
			break;
		case 3:
			repeatTime = AlarmManager.INTERVAL_HOUR;
			break;
		case 41:
			repeatTime = AlarmManager.INTERVAL_HALF_DAY;
			break;
		case 5:
			repeatTime = AlarmManager.INTERVAL_DAY;
			break;
		default:
			break;
		}
		
		Log.d("DERVICE", "onReceive_Schedule");
		AlarmManager service = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(context, MyStartServiceReceiver.class);
		PendingIntent pending = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
		Calendar cal = Calendar.getInstance();
		// Start 10 seconds after boot completed
		cal.add(Calendar.SECOND, 30);
		//
		// Fetch every 30 seconds
		// InexactRepeating allows Android to optimize the energy consumption
		if (repeatTime != 0)
			service.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), repeatTime, pending);
//  	  service.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), REPEAT_TIME, pending);
	}
} 