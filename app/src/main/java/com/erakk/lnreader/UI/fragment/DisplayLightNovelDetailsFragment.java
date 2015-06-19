package com.erakk.lnreader.UI.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UI.activity.DisplayImageActivity;
import com.erakk.lnreader.UI.activity.DisplayLightNovelContentActivity;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.adapter.BookModelAdapter;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.IExtendedCallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.Util;
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

public class DisplayLightNovelDetailsFragment extends Fragment implements IExtendedCallbackNotifier<AsyncTaskResult<?>> {
    public static final String TAG = DisplayLightNovelDetailsFragment.class.toString();
    private PageModel page;
    private NovelCollectionModel novelCol;
    private final NovelsDao dao = NovelsDao.getInstance();

    private BookModelAdapter bookModelAdapter;
    private ExpandableListView expandList;

    private DownloadNovelContentTask downloadTask = null;
    private LoadNovelDetailsTask task = null;

    private TextView loadingText;
    private ProgressBar loadingBar;
    private String touchedForDownload;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Get intent and message
        page = new PageModel();
        String pageTitle = getArguments().getString(Constants.EXTRA_PAGE);
        if (Util.isStringNullOrEmpty(pageTitle)) {
            Log.w(TAG, "Page title is empty!");
        }
        page.setPage(pageTitle);

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        UIHelper.SetActionBarDisplayHomeAsUp((AppCompatActivity) getActivity(), true);
        View view = inflater.inflate(R.layout.fragment_display_light_novel_details, container, false);

        loadingText = (TextView) view.findViewById(R.id.emptyList);
        loadingBar = (ProgressBar) view.findViewById(R.id.empttListProgress);

        // setup listener
        expandList = (ExpandableListView) view.findViewById(R.id.chapter_list);
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
        setHasOptionsMenu(true);

        try {
            page = dao.getPageModel(page, null);
            getActivity().setTitle(page.getTitle());
        } catch (Exception e) {
            Log.e(TAG, "Error when getting Page Model for " + page.getPage(), e);
            getActivity().setTitle(page.getPage());
        }

        executeTask(page, false);

        return view;
    }


    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "OnResume: " + task.getStatus().toString());
        if (bookModelAdapter != null)
            bookModelAdapter.refreshData();
        if (expandList != null)
            expandList.invalidateViews();

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_display_light_novel_details, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "menu Option called.");
        switch (item.getItemId()) {
            case R.id.menu_refresh_chapter_list:
                Log.d(TAG, "Refreshing Details");
                executeTask(page, true);
                Toast.makeText(getActivity(), getResources().getString(R.string.refreshing_detail), Toast.LENGTH_SHORT).show();
                return true;
            case R.id.menu_details_download_all:
            /*
             * Download all chapters
			 */
                ArrayList<PageModel> availableChapters = novelCol.getFlattedChapterList();
                ArrayList<PageModel> notDownloadedChapters = new ArrayList<PageModel>();
                for (PageModel pageModel : availableChapters) {
                    if (pageModel.isMissing() || pageModel.isExternal())
                        continue;
                    else if (!pageModel.isDownloaded()
                            || (pageModel.isDownloaded() && dao.isContentUpdated(pageModel))) {
                        notDownloadedChapters.add(pageModel);
                    }
                }
                touchedForDownload = "Volumes";
                executeDownloadTask(notDownloadedChapters, true);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;

        MenuInflater inflater = getActivity().getMenuInflater();
        int type = ExpandableListView.getPackedPositionType(info.packedPosition);
        if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
            inflater.inflate(R.menu.context_menu_novel_details_volume, menu);
        } else if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            inflater.inflate(R.menu.context_menu_novel_details_chapter, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        if (!(item.getMenuInfo() instanceof ExpandableListView.ExpandableListContextMenuInfo))
            return super.onContextItemSelected(item);
        Log.d(TAG, "Context menu called");

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
                for (Iterator<PageModel> i = book.getChapterCollection().iterator(); i.hasNext(); ) {
                    PageModel temp = i.next();
                    if (temp.isDownloaded()) {
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
                Toast.makeText(getActivity(), String.format(getResources().getString(R.string.toast_clear_volume), bookClear.getTitle()), Toast.LENGTH_SHORT).show();
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
                Toast.makeText(getActivity(), getResources().getString(R.string.toast_mark_volume), Toast.LENGTH_SHORT).show();
                BookModel book2 = novelCol.getBookCollections().get(groupPosition);
                for (Iterator<PageModel> iPage = book2.getChapterCollection().iterator(); iPage.hasNext(); ) {
                    PageModel page = iPage.next();
                    page.setFinishedRead(true);
                    dao.updatePageModel(page);
                }
                bookModelAdapter.notifyDataSetChanged();
                return true;
            case R.id.mark_volume2:

			/*
			 * Implement code to mark entire volume as unread
			 */
                Toast.makeText(getActivity(), getResources().getString(R.string.toast_mark_volume2), Toast.LENGTH_SHORT).show();
                BookModel ubook2 = novelCol.getBookCollections().get(groupPosition);
                for (Iterator<PageModel> iPage = ubook2.getChapterCollection().iterator(); iPage.hasNext(); ) {
                    PageModel page = iPage.next();
                    page.setFinishedRead(false);
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
                downloadTask = new DownloadNovelContentTask(new PageModel[]{chapter}, touchedForDownload, this);
                downloadTask.execute();
                return true;
            case R.id.clear_chapter:

			/*
			 * Implement code to clear this chapter cache
			 */
                chapter = bookModelAdapter.getChild(groupPosition, childPosition);
                Toast.makeText(getActivity(), String.format(getResources().getString(R.string.toast_clear_chapter), chapter.getTitle()), Toast.LENGTH_SHORT).show();
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
                Toast.makeText(getActivity(), getResources().getString(R.string.toast_toggle_read), Toast.LENGTH_SHORT).show();
                return true;
            case R.id.delete_volume:

			/*
			 * Implement code to delete this volume cache
			 */
                BookModel bookDel = novelCol.getBookCollections().get(groupPosition);
                Toast.makeText(getActivity(), getResources().getString(R.string.delete_this_volume, bookDel.getTitle()), Toast.LENGTH_SHORT).show();
                dao.deleteBooks(bookDel);
                novelCol.getBookCollections().remove(groupPosition);
                bookModelAdapter.notifyDataSetChanged();
                return true;
            case R.id.delete_chapter:

			/*
			 * Implement code to delete this chapter cache
			 */
                chapter = bookModelAdapter.getChild(groupPosition, childPosition);
                Toast.makeText(getActivity(), getResources().getString(R.string.delete_this_chapter, chapter.getTitle()), Toast.LENGTH_SHORT).show();
                dao.deleteNovelContent(chapter);
                dao.deletePage(chapter);
                novelCol.getBookCollections().get(groupPosition).getChapterCollection().remove(chapter);
                bookModelAdapter.notifyDataSetChanged();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    // region IExtendedCallbackNotifier

    @Override
    public boolean downloadListSetup(String id, String toastText, int type, boolean hasError) {
        boolean exists = false;
        if (!this.isAdded() || this.isDetached())
            return exists;

        if (page != null && !Util.isStringNullOrEmpty(page.getTitle())) {
            if (type == 0) {
                if (LNReaderApplication.getInstance().isDownloadExists(id)) {
                    exists = true;
                    Toast.makeText(getActivity(), getResources().getString(R.string.toast_download_on_queue), Toast.LENGTH_SHORT).show();
                } else {
                    String name = page.getTitle() + " " + touchedForDownload;
                    Toast.makeText(getActivity(), getResources().getString(R.string.toast_downloading, name), Toast.LENGTH_SHORT).show();
                    LNReaderApplication.getInstance().addDownload(id, name);
                }
            } else if (type == 1) {
                Toast.makeText(getActivity(), "" + toastText, Toast.LENGTH_SHORT).show();
            } else if (type == 2) {
                String downloadDescription = LNReaderApplication.getInstance().getDownloadName(id);
                if (downloadDescription != null) {
                    String message = getResources().getString(R.string.toast_download_finish, page.getTitle(), downloadDescription);
                    if (hasError)
                        message = getResources().getString(R.string.toast_download_finish_with_error, page.getTitle(), downloadDescription);
                    Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
                }
                LNReaderApplication.getInstance().removeDownload(id);
            }
        }
        return exists;
    }

    @Override
    public void onProgressCallback(ICallbackEventData message) {
        toggleProgressBar(true);
        loadingText.setText(message.getMessage());
        if (message.getPercentage() > 0) {
            LNReaderApplication.getInstance().updateDownload(message.getSource(), message.getPercentage(), message.getMessage());
            if (loadingBar != null && loadingBar.getVisibility() == View.VISIBLE) {
                loadingBar.setIndeterminate(false);
                loadingBar.setMax(100);
                loadingBar.setProgress(message.getPercentage());
                loadingBar.setProgress(0);
                loadingBar.setProgress(message.getPercentage());
                loadingBar.setMax(100);
            }
        } else {
            loadingBar.setIndeterminate(true);
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
                    bookModelAdapter.notifyDataSetChanged();
                }
            }
            // from LoadNovelDetailsTask
            else if (result.getResultType() == NovelCollectionModel.class) {
                novelCol = (NovelCollectionModel) result.getResult();
                // now add the volume and chapter list.
                try {
                    // Prepare header
                    if (getArguments().getBoolean("show_list_child")) {

                        page = novelCol.getPageModel();
                        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
                        View synopsis = layoutInflater.inflate(R.layout.fragment_display_synopsis, null);
                        TextView textViewTitle = (TextView) synopsis.findViewById(R.id.title);
                        TextView textViewSynopsis = (TextView) synopsis.findViewById(R.id.synopsys);
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

                        CheckBox isWatched = (CheckBox) synopsis.findViewById(R.id.isWatched);
                        isWatched.setChecked(page.isWatched());
                        isWatched.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                if (isChecked) {
                                    Toast.makeText(getActivity(), getResources().getString(R.string.toast_add_watch, page.getTitle()), Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(getActivity(), getResources().getString(R.string.toast_remove_watch, page.getTitle()), Toast.LENGTH_SHORT).show();
                                }
                                // update the db!
                                page.setWatched(isChecked);
                                dao.updatePageModel(page);
                            }
                        });

                        // cover
                        ImageView ImageViewCover = (ImageView) synopsis.findViewById(R.id.cover);
                        if (novelCol.getCoverBitmap() == null) {
                            // IN app test, is returning empty bitmap
                            Toast.makeText(getActivity(), getResources().getString(R.string.toast_err_bitmap_empty), Toast.LENGTH_LONG).show();
                        } else {
                            ImageViewCover.setOnClickListener(new OnClickListener() {

                                @Override
                                public void onClick(View v) {
                                    handleCoverClick(novelCol.getCoverUrl());
                                }
                            });
                            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) && UIHelper.getStrechCoverPreference(getActivity())) {
                                Drawable coverDrawable = new BitmapDrawable(getResources(), novelCol.getCoverBitmap());
                                int coverHeight = novelCol.getCoverBitmap().getHeight();
                                int coverWidth = novelCol.getCoverBitmap().getWidth();
                                double screenWidth = this.getView().getWidth() * 0.9; //UIHelper.getScreenWidth(getActivity()) * 0.9;
                                double ratio = screenWidth / coverWidth;
                                int finalHeight = (int) (coverHeight * ratio);
                                ImageViewCover.setBackground(coverDrawable);
                                ImageViewCover.getLayoutParams().height = finalHeight;
                                ImageViewCover.getLayoutParams().width = (int) screenWidth;
                            } else {
                                ImageViewCover.setImageBitmap(novelCol.getCoverBitmap());
                                ImageViewCover.getLayoutParams().height = novelCol.getCoverBitmap().getHeight();
                                ImageViewCover.getLayoutParams().width = novelCol.getCoverBitmap().getWidth();
                            }
                        }

                        // update the header
                        ScrollView v = (ScrollView) expandList.findViewById(R.id.novel_synopsis_screen);
                        if (v != null) {
                            v.removeAllViews();
                            v.addView(synopsis);
                        } else expandList.addHeaderView(synopsis);
                    }
                    bookModelAdapter = new BookModelAdapter(getActivity(), novelCol.getBookCollections());
                    expandList.setAdapter(bookModelAdapter);
                } catch (Exception e2) {
                    Log.e(TAG, "Error when setting up chapter list: " + e2.getMessage(), e2);
                    Toast.makeText(getActivity(), getResources().getString(R.string.error_setting_chapter_list, e2.getMessage()), Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            String msg = e.getClass().toString();
            if (e.getMessage() != null)
                msg += ": " + e.getMessage();
            Log.e(TAG, msg, e);
            Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
        }
        toggleProgressBar(false);
    }

    // endregion

    // region private method
    private void handleCoverClick(URL coverUrl) {
        String bigCoverUrl = CommonParser.getImageFilePageFromImageUrl(coverUrl.toString());
        Intent intent = new Intent(getActivity(), DisplayImageActivity.class);
        intent.putExtra(Constants.EXTRA_IMAGE_URL, bigCoverUrl);
        startActivity(intent);
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
    private void executeTask(PageModel pageModel, boolean willRefresh) {
        String key = TAG + ":" + pageModel.getPage();
        task = new LoadNovelDetailsTask(pageModel, willRefresh, this, key);
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

    @SuppressLint("NewApi")
    private void executeDownloadTask(ArrayList<PageModel> chapters, boolean isAll) {
        if (page != null) {

            String key = TAG + ":DownloadChapters:" + page.getPage();
            if (isAll) {
                key = TAG + ":DownloadChaptersAll:" + page.getPage();
            }
            downloadTask = new DownloadNovelContentTask(chapters.toArray(new PageModel[chapters.size()]), key, this);
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

    private void loadChapter(PageModel chapter) {
        boolean useInternalWebView = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(Constants.PREF_USE_INTERNAL_WEBVIEW, false);

        if (chapter.isExternal() && !useInternalWebView) {
            try {
                Uri url = Uri.parse(chapter.getPage());
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, url);
                startActivity(browserIntent);
            } catch (Exception ex) {
                String message = getResources().getString(R.string.error_parsing_url, chapter.getPage());
                Log.e(TAG, message, ex);
                Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
            }
        } else {
            if (chapter.isExternal() || chapter.isDownloaded() || !UIHelper.getDownloadTouchPreference(getActivity())) {
                Intent intent = new Intent(getActivity(), DisplayLightNovelContentActivity.class);
                intent.putExtra(Constants.EXTRA_PAGE, chapter.getPage());
                startActivity(intent);
            } else {
                downloadTask = new DownloadNovelContentTask(new PageModel[]{chapter}, TAG + ":load_chapter:" + chapter.getPage(), DisplayLightNovelDetailsFragment.this);
                downloadTask.execute();
            }
        }
    }

    // endregion
}
