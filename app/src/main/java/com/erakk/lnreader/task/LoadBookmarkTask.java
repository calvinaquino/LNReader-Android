package com.erakk.lnreader.task;

import android.os.AsyncTask;
import android.util.Log;

import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.callback.CallbackEventData;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.IExtendedCallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.model.BookmarkModel;
import com.erakk.lnreader.model.PageModel;

import java.util.ArrayList;

public class LoadBookmarkTask extends AsyncTask<Void, ICallbackEventData, AsyncTaskResult<BookmarkModel[]>>{

    private static final String TAG = LoadBookmarkTask.class.toString();
    public volatile IExtendedCallbackNotifier<AsyncTaskResult<?>> owner;

    public LoadBookmarkTask(IExtendedCallbackNotifier owner) {
        this.owner = owner;
    }

    @Override
    protected AsyncTaskResult<BookmarkModel[]> doInBackground(Void... params) {
        ArrayList<BookmarkModel> temp = NovelsDao.getInstance().getAllBookmarks(UIHelper.getAllBookmarkOrder(LNReaderApplication.getInstance()));
        BookmarkModel[] result = temp.toArray(new BookmarkModel[temp.size()]);

        // preload pagemodel
        for(BookmarkModel bm : result) {
            try {
                PageModel p = bm.getPageModel();
                p.getParentPageModel();

            }catch (Exception ex) {
                Log.e(TAG, "Failed to get page model: " + bm.getPage());
            }
        }

        return new AsyncTaskResult<BookmarkModel[]>(result, BookmarkModel[].class);
    }

    @Override
    protected void onPostExecute(AsyncTaskResult<BookmarkModel[]> result) {
        CallbackEventData message = new CallbackEventData("Completed", "LoadBookmarkTask");
        owner.onCompleteCallback(message, result);
    }
}
