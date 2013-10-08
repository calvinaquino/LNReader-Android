package com.erakk.lnreader.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.Activity;
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
	private final ArrayList<SpeakValue> queue;
	private boolean isPaused = false;
	private int startId;
	private final OnCompleteListener onCompleteListener;
	private final Activity context;
	private boolean isTtsInitSuccess = false;

	private static final String SILENCE = "%SILENCE%";

	public TtsHelper(Activity context, OnInitListener listener, OnCompleteListener onComplete) {
		tts = new TextToSpeech(context, this);
		this.listener = listener;
		this.onCompleteListener = onComplete;
		this.context = context;

		queue = new ArrayList<SpeakValue>();
		currentQueueIndex = 0;
	}

	public boolean IsTtsInitSuccess() {
		return isTtsInitSuccess;
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
		if (queue != null && queue.size() > currentQueueIndex) {
			SpeakValue val = queue.get(currentQueueIndex);

			HashMap<String, String> params = new HashMap<String, String>();
			params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "ID:" + currentQueueIndex);

			if (val.Val.equals(SILENCE)) {
				tts.playSilence(whiteSpaceDelay, TextToSpeech.QUEUE_FLUSH, params);
			} else {
				tts.speak(val.Val, TextToSpeech.QUEUE_FLUSH, params);
			}

			++currentQueueIndex;
		}
	}

	private void onComplete(String utteranceId) {
		synchronized (this) {
			if (queue != null && queue.size() > currentQueueIndex) {
				final SpeakValue s = queue.get(currentQueueIndex);
				if (onCompleteListener != null && context != null) {
					context.runOnUiThread(new Runnable() {

						@Override
						public void run() {
							onCompleteListener.onComplete(s.ID);

						}
					});
				}
			}
			if (isPaused) {
				Log.d(TAG, "Paused!");
				return;
			}
			speakFromQueue();
		}
	}

	public void initConfig() {
		if (tts != null) {
			// int result = tts.setLanguage(Locale.US);
			tts.setPitch(UIHelper.getFloatFromPreferences(Constants.PREF_TTS_PITCH, 1.0f));
			tts.setSpeechRate(UIHelper.getFloatFromPreferences(Constants.PREF_TTS_SPEECH_RATE, 1.0f));
			whiteSpaceDelay = UIHelper.getIntFromPreferences(Constants.PREF_TTS_DELAY, 500);

			// if (result == TextToSpeech.LANG_MISSING_DATA) {
			// Toast.makeText(LNReaderApplication.getInstance(), "TTS not supported", Toast.LENGTH_LONG).show();
			// }
		}
	}

	@Override
	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS) {
			Log.d(TAG, "TTS init success");
			if (listener != null) {
				listener.onInit(status);
			}
			initConfig();

			setupOnCompleteListener();
			isTtsInitSuccess = true;
		} else {
			String message = "TTS init failed, status: " + status;
			Log.w(TAG, message);
			Toast.makeText(LNReaderApplication.getInstance(), message, Toast.LENGTH_LONG).show();
			if (listener != null) {
				listener.onInit(status);
			}
			isTtsInitSuccess = false;
		}
	}

	@SuppressWarnings("deprecation")
	public void setupOnCompleteListener() {
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
					Log.d(TAG, "Start v15: " + utteranceId);
				}

				@Override
				public void onError(String utteranceId) {
					Log.e(TAG, "Error v15: " + utteranceId);
				}

				@Override
				public void onDone(String utteranceId) {
					Log.d(TAG, "Completed v15: " + utteranceId);
					onComplete(utteranceId);
				}
			});
		}
	}

	public void dispose() {
		if (tts != null) {
			tts.stop();
			tts.shutdown();
		}
	}

	public void speak(String html, int startId) {
		Log.d(TAG, "Start Speaking from: " + startId);
		this.startId = startId;
		Document doc = Jsoup.parse(html);
		Elements elements = doc.body().select("*:not(.editsection)");
		parseText(elements, startId);
		speakFromQueue();
	}

	private static final Set<String> FORMATTING_ELEMENTS = new HashSet<String>(Arrays.asList(new String[] { "i", "b", "u", "sup" }));

	private void parseText(Elements elements, int startId) {
		Log.d(TAG, "Start ID:" + startId);
		boolean isSkip = true;
		if (startId == 0)
			isSkip = false;

		for (int idx = 0; idx < elements.size(); idx++) {
			Element el = elements.get(idx);
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
				SpeakValue s = new SpeakValue();
				s.Val = SILENCE;
				s.ID = null;
				queue.add(s);
			}

			SpeakValue val = new SpeakValue();
			// check if have children element for formatting
			boolean hasFormattingChild = false;
			for (Element child : el.children()) {
				if (FORMATTING_ELEMENTS.contains(child.tagName().toLowerCase())) {
					hasFormattingChild = true;
					break;
				}
			}

			if (hasFormattingChild) {
				Log.d(TAG, "Got formatting text: " + el.html());
				val.Val = el.text();
				Log.d(TAG, "Use text: " + el.text());
				removeAllChildren(el, elements);
				idx--;
			} else {
				val.Val = el.ownText();
			}

			if (el.hasAttr("id"))
				val.ID = el.attr("id");
			else
				val.ID = null;

			queue.add(val);
		}
	}

	private void removeAllChildren(Element el, Elements elements) {
		for (Element child : el.getAllElements()) {
			elements.remove(child);
		}
	}

	private boolean isWhiteSpace(String node) {
		for (String s : WHITE_SPACE_NODES) {
			if (node.equals(s))
				return true;
		}
		return false;
	}

	private class SpeakValue {
		public String Val;
		public String ID;
	}
}
