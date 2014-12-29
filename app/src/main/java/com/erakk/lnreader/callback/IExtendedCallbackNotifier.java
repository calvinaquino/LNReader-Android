package com.erakk.lnreader.callback;

public interface IExtendedCallbackNotifier<T> {
	void onCompleteCallback(ICallbackEventData message, T result);
	void onProgressCallback(ICallbackEventData message);

	/***
	 * Add Task to the Download List activity list.
	 * @param taskId
	 * @param message
	 * @param setupType 0: Add to list, 1: Show toast message, 2: Show toast message and remove from list.
	 * @param hasError
	 * @return
	 */
	boolean downloadListSetup(String taskId, String message, int setupType, boolean hasError);
}
