package com.erakk.lnreader.UI.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTabHost;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.R;
import com.erakk.lnreader.activity.INovelListHelper;


public class DisplayNovelTabFragment extends Fragment {
	// TabSpec Names
	private static final String MAIN_SPEC = "Main";
	private static final String TEASER_SPEC = "Teaser";
	private static final String ORIGINAL_SPEC = "Original";
	private static final String TAG = DisplayNovelTabFragment.class.toString();
	private FragmentTabHost mTabHost;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.fragment_display_novel_tab, container, false);

		mTabHost = (FragmentTabHost) view.findViewById(android.R.id.tabhost);
		mTabHost.setup(getActivity(), getChildFragmentManager(), R.id.content);

		Bundle mainBundle = new Bundle();
		mainBundle.putString(Constants.EXTRA_NOVEL_LIST_MODE, Constants.EXTRA_NOVEL_LIST_MODE_MAIN);
		mainBundle.putBoolean(Constants.EXTRA_ONLY_WATCHED, false);
		mTabHost.addTab(mTabHost.newTabSpec(MAIN_SPEC).setIndicator(MAIN_SPEC),
				DisplayLightNovelListFragment.class, mainBundle);

		Bundle teaserBundle = new Bundle();
		teaserBundle.putString(Constants.EXTRA_NOVEL_LIST_MODE, Constants.EXTRA_NOVEL_LIST_MODE_TEASER);
		mTabHost.addTab(mTabHost.newTabSpec(TEASER_SPEC).setIndicator(TEASER_SPEC),
				DisplayLightNovelListFragment.class, teaserBundle);

		Bundle oriBundle = new Bundle();
		oriBundle.putString(Constants.EXTRA_NOVEL_LIST_MODE, Constants.EXTRA_NOVEL_LIST_MODE_ORIGINAL);
		mTabHost.addTab(mTabHost.newTabSpec(ORIGINAL_SPEC).setIndicator(ORIGINAL_SPEC),
				DisplayLightNovelListFragment.class, oriBundle);

		return view;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// set menu for main/teaser/original
		inflater.inflate(R.menu.fragment_display_novel_tab, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Fragment currentTab = getChildFragmentManager().findFragmentById(R.id.content);
		Log.d(TAG, "Current fragment: " + currentTab.getClass().toString());
		switch(item.getItemId()) {
		case R.id.menu_manual_add:
			if(currentTab instanceof INovelListHelper)
				((INovelListHelper)currentTab).manualAdd();
			return true;
		case R.id.menu_download_all_info:
			if(currentTab instanceof INovelListHelper)
				((INovelListHelper)currentTab).downloadAllNovelInfo();
			return true;
		case R.id.menu_refresh_novel_list:
			if(currentTab instanceof INovelListHelper)
				((INovelListHelper)currentTab).refreshList();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}