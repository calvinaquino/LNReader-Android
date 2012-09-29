package com.erakk.lnreader.adapter;

import java.util.ArrayList;
import java.util.Iterator;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.model.BookModel;
import com.erakk.lnreader.model.PageModel;

public class BookModelAdapter extends BaseExpandableListAdapter {
	private static final String TAG = BookModelAdapter.class.toString();
	private Context context;
	private ArrayList<BookModel> groups;
	private int read = Color.parseColor("#888888");
	private int notRead = Color.parseColor("#dddddd");
	private int notReadDark = Color.parseColor("#222222");

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
		LayoutInflater infalInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		int resourceId = R.layout.expandchapter_list_item;
		if(UIHelper.IsSmallScreen(((Activity)context))) {
			resourceId = R.layout.expandchapter_list_item_small; 
		}
		view = infalInflater.inflate(resourceId, null);
		
		TextView tv = (TextView) view.findViewById(R.id.novel_chapter);
		tv.setText(child.getTitle());
		tv.setTag(child.getPage());
		
		if(child.isFinishedRead()) {
			tv.setTextColor(read);
		}
		else {
			if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean("invert_colors", false)) {
				tv.setTextColor(notRead);
			}
			else {
				tv.setTextColor(notReadDark);
			}
		}
		
		TextView tvIsDownloaded = (TextView) view.findViewById(R.id.novel_is_downloaded);
		//Log.d("getChildView", "Downloaded " + child.getTitle() + " id " + child.getId() + " : " + child.isDownloaded() );
		if(tvIsDownloaded != null) {
			if(!child.isDownloaded()) {
				tvIsDownloaded.setVisibility(TextView.GONE);
			}
			else {
				tvIsDownloaded.setVisibility(TextView.VISIBLE);
			}
		}
		
		TextView tvLastUpdate = (TextView) view.findViewById(R.id.novel_last_update);
		if(tvLastUpdate != null){
			tvLastUpdate.setText("Last Update: " + child.getLastUpdate().toString());
		}
		
		TextView tvLastCheck = (TextView) view.findViewById(R.id.novel_last_check);
		if(tvLastCheck != null){
			tvLastCheck.setText("Last Check: " + child.getLastCheck().toString());
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
		LayoutInflater inf = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		view = inf.inflate(R.layout.expandvolume_list_item, null);
		TextView tv = (TextView) view.findViewById(R.id.novel_volume);
		tv.setText(group.getTitle());
		
		// check if all chapter is read
		boolean readAll = true;
		for(Iterator<PageModel> iPage = group.getChapterCollection().iterator(); iPage.hasNext();) {
			PageModel page = iPage.next();
			if(!page.isFinishedRead()) {
				readAll = false;
				break;
			}
		}
		if(readAll) {
			tv.setTextColor(read);
		}
		else {
			if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean("invert_colors", false)) {
				tv.setTextColor(notRead);
			}
			else {
				tv.setTextColor(notReadDark);
			}
		}
		
		return view;
	}

	public boolean hasStableIds() {
		return true;
	}

	public boolean isChildSelectable(int arg0, int arg1) {
		return true;
	}
	
	@Override
	public void notifyDataSetChanged() {
		// refresh the data
		for(int i = 0; i< groups.size();++i) {
			ArrayList<PageModel> chapters = groups.get(i).getChapterCollection();
			for(int j = 0; j < chapters.size(); ++j)
			try {
				PageModel temp = NovelsDao.getInstance(context).getPageModel(chapters.get(j).getPage(), null);
				chapters.set(j, temp);
			} catch (Exception e) {
				Log.e(TAG, "Error when refreshing PageModel: " + chapters.get(j).getPage(), e);
			}
		}
		super.notifyDataSetChanged();
	}
}
