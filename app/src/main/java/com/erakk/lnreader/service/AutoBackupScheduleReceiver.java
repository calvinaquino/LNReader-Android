package com.erakk.lnreader.service;

import java.util.Calendar;
import java.util.Date;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AutoBackupScheduleReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		reschedule(context);
	}

	public static void reschedule(Context context) {

		long repeatTime = 86400000L;
		if (repeatTime > 0) {
			Log.d(AutoBackupService.TAG, "Setting up schedule");

			AlarmManager service = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			Intent intent = new Intent(context, AutoBackupStartServiceReceiver.class);
			PendingIntent pending = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

			// Start repeatTime seconds after boot completed
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.SECOND, 60);

			Date nextRun = new Date(cal.getTimeInMillis() + repeatTime);
			// InexactRepeating allows Android to optimize the energy consumption
			Log.i(AutoBackupService.TAG, "Repeating in: " + nextRun);
			// service.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), repeatTime, pending);
			service.set(AlarmManager.RTC, nextRun.getTime(), pending);
		}
		else {
			removeSchedule(context);
		}
	}

	public static void removeSchedule(Context context) {
		Log.i(AutoBackupService.TAG, "Canceling Schedule");

		AlarmManager service = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(context, AutoBackupStartServiceReceiver.class);
		PendingIntent pending = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

		service.cancel(pending);
	}
}