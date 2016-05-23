package com.erakk.lnreader.UI.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.erakk.lnreader.AlternativeLanguageInfo;
import com.erakk.lnreader.Constants;
import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.adapter.PageModelAdapter;
import com.erakk.lnreader.callback.CallbackEventData;
import com.erakk.lnreader.callback.DownloadCallbackEventData;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.IExtendedCallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.PageModel;
import com.erakk.lnreader.task.AddNovelTask;
import com.erakk.lnreader.task.AsyncTaskResult;
import com.erakk.lnreader.task.DownloadNovelDetailsTask;
import com.erakk.lnreader.task.LoadAlternativeTask;
import com.erakk.lnreader.task.LoadNovelsTask;

import java.util.ArrayList;

/*
 * Author: Nandaka
 * Copy from: NovelsActivity.java
 */

public class DisplayLightNovelListFragment extends ListFragment implements IExtendedCallbackNotifier<AsyncTaskResult<?>>, INovelListHelper {
    private static final String TAG = DisplayLightNovelListFragment.class.toString();

    private final ArrayList<PageModel> listItems = new ArrayList<PageModel>();
    private PageModelAdapter adapter;

    private LoadNovelsTask task = null;
    private LoadAlternativeTask altTask = null;
    private DownloadNovelDetailsTask downloadTask = null;
    private AddNovelTask addTask = null;

    private boolean onlyWatched = false;
    private String touchedForDownload;
    private String mode;
    private String lang;

    private TextView loadingText;
    private ProgressBar loadingBar;

    private IFragmentListener mFragListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mFragListener = (IFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement FragListener");
        }

        mode = getArguments().getString(Constants.EXTRA_NOVEL_LIST_MODE);
        if(mode == null) mode = Constants.EXTRA_NOVEL_LIST_MODE_MAIN;

        onlyWatched = getArguments().getBoolean(Constants.EXTRA_ONLY_WATCHED, false);
        lang = getArguments().getString(Constants.EXTRA_NOVEL_LANG);
        Log.i(TAG, "IsWatched: " + onlyWatched + " Mode: " + mode + " lang: " + lang);

        String page = getArguments().getString(Constants.EXTRA_PAGE);
        String pageTitleHint = getArguments().getString(Constants.EXTRA_TITLE);
        if (page != null)
            loadNovel(page, pageTitleHint);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_display_light_novel_list, container, false);

        if (onlyWatched) {
            getActivity().setTitle("Watched Novels");
        } else if (lang != null) {
            getActivity().setTitle("Alt. Novels");
        } else {
            getActivity().setTitle("Light Novels");
        }

        loadingText = (TextView) view.findViewById(R.id.emptyList);
        loadingBar = (ProgressBar) getActivity().findViewById(R.id.empttListProgress);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        /************************************************
         * These lines of code require the ListView to already be created before
         * they are used, hence, put in the onStart() method
         ****************************************************/

        registerForContextMenu(getListView());

        // Encapsulated in updateContent
        updateContent(false, onlyWatched);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_display_novel_watched, menu);
        MenuItem searchItem = menu.findItem(R.id.search_item);
        if(searchItem != null) {
            SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
            if (searchView != null) {
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        Log.d("SEARCH", "Search: + " + query);
                        performSearch(query);
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String query) {
                        performSearch(query);
                        return false;
                    }
                });
                searchView.setOnCloseListener(new SearchView.OnCloseListener() {
                    @Override
                    public boolean onClose() {
                        performSearch(null);
                        return false;
                    }
                });
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                    searchView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                        @Override
                        public void onViewAttachedToWindow(View v) {

                        }

                        @Override
                        public void onViewDetachedFromWindow(View v) {
                            performSearch(null);
                        }
                    });
                }
            }
        }
    }

    private void performSearch(String query) {
        if(adapter != null) {
            adapter.filterData(query);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_download_all_info:
                this.downloadAllNovelInfo();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        // Get the item that was clicked
        PageModel o = adapter.getItem(position);
        String novel = o.toString();

        // Create a bundle containing information about the novel that is clicked
        Bundle bundle = new Bundle();
        bundle.putString(Constants.EXTRA_NOVEL, novel);
        bundle.putString(Constants.EXTRA_PAGE, o.getPage());
        bundle.putString(Constants.EXTRA_TITLE, o.getTitle());
        bundle.putBoolean(Constants.EXTRA_ONLY_WATCHED, onlyWatched);

        mFragListener.changeNextFragment(bundle);

        Log.d(TAG, o.getPage() + " (" + o.getTitle() + ")");
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_novel_item, menu);
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        if (!(item.getMenuInfo() instanceof AdapterView.AdapterContextMenuInfo))
            return super.onContextItemSelected(item);
        Log.d(TAG, "Context menu called");
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.add_to_watch:
            /*
             * Implement code to toggle watch of this novel
			 */
                if (info.position > -1) {
                    PageModel novel = listItems.get(info.position);
                    if (novel.isWatched()) {
                        novel.setWatched(false);
                        Toast.makeText(getActivity(), "Removed from watch list: " + novel.getTitle(), Toast.LENGTH_SHORT).show();
                    } else {
                        novel.setWatched(true);
                        Toast.makeText(getActivity(), "Added to watch list: " + novel.getTitle(), Toast.LENGTH_SHORT).show();
                    }
                    NovelsDao.getInstance().updatePageModel(novel);
                    adapter.notifyDataSetChanged();
                }
                return true;
            case R.id.download_novel:
            /*
             * Implement code to download novel synopsis
			 */
                if (info.position > -1) {
                    PageModel novel = listItems.get(info.position);
                    ArrayList<PageModel> novels = new ArrayList<PageModel>();
                    novels.add(novel);
                    touchedForDownload = novel.getTitle() + "'s information";
                    executeDownloadTask(novels);
                }
                return true;
            case R.id.delete_novel:
                if (info.position > -1) {
                    toggleProgressBar(true);
                    PageModel novel = listItems.get(info.position);
                    int result = NovelsDao.getInstance().deleteNovel(novel);
                    if (result > 0) {
                        listItems.remove(novel);
                        adapter.notifyDataSetChanged();
                    }
                    toggleProgressBar(false);
                }
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    // region private methods

    private void updateContent(boolean isRefresh, boolean onlyWatched) {
        // use the cached items.
        if (!isRefresh && listItems != null && listItems.size() > 0)
            return;

        try {
            // Check size
            int resourceId = R.layout.item_novel;
            if (UIHelper.isSmallScreen(getActivity())) {
                resourceId = R.layout.item_novel_small;
            }
            if (adapter != null) {
                adapter.setResourceId(resourceId);
            } else {
                adapter = new PageModelAdapter(getActivity(), resourceId, listItems);
            }
            boolean alphOrder = UIHelper.isAlphabeticalOrder(getActivity());

            if (lang == null)
                executeTask(isRefresh, onlyWatched, alphOrder);
            else {
                executeAltTask(isRefresh, alphOrder, lang);
            }

            setListAdapter(adapter);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            Toast.makeText(getActivity(), "Error when updating: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @SuppressLint("NewApi")
    private void executeTask(boolean isRefresh, boolean onlyWatched, boolean alphOrder) {
        task = new LoadNovelsTask(this, isRefresh, onlyWatched, alphOrder, mode);
        String key = null;
        if (mode.equalsIgnoreCase(Constants.EXTRA_NOVEL_LIST_MODE_MAIN)) {
            key = TAG + ":LoadNovelsTask:" + Constants.ROOT_NOVEL_ENGLISH;
        } else if (mode.equalsIgnoreCase(Constants.EXTRA_NOVEL_LIST_MODE_TEASER)) {
            key = TAG + ":LoadNovelsTask:" + Constants.ROOT_TEASER;
        } else if (mode.equalsIgnoreCase(Constants.EXTRA_NOVEL_LIST_MODE_ORIGINAL)) {
            key = TAG + ":LoadNovelsTask:" + Constants.ROOT_ORIGINAL;
        } else if (mode.equalsIgnoreCase(Constants.EXTRA_NOVEL_LIST_MODE_WEB)) {
            key = TAG + ":LoadNovelsTask:" + Constants.ROOT_WEB;
        }
        boolean isAdded = LNReaderApplication.getInstance().addTask(key, task);
        if (isAdded) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            else
                task.execute();
        } else {
            Log.i(TAG, "Continue execute task: " + key);
            LoadNovelsTask tempTask = (LoadNovelsTask) LNReaderApplication.getInstance().getTask(key);
            if (tempTask != null) {
                task = tempTask;
                task.owner = this;
            }
        }
        toggleProgressBar(true);
    }

    private void executeAltTask(boolean isRefresh, boolean alphOrder, String lang) {
        altTask = new LoadAlternativeTask(this, isRefresh, alphOrder, lang);
        String key = null;
        if (lang != null)
            key = TAG + ":LoadAlternativeTask:" + AlternativeLanguageInfo.getAlternativeLanguageInfo().get(lang).getCategoryInfo();
        boolean isAdded = LNReaderApplication.getInstance().addTask(key, altTask);
        if (isAdded) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                altTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            else
                altTask.execute();
        } else {
            Log.i(TAG, "Continue execute task: " + key);
            LoadAlternativeTask tempTask = (LoadAlternativeTask) LNReaderApplication.getInstance().getTask(key);
            if (tempTask != null) {
                altTask = tempTask;
                altTask.owner = this;
            }
        }
        toggleProgressBar(true);
    }

    @SuppressLint("NewApi")
    private void executeDownloadTask(ArrayList<PageModel> novels) {
        downloadTask = new DownloadNovelDetailsTask(this);
        if (novels == null || novels.size() == 0)
            return;

        String key = DisplayLightNovelDetailsFragment.TAG + ":" + novels.get(0).getPage();
        if (novels.size() > 1) {
            if (mode.equalsIgnoreCase(Constants.EXTRA_NOVEL_LIST_MODE_MAIN)) {
                key = DisplayLightNovelDetailsFragment.TAG + ":All_Novels";
            } else if (mode.equalsIgnoreCase(Constants.EXTRA_NOVEL_LIST_MODE_TEASER)) {
                key = DisplayLightNovelDetailsFragment.TAG + ":All_Teasers";
            } else if (mode.equalsIgnoreCase(Constants.EXTRA_NOVEL_LIST_MODE_ORIGINAL)) {
                key = DisplayLightNovelDetailsFragment.TAG + ":All_Original";
            }
        }
        boolean isAdded = LNReaderApplication.getInstance().addTask(key, downloadTask);
        if (isAdded) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                downloadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, novels.toArray(new PageModel[novels.size()]));
            else
                downloadTask.execute(novels.toArray(new PageModel[novels.size()]));
        } else {
            Log.i(TAG, "Continue download task: " + key);
            DownloadNovelDetailsTask tempTask = (DownloadNovelDetailsTask) LNReaderApplication.getInstance().getTask(key);
            if (tempTask != null) {
                downloadTask = tempTask;
                downloadTask.owner = this;
            }
            toggleProgressBar(true);
        }
    }

    @SuppressLint("NewApi")
    private void executeAddTask(PageModel novel) {
        String key = DisplayLightNovelDetailsFragment.TAG + ":Add:" + novel.getPage();
        addTask = new AddNovelTask(this, key);
        boolean isAdded = LNReaderApplication.getInstance().addTask(key, addTask);
        if (isAdded) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                addTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new PageModel[]{novel});
            else
                addTask.execute(new PageModel[]{novel});
        } else {
            Log.i(TAG, "Continue Add task: " + key);
            AddNovelTask tempTask = (AddNovelTask) LNReaderApplication.getInstance().getTask(key);
            if (tempTask != null) {
                addTask = tempTask;
                addTask.owner = this;
            }
        }
        toggleProgressBar(true);
    }

    private void loadNovel(String page, String novelTitleHint) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.EXTRA_PAGE, page);
        bundle.putString(Constants.EXTRA_TITLE, novelTitleHint);
        mFragListener.changeNextFragment(bundle);
    }

    @Override
    public void refreshList() {
        boolean onlyWatched = getActivity().getIntent().getBooleanExtra(Constants.EXTRA_ONLY_WATCHED, false);
        updateContent(true, onlyWatched);
        Toast.makeText(getActivity(), "Refreshing Main Novel...", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void downloadAllNovelInfo() {
        if (onlyWatched) {
            touchedForDownload = "Watched Light Novels information";
        } else if (mode == null || mode.equalsIgnoreCase(Constants.EXTRA_NOVEL_LIST_MODE_MAIN)) {
            touchedForDownload = "All Main Light Novels information";
        } else if (mode.equalsIgnoreCase(Constants.EXTRA_NOVEL_LIST_MODE_TEASER)) {
            touchedForDownload = "All Teaser Light Novels information";
        } else if (mode.equalsIgnoreCase(Constants.EXTRA_NOVEL_LIST_MODE_ORIGINAL)) {
            touchedForDownload = "All Original Light Novels information";
        }
        executeDownloadTask(listItems);
    }

    @Override
    public void manualAdd() {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setTitle(getString(R.string.add_novel_main, mode));

        LayoutInflater factory = LayoutInflater.from(getActivity());
        View inputView = factory.inflate(R.layout.dialog_add_new_novel, null);
        final EditText inputName = (EditText) inputView.findViewById(R.id.page);
        final EditText inputTitle = (EditText) inputView.findViewById(R.id.title);
        alert.setView(inputView);
        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                if (whichButton == DialogInterface.BUTTON_POSITIVE) {
                    handleOK(inputName, inputTitle);
                }
            }
        });
        alert.setNegativeButton("Cancel", null);
        alert.show();
    }

    private void handleOK(EditText input, EditText inputTitle) {
        String novel = input.getText().toString();
        String title = inputTitle.getText().toString();
        if (novel != null && novel.length() > 0 && inputTitle != null && inputTitle.length() > 0) {
            PageModel temp = new PageModel();
            temp.setPage(novel);
            temp.setTitle(title);
            temp.setType(PageModel.TYPE_NOVEL);
            temp.setParent(Constants.ROOT_NOVEL_ENGLISH);
            if (mode.equalsIgnoreCase(Constants.EXTRA_NOVEL_LIST_MODE_MAIN)) {
                temp.setParent(Constants.ROOT_NOVEL_ENGLISH);
            } else if (mode.equalsIgnoreCase(Constants.EXTRA_NOVEL_LIST_MODE_TEASER)) {
                temp.setParent(Constants.ROOT_TEASER);
                temp.setStatus(Constants.STATUS_TEASER);
            } else if (mode.equalsIgnoreCase(Constants.EXTRA_NOVEL_LIST_MODE_ORIGINAL)) {
                temp.setParent(Constants.ROOT_ORIGINAL);
                temp.setStatus(Constants.STATUS_ORIGINAL);
            }
            executeAddTask(temp);
        } else {
            Toast.makeText(getActivity(), "Empty Input", Toast.LENGTH_LONG).show();
        }
    }

    private void toggleProgressBar(boolean show) {
        View root = getView();
        if (root != null) {
            ListView listView = getListView();

            if (listView == null || loadingText == null || loadingBar == null)
                return;

            if (show) {
                loadingText.setText("Loading List, please wait...");
                loadingText.setVisibility(TextView.VISIBLE);
                loadingBar.setVisibility(ProgressBar.VISIBLE);
                listView.setVisibility(ListView.GONE);
            } else {
                loadingText.setVisibility(TextView.GONE);
                loadingBar.setVisibility(ProgressBar.GONE);
                listView.setVisibility(ListView.VISIBLE);
            }
        }
    }
    // endregion

    // region IExtendedCallbackNotifier<AsyncTaskResult<?>> implementation

    @Override
    public void onCompleteCallback(ICallbackEventData message, AsyncTaskResult<?> result) {
        if (!isAdded())
            return;

        Exception e = result.getError();
        if (e == null) {
            Class t = result.getResultType();

            // from LoadNovelsTask
            if (t == PageModel[].class) {
                PageModel[] list = (PageModel[]) result.getResult();
                Log.d(TAG, "LoadNovelsTask result ok");
                if (list != null && list.length > 0) {
                    adapter.clear();
                    adapter.addAll(list);
                    toggleProgressBar(false);

                    // Show message if watch list is empty
                    if (list.length == 0 && onlyWatched) {
                        Log.d(TAG, "WatchList result set message empty");
                        if (loadingText != null) {
                            loadingText.setVisibility(TextView.VISIBLE);
                            loadingText.setText("Watch List is empty.");
                        }
                    }
                } else {
                    toggleProgressBar(false);
                    if (loadingText != null) {
                        loadingText.setVisibility(TextView.VISIBLE);
                        loadingText.setText(getResources().getString(R.string.list_empty));
                    }
                    Log.w(TAG, "Empty ArrayList!");
                }
            }
            // from DownloadNovelDetailsTask
            else if (t == NovelCollectionModel[].class) {
                onProgressCallback(new CallbackEventData("Download complete.", "DownloadNovelDetailsTask"));
                NovelCollectionModel[] list = (NovelCollectionModel[]) result.getResult();
                for (NovelCollectionModel novelCol : list) {
                    try {
                        PageModel page = novelCol.getPageModel();
                        boolean found = false;
                        for (PageModel temp : adapter.data) {
                            if (temp.getPage().equalsIgnoreCase(page.getPage())) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            adapter.data.add(page);
                        }
                    } catch (Exception e1) {
                        Log.e(TAG, e1.getClass().toString() + ": " + e1.getMessage(), e1);
                    }
                }
                adapter.notifyDataSetChanged();
                toggleProgressBar(false);
            } else {
                Log.e(TAG, "Unknown ResultType: " + t.getName());
            }
        } else {
            Log.e(TAG, e.getClass().toString() + ": " + e.getMessage(), e);
            Toast.makeText(getActivity(), e.getClass().toString() + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        toggleProgressBar(false);
    }

    @Override
    public void onProgressCallback(ICallbackEventData message) {
        if (loadingText != null && loadingText.getVisibility() == TextView.VISIBLE)
            loadingText.setText(message.getMessage());

        if (message instanceof DownloadCallbackEventData) {
            DownloadCallbackEventData downloadData = (DownloadCallbackEventData) message;
            LNReaderApplication.getInstance().updateDownload(message.getSource(), downloadData.getPercentage(), message.getMessage());

            if (loadingBar != null && loadingBar.getVisibility() == View.VISIBLE) {
                loadingBar.setIndeterminate(false);
                loadingBar.setMax(100);
                loadingBar.setProgress(downloadData.getPercentage());
                loadingBar.setProgress(0);
                loadingBar.setProgress(downloadData.getPercentage());
                loadingBar.setMax(100);
            }
        }
    }

    @Override
    public boolean downloadListSetup(String id, String toastText, int type, boolean hasError) {
        if (!this.isAdded() || this.isDetached())
            return false;

        boolean exists = false;
        if (type == 0) {
            if (LNReaderApplication.getInstance().isDownloadExists(id)) {
                exists = true;
                Toast.makeText(getActivity(), "Download already on queue.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), "Downloading " + touchedForDownload + ".", Toast.LENGTH_SHORT).show();
                LNReaderApplication.getInstance().addDownload(id, touchedForDownload);
            }
        } else if (type == 1) {
            Toast.makeText(getActivity(), toastText, Toast.LENGTH_SHORT).show();
        } else if (type == 2) {
            String message = String.format("%s's download finished!", LNReaderApplication.getInstance().getDownloadName(id));
            if (hasError)
                message = String.format("%s's download finished with error(s)!", LNReaderApplication.getInstance().getDownloadName(id));
            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
            LNReaderApplication.getInstance().removeDownload(id);
        }
        return exists;
    }

    // endregion
}
