package com.erakk.lnreader.adapter;

import java.util.Iterator;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.Util;
import com.erakk.lnreader.model.BookModel;
import com.erakk.lnreader.model.BookmarkModel;
import com.erakk.lnreader.model.PageModel;

public class BookmarkModelAdapter extends ArrayAdapter<BookmarkModel> {
	private static final String TAG = BookmarkModelAdapter.class.toString();
	private final int layoutResourceId;
	private final Context context;
	private final List<BookmarkModel> data;
	// private boolean isAdding = false;
	private PageModel novel = null;
	public boolean showPage = false;
	public boolean showCheckBox = false;

	public BookmarkModelAdapter(Context context, int resourceId, List<BookmarkModel> objects, PageModel parent) {
		super(context, resourceId, objects);
		this.layoutResourceId = resourceId;
		this.context = context;
		this.data = objects;
		this.novel = parent;
	}

	@SuppressLint("NewApi")
	public void addAll(List<BookmarkModel> objects) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			super.addAll(objects);
		else {
			for (Iterator<BookmarkModel> iPage = objects.iterator(); iPage.hasNext();) {
				// isAdding = true;
				this.add(iPage.next());
			}
			// isAdding = false;
			this.notifyDataSetChanged();
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		BookmarkModelHolder holder = new BookmarkModelHolder();

		final BookmarkModel bookmark = data.get(position);
		PageModel pageModel = null;
		try {
			pageModel = bookmark.getPageModel();
		} catch (Exception e) {
			Log.e(TAG, "Failed to get PageModel for Bookmark.", e);
		}

		LayoutInflater inflater = ((Activity) context).getLayoutInflater();
		row = inflater.inflate(layoutResourceId, parent, false);

		holder.txtPIndex = (TextView) row.findViewById(R.id.p_index);
		if (holder.txtPIndex != null) {
			holder.txtPIndex.setText("#" + bookmark.getpIndex());
		}

		holder.txtCreateDate = (TextView) row.findViewById(R.id.create_date);
		if (holder.txtCreateDate != null) {
			holder.txtCreateDate.setText("Added " + Util.formatDateForDisplay(bookmark.getCreationDate()));
		}

		holder.txtExcerpt = (TextView) row.findViewById(R.id.excerpt);
		if (holder.txtExcerpt != null) {
			holder.txtExcerpt.setText(bookmark.getExcerpt());
		}

		holder.txtPageTitle = (TextView) row.findViewById(R.id.pageTitle);
		if (holder.txtPageTitle != null) {
			if (showPage) {
				holder.txtPageTitle.setVisibility(View.VISIBLE);
				try {
					PageModel parentPage = pageModel.getParentPageModel();
					holder.txtPageTitle.setText(parentPage.getTitle());
				} catch (Exception ex) {
					Log.e(TAG, "Failed to get pageModel: " + ex.getMessage(), ex);
					holder.txtPageTitle.setText(bookmark.getPage());
				}
			} else {
				holder.txtPageTitle.setVisibility(View.GONE);
			}
		}
		holder.txtPageSubTitle = (TextView) row.findViewById(R.id.page_subtitle);
		if (holder.txtPageSubTitle != null) {
			if (showPage) {
				String subTitle = bookmark.getPage();
				holder.txtPageSubTitle.setVisibility(View.VISIBLE);
				try {
					subTitle = pageModel.getTitle();
					try {
						BookModel book = pageModel.getBook();
						subTitle = String.format("(%s) %s", book.getTitle(), subTitle);
					} catch (Exception ex) {
						Log.e(TAG, "Failed to get bookModel: " + ex.getMessage(), ex);
					}
				} catch (Exception ex) {
					Log.e(TAG, "Failed to get pageModel: " + ex.getMessage(), ex);
				}
				holder.txtPageSubTitle.setText(subTitle);
			} else {
				holder.txtPageSubTitle.setVisibility(View.GONE);
			}
		}

		holder.chkSelection = (CheckBox) row.findViewById(R.id.chk_selection);
		if (holder.chkSelection != null) {
			if (showCheckBox) {
				holder.chkSelection.setVisibility(View.VISIBLE);
				holder.chkSelection.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						bookmark.setSelected(isChecked);
					}
				});
			} else
				holder.chkSelection.setVisibility(View.GONE);
		}

		row.setTag(holder);
		return row;
	}

	public void refreshData() {
		clear();
		if (novel != null) {
			addAll(NovelsDao.getInstance().getBookmarks(novel));
		} else {
			addAll(NovelsDao.getInstance().getAllBookmarks(UIHelper.getAllBookmarkOrder(context)));
		}
		notifyDataSetChanged();
		Log.d(TAG, "Refreshing data...");
	}

	static class BookmarkModelHolder {
		TextView txtPageTitle;
		TextView txtPIndex;
		TextView txtExcerpt;
		TextView txtCreateDate;
		TextView txtPageSubTitle;
		CheckBox chkSelection;
	}
}
