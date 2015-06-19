package com.erakk.lnreader;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.erakk.lnreader.callback.IExtendedCallbackNotifier;
import com.erakk.lnreader.model.DownloadModel;
import com.erakk.lnreader.service.AutoBackupService;
import com.erakk.lnreader.service.UpdateService;
import com.erakk.lnreader.task.AsyncTaskResult;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;

/*
 * http://www.devahead.com/blog/2011/06/extending-the-android-application-class-and-dealing-with-singleton/
 */
public class LNReaderApplication extends Application {
    private static final String TAG = LNReaderApplication.class.toString();
    private static UpdateService updateService = null;
    private static AutoBackupService autoBackupService = null;
    private static LNReaderApplication instance;
    private static Hashtable<String, AsyncTask<?, ?, ?>> runningTasks;
    private static ArrayList<DownloadModel> downloadList;

    private static final Object lock = new Object();

    private static IExtendedCallbackNotifier<DownloadModel> downloadNotifier = null;

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialise the singletons so their instances
        // are bound to the application process.
        initSingletons();
        instance = this;

        doBindService();
        doBindAutoBackupService();
        Log.d(TAG, "Application created.");
    }

    public static LNReaderApplication getInstance() {
        return instance;
    }

    protected void initSingletons() {
        if (runningTasks == null)
            runningTasks = new Hashtable<String, AsyncTask<?, ?, ?>>();
        if (downloadList == null)
            downloadList = new ArrayList<DownloadModel>();
    }

    // region AsyncTask listing method
    public AsyncTask<?, ?, ?> getTask(String key) {
        return runningTasks.get(key);
    }

    public boolean addTask(String key, AsyncTask<?, ?, ?> task) {
        synchronized (lock) {
            if (runningTasks.containsKey(key)) {
                AsyncTask<?, ?, ?> tempTask = runningTasks.get(key);
                if (tempTask != null && tempTask.getStatus() != Status.FINISHED)
                    return false;
            }
            runningTasks.put(key, task);
            return true;
        }
    }

    public boolean removeTask(String key) {
        synchronized (lock) {
            if (!runningTasks.containsKey(key))
                return false;
            runningTasks.remove(key);
            return true;
        }
    }

    // endregion

    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        }
        return false;
    }

    // region DownloadActivity method

    public void setDownloadNotifier(IExtendedCallbackNotifier<DownloadModel> notifier) {
        LNReaderApplication.downloadNotifier = notifier;
    }

    public int addDownload(String id, String friendlyName) {
        synchronized (lock) {
            if (isDownloadExists(id))
                return -1;

            downloadList.add(new DownloadModel(id, friendlyName, 0));

            if (downloadNotifier != null)
                downloadNotifier.onCompleteCallback(null, null);

            return downloadList.size();
        }
    }

    public void removeDownload(String id) {
        synchronized (lock) {
            for (int i = 0; i < downloadList.size(); i++) {
                if (downloadList.get(i).getDownloadId().equalsIgnoreCase(id)) {
                    downloadList.remove(i);
                    break;
                }
            }
        }
        if (downloadNotifier != null)
            downloadNotifier.onCompleteCallback(null, null);
    }

    public String getDownloadName(String id) {
        for (final DownloadModel tempModel : downloadList) {
            if (tempModel.getDownloadId().equalsIgnoreCase(id)) {
                return tempModel.getDownloadName();
            }
        }
        return "N/A";
    }

    public boolean isDownloadExists(String id) {
        synchronized (lock) {
            for (final DownloadModel tempModel : downloadList) {
                if (tempModel.getDownloadId().equalsIgnoreCase(id)) {
                    return true;
                }
            }
            return false;
        }
    }

    public ArrayList<DownloadModel> getDownloadList() {
        return downloadList;
    }

    public void updateDownload(String id, Integer progress, String message) {

		/*
         * Although this may seem an attempt at a fake incremental download bar
		 * its actually a progressbar smoother.
		 */

        for (final DownloadModel tempModel : downloadList) {
            if (tempModel.getDownloadId().equalsIgnoreCase(id)) {
                int smoothTime = 1000;
                int tickTime = 25;

                // Download status message
                tempModel.setDownloadMessage(message);
                if (downloadNotifier != null)
                    downloadNotifier.onCompleteCallback(null, null);

                Integer oldProgress = tempModel.getDownloadProgress();
                int tempIncrease = (progress - oldProgress);
                if (tempIncrease < smoothTime / tickTime) {
                    smoothTime = tickTime * tempIncrease;
                    tempIncrease = 1;
                } else
                    tempIncrease /= (smoothTime / tickTime);

                final Integer Increment = tempIncrease;
                new CountDownTimer(smoothTime, tickTime) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        if (tempModel != null) {
                            tempModel.setDownloadProgress(tempModel.getDownloadProgress() + Increment);
                        }
                        if (downloadNotifier != null)
                            downloadNotifier.onCompleteCallback(null, null);
                    }

                    @Override
                    public void onFinish() {
                    }
                }.start();
                return;
            }
        }
    }

    // endregion

    // region UpdateService method

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            updateService = ((UpdateService.MyBinder) binder).getService();
            Log.d(UpdateService.TAG, "onServiceConnected");
            Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            updateService = null;
            Log.d(UpdateService.TAG, "onServiceDisconnected");
        }
    };

    private void doBindService() {
        bindService(new Intent(this, UpdateService.class), mConnection, Context.BIND_AUTO_CREATE);
        Log.d(UpdateService.TAG, "doBindService");
    }

    public void setUpdateServiceListener(IExtendedCallbackNotifier<AsyncTaskResult<?>> notifier) {
        if (updateService != null) {
            updateService.setOnCallbackNotifier(notifier);
        }
    }

    public void runUpdateService(boolean force, IExtendedCallbackNotifier<AsyncTaskResult<?>> notifier) {
        if (updateService == null)
            doBindService();
        updateService.force = force;
        updateService.setOnCallbackNotifier(notifier);
        updateService.onStartCommand(null, BIND_AUTO_CREATE, (int) (new Date().getTime() / 1000));
    }

    public void cancelUpdateService() {
        if (updateService != null)
            updateService.cancelUpdate();
    }

    // endregion

    // region CSS caching method.
    // Also used for caching javascript.

    private Hashtable<Integer, String> cssCache = null;

    public String ReadCss(int styleId) {
        if (cssCache == null)
            cssCache = new Hashtable<Integer, String>();
        if (!cssCache.containsKey(styleId)) {
            cssCache.put(styleId, UIHelper.readRawStringResources(getApplicationContext(), styleId));
        }
        return cssCache.get(styleId);
    }

    // endregion

    public void resetFirstRun() {
        SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(this).edit();
        edit.remove(Constants.PREF_FIRST_RUN);
        edit.commit();
    }

    public void restartApplication() {
        Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    // region AutoBackup Service method
    private ServiceConnection mConnection2 = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            autoBackupService = ((AutoBackupService.AutoBackupServiceBinder) binder).getService();
            Log.d(AutoBackupService.TAG, "onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            autoBackupService = null;
            Log.d(AutoBackupService.TAG, "onServiceDisconnected");
        }
    };

    private void doBindAutoBackupService() {
        bindService(new Intent(this, AutoBackupService.class), mConnection2, Context.BIND_AUTO_CREATE);
        Log.d(AutoBackupService.TAG, "doBindService");
    }

    public void setAutoBackupServiceListener(IExtendedCallbackNotifier<AsyncTaskResult<?>> notifier) {
        if (autoBackupService != null) {
            autoBackupService.setOnCallbackNotifier(notifier);
        }
    }

    public void runAutoBackupService(IExtendedCallbackNotifier<AsyncTaskResult<?>> notifier) {
        if (autoBackupService == null)
            doBindAutoBackupService();
        autoBackupService.setOnCallbackNotifier(notifier);
        autoBackupService.onStartCommand(null, BIND_AUTO_CREATE, (int) (new Date().getTime() / 1000));
    }
    // endregion

    // region service helper

    /**
     * http://stackoverflow.com/a/5921190
     *
     * @param serviceClass
     * @return
     */
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onLowMemory() {

		/*
         * java.lang.IllegalArgumentException
		 * in android.app.LoadedApk.forgetServiceDispatcher
		 *
		 * probable crash: updateService is not checked if it exists after onLowMemory.
		 * Technically fixed. needs checking.
		 */
        if (mConnection != null && isMyServiceRunning(UpdateService.class)) {
            Log.w(TAG, "Low Memory, Trying to unbind updateService...");
            try {
                unbindService(mConnection);
                mConnection = null;
                Log.i(TAG, "Unbind updateService done.");
            } catch (Exception ex) {
                Log.e(TAG, "Failed to unbind.", ex);
            }
        }
        if (mConnection2 != null && isMyServiceRunning(AutoBackupService.class)) {
            Log.w(TAG, "Low Memory, Trying to unbind autoBackupService...");
            try {
                unbindService(mConnection2);
                mConnection2 = null;
                Log.i(TAG, "Unbind autoBackupService done.");
            } catch (Exception ex) {
                Log.e(TAG, "Failed to unbind.", ex);
            }
        }
        super.onLowMemory();
    }

    // endregion
}
