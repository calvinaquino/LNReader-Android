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
import android.widget.FrameLayout;
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

    static class BookModelChildViewHolder {
        TextView txtNovel;
        ViewGroup vgChapter;
        ImageView ivFinishedReading;
        ImageView ivExternal;
        ImageView ivIsDownloaded;
        ImageView ivHasUpdates;
        TextView tvLastUpdate;
        TextView tvLastCheck;
    }

    static class BookModelGroupViewHolder {
        TextView txtNovel;
        ViewGroup container;
        ImageView ivHasUpdates;
    }

	private static final String TAG = BookModelAdapter.class.toString();
	private final Context context;
	private final ArrayList<BookModel> groups;

	public BookModelAdapter(Context context, ArrayList<BookModel> groups) {
		this.context = context;
		this.groups = groups;
	}


    public void refreshData() {
        for (int i = 0; i < groups.size(); ++i) {
            ArrayList<PageModel> chapters = groups.get(i).getChapterCollection();
            for (int j = 0; j < chapters.size(); ++j)
                try {
                    PageModel temp = NovelsDao.getInstance().getPageModel(chapters.get(j), null);
                    chapters.set(j, temp);
                } catch (Exception e) {
                    Log.e(TAG, "Error when refreshing PageModel: " + chapters.get(j).getPage(), e);
                }
        }
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
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		PageModel child = getChild(groupPosition, childPosition);
        View view = convertView;
        BookModelChildViewHolder holder;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            int resourceId = R.layout.expandchapter_list_item;
            // if(UIHelper.IsSmallScreen(((Activity)context))) {
            // resourceId = R.layout.expandchapter_list_item_small;
            // }
            view = inflater.inflate(resourceId, null);
            holder = new BookModelChildViewHolder();
            holder.txtNovel = (TextView) view.findViewById(R.id.novel_chapter);
            holder.vgChapter = (ViewGroup) view.findViewById(R.id.novel_chapter_container);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
            // Converts 8dp into px
            int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, context.getResources().getDisplayMetrics());

            holder.ivFinishedReading = new ImageView(context);
            holder.ivFinishedReading.setImageResource(R.drawable.ic_finished_reading);
            holder.ivFinishedReading.setScaleType(ImageView.ScaleType.CENTER);
            holder.ivFinishedReading.setPadding(padding, padding, padding, padding);
            holder.ivFinishedReading.setLayoutParams(params);

            holder.ivExternal = new ImageView(context);
            holder.ivExternal.setImageResource(R.drawable.ic_is_external);
            holder.ivExternal.setScaleType(ImageView.ScaleType.CENTER);
            holder.ivExternal.setPadding(padding, padding, padding, padding);
            holder.ivExternal.setLayoutParams(params);

            holder.ivIsDownloaded = new ImageView(context);
            holder.ivIsDownloaded.setImageResource(R.drawable.ic_downloaded);
            holder.ivIsDownloaded.setScaleType(ImageView.ScaleType.CENTER);
            holder.ivIsDownloaded.setPadding(padding, padding, padding, padding);
            holder.ivIsDownloaded.setLayoutParams(params);

            holder.ivHasUpdates = new ImageView(context);
            holder.ivHasUpdates.setImageResource(R.drawable.ic_update_avaliable);
            holder.ivHasUpdates.setScaleType(ImageView.ScaleType.CENTER);
            holder.ivHasUpdates.setPadding(padding, padding, padding, padding);
            holder.ivHasUpdates.setLayoutParams(params);

            holder.tvLastUpdate = (TextView) view.findViewById(R.id.novel_last_update);
            holder.tvLastCheck = (TextView) view.findViewById(R.id.novel_last_check);

            view.setTag(holder);
        }
        else {
            holder = (BookModelChildViewHolder)view.getTag();
            ViewGroup ivparent = (ViewGroup)holder.ivIsDownloaded.getParent();
            if (ivparent != null) ivparent.removeView(holder.ivIsDownloaded);
            ivparent = (ViewGroup)holder.ivFinishedReading.getParent();
            if (ivparent != null) ivparent.removeView(holder.ivFinishedReading);
            ivparent = (ViewGroup)holder.ivExternal.getParent();
            if (ivparent != null) ivparent.removeView(holder.ivExternal);
            ivparent = (ViewGroup)holder.ivHasUpdates.getParent();
            if (ivparent != null) ivparent.removeView(holder.ivHasUpdates);
        }

		TextView tv = holder.txtNovel;
		tv.setText(child.getTitle());
		tv.setTag(child.getPage());

		/*
		 * DYNAMIC ICON ADDITION
		 * 
		 * This will creating icons without a 8dp padding, then send it over to the end of the list container.
		 * To reorder the icons, simply reorder when they get added in. The first will be the one farthest to the left.
		 * Addition command: container.addView([insert ImageView]);
		 */

		ViewGroup container = holder.vgChapter;

		if (holder.ivFinishedReading != null) {
			if (child.isFinishedRead()) {
				container.addView(holder.ivFinishedReading);
				UIHelper.setColorFilter(holder.ivFinishedReading);
			}
		}

		if (child.isMissing()) {
			tv.setTextColor(Constants.COLOR_MISSING);
		}
		if (child.isRedlink()) {
			tv.setTextColor(Constants.COLOR_REDLINK);
		}
		if (child.isExternal()) {
			container.addView(holder.ivExternal);
			UIHelper.setColorFilter(holder.ivExternal);
			String wacName = Util.getSavedWacName(child.getPage());
			if (!Util.isStringNullOrEmpty(wacName)) {
				child.setDownloaded(true);
			}
		}

		// Log.d("getChildView", "Downloaded " + child.getTitle() + " id " + child.getId() + " : " +
		// child.isDownloaded() );
		if (holder.ivIsDownloaded != null && holder.ivHasUpdates != null) {
			if (child.isDownloaded()) {
				if (NovelsDao.getInstance().isContentUpdated(child)) {
					container.addView(holder.ivHasUpdates);
				}
				container.addView(holder.ivIsDownloaded);
			}
			UIHelper.setColorFilter(holder.ivIsDownloaded);
			UIHelper.setColorFilter(holder.ivHasUpdates);
		}

		TextView tvLastUpdate = holder.tvLastUpdate;
		if (tvLastUpdate != null) {
			tvLastUpdate.setText(context.getResources().getString(R.string.last_update) + ": " + Util.formatDateForDisplay(context, child.getLastUpdate()));
		}

		TextView tvLastCheck = holder.tvLastCheck;
		if (tvLastCheck != null) {
			tvLastCheck.setText(context.getResources().getString(R.string.last_check) + ": " + Util.formatDateForDisplay(context, child.getLastCheck()));
		}

		return view;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		boolean showExternal = UIHelper.getShowExternal(context);
		boolean showMissing = UIHelper.getShowMissing(context);
		boolean showRedlink = UIHelper.getShowRedlink(context);

		ArrayList<PageModel> chList = groups.get(groupPosition).getChapterCollection();
		int count = 0;
		for (PageModel pageModel : chList) {
			if (pageModel.isExternal() && !showExternal) {
				continue;
			} else if (!pageModel.isExternal() && pageModel.isMissing() && !showMissing) {
				continue;
			} else if (pageModel.isRedlink() && !showRedlink) {
				continue;
			}
			++count;
		}
		return count;
	}

	@Override
	public PageModel getChild(int groupPosition, int childPosition) {
		boolean showExternal = UIHelper.getShowExternal(context);
		boolean showMissing = UIHelper.getShowMissing(context);
		boolean showRedlink = UIHelper.getShowRedlink(context);

		ArrayList<PageModel> chList = groups.get(groupPosition).getChapterCollection();
		int count = 0;
		for (int i = 0; i < chList.size(); ++i) {
			PageModel temp = chList.get(i);
			if (temp.isExternal() && !showExternal) {
				continue;
			} else if (!temp.isExternal() && temp.isMissing() && !showMissing) {
				continue;
			} else if (temp.isRedlink() && !showRedlink) {
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
        BookModelGroupViewHolder holder;
        if (view == null) {
            LayoutInflater inf = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inf.inflate(R.layout.expandvolume_list_item, null);
            holder = new BookModelGroupViewHolder();
            holder.txtNovel = (TextView) view.findViewById(R.id.novel_volume);
            holder.container = (ViewGroup) view.findViewById(R.id.novel_volume_container);
            holder.ivHasUpdates = new ImageView(context);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
            // Converts 8dp into px
            int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, context.getResources().getDisplayMetrics());

            holder.ivHasUpdates.setImageResource(R.drawable.ic_update_avaliable);
            holder.ivHasUpdates.setScaleType(ImageView.ScaleType.CENTER);
            holder.ivHasUpdates.setPadding(padding, padding, padding, padding);
            holder.ivHasUpdates.setLayoutParams(params);
            view.setTag(holder);
        } else {
            holder = (BookModelGroupViewHolder)view.getTag();
        }

		BookModel group = getGroup(groupPosition);
		ArrayList<PageModel> chapterList = group.getChapterCollection();
		boolean isHideEmptyVolume = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.PREF_HIDE_EMPTY_VOLUME, false);
		if (isHideEmptyVolume && (chapterList == null || chapterList.size() == 0)) {
			return new FrameLayout(context);
		}

		TextView tv = holder.txtNovel;
		tv.setText(group.getTitle());

		ViewGroup container = holder.container;
		ImageView ivHasUpdates = holder.ivHasUpdates;

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
			if (pageModel.getPage().endsWith("&action=edit&redlink=1"))
				continue;
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

}
