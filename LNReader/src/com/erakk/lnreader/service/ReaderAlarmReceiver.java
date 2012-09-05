package com.erakk.lnreader.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.erakk.lnreader.Constants;

public class ReaderAlarmReceiver extends BroadcastReceiver {

   // onReceive must be very quick and not block, so it just fires up a Service
   @Override
   public void onReceive(Context context, Intent intent) {
      Log.i(Constants.TAG, "ReaderAlarmReceiver invoked, starting UpdateService in background");
      context.startService(new Intent(context, UpdateService.class));
   }
}