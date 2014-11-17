package com.erakk.lnreader.adapter;

import java.util.List;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.erakk.lnreader.R;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.Util;
import com.erakk.lnreader.model.PageModel;

public class SearchPageModelAdapter extends PageModelAdapter {
	private static final String TAG = SearchPageModelAdapter.class.toString();
	private final Context context;
	private final int layoutResourceId;
	public List<PageModel> data;

	public SearchPageModelAdapter(Context context, int resourceId, List<PageModel> objects) {
		super(context, resourceId, objects);
		this.layoutResourceId = resourceId;
		this.context = context;
		this.data = objects;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final PageModel page = data.get(position);
		View view = convertView;
		SearchPageModelHolder holder;

		if (view == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(layoutResourceId, parent, false);

			holder = new SearchPageModelHolder();
			holder.txtNovel = (TextView) view.findViewById(R.id.novel_name);
			holder.txtLastUpdate = (TextView) view.findViewById(R.id.novel_last_update);
			holder.txtLastCheck = (TextView) view.findViewById(R.id.novel_last_check);
			holder.chkIsWatched = (CheckBox) view.findViewById(R.id.novel_is_watched);

			view.setTag(holder);
		}
		else {
			holder = (SearchPageModelHolder) view.getTag();
		}

		if (holder.txtNovel != null) {
			String title = resolveTitle(page);
			holder.txtNovel.setText(title);
			if (page.isHighlighted()) {
				holder.txtNovel.setTypeface(null, Typeface.BOLD);
				holder.txtNovel.setTextSize(18);
			}
		}
		if (holder.txtLastUpdate != null) {
			holder.txtLastUpdate.setText(context.getResources().getString(R.string.last_update) + ": " + Util.formatDateForDisplay(context, page.getLastUpdate()));
		}
		if (holder.txtLastCheck != null) {
			holder.txtLastCheck.setText(context.getResources().getString(R.string.last_check) + ": " + Util.formatDateForDisplay(context, page.getLastCheck()));
		}
		if (holder.chkIsWatched != null) {
			if (page.getType().equalsIgnoreCase(PageModel.TYPE_NOVEL)) {
				holder.chkIsWatched.setVisibility(View.VISIBLE);
				holder.chkIsWatched.setChecked(page.isWatched());
				holder.chkIsWatched.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						if (isChecked) {
							Toast.makeText(context, "Added to watch list: " + page.getTitle(), Toast.LENGTH_SHORT).show();
						} else {
							Toast.makeText(context, "Removed from watch list: " + page.getTitle(), Toast.LENGTH_SHORT).show();
						}

						page.setWatched(isChecked);
						NovelsDao.getInstance().updatePageModel(page);
					}
				});
			} else {
				holder.chkIsWatched.setVisibility(View.GONE);
			}
		}
		return view;
	}

	/**
	 * @param page
	 * @return
	 */
	private String resolveTitle(final PageModel page) {
		String title;
		if (page.getType().equalsIgnoreCase(PageModel.TYPE_NOVEL)) {
			title = page.getTitle();
		} else {
			// novel name
			try {
				title = page.getParentPageModel().getTitle() + ": ";
			} catch (Exception e) {
				Log.e(TAG, "Unable to get novel name: " + page.getParent(), e);
				title = "Chapter: ";
			}
			// book name
			title += " " + page.getBookTitle();
			// chapter name
			title += "\n\t" + page.getTitle();
		}
		return title;
	}

	static class SearchPageModelHolder {
		TextView txtNovel;
		TextView txtLastUpdate;
		TextView txtLastCheck;
		CheckBox chkIsWatched;
	}
}
