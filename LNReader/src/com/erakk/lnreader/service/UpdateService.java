package com.erakk.lnreader.service;

import java.util.ArrayList;
import java.util.Date;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.widget.Toast;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.activity.DisplayLightNovelContentActivity;
import com.erakk.lnreader.activity.UpdateHistoryActivity;
import com.erakk.lnreader.callback.CallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.model.PageModel;
import com.erakk.lnreader.model.UpdateInfoModel;
import com.erakk.lnreader.model.UpdateType;
import com.erakk.lnreader.task.GetUpdatedChaptersTask;

public class UpdateService extends Service {
	private final IBinder mBinder = new MyBinder();
	public boolean force = false;
	public final static String TAG = UpdateService.class.toString();
	private static boolean isRunning;
	private ICallbackNotifier notifier;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "onStartCommand");
		execute();
		return Service.START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		Log.d(TAG, "onBind");
		return mBinder;
	}

	@Override
	public void onCreate() {
		// Display a notification about us starting. We put an icon in the status bar.
		Log.d(TAG, "onCreate");
		execute();
	}

	@TargetApi(11)
	public void execute() {
		if (!shouldRun(force)) {
			// Reschedule for next run
			UpdateScheduleReceiver.reschedule(this);
			isRunning = false;
			return;
		}

		if (!isRunning) {
			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
			SharedPreferences.Editor editor = sharedPrefs.edit();
			editor.putString(Constants.PREF_RUN_UPDATES, "Running...");
			editor.putString(Constants.PREF_RUN_UPDATES_STATUS, "");
			editor.commit();

			GetUpdatedChaptersTask task = new GetUpdatedChaptersTask(this, GetAutoDownloadUpdatedChapterPreferences(), notifier);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
				task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			else
				task.execute();

			// add on Download List
			LNReaderApplication.getInstance().addDownload(TAG, "Update Service");
		}
	}

	private boolean GetAutoDownloadUpdatedChapterPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_AUTO_DOWNLOAD_UPDATED_CHAPTER, false);
	}

	public class MyBinder extends Binder {
		public UpdateService getService() {
			Log.d(TAG, "getService");
			return UpdateService.this;
		}
	}

	public void sendNotification(ArrayList<PageModel> updatedChapters) {
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		if (updatedChapters != null && updatedChapters.size() > 0) {
			Log.d(TAG, "sendNotification");

			// remove previous update history.
			// NovelsDao.getInstance(this).deleteAllUpdateHistory();

			// create UpdateInfoModel list
			int updateCount = 0;
			int newCount = 0;
			int newNovel = 0;
			ArrayList<UpdateInfoModel> updatesInfo = new ArrayList<UpdateInfoModel>();
			for (PageModel pageModel : updatedChapters) {
				UpdateInfoModel updateInfo = new UpdateInfoModel();

				if (pageModel.getType().equalsIgnoreCase(PageModel.TYPE_NOVEL)) {
					++newNovel;
					updateInfo.setUpdateTitle("New Novel: " + pageModel.getTitle());
					updateInfo.setUpdateType(UpdateType.NewNovel);
				} else if (pageModel.getType().equalsIgnoreCase(PageModel.TYPE_TOS)) {
					updateInfo.setUpdateTitle("Updated TOS");
					updateInfo.setUpdateType(UpdateType.UpdateTos);
				} else {
					if (pageModel.isUpdated()) {
						updateInfo.setUpdateType(UpdateType.Updated);
						++updateCount;
					} else {
						updateInfo.setUpdateType(UpdateType.New);
						++newCount;
					}

					String novelTitle = "";
					try {
						novelTitle = pageModel.getBook().getParent().getPageModel().getTitle() + ": ";
					} catch (Exception ex) {
						Log.e(TAG, "Error when getting Novel title", ex);
					}

					updateInfo.setUpdateTitle(novelTitle + pageModel.getTitle() + " (" + pageModel.getBook().getTitle() + ")");
				}

				updateInfo.setUpdateDate(pageModel.getLastUpdate());
				updateInfo.setUpdatePage(pageModel.getPage());
				updateInfo.setUpdatePageModel(pageModel);

				// insert to db
				NovelsDao.getInstance(this).insertUpdateHistory(updateInfo);
				updatesInfo.add(updateInfo);
			}

			if (getConsolidateNotificationPref()) {
				createConsolidatedNotification(mNotificationManager, updateCount, newCount, newNovel);
			} else {
				int id = Constants.NOTIFIER_ID;
				boolean first = true;
				for (UpdateInfoModel updateInfoModel : updatesInfo) {
					final int notifId = ++id;
					Log.d(TAG, "set Notification for: " + updateInfoModel.getUpdatePage());
					Notification notification = getNotificationTemplate(first);
					first = false;

					prepareNotification(notifId, updateInfoModel, notification);
					mNotificationManager.notify(notifId, notification);
				}
			}
		}

		updateStatus("OK");
		Toast.makeText(this, "Update Service completed", Toast.LENGTH_SHORT).show();
		LNReaderApplication.getInstance().updateDownload(TAG, 100, "Update Service completed");

		// remove from download list
		LNReaderApplication.getInstance().removeDownload(TAG);
	}

	@SuppressWarnings("deprecation")
	public void createConsolidatedNotification(NotificationManager mNotificationManager, int updateCount, int newCount, int newNovel) {
		Log.d(TAG, "set consolidated Notification");
		Notification notification = getNotificationTemplate(true);
		CharSequence contentTitle = "BakaReader EX Updates";
		String contentText = "Found";
		if (updateCount > 0) {
			contentText += " " + updateCount + " updated chapter(s)";
		}
		if (newCount > 0) {
			if (updateCount > 0)
				contentText += " and ";
			contentText += " " + newCount + " new chapter(s)";
		}
		if (newNovel > 0) {
			if (updateCount > 0 || newCount > 0)
				contentText += " and ";
			contentText += " " + newNovel + " new novel(s)";
		}
		contentText += ".";

		Intent notificationIntent = new Intent(this, UpdateHistoryActivity.class);
		notificationIntent.putExtra(Constants.EXTRA_CALLER_ACTIVITY, UpdateService.class.toString());
		int pendingFlag = PendingIntent.FLAG_CANCEL_CURRENT;
		PendingIntent contentIntent = PendingIntent.getActivity(this, Constants.CONSOLIDATED_NOTIFIER_ID, notificationIntent, pendingFlag);

		notification.setLatestEventInfo(this, contentTitle, contentText, contentIntent);
		mNotificationManager.notify(Constants.CONSOLIDATED_NOTIFIER_ID, notification);
	}

	@SuppressWarnings("deprecation")
	public void prepareNotification(final int notifId, UpdateInfoModel chapter, Notification notification) {
		CharSequence contentTitle = chapter.getUpdateType().toString();
		CharSequence contentText = chapter.getUpdateTitle();

		Intent notificationIntent = new Intent(this, DisplayLightNovelContentActivity.class);
		notificationIntent.putExtra(Constants.EXTRA_PAGE, chapter.getUpdatePage());

		int pendingFlag = PendingIntent.FLAG_CANCEL_CURRENT;
		PendingIntent contentIntent = PendingIntent.getActivity(this, notifId, notificationIntent, pendingFlag);

		notification.setLatestEventInfo(this, contentTitle, contentText, contentIntent);
	}

	@SuppressWarnings("deprecation")
	public Notification getNotificationTemplate(boolean firstNotification) {
		int icon = android.R.drawable.arrow_up_float; // Just a placeholder
		CharSequence tickerText = "New Chapters Update";
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);
		if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_PERSIST_NOTIFICATION, false)) {
			notification.flags = Notification.FLAG_AUTO_CANCEL;
		}

		notification.defaults = 0;
		if (firstNotification) {
			if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_UPDATE_RING, false)) {
				notification.defaults |= Notification.DEFAULT_SOUND;
			}
			if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_UPDATE_VIBRATE, false)) {
				notification.defaults |= Notification.DEFAULT_VIBRATE;
			}
		}
		return notification;
	}

	public void updateStatus(String status) {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = sharedPrefs.edit();
		String date = new Date().toString();
		editor.putString(Constants.PREF_RUN_UPDATES, date);
		editor.putString(Constants.PREF_RUN_UPDATES_STATUS, status);
		editor.commit();
		if (notifier != null)
			notifier.onCallback(new CallbackEventData("Last Run: " + date + "\nStatus: " + status, Constants.PREF_RUN_UPDATES));
	}

	private boolean getConsolidateNotificationPref() {
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_CONSOLIDATE_NOTIFICATION, true);
	}

	@SuppressWarnings("deprecation")
	private boolean shouldRun(boolean forced) {
		if (forced) {
			Log.i(TAG, "Forced run");
			return true;
		}

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		String updatesIntervalStr = preferences.getString(Constants.PREF_UPDATE_INTERVAL, "0");

		if (!updatesIntervalStr.equalsIgnoreCase("0")) {
			long lastUpdate = preferences.getLong(Constants.PREF_LAST_UPDATE, 0);
			Date nowDate = new Date();
			long now = nowDate.getTime();

			if (updatesIntervalStr.equalsIgnoreCase("1")) {
				lastUpdate += 15 * 60 * 1000;
			} else if (updatesIntervalStr.equalsIgnoreCase("2")) {
				lastUpdate += 30 * 60 * 1000;
			} else if (updatesIntervalStr.equalsIgnoreCase("3")) {
				lastUpdate += 60 * 60 * 1000;
			} else if (updatesIntervalStr.equalsIgnoreCase("4")) {
				lastUpdate += 12 * 60 * 60 * 1000;
			} else if (updatesIntervalStr.equalsIgnoreCase("5")) {
				lastUpdate += 24 * 60 * 60 * 1000;
			}

			Date lastUpdateDate = new Date(lastUpdate);
			if (lastUpdate <= now) {
				Log.e(TAG, "Updating: " + lastUpdateDate.toLocaleString() + " <= " + nowDate.toLocaleString());
				return true;
			}

			Log.i(TAG, "Next Update: " + lastUpdateDate.toLocaleString() + ", Now: " + nowDate.toLocaleString());
			return false;
		} else {
			Log.i(TAG, "Update Interval set to Never.");
			return false;
		}
	}

	public void setRunning(boolean isRunning) {
		UpdateService.isRunning = isRunning;
	}

	public void setForce(boolean isForced) {
		this.force = isForced;
	}

	public boolean isForced() {
		return force;
	}

	public void setOnCallbackNotifier(ICallbackNotifier notifier) {
		this.notifier = notifier;
	}
}
