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
import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.activity.DisplayLightNovelContentActivity;
import com.erakk.lnreader.activity.UpdateHistoryActivity;
import com.erakk.lnreader.callback.CallbackEventData;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.AsyncTaskResult;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.PageModel;
import com.erakk.lnreader.model.UpdateInfoModel;
import com.erakk.lnreader.model.UpdateType;

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
			
			// add on Download List
			LNReaderApplication.getInstance().addDownload(TAG, "Update Service");
		}
	}
	
	public class MyBinder extends Binder {
	    public UpdateService getService() {
			Log.d(TAG, "getService");
	    	return UpdateService.this;
	    }
	}
	
	public void sendNotification(ArrayList<PageModel> updatedChapters) {
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		if(updatedChapters != null && updatedChapters.size() > 0) {
			Log.d(TAG, "sendNotification");
			
			// remove previous update history.
			NovelsDao.getInstance(this).deleteAllUpdateHistory();
			
			// create UpdateInfoModel list
			int updateCount = 0;
			int newCount = 0;
			int newNovel = 0;
			ArrayList<UpdateInfoModel> updatesInfo = new ArrayList<UpdateInfoModel>();
			for (PageModel pageModel : updatedChapters) {
				UpdateInfoModel updateInfo = new UpdateInfoModel();
				
				if(pageModel.getType().equalsIgnoreCase(PageModel.TYPE_NOVEL)) {
					++newNovel;
					updateInfo.setUpdateTitle("New Novel: " + pageModel.getTitle());
					updateInfo.setUpdateType(UpdateType.NewNovel);
				}
				else if(pageModel.getType().equalsIgnoreCase(PageModel.TYPE_TOS)) {
					updateInfo.setUpdateTitle("Updated TOS");
					updateInfo.setUpdateType(UpdateType.UpdateTos);
				}
				else {
					if(pageModel.isUpdated()) {
						updateInfo.setUpdateType(UpdateType.Updated);
						++updateCount;
					}
					else {
						updateInfo.setUpdateType(UpdateType.New);
						++newCount;
					}

					String novelTitle = "";
					try{
						novelTitle = pageModel.getBook().getParent().getPageModel().getTitle() + ": ";
					}
					catch(Exception ex){
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
			
			if(getConsolidateNotificationPref()) {
				createConsolidatedNotification(mNotificationManager, updateCount, newCount, newNovel);
			}
			else {
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
    	Toast.makeText(getApplicationContext(), "Update Service completed", Toast.LENGTH_SHORT).show();
    	LNReaderApplication.getInstance().updateDownload(TAG, 100, "Update Service completed");
	}

	@SuppressWarnings("deprecation")
	public void createConsolidatedNotification(NotificationManager mNotificationManager, int updateCount, int newCount, int newNovel) {
		Log.d(TAG, "set consolidated Notification");
		Notification notification = getNotificationTemplate(true);
		CharSequence contentTitle = "BakaReader EX Updates";
		String contentText = "Found";
		if(updateCount > 0) {
			contentText += " " + updateCount + " updated chapter(s)";
		}
		if(newCount > 0) {
			if(updateCount > 0) contentText += " and ";
			contentText += " " + newCount + " new chapter(s)";
		}
		if(newNovel > 0) {
			if(updateCount > 0 || newCount > 0) contentText += " and ";
			contentText += " " + newNovel + " new novel(s)";
		}				
		contentText += ".";
		
		Intent notificationIntent = new Intent(this, UpdateHistoryActivity.class);
		int pendingFlag = PendingIntent.FLAG_CANCEL_CURRENT;
		PendingIntent contentIntent = PendingIntent.getActivity(this, Constants.CONSOLIDATED_NOTIFIER_ID, notificationIntent, pendingFlag);

		notification.setLatestEventInfo(getApplicationContext(), contentTitle, contentText, contentIntent);
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
		private int lastProgress;
		
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
			
			// remove from download list
			LNReaderApplication.getInstance().removeDownload(TAG);
		}
		
		private ArrayList<PageModel> GetUpdatedChapters(ICallbackNotifier callback) throws Exception {
			Log.d(TAG, "Checking Updates...");
			ArrayList<PageModel> updatesTotal = new ArrayList<PageModel>();
			NovelsDao dao = NovelsDao.getInstance();
			
			PageModel updatedTos = getUpdatedTOS(callback);
			if(updatedTos != null) {
				updatesTotal.add(updatedTos);
			}
			
			// check updated novel list
			ArrayList<PageModel> updatedNovelList = getUpdatedNovelList(callback);
			if(updatedNovelList != null && updatedNovelList.size() > 0) {
				Log.d(TAG, "Got new novel! ");
				for (PageModel pageModel : updatedNovelList) {
					updatesTotal.add(pageModel);
				}
			}

			// check only watched novel
			if(callback != null) callback.onCallback(new CallbackEventData("Getting watched novel."));
			ArrayList<PageModel> watchedNovels = dao.getWatchedNovel();
			if(watchedNovels != null){
				double total = watchedNovels.size() + 1;
				double current = 0;
				for(Iterator<PageModel> iNovels = watchedNovels.iterator(); iNovels.hasNext();) {
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
							ArrayList<PageModel> updates = updatedNovelDetails.getFlattedChapterList();
							
							Log.d(TAG, "Starting size: " + updates.size());
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
											//updates.remove(j);
											//--j;
											Log.i(TAG, "No Update for Chapter: " + newChapter.getTitle());
										}											
										break;
									}
								}								
							}
							Log.d(TAG, "End size: " + updates.size());							
							updatesTotal.addAll(updates);
						}
					}
					lastProgress = (int) (++current / total * 100);
					Log.d(TAG, "Progress: " + lastProgress);
				}				
				force = false;
			}
			
			Log.i(TAG, "Found updates: " + updatesTotal.size());
			
			return updatesTotal;
		}
		
		private ArrayList<PageModel> getUpdatedNovelList(ICallbackNotifier callback) throws Exception {
			ArrayList<PageModel> newList = null;
			
			PageModel mainPage = new PageModel();
			mainPage.setPage("Main_Page");
			
			mainPage = NovelsDao.getInstance().getPageModel(mainPage, callback);

			// check if more than 7 day
			Date today = new Date();
			long diff = today.getTime() - mainPage.getLastCheck().getTime();
			if (force || diff > (Constants.CHECK_INTERVAL * 24 * 3600 * 1000) && LNReaderApplication.getInstance().isOnline()) {
				Log.d(TAG, "Last check is over 7 days, checking online status");
				ArrayList<PageModel> currList =  NovelsDao.getInstance().getNovels(callback, true);
				newList = NovelsDao.getInstance().getNovelsFromInternet(callback);
				
				for(int i = 0; i < currList.size(); ++i) {
					for(int j = 0; j < newList.size(); ++j) {
						if(currList.get(i).getPage().equalsIgnoreCase(newList.get(j).getPage())) {
							newList.remove(j);
							break;
						}
					}					
				}
			}
			return newList;			
		}
		
		public PageModel getUpdatedTOS(ICallbackNotifier callback) throws Exception {

			// checking copyrights
			PageModel p = new PageModel();
			p.setPage("Baka-Tsuki:Copyrights");
			p.setTitle("Baka-Tsuki:Copyrights");
			p.setType(PageModel.TYPE_TOS);
			
			// get current tos
			NovelsDao.getInstance().getPageModel(p, callback);
			
			PageModel newP = NovelsDao.getInstance().getPageModelFromInternet(p, callback);
			
			if(newP.getLastUpdate().getTime() > p.getLastUpdate().getTime()) {
				Log.d(TAG, "TOS Updated.");
				return newP;
			}
			return null;
		}

		public void onCallback(ICallbackEventData message) {
			publishProgress(message.getMessage());
		}
		
		@Override
		protected void onProgressUpdate (String... values){
			if(notifier != null) notifier.onCallback(new CallbackEventData(values[0]));
			LNReaderApplication.getInstance().updateDownload(TAG, lastProgress, values[0]);
		}
	}

	private boolean getConsolidateNotificationPref() {
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_CONSOLIDATE_NOTIFICATION, true);
	}
} 

