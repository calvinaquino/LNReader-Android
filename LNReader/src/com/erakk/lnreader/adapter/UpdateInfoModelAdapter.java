package com.erakk.lnreader.adapter;

import java.util.Iterator;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.erakk.lnreader.R;
import com.erakk.lnreader.helper.Util;
import com.erakk.lnreader.model.UpdateInfoModel;


public class UpdateInfoModelAdapter  extends ArrayAdapter<UpdateInfoModel> {
	//private static final String TAG = PageModelAdapter.class.toString();
	private Context context;
	private int layoutResourceId;
	public List<UpdateInfoModel> data;
	public UpdateInfoModelAdapter(Context context, int resourceId, List<UpdateInfoModel> objects) {
		super(context, resourceId, objects);
		this.layoutResourceId = resourceId;
		this.context = context;
		this.data = objects;
	}
	
	public void setLayout(int resourceId) {
		this.layoutResourceId = resourceId;
	}
	
	@SuppressLint("NewApi")
	public void addAll(List<UpdateInfoModel> objects) {
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB )
			super.addAll(objects);
		else {
			for(Iterator<UpdateInfoModel> iPage = objects.iterator(); iPage.hasNext();) {
				this.add(iPage.next());
			}
			this.notifyDataSetChanged();
		}
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		UpdateInfoModelHolder holder = null;

		final UpdateInfoModel page = data.get(position);
		
		LayoutInflater inflater = ((Activity)context).getLayoutInflater();
		row = inflater.inflate(layoutResourceId, parent, false);

		holder = new UpdateInfoModelHolder();
		holder.txtUpdateType = (TextView)row.findViewById(R.id.update_type);
		if(holder.txtUpdateType != null) {
			switch(page.getUpdateType()) {
				case New:
					holder.txtUpdateType.setText("New Chapter");
					break;
				case NewNovel:
					holder.txtUpdateType.setText("New Novel");
					break;
				case Updated:
					holder.txtUpdateType.setText("Updated Chapter");
					break;
				case UpdateTos:
					holder.txtUpdateType.setText("TOS Updated");
					break;
				case Deleted:
					holder.txtUpdateType.setText("Deleted Chapter");
					break;
				default:
					holder.txtUpdateType.setText("Unknown!");
					break;
			}			
		}
		
		holder.txtUpdateTitle = (TextView)row.findViewById(R.id.update_chapter);
		if(holder.txtUpdateTitle != null) {
			holder.txtUpdateTitle.setText(page.getUpdateTitle());
		}
		
		holder.txtUpdateDate = (TextView)row.findViewById(R.id.update_date);
		if(holder.txtUpdateDate != null) {
			holder.txtUpdateDate.setText("Update:" +  Util.formatDateForDisplay(page.getUpdateDate()));
		}
		
		row.setTag(holder);
		return row;
	}
	
	static class UpdateInfoModelHolder
	{
		TextView txtUpdateType;
		TextView txtUpdateTitle;
		TextView txtUpdateDate;
	}

	public void setResourceId (int id) {
		this.layoutResourceId = id;
	}
}
