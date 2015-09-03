package com.erakk.lnreader.task;

import android.os.AsyncTask;

import com.erakk.lnreader.callback.CallbackEventData;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.IExtendedCallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.model.UpdateInfoModel;

import java.util.ArrayList;

public class LoadUpdatesTask extends AsyncTask<Void, ICallbackEventData, AsyncTaskResult<UpdateInfoModel[]>> {

    private static final String TAG = LoadUpdatesTask.class.toString();
    public volatile IExtendedCallbackNotifier<AsyncTaskResult<?>> owner;

    public LoadUpdatesTask(IExtendedCallbackNotifier owner) {
        this.owner = owner;
    }

    @Override
    protected AsyncTaskResult<UpdateInfoModel[]> doInBackground(Void... params) {
        ArrayList<UpdateInfoModel> temp = NovelsDao.getInstance().getAllUpdateHistory();
        UpdateInfoModel[] result = temp.toArray(new UpdateInfoModel[temp.size()]);

        return new AsyncTaskResult<UpdateInfoModel[]>(result, UpdateInfoModel[].class);
    }

    @Override
    protected void onPostExecute(AsyncTaskResult<UpdateInfoModel[]> result) {
        CallbackEventData message = new CallbackEventData("Completed", "LoadUpdatesTask");
        owner.onCompleteCallback(message, result);
    }
}
