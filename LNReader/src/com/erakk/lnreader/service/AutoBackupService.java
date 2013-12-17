package com.erakk.lnreader.service;

import java.io.IOException;
import java.util.Date;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.callback.CallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;

public class AutoBackupService extends Service{
	public static final String TAG = AutoBackupService.class.toString();
	private final IBinder mBinder = new AutoBackupServiceBinder();
	private static boolean isRunning;
	private ICallbackNotifier notifier;

	@Override
	public void onCreate() {
		// Display a notification about us starting. We put an icon in the status bar.
		Log.d(TAG, "onCreate");
		execute();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "onStartCommand");
		execute();
		return Service.START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		Log.d(TAG, "AutoBackupService onBind");
		return mBinder;
	}

	private void execute() {
		if(!shouldRun()) {
			return;
		}

		if(!isRunning) {
			AutoBackupService.isRunning = true;

			int backupCount = UIHelper.getIntFromPreferences(Constants.PREF_AUTO_BACKUP_COUNT, 4);
			int nextIndex = UIHelper.getIntFromPreferences(Constants.PREF_LAST_AUTO_BACKUP_INDEX, 0) + 1;
			if(nextIndex > backupCount) nextIndex = 0;

			String backupFilename = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Backup_pages.db." + nextIndex;

			try {
				NovelsDao.getInstance(this).copyDB(true, backupFilename);
			} catch (IOException e) {
				Log.e(TAG, "Failed to auto backup DB", e);
				if(notifier != null)
					notifier.onCallback(new CallbackEventData("Failed to auto backup DB."));
			}

			if(notifier != null)
				notifier.onCallback(new CallbackEventData("Auto Backup to: " + backupFilename));

			// update last backup information
			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
			SharedPreferences.Editor editor = sharedPrefs.edit();
			editor.putInt(Constants.PREF_LAST_AUTO_BACKUP_INDEX, nextIndex);
			editor.putLong(Constants.PREF_LAST_AUTO_BACKUP_TIME, new Date().getTime());
			editor.commit();

			AutoBackupScheduleReceiver.reschedule(this);

			AutoBackupService.isRunning = false;
		}
	}

	private boolean shouldRun() {
		boolean result = false;
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

		boolean isEnabled = preferences.getBoolean(Constants.PREF_AUTO_BACKUP_ENABLED, false);
		if(isEnabled) {
			long lastBackupTime = preferences.getLong(Constants.PREF_LAST_AUTO_BACKUP_TIME, 0);

			// last backup time + 1 day
			Date nextBackupTime = new Date(lastBackupTime + 86400000L);
			if (nextBackupTime.after(new Date())) {
				result = true;
			}
			else {
				AutoBackupScheduleReceiver.reschedule(this);
				result = false;
			}
		}

		return result;
	}

	public boolean isRunning(){
		return AutoBackupService.isRunning;
	}

	public void setOnCallbackNotifier(ICallbackNotifier notifier) {
		this.notifier = notifier;
	}

	public class AutoBackupServiceBinder extends Binder {
		public AutoBackupService getService() {
			Log.d(TAG, "getService");
			return AutoBackupService.this;
		}
	}
}
