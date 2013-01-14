package com.erakk.lnreader.task;

import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.helper.AsyncTaskResult;

public interface IAsyncTaskOwner {
	void updateProgress(String id, int current, int total, String message);
	boolean downloadListSetup(String id, String toastText, int type);
	void toggleProgressBar(boolean show);
	void setMessageDialog(ICallbackEventData message);
	void getResult(AsyncTaskResult<?> result);
}
