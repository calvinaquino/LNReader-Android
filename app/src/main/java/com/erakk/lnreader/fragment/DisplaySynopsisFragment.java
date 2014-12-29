package com.erakk.lnreader.fragment;

import java.net.URL;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
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
import com.erakk.lnreader.activity.DisplayImageActivity;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.IExtendedCallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.model.BookModel;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.NovelContentModel;
import com.erakk.lnreader.model.PageModel;
import com.erakk.lnreader.parser.CommonParser;
import com.erakk.lnreader.task.AsyncTaskResult;
import com.erakk.lnreader.task.LoadNovelDetailsTask;

public class DisplaySynopsisFragment extends SherlockFragment implements IExtendedCallbackNotifier<AsyncTaskResult<?>> {
	public static final String TAG = DisplaySynopsisFragment.class.toString();
	private final NovelsDao dao = NovelsDao.getInstance();
	private NovelCollectionModel novelCol;
	private View currentLayout;

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
			page = dao.getPageModel(page, null);
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
		task = new LoadNovelDetailsTask(pageModel, willRefresh, this);
		String key = TAG + ":" + pageModel.getPage();
		boolean isAdded = LNReaderApplication.getInstance().addTask(key, task);
		if (isAdded) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
				task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			else
				task.execute();
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
	public boolean downloadListSetup(String id, String toastText, int type, boolean hasError) {
		return false;
	}

	@Override
	public void onProgressCallback(ICallbackEventData message) {
		toggleProgressBar(true);
		loadingText.setText(message.getMessage());

		if(message.getPercentage() > 0) {
			LNReaderApplication.getInstance().updateDownload(message.getSource(), message.getPercentage(), message.getMessage());
			if (loadingBar != null && loadingBar.getVisibility() == View.VISIBLE) {
				loadingBar.setIndeterminate(false);
				loadingBar.setMax(100);
				loadingBar.setProgress(message.getPercentage());
				loadingBar.setProgress(0);
				loadingBar.setProgress(message.getPercentage());
				loadingBar.setMax(100);
			}
		}
		else {
			loadingBar.setIndeterminate(true);
		}
	}

	public void toggleProgressBar(boolean show) {
		if (show) {
			loadingText.setText("Loading List, please wait...");
			loadingText.setVisibility(TextView.VISIBLE);
			loadingBar.setVisibility(ProgressBar.VISIBLE);
		} else {
			loadingText.setVisibility(TextView.GONE);
			loadingBar.setVisibility(ProgressBar.GONE);
		}
	}

	@Override
	@SuppressLint("NewApi")
	public void onCompleteCallback(ICallbackEventData message, AsyncTaskResult<?> result) {
		if (!isAdded())
			return;

		Exception e = result.getError();
		if (e == null) {
			// from DownloadNovelContentTask
			if (result.getResultType() == NovelContentModel[].class) {
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
			else if (result.getResultType() == NovelCollectionModel.class) {
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
							dao.updatePageModel(page);
						}
					});

					ImageView ImageViewCover = (ImageView) currentLayout.findViewById(R.id.cover);
					if (novelCol.getCoverBitmap() == null) {
						// IN app test, is returning empty bitmap
						Toast.makeText(getSherlockActivity(), "Bitmap empty", Toast.LENGTH_LONG).show();
					} else {
						ImageViewCover.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View v) {
								handleCoverClick(novelCol.getCoverUrl());
							}
						});
						if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) && UIHelper.getStrechCoverPreference(getSherlockActivity())) {
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
							ImageViewCover.getLayoutParams().height = novelCol.getCoverBitmap().getHeight();
							ImageViewCover.getLayoutParams().width = novelCol.getCoverBitmap().getWidth();
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

	private void handleCoverClick(URL coverUrl) {
		String bigCoverUrl = CommonParser.getImageFilePageFromImageUrl(coverUrl.toString());
		Intent intent = new Intent(getSherlockActivity(), DisplayImageActivity.class);
		intent.putExtra(Constants.EXTRA_IMAGE_URL, bigCoverUrl);
		startActivity(intent);
	}
}
