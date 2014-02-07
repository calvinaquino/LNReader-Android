package com.erakk.lnreader.callback;

public interface ICompleteCallbackNotifier<T> {
	public void onCompleteCallback(ICallbackEventData message, T result);
}
