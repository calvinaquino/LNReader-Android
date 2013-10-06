package com.erakk.lnreader.activity;

import java.util.ArrayList;
import java.util.Iterator;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
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

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.erakk.lnreader.Constants;
import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.adapter.BookModelAdapter;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.AsyncTaskResult;
import com.erakk.lnreader.model.BookModel;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.NovelContentModel;
import com.erakk.lnreader.model.PageModel;
import com.erakk.lnreader.task.DownloadNovelContentTask;
import com.erakk.lnreader.task.IAsyncTaskOwner;
import com.erakk.lnreader.task.LoadNovelDetailsTask;

public class DisplayLightNovelDetailsActivity extends SherlockActivity implements IAsyncTaskOwner {
	public static final String TAG = DisplayLightNovelDetailsActivity.class.toString();
	private PageModel page;
	private NovelCollectionModel novelCol;
	private final NovelsDao dao = NovelsDao.getInstance(this);

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
			page = NovelsDao.getInstance(this).getPageModel(page, null);
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
		isInverted = getColorPreferences();

		loadingText = (TextView) findViewById(R.id.emptyList);
		loadingBar = (ProgressBar) findViewById(R.id.empttListProgress);

		executeTask(page, false);
	}

	private void loadChapter(PageModel chapter) {
		boolean useInternalWebView = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(Constants.PREF_USE_INTERNAL_WEBVIEW, false);

		if (chapter.isExternal() && !useInternalWebView) {
			try {
				Uri url = Uri.parse(chapter.getPage());
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, url);
				startActivity(browserIntent);
			} catch (Exception ex) {
				String message = getResources().getString(R.string.error_parsing_url) + ": " + chapter.getPage();
				Log.e(TAG, message, ex);
				Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
			}
		} else {
			if (chapter.isExternal() || chapter.isDownloaded() || !getDownloadTouchPreference()) {
				Intent intent = new Intent(getApplicationContext(), DisplayLightNovelContentActivity.class);
				intent.putExtra(Constants.EXTRA_PAGE, chapter.getPage());
				startActivity(intent);
			} else {
				downloadTask = new DownloadNovelContentTask(new PageModel[] { chapter }, DisplayLightNovelDetailsActivity.this);
				downloadTask.execute();
			}
		}
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		if (isInverted != getColorPreferences()) {
			UIHelper.Recreate(this);
		}
		if (bookModelAdapter != null) {
			bookModelAdapter.notifyDataSetChanged();
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
		getSupportMenuInflater().inflate(R.menu.activity_display_light_novel_details, menu);
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
			Toast.makeText(getApplicationContext(), getResources().getString(R.string.refreshing_detail), Toast.LENGTH_SHORT).show();
			return true;
		case R.id.invert_colors:
			UIHelper.ToggleColorPref(this);
			UIHelper.Recreate(this);
			return true;
		case R.id.menu_bookmarks:
			Intent bookmarkIntent = new Intent(this, DisplayBookmarkActivity.class);
			startActivity(bookmarkIntent);
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
							|| (pageModel.isDownloaded() && NovelsDao.getInstance(this).isContentUpdated(pageModel))) {
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
			;
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
					if (NovelsDao.getInstance(this).isContentUpdated(temp)) {
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
			bookModelAdapter.notifyDataSetChanged();
			return true;
		case R.id.mark_volume:

			/*
			 * Implement code to mark entire volume as read
			 */
			Toast.makeText(this, getResources().getString(R.string.mark_volume_read), Toast.LENGTH_SHORT).show();
			BookModel book2 = novelCol.getBookCollections().get(groupPosition);
			for (Iterator<PageModel> iPage = book2.getChapterCollection().iterator(); iPage.hasNext();) {
				PageModel page = iPage.next();
				page.setFinishedRead(true);
				dao.updatePageModel(page);
			}
			bookModelAdapter.notifyDataSetChanged();
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
			 * Implement code to delete this volume cache
			 */
			BookModel bookDel = novelCol.getBookCollections().get(groupPosition);
			Toast.makeText(this, getResources().getString(R.string.delete_this_volume, bookDel.getTitle()), Toast.LENGTH_SHORT).show();
			dao.deleteBooks(bookDel);
			novelCol.getBookCollections().remove(groupPosition);
			bookModelAdapter.notifyDataSetChanged();
			return true;
		case R.id.delete_chapter:

			/*
			 * Implement code to delete this chapter cache
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
		task = new LoadNovelDetailsTask(willRefresh, this);
		String key = TAG + Constants.KEY_LOAD_CHAPTER + pageModel.getPage();
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
		boolean exists = false;
		String name = page.getTitle() + " " + touchedForDownload;
		if (type == 0) {
			if (LNReaderApplication.getInstance().checkIfDownloadExists(name)) {
				exists = true;
				Toast.makeText(this, getResources().getString(R.string.download_on_queue), Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(this, getResources().getString(R.string.toast_downloading, name), Toast.LENGTH_SHORT).show();
				LNReaderApplication.getInstance().addDownload(id, name);
			}
		} else if (type == 1) {
			Toast.makeText(this, toastText, Toast.LENGTH_SHORT).show();
		} else if (type == 2) {
			String message = getResources().getString(R.string.toast_download_finish, page.getTitle(), LNReaderApplication.getInstance().getDownloadDescription(id));
			if (hasError)
				message = getResources().getString(R.string.toast_download_finish_with_error, page.getTitle(), LNReaderApplication.getInstance().getDownloadDescription(id));
			Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
			LNReaderApplication.getInstance().removeDownload(id);
		}
		return exists;
	}

	@Override
	public void updateProgress(String id, int current, int total, String message) {
		double cur = current;
		double tot = total;
		double result = (cur / tot) * 100;
		LNReaderApplication.getInstance().updateDownload(id, (int) result, message);
	}

	@Override
	public void toggleProgressBar(boolean show) {
		if (show) {
			loadingText.setText("Loading List, please wait...");
			loadingText.setVisibility(TextView.VISIBLE);
			loadingBar.setVisibility(ProgressBar.VISIBLE);
			loadingBar.setIndeterminate(true);
			expandList.setVisibility(ListView.GONE);
		} else {
			loadingText.setVisibility(TextView.GONE);
			loadingBar.setVisibility(ProgressBar.GONE);
			expandList.setVisibility(ListView.VISIBLE);
		}
	}

	@Override
	public void setMessageDialog(ICallbackEventData message) {
		if (loadingText.getVisibility() == View.VISIBLE) {
			loadingText.setText(message.getMessage());
		}
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
					bookModelAdapter.notifyDataSetChanged();
				}
			}
			// from LoadNovelDetailsTask
			else if (result.getResult() instanceof NovelCollectionModel) {
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
								if (isChecked) {
									Toast.makeText(getApplicationContext(), getResources().getString(R.string.toast_add_watch, page.getTitle()), Toast.LENGTH_SHORT).show();
								} else {
									Toast.makeText(getApplicationContext(), getResources().getString(R.string.toast_remove_watch, page.getTitle()), Toast.LENGTH_SHORT).show();
								}
								// update the db!
								page.setWatched(isChecked);
								NovelsDao dao = NovelsDao.getInstance(getApplicationContext());
								dao.updatePageModel(page);
							}
						});

						ImageView ImageViewCover = (ImageView) synopsis.findViewById(R.id.cover);
						if (novelCol.getCoverBitmap() == null) {
							// IN app test, is returning empty bitmap
							Toast.makeText(getApplicationContext(), getResources().getString(R.string.toast_err_bitmap_empty), Toast.LENGTH_LONG).show();
						} else {

							if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) && getStrechCoverPreference()) {
								Drawable coverDrawable = new BitmapDrawable(getResources(), novelCol.getCoverBitmap());
								int coverHeight = novelCol.getCoverBitmap().getHeight();
								int coverWidth = novelCol.getCoverBitmap().getWidth();
								int screenWidth = (int) (UIHelper.getScreenHeight(this) * 0.9);
								int finalHeight = coverHeight * (screenWidth / coverWidth);
								ImageViewCover.setBackground(coverDrawable);
								ImageViewCover.getLayoutParams().height = finalHeight;
								ImageViewCover.getLayoutParams().width = screenWidth;
							} else {
								ImageViewCover.setImageBitmap(novelCol.getCoverBitmap());
							}
						}

						expandList.addHeaderView(synopsis);
					}
					bookModelAdapter = new BookModelAdapter(DisplayLightNovelDetailsActivity.this, novelCol.getBookCollections());
					expandList.setAdapter(bookModelAdapter);
				} catch (Exception e2) {
					Log.e(TAG, "Error when setting up chapter list: " + e2.getMessage(), e2);
					Toast.makeText(DisplayLightNovelDetailsActivity.this, getResources().getString(R.string.error_setting_chapter_list, e2.getMessage()), Toast.LENGTH_SHORT).show();
				}
				Log.d(TAG, "Loaded: " + novelCol.getPage());
			}
		} else {
			Log.e(TAG, e.getClass().toString() + ": " + e.getMessage(), e);
			Toast.makeText(getApplicationContext(), e.getClass().toString() + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
		}

		toggleProgressBar(false);
	}

	private boolean getColorPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_INVERT_COLOR, true);
	}

	private boolean getDownloadTouchPreference() {
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_DOWNLOAD_TOUCH, false);
	}

	private boolean getStrechCoverPreference() {
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_STRETCH_COVER, false);
	}

}
