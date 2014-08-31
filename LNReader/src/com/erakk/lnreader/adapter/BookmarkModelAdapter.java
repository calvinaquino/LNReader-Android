package com.erakk.lnreader.adapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

import com.erakk.lnreader.Constants;
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
	private List<BookmarkModel> data;
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

	@Override
	public void addAll(Collection<? extends BookmarkModel> objects) {
		synchronized (this) {
			if (data == null) {
				data = new ArrayList<BookmarkModel>();
			}
			data.addAll(objects);

			this.notifyDataSetChanged();
		}
	}

	@Override
	public void addAll(BookmarkModel... objects) {
		synchronized (this) {
			if (data == null) {
				data = new ArrayList<BookmarkModel>();
			}

			for (BookmarkModel item : objects) {
				data.add(item);
			}

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
			holder.txtCreateDate.setText(context.getResources().getString(R.string.added) + " " + Util.formatDateForDisplay(context, bookmark.getCreationDate()));
		}

		holder.txtExcerpt = (TextView) row.findViewById(R.id.excerpt);
		if (holder.txtExcerpt != null) {
			holder.txtExcerpt.setText(bookmark.getExcerpt());
		}

		// Bookmark title
		holder.txtPageTitle = (TextView) row.findViewById(R.id.pageTitle);
		if (holder.txtPageTitle != null) {
			if (showPage) {
				holder.txtPageTitle.setVisibility(View.VISIBLE);
				if (Util.isStringNullOrEmpty(bookmark.getBookmarkTitle())) {
					try {
						PageModel parentPage = pageModel.getParentPageModel();
						bookmark.setBookmarkTitle(parentPage.getTitle());
					} catch (Exception ex) {
						Log.e(TAG, "Failed to get pageModel: " + bookmark.getPage(), ex);
						bookmark.setBookmarkTitle(bookmark.getPage());
					}
				}
				holder.txtPageTitle.setText(bookmark.getBookmarkTitle());
			} else {
				holder.txtPageTitle.setVisibility(View.GONE);
			}
		}

		// Sub Title
		holder.txtPageSubTitle = (TextView) row.findViewById(R.id.page_subtitle);
		if (holder.txtPageSubTitle != null) {
			if (showPage) {
				String subTitle = bookmark.getPage();
				holder.txtPageSubTitle.setVisibility(View.VISIBLE);
				if (Util.isStringNullOrEmpty(bookmark.getSubTitle()) && pageModel != null) {
					subTitle = pageModel.getTitle();
					try {
						String bookTitle = pageModel.getParent().substring(pageModel.getParent().indexOf(Constants.NOVEL_BOOK_DIVIDER) + Constants.NOVEL_BOOK_DIVIDER.length());
						if (!Util.isStringNullOrEmpty(bookTitle)) {
							subTitle = String.format("(%s) %s", bookTitle, subTitle);
						}
						else {
							BookModel book = pageModel.getBook();
							subTitle = String.format("(%s) %s", book.getTitle(), subTitle);
						}
					} catch (Exception ex) {
						Log.e(TAG, "Failed to get bookModel: " + ex.getMessage(), ex);
					}
					bookmark.setSubTitle(subTitle);
				} else {
					bookmark.setSubTitle(bookmark.getPage());
				}
				holder.txtPageSubTitle.setText(bookmark.getSubTitle());
			} else {
				holder.txtPageSubTitle.setVisibility(View.GONE);
			}
		}

		holder.chkSelection = (CheckBox) row.findViewById(R.id.chk_selection);
		if (holder.chkSelection != null) {
			if (showCheckBox) {
				holder.chkSelection.setVisibility(View.VISIBLE);
				holder.chkSelection.setChecked(bookmark.isSelected());
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
