package com.erakk.lnreader.helper;

import android.os.Build;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.erakk.lnreader.R;
import com.erakk.lnreader.UI.activity.DisplayLightNovelContentActivity;

import java.lang.reflect.Method;

public class DisplayNovelContentUIHelper {
    private static final String TAG = DisplayNovelContentUIHelper.class.toString();

    private final DisplayLightNovelContentActivity parent;

    private final Handler mHandler = new Handler();
    private ImageButton goTop;
    private ImageButton goBottom;
    private Runnable hideBottom;
    private Runnable hideTop;
    private Runnable hideToolbarDelayed;

    public DisplayNovelContentUIHelper(DisplayLightNovelContentActivity parent) {
        this.parent = parent;
    }

    // region Compatibility search method for older android version
    public void prepareCompatSearchBox(final WebView webView) {

        final EditText searchText = (EditText) parent.findViewById(R.id.searchText);
        searchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                search(webView, searchText.getText().toString());
                return false;
            }
        });
    }

    @SuppressWarnings("deprecation")
    private void search(WebView webView, String string) {
        if (string != null && string.length() > 0)
            webView.findAll(string);

        try {
            Method m = NonLeakingWebView.class.getMethod("setFindIsUp", Boolean.TYPE);
            m.invoke(webView, true);
        } catch (NoSuchMethodException me) {
        } catch (Exception e) {
            Log.e(TAG, "Error when searching", e);
        }
    }

    public void closeSearchBox(WebView webView) {
        RelativeLayout searchBox = (RelativeLayout) parent.findViewById(R.id.searchBox);
        searchBox.setVisibility(View.GONE);
        webView.clearMatches();
    }

    // endregion Compatibility search method for older android version

    public void prepareTopDownButton() {
        goTop = (ImageButton) parent.findViewById(R.id.webview_go_top);
        goBottom = (ImageButton) parent.findViewById(R.id.webview_go_bottom);

        // Hide button after a certain time being shown
        hideBottom = new Runnable() {

            @Override
            public void run() {
                goBottom.setVisibility(ImageButton.GONE);
            }
        };
        hideTop = new Runnable() {

            @Override
            public void run() {
                goTop.setVisibility(ImageButton.GONE);
            }
        };
    }

    public void toggleTopButton(boolean enable) {
        if (enable) {
            goTop.setVisibility(ImageButton.VISIBLE);
            mHandler.removeCallbacks(hideTop);
            mHandler.postDelayed(hideTop, 1500);
        } else
            goTop.setVisibility(ImageButton.GONE);

        if (parent.getFullscreenPreferences()) {
            mHandler.removeCallbacks(hideToolbarDelayed);
            mHandler.post(hideToolbarDelayed);
        }
    }

    public void toggleBottomButton(boolean enable) {
        if (enable) {
            goBottom.setVisibility(ImageButton.VISIBLE);
            mHandler.removeCallbacks(hideBottom);
            mHandler.postDelayed(hideBottom, 1500);
        } else
            goBottom.setVisibility(ImageButton.GONE);

        if (parent.getFullscreenPreferences()) {
            mHandler.removeCallbacks(hideToolbarDelayed);
            mHandler.post(hideToolbarDelayed);
        }
    }

    public void toggleFullscreen(boolean hideToolbar) {
        Log.d(TAG, "Hide Toolbar: " + hideToolbar);

        if (hideToolbar) {
            hideToolbar();
        } else {
            if (Build.VERSION.SDK_INT >= 11)
                parent.getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(null);
            showToolbar();
        }
    }

    private void hideToolbar() {
        final ActionBar actionBar = parent.getSupportActionBar();
        if (actionBar == null) return;
        if (!actionBar.isShowing()) return;

        final Animation mSlideUp = AnimationUtils.loadAnimation(parent, R.anim.abc_slide_out_top);
        final Toolbar mToolBar = (Toolbar) parent.findViewById(R.id.toolbar);
        final View root = parent.findViewById(R.id.txtDebug);
        final View decorView = parent.getWindow().getDecorView();

        if (root == null || decorView == null) return;

        mToolBar.startAnimation(mSlideUp);
        actionBar.hide();

        // claim the empty space from toolbar.
        if (parent.getFullscreenPreferences()) {
            final ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) root.getLayoutParams();
            lp.topMargin = 0;
            lp.bottomMargin = 0;
            root.setLayoutParams(lp);
        }

        parent.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        if (Build.VERSION.SDK_INT >= 14) {
            int uiFlag = decorView.getSystemUiVisibility();
            uiFlag ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;

            if (Build.VERSION.SDK_INT >= 16) {
                uiFlag ^= View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            }
            if (Build.VERSION.SDK_INT >= 19) {
                uiFlag ^= View.SYSTEM_UI_FLAG_IMMERSIVE;
            }
            decorView.setSystemUiVisibility(uiFlag);
        }
    }

    private void showToolbar() {
        final ActionBar actionBar = parent.getSupportActionBar();
        if (actionBar == null) return;

        // auto hide only if fullscreen is enabled
        mHandler.removeCallbacks(hideToolbarDelayed);
        if (parent.getFullscreenPreferences()) {
            mHandler.postDelayed(hideToolbarDelayed, 10000);
        }

        final Toolbar mToolBar = (Toolbar) parent.findViewById(R.id.toolbar);
        final View root = parent.findViewById(R.id.txtDebug);

        // adjust the layout to provide empty space for the toolbar
        if (!parent.getFullscreenPreferences()) {
            final ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) root.getLayoutParams();
            final TypedValue tv = new TypedValue();
            parent.getTheme().resolveAttribute(R.attr.actionBarSize, tv, true);
            lp.topMargin = TypedValue.complexToDimensionPixelSize(tv.data, parent.getResources().getDisplayMetrics());
            root.setLayoutParams(lp);

            // also nav bar?
            // http://stackoverflow.com/a/29609679
        }

        if (actionBar.isShowing()) return;

        final Animation mSlideDown = AnimationUtils.loadAnimation(parent, R.anim.abc_slide_in_top);
        final View decorView = parent.getWindow().getDecorView();

        if (root == null || decorView == null) return;

        mToolBar.startAnimation(mSlideDown);
        actionBar.show();


        parent.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= 14) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    public void prepareFullscreenHandler(NonLeakingWebView webView) {
        hideToolbarDelayed = new Runnable() {

            @Override
            public void run() {
                hideToolbar();
            }
        };

        /// adapted from http://stackoverflow.com/a/16485989
        webView.setOnTouchListener(new View.OnTouchListener() {
            private float mDownX;
            private float mDownY;
            private final float SCROLL_THRESHOLD = 10;
            private boolean isOnClick;

            @Override
            public boolean onTouch(View v, MotionEvent ev) {

                switch (ev.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        mDownX = ev.getX();
                        mDownY = ev.getY();
                        isOnClick = true;
                        break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                        if (isOnClick) {
//                            Log.i(TAG, "onClick ");
                            showToolbar();
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (isOnClick && (Math.abs(mDownX - ev.getX()) > SCROLL_THRESHOLD || Math.abs(mDownY - ev.getY()) > SCROLL_THRESHOLD)) {
//                            Log.i(TAG, "movement detected");
                            isOnClick = false;
                        }
                        break;
                    default:
                        break;
                }

                return false;
            }
        });
    }
}
