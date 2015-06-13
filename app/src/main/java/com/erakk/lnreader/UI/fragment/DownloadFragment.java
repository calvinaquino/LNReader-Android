package com.erakk.lnreader.UI.fragment;


import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.adapter.DownloadListAdapter;
import com.erakk.lnreader.model.DownloadModel;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class DownloadFragment extends Fragment {

    private static final String TAG = DownloadFragment.class.toString();
    ArrayList<DownloadModel> downloadList;
    ListView downloadListView;
    DownloadListAdapter adapter;

    public DownloadFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_download_list, null);
        downloadListView = (ListView) view.findViewById(R.id.download_list);
        downloadList = LNReaderApplication.getInstance().getDownloadList();
        updateContent();

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        ((AppCompatActivity)activity).getSupportActionBar().setTitle(R.string.download_list);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }


    public void updateContent() {
        try {
            int resourceId = R.layout.download_list_item;
            adapter = new DownloadListAdapter(getActivity(), resourceId, downloadList);
            downloadListView.setAdapter(adapter);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            Toast.makeText(getActivity(), getResources().getString(R.string.error_update) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
