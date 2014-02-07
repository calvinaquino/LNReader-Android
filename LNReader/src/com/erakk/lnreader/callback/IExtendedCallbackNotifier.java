package com.erakk.lnreader.callback;

public interface IExtendedCallbackNotifier<T> {
	void onCompleteCallback(ICallbackEventData message, T result);
	void onProgressCallback(ICallbackEventData message);
	boolean downloadListSetup(String taskId, String message, int setupType, boolean hasError);
}
