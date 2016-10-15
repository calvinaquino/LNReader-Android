package com.erakk.lnreader.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.R;
import com.erakk.lnreader.helper.Util;
import com.erakk.lnreader.model.FindMissingModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FindMissingAdapter extends ArrayAdapter<FindMissingModel> {

    protected static final String TAG = FindMissingAdapter.class.toString();
    private final int layoutResourceId;
    private final Context context;
    private List<FindMissingModel> data;
    public FindMissingModel[] originalData = new FindMissingModel[0];
    private final String mode;
    private boolean showDownloaded = false;
    private boolean showEverythingElse = true;

    public FindMissingAdapter(Context context, int resourceId, List<FindMissingModel> objects, String extra, boolean dowloadSelected, boolean elseSelected) {
        super(context, resourceId, objects);
        this.layoutResourceId = resourceId;
        this.context = context;
        this.showDownloaded = dowloadSelected;
        this.showEverythingElse = elseSelected;
        this.mode = extra;
        this.data = objects;
        this.originalData = data.toArray(originalData);
        filterData();
    }

    public List<FindMissingModel> getItems() {
        return data;
    }

    @Override
    public void addAll(Collection<? extends FindMissingModel> objects) {
        synchronized (this) {
            if (data == null) {
                data = new ArrayList<FindMissingModel>();
            }
            data.addAll(objects);

            this.notifyDataSetChanged();
        }
    }

    @Override
    public void addAll(FindMissingModel... objects) {
        synchronized (this) {
            if (data == null) {
                data = new ArrayList<FindMissingModel>();
            }

            for (FindMissingModel item : objects) {
                data.add(item);
            }

            this.notifyDataSetChanged();
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            convertView = inflater.inflate(layoutResourceId, parent, false);
        }

        FindMissingModelHolder holder = new FindMissingModelHolder();
        final FindMissingModel model = data.get(position);

        holder.txtTitle = (TextView) convertView.findViewById(R.id.title);
        if (holder.txtTitle != null) {
            holder.txtTitle.setText(model.getTitle());
            if (mode.equalsIgnoreCase(Constants.PREF_MISSING_CHAPTER))
                holder.txtTitle.setTextColor(Constants.COLOR_MISSING);
            else if (mode.equalsIgnoreCase(Constants.PREF_REDLINK_CHAPTER))
                holder.txtTitle.setTextColor(Constants.COLOR_REDLINK);
            else {
                holder.txtTitle.setTextColor(Constants.COLOR_READ);
            }
        }

        holder.txtDetails = (TextView) convertView.findViewById(R.id.details);
        if (holder.txtDetails != null) {
            String details = model.getDetails();
            if (Util.isStringNullOrEmpty(details))
                details = String.format("%s (%s)", model.getPage(), model.getId());
            holder.txtDetails.setText(details);
        }

        holder.imgIsDownloaded = (ImageView) convertView.findViewById(R.id.is_downloaded);
        if (holder.imgIsDownloaded != null) {
            if (model.isDownloaded()) {
                holder.imgIsDownloaded.setVisibility(View.VISIBLE);
            } else {
                holder.imgIsDownloaded.setVisibility(View.GONE);
            }
        }

        holder.chkSelection = (CheckBox) convertView.findViewById(R.id.chk_selection);
        if (holder.chkSelection != null) {
            holder.chkSelection.setChecked(model.isSelected());
            holder.chkSelection.setVisibility(View.VISIBLE);
            holder.chkSelection.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    model.setSelected(isChecked);
                }
            });
        }

        convertView.setTag(holder);
        return convertView;
    }

    public void filterDownloaded(boolean value) {
        this.showDownloaded = value;
        filterData();
    }

    public void filterEverythingElse(boolean value) {
        this.showEverythingElse = value;
        filterData();
    }

    private void filterData() {
        synchronized (this) {
            this.clear();
            data.clear();
            for (FindMissingModel item : originalData) {
                if (item.isDownloaded() && this.showDownloaded) {
                    data.add(item);
                } else if (!item.isDownloaded() && this.showEverythingElse) {
                    data.add(item);
                }
            }
            this.notifyDataSetChanged();
        }
    }

    static class FindMissingModelHolder {
        TextView txtTitle;
        TextView txtDetails;
        ImageView imgIsDownloaded;
        CheckBox chkSelection;
    }
}
