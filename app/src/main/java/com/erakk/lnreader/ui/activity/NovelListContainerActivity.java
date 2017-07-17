package com.erakk.lnreader.ui.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.ui.fragment.DisplayLightNovelDetailsFragment;
import com.erakk.lnreader.ui.fragment.DisplayLightNovelListFragment;
import com.erakk.lnreader.ui.fragment.DisplayNovelTabFragment;
import com.erakk.lnreader.ui.fragment.IFragmentListener;

public class NovelListContainerActivity extends BaseActivity implements IFragmentListener {

    private static final String TAG = NovelListContainerActivity.class.toString();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragactivity_framework);

        // get the intent args
        boolean onlyWatched = getIntent().getBooleanExtra(Constants.EXTRA_ONLY_WATCHED, false);
        String mode = getIntent().getStringExtra(Constants.EXTRA_NOVEL_LIST_MODE);
        String lang = getIntent().getStringExtra(Constants.EXTRA_NOVEL_LANG);
        String loadedNovel = getIntent().getStringExtra(Constants.EXTRA_PAGE);
        Log.i(TAG, "IsWatched: " + onlyWatched + " Mode: " + mode + " lang: " + lang);

        init(onlyWatched, mode, lang, loadedNovel);
    }

    private void init(boolean onlyWatched, String mode, String lang, String loadedNovel) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (mContent == null) {
            // Fragment setup
            Bundle b = new Bundle();
            b.putString(Constants.EXTRA_NOVEL_LIST_MODE, mode);
            b.putBoolean(Constants.EXTRA_ONLY_WATCHED, onlyWatched);
            b.putString(Constants.EXTRA_NOVEL_LANG, lang);
            b.putString(Constants.EXTRA_PAGE, loadedNovel);

            if (onlyWatched) {
                mContent = new DisplayLightNovelListFragment();
            } else {
                mContent = new DisplayNovelTabFragment();
            }
            mContent.setArguments(b);

        }
        transaction.replace(R.id.mainFrame, mContent).commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        //if(adapter != null) adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.invert_colors:
                UIHelper.ToggleColorPref(this);
                UIHelper.Recreate(this);
                return true;
            case android.R.id.home:
                super.onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void changeNextFragment(Bundle bundle) {
        mContent = new DisplayLightNovelDetailsFragment();
        bundle.putBoolean("show_list_child", true);
        mContent.setArguments(bundle);

        if (findViewById(R.id.rightFragment) != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.rightFragment, mContent, DisplayLightNovelDetailsFragment.class.toString())
                    .commit();
        } else {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.mainFrame, mContent, DisplayLightNovelDetailsFragment.class.toString())
                    .addToBackStack(null)
                    .commit();
        }

        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
    }

}
