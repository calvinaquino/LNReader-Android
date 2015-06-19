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
import com.erakk.lnreader.model.NovelContentModel;
import com.erakk.lnreader.model.PageModel;

import java.util.ArrayList;

public class DownloadNovelContentTask extends AsyncTask<Void, ICallbackEventData, AsyncTaskResult<NovelContentModel[]>> implements ICallbackNotifier {
    private static final String TAG = DownloadNovelContentTask.class.toString();
    private final PageModel[] chapters;
    public volatile IExtendedCallbackNotifier<AsyncTaskResult<?>> owner;
    private int currentChapter = 0;
    private final String taskId;

    public DownloadNovelContentTask(PageModel[] chapters, String taskId, IExtendedCallbackNotifier<AsyncTaskResult<?>> owner) {
        this.chapters = chapters;
        this.owner = owner;
        this.taskId = taskId;
    }

    @Override
    protected void onPreExecute() {
        // executed on UI thread.
        owner.downloadListSetup(this.taskId, null, 0, false);
        LNReaderApplication.getInstance().addDownload(this.taskId, this.taskId);
    }

    @Override
    public void onProgressCallback(ICallbackEventData message) {
        publishProgress(message);
    }

    @Override
    protected AsyncTaskResult<NovelContentModel[]> doInBackground(Void... params) {
        Context ctx = LNReaderApplication.getInstance().getApplicationContext();
        ArrayList<Exception> exceptionList = new ArrayList<Exception>();
        try {
            NovelContentModel[] contents = new NovelContentModel[chapters.length];
            for (int i = 0; i < chapters.length; ++i) {
                currentChapter++;
                NovelContentModel oldContent = NovelsDao.getInstance().getNovelContent(chapters[i], false, null);
                String message = ctx.getResources().getString(R.string.download_novel_content_task_progress, chapters[i].getTitle());
                if (oldContent != null) {
                    message = ctx.getResources().getString(R.string.download_novel_content_task_update, chapters[i].getTitle());
                }
                Log.i(TAG, message);
                publishProgress(new CallbackEventData(message, this.taskId));

                try {
                    NovelContentModel temp = NovelsDao.getInstance().getNovelContentFromInternet(chapters[i], this);
                    contents[i] = temp;
                } catch (Exception e) {
                    Log.e(TAG, String.format("Error when downloading: %s", chapters[i].getTitle()), e);
                    publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.download_novel_content_task_error, chapters[i].getTitle(), e.getMessage()), this.taskId));
                    exceptionList.add(e);
                }

            }
            if (exceptionList.size() > 0) {
                return new AsyncTaskResult<NovelContentModel[]>(contents, contents.getClass(), exceptionList.get(exceptionList.size() - 1));
            }
            return new AsyncTaskResult<NovelContentModel[]>(contents, contents.getClass());
        } catch (Exception e) {
            Log.e(TAG, String.format("Error when downloading: %s", chapters[currentChapter - 1].getPage()), e);
            publishProgress(new CallbackEventData(ctx.getResources().getString(R.string.download_novel_content_task_error, chapters[currentChapter - 1].getPage(), e.getMessage()), this.taskId));
            return new AsyncTaskResult<NovelContentModel[]>(null, NovelContentModel[].class, e);
        }
    }

    @Override
    protected void onProgressUpdate(ICallbackEventData... values) {
        // executed on UI thread.
        DownloadCallbackEventData message = new DownloadCallbackEventData(values[0].getMessage(), currentChapter, chapters.length, this.taskId);
        owner.onProgressCallback(message);
        LNReaderApplication.getInstance().updateDownload(this.taskId, message.getPercentage(), message.getMessage());
    }

    @Override
    protected void onPostExecute(AsyncTaskResult<NovelContentModel[]> result) {
        Context ctx = LNReaderApplication.getInstance().getApplicationContext();
        CallbackEventData message = new CallbackEventData(ctx.getResources().getString(R.string.download_novel_content_task_complete), this.taskId);
        owner.downloadListSetup(this.taskId, message.getMessage(), 2, result.getError() != null ? true : false);
        LNReaderApplication.getInstance().removeDownload(this.taskId);
        owner.onCompleteCallback(message, result);
    }
}