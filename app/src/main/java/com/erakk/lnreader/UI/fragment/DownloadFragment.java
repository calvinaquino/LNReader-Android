package com.erakk.lnreader.UI.fragment;


import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.adapter.DownloadListAdapter;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.IExtendedCallbackNotifier;
import com.erakk.lnreader.model.DownloadModel;

/**
 * A simple {@link Fragment} subclass.
 */
public class DownloadFragment extends Fragment implements IExtendedCallbackNotifier<DownloadModel> {

    private static final String TAG = DownloadFragment.class.toString();
    ListView downloadListView;
    DownloadListAdapter adapter;

    public DownloadFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_download_list, null);
        downloadListView = (ListView) view.findViewById(R.id.download_list);
        updateContent();
        getActivity().setTitle(R.string.download_list);
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onResume() {
        super.onResume();
        LNReaderApplication.getInstance().setDownloadNotifier(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        LNReaderApplication.getInstance().setDownloadNotifier(null);
    }


    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void updateContent() {
        try {
            int resourceId = R.layout.item_download;
            adapter = new DownloadListAdapter(getActivity(), resourceId, LNReaderApplication.getInstance().getDownloadList());
            downloadListView.setAdapter(adapter);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            Toast.makeText(getActivity(), getResources().getString(R.string.error_update) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // region implementation of IExtendedCallbackNotifier<DownloadModel>

    @Override
    public void onCompleteCallback(ICallbackEventData message, DownloadModel result) {
        updateContent();
    }

    @Override
    public void onProgressCallback(ICallbackEventData message) {
        updateContent();
    }

    @Override
    public boolean downloadListSetup(String taskId, String message, int setupType, boolean hasError) {
        return false;
    }

    // endregion
}
