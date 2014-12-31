package com.erakk.lnreader.helper;

import android.content.Context;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.model.BookmarkModel;

import java.io.File;
import java.util.ArrayList;

public class DisplayNovelContentHtmlHelper {
    private static final String TAG = DisplayNovelContentHtmlHelper.class.toString();

    public static String getViewPortMeta() {
        return "<meta name='viewport' content='width=device-width, minimum-scale=0.1, maximum-scale=10.0' id='viewport-meta'/>";
    }

    /**
     * Prepare javascript to enable highlighting and setting up bookmarks.
     *
     * @param lastPos
     * @param bookmarks
     * @param enableBookmark
     * @return
     */
    public static String prepareJavaScript(int lastPos, ArrayList<BookmarkModel> bookmarks, boolean enableBookmark) {
        StringBuilder scriptBuilder = new StringBuilder();

        scriptBuilder.append("<script type='text/javascript'>");
        scriptBuilder.append(String.format("var isBookmarkEnabled = %s;", enableBookmark));
        scriptBuilder.append("\n");

        String bookmarkJs = "var bookmarkCol = [%s];";
        if (bookmarks != null && bookmarks.size() > 0) {
            ArrayList<Integer> list = new ArrayList<Integer>();
            for (BookmarkModel bookmark : bookmarks) {
                list.add(bookmark.getpIndex());
            }
            bookmarkJs = String.format(bookmarkJs, Util.join(list, ","));
        } else {
            bookmarkJs = String.format(bookmarkJs, "");
        }
        scriptBuilder.append(bookmarkJs);
        scriptBuilder.append("\n");

        String lastPosJs = String.format("var lastPos = %s;", lastPos > 0 ? lastPos : 0);
        scriptBuilder.append(lastPosJs);
        scriptBuilder.append("\n");

        String js = LNReaderApplication.getInstance().ReadCss(R.raw.content_script);
        scriptBuilder.append(js);

        scriptBuilder.append("</script>");

        return scriptBuilder.toString();
    }


    /**
     * getCSSSheet() method will generate the CSS data into the <style> elements.
     * At the current moment, it reads the external data line by line then applies
     * it directly to the header.
     *
     * @return
     */
    public static String getCSSSheet() {
        Context ctx = LNReaderApplication.getInstance().getApplicationContext();
        if (getUseCustomCSS(ctx)) {
            String externalCss = getExternalCss();
            if (!Util.isStringNullOrEmpty(externalCss))
                return externalCss;
        }

        // Default CSS start here
        String key = "";
        int styleId = -1;
        StringBuilder css = new StringBuilder();

        css.append("<style type=\"text/css\">");

        if (UIHelper.getCssUseCustomColorPreferences(ctx)) {
            styleId = R.raw.style_custom_color;
            key = "style_custom_color" + UIHelper.getBackgroundColor(ctx) + UIHelper.getForegroundColor(ctx) + UIHelper.getLinkColor(ctx) + UIHelper.getThumbBorderColor(ctx) + UIHelper.getThumbBackgroundColor(ctx);
        } else if (UIHelper.getColorPreferences(ctx)) {
            styleId = R.raw.style_dark;
            key = "style_dark";
        } else {
            styleId = R.raw.style;
            key = "style";
        }

        // check if exists in css cache
        if (UIHelper.CssCache.containsKey(key))
            return UIHelper.CssCache.get(key);

        // build the css
        css.append(LNReaderApplication.getInstance().ReadCss(styleId));

        if (getUseJustifiedPreferences(ctx)) {
            css.append("\nbody { text-align: justify !important; }\n");
        }
        css.append("\np { line-height:" + getLineSpacingPreferences(ctx) + "% !important; \n");
        css.append("      font-family:" + getContentFontPreferences(ctx) + "; }\n");
        css.append("\nbody { margin: " + getMarginPreferences(ctx) + "% !important; }\n");

        css.append("\n.mw-headline{ font-family: " + getHeadingFontPreferences(ctx) + "; }\n");

        css.append("</style>");

        String cssStr = css.toString();

        // replace custom color if enabled.
        if (UIHelper.getCssUseCustomColorPreferences(ctx)) {
            cssStr = cssStr.replace("@background@", UIHelper.getBackgroundColor(ctx));
            cssStr = cssStr.replace("@foreground@", UIHelper.getForegroundColor(ctx));
            cssStr = cssStr.replace("@link@", UIHelper.getLinkColor(ctx));
            cssStr = cssStr.replace("@thumb-border@", UIHelper.getThumbBorderColor(ctx));
            cssStr = cssStr.replace("@thumb-back@", UIHelper.getThumbBackgroundColor(ctx));
        }

        UIHelper.CssCache.put(key, cssStr);
        return cssStr;
    }

    /**
     * link to external CSS file, not cached.
     *
     * @return <link rel="stylesheet" href="file://EXTERNAL-CSS-PATH">
     */
    public static String getExternalCss() {
        Context ctx = LNReaderApplication.getInstance().getApplicationContext();
        String cssPath = PreferenceManager.getDefaultSharedPreferences(ctx).getString(Constants.PREF_CUSTOM_CSS_PATH, Environment.getExternalStorageDirectory().getPath() + "/custom.css");
        if (!Util.isStringNullOrEmpty(cssPath)) {
            File cssFile = new File(cssPath);
            if (cssFile.exists()) {
                String external = String.format("<link rel=\"stylesheet\" href=\"file://%s\">", cssPath);
                Log.d(TAG, "External CSS: " + external);
                return external;
            }
        }
        // should not hit this code, either external css not exists or failed to read.
        Toast.makeText(ctx, ctx.getResources().getString(R.string.css_layout_not_exist), Toast.LENGTH_SHORT).show();
        return null;
    }

    private static boolean getUseCustomCSS(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(Constants.PREF_USE_CUSTOM_CSS, false);
    }

    private static float getLineSpacingPreferences(Context ctx) {
        return Float.parseFloat(PreferenceManager.getDefaultSharedPreferences(ctx).getString(Constants.PREF_LINESPACING, "150"));
    }

    private static boolean getUseJustifiedPreferences(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(Constants.PREF_FORCE_JUSTIFIED, false);
    }

    private static float getMarginPreferences(Context ctx) {
        return Float.parseFloat(PreferenceManager.getDefaultSharedPreferences(ctx).getString(Constants.PREF_MARGINS, "5"));
    }

    private static String getHeadingFontPreferences(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getString(Constants.PREF_HEADING_FONT, "serif");
    }

    private static String getContentFontPreferences(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getString(Constants.PREF_CONTENT_FONT, "sans-serif");
    }
}
