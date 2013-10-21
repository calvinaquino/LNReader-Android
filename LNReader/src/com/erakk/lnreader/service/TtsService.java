package com.erakk.lnreader.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;

import com.erakk.lnreader.helper.NonLeakingWebView;
import com.erakk.lnreader.helper.OnCompleteListener;
import com.erakk.lnreader.helper.TtsHelper;

public class TtsService extends Service implements OnInitListener, OnCompleteListener {

	public static final String TAG = TtsService.class.toString();
	private final IBinder mBinder = new TtsBinder();
	private TtsHelper tts;
	private OnInitListener onInitListener;
	private OnCompleteListener onComplete;

	public void OnCreate() {
		tts = new TtsHelper(this, this, this);
	}

	public void setOnInitListener(OnInitListener onInit) {
		this.onInitListener = onInit;
	}

	public void setOnCompleteListener(OnCompleteListener onComplete) {
		this.onComplete = onComplete;
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "TTS Service onBind");
		if (tts == null) {
			tts = new TtsHelper(this, this, this);
		}
		return mBinder;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (tts != null) {
			tts.dispose();
		}
	}

	@Override
	public void onComplete(Object i) {
		if (this.onComplete != null) {
			this.onComplete.onComplete(i);
		}
	}

	@Override
	public void onInit(int arg0) {
		if (this.onInitListener != null) {
			this.onInitListener.onInit(arg0);
		}
	}

	public class TtsBinder extends Binder {
		public TtsService getService() {
			Log.d(TAG, "getService");
			return TtsService.this;
		}

		public void speak(String html, int startIndex) {
			if (tts != null) {
				tts.speak(html, startIndex);
			}
		}

		public void stop() {
			if (tts != null) {
				tts.stop();
			}
		}

		public boolean IsTtsInitSuccess() {
			if (tts != null) {
				return tts.isTtsInitSuccess();
			}
			return false;
		}

		public boolean isPaused() {
			if (tts != null) {
				return tts.isPaused();
			}
			return false;
		}

		public void pause() {
			if (tts != null) {
				tts.pause();
			}
		}

		public void start(NonLeakingWebView webView, int lastYScroll) {
			if (tts != null) {
				tts.start(webView, lastYScroll);
			}
		}

		public void initConfig() {
			if (tts != null) {
				tts.initConfig();
			}
		}
	}
}
