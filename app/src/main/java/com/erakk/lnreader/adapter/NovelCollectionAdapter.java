package com.erakk.lnreader.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.BakaReaderException;
import com.erakk.lnreader.helper.Util;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.NovelContentModel;
import com.erakk.lnreader.model.PageModel;
import com.erakk.lnreader.parser.BakaTsukiParser;
import com.erakk.lnreader.parser.BakaTsukiParserAlternative;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sam10795 on 6/7/18.
 */

public class NovelCollectionAdapter extends ArrayAdapter<PageModel> {

    private static final String TAG = PageModelAdapter.class.toString();
    private final Context context;
    private int layoutResourceId;
    public List<PageModel> data;
    private PageModel[] originalData = new PageModel[0];

    public NovelCollectionAdapter(Context context, int resourceId, List<PageModel> objects) {
        super(context, resourceId, objects);
        this.layoutResourceId = resourceId;
        this.context = context;
        this.data = objects;
        Log.d(TAG, "created with " + objects.size() + " items");
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        NovelCollectionHolder holder = null;

        final PageModel novel = data.get(position);
        NovelCollectionModel novel_model = null;
        PageModel novelPage = null;
        try {
            novel_model = NovelsDao.getInstance().getNovelDetails(novel,null,true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(novel_model!=null)
        {
            try {
                novelPage = novel_model.getPageModel();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }


        if (null == row) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);
            holder = new NovelCollectionHolder();
            holder.txtNovel = (TextView) row.findViewById(R.id.novel_name);
            holder.txtLastUpdate = (TextView) row.findViewById(R.id.novel_last_update);
            holder.txtStausVol = (TextView) row.findViewById(R.id.novel_status_volumes);
            holder.chkIsWatched = (CheckBox) row.findViewById(R.id.novel_is_watched);
            holder.ivNovelCover = (ImageView) row.findViewById(R.id.novel_cover);
        } else {
            holder = (NovelCollectionHolder) row.getTag();
        }

        if (holder.txtNovel != null) {
            holder.txtNovel.setText(novel.getTitle());
            if (novel.isHighlighted()) {
                holder.txtNovel.setTypeface(null, Typeface.BOLD);
                holder.txtNovel.setTextSize(20);
                holder.txtNovel.setText("⇒" + holder.txtNovel.getText());
            }
        }

        if (holder.txtLastUpdate != null) {
            holder.txtLastUpdate.setText(context.getResources().getString(R.string.last_update) + ": " + Util.formatDateForDisplay(context, novel.getLastUpdate()));
        }

        if (holder.txtStausVol !=null){
            if(novel_model==null||novelPage==null)
            {
                holder.txtStausVol.setText("N/A");
            }
            else
            {
                holder.txtStausVol.setText(novelPage.getCategories().get(0));
            }
        }

        if (holder.chkIsWatched != null) {
            // Log.d(TAG, page.getId() + " " + page.getTitle() + " isWatched: " + page.isWatched());
            holder.chkIsWatched.setOnCheckedChangeListener(null);
            holder.chkIsWatched.setChecked(novel.isWatched());
            holder.chkIsWatched.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        Toast.makeText(context, "Added to watch list: " + novel.getTitle(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Removed from watch list: " + novel.getTitle(), Toast.LENGTH_SHORT).show();
                    }
                    // update the db!
                    novel.setWatched(isChecked);
                    NovelsDao.getInstance().updatePageModel(novel);
                }
            });
        }

        if(holder.ivNovelCover != null)
        {
            if(novel_model!=null && novel_model.getCoverBitmap()!=null) {
                holder.ivNovelCover.setImageBitmap(novel_model.getCoverBitmap());
            }
            else
            {
                holder.ivNovelCover.setImageResource(R.drawable.ic_launcher);
            }
        }

        row.setTag(holder);
        return row;
    }

    public List<PageModel> allData;
    public void filterData(String query) {

        // keep the original data
        if(allData == null || allData.size() < data.size()) {
            allData = new ArrayList<PageModel>();
            allData.addAll(data);
        }
        if(Util.isStringNullOrEmpty(query)) {
            // restore data
            data.clear();
            if(allData.size() > data.size()) {
                data.addAll(allData);
            }
        }
        else {
            query = query.toLowerCase();
            this.clear();
            data.clear();
            for(PageModel item : allData) {
                if(item.getTitle().toLowerCase().contains(query)) data.add(item);
            }
        }

        super.notifyDataSetChanged();
        Log.d(TAG, "Filtered result : " + data.size());
    }

    public void filterData() {
        this.clear();
        data.clear();
        for (PageModel item : originalData) {
            if (!item.isHighlighted()) {
                if (!UIHelper.getShowRedlink(getContext()) && item.isRedlink())
                    continue;
                if (!UIHelper.getShowMissing(getContext()) && item.isMissing())
                    continue;
                if (!UIHelper.getShowExternal(getContext()) && item.isExternal())
                    continue;
            }
            data.add(item);
        }
        super.notifyDataSetChanged();
        Log.d(TAG, "Filtered result : " + data.size());
    }

    static class NovelCollectionHolder {
        TextView txtNovel;
        TextView txtLastUpdate;
        TextView txtStausVol;
        CheckBox chkIsWatched;
        ImageView ivNovelCover;
    }

    public void setResourceId(int id) {
        this.layoutResourceId = id;
    }

}
