package com.erakk.lnreader.task;

import android.content.Context;

import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.helper.AsyncTaskResult;

public interface IAsyncTaskOwner {
	void updateProgress(String id, int current, int total, String message);

	boolean downloadListSetup(String id, String toastText, int type, boolean hasError);

	void toggleProgressBar(boolean show);

	void setMessageDialog(ICallbackEventData message);

	void onGetResult(AsyncTaskResult<?> result, Class<?> type);

	Context getContext();
}
