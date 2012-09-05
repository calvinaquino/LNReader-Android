package com.erakk.lnreader.service;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.erakk.lnreader.Constants;

public class UpdateService extends IntentService {

	public UpdateService() {
		super(Constants.TAG);
	}

	@Override
	public void onCreate() {
	    super.onCreate();
	    Log.d(Constants.TAG, "onCreate");
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
	    Log.d(Constants.TAG, "onHandleIntent for action: " + intent.getAction());
	}
	
	@Override
	public void onDestroy() {	
		super.onDestroy();
	    Log.d(Constants.TAG, "onDestroy");
	}

}
