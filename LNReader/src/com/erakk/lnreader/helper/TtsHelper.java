package com.erakk.lnreader.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.UIHelper;

public class TtsHelper implements OnInitListener {
	private static final String TAG = TtsHelper.class.toString();
	private final String[] WHITE_SPACE_NODES = { "br", "p", "h1", "h2", "h3", "h4", "h5" };
	private final TextToSpeech tts;
	private final OnInitListener listener;
	private int whiteSpaceDelay = 500;
	private int currentQueueIndex = 0;
	private final ArrayList<String> queue;
	private boolean isPaused = false;
	private int startId;

	private static final String SILENCE = "%SILENCE%";

	public TtsHelper(Context context, OnInitListener listener) {
		tts = new TextToSpeech(context, this);
		this.listener = listener;

		queue = new ArrayList<String>();
		currentQueueIndex = 0;
	}

	public boolean isReady() {
		if (queue != null && !queue.isEmpty())
			return true;
		return false;
	}

	public boolean isPaused() {
		return isPaused;
	}

	public void pause() {
		isPaused = true;
		Log.d(TAG, "TTS Paused at " + currentQueueIndex);
		if (tts != null && tts.isSpeaking()) {
			tts.stop();
		}
	}

	public void resume() {
		isPaused = false;
		speakFromQueue();
		Log.d(TAG, "TTS Resumed at " + currentQueueIndex);
	}

	public void stop() {
		if (queue != null) {
			queue.clear();
		}
		currentQueueIndex = 0;
		isPaused = false;

		if (tts != null && tts.isSpeaking()) {
			tts.stop();
		}
	}

	public void start(WebView webView, int startId) {
		if (!isReady() || this.startId != startId) {
			stop();
			webView.loadUrl("javascript:doSpeak()");
		} else {
			resume();
		}
	}

	private void speakFromQueue() {
		String val = queue.get(currentQueueIndex);

		HashMap<String, String> params = new HashMap<String, String>();
		params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "ID:" + currentQueueIndex);

		if (val.equals(SILENCE)) {
			tts.playSilence(whiteSpaceDelay, TextToSpeech.QUEUE_FLUSH, params);
		} else
			tts.speak(val, TextToSpeech.QUEUE_FLUSH, params);
	}

	private void onComplete(String utteranceId) {
		if (isPaused) {
			Log.d(TAG, "Paused!");
			return;
		}
		++currentQueueIndex;
		speakFromQueue();
	}

	@Override
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS) {
			int result = tts.setLanguage(Locale.US);
			tts.setPitch(UIHelper.getFloatFromPreferences(Constants.PREF_TTS_PITCH, 1.0f));
			tts.setSpeechRate(UIHelper.getFloatFromPreferences(Constants.PREF_TTS_SPEECH_RATE, 1.0f));
			whiteSpaceDelay = UIHelper.getIntFromPreferences(Constants.PREF_TTS_DELAY, 500);

			if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
				Toast.makeText(LNReaderApplication.getInstance(), "TTS not supported", Toast.LENGTH_LONG).show();
			}
			if (listener != null) {
				listener.onInit(1);
			}

			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
				tts.setOnUtteranceCompletedListener(new OnUtteranceCompletedListener() {

					@Override
					public void onUtteranceCompleted(String utteranceId) {
						Log.d(TAG, "Completed: " + utteranceId);
						onComplete(utteranceId);
					}
				});
			} else {
				tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {

					@Override
					public void onStart(String utteranceId) {
						// TODO Auto-generated method stub

					}

					@Override
					public void onError(String utteranceId) {
						// TODO Auto-generated method stub

					}

					@Override
					public void onDone(String utteranceId) {
						Log.d(TAG, "Completed v15: " + utteranceId);
						onComplete(utteranceId);
					}
				});
			}

			Toast.makeText(LNReaderApplication.getInstance(), "TTS ready", Toast.LENGTH_LONG).show();

		} else {
			Toast.makeText(LNReaderApplication.getInstance(), "TTS init failed", Toast.LENGTH_LONG).show();
			if (listener != null) {
				listener.onInit(0);
			}
		}
	}

	public void dispose() {
		if (tts != null) {
			tts.stop();
			tts.shutdown();
		}
	}

	public void speak(String html, int startId) {
		this.startId = startId;
		Document doc = Jsoup.parse(html);
		Elements elements = doc.body().select("*:not(.editsection)");
		parseText(elements, startId);
		speakFromQueue();
	}

	private void parseText(Elements elements, int startId) {
		Log.d(TAG, "Start ID:" + startId);
		boolean isSkip = true;
		if (startId == 0)
			isSkip = false;

		for (org.jsoup.nodes.Element el : elements) {
			if (el.hasAttr("id") && isSkip) {
				try {
					int id = Integer.parseInt(el.attr("id"));
					if (id >= startId)
						isSkip = false;
				} catch (Exception ex) {
					Log.e(TAG, ex.getMessage());
				}
			}
			if (isSkip)
				continue;
			if (el.parent().hasClass("editsection"))
				continue;
			if (isWhiteSpace(el.tagName())) {
				// tts.playSilence(whiteSpaceDelay, TextToSpeech.QUEUE_ADD, null);
				queue.add(SILENCE);
			}

			queue.add(el.ownText());
			// tts.speak(el.ownText(), TextToSpeech.QUEUE_ADD, null);
		}
	}

	private boolean isWhiteSpace(String node) {
		for (String s : WHITE_SPACE_NODES) {
			if (node.equals(s))
				return true;
		}
		return false;
	}

}
