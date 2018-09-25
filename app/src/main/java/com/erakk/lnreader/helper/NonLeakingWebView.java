/**
 * Taken from http://stackoverflow.com/a/8949378
 */
package com.erakk.lnreader.helper;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebHistoryItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.widget.ZoomButtonsController;

import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.model.PageModel;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;

/**
 * see http://stackoverflow.com/questions/3130654/memory-leak-in-webview and
 * http://code.google.com/p/android/issues/detail?id=9375
 * Note that the bug does NOT appear to be fixed in android 2.2 as romain claims
 * <p/>
 * Also, you must call {@link #destroy()} from your activity's onDestroy method.
 */
public class NonLeakingWebView extends WebView {
    private static final String TAG = NonLeakingWebView.class.toString();
    private static Field sConfigCallback;
    private ZoomButtonsController zoom_control;
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
        init(context);
    }

    public NonLeakingWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public NonLeakingWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        if (!isInEditMode()) {
            setWebViewClient(new MyWebViewClient((Activity) context));

            // Create our ScaleGestureDetector
            mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());

            // fake user agent to mobile
            String userAgent = this.getSettings().getUserAgentString();
            if (!userAgent.contains("Mobile")) {
                if (userAgent.contains("Safari")) {
                    userAgent = userAgent.replace("Safari", "Mobile Safari");
                } else {
                    userAgent = userAgent + " Mobile Safari/537.16";
                }
                this.getSettings().setUserAgentString(userAgent);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // allow to open local file even in https mode.
                this.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            }
        }
    }

    @Override
    public void destroy() {
        Log.d(TAG, "Destroying WebView");
        setDisplayZoomControl(false);
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
    @SuppressLint("NewApi")
    public void setDisplayZoomControl(boolean show) {
        this.getSettings().setDisplayZoomControls(show);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // TODO: Error when exiting the current activity with zoom control shown.
        // E/WindowManager(6797): android.view.WindowLeaked: Activity
        // com.erakk.lnreader.UI.activity.DisplayImageActivity has leaked window
        // android.widget.ZoomButtonsController$Container{41c709a0 V.E..... ........ 0,0-540,73} that was originally
        // added here
        super.onTouchEvent(ev);
        try {
            checkZoomEvent(ev);
        } catch (IllegalArgumentException ex) {
            Log.e(TAG, "Failed to handle zoom event.", ex);
        }
        return true;
    }

    public static final String PREFIX_PAGEMODEL = "pageModel:";

    @Override
    public boolean canGoBack() {
        WebBackForwardList history = this.copyBackForwardList();
        WebHistoryItem prevData = history.getItemAtIndex(history.getCurrentIndex() - 1);
        if (prevData != null) {
            if (prevData.getUrl().startsWith(PREFIX_PAGEMODEL)) {
                Log.d(TAG, "Previous data is internal page: " + prevData.getUrl());
                return false;
            } else
                Log.d(TAG, "Back to: " + prevData.getUrl());
        }

        return super.canGoBack();
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
                Log.d(TAG, ignored.getMessage(), ignored);
            }
            return true;
        }
    }

    /**
     * Enable onScaleChange for pinch zoom
     * http://android-developers.blogspot.sg/2010/06/making-sense-of-multitouch.html
     */
    private float mPosX;
    private float mPosY;
    private float mLastTouchX;
    private float mLastTouchY;
    private static final int INVALID_POINTER_ID = -1;
    // The �active pointer� is the one currently moving our object.
    private int mActivePointerId = INVALID_POINTER_ID;
    private ScaleGestureDetector mScaleDetector;
    private float mScaleFactor = 1.f;

    private void checkZoomEvent(MotionEvent ev) {
        // Let the ScaleGestureDetector inspect all events.

        mScaleFactor = this.getScale();
        mScaleDetector.onTouchEvent(ev);

        final int action = ev.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                final float x = ev.getX();
                final float y = ev.getY();

                mLastTouchX = x;
                mLastTouchY = y;
                mActivePointerId = ev.getPointerId(0);
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                try {
                    final int pointerIndex = ev.findPointerIndex(mActivePointerId);
                    final float x = ev.getX(pointerIndex);
                    final float y = ev.getY(pointerIndex);

                    // Only move if the ScaleGestureDetector isn't processing a gesture.
                    if (!mScaleDetector.isInProgress()) {
                        final float dx = x - mLastTouchX;
                        final float dy = y - mLastTouchY;

                        mPosX += dx;
                        mPosY += dy;

                        invalidate();
                    }

                    mLastTouchX = x;
                    mLastTouchY = y;
                } catch (ArrayIndexOutOfBoundsException ex) {
                    Log.i(TAG, "Failed to get the motion event");
                }

                break;
            }

            case MotionEvent.ACTION_UP: {
                mActivePointerId = INVALID_POINTER_ID;
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                mActivePointerId = INVALID_POINTER_ID;
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK)
                        >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                final int pointerId = ev.getPointerId(pointerIndex);
                if (pointerId == mActivePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mLastTouchX = ev.getX(newPointerIndex);
                    mLastTouchY = ev.getY(newPointerIndex);
                    mActivePointerId = ev.getPointerId(newPointerIndex);
                }
                break;
            }
        }
    }

    private WebViewClient currentWebClient = null;

    @Override
    public void setWebViewClient(WebViewClient client) {
        super.setWebViewClient(client);
        this.currentWebClient = client;
    }

    public WebViewClient getWebViewClient() {
        return this.currentWebClient;
    }

    private WebChromeClient currentWebChromeClient;

    @Override
    public void setWebChromeClient(WebChromeClient client) {
        super.setWebChromeClient(client);
        this.currentWebChromeClient = client;
    }

    public WebChromeClient getWebChromeClient() {
        return this.currentWebChromeClient;
    }


    private void triggerOnScaleChanged(float oldScale, float newScale) {
        if (currentWebClient != null) {
            currentWebClient.onScaleChanged(this, oldScale, newScale);
        }
    }

    /**
     * Save as webarchive, only works for API 11++
     *
     * @param page
     */
    public void saveMyWebArchive(String page) {
        if (page == null) {
            Log.w(TAG, "Empty page name, trying to resolve from current webView url!");

            String url = this.getUrl();
            if (Util.isStringNullOrEmpty(url)) {
                Log.w(TAG, "Empty Url!");
                return;
            }
            page = url;
        }

        if (!page.startsWith("http")) {
            Log.w(TAG, "Skipping non-http url: " + page);
            return;
        }

        Log.d(TAG, "Trying to save: " + page);
        try {
            PageModel pageModel = new PageModel(page);
            pageModel = NovelsDao.getInstance().getExistingPageModel(pageModel, null);
            if (pageModel != null && !pageModel.isExternal()) {
                Log.w(TAG, "PageModel: " + pageModel.getPage() + " is not external!");
                return;
            }
        } catch (Exception e1) {
            Log.e(TAG, "Failed to load page model: " + page, e1);
        }

        try {
            final String wacName = getWacNameForSaving(Util.SanitizeBaseUrl(page), false);
            final String p2 = Util.SanitizeBaseUrl(page);
            final ValueCallback<String> callback = new ValueCallback<String>() {

                @Override
                public void onReceiveValue(String value) {
                    Log.i(TAG, "Saving url: " + p2 + " ==> Saved to: " + value);
                    if(value == null) {
                        // try to resave
                        Log.w(TAG, "Receive null value callback, trying to re-save...");
                        saveWebArchive(wacName, false, this);
                    }
                    else {
                        Toast.makeText(LNReaderApplication.getInstance().getApplicationContext(), "Page saved to: " + value, Toast.LENGTH_SHORT).show();
                    }
                }
            };
            this.saveWebArchive(wacName, false, callback);
        } catch (Exception e) {
            Log.e(TAG, "Failed to save external page: " + page, e);
        }
    }

    private String getWacNameForSaving(String url, boolean refresh) {
        String path = UIHelper.getImageRoot(LNReaderApplication.getInstance()) + "/wac";
        File f = new File(path);
        if (!f.exists())
            f.mkdirs();
        Log.i(TAG, "WAC dirs: " + path);

        String filename = path + "/" + Util.getBaseWacName(url);
        String extension = ".wac";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            extension = ".mht";
        }
        return filename + extension;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float oldScale = mScaleFactor;
            mScaleFactor *= detector.getScaleFactor();

            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(0.5f, Math.min(mScaleFactor, 5.0f));

            triggerOnScaleChanged(oldScale, mScaleFactor);
            return true;
        }
    }


}