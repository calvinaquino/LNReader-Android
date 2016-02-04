package com.erakk.lnreader.UI.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;

public class MainFragment extends BaseFragment implements View.OnClickListener {
    private static final String TAG = MainFragment.class.toString();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        // assign button to method
        Button btnLightNovel = (Button) view.findViewById(R.id.btnLightNovel);
        btnLightNovel.setOnClickListener(this);
        Button btnWatchList = (Button) view.findViewById(R.id.btnWatchList);
        btnWatchList.setOnClickListener(this);
        Button btnResumeNovel = (Button) view.findViewById(R.id.btnResumeNovel);
        btnResumeNovel.setOnClickListener(this);
        Button btnAltLanguage = (Button) view.findViewById(R.id.btnAltLanguage);
        btnAltLanguage.setOnClickListener(this);
        TextView txtReportIssue = (TextView) view.findViewById(R.id.report_issue);
        txtReportIssue.setOnClickListener(this);

        getActivity().setTitle(getActivity().getApplicationInfo().labelRes);

        return view;
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnLightNovel:
                UIHelper.openNovelList(getActivity());
                break;
            case R.id.btnWatchList:
                UIHelper.openWatchList(getActivity());
                break;
            case R.id.btnResumeNovel:
                UIHelper.openLastRead(getActivity());
                break;
            case R.id.btnAltLanguage:
                UIHelper.selectAlternativeLanguage(getActivity());
                break;
            case R.id.report_issue:
                String url = "https://github.com/calvinaquino/LNReader-Android/issues";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
                break;
            default:
                Log.w(TAG, "Missing id: " + v.getId());
                break;
        }
    }
}
