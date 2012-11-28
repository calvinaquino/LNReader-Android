package com.erakk.lnreader.adapter;

import java.util.Iterator;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.erakk.lnreader.R;
import com.erakk.lnreader.adapter.PageModelAdapter.PageModelHolder;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.model.BookmarkModel;
import com.erakk.lnreader.model.PageModel;


public class BookmarkModelAdapter extends ArrayAdapter<BookmarkModel>{
	private int layoutResourceId;
	private Context context;
	private List<BookmarkModel> data;
	private boolean isAdding = false;
	
	public BookmarkModelAdapter(Context context, int resourceId, List<BookmarkModel> objects) {
		super(context, resourceId, objects);
		this.layoutResourceId = resourceId;
		this.context = context;
		this.data = objects;
	}
	
	@SuppressLint("NewApi")
	public void addAll(List<BookmarkModel> objects) {
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB )
			super.addAll(objects);
		else {
			for(Iterator<BookmarkModel> iPage = objects.iterator(); iPage.hasNext();) {
				isAdding = true;
				this.add(iPage.next());
			}
			isAdding = false;
			this.notifyDataSetChanged();
		}
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		BookmarkModelHolder holder = new BookmarkModelHolder();

		final BookmarkModel page = data.get(position);
		
		LayoutInflater inflater = ((Activity)context).getLayoutInflater();
		row = inflater.inflate(layoutResourceId, parent, false);
 
		holder.txtPIndex = (TextView)row.findViewById(R.id.p_index);
		if(holder.txtPIndex != null) {
			holder.txtPIndex.setText("#" + page.getpIndex());
		}
		
		holder.txtCreateDate = (TextView)row.findViewById(R.id.create_date);
		if(holder.txtCreateDate != null) {
			holder.txtCreateDate.setText("Added: " + page.getCreationDate().toString());
		}
		
		holder.txtExcerpt = (TextView)row.findViewById(R.id.excerpt);
		if(holder.txtExcerpt != null) {
			holder.txtExcerpt.setText(page.getExcerpt());
		}
		
		row.setTag(holder);
		return row;
	}
	
	static class BookmarkModelHolder
	{
		TextView txtPIndex;
		TextView txtExcerpt;
		TextView txtCreateDate;
	}
	
}
