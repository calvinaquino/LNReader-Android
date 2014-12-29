package com.erakk.lnreader.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AutoBackupStartServiceReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Intent service = new Intent(context, AutoBackupService.class);
		context.startService(service);
		Log.d(AutoBackupService.TAG, "onReceive_Start");
	}
}