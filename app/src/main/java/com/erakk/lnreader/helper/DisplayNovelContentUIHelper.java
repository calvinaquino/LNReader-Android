package com.erakk.lnreader.helper;

import android.annotation.SuppressLint;
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

    private boolean isToolbarVisible;

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
    }

    public void toggleBottomButton(boolean enable) {
        if (enable) {
            goBottom.setVisibility(ImageButton.VISIBLE);
            mHandler.removeCallbacks(hideBottom);
            mHandler.postDelayed(hideBottom, 1500);
        } else
            goBottom.setVisibility(ImageButton.GONE);
    }

    @SuppressLint("NewApi")
    public void ToggleFullscreen(final boolean fullscreen) {
        Log.d(TAG, "Fullscreen: " + fullscreen);

        if (fullscreen) {
            isToolbarVisible = true;
            hideToolbar();
        } else {
            isToolbarVisible = false;
            parent.getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(null);
            showToolbar();
        }
    }

    private void hideToolbar() {
        if (!isToolbarVisible) return;

        final Animation mSlideUp = AnimationUtils.loadAnimation(parent, R.anim.abc_slide_out_top);
        final Toolbar mToolBar = (Toolbar) parent.findViewById(R.id.toolbar);
        final View root = parent.findViewById(R.id.root);
        final View decorView = parent.getWindow().getDecorView();
        final ActionBar actionBar = parent.getSupportActionBar();

        if (root == null || decorView == null) return;

        if (actionBar != null) {
            mToolBar.startAnimation(mSlideUp);
            actionBar.hide();
        }

        final ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) root.getLayoutParams();
        lp.topMargin = 0;
        root.setLayoutParams(lp);

        parent.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= 19) {
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN        // API 16
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION   // API 14
                            | View.SYSTEM_UI_FLAG_IMMERSIVE           // API 19
            );
        }

        isToolbarVisible = false;
    }

    private void showToolbar() {
        if (isToolbarVisible) return;

        final Animation mSlideDown = AnimationUtils.loadAnimation(parent, R.anim.abc_slide_in_top);
        final Toolbar mToolBar = (Toolbar) parent.findViewById(R.id.toolbar);

        final View root = parent.findViewById(R.id.root);
        final View decorView = parent.getWindow().getDecorView();
        final ActionBar actionBar = parent.getSupportActionBar();

        if (root == null || decorView == null) return;

        parent.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (actionBar != null) {
            mToolBar.startAnimation(mSlideDown);
            actionBar.show();
        }

        final ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) root.getLayoutParams();
        final TypedValue tv = new TypedValue();
        parent.getTheme().resolveAttribute(R.attr.actionBarSize, tv, true);
        lp.topMargin = TypedValue.complexToDimensionPixelSize(tv.data, parent.getResources().getDisplayMetrics());
        root.setLayoutParams(lp);

        if (Build.VERSION.SDK_INT >= 19) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }

        mHandler.removeCallbacks(hideToolbarDelayed);
        mHandler.postDelayed(hideToolbarDelayed, 4000);
        isToolbarVisible = true;
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
                        if (isOnClick && parent.getFullscreenPreferences()) {
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
