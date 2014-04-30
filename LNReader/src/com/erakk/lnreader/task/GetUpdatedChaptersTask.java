package com.erakk.lnreader.task;

import java.util.ArrayList;
import java.util.Date;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.callback.CallbackEventData;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.callback.IExtendedCallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.PageModel;
import com.erakk.lnreader.service.UpdateScheduleReceiver;
import com.erakk.lnreader.service.UpdateService;

public class GetUpdatedChaptersTask extends AsyncTask<Void, String, AsyncTaskResult<ArrayList<PageModel>>> implements ICallbackNotifier {
	private static final String TAG = GetUpdatedChaptersTask.class.toString();
	private int lastProgress;
	private final boolean autoDownloadUpdatedContent;
	private final UpdateService service;
	private IExtendedCallbackNotifier<AsyncTaskResult<?>> notifier;
	private String source;

	public GetUpdatedChaptersTask(UpdateService service, boolean autoDownloadUpdatedContent, IExtendedCallbackNotifier<AsyncTaskResult<?>> notifier) {
		this.autoDownloadUpdatedContent = autoDownloadUpdatedContent;
		this.service = service;
		this.notifier = notifier;
		Log.d(TAG, "Auto Download: " + autoDownloadUpdatedContent);
	}

	@Override
	protected AsyncTaskResult<ArrayList<PageModel>> doInBackground(Void... arg0) {
		// add on Download List
		LNReaderApplication.getInstance().addDownload(TAG, "Update Service");
		service.setRunning(true);
		try {
			ArrayList<PageModel> result = getUpdatedChapters(this);
			return new AsyncTaskResult<ArrayList<PageModel>>(result, result.getClass());
		} catch (Exception ex) {
			Log.e(TAG, "Error when updating", ex);
			return new AsyncTaskResult<ArrayList<PageModel>>(ex);
		}
	}

	@Override
	protected void onPostExecute(AsyncTaskResult<ArrayList<PageModel>> result) {
		Exception e = result.getError();
		if (e == null) {
			service.sendNotification(result.getResult());
		} else {
			String text = "Error when getting updates: " + e.getMessage();
			Log.e(TAG, text, e);
			service.updateStatus("ERROR==>" + e.getMessage());
			Toast.makeText(service.getApplicationContext(), text, Toast.LENGTH_LONG).show();
		}

		// Reschedule for next run
		UpdateScheduleReceiver.reschedule(service);
		service.setRunning(false);

		// update last run
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(service.getApplicationContext());
		SharedPreferences.Editor editor = preferences.edit();
		editor.putLong(Constants.PREF_LAST_UPDATE, new Date().getTime());
		editor.commit();

		// remove from download list
		LNReaderApplication.getInstance().removeDownload(TAG);
	}

	private ArrayList<PageModel> getUpdatedChapters(ICallbackNotifier callback) throws Exception {
		Log.d(TAG, "Checking Updates...");
		ArrayList<PageModel> updatesTotal = new ArrayList<PageModel>();
		NovelsDao dao = NovelsDao.getInstance();

		PageModel updatedTos = getUpdatedTOS(callback);
		if (updatedTos != null) {
			updatesTotal.add(updatedTos);
		}

		// check updated novel list
		ArrayList<PageModel> updatedNovelList = getUpdatedNovelList(callback);
		if (updatedNovelList != null && updatedNovelList.size() > 0) {
			Log.d(TAG, "Got new novel! ");
			for (PageModel pageModel : updatedNovelList) {
				updatesTotal.add(pageModel);
			}
		}

		// check only watched novel
		if (callback != null)
			callback.onProgressCallback(new CallbackEventData("Getting watched novel.", source));
		ArrayList<PageModel> watchedNovels = dao.getWatchedNovel();
		if (watchedNovels != null) {
			double total = watchedNovels.size() + 1;
			double current = 0;
			for (PageModel watchedNovel : watchedNovels) {
				ArrayList<PageModel> updatedChapters = processWatchedNovel(watchedNovel, callback);
				updatesTotal.addAll(updatedChapters);

				if (autoDownloadUpdatedContent) {
					for (PageModel chapter : updatedChapters) {
						downloadUpdatedChapter(chapter, callback);
					}
					Log.i(TAG, "Updated Chapter Downloaded: " + updatedChapters.size() + " for: " + watchedNovel.getPage());
				}

				lastProgress = (int) (++current / total * 100);
				Log.d(TAG, "Progress: " + lastProgress);
			}
			service.setForce(false);
		}

		Log.i(TAG, "Found updates: " + updatesTotal.size());

		return updatesTotal;
	}

	private void downloadUpdatedChapter(PageModel chapter, ICallbackNotifier callback) throws Exception {
		String message = "Downloading updated content for: " + chapter.getPage();
		Log.i(TAG, message);
		NovelsDao dao = NovelsDao.getInstance();
		if (callback != null) {
			callback.onProgressCallback(new CallbackEventData(message, source));
		}
		if (!chapter.isMissing() && !chapter.isExternal()) {
			dao.getNovelContentFromInternet(chapter, callback);
		}
	}

	private ArrayList<PageModel> processWatchedNovel(PageModel novel, ICallbackNotifier callback) throws Exception {
		ArrayList<PageModel> updatedWatchedNovel = new ArrayList<PageModel>();
		NovelsDao dao = NovelsDao.getInstance();

		// get last update date from internet
		if (callback != null)
			callback.onProgressCallback(new CallbackEventData("Checking: " + novel.getTitle(), source));
		PageModel updatedNovel = dao.getPageModelFromInternet(novel.getPageModel(), callback);

		// different timestamp
		if (service.isForced() || !novel.getLastUpdate().equals(updatedNovel.getLastUpdate())) {
			if (service.isForced()) {
				Log.i(TAG, "Force Mode: " + novel.getPage());
			} else {
				Log.d(TAG, "Different Timestamp for: " + novel.getPage());
				Log.d(TAG, "old: " + novel.getLastUpdate().toString() + " != " + updatedNovel.getLastUpdate().toString());
			}
			ArrayList<PageModel> novelDetailsChapters = dao.getNovelDetails(novel, callback).getFlattedChapterList();

			if (callback != null)
				callback.onProgressCallback(new CallbackEventData("Getting updated chapters: " + novel.getTitle(), source));
			NovelCollectionModel updatedNovelDetails = dao.getNovelDetailsFromInternet(novel, callback);
			if (updatedNovelDetails != null) {
				ArrayList<PageModel> updates = updatedNovelDetails.getFlattedChapterList();

				Log.d(TAG, "Starting size: " + updates.size());
				// compare the chapters!
				for (int i = 0; i < novelDetailsChapters.size(); ++i) {
					PageModel oldChapter = novelDetailsChapters.get(i);
					for (int j = 0; j < updates.size(); j++) {
						PageModel newChapter = updates.get(j);
						if (callback != null)
							callback.onProgressCallback(new CallbackEventData("Checking: " + oldChapter.getTitle() + " ==> " + newChapter.getTitle(), source));
						// check if the same page
						if (newChapter.getPage().compareTo(oldChapter.getPage()) == 0) {
							// check if last update date is newer
							// TODO: Nandaka: got some bug on the parse api, so check if not equal.
							if (newChapter.getLastUpdate().getTime() > oldChapter.getLastUpdate().getTime()) {
								newChapter.setUpdated(true);
								Log.i(TAG, "Found updated chapter: " + newChapter.getTitle());
							} else {
								updates.remove(newChapter);
								Log.i(TAG, "No Update for Chapter: " + newChapter.getTitle());
							}
							break;
						}
					}
				}
				Log.d(TAG, "End size: " + updates.size());
				updatedWatchedNovel.addAll(updates);
			}
		}
		return updatedWatchedNovel;
	}

	private ArrayList<PageModel> getUpdatedNovelList(ICallbackNotifier callback) throws Exception {
		ArrayList<PageModel> newList = null;

		PageModel mainPage = new PageModel();
		mainPage.setPage("Main_Page");

		mainPage = NovelsDao.getInstance().getPageModel(mainPage, callback);

		// check if more than 7 day
		Date today = new Date();
		long diff = today.getTime() - mainPage.getLastCheck().getTime();
		if (service.isForced() || diff > (Constants.CHECK_INTERVAL * 24 * 3600 * 1000) && LNReaderApplication.getInstance().isOnline()) {
			Log.d(TAG, "Last check is over 7 days, checking online status");
			ArrayList<PageModel> currList = NovelsDao.getInstance().getNovels(callback, true);
			newList = NovelsDao.getInstance().getNovelsFromInternet(callback);

			for (int i = 0; i < currList.size(); ++i) {
				for (int j = 0; j < newList.size(); ++j) {
					if (currList.get(i).getPage().equalsIgnoreCase(newList.get(j).getPage())) {
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

		if (newP.getLastUpdate().getTime() > p.getLastUpdate().getTime()) {
			Log.d(TAG, "TOS Updated.");
			return newP;
		}
		return null;
	}

	@Override
	public void onProgressCallback(ICallbackEventData message) {
		publishProgress(message.getMessage());
	}

	@Override
	protected void onProgressUpdate(String... values) {
		if (notifier != null)
			notifier.onProgressCallback(new CallbackEventData(values[0], lastProgress, Constants.PREF_RUN_UPDATES));
		LNReaderApplication.getInstance().updateDownload(TAG, lastProgress, values[0]);
	}

	public void setCallbackNotifier(IExtendedCallbackNotifier<AsyncTaskResult<?>> notifier) {
		this.notifier = notifier;
	}
}
