package com.erakk.lnreader.task;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.callback.CallbackEventData;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.callback.IExtendedCallbackNotifier;
import com.erakk.lnreader.helper.MHTUtil;
import com.erakk.lnreader.helper.WebArchiveReader;

import java.io.File;
import java.io.FileInputStream;

public class LoadWacTask extends AsyncTask<Void, ICallbackEventData, AsyncTaskResult<Boolean>> implements ICallbackNotifier {
    private static final String TAG = LoadWacTask.class.toString();
    private final WebView wv;
    private final String wacName;
    private final IExtendedCallbackNotifier<AsyncTaskResult<?>> owner;
    private final WebArchiveReader wr;
    private final String source;
    private final String anchorLink;
    private final String historyUrl;
    private String extractedMhtName;

    public LoadWacTask(IExtendedCallbackNotifier<AsyncTaskResult<?>> owner, WebView wv, String wacName, final WebViewClient client, String anchorLink, String historyUrl) {
        this.wv = wv;
        this.wacName = wacName;
        this.owner = owner;
        this.anchorLink = anchorLink;
        this.historyUrl = historyUrl;

        wr = new WebArchiveReader() {

            @Override
            protected void onFinished(WebView webView) {
                webView.setWebViewClient(client);
                Log.d(TAG, "WAC loaded");
            }
        };
        this.source = TAG + ":" + wacName;
    }

    @Override
    protected void onPreExecute() {
        // executed on UI thread.
        owner.onProgressCallback(new CallbackEventData("", source));
    }

    @Override
    protected AsyncTaskResult<Boolean> doInBackground(Void... arg0) {
        return new AsyncTaskResult<Boolean>(loadFromWac(this.wacName), Boolean.class);
    }

    @Override
    protected void onProgressUpdate(ICallbackEventData... values) {
        owner.onProgressCallback(values[0]);
    }

    private boolean loadFromWac(String wacName) {
        String msg = "Loading from web archive: " + wacName;
        Log.i(TAG, msg);
        publishProgress(new CallbackEventData(msg, source));
        try {
            if (wacName.endsWith(".mht")) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    // Kitkat able to open mht directly...
                    return true;
                } else {
                    Log.i(TAG, "Using MHT Loader");
                    String tempPath = UIHelper.getImageRoot(LNReaderApplication.getInstance().getApplicationContext()) + "/wac/temp";
                    File f = new File(tempPath);
                    if (!f.exists())
                        f.mkdirs();
                    File w = new File(wacName);
                    extractedMhtName = MHTUtil.exportHtml(wacName, tempPath, w.getName());
                    Log.d(TAG, "Exported to: " + extractedMhtName);
                    return true;
                }
            } else if (wacName.endsWith(".wac")) {
                Log.i(TAG, "Using WAC Loader");
                FileInputStream is;
                is = new FileInputStream(wacName);
                return wr.readWebArchive(is);
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to load saved web archive: " + wacName, e);
        }
        return false;
    }

    @Override
    protected void onPostExecute(AsyncTaskResult<Boolean> result) {
        String message = null;
        if (result.getResult()) {
            if (wacName.endsWith(".mht")) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    wv.loadUrl("file://" + wacName + "#" + anchorLink);
                } else
                    wv.loadUrl("file://" + Uri.encode(extractedMhtName, "/\\") + "#" + anchorLink);
            } else {
                wr.loadToWebView(wv, anchorLink, historyUrl);
            }
            message = "Load from: " + wacName;
        } else {
            message = "Load WAC Failed";
        }

        owner.onCompleteCallback(new CallbackEventData(message, source), result);
    }

    @Override
    public void onProgressCallback(ICallbackEventData message) {
        publishProgress(message);
    }
}
