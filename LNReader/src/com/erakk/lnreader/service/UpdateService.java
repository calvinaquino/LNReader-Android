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
	
	@SuppressWarnings("deprecation")
	public void sendNotification(ArrayList<PageModel> updatedChapters) {
		if(updatedChapters != null) {		
			Log.d(TAG, "sendNotification");
			String ns = Context.NOTIFICATION_SERVICE;
			NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
			
			int icon = android.R.drawable.arrow_up_float; //Just a placeholder
			CharSequence tickerText = "New Chapters Update";
			long when = System.currentTimeMillis();		
		
			int id = Constants.NOTIFIER_ID;
			for(Iterator<PageModel> iChapter = updatedChapters.iterator(); iChapter.hasNext();) {
				final int notifId = ++id;
				PageModel chapter = iChapter.next();
				
				Notification notification = new Notification(icon, tickerText, when);
				notification.flags = Notification.FLAG_AUTO_CANCEL;
				notification.defaults = Notification.DEFAULT_ALL;
				notification.audioStreamType =  Notification.STREAM_DEFAULT; 
				
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
				PendingIntent contentIntent = PendingIntent.getActivity(this, notifId, notificationIntent, Intent.FLAG_ACTIVITY_MULTIPLE_TASK|PendingIntent.FLAG_CANCEL_CURRENT|PendingIntent.FLAG_ONE_SHOT );
		
				notification.setLatestEventInfo(getApplicationContext(), contentTitle, contentText, contentIntent);
				
				mNotificationManager.notify(notifId, notification);
			}		
		}
		updateStatus("OK");
    	Toast.makeText(getApplicationContext(), "Update Service completed", Toast.LENGTH_SHORT).show();
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
							ArrayList<PageModel> updatedNovelDetailsChapters = updatedNovelDetails.getFlattedChapterList();
							
							updates = updatedNovelDetailsChapters;
							// compare the chapters!
							for(int i = 0 ; i < novelDetailsChapters.size() ; ++i) {
								PageModel oldChapter = novelDetailsChapters.get(i);
								//if(callback != null) callback.onCallback(new CallbackEventData("Checking: " + oldChapter.getTitle()));
								for(int j = 0; j < updatedNovelDetailsChapters.size(); j++) {								
									PageModel newChapter = updatedNovelDetailsChapters.get(j);
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
