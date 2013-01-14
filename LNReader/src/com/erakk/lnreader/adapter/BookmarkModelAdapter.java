package com.erakk.lnreader.adapter;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.erakk.lnreader.R;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.model.BookmarkModel;
import com.erakk.lnreader.model.PageModel;


public class BookmarkModelAdapter extends ArrayAdapter<BookmarkModel>{
	private static final String TAG = BookmarkModelAdapter.class.toString();
	private int layoutResourceId;
	private Context context;
	private List<BookmarkModel> data;
	//private boolean isAdding = false;
	private PageModel novel = null;
	public boolean showPage = false;
	
	public BookmarkModelAdapter(Context context, int resourceId, List<BookmarkModel> objects, PageModel parent) {
		super(context, resourceId, objects);
		this.layoutResourceId = resourceId;
		this.context = context;
		this.data = objects;
		this.novel = parent;
	}
	
	@SuppressLint("NewApi")
	public void addAll(List<BookmarkModel> objects) {
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB )
			super.addAll(objects);
		else {
			for(Iterator<BookmarkModel> iPage = objects.iterator(); iPage.hasNext();) {
				//isAdding = true;
				this.add(iPage.next());
			}
			//isAdding = false;
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
			holder.txtCreateDate.setText("Added " + formatDateForDisplay(page.getCreationDate()));
		}
		
		holder.txtExcerpt = (TextView)row.findViewById(R.id.excerpt);
		if(holder.txtExcerpt != null) {
			holder.txtExcerpt.setText(page.getExcerpt());
		}
		
		holder.txtPageTitle = (TextView)row.findViewById(R.id.pageTitle);
		if(holder.txtPageTitle != null) {
			if(showPage) {
				holder.txtPageTitle.setVisibility(View.VISIBLE);
				try{
					PageModel pageModel = page.getPageModel();
					PageModel parentPage = pageModel.getParentPageModel();
					holder.txtPageTitle.setText(parentPage.getTitle());
				} catch (Exception ex) {
					Log.e(TAG, "Failed to get pageModel: " + ex.getMessage(), ex);
					holder.txtPageTitle.setText(page.getPage());
				}
			}
			else {
				holder.txtPageTitle.setVisibility(View.GONE);
			}
		}
		holder.txtPageSubTitle = (TextView)row.findViewById(R.id.page_subtitle);
		if(holder.txtPageSubTitle != null) {
			if(showPage) {
				holder.txtPageSubTitle.setVisibility(View.VISIBLE);
				try{
					PageModel pageModel = page.getPageModel();
					holder.txtPageSubTitle.setText(pageModel.getTitle());
				} catch (Exception ex) {
					Log.e(TAG, "Failed to get pageModel: " + ex.getMessage(), ex);
					holder.txtPageSubTitle.setText(page.getPage());
				}
			}
			else {
				holder.txtPageSubTitle.setVisibility(View.GONE);
			}
		}
		
		row.setTag(holder);
		return row;
	}
	
	public void refreshData() {
		clear();
		if(novel != null) addAll(NovelsDao.getInstance().getBookmarks(novel));
		else addAll(NovelsDao.getInstance().getAllBookmarks());
		notifyDataSetChanged();
		Log.d(TAG, "Refreshing data...");
	}

	static class BookmarkModelHolder
	{
		TextView txtPageTitle;
		TextView txtPIndex;
		TextView txtExcerpt;
		TextView txtCreateDate;
		TextView txtPageSubTitle;
	}
	@SuppressWarnings({ "deprecation", "unused" })
	private String formatDateForDisplay(Date date) {
		String since= "";
		//Setup
		Time now = new Time();
		int dif = 0;
		now.setToNow();
		dif = now.hour-date.getHours();
		if(dif<0) {
			since = "invalid";
		}
		else if(dif<24) {
			since = "hour";
		}
		else if (dif<168) {
			dif/=24;
			since = "day";
		}
		else if (dif<720) {
			dif/=168;
			since = "week";
		}
		else if (dif<8760) {
			dif/=720;
			since = "month";
		}
		else {
			dif/=8760;
			since = "year";
		}
		if (dif < 0) return since;
		else if (dif == 1) return dif+" "+since+" ago";
		else return dif+" "+since+"s ago";
	}
}
