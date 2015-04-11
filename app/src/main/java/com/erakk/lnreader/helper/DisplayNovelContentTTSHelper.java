package com.erakk.lnreader.helper;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.activity.DisplayLightNovelContentActivity;
import com.erakk.lnreader.service.TtsService;
import com.erakk.lnreader.service.TtsService.TtsBinder;

public class DisplayNovelContentTTSHelper {

	protected static final String TAG = DisplayNovelContentTTSHelper.class.toString();
	private final DisplayLightNovelContentActivity callingCtx;
	private TtsBinder ttsBinder = null;
	private TtsService ttsService = null;

	public DisplayNovelContentTTSHelper(DisplayLightNovelContentActivity parent) {
		callingCtx = parent;
	}

	private TtsBinder getTtsBinder() {
		if (UIHelper.isTTSEnabled(callingCtx)) {
			int retry = 0;
			while (ttsBinder == null && retry < 3) {
				Log.i(TAG, "Trying to get TTS Binder: " + retry);
				if (ttsService == null || ttsBinder == null) {
					setupTtsService();
					try{
                        ttsBinder.initConfig();
                    }
                    catch (NullPointerException ex) {
                        Log.i(TAG, "Failed to init TTS Binder, retrying...");
                    }
					break;
				}
				++retry;
			}
			if (retry >= 3)
				Toast.makeText(callingCtx, "Failed to get TTS Service", Toast.LENGTH_SHORT).show();
		}
		return ttsBinder;
	}

	public void stop() {
		if (getTtsBinder() != null)
			ttsBinder.stop();
	}

	public void pause() {
		if (getTtsBinder() != null)
			ttsBinder.pause();
	}

	public void start(NonLeakingWebView webView, int index) {
		if (getTtsBinder() != null)
			ttsBinder.start(webView, index);
	}

	public void start(String html, int index) {
		if (getTtsBinder() != null)
			ttsBinder.speak(html, index);
	}

	private final ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder binder) {
			ttsBinder = (TtsBinder) binder;
			ttsService = ttsBinder.getService();
			Log.d(TAG, "TTS onServiceConnected");
			ttsService.setOnCompleteListener(callingCtx);
			ttsService.setOnInitListener(callingCtx);
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			ttsService = null;
			Log.d(TAG, "TTS onServiceDisconnected");
		}
	};

	public void setupTtsService() {
		Log.d(TAG, "Binding TTS Service");
		Intent serviceIntent = new Intent(callingCtx, TtsService.class);
		callingCtx.bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
	}

	public void unbindTtsService() {
		if (mConnection != null && ttsService != null) {
			try {
				callingCtx.unbindService(mConnection);
				Log.i(TAG, "Unbind service done.");
			} catch (Exception ex) {
				Log.e(TAG, "Failed to unbind.", ex);
			}
		}
	}

	public void setupTTSMenu(Menu menu) {
		MenuItem menuSpeak = menu.findItem(R.id.menu_speak);
		MenuItem menuPause = menu.findItem(R.id.menu_pause_tts);

		if (ttsBinder != null && UIHelper.isTTSEnabled(callingCtx)) {
			menuSpeak.setEnabled(ttsBinder.IsTtsInitSuccess());
			menuPause.setEnabled(!ttsBinder.isPaused());
		} else {
			menuSpeak.setEnabled(false);
			menuPause.setEnabled(false);
		}
	}

	public void autoScroll(final NonLeakingWebView webView, final String index) {
		if (webView != null) {
			callingCtx.runOnUiThread(new Runnable() {

				@Override
				public void run() {

					if (true && webView != null && index != null) {
						Log.d(TAG, "Auto Scroll to: " + index);
						try {
							webView.loadUrl("javascript:goToParagraph(" + index + ", true)");
						} catch (Exception ex) {
							Log.e(TAG, ex.getMessage(), ex);
						}
					}
				}
			});
		}
	}
}
