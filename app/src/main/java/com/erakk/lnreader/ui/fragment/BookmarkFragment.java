package com.erakk.lnreader.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.adapter.BookmarkModelAdapter;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.IExtendedCallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.model.BookmarkModel;
import com.erakk.lnreader.task.AsyncTaskResult;
import com.erakk.lnreader.task.LoadBookmarkTask;
import com.erakk.lnreader.ui.activity.DisplayLightNovelContentActivity;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A fragment representing a list of Items.
 * <p/>
 * <p/>
 */
public class BookmarkFragment extends ListFragment implements IExtendedCallbackNotifier<AsyncTaskResult<BookmarkModel[]>> {

    private BookmarkModelAdapter adapter = null;
    private ArrayList<BookmarkModel> bookmarks = null;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public BookmarkFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        getActivity().setTitle(R.string.bookmarks);
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        getBookmarks();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        // Get the item that was clicked
        BookmarkModel page = adapter.getItem(position);

        // Create new intent
        Intent intent = new Intent(getActivity(), DisplayLightNovelContentActivity.class);
        intent.putExtra(Constants.EXTRA_PAGE, page.getPage());
        intent.putExtra(Constants.EXTRA_P_INDEX, page.getpIndex());
        startActivity(intent);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_display_bookmark, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_bookmark_delete_selected:
                handleDeleteBookmark();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // region private methods

    private void getBookmarks() {
        LoadBookmarkTask task = new LoadBookmarkTask(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        else
            task.execute();
    }

    public void handleDeleteBookmark() {
        if (bookmarks != null) {
            for (BookmarkModel bookmark : bookmarks) {
                if (bookmark.isSelected()) NovelsDao.getInstance().deleteBookmark(bookmark);
            }
            if (adapter != null) adapter.refreshData();
        }
    }

    @Override
    public void onCompleteCallback(ICallbackEventData message, AsyncTaskResult<BookmarkModel[]> result) {

        int resourceId = R.layout.item_bookmark;
        if (UIHelper.isSmallScreen(getActivity())) {
            resourceId = R.layout.item_bookmark;
        }
        BookmarkModel[] temp = result.getResult();
        bookmarks = new ArrayList<BookmarkModel>(Arrays.asList(temp));

        adapter = new BookmarkModelAdapter(getActivity(), resourceId, bookmarks, null);
        adapter.showPage = true;
        adapter.showCheckBox = true;
        setListAdapter(adapter);
    }

    @Override
    public void onProgressCallback(ICallbackEventData message) {

    }

    @Override
    public boolean downloadListSetup(String taskId, String message, int setupType, boolean hasError) {
        return false;
    }
    // endregion

}
