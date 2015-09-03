package com.erakk.lnreader.service;

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
import com.erakk.lnreader.R;
import com.erakk.lnreader.UI.activity.DisplayLightNovelContentActivity;
import com.erakk.lnreader.UI.activity.MainActivity;
import com.erakk.lnreader.UI.fragment.UpdateInfoFragment;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.callback.CallbackEventData;
import com.erakk.lnreader.callback.IExtendedCallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.Util;
import com.erakk.lnreader.model.PageModel;
import com.erakk.lnreader.model.UpdateInfoModel;
import com.erakk.lnreader.model.UpdateTypeEnum;
import com.erakk.lnreader.task.AsyncTaskResult;
import com.erakk.lnreader.task.GetUpdatedChaptersTask;

import java.util.ArrayList;
import java.util.Date;

public class UpdateService extends Service {
    private final IBinder mBinder = new MyBinder();
    public boolean force = false;
    public final static String TAG = UpdateService.class.toString();
    private static boolean isRunning;
    private IExtendedCallbackNotifier<AsyncTaskResult<?>> notifier;
    private GetUpdatedChaptersTask task;

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

            task = new GetUpdatedChaptersTask(this, GetAutoDownloadUpdatedChapterPreferences(), notifier);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            else
                task.execute();
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
                    updateInfo.setUpdateType(UpdateTypeEnum.NewNovel);
                } else if (pageModel.getType().equalsIgnoreCase(PageModel.TYPE_TOS)) {
                    updateInfo.setUpdateTitle("Updated TOS");
                    updateInfo.setUpdateType(UpdateTypeEnum.UpdateTos);
                } else {
                    if (pageModel.isUpdated()) {
                        updateInfo.setUpdateType(UpdateTypeEnum.Updated);
                        ++updateCount;
                    } else {
                        updateInfo.setUpdateType(UpdateTypeEnum.New);
                        ++newCount;
                    }

                    String novelTitle = "";
                    try {
                        novelTitle = pageModel.getBook(true).getParent().getPageModel().getTitle() + ": ";
                    } catch (Exception ex) {
                        Log.e(TAG, "Error when getting Novel title", ex);
                    }
                    novelTitle = novelTitle + pageModel.getTitle() + " (" + pageModel.getBook(true).getTitle() + ")";
                    if (pageModel.isExternal()) {
                        // double check
                        if (pageModel.getPage().startsWith("http://") || pageModel.getPage().startsWith("https://")) {
                            novelTitle += " - EXTERNAL LINK";
                            updateInfo.setExternal(true);
                        }
                    }
                    updateInfo.setUpdateTitle(novelTitle);
                }

                updateInfo.setUpdateDate(pageModel.getLastUpdate());
                updateInfo.setUpdatePage(pageModel.getPage());
                updateInfo.setUpdatePageModel(pageModel);

                // insert to db
                NovelsDao.getInstance().insertUpdateHistory(updateInfo);
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
        LNReaderApplication.getInstance().updateDownload(TAG, 100, getString(R.string.svc_update_complete));
        if (notifier != null)
            notifier.onProgressCallback(new CallbackEventData(getString(R.string.svc_update_complete), 100, Constants.PREF_RUN_UPDATES));
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

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.putExtra(Constants.EXTRA_INITIAL_FRAGMENT, UpdateInfoFragment.class.toString());
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
            notifier.onProgressCallback(new CallbackEventData(getString(R.string.svc_update_status, date, status), Constants.PREF_RUN_UPDATES));
    }

    private boolean getConsolidateNotificationPref() {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_CONSOLIDATE_NOTIFICATION, true);
    }

    @SuppressWarnings("deprecation")
    private boolean shouldRun(boolean forced) {
        if (forced) {
            Log.i(TAG, "Forced run");
            return true;
        } else {
            // check wifi only preferences
            if (UIHelper.isAutoUpdateOnlyUseWifi(this) && !Util.isWifiConnected()) {
                Log.i(TAG, "Wifi is not connected!");
                return false;
            } else {
                Log.i(TAG, "Wifi available.");
            }

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            String updatesIntervalStr = preferences.getString(Constants.PREF_UPDATE_INTERVAL, "0");

            if (!updatesIntervalStr.equalsIgnoreCase("0")) {
                long lastUpdate = preferences.getLong(Constants.PREF_LAST_UPDATE, 0);
                Date nowDate = new Date();
                long now = nowDate.getTime();

                lastUpdate += GetUpdateInterval(updatesIntervalStr);

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
    }

    public static long GetUpdateInterval(String updatesIntervalStr) {
        long interval = 0;
        if (updatesIntervalStr.equalsIgnoreCase("1")) {
            interval = 6 * 60 * 60 * 1000;
        } else if (updatesIntervalStr.equalsIgnoreCase("2")) {
            interval = 12 * 60 * 60 * 1000;
        } else if (updatesIntervalStr.equalsIgnoreCase("3")) {
            interval = 24 * 60 * 60 * 1000;
        } else if (updatesIntervalStr.equalsIgnoreCase("4")) {
            interval = 2 * 24 * 60 * 60 * 1000;
        } else if (updatesIntervalStr.equalsIgnoreCase("5")) {
            interval = 7 * 24 * 60 * 60 * 1000;
        }
        return interval;
    }

    public void cancelUpdate() {
        if (task != null) {
            task.cancel(true);
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

    public void setOnCallbackNotifier(IExtendedCallbackNotifier<AsyncTaskResult<?>> notifier) {
        this.notifier = notifier;
        if (task != null)
            task.setCallbackNotifier(notifier);
    }
}
