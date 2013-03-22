package com.erakk.lnreader.adapter;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.helper.Util;
import com.erakk.lnreader.model.UpdateInfoModel;


public class UpdateInfoModelAdapter  extends ArrayAdapter<UpdateInfoModel> {
	//private static final String TAG = PageModelAdapter.class.toString();
	private Context context;
	private int layoutResourceId;
	private Date now;
	long repeatTime = 0;	
	public List<UpdateInfoModel> data;
	
	public UpdateInfoModelAdapter(Context context, int resourceId, List<UpdateInfoModel> objects) {
		super(context, resourceId, objects);
		this.layoutResourceId = resourceId;
		this.context = context;
		this.data = objects;
		
		now = new Date();
		
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(LNReaderApplication.getInstance().getApplicationContext());
		String updatesIntervalStr = preferences.getString(Constants.PREF_UPDATE_INTERVAL, "0");
		int updatesInterval = Integer.parseInt(updatesIntervalStr);
		
		switch (updatesInterval) {
			case 1:
				repeatTime = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
				break;
			case 2:
				repeatTime = AlarmManager.INTERVAL_HALF_HOUR;
				break;
			case 3:
				repeatTime = AlarmManager.INTERVAL_HOUR;
				break;
			case 4:
				repeatTime = AlarmManager.INTERVAL_HALF_DAY;
				break;
			case 5:
				repeatTime = AlarmManager.INTERVAL_DAY;
				break;
			default:
				break;
		}
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
		boolean freshData = false;
		if(now.getTime() - page.getUpdateDate().getTime() <= repeatTime ) {
			freshData = true;
		}
		
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
			if(freshData) holder.txtUpdateType.setTypeface(null, Typeface.BOLD);
		}
		
		holder.txtUpdateTitle = (TextView)row.findViewById(R.id.update_chapter);
		if(holder.txtUpdateTitle != null) {
			holder.txtUpdateTitle.setText(page.getUpdateTitle());
			if(freshData) holder.txtUpdateTitle.setTypeface(null, Typeface.BOLD);
		}
		
		holder.txtUpdateDate = (TextView)row.findViewById(R.id.update_date);
		if(holder.txtUpdateDate != null) {
			holder.txtUpdateDate.setText("Update:" +  Util.formatDateForDisplay(page.getUpdateDate()));
		}
		
		holder.chkSelected = (CheckBox)row.findViewById(R.id.chk_selection);
		if(holder.chkSelected != null) {
			//holder.txtUpdateDate.setText("Update:" +  Util.formatDateForDisplay(page.getUpdateDate()));
			holder.chkSelected.setSelected(page.isSelected());
			
			holder.chkSelected.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					page.setSelected(isChecked);
				}
			});
		}
		
		row.setTag(holder);
		return row;
	}
	
	static class UpdateInfoModelHolder
	{
		TextView txtUpdateType;
		TextView txtUpdateTitle;
		TextView txtUpdateDate;
		CheckBox chkSelected;
	}

	public void setResourceId (int id) {
		this.layoutResourceId = id;
	}
}
