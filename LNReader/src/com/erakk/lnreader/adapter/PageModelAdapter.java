package com.erakk.lnreader.adapter;

import java.util.List;

import com.erakk.lnreader.R;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.model.PageModel;

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

public class PageModelAdapter extends ArrayAdapter<PageModel> {
	Context context;
	int layoutResourceId;
	List<PageModel> data;


	public PageModelAdapter(Context context, int resourceId, List<PageModel> objects) {
		super(context, resourceId, objects);
		this.layoutResourceId = resourceId;
		this.context = context;
		this.data = objects;
		Log.d("PageModelAdapter", "Count = " + objects.size());
	}
	
	@SuppressLint("NewApi")
	public void addAll(List<PageModel> objects) {
		super.addAll(objects);
		Log.d("PageModelAdapter", "Count = " + objects.size());
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		PageModelHolder holder = null;

		//if(row == null)		{
			final PageModel page = data.get(position);
			LayoutInflater inflater = ((Activity)context).getLayoutInflater();
			row = inflater.inflate(layoutResourceId, parent, false);

			holder = new PageModelHolder();
			holder.txtNovel = (TextView)row.findViewById(R.id.novel_name);
			holder.chkIsWatched = (CheckBox)row.findViewById(R.id.novel_is_watched);

			holder.txtNovel.setText(page.getTitle());// + " (" + page.getTitle() + ")");
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

			row.setTag(holder);
//		}
//		else
//		{
//			holder = (PageModelHolder)row.getTag();
//		}

		return row;
	}

	static class PageModelHolder
	{
		TextView txtNovel;
		CheckBox chkIsWatched;
	}
}
