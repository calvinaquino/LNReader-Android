package com.erakk.lnreader.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.erakk.lnreader.Constants;

public class ReaderBootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i(Constants.TAG, "ReaderBootReceiver invoked, configuring AlarmManager");
		AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(context, ReaderBootReceiver.class), 0);
		
		int PrefInterval = PreferenceManager.getDefaultSharedPreferences(context).getInt("updates_interval", 0);
		long TimeInterval = 0;
		long TriggerAt = SystemClock.elapsedRealtime() + 30000;
		
		switch (PrefInterval) {
		case '1':
			TimeInterval = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
			break;
		case '2':
			TimeInterval = AlarmManager.INTERVAL_HALF_HOUR;
			break;
		case '3':
			TimeInterval = AlarmManager.INTERVAL_HOUR;
			break;
		case '4':
			TimeInterval = AlarmManager.INTERVAL_HALF_DAY;
			break;
		case '5':
			TimeInterval = AlarmManager.INTERVAL_DAY;
			break;
		default:
			break;
		}
		// use inexact repeating which is easier on battery (system can phase events and not wake at exact times)
		alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, TriggerAt, TimeInterval, pendingIntent); 
	}
}