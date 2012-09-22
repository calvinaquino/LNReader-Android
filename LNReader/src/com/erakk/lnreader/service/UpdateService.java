package com.erakk.lnreader.service;

import java.util.ArrayList;
import java.util.Iterator;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.erakk.lnreader.activity.MainActivity;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.AsyncTaskResult;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.PageModel;

public class UpdateService extends Service {
	private final IBinder mBinder = new MyBinder();
	private final String TAG = this.getClass().toString();
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "onStartCommand");
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
		GetUpdatedChaptersTask task = new GetUpdatedChaptersTask();
		task.execute();
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
			CharSequence tickerText = "Novel Update";
			long when = System.currentTimeMillis();		
		
			int i = 0;
			for(Iterator<PageModel> iChapter = updatedChapters.iterator(); iChapter.hasNext();) {
				PageModel chapter = iChapter.next();
				Notification notification = new Notification(icon, tickerText, when);
				
				Context context = getApplicationContext();
				CharSequence contentTitle = "Novel Name";
				CharSequence contentText = chapter.getTitle();
				Intent notificationIntent = new Intent(this, MainActivity.class);
				PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		
				notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
				
				final int NOTIF_ID = ++i;
		
				mNotificationManager.notify(NOTIF_ID, notification);
			}		
		}
	}

	public class GetUpdatedChaptersTask extends AsyncTask<Void, String, AsyncTaskResult<ArrayList<PageModel>>>{

		@Override
		protected AsyncTaskResult<ArrayList<PageModel>> doInBackground(Void... arg0) {
			try{
				return new AsyncTaskResult<ArrayList<PageModel>>(GetUpdatedChapters());
			}catch(Exception ex) {
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
				Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
			}
		}
	}
	
	public ArrayList<PageModel> GetUpdatedChapters() throws Exception {
		ArrayList<PageModel> updates = new ArrayList<PageModel>();
		NovelsDao dao = NovelsDao.getInstance();
		
		// check only watched novel
		ArrayList<PageModel> watchedNovels = dao.getWatchedNovel();
		if(watchedNovels != null){
			for(Iterator<PageModel> iNovels = watchedNovels.iterator(); iNovels.hasNext();){
				// get last update date from internet
				PageModel novel = iNovels.next();
				PageModel updatedNovel = dao.getPageModelFromInternet(novel.getPage(), null);
				
				// different timestamp
				if(!novel.getLastUpdate().equals(updatedNovel.getPage())) {
					Log.d(TAG, "Different Timestamp for: " + novel.getPage());
					ArrayList<PageModel> novelDetailsChapters = dao.getNovelDetails(novel, null).getFlattedChapterList();
					NovelCollectionModel updatedNovelDetails = dao.getNovelDetailsFromInternet(novel, null);
					if(updatedNovelDetails!= null){
						ArrayList<PageModel> updatedNovelDetailsChapters = updatedNovelDetails.getFlattedChapterList();
						
						// compare the chapters!
						for(Iterator<PageModel> iChapters = novelDetailsChapters.iterator(); iChapters.hasNext();){
							PageModel chapter = iChapters.next();
							if(!updatedNovelDetailsChapters.contains(chapter)) {
								updates.add(chapter);
								Log.d(TAG, "Found updated chapter: " + chapter.getPage());
							}
						}
					}
				}				
			}
		}
		
		return updates;
	}
} 
