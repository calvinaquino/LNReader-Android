package com.erakk.lnreader.task;

public class AsyncTaskResult<T> {
	private T result;
	private Exception error;

	public AsyncTaskResult(T result) {
		super();
		this.result = result;
	}

	public AsyncTaskResult(Exception error) {
		super();
		this.error = error;
	}

	public T getResult() {
		return result;
	}

	public Exception getError() {
		return error;
	}

	public Class<? extends Object> getResultType() {
		return result.getClass();
	}

}
