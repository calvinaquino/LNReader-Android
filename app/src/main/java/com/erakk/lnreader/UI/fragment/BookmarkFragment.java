package com.erakk.lnreader.UI.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.activity.DisplayLightNovelContentActivity;
import com.erakk.lnreader.adapter.BookmarkModelAdapter;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.model.BookmarkModel;

import java.util.ArrayList;

/**
 * A fragment representing a list of Items.
 * <p/>
 * <p/>
 */
public class BookmarkFragment extends ListFragment {

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
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((AppCompatActivity)activity).getSupportActionBar().setTitle(getResources().getString(R.string.bookmarks));
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
        // TODO: replace with fragment
        // Create new intent
        Intent intent = new Intent(getActivity(), DisplayLightNovelContentActivity.class);
        intent.putExtra(Constants.EXTRA_PAGE, page.getPage());
        intent.putExtra(Constants.EXTRA_P_INDEX, page.getpIndex());
        startActivity(intent);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.activity_display_bookmark, menu);
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
        int resourceId = R.layout.bookmark_list_item;
        if (UIHelper.isSmallScreen(getActivity())) {
            resourceId = R.layout.bookmark_list_item;
        }
        bookmarks = NovelsDao.getInstance().getAllBookmarks(UIHelper.getAllBookmarkOrder(getActivity()));
        adapter = new BookmarkModelAdapter(getActivity(), resourceId, bookmarks, null);
        adapter.showPage = true;
        adapter.showCheckBox = true;
        setListAdapter(adapter);
    }

    public void handleDeleteBookmark() {
        if (bookmarks != null) {
            for (BookmarkModel bookmark : bookmarks) {
                if (bookmark.isSelected()) NovelsDao.getInstance().deleteBookmark(bookmark);
            }
            if (adapter != null) adapter.refreshData();
        }
    }
    // endregion

}
