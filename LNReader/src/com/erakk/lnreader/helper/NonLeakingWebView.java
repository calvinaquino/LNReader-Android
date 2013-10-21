/**
 * Taken from http://stackoverflow.com/a/8949378
 */
package com.erakk.lnreader.helper;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ZoomButtonsController;

/**
 * see http://stackoverflow.com/questions/3130654/memory-leak-in-webview and
 * http://code.google.com/p/android/issues/detail?id=9375
 * Note that the bug does NOT appear to be fixed in android 2.2 as romain claims
 * 
 * Also, you must call {@link #destroy()} from your activity's onDestroy method.
 */
public class NonLeakingWebView extends WebView {
	private static final String TAG = NonLeakingWebView.class.toString();
	private static Field sConfigCallback;
	private ZoomButtonsController zoom_controll;
	private static boolean showZoom;

	static {
		try {
			sConfigCallback = Class.forName("android.webkit.BrowserFrame").getDeclaredField("sConfigCallback");
			sConfigCallback.setAccessible(true);
		} catch (Exception e) {
			// ignored
		}

	}

	public NonLeakingWebView(Context context) {
		super(context);
		if (!isInEditMode()) {
			setWebViewClient(new MyWebViewClient((Activity) context));
		}
	}

	public NonLeakingWebView(Context context, AttributeSet attrs) {
		super(context, attrs);
		if (!isInEditMode()) {
			setWebViewClient(new MyWebViewClient((Activity) context));
		}
	}

	public NonLeakingWebView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		if (!isInEditMode()) {
			setWebViewClient(new MyWebViewClient((Activity) context));
		}
	}

	@Override
	public void destroy() {
		super.destroy();

		try {
			if (sConfigCallback != null)
				sConfigCallback.set(null, null);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Set option to display zoom control
	 * http://stackoverflow.com/a/11901948
	 * 
	 * @param show
	 */
	public void setDisplayZoomControl(boolean show) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			this.getSettings().setDisplayZoomControls(show);
		} else {
			// get the control
			try {
				Class webview = Class.forName("android.webkit.WebView");
				Method method = webview.getMethod("getZoomButtonsController");
				zoom_controll = (ZoomButtonsController) method.invoke(this, null);
				showZoom = show;
			} catch (Exception e) {
				Log.e(TAG, "Error when getting zoom control", e);
			}
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		super.onTouchEvent(ev);
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB && zoom_controll != null) {
			// Hide the controlls AFTER they where made visible by the default implementation.
			zoom_controll.setVisible(showZoom);
		}
		return true;
	}

	protected static class MyWebViewClient extends WebViewClient {
		protected WeakReference<Activity> activityRef;

		public MyWebViewClient(Activity activity) {
			this.activityRef = new WeakReference<Activity>(activity);
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			try {
				final Activity activity = activityRef.get();
				if (activity != null)
					activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
			} catch (RuntimeException ignored) {
				// ignore any url parsing exceptions
			}
			return true;
		}
	}
}