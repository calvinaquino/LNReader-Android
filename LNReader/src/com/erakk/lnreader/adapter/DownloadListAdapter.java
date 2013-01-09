package com.erakk.lnreader.adapter;

import java.util.ArrayList;

import com.erakk.lnreader.R;
import com.erakk.lnreader.R.layout;
import com.erakk.lnreader.R.menu;
import com.erakk.lnreader.model.DownloadModel;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

public class DownloadListAdapter extends ArrayAdapter<DownloadModel> {
        private ArrayList<DownloadModel> downloads;

        public DownloadListAdapter(Context context, int textViewResourceId, ArrayList<DownloadModel> items) {
                super(context, textViewResourceId, items);
                this.downloads = items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater vi = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = vi.inflate(R.layout.download_list_item, null);
                }
                DownloadModel d = downloads.get(position);
                if (d != null) {
                        TextView name = (TextView) v.findViewById(R.id.download_name);
                        ProgressBar progress = (ProgressBar) v.findViewById(R.id.download_progress_bar);
                        if (name != null) {
                        	name.setText(d.getDownloadName());                            }
                        if(progress != null){
                        	progress.setProgress(d.getDownloadProgress());
                        }
                }
                return v;
        }
}
