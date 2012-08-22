package com.erakk.lnreader.callback;

public class CallbackEventData implements ICallbackEventData {
	protected String message;

	public CallbackEventData() {};
	
	public CallbackEventData(String message) {
		this.message = message;
	}
	
	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public void setMessage(String message) {
		this.message = message;
	}
	
}
