package com.erakk.lnreader.callback;

public class CallbackEventData implements ICallbackEventData {
	protected String message;
	protected String source;
	protected int percentage = -1;

	public CallbackEventData(String message, String source) {
		this.message = message;
		this.source = source;
	}

	public CallbackEventData(String message, int percentage, String source) {
		this.message = message;
		this.percentage = percentage;
		this.source = source;
	}

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public String getSource() {
		return source;
	}

	@Override
	public int getPercentage() {
		return percentage;
	}
}
