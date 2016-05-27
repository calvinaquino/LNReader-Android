package com.erakk.lnreader.task;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.callback.CallbackEventData;
import com.erakk.lnreader.callback.DownloadCallbackEventData;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.callback.IExtendedCallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.BakaReaderException;
import com.erakk.lnreader.helper.Util;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.PageModel;

import java.util.ArrayList;

public class DownloadNovelDetailsTask extends AsyncTask<PageModel, ICallbackEventData, AsyncTaskResult<NovelCollectionModel[]>> implements ICallbackNotifier {

    private static final String TAG = DownloadNovelDetailsTask.class.toString();
    public volatile IExtendedCallbackNotifier<AsyncTaskResult<?>> owner;
    private int currentPart = 0;
    private int totalParts = 0;
    private final String taskId;

    public DownloadNovelDetailsTask(IExtendedCallbackNotifier<AsyncTaskResult<?>> owner) {
        this.owner = owner;
        this.taskId = this.toString();
    }

    @Override
    protected void onPreExecute() {
        owner.downloadListSetup(this.taskId, null, 0, false);
        LNReaderApplication.getInstance().addDownload(this.taskId, this.taskId);
    }

    @Override
    public void onProgressCallback(ICallbackEventData message) {
        publishProgress(message);
    }

    @Override
    protected AsyncTaskResult<NovelCollectionModel[]> doInBackground(PageModel... params) {
        Context ctx = LNReaderApplication.getInstance();
        ArrayList<NovelCollectionModel> result = new ArrayList<NovelCollectionModel>();
        totalParts = params.length;

        ArrayList<Exception> exs = new ArrayList<Exception>();
        for (PageModel pageModel : params) {
            currentPart++;
            try {
                Log.i(TAG, "Downloading: " + pageModel.getPage());
                publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.download_novel_details_task_progress, pageModel.getTitle()), this.taskId));
                NovelCollectionModel novelCol = NovelsDao.getInstance().getNovelDetailsFromInternet(pageModel, this);
                result.add(novelCol);
            } catch (Exception e) {
                Log.e(TAG, "Failed to download novel details for " + pageModel.getPage() + ": " + e.getMessage(), e);
                publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.download_novel_details_task_error, pageModel.getPage(), e.getMessage()), this.taskId));
                exs.add(e);
            }
        }
        if (exs.size() == 1) {
            return new AsyncTaskResult<NovelCollectionModel[]>(result.toArray(new NovelCollectionModel[result.size()]), NovelCollectionModel[].class, new BakaReaderException(exs.get(0).getMessage(), BakaReaderException.DOWNLOADNOVELDETAIL_ERROR));
        } else if (exs.size() > 1) {
            String errors = Util.join(exs, "\n");
            return new AsyncTaskResult<NovelCollectionModel[]>(result.toArray(new NovelCollectionModel[result.size()]), NovelCollectionModel[].class, new BakaReaderException(errors, BakaReaderException.MULTIPLE_ERRORS));
        }
        return new AsyncTaskResult<NovelCollectionModel[]>(result.toArray(new NovelCollectionModel[result.size()]), NovelCollectionModel[].class);
    }

    @Override
    protected void onProgressUpdate(ICallbackEventData... values) {
        // executed on UI thread.
        DownloadCallbackEventData message = new DownloadCallbackEventData(values[0].getMessage(), currentPart, totalParts, this.taskId);
        owner.onProgressCallback(message);
        LNReaderApplication.getInstance().updateDownload(this.taskId, message.getPercentage(), message.getMessage());
    }

    @Override
    protected void onPostExecute(AsyncTaskResult<NovelCollectionModel[]> result) {
        Context ctx = LNReaderApplication.getInstance();
        owner.downloadListSetup(this.taskId, null, 2, result.getError() != null);
        LNReaderApplication.getInstance().removeDownload(this.taskId);
        owner.onCompleteCallback(new CallbackEventData(ctx.getResources().getString(R.string.download_novel_details_task_complete), this.taskId), result);
    }
}
