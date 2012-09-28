package com.erakk.lnreader.service;

import java.util.Calendar;

import com.erakk.lnreader.Constants;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class MyScheduleReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		reschedule(context);
	}

	public static void reschedule(Context context) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		String updatesIntervalStr = preferences.getString(Constants.PREF_UPDATE_INTERVAL, "0");
		int updatesInterval = Integer.parseInt(updatesIntervalStr);	
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
			case 4:
				repeatTime = AlarmManager.INTERVAL_HALF_DAY;
				break;
			case 5:
				repeatTime = AlarmManager.INTERVAL_DAY;
				break;
			default:
				break;
		}
		
		AlarmManager service = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(context, MyStartServiceReceiver.class);
		PendingIntent pending = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
		
		if(repeatTime > 0) {
			Log.d(UpdateService.TAG, "Setting up schedule");
			
			// Start repeatTime seconds after boot completed
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.SECOND, 60);
	
			// InexactRepeating allows Android to optimize the energy consumption
			Log.d(UpdateService.TAG, "Repeating in: " + repeatTime);
			service.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), repeatTime, pending);
		}
		else {
			Log.d(UpdateService.TAG, "Canceling Schedule");
			service.cancel(pending);
		}
	}
} 