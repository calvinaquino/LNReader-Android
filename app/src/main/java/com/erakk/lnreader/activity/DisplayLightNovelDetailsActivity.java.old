package com.erakk.lnreader.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UI.activity.BaseActivity;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.adapter.BookModelAdapter;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.IExtendedCallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.model.BookModel;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.NovelContentModel;
import com.erakk.lnreader.model.PageModel;
import com.erakk.lnreader.parser.CommonParser;
import com.erakk.lnreader.task.AsyncTaskResult;
import com.erakk.lnreader.task.DownloadNovelContentTask;
import com.erakk.lnreader.task.LoadNovelDetailsTask;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

public class DisplayLightNovelDetailsActivity extends BaseActivity implements IExtendedCallbackNotifier<AsyncTaskResult<?>> {
	public static final String TAG = DisplayLightNovelDetailsActivity.class.toString();
	private PageModel page;
	private NovelCollectionModel novelCol;
	private final NovelsDao dao = NovelsDao.getInstance();

	private BookModelAdapter bookModelAdapter;
	private ExpandableListView expandList;

	private DownloadNovelContentTask downloadTask = null;
	private LoadNovelDetailsTask task = null;

	private TextView loadingText;
	private ProgressBar loadingBar;
	private boolean isInverted;
	private String touchedForDownload;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		UIHelper.SetTheme(this, R.layout.activity_display_light_novel_details);
		UIHelper.SetActionBarDisplayHomeAsUp(this, true);

		// Get intent and message
		Intent intent = getIntent();
		page = new PageModel();
		page.setPage(intent.getStringExtra(Constants.EXTRA_PAGE));
		try {
			page = dao.getPageModel(page, null);
		} catch (Exception e) {
			Log.e(TAG, "Error when getting Page Model for " + page.getPage(), e);
		}

		// setup listener
		expandList = (ExpandableListView) findViewById(R.id.chapter_list);
		registerForContextMenu(expandList);
		expandList.setOnChildClickListener(new OnChildClickListener() {
			@Override
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
				if (novelCol != null) {
					PageModel chapter = bookModelAdapter.getChild(groupPosition, childPosition);
					String bookName = novelCol.getBookCollections().get(groupPosition).getTitle();
					touchedForDownload = bookName + " " + chapter.getTitle();
					loadChapter(chapter);
				}
				return false;
			}
		});

		setTitle(page.getTitle());
		isInverted = UIHelper.getColorPreferences(this);

		loadingText = (TextView) findViewById(R.id.emptyList);
		loadingBar = (ProgressBar) findViewById(R.id.empttListProgress);

		executeTask(page, false);
	}

	private void loadChapter(PageModel chapter) {
		if (chapter.isExternal() && !UIHelper.isUseInternalWebView(this)) {
			try {
				Uri url = Uri.parse(chapter.getPage());
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, url);
				startActivity(browserIntent);
			} catch (Exception ex) {
				String message = getResources().getString(R.string.error_parsing_url, chapter.getPage());
				Log.e(TAG, message, ex);
				Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
			}
		} else {
			if (chapter.isExternal() || chapter.isDownloaded() || !UIHelper.getDownloadTouchPreference(this)) {
				Intent intent = new Intent(this, DisplayLightNovelContentActivity.class);
				intent.putExtra(Constants.EXTRA_PAGE, chapter.getPage());
				startActivity(intent);
			} else {
				downloadTask = new DownloadNovelContentTask(new PageModel[] { chapter }, DisplayLightNovelDetailsActivity.this);
				downloadTask.execute();
			}
		}
		// }
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		if (isInverted != UIHelper.getColorPreferences(this)) {
			UIHelper.Recreate(this);
		}
		if (bookModelAdapter != null) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					bookModelAdapter.refreshData();
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							bookModelAdapter.notifyDataSetChanged();
						}
					});
				}
			}).start();

		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "OnResume: " + task.getStatus().toString());
	}

	@Override
	public void onStop() {
		// check running task
		// disable canceling, so it can continue to show the status
		// if(task != null && !(task.getStatus() == Status.FINISHED)) {
		// task.cancel(true);
		// }
		// if(downloadTask != null && !(downloadTask.getStatus() ==
		// Status.FINISHED)) {
		// downloadTask.cancel(true);
		// }
		super.onStop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_display_light_novel_details, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			Intent launchNewIntent = new Intent(this, DisplaySettingsActivity.class);
			startActivity(launchNewIntent);
			return true;
		case R.id.menu_refresh_chapter_list:
			executeTask(page, true);
			Toast.makeText(this, getResources().getString(R.string.refreshing_detail), Toast.LENGTH_SHORT).show();
			return true;
		case R.id.menu_refresh_all_chapter:
			/*
			 * Re-download all downloaded chapter :P
			 */

			if (novelCol != null) {
				ArrayList<PageModel> availableChapters = novelCol.getFlattedChapterList();
				ArrayList<PageModel> toBeDownloadedChapters = new ArrayList<PageModel>();
				for (PageModel pageModel : availableChapters) {
					if (pageModel.isMissing() || pageModel.isExternal())
						continue;
					else if (pageModel.isDownloaded() // add to list if already downloaded or the update available.
							|| (pageModel.isDownloaded() && dao.isContentUpdated(pageModel))) {
						toBeDownloadedChapters.add(pageModel);
					}
				}
				touchedForDownload = "Volumes";
				executeDownloadTask(toBeDownloadedChapters, true);
			}
			return true;
		case R.id.invert_colors:
			UIHelper.ToggleColorPref(this);
			UIHelper.Recreate(this);
			return true;
		case R.id.menu_details_download_all:
			/*
			 * Download all chapters
			 */
			if (novelCol != null) {
				ArrayList<PageModel> availableChapters = novelCol.getFlattedChapterList();
				ArrayList<PageModel> notDownloadedChapters = new ArrayList<PageModel>();
				for (PageModel pageModel : availableChapters) {
					if (pageModel.isMissing() || pageModel.isExternal())
						continue;
					else if (!pageModel.isDownloaded() // add to list if not downloaded or the update available.
							|| (pageModel.isDownloaded() && dao.isContentUpdated(pageModel))) {
						notDownloadedChapters.add(pageModel);
					}
				}
				touchedForDownload = "Volumes";
				executeDownloadTask(notDownloadedChapters, true);
			}
			return true;
		case R.id.menu_downloads_list:
			Intent downloadsItent = new Intent(this, DownloadListActivity.class);
			startActivity(downloadsItent);
			return true;
		case android.R.id.home:
			super.onBackPressed();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;

		MenuInflater inflater = getMenuInflater();
		int type = ExpandableListView.getPackedPositionType(info.packedPosition);
		if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
			inflater.inflate(R.menu.novel_details_volume_context_menu, menu);
		} else if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
			inflater.inflate(R.menu.novel_details_chapter_context_menu, menu);
		}
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo) item.getMenuInfo();
		// unpacking
		int groupPosition = ExpandableListView.getPackedPositionGroup(info.packedPosition);
		int childPosition = ExpandableListView.getPackedPositionChild(info.packedPosition);

		PageModel chapter = null;

		switch (item.getItemId()) {
		// Volume cases
		case R.id.download_volume:

			/*
			 * Implement code to download this volume
			 */
			BookModel book = novelCol.getBookCollections().get(groupPosition);
			// get the chapter which not downloaded yet
			ArrayList<PageModel> downloadingChapters = new ArrayList<PageModel>();
			for (Iterator<PageModel> i = book.getChapterCollection().iterator(); i.hasNext();) {
				PageModel temp = i.next();
				if (temp.isDownloaded()) {
					// add to list if the update available.
					if (dao.isContentUpdated(temp)) {
						downloadingChapters.add(temp);
					}
				} else {
					downloadingChapters.add(temp);
				}
			}
			touchedForDownload = book.getTitle();
			executeDownloadTask(downloadingChapters, false);
			return true;
		case R.id.clear_volume:

			/*
			 * Implement code to clear this volume cache
			 */
			BookModel bookClear = novelCol.getBookCollections().get(groupPosition);
			Toast.makeText(this, getResources().getString(R.string.toast_clear_volume, bookClear.getTitle()), Toast.LENGTH_SHORT).show();
			dao.deleteBookCache(bookClear);
			for (PageModel page : bookClear.getChapterCollection()) {
				page.setDownloaded(false);
			}
			bookModelAdapter.notifyDataSetChanged();
			return true;
		case R.id.mark_volume:

			/*
			 * Implement code to mark entire volume as read
			 */
			Toast.makeText(this, getResources().getString(R.string.mark_volume_read), Toast.LENGTH_SHORT).show();
			BookModel book2 = novelCol.getBookCollections().get(groupPosition);
			if (book2 != null) {
				for (PageModel page : book2.getChapterCollection()) {
					page.setFinishedRead(true);
					dao.updatePageModel(page);
				}
				bookModelAdapter.notifyDataSetChanged();
			}
			return true;
		case R.id.mark_volume2:

			/*
			 * Implement code to mark entire volume as unread
			 */
			Toast.makeText(this, getResources().getString(R.string.mark_volume_unread), Toast.LENGTH_SHORT).show();
			BookModel ubook2 = novelCol.getBookCollections().get(groupPosition);
			if (ubook2 != null) {
				for (PageModel page : ubook2.getChapterCollection()) {
					page.setFinishedRead(false);
					dao.updatePageModel(page);
				}
				bookModelAdapter.notifyDataSetChanged();
			}
			return true;
			// Chapter cases
		case R.id.download_chapter:

			/*
			 * Implement code to download this chapter
			 */
			chapter = bookModelAdapter.getChild(groupPosition, childPosition);
			String bookName = novelCol.getBookCollections().get(groupPosition).getTitle();
			touchedForDownload = bookName + " " + chapter.getTitle();
			downloadTask = new DownloadNovelContentTask(new PageModel[] { chapter }, this);
			downloadTask.execute();
			return true;
		case R.id.clear_chapter:

			/*
			 * Implement code to clear this chapter cache
			 */
			chapter = bookModelAdapter.getChild(groupPosition, childPosition);
			Toast.makeText(this, getResources().getString(R.string.toast_clear_chapter, chapter.getTitle()), Toast.LENGTH_SHORT).show();
			dao.deleteChapterCache(chapter);
			chapter.setDownloaded(false);
			bookModelAdapter.notifyDataSetChanged();
			return true;
		case R.id.mark_read:

			/*
			 * Implement code to mark this chapter read >> change to toggle
			 */
			chapter = bookModelAdapter.getChild(groupPosition, childPosition);
			chapter.setFinishedRead(!chapter.isFinishedRead());
			dao.updatePageModel(chapter);
			bookModelAdapter.notifyDataSetChanged();
			Toast.makeText(this, getResources().getString(R.string.toast_toggle_read), Toast.LENGTH_SHORT).show();
			return true;
		case R.id.delete_volume:

			/*
			 * Implement code to delete this volume
			 */
			BookModel bookDel = novelCol.getBookCollections().get(groupPosition);
			Toast.makeText(this, getResources().getString(R.string.delete_this_volume, bookDel.getTitle()), Toast.LENGTH_SHORT).show();
			dao.deleteBooks(bookDel);
			novelCol.getBookCollections().remove(groupPosition);
			bookModelAdapter.notifyDataSetChanged();
			return true;
		case R.id.delete_chapter:

			/*
			 * Implement code to delete this chapter
			 */
			chapter = bookModelAdapter.getChild(groupPosition, childPosition);
			Toast.makeText(this, getResources().getString(R.string.delete_this_chapter, chapter.getTitle()), Toast.LENGTH_SHORT).show();
			dao.deleteNovelContent(chapter);
			dao.deletePage(chapter);
			novelCol.getBookCollections().get(groupPosition).getChapterCollection().remove(chapter);
			bookModelAdapter.notifyDataSetChanged();
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	@SuppressLint("NewApi")
	private void executeTask(PageModel pageModel, boolean willRefresh) {
		task = new LoadNovelDetailsTask(pageModel, willRefresh, this);
		String key = TAG + Constants.KEY_LOAD_CHAPTER + pageModel.getPage();
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
				toggleProgressBar(true);
			}
		}
	}

	@SuppressLint("NewApi")
	private void executeDownloadTask(ArrayList<PageModel> chapters, boolean isAll) {
		if (page != null) {
			downloadTask = new DownloadNovelContentTask(chapters.toArray(new PageModel[chapters.size()]), this);
			String key = TAG + Constants.KEY_DOWNLOAD_CHAPTER + page.getPage();
			if (isAll) {
				key = TAG + Constants.KEY_DOWNLOAD_ALL_CHAPTER + page.getPage();
			}
			boolean isAdded = LNReaderApplication.getInstance().addTask(key, task);

			if (isAdded) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
					downloadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				else
					downloadTask.execute();
			} else {
				Log.i(TAG, "Continue download task: " + key);
				DownloadNovelContentTask tempTask = (DownloadNovelContentTask) LNReaderApplication.getInstance().getTask(key);
				if (tempTask != null) {
					downloadTask = tempTask;
					downloadTask.owner = this;
				}
			}
		}
	}

	@Override
	public boolean downloadListSetup(String id, String toastText, int type, boolean hasError) {
		String name = page.getTitle() + " " + touchedForDownload;
		return UIHelper.downloadListSetup(this, name, id, toastText, type, hasError);
	}

	@Override
	public void onProgressCallback(ICallbackEventData message) {
		toggleProgressBar(true);
		loadingText.setText(message.getMessage());

		LNReaderApplication.getInstance().updateDownload(message.getSource(), message.getPercentage(), message.getMessage());
		if (message.getPercentage() > 0) {
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
			expandList.setVisibility(ListView.GONE);
		} else {
			loadingText.setVisibility(TextView.GONE);
			loadingBar.setVisibility(ProgressBar.GONE);
			expandList.setVisibility(ListView.VISIBLE);
		}
	}

	@SuppressLint("NewApi")
	@Override
	public void onCompleteCallback(ICallbackEventData message, AsyncTaskResult<?> result) {
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
					bookModelAdapter.notifyDataSetChanged();
				}
			}
			// from LoadNovelDetailsTask
			else if (result.getResultType() == NovelCollectionModel.class) {
				novelCol = (NovelCollectionModel) result.getResult();
				expandList = (ExpandableListView) findViewById(R.id.chapter_list);
				// now add the volume and chapter list.
				try {
					// Prepare header
					if (expandList.getHeaderViewsCount() == 0) {
						page = novelCol.getPageModel();
						LayoutInflater layoutInflater = getLayoutInflater();
						View synopsis = layoutInflater.inflate(R.layout.activity_display_synopsis, null);
						TextView textViewTitle = (TextView) synopsis.findViewById(R.id.title);
						TextView textViewSynopsis = (TextView) synopsis.findViewById(R.id.synopsys);
						textViewTitle.setTextSize(20);
						textViewSynopsis.setTextSize(16);
						String title = page.getTitle();
						if (page.isTeaser()) {
							title += " (" + getResources().getString(R.string.teaser_project) + ")";
						}
						if (page.isStalled()) {
							title += "\nStatus: " + getResources().getString(R.string.project_stalled);
						}
						if (page.isAbandoned()) {
							title += "\nStatus: " + getResources().getString(R.string.project_abandonded);
						}
						if (page.isPending()) {
							title += "\nStatus: " + getResources().getString(R.string.project_pending_authorization);
						}

						textViewTitle.setText(title);
						textViewSynopsis.setText(novelCol.getSynopsis());

						CheckBox isWatched = (CheckBox) synopsis.findViewById(R.id.isWatched);
						isWatched.setChecked(page.isWatched());
						isWatched.setOnCheckedChangeListener(new OnCheckedChangeListener() {

							@Override
							public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
								updateWatchStatus(isChecked, page);
							}
						});

						ImageView ImageViewCover = (ImageView) synopsis.findViewById(R.id.cover);
						if (novelCol.getCoverBitmap() == null) {
							// IN app test, is returning empty bitmap
							View div = synopsis.findViewById(R.id.divider_bottom);
							div.setVisibility(View.GONE);
							ImageViewCover.setVisibility(View.GONE);
							Toast.makeText(this, getResources().getString(R.string.toast_err_bitmap_empty), Toast.LENGTH_LONG).show();
						} else {
							ImageViewCover.setOnClickListener(new OnClickListener() {

								@Override
								public void onClick(View v) {
									handleCoverClick(novelCol.getCoverUrl());
								}
							});

							if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) && UIHelper.getStrechCoverPreference(this)) {
								Drawable coverDrawable = new BitmapDrawable(getResources(), novelCol.getCoverBitmap());
								int coverHeight = novelCol.getCoverBitmap().getHeight();
								int coverWidth = novelCol.getCoverBitmap().getWidth();
                                double screenWidth = UIHelper.getScreenWidth(this) * 0.9;
                                double ratio = screenWidth / coverWidth;
                                int finalHeight = (int) (coverHeight * ratio);
                                ImageViewCover.setBackground(coverDrawable);
								ImageViewCover.getLayoutParams().height = finalHeight;
                                ImageViewCover.getLayoutParams().width = (int) screenWidth;
                            } else {
								Log.d(TAG, "Non Stretch");
								ImageViewCover.setImageBitmap(novelCol.getCoverBitmap());
								ImageViewCover.getLayoutParams().height = novelCol.getCoverBitmap().getHeight();
								ImageViewCover.getLayoutParams().width = novelCol.getCoverBitmap().getWidth();
							}
						}

						expandList.addHeaderView(synopsis);
					}
					bookModelAdapter = new BookModelAdapter(DisplayLightNovelDetailsActivity.this, novelCol.getBookCollections());
					expandList.setAdapter(bookModelAdapter);
					Log.d(TAG, "Loaded: " + novelCol.getPage());
				} catch (Exception e2) {
					Log.e(TAG, "Error when setting up chapter list: " + e2.getMessage(), e2);
					Toast.makeText(DisplayLightNovelDetailsActivity.this, getResources().getString(R.string.error_setting_chapter_list, e2.getMessage()), Toast.LENGTH_SHORT).show();
				}

				if (novelCol == null) {
					Log.e(TAG, "Empty Novel Collection: " + getIntent().getStringExtra(Constants.EXTRA_PAGE));
				}
			}
		} else {
			Log.e(TAG, e.getClass().toString() + ": " + e.getMessage(), e);
			Toast.makeText(this, e.getClass().toString() + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
		}

		toggleProgressBar(false);
	}

	private void updateWatchStatus(boolean isChecked, PageModel page) {
		if (isChecked) {
			Toast.makeText(this, getResources().getString(R.string.toast_add_watch, page.getTitle()), Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(this, getResources().getString(R.string.toast_remove_watch, page.getTitle()), Toast.LENGTH_SHORT).show();
		}
		// update the db!
		page.setWatched(isChecked);
		dao.updatePageModel(page);
	}

	private void handleCoverClick(URL coverUrl) {
		String bigCoverUrl = CommonParser.getImageFilePageFromImageUrl(coverUrl.toString());
		Intent intent = new Intent(this, DisplayImageActivity.class);
		intent.putExtra(Constants.EXTRA_IMAGE_URL, bigCoverUrl);
		startActivity(intent);
	}
}
