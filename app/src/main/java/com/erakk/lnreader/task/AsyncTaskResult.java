package com.erakk.lnreader.task;

public class AsyncTaskResult<T> {
	private T result;
	private Class<? extends Object> resultType;
	private Exception error;

	public AsyncTaskResult(T result, Class<? extends Object> resultType) {
		super();
		this.result = result;
		this.resultType = resultType;
	}

	public AsyncTaskResult(T result, Class<? extends Object> resultType, Exception error) {
		super();
		this.error = error;
		this.result = result;
		this.resultType = resultType;
	}

	public T getResult() {
		return result;
	}

	public Exception getError() {
		return error;
	}

	public Class<? extends Object> getResultType() {
		if(result == null)
			return resultType;
		return result.getClass();
	}

}
