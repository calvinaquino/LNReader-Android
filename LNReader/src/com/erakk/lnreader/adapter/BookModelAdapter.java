package com.erakk.lnreader.adapter;

import java.util.ArrayList;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.Util;
import com.erakk.lnreader.model.BookModel;
import com.erakk.lnreader.model.PageModel;

public class BookModelAdapter extends BaseExpandableListAdapter {
	private static final String TAG = BookModelAdapter.class.toString();
	private final Context context;
	private final ArrayList<BookModel> groups;

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

	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View view, ViewGroup parent) {
		PageModel child = getChild(groupPosition, childPosition);
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		int resourceId = R.layout.expandchapter_list_item;
		// if(UIHelper.IsSmallScreen(((Activity)context))) {
		// resourceId = R.layout.expandchapter_list_item_small;
		// }
		view = inflater.inflate(resourceId, null);

		TextView tv = (TextView) view.findViewById(R.id.novel_chapter);
		tv.setText(child.getTitle());
		tv.setTag(child.getPage());

		/*
		 * DYNAMIC ICON ADDITION
		 * 
		 * This will creating icons without a 8dp padding, then send it over to the end of the list container.
		 * To reorder the icons, simply reorder when they get added in. The first will be the one farthest to the left.
		 * Addition command: container.addView([insert ImageView]);
		 */

		ViewGroup container = (ViewGroup) view.findViewById(R.id.novel_chapter_container);

		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
		int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, context.getResources().getDisplayMetrics()); // Converts
																																	// 8dp
																																	// into
																																	// px

		ImageView ivFinishedReading = new ImageView(context);
		ivFinishedReading.setImageResource(R.drawable.ic_finished_reading);
		ivFinishedReading.setScaleType(ImageView.ScaleType.CENTER);
		ivFinishedReading.setPadding(padding, padding, padding, padding);
		ivFinishedReading.setLayoutParams(params);

		ImageView ivExternal = new ImageView(context);
		ivExternal.setImageResource(R.drawable.ic_is_external);
		ivExternal.setScaleType(ImageView.ScaleType.CENTER);
		ivExternal.setPadding(padding, padding, padding, padding);
		ivExternal.setLayoutParams(params);

		ImageView ivIsDownloaded = new ImageView(context);
		ivIsDownloaded.setImageResource(R.drawable.ic_downloaded);
		ivIsDownloaded.setScaleType(ImageView.ScaleType.CENTER);
		ivIsDownloaded.setPadding(padding, padding, padding, padding);
		ivIsDownloaded.setLayoutParams(params);

		ImageView ivHasUpdates = new ImageView(context);
		ivHasUpdates.setImageResource(R.drawable.ic_update_avaliable);
		ivHasUpdates.setScaleType(ImageView.ScaleType.CENTER);
		ivHasUpdates.setPadding(padding, padding, padding, padding);
		ivHasUpdates.setLayoutParams(params);

		if (ivFinishedReading != null) {
			if (child.isFinishedRead()) {
				container.addView(ivFinishedReading);
				UIHelper.setColorFilter(ivFinishedReading);
			}
		}

		if (child.isMissing()) {
			tv.setTextColor(Constants.COLOR_MISSING);
		}
		if (child.isExternal()) {
			container.addView(ivExternal);
			UIHelper.setColorFilter(ivExternal);
		}

		// Log.d("getChildView", "Downloaded " + child.getTitle() + " id " + child.getId() + " : " +
		// child.isDownloaded() );
		if (ivIsDownloaded != null && ivHasUpdates != null) {
			if (child.isDownloaded()) {
				if (NovelsDao.getInstance().isContentUpdated(child)) {
					container.addView(ivHasUpdates);
				}
				container.addView(ivIsDownloaded);
			}
			UIHelper.setColorFilter(ivIsDownloaded);
			UIHelper.setColorFilter(ivHasUpdates);
		}

		TextView tvLastUpdate = (TextView) view.findViewById(R.id.novel_last_update);
		if (tvLastUpdate != null) {
			tvLastUpdate.setText(context.getResources().getString(R.string.last_update) + ": " + Util.formatDateForDisplay(child.getLastUpdate()));
		}

		TextView tvLastCheck = (TextView) view.findViewById(R.id.novel_last_check);
		if (tvLastCheck != null) {
			tvLastCheck.setText(context.getResources().getString(R.string.last_check) + ": " + Util.formatDateForDisplay(child.getLastCheck()));
		}

		return view;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		boolean showExternal = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.PREF_SHOW_EXTERNAL, true);
		boolean showMissing = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.PREF_SHOW_MISSING, true);

		ArrayList<PageModel> chList = groups.get(groupPosition).getChapterCollection();
		int count = 0;
		for (PageModel pageModel : chList) {
			if (pageModel.isExternal() && !showExternal) {
				continue;
			} else if (!pageModel.isExternal() && pageModel.isMissing() && !showMissing) {
				continue;
			}
			++count;
		}
		return count;
	}

	@Override
	public PageModel getChild(int groupPosition, int childPosition) {
		boolean showExternal = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.PREF_SHOW_EXTERNAL, true);
		boolean showMissing = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.PREF_SHOW_MISSING, true);

		ArrayList<PageModel> chList = groups.get(groupPosition).getChapterCollection();
		int count = 0;
		for (int i = 0; i < chList.size(); ++i) {
			PageModel temp = chList.get(i);
			if (temp.isExternal() && !showExternal) {
				continue;
			} else if (!temp.isExternal() && temp.isMissing() && !showMissing) {
				continue;
			}

			if (count == childPosition) {
				return temp;
			}
			++count;
		}

		return chList.get(childPosition);
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	@Override
	public BookModel getGroup(int groupPosition) {
		return groups.get(groupPosition);
	}

	@Override
	public int getGroupCount() {
		return groups.size();
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isLastChild, View view, ViewGroup parent) {
		BookModel group = getGroup(groupPosition);
		LayoutInflater inf = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		view = inf.inflate(R.layout.expandvolume_list_item, null);
		TextView tv = (TextView) view.findViewById(R.id.novel_volume);
		tv.setText(group.getTitle());

		ViewGroup container = (ViewGroup) view.findViewById(R.id.novel_volume_container);

		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
		// Converts 8dp into px
		int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, context.getResources().getDisplayMetrics());

		ImageView ivHasUpdates = new ImageView(context);
		ivHasUpdates.setImageResource(R.drawable.ic_update_avaliable);
		ivHasUpdates.setScaleType(ImageView.ScaleType.CENTER);
		ivHasUpdates.setPadding(padding, padding, padding, padding);
		ivHasUpdates.setLayoutParams(params);

		ArrayList<PageModel> chapterList = group.getChapterCollection();
		// check if any chapter has updates
		for (PageModel pageModel : chapterList) {
			if (NovelsDao.getInstance().isContentUpdated(pageModel)) {
				container.addView(ivHasUpdates);
				UIHelper.setColorFilter(ivHasUpdates);
				break;
			}
		}

		// check if all chapter is read
		boolean readAll = true;
		for (PageModel pageModel : chapterList) {
			if (!pageModel.isFinishedRead()) {
				readAll = false;
				break;
			}
		}

		if (readAll) {
			tv.setTextColor(Constants.COLOR_READ);
		} else {
			if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.PREF_INVERT_COLOR, true)) {
				tv.setTextColor(Constants.COLOR_UNREAD);
			} else {
				tv.setTextColor(Constants.COLOR_UNREAD_DARK);
			}
		}

		return view;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean isChildSelectable(int arg0, int arg1) {
		return true;
	}

	@Override
	public void notifyDataSetChanged() {
		// refresh the data
		for (int i = 0; i < groups.size(); ++i) {
			ArrayList<PageModel> chapters = groups.get(i).getChapterCollection();
			for (int j = 0; j < chapters.size(); ++j)
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
