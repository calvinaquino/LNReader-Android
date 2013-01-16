package com.erakk.lnreader.adapter;

import java.util.ArrayList;
import java.util.Iterator;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.R;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.Util;
import com.erakk.lnreader.model.BookModel;
import com.erakk.lnreader.model.PageModel;

public class BookModelAdapter extends BaseExpandableListAdapter {
	private static final String TAG = BookModelAdapter.class.toString();
	private Context context;
	private ArrayList<BookModel> groups;
	

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
	
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View view, ViewGroup parent) {
		PageModel child = getChild(groupPosition, childPosition);
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		int resourceId = R.layout.expandchapter_list_item;
//		if(UIHelper.IsSmallScreen(((Activity)context))) {
//			resourceId = R.layout.expandchapter_list_item_small; 
//		}
		view = inflater.inflate(resourceId, null);
		
		TextView tv = (TextView) view.findViewById(R.id.novel_chapter);
		tv.setText(child.getTitle());
		tv.setTag(child.getPage());

		ImageView ivFinishedReading = (ImageView) view.findViewById(R.id.novel_finished_reading);
		ImageView ivIsExternal = (ImageView) view.findViewById(R.id.novel_is_external);
		
		if(child.isFinishedRead()) {
			
			//Old check
//			tv.setTextColor(Constants.COLOR_READ);
//		}
//		else {
//			if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.PREF_INVERT_COLOR, true)) {
//				tv.setTextColor(Constants.COLOR_UNREAD);
//			}
//			else {
//				tv.setTextColor(Constants.COLOR_UNREAD_INVERT);
//			}
			
			//New check
			if(ivFinishedReading != null) {
				ivFinishedReading.setVisibility(ImageView.VISIBLE);
			}
		}
		else {
		
			if(ivFinishedReading != null) {
				if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.PREF_INVERT_COLOR, true)) {
					ivFinishedReading.setVisibility(ImageView.GONE);
				}
				else {
					ivFinishedReading.setVisibility(ImageView.GONE); //Invert later
				}
			}
		}
		
		
		if(child.isMissing()) {
			tv.setTextColor(Constants.COLOR_MISSING);
		}
		if(child.isExternal()) {
			ivIsExternal.setVisibility(ImageView.VISIBLE);
		}
		else {
			ivIsExternal.setVisibility(ImageView.GONE);
		}

		ImageView ivIsDownloaded = (ImageView) view.findViewById(R.id.novel_is_downloaded);
		ImageView ivHasUpdates = (ImageView) view.findViewById(R.id.novel_has_updates);
		//Log.d("getChildView", "Downloaded " + child.getTitle() + " id " + child.getId() + " : " + child.isDownloaded() );
		if(ivIsDownloaded != null && ivHasUpdates != null) {
//			if(child.isExternal()) {
//				tvIsDownloaded.setText("(EX)");
//				tvIsDownloaded.setVisibility(TextView.VISIBLE);
			//Get a image for this too
//			}
//			else if(!child.isDownloaded()) {
			if(!child.isDownloaded()) {
				ivIsDownloaded.setVisibility(ImageView.GONE);
			}
			else {
				if(NovelsDao.getInstance().isContentUpdated(child)) {
					ivHasUpdates.setVisibility(ImageView.VISIBLE);
				}
				else
					ivHasUpdates.setVisibility(ImageView.GONE);
					
				ivIsDownloaded.setVisibility(TextView.VISIBLE);
			}
		}
		
		
		TextView tvLastUpdate = (TextView) view.findViewById(R.id.novel_last_update);
		if(tvLastUpdate != null){
			tvLastUpdate.setText("Last Update: " + Util.formatDateForDisplay(child.getLastUpdate()));
		}
		
		TextView tvLastCheck = (TextView) view.findViewById(R.id.novel_last_check);
		if(tvLastCheck != null){
			tvLastCheck.setText("Last Check: " + Util.formatDateForDisplay(child.getLastCheck()));
		}
		
		return view;
	}
	
	public int getChildrenCount(int groupPosition) {
		boolean showExternal = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.PREF_SHOW_EXTERNAL, true);
		boolean showMissing = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.PREF_SHOW_MISSING, true);
		
		ArrayList<PageModel> chList = groups.get(groupPosition).getChapterCollection();
		int count = 0;
		for (PageModel pageModel : chList) {
			if(pageModel.isExternal() && !showExternal) {
				continue;
			}
			else if(!pageModel.isExternal() && pageModel.isMissing() && !showMissing) {
				continue;
			}
			++count;
		}
		return count;
	}
	
	public PageModel getChild(int groupPosition, int childPosition) {
		boolean showExternal = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.PREF_SHOW_EXTERNAL, true);
		boolean showMissing = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.PREF_SHOW_MISSING, true);
		
		ArrayList<PageModel> chList = groups.get(groupPosition).getChapterCollection();
		int count = 0;
		for (int i = 0; i < chList.size(); ++i) {
			PageModel temp = chList.get(i);
			if(temp.isExternal() && !showExternal) {
				continue;
			}
			else if(!temp.isExternal() && temp.isMissing() && !showMissing) {
				continue;
			}
			
			if(count == childPosition) {
				return temp;
			}
			++count;
		}
		
		return chList.get(childPosition);
	}

	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
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
			tv.setTextColor(Constants.COLOR_READ);
		}
		else {
			if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.PREF_INVERT_COLOR, true)) {
				tv.setTextColor(Constants.COLOR_UNREAD);
			}
			else {
				tv.setTextColor(Constants.COLOR_UNREAD_INVERT);
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
				PageModel temp = NovelsDao.getInstance(context).getPageModel(chapters.get(j), null);
				chapters.set(j, temp);
			} catch (Exception e) {
				Log.e(TAG, "Error when refreshing PageModel: " + chapters.get(j).getPage(), e);
			}
		}
		super.notifyDataSetChanged();
	}
}
