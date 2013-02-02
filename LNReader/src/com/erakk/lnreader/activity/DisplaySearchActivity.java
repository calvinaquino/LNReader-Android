package com.erakk.lnreader.activity;

import java.util.ArrayList;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.erakk.lnreader.Constants;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.adapter.SearchPageModelAdapter;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.model.PageModel;

public class DisplaySearchActivity extends SherlockActivity{
	protected static final String TAG = DisplaySearchActivity.class.toString();
	private boolean isInverted;
	private Handler mHandler = new Handler();
	private long mStartTime;
	private ProgressBar progress = null;
	SearchPageModelAdapter adapter = null;
	private CheckBox chkNovelOnly = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		UIHelper.SetTheme(this, R.layout.activity_search);
		UIHelper.SetActionBarDisplayHomeAsUp(this, true);
		
		setTitle("Search");
		isInverted = getColorPreferences();
		
		final EditText search = (EditText) findViewById(R.id.searchText);
		search.addTextChangedListener( new TextWatcher() {			
			public void onTextChanged(CharSequence s, int start, int before, int count) { }
			
			public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
			
			public void afterTextChanged(Editable s) {
				progress.setVisibility(View.VISIBLE);
				doSearch(s);
			}
		});
		
		ListView searchResult = (ListView) findViewById(R.id.searchResult);
		int resourceId = R.layout.novel_list_item;
		if(UIHelper.IsSmallScreen(this)) {
			resourceId = R.layout.novel_list_item_small; 
		}
		adapter = new SearchPageModelAdapter(this, resourceId, new ArrayList<PageModel>());
		searchResult.setAdapter(adapter);
		searchResult.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				LoadItem(arg2);
			}
		});		
		
		progress = (ProgressBar) findViewById(R.id.progressBar1);
		
		chkNovelOnly =  (CheckBox) findViewById(R.id.chkNovelOnly);
		chkNovelOnly.setChecked(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_IS_NOVEL_ONLY, false));
		chkNovelOnly.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				SetNovelOnly(isChecked);
				doSearch(search.getEditableText());
				progress.setVisibility(View.VISIBLE);
			}
		});
		
	}
	
	protected void SetNovelOnly(boolean isChecked) {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    	SharedPreferences.Editor editor = sharedPrefs.edit();
    	editor.putBoolean(Constants.PREF_IS_NOVEL_ONLY, isChecked);
    	editor.commit();
	}

	protected void LoadItem(int position) {
		PageModel page = adapter.getItem(position);
		
		Intent intent = null;
		if(page.getType().equalsIgnoreCase(PageModel.TYPE_NOVEL)) {
			intent = new Intent(this, DisplayLightNovelDetailsActivity.class);
		}
		else if(page.getType().equalsIgnoreCase(PageModel.TYPE_CONTENT)) {
			intent = new Intent(this, DisplayLightNovelContentActivity.class);
		}
		if(intent != null) {
			intent.putExtra(Constants.EXTRA_PAGE, page.getPage());
			startActivity(intent);
		}
		else {
			Toast.makeText(this, "Unknown type for: " + page.getPage(), Toast.LENGTH_LONG).show();
		}
	}

	private String searchString = "";
	private SearchHelper callback;
	protected void doSearch(Editable s) {
		searchString = s.toString();
		boolean isNovelOnly = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_IS_NOVEL_ONLY, false);
		synchronized (s) {
			mStartTime = System.currentTimeMillis();
			if(callback != null) {
				mHandler.removeCallbacks(callback);
			}
			callback = new SearchHelper(mStartTime, isNovelOnly);
	        mHandler.postDelayed(callback, 1000);
		}
	}
	
	private class SearchHelper implements Runnable {
		private final long time;
		private final boolean isNovelOnly;
		
		public SearchHelper(long time, boolean isNovelOnly) {
			this.time = time;
			this.isNovelOnly = isNovelOnly;
		}
		
		public void run() {
			Log.d(TAG, "Time: " + time + " Start Time: " + mStartTime);
			if(time == mStartTime) {
				adapter.clear();
				ArrayList<PageModel> result = NovelsDao.getInstance().doSearch(searchString, isNovelOnly);
				if (result != null)
					adapter.addAll(result);
				progress.setVisibility(View.GONE);
			}
		}		
	}
	
	@Override
	protected void onStop() {
		super.onStop();
	}
	
	@Override
    protected void onRestart() {
        super.onRestart();
        if(isInverted != getColorPreferences()) {
        	UIHelper.Recreate(this);
        }
    }
	
	private boolean getColorPreferences(){
    	return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_INVERT_COLOR, true);
	}
}
