package com.erakk.lnreader.UI.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleExpandableListAdapter;
import android.widget.Toast;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UI.activity.DisplayLightNovelContentActivity;
import com.erakk.lnreader.UI.activity.NovelListContainerActivity;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.adapter.SearchPageModelAdapter;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.model.PageModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SearchFragment extends Fragment {
    protected static final String TAG = SearchFragment.class.toString();
    private boolean isInverted;
    private final Handler mHandler = new Handler();
    private long mStartTime;
    private ProgressBar progress = null;
    SearchPageModelAdapter adapter = null;
    private CheckBox chkNovelOnly = null;
    private Context ctx;
    private ExpandableListView languageSelection = null;

    public SearchFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ctx = getActivity();

        View view = inflater.inflate(R.layout.fragment_search, null);

        final EditText search = (EditText) view.findViewById(R.id.searchText);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                progress.setVisibility(View.VISIBLE);
                doSearch(s);
            }
        });

		/* A section for Expandable List */
        languageSelection = (ExpandableListView) view.findViewById(R.id.categorySearchLanguage);


        languageSelection.setAdapter(setupAdapter());
        languageSelection.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {

            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
                SharedPreferences.Editor editor = sharedPrefs.edit();
                for (int i = 0; i < Constants.languageList.length; i++) {
                    if (i == childPosition) {
                        editor.putBoolean("Search:" + Constants.languageList[i], !sharedPrefs.getBoolean("Search:" + Constants.languageList[i], true));
                        editor.commit();
                    }
                }
                doSearch(search.getEditableText());
                progress.setVisibility(View.VISIBLE);
                languageSelection.setAdapter(setupAdapter());
                return false;
            }
        });
        /* End of Expandable List section */

        ListView searchResult = (ListView) view.findViewById(R.id.searchResult);
        int resourceId = R.layout.item_novel;
        if (UIHelper.isSmallScreen(getActivity())) {
            resourceId = R.layout.item_novel_small;
        }
        adapter = new SearchPageModelAdapter(getActivity(), resourceId, new ArrayList<PageModel>());
        searchResult.setAdapter(adapter);
        searchResult.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                LoadItem(arg2);
            }
        });

        progress = (ProgressBar) view.findViewById(R.id.progressBar1);

        chkNovelOnly = (CheckBox) view.findViewById(R.id.chkNovelOnly);
        chkNovelOnly.setChecked(PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(Constants.PREF_IS_NOVEL_ONLY, false));
        chkNovelOnly.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SetNovelOnly(isChecked);
                doSearch(search.getEditableText());
                progress.setVisibility(View.VISIBLE);
            }
        });


        getActivity().setTitle(R.string.search);
        return view;
    }

    private SimpleExpandableListAdapter setupAdapter() {
        return new SimpleExpandableListAdapter(getActivity(), createGroupList(), // groupData describes the first-level entries
                R.layout.fragment_search_item_header, // Layout for the first-level entries
                new String[]{"searchOption"}, // Key in the groupData maps to display
                new int[]{R.id.optionName}, // Data under "colorName" key goes into this TextView
                createChildList(), // childData describes second-level entries
                R.layout.fragment_search_item_text, // Layout for second-level entries
                new String[]{"languagePreferences"}, // Keys in childData maps to display
                new int[]{R.id.languageOptionName} // Data under the keys above go into these TextViews
        );
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    /* A section for Expandable List */
    private List<HashMap<String, String>> createGroupList() {
        ArrayList<HashMap<String, String>> advancedSearch = new ArrayList<HashMap<String, String>>();
        HashMap<String, String> option = new HashMap<String, String>();
        option.put("searchOption", getResources().getString(R.string.category_search_language));
        advancedSearch.add(option);
        return advancedSearch;
    }

    private List<ArrayList<HashMap<String, String>>> createChildList() {
        ArrayList<ArrayList<HashMap<String, String>>> firstTierOption = new ArrayList<ArrayList<HashMap<String, String>>>();
        ArrayList<HashMap<String, String>> secondTierOption = new ArrayList<HashMap<String, String>>();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = sharedPrefs.edit();
        for (int n = 0; n < Constants.languageList.length; n++) {
            HashMap<String, String> child = new HashMap<String, String>();
            /* Put a shared Preference if null */
            if (!sharedPrefs.contains("Search:" + Constants.languageList[n])) {
                editor.putBoolean("Search:" + Constants.languageList[n], true);
                editor.commit();
            }
            if (sharedPrefs.getBoolean("Search:" + Constants.languageList[n], true))
                child.put("languagePreferences", Constants.languageList[n] + " : " + getResources().getString(R.string.enabled));
            else
                child.put("languagePreferences", Constants.languageList[n] + " : " + getResources().getString(R.string.disabled));
            secondTierOption.add(child);
        }
        firstTierOption.add(secondTierOption);
        return firstTierOption;
    }

	/* End of Expandable List section */

    protected void SetNovelOnly(boolean isChecked) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putBoolean(Constants.PREF_IS_NOVEL_ONLY, isChecked);
        editor.commit();
    }

    protected void LoadItem(int position) {
        PageModel page = adapter.getItem(position);

        Intent intent = null;
        if (page.getType().equalsIgnoreCase(PageModel.TYPE_NOVEL)) {
            intent = new Intent(getActivity(), NovelListContainerActivity.class);
            intent.putExtra(Constants.EXTRA_ONLY_WATCHED, false);
        } else if (page.getType().equalsIgnoreCase(PageModel.TYPE_CONTENT)) {
            intent = new Intent(getActivity(), DisplayLightNovelContentActivity.class);
        }
        if (intent != null) {
            intent.putExtra(Constants.EXTRA_PAGE, page.getPage());
            startActivity(intent);
        } else {
            Toast.makeText(getActivity(), "Unknown type for: " + page.getPage(), Toast.LENGTH_LONG).show();
        }
    }

    private String searchString = "";
    private SearchHelper callback;

    protected void doSearch(Editable s) {
        searchString = s.toString();
        boolean isNovelOnly = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(Constants.PREF_IS_NOVEL_ONLY, false);
        synchronized (s) {
            mStartTime = System.currentTimeMillis();
            if (callback != null) {
                mHandler.removeCallbacks(callback);
            }
            callback = new SearchHelper(mStartTime, isNovelOnly, getActivity());
            mHandler.postDelayed(callback, 1000);
        }
    }

    private class SearchHelper implements Runnable {
        private final long time;
        private final boolean isNovelOnly;
        private final Context context;

        public SearchHelper(long time, boolean isNovelOnly, Context context) {
            this.time = time;
            this.isNovelOnly = isNovelOnly;
            this.context = context;
        }

        @SuppressLint("NewApi")
        @Override
        public void run() {
            Log.d(TAG, "Time: " + time + " Start Time: " + mStartTime);
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
            ArrayList<String> languageList = new ArrayList<String>();
            for (int i = 0; i < Constants.languageList.length; i++)
                if (sharedPrefs.getBoolean("Search:" + Constants.languageList[i], true))
                    languageList.add(Constants.languageList[i]);
            if (time == mStartTime) {
                adapter.clear();
                ArrayList<PageModel> result;
                try {
                    result = NovelsDao.getInstance().doSearch(searchString, isNovelOnly, languageList);
                    if (result != null)
                        adapter.addAll(result);
                } catch (Exception ex) {
                    Log.e(TAG, ex.toString(), ex);
                    Toast.makeText(context, ex.toString(), Toast.LENGTH_SHORT).show();
                }

                progress.setVisibility(View.GONE);
            }
        }
    }
}
