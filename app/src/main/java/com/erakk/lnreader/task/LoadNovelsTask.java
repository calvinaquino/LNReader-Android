package com.erakk.lnreader.task;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.callback.CallbackEventData;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.callback.IExtendedCallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.model.PageModel;

import java.util.ArrayList;

public class LoadNovelsTask extends AsyncTask<Void, ICallbackEventData, AsyncTaskResult<PageModel[]>> implements ICallbackNotifier {
    private static final String TAG = LoadNovelsTask.class.toString();
    private boolean refreshOnly = false;
    private boolean onlyWatched = false;
    private boolean alphOrder = false;
    private final String mode;
    public volatile IExtendedCallbackNotifier<AsyncTaskResult<?>> owner;
    private String source;

    public LoadNovelsTask(IExtendedCallbackNotifier<AsyncTaskResult<?>> owner, boolean refreshOnly, boolean onlyWatched, boolean alphOrder, String mode) {
        this.refreshOnly = refreshOnly;
        this.onlyWatched = onlyWatched;
        this.alphOrder = alphOrder;
        this.owner = owner;
        this.mode = mode;
    }

    @Override
    public void onProgressCallback(ICallbackEventData message) {
        publishProgress(message);
    }

    @Override
    protected void onPreExecute() {
        // executed on UI thread.
        owner.onProgressCallback(new CallbackEventData("Loading novels...", source));
    }

    @Override
    protected AsyncTaskResult<PageModel[]> doInBackground(Void... arg0) {
        Context ctx = LNReaderApplication.getInstance().getApplicationContext();
        // different thread from UI
        try {
            ArrayList<PageModel> novels = new ArrayList<PageModel>();
            if (mode.equalsIgnoreCase(Constants.EXTRA_NOVEL_LIST_MODE_MAIN)) {
                if (onlyWatched) {
                    publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.load_novels_task_watched), source));
                    novels = NovelsDao.getInstance().getWatchedNovel();
                } else {
                    if (refreshOnly) {
                        publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.load_novels_task_refreshing), source));
                        novels = NovelsDao.getInstance().getNovelsFromInternet(this);
                    } else {
                        publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.load_novels_task_loading), source));
                        novels = NovelsDao.getInstance().getNovels(this, alphOrder);
                    }
                }
            } else if (mode.equalsIgnoreCase(Constants.EXTRA_NOVEL_LIST_MODE_ORIGINAL)) {
                if (refreshOnly) {
                    publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.load_original_task_refreshing), source));
                    novels = NovelsDao.getInstance().getOriginalFromInternet(this);
                } else {
                    publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.load_original_task_loading), source));
                    novels = NovelsDao.getInstance().getOriginal(this, alphOrder);
                }
            } else if (mode.equalsIgnoreCase(Constants.EXTRA_NOVEL_LIST_MODE_TEASER)) {
                if (refreshOnly) {
                    publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.load_teaser_task_refreshing), source));
                    novels = NovelsDao.getInstance().getTeaserFromInternet(this);
                } else {
                    publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.load_teaser_task_loading), source));
                    novels = NovelsDao.getInstance().getTeaser(this, alphOrder);
                }
            } else if (mode.equalsIgnoreCase(Constants.EXTRA_NOVEL_LIST_MODE_WEB)) {
                if (refreshOnly) {
                    publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.load_web_task_refreshing), source));
                    novels = NovelsDao.getInstance().getWebNovelFromInternet(this);
                } else {
                    publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.load_web_task_loading), source));
                    novels = NovelsDao.getInstance().getWebNovel(this, alphOrder);
                }
            }

            return new AsyncTaskResult<PageModel[]>(novels.toArray(new PageModel[novels.size()]), PageModel[].class);
        } catch (Exception e) {
            Log.e(TAG, "Error when getting novel list: " + e.getMessage(), e);
            publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.load_novels_task_error, e.getMessage()), source));
            return new AsyncTaskResult<PageModel[]>(null, PageModel[].class, e);
        }
    }

    @Override
    protected void onProgressUpdate(ICallbackEventData... values) {
        owner.onProgressCallback(values[0]);
    }

    @Override
    protected void onPostExecute(AsyncTaskResult<PageModel[]> result) {
        Context ctx = LNReaderApplication.getInstance();
        // executed on UI thread.
        CallbackEventData message = null;
        if (mode.equalsIgnoreCase(Constants.EXTRA_NOVEL_LIST_MODE_MAIN)) {
            if (onlyWatched) {
                message = new CallbackEventData(ctx.getResources().getString(R.string.load_novels_task_watched_complete), source);
            } else {
                message = new CallbackEventData(ctx.getResources().getString(R.string.load_novels_task_complete), source);
            }
        } else if (mode.equalsIgnoreCase(Constants.EXTRA_NOVEL_LIST_MODE_ORIGINAL)) {
            message = new CallbackEventData(ctx.getResources().getString(R.string.load_original_task_complete), source);
        } else if (mode.equalsIgnoreCase(Constants.EXTRA_NOVEL_LIST_MODE_TEASER)) {
            message = new CallbackEventData(ctx.getResources().getString(R.string.load_teaser_task_complete), source);
        } else if (mode.equalsIgnoreCase(Constants.EXTRA_NOVEL_LIST_MODE_WEB)) {
            message = new CallbackEventData(ctx.getResources().getString(R.string.load_web_task_complete), source);
        }

        owner.onCompleteCallback(message, result);
    }
}