package com.erakk.lnreader.task;

import com.erakk.lnreader.helper.AsyncTaskResult;

public interface IAsyncTaskOwner {
	void toggleProgressBar(boolean show);
	void setMessageDialog(String message);
	void getResult(AsyncTaskResult<?> result);
}
