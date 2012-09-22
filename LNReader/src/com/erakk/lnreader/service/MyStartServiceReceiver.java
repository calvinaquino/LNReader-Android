package com.erakk.lnreader.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MyStartServiceReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    Intent service = new Intent(context, UpdateService.class);
    context.startService(service);
	Log.d(UpdateService.TAG, "onReceive_Start");
  }
} 