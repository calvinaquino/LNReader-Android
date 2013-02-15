package com.erakk.lnreader.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTabHost;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.erakk.lnreader.R;


public class DisplayNovelTabFragment extends SherlockFragment {
    // TabSpec Names
    private static final String MAIN_SPEC = "Main";
    private static final String TEASER_SPEC = "Teaser";
    private static final String ORIGINAL_SPEC = "Original";
    private FragmentTabHost mTabHost;
 
    
    
    @Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}



	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
    	
    	View view = inflater.inflate(R.layout.fragment_display_novel_tab, container, false);
    	
        mTabHost = (FragmentTabHost) view.findViewById(android.R.id.tabhost);
        mTabHost.setup(getSherlockActivity(), getChildFragmentManager(), R.id.content);

        mTabHost.addTab(mTabHost.newTabSpec(MAIN_SPEC).setIndicator(MAIN_SPEC),
                DisplayLightNovelListFragment.class, null);
        mTabHost.addTab(mTabHost.newTabSpec(TEASER_SPEC).setIndicator(TEASER_SPEC),
                DisplayTeaserListFragment.class, null);
        mTabHost.addTab(mTabHost.newTabSpec(ORIGINAL_SPEC).setIndicator(ORIGINAL_SPEC),
                DisplayOriginalListFragment.class, null);
        
//        setTabColor();
//        
//        mTabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
//            public void onTabChanged(String tabId) {
//            	setTabColor();
//            }
//        });

        //Cheap preload list hack.
        mTabHost.setCurrentTabByTag(TEASER_SPEC);
        mTabHost.setCurrentTabByTag(ORIGINAL_SPEC);
        mTabHost.setCurrentTabByTag(MAIN_SPEC);
        
        return view;
	}



	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// TODO Auto-generated method stub
		super.onCreateOptionsMenu(menu, inflater);
		menu.add(0, 0, Menu.NONE, "Manual \nAdd").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add(0,1,Menu.NONE, "Download\n All Info").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
	}



	@Override
	public boolean onOptionsItemSelected(
			com.actionbarsherlock.view.MenuItem item) {
		// TODO Auto-generated method stub
		Fragment currentTab = getChildFragmentManager().findFragmentById(R.id.content);
		switch(item.getItemId()) {
			case 0:
				// Need to enquire about this, (why is it all "refresh" except for the main one)
				if(currentTab instanceof DisplayLightNovelListFragment) {
					((DisplayLightNovelListFragment)currentTab).manualAdd();
				}
				else if(currentTab instanceof DisplayTeaserListFragment) {
					((DisplayTeaserListFragment)currentTab).refreshList();
				}
				else if(currentTab instanceof DisplayOriginalListFragment) {
					((DisplayOriginalListFragment)currentTab).refreshList();
				}
				return true;
			case 1:
				if(currentTab instanceof DisplayLightNovelListFragment) {
					((DisplayLightNovelListFragment)currentTab).downloadAllNovelInfo();								
				}
				else if(currentTab instanceof DisplayTeaserListFragment) {
					((DisplayTeaserListFragment)currentTab).downloadAllNovelInfo();
				}
				else if(currentTab instanceof DisplayOriginalListFragment) {
					((DisplayOriginalListFragment)currentTab).downloadAllNovelInfo();
				}
				return true;
			case R.id.menu_refresh_novel_list:
				if(currentTab instanceof DisplayLightNovelListFragment) {
					((DisplayLightNovelListFragment)currentTab).refreshList();								
				}
				else if(currentTab instanceof DisplayTeaserListFragment) {
					((DisplayTeaserListFragment)currentTab).refreshList();
				}
				else if(currentTab instanceof DisplayOriginalListFragment) {
					((DisplayOriginalListFragment)currentTab).refreshList();
				}
		}
		
		return super.onOptionsItemSelected(item);
	}

    
	
//    
//    public static void setTabColor() {
//        for(int i=0;i<mTabHost.getTabWidget().getChildCount();i++)
//        {
////            tabHost.getTabWidget().getChildAt(i).setBackgroundColor(Color.parseColor("#2D5A9C")); //unselected
//            mTabHost.getTabWidget().getChildAt(i).setBackgroundColor(Color.parseColor("#000000")); //unselected
//        }
////        tabHost.getTabWidget().getChildAt(tabHost.getCurrentTab()).setBackgroundColor(Color.parseColor("#234B7E")); // selected
//        mTabHost.getTabWidget().getChildAt(mTabHost.getCurrentTab()).setBackgroundColor(Color.parseColor("#708090")); // selected
//    }
    
    
}