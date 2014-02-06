package com.erakk.lnreader.adapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import com.erakk.lnreader.R;
import com.erakk.lnreader.model.FindMissingModel;

public class FindMissingAdapter extends ArrayAdapter<FindMissingModel>{

	private final int layoutResourceId;
	private final Context context;
	private List<FindMissingModel> data;

	public FindMissingAdapter(Context context, int resourceId, List<FindMissingModel> objects) {
		super(context, resourceId, objects);
		this.layoutResourceId = resourceId;
		this.context = context;
		this.data = objects;
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
		View row = convertView;
		FindMissingModelHolder holder = new FindMissingModelHolder();

		final FindMissingModel model = data.get(position);

		LayoutInflater inflater = ((Activity) context).getLayoutInflater();
		row = inflater.inflate(layoutResourceId, parent, false);

		holder.txtTitle = (TextView) row.findViewById(R.id.title);
		if (holder.txtTitle != null) {
			holder.txtTitle.setText(model.getTitle());
		}

		holder.txtDetails = (TextView) row.findViewById(R.id.details);
		if (holder.txtDetails != null) {
			holder.txtDetails.setText(model.getDetails());
		}

		holder.chkSelection = (CheckBox) row.findViewById(R.id.chk_selection);
		if (holder.chkSelection != null) {
			holder.chkSelection.setVisibility(View.VISIBLE);
			holder.chkSelection.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					model.setSelected(isChecked);
				}
			});
		}

		row.setTag(holder);
		return row;
	}

	static class FindMissingModelHolder {
		TextView txtTitle;
		TextView txtDetails;
		CheckBox chkSelection;
	}
}
