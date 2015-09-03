package com.erakk.lnreader.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.callback.CallbackEventData;
import com.erakk.lnreader.callback.IExtendedCallbackNotifier;
import com.erakk.lnreader.task.AsyncTaskResult;
import com.erakk.lnreader.task.CopyDBTask;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

public class AutoBackupService extends Service {
	public static final String TAG = AutoBackupService.class.toString();
	private final IBinder mBinder = new AutoBackupServiceBinder();
	private static boolean isRunning;
	private IExtendedCallbackNotifier<AsyncTaskResult<?>> notifier;
	private CopyDBTask task;

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

	@SuppressLint({ "InlinedApi", "NewApi" })
	private void execute() {
		if (!shouldRun()) {
			return;
		}

		if (!isRunning) {
			AutoBackupService.isRunning = true;
			if (notifier != null)
				notifier.onProgressCallback(new CallbackEventData("Auto Backup is running...", Constants.PREF_AUTO_BACKUP_ENABLED));

			int backupCount = UIHelper.getIntFromPreferences(Constants.PREF_AUTO_BACKUP_COUNT, 4);
			int nextIndex = UIHelper.getIntFromPreferences(Constants.PREF_LAST_AUTO_BACKUP_INDEX, 0) + 1;
			if (nextIndex > backupCount)
				nextIndex = 0;

			String backupFilename = UIHelper.getBackupRoot(this) + "/Backup_pages.db." + nextIndex;

			task = new CopyDBTask(true, notifier, Constants.PREF_BACKUP_DB, backupFilename);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
				task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			else
				task.execute();

			if (notifier != null)
				notifier.onProgressCallback(new CallbackEventData("Auto Backup to: " + backupFilename, Constants.PREF_AUTO_BACKUP_ENABLED));

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
		if (isEnabled) {
			long lastBackupTime = preferences.getLong(Constants.PREF_LAST_AUTO_BACKUP_TIME, 0);

			// last backup time + 1 day
			Date nextBackupTime = new Date(lastBackupTime + 86400000);
			Date currentTime = new Date();
			if (nextBackupTime.before(currentTime)) {
				result = true;
			}
		}
		if (!result)
			AutoBackupScheduleReceiver.reschedule(this);
		return result;
	}

	public boolean isRunning() {
		return AutoBackupService.isRunning;
	}

	public void setOnCallbackNotifier(IExtendedCallbackNotifier<AsyncTaskResult<?>> notifier) {
		this.notifier = notifier;
		if (task != null) {
			task.setCallbackNotifier(notifier);
		}
	}

	public static ArrayList<File> getBackupFiles(Context ctx) {
		ArrayList<File> backups = new ArrayList<File>();
		String rootPath = UIHelper.getBackupRoot(ctx);
		int backupCount = UIHelper.getIntFromPreferences(Constants.PREF_AUTO_BACKUP_COUNT, 4);

		String backupFilename = rootPath + "/Backup_pages.db";
		File f = new File(backupFilename);
		if (f.exists())
			backups.add(f);

		for (int i = 0; i < backupCount; ++i) {
			backupFilename = rootPath + "/Backup_pages.db." + i;
			f = new File(backupFilename);
			if (f.exists())
				backups.add(f);
		}
		Log.i(TAG, "Found backups: " + backups.size());
		return backups;
	}

	public class AutoBackupServiceBinder extends Binder {
		public AutoBackupService getService() {
			Log.d(TAG, "getService");
			return AutoBackupService.this;
		}
	}

}
