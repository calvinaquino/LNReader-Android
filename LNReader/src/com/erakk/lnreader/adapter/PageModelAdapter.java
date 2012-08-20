package com.erakk.lnreader.adapter;

import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.erakk.lnreader.R;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.model.PageModel;

public class PageModelAdapter extends ArrayAdapter<PageModel> {
	private static final String TAG = PageModelAdapter.class.toString();
	private Context context;
	private int layoutResourceId;
	private List<PageModel> data;

	public PageModelAdapter(Context context, int resourceId, List<PageModel> objects) {
		super(context, resourceId, objects);
		this.layoutResourceId = resourceId;
		this.context = context;
		this.data = objects;
		Log.d("PageModelAdapter", "onConstruct Count = " + objects.size());
	}

	@SuppressLint("NewApi")
	public void addAll(List<PageModel> objects) {
		super.addAll(objects);
		Log.d("PageModelAdapter", "onAddAll Count = " + objects.size());
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		PageModelHolder holder = null;

		final PageModel page = data.get(position);
		LayoutInflater inflater = ((Activity)context).getLayoutInflater();
		row = inflater.inflate(layoutResourceId, parent, false);

		holder = new PageModelHolder();
		holder.txtNovel = (TextView)row.findViewById(R.id.novel_name);
		if(holder.txtNovel != null) {
			holder.txtNovel.setText(page.getTitle());// + " (" + page.getTitle() + ")");
		}
		
//		holder.txtLastUpdate = (TextView)row.findViewById(R.id.novel_last_update);
//		if(holder.txtLastUpdate != null) {
//			holder.txtLastUpdate.setText(page.getLastUpdate().toString());
//		}
		
		holder.chkIsWatched = (CheckBox)row.findViewById(R.id.novel_is_watched);
		if(holder.chkIsWatched != null) {
			Log.d(TAG, page.getId() + " " + page.getTitle() + " isWatched: " + page.isWatched());
			holder.chkIsWatched.setChecked(page.isWatched());
			holder.chkIsWatched.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if(isChecked){
						Toast.makeText(context, "Added to watch list: " + page.getTitle(),	Toast.LENGTH_SHORT).show();
					}
					else {
						Toast.makeText(context, "Removed from watch list: " + page.getTitle(),	Toast.LENGTH_SHORT).show();
					}
					// update the db!
					page.setWatched(isChecked);
					NovelsDao dao = new NovelsDao(context);
					dao.updatePageModel(page);
				}
			});
		}

		row.setTag(holder);
		return row;
	}

	static class PageModelHolder
	{
		TextView txtNovel;
		TextView txtLastUpdate;
		CheckBox chkIsWatched;
	}

	public void setResourceId (int id) {
		this.layoutResourceId = id;
	}
}
