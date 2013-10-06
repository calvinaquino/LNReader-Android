package com.erakk.lnreader.fragment;

import android.annotation.SuppressLint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.erakk.lnreader.Constants;
import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.AsyncTaskResult;
import com.erakk.lnreader.model.BookModel;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.NovelContentModel;
import com.erakk.lnreader.model.PageModel;
import com.erakk.lnreader.task.IAsyncTaskOwner;
import com.erakk.lnreader.task.LoadNovelDetailsTask;

public class DisplaySynopsisFragment extends SherlockFragment implements IAsyncTaskOwner {
	public static final String TAG = DisplaySynopsisFragment.class.toString();
	NovelsDao dao = NovelsDao.getInstance(getSherlockActivity());
	NovelCollectionModel novelCol;
	TextView textViewTitle;
	TextView textViewSynopsis;
	View currentLayout;

	private LoadNovelDetailsTask task = null;
	private PageModel page;

	private TextView loadingText;
	private ProgressBar loadingBar;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		UIHelper.SetActionBarDisplayHomeAsUp(getSherlockActivity(), true);
		View view = inflater.inflate(R.layout.activity_display_synopsis, container, false);

		currentLayout = view;
		// Get intent and message
		page = new PageModel();
		page.setPage(getArguments().getString(Constants.EXTRA_PAGE));

		try {
			page = NovelsDao.getInstance(getSherlockActivity()).getPageModel(page, null);
		} catch (Exception e) {
			Log.e(TAG, "Error when getting Page Model for " + page.getPage(), e);
		}

		loadingText = (TextView) view.findViewById(R.id.emptyList);
		loadingBar = (ProgressBar) view.findViewById(R.id.empttListProgress);

		executeTask(page, false);

		return view;
	}

	@SuppressLint("NewApi")
	private void executeTask(PageModel pageModel, boolean willRefresh) {
		task = new LoadNovelDetailsTask(willRefresh, this);
		String key = TAG + ":" + pageModel.getPage();
		boolean isAdded = LNReaderApplication.getInstance().addTask(key, task);
		if (isAdded) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
				task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new PageModel[] { pageModel });
			else
				task.execute(new PageModel[] { pageModel });
		} else {
			Log.i(TAG, "Continue execute task: " + key);
			LoadNovelDetailsTask tempTask = (LoadNovelDetailsTask) LNReaderApplication.getInstance().getTask(key);
			if (tempTask != null) {
				task = tempTask;
				task.owner = this;
			}
			toggleProgressBar(true);
		}
	}

	@Override
	public void updateProgress(String id, int current, int total, String message) {
		double cur = current;
		double tot = total;
		double result = (cur / tot) * 100;
		LNReaderApplication.getInstance().updateDownload(id, (int) result, message);
	}

	@Override
	public boolean downloadListSetup(String id, String toastText, int type, boolean hasError) {
		return false;
	}

	@Override
	public void toggleProgressBar(boolean show) {
		if (show) {
			loadingText.setText("Loading List, please wait...");
			loadingText.setVisibility(TextView.VISIBLE);
			loadingBar.setVisibility(ProgressBar.VISIBLE);
			loadingBar.setIndeterminate(true);
		} else {
			loadingText.setVisibility(TextView.GONE);
			loadingBar.setVisibility(ProgressBar.GONE);

		}
	}

	@Override
	public void setMessageDialog(ICallbackEventData message) {
		if (loadingText.getVisibility() == TextView.VISIBLE)
			loadingText.setText(message.getMessage());
	}

	@Override
	@SuppressLint("NewApi")
	public void getResult(AsyncTaskResult<?> result) {
		Exception e = result.getError();

		if (e == null) {
			// from DownloadNovelContentTask
			if (result.getResult() instanceof NovelContentModel[]) {
				NovelContentModel[] content = (NovelContentModel[]) result.getResult();
				if (content != null) {
					for (BookModel book : novelCol.getBookCollections()) {
						for (PageModel temp : book.getChapterCollection()) {
							for (int i = 0; i < content.length; ++i) {
								if (temp.getPage() == content[i].getPage()) {
									temp.setDownloaded(true);
								}
							}
						}
					}
				}
			}
			// from LoadNovelDetailsTask
			else if (result.getResult() instanceof NovelCollectionModel) {
				novelCol = (NovelCollectionModel) result.getResult();
				// now add the volume and chapter list.
				try {
					// Prepare header
					page = novelCol.getPageModel();
					TextView textViewTitle = (TextView) currentLayout.findViewById(R.id.title);
					TextView textViewSynopsis = (TextView) currentLayout.findViewById(R.id.synopsys);
					textViewTitle.setTextSize(20);
					textViewSynopsis.setTextSize(16);
					String title = page.getTitle();
					if (page.isTeaser()) {
						title += " (Teaser Project)";
					}
					if (page.isStalled()) {
						title += "\nStatus: Project Stalled";
					}
					if (page.isAbandoned()) {
						title += "\nStatus: Project Abandoned";
					}
					if (page.isPending()) {
						title += "\nStatus: Project Pending Authorization";
					}

					textViewTitle.setText(title);
					textViewSynopsis.setText(novelCol.getSynopsis());

					CheckBox isWatched = (CheckBox) currentLayout.findViewById(R.id.isWatched);
					isWatched.setChecked(page.isWatched());
					isWatched.setOnCheckedChangeListener(new OnCheckedChangeListener() {

						@Override
						public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
							if (isChecked) {
								Toast.makeText(getSherlockActivity(), "Added to watch list: " + page.getTitle(), Toast.LENGTH_SHORT).show();
							} else {
								Toast.makeText(getSherlockActivity(), "Removed from watch list: " + page.getTitle(), Toast.LENGTH_SHORT).show();
							}
							// update the db!
							page.setWatched(isChecked);
							NovelsDao dao = NovelsDao.getInstance(getSherlockActivity());
							dao.updatePageModel(page);
						}
					});

					ImageView ImageViewCover = (ImageView) currentLayout.findViewById(R.id.cover);
					if (novelCol.getCoverBitmap() == null) {
						// IN app test, is returning empty bitmap
						Toast.makeText(getSherlockActivity(), "Bitmap empty", Toast.LENGTH_LONG).show();
					} else {

						if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) && getStrechCoverPreference()) {
							Drawable coverDrawable = new BitmapDrawable(getResources(), novelCol.getCoverBitmap());
							int coverHeight = novelCol.getCoverBitmap().getHeight();
							int coverWidth = novelCol.getCoverBitmap().getWidth();
							int screenWidth = (int) (UIHelper.getScreenHeight(getSherlockActivity()) * 0.9);
							int finalHeight = coverHeight * (screenWidth / coverWidth);
							ImageViewCover.setBackground(coverDrawable);
							ImageViewCover.getLayoutParams().height = finalHeight;
							ImageViewCover.getLayoutParams().width = screenWidth;
						} else {
							ImageViewCover.setImageBitmap(novelCol.getCoverBitmap());
						}
					}

				} catch (Exception e2) {
					Log.e(TAG, "Error when setting up chapter list: " + e2.getMessage(), e2);
					Toast.makeText(getSherlockActivity(), e2.getClass().toString() + ": " + e2.getMessage(), Toast.LENGTH_SHORT).show();
				}
				Log.d(TAG, "Loaded: " + novelCol.getPage());
			}
		} else {
			Log.e(TAG, e.getClass().toString() + ": " + e.getMessage(), e);
			Toast.makeText(getSherlockActivity(), e.getClass().toString() + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
		}
	}

	private boolean getStrechCoverPreference() {
		return PreferenceManager.getDefaultSharedPreferences(getSherlockActivity()).getBoolean(Constants.PREF_STRETCH_COVER, false);
	}
}
