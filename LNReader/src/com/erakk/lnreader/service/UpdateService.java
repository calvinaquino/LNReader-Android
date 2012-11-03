package com.erakk.lnreader.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

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
import com.erakk.lnreader.activity.DisplayLightNovelContentActivity;
import com.erakk.lnreader.callback.CallbackEventData;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.AsyncTaskResult;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.PageModel;

public class UpdateService extends Service {
	private final IBinder mBinder = new MyBinder();
	public boolean force = false;
	public final static String TAG = UpdateService.class.toString();
	private static boolean isRunning;
	public ICallbackNotifier notifier;

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
        // Display a notification about us starting.  We put an icon in the status bar.
		Log.d(TAG, "onCreate");
		execute();
    }
	
	@TargetApi(11)
	public void execute() {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		String updatesIntervalStr = preferences.getString(Constants.PREF_UPDATE_INTERVAL, "0");
		Log.d(TAG, "updatesIntervalStr = " + updatesIntervalStr);
		if(updatesIntervalStr.startsWith("0") && !force) return;
		
		if(!isRunning) {
			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
	    	SharedPreferences.Editor editor = sharedPrefs.edit();
	    	editor.putString(Constants.PREF_RUN_UPDATES, "Running...");
	    	editor.putString(Constants.PREF_RUN_UPDATES_STATUS, "");
	    	editor.commit();
	    	
			GetUpdatedChaptersTask task = new GetUpdatedChaptersTask();
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
				task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			else
				task.execute();
		}
	}
	
	public class MyBinder extends Binder {
	    public UpdateService getService() {
			Log.d(TAG, "getService");
	    	return UpdateService.this;
	    }
	}
	
	public void sendNotification(ArrayList<PageModel> updatedChapters) {
		int id = Constants.NOTIFIER_ID;
		boolean first = true;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		if(updatedChapters != null && updatedChapters.size() > 0) {		
			Log.d(TAG, "sendNotification");
			for(Iterator<PageModel> iChapter = updatedChapters.iterator(); iChapter.hasNext();) {
				final int notifId = ++id;
				PageModel chapter = iChapter.next();
				Log.d(TAG, "set Notification for: " + chapter.getPage());
				Notification notification = getNotificationTemplate(first);
				first = false;
				
				prepareNotification(notifId, chapter, notification);
				
				mNotificationManager.notify(notifId, notification);
			}		
		}
		
//		try {
//			//testing only
//			Notification notification = getNotificationTemplate(true);
//			int notifId = ++id;
//			PageModel testPageModel = new PageModel();
//			testPageModel.setPage("Hyouka:Volume_3_Chapter_2-1");
//			testPageModel = NovelsDao.getInstance().getPageModel(testPageModel, notifier);
//			prepareNotification(notifId, testPageModel, notification);
//			mNotificationManager.notify(notifId, notification);			
//		} catch (Exception e) {
//			Log.e(TAG, "" + e.getMessage(), e);
//		}
//		try {
//			//testing only
//			Notification notification = getNotificationTemplate(false);
//			int notifId = ++id;
//			PageModel testPageModel = new PageModel();
//			testPageModel.setPage("Hyouka:Volume_3_Chapter_2-2");
//			testPageModel = NovelsDao.getInstance().getPageModel(testPageModel, notifier);
//			prepareNotification(notifId, testPageModel, notification);
//			mNotificationManager.notify(notifId, notification);			
//		} catch (Exception e) {
//			Log.e(TAG, "???" + e.getMessage(), e);
//		}
		
		updateStatus("OK");
    	Toast.makeText(getApplicationContext(), "Update Service completed", Toast.LENGTH_SHORT).show();
	}
	
	@SuppressWarnings("deprecation")
	public void prepareNotification(final int notifId, PageModel chapter, Notification notification) {
		CharSequence contentTitle = "New Chapter";
		if(chapter.isUpdated()) contentTitle = "Updated Chapter";

		String novelTitle = "";
		try{
			novelTitle = chapter.getBook().getParent().getPageModel().getTitle() + " ";
		}
		catch(Exception ex){
			Log.e(TAG, "Error when getting Novel title", ex);
		}
						
		CharSequence contentText = novelTitle + chapter.getTitle() + " (" + chapter.getBook().getTitle() + ")";
		
		Intent notificationIntent = new Intent(this, DisplayLightNovelContentActivity.class);
		notificationIntent.putExtra(Constants.EXTRA_PAGE, chapter.getPage());
//		int intentFlag = Intent.FLAG_ACTIVITY_MULTIPLE_TASK
//		   		 	   | Intent.FLAG_ACTIVITY_NEW_TASK;
//		notificationIntent.setFlags(intentFlag);
		
		int pendingFlag = PendingIntent.FLAG_CANCEL_CURRENT;
						//| PendingIntent.FLAG_ONE_SHOT;
		PendingIntent contentIntent = PendingIntent.getActivity(this, notifId, notificationIntent, pendingFlag);

		notification.setLatestEventInfo(getApplicationContext(), contentTitle, contentText, contentIntent);
	}

	@SuppressWarnings("deprecation")
	public Notification getNotificationTemplate(boolean firstNotification) {
		int icon = android.R.drawable.arrow_up_float; //Just a placeholder
		CharSequence tickerText = "New Chapters Update";
		long when = System.currentTimeMillis();	
		
		Notification notification = new Notification(icon, tickerText, when);
		if(!PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_PERSIST_NOTIFICATION, false)) {
			notification.flags = Notification.FLAG_AUTO_CANCEL;	
		}		
		
		notification.defaults = 0;
		if(firstNotification){
			if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_UPDATE_RING, false)) {
				notification.defaults |= Notification.DEFAULT_SOUND;		
			}
			if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_UPDATE_VIBRATE, false)) {
				notification.defaults |= Notification.DEFAULT_VIBRATE;
			}
		}
		return notification;
	}

	private void updateStatus(String status) {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    	SharedPreferences.Editor editor = sharedPrefs.edit();
    	String date = new Date().toString();
    	editor.putString(Constants.PREF_RUN_UPDATES, date);
    	editor.putString(Constants.PREF_RUN_UPDATES_STATUS, status);
    	editor.commit();
    	if(notifier != null) notifier.onCallback(new CallbackEventData("Last Run: " + date + "\nStatus: " + status));
	}

	public class GetUpdatedChaptersTask extends AsyncTask<Void, String, AsyncTaskResult<ArrayList<PageModel>>> implements ICallbackNotifier{
		@Override
		protected AsyncTaskResult<ArrayList<PageModel>> doInBackground(Void... arg0) {
			isRunning = true;
			try{
				ArrayList<PageModel> result = GetUpdatedChapters(this);
				return new AsyncTaskResult<ArrayList<PageModel>>(result);
			}
			catch(Exception ex) {
				Log.e("GetUpdatedChaptersTask", "Error when updating", ex);
				return new AsyncTaskResult<ArrayList<PageModel>>(ex);
			}
		}
		@Override
		protected void onPostExecute(AsyncTaskResult<ArrayList<PageModel>> result) {
			Exception e = result.getError();
			if(e == null) {
				sendNotification(result.getResult());
			}
			else {
				String text = "Error when getting updates: " + e.getMessage();
				Log.e(TAG, text, e);
				updateStatus("ERROR==>" +  e.getMessage());
				Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
			}
			
			// Reschedule for next run
			MyScheduleReceiver.reschedule();
			isRunning = false;
		}
		
		private ArrayList<PageModel> GetUpdatedChapters(ICallbackNotifier callback) throws Exception {
			Log.d(TAG, "Checking Updates...");
			ArrayList<PageModel> updates = new ArrayList<PageModel>();
			NovelsDao dao = NovelsDao.getInstance();
			
			// checking copyrights
			PageModel p = new PageModel();
			p.setPage("Baka-Tsuki:Copyrights");
			p.setTitle("Baka-Tsuki:Copyrights");
			p.setType("Copyrights");
			p = NovelsDao.getInstance().getPageModelFromInternet(p, callback);			
			
			// check only watched novel
			if(callback != null) callback.onCallback(new CallbackEventData("Getting watched novel."));
			ArrayList<PageModel> watchedNovels = dao.getWatchedNovel();
			if(watchedNovels != null){
				for(Iterator<PageModel> iNovels = watchedNovels.iterator(); iNovels.hasNext();){
					// get last update date from internet
					PageModel novel = iNovels.next();
					if(callback != null) callback.onCallback(new CallbackEventData("Checking: " + novel.getTitle()));
					PageModel updatedNovel = dao.getPageModelFromInternet(novel.getPageModel(), callback);
					
					// different timestamp
					if(force || !novel.getLastUpdate().equals(updatedNovel.getLastUpdate())) {
						if(force) {
							Log.i(TAG, "Force Mode: " + novel.getPage());
						}
						else {
							Log.d(TAG, "Different Timestamp for: " + novel.getPage());
							Log.d(TAG, "old: " + novel.getLastUpdate().toString() + " != " + updatedNovel.getLastUpdate().toString());
						}
						ArrayList<PageModel> novelDetailsChapters = dao.getNovelDetails(novel, callback).getFlattedChapterList();
						
						if(callback != null) callback.onCallback(new CallbackEventData("Getting updated chapters: " + novel.getTitle()));
						NovelCollectionModel updatedNovelDetails = dao.getNovelDetailsFromInternet(novel, callback);
						if(updatedNovelDetails!= null){
							updates = updatedNovelDetails.getFlattedChapterList();

							// compare the chapters!
							for(int i = 0 ; i < novelDetailsChapters.size() ; ++i) {
								PageModel oldChapter = novelDetailsChapters.get(i);
								//if(callback != null) callback.onCallback(new CallbackEventData("Checking: " + oldChapter.getTitle()));
								for(int j = 0; j < updates.size(); j++) {								
									PageModel newChapter = updates.get(j);
									if(callback != null) callback.onCallback(new CallbackEventData("Checking: " + oldChapter.getTitle() + " ==> " + newChapter.getTitle()));
									// check if the same page
									if(newChapter.getPage().compareTo(oldChapter.getPage()) == 0) {
										// check if last update date is newer
										//Log.i(TAG, oldChapter.getPage() +  " new: " + newChapter.getLastUpdate().toString() + " old: " + oldChapter.getLastUpdate().toString());
										if(newChapter.getLastUpdate().getTime() != oldChapter.getLastUpdate().getTime()){
											newChapter.setUpdated(true);
											Log.i(TAG, "Found updated chapter: " + newChapter.getTitle());
										}
										else{
											updates.remove(newChapter);
											Log.i(TAG, "No Update for Chapter: " + newChapter.getTitle());
										}											
										break;
									}
								}
							}
						}
					}
				}
				force = false;
			}
			
			Log.i(TAG, "Found updates: " + updates.size());
			
			return updates;
		}

		public void onCallback(ICallbackEventData message) {
			publishProgress(message.getMessage());
		}
		
		@Override
		protected void onProgressUpdate (String... values){
			if(notifier != null) notifier.onCallback(new CallbackEventData(values[0]));
		}
	}
} 
