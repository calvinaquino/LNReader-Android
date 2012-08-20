package com.erakk.lnreader.adapter;

import java.util.ArrayList;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.erakk.lnreader.R;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.model.BookModel;
import com.erakk.lnreader.model.NovelContentModel;
import com.erakk.lnreader.model.PageModel;

public class BookModelAdapter extends BaseExpandableListAdapter {

	private Context context;
	private ArrayList<BookModel> groups;
	boolean invertColors = false;

	public BookModelAdapter(Context context, ArrayList<BookModel> groups) {
		this.context = context;
		this.groups = groups;
	}

	public void addItem(PageModel item, BookModel group) {
		if (!groups.contains(group)) {
			groups.add(group);
		}
		int index = groups.indexOf(group);
		ArrayList<PageModel> ch = groups.get(index).getChapterCollection();
		ch.add(item);
		groups.get(index).setChapterCollection(ch);
	}

	public PageModel getChild(int groupPosition, int childPosition) {
		ArrayList<PageModel> chList = groups.get(groupPosition).getChapterCollection();
		return chList.get(childPosition);
	}

	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View view, ViewGroup parent) {
		PageModel child = getChild(groupPosition, childPosition);
		if (view == null) {
			LayoutInflater infalInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			if (invertColors)
				view = infalInflater.inflate(R.layout.expandchapter_list_item_black, null);
			else
				view = infalInflater.inflate(R.layout.expandchapter_list_item, null);
		}
		
		TextView tv = (TextView) view.findViewById(R.id.novel_chapter);
		tv.setText(child.getTitle());
		tv.setTag(child.getPage());
		
		//Log.d("getChildView", "Finished read " + child.getTitle() + " : " + child.isFinishedRead());
		if(child.isFinishedRead()) {
			tv.setTextColor(Color.parseColor("#888888"));
		}
		
		TextView tvIsDownloaded = (TextView) view.findViewById(R.id.novel_is_downloaded);
		Log.d("getChildView", "Downloaded " + child.getTitle() + " id " + child.getId() + " : " + child.isDownloaded() );
		if(tvIsDownloaded != null) {
			if(!child.isDownloaded()) {
				tvIsDownloaded.setVisibility(TextView.GONE);
			}
		}
		
		return view;
	}

	public int getChildrenCount(int groupPosition) {
		ArrayList<PageModel> chList = groups.get(groupPosition).getChapterCollection();
		return chList.size();
	}

	public BookModel getGroup(int groupPosition) {
		return groups.get(groupPosition);
	}

	public int getGroupCount() {
		return groups.size();
	}

	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	public View getGroupView(int groupPosition, boolean isLastChild, View view, ViewGroup parent) {
		BookModel group =  getGroup(groupPosition);
		if (view == null) {
			LayoutInflater inf = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			if (invertColors)
				view = inf.inflate(R.layout.expandvolume_list_item_black, null);
			else
				view = inf.inflate(R.layout.expandvolume_list_item, null);
		}
		TextView tv = (TextView) view.findViewById(R.id.novel_volume);
		tv.setText(group.getTitle());
		return view;
	}

	public boolean hasStableIds() {
		return true;
	}

	public boolean isChildSelectable(int arg0, int arg1) {
		return true;
	}

	public void invertColorMode(boolean invert) {
		invertColors = invert;
	}
}
