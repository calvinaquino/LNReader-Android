package com.erakk.lnreader.UI.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;

public class MainFragment extends BaseFragment implements View.OnClickListener {
    private static final String TAG = MainFragment.class.toString();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_main, container, false);

        // assign button to method
        Button btnLightNovel = (Button) view.findViewById(R.id.btnLightNovel);
        btnLightNovel.setOnClickListener(this);
        Button btnWatchList = (Button) view.findViewById(R.id.btnWatchList);
        btnWatchList.setOnClickListener(this);
        Button btnResumeNovel = (Button) view.findViewById(R.id.btnResumeNovel);
        btnResumeNovel.setOnClickListener(this);
        Button btnAltLanguage = (Button) view.findViewById(R.id.btnAltLanguage);
        btnAltLanguage.setOnClickListener(this);

        return view;
    }


    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnLightNovel:
                openNovelList(v);
                break;
            case R.id.btnWatchList:
                openWatchList(v);
                break;
            case R.id.btnResumeNovel:
                jumpLastRead(v);
                break;
            case R.id.btnAltLanguage:
                openAlternativeNovelList(v);
                break;
            default:
                Log.w(TAG, "Missing id: " + v.getId());
                break;
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.invert_colors:
                UIHelper.ToggleColorPref(getActivity());
                UIHelper.Recreate(getActivity());
                setIconColor();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    //region private methods

    private void setIconColor() {
        LinearLayout rightMenu = (LinearLayout) getView().findViewById(R.id.menu_right);
        if (rightMenu != null) {
            int childCount = rightMenu.getChildCount();
            for (int i = 0; i < childCount; ++i) {
                ImageButton btn = (ImageButton) rightMenu.getChildAt(i);
                btn.setImageDrawable(UIHelper.setColorFilter(btn.getDrawable()));
            }
        }
    }

    public void openNovelList(View view) {
        UIHelper.openNovelList(getActivity());
    }

    public void openWatchList(View view) {
        UIHelper.openWatchList(getActivity());
    }

    public void jumpLastRead(View view) {
        UIHelper.openLastRead(getActivity());
    }

    /* Open An activity to select alternative language */
    public void openAlternativeNovelList(View view) {
        UIHelper.selectAlternativeLanguage(getActivity());
    }

    //endregion
}
