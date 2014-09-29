package com.erakk.lnreader.helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

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

public class DisplayNovelContentHtmlHelper {
	private static final String TAG = DisplayNovelContentHtmlHelper.class.toString();

	/***
	 * Prepare javascript to enable highlighting and setting up bookmarks.
	 * 
	 * @param lastPos
	 * @param bookmarks
	 * @param enableBookmark
	 * @return
	 */
	public static String prepareJavaScript(int lastPos, ArrayList<BookmarkModel> bookmarks, boolean enableBookmark) {
		String script = "<script type='text/javascript'>";
		String js = LNReaderApplication.getInstance().ReadCss(R.raw.content_script);

		String bookmarkEnabledJs = String.format("var isBookmarkEnabled = %s;", enableBookmark);

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

		String lastPosJs = "var lastPos = %s;";
		if (lastPos > 0) {
			lastPosJs = String.format(lastPosJs, lastPos);
			Log.d(TAG, "Last Position: " + lastPos);
		} else {
			lastPosJs = String.format(lastPosJs, "0");
		}

		script += bookmarkEnabledJs + "\n" + bookmarkJs + "\n" + lastPosJs + "\n" + js;
		script += "</script>";

		return script;
	}


	/**
	 * getCSSSheet() method will put all the CSS data into the HTML header. At
	 * the current moment, it reads the external data line by line then applies
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

		if (UIHelper.getCssUseCustomColorPreferences(ctx)) {
			styleId = R.raw.style_custom_color;
			key = "style_custom_color" + UIHelper.getBackgroundColor(ctx) + UIHelper.getForegroundColor(ctx) + UIHelper.getLinkColor(ctx) + UIHelper.getThumbBorderColor(ctx) + UIHelper.getThumbBackgroundColor(ctx);
		}
		else if (UIHelper.getColorPreferences(ctx)) {
			styleId = R.raw.style_dark;
			key = "style_dark";
		}
		else {
			styleId = R.raw.style;
			key = "style";
		}
		if (UIHelper.CssCache.containsKey(key))
			return UIHelper.CssCache.get(key);

		css.append(LNReaderApplication.getInstance().ReadCss(styleId));

		if (getUseJustifiedPreferences(ctx)) {
			css.append("\nbody { text-align: justify !important; }\n");
		}
		css.append("\np { line-height:" + getLineSpacingPreferences(ctx) + "% !important; \n");
		css.append("      font-family:" + getContentFontPreferences(ctx) + "; }\n");
		css.append("\nbody { margin: " + getMarginPreferences(ctx) + "% !important; }\n");

		css.append("\n.mw-headline{ font-family: " + getHeadingFontPreferences(ctx) + "; }\n");

		String cssStr = css.toString();
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

	/***
	 * Get external CSS file, not cached.
	 * 
	 * @return
	 */
	public static String getExternalCss() {
		Context ctx = LNReaderApplication.getInstance().getApplicationContext();
		StringBuilder css = new StringBuilder();
		String cssPath = PreferenceManager.getDefaultSharedPreferences(ctx).getString(Constants.PREF_CUSTOM_CSS_PATH, Environment.getExternalStorageDirectory().getPath() + "/custom.css");
		if (!Util.isStringNullOrEmpty(cssPath)) {
			File cssFile = new File(cssPath);
			if (cssFile.exists()) {
				// read the file
				BufferedReader br = null;
				FileReader fr = null;
				try {
					try {
						fr = new FileReader(cssFile);
						br = new BufferedReader(fr);
						String line;

						while ((line = br.readLine()) != null) {
							css.append(line);
						}

						return css.toString();

					} catch (Exception e) {
						throw e;
					} finally {
						if (fr != null)
							fr.close();
						if (br != null)
							br.close();
					}
				} catch (Exception e) {
					Log.e(TAG, "Error when reading Custom CSS: " + cssPath, e);
				}
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
