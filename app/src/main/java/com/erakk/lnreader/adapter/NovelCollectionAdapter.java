package com.erakk.lnreader.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.callback.IExtendedCallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.BakaReaderException;
import com.erakk.lnreader.helper.Util;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.NovelContentModel;
import com.erakk.lnreader.model.PageModel;
import com.erakk.lnreader.parser.BakaTsukiParser;
import com.erakk.lnreader.parser.BakaTsukiParserAlternative;
import com.erakk.lnreader.task.AsyncTaskResult;
import com.erakk.lnreader.task.LoadNovelDetailsTask;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
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
    private static SparseArray<NovelCollectionModel> novels;

    public NovelCollectionAdapter(Context context, int resourceId, List<PageModel> objects) {
        super(context, resourceId, objects);
        this.layoutResourceId = resourceId;
        this.context = context;
        this.data = objects;
        novels = new SparseArray<NovelCollectionModel>();
        Log.d(TAG, "created with " + objects.size() + " items");
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View row = convertView;
        NovelCollectionHolder holder;

        final PageModel novel = data.get(position);

        if (null == row) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);
            holder = new NovelCollectionHolder();
            holder.txtNovel = (TextView) row.findViewById(R.id.novel_name);
            holder.txtLastCheck = (TextView) row.findViewById(R.id.novel_last_check);
            holder.txtLastUpdate = (TextView) row.findViewById(R.id.novel_last_update);
            holder.txtStausVol = (TextView) row.findViewById(R.id.novel_status_volumes);
            holder.chkIsWatched = (CheckBox) row.findViewById(R.id.novel_is_watched);
            holder.ivNovelCover = (ImageView) row.findViewById(R.id.novel_cover);
            holder.imgprogressBar = (ProgressBar) row.findViewById(R.id.imgprogressBar);
            row.setTag(holder);
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

        if (holder.txtLastCheck != null) {
            holder.txtLastCheck.setText(context.getResources().getString(R.string.last_check) + ": " + Util.formatDateForDisplay(context, novel.getLastUpdate()));
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

        holder.ivNovelCover.setImageResource(R.drawable.dummy_1);
        holder.txtStausVol.setText("N/A");

        holder.ivNovelCover.setVisibility(View.GONE);
        holder.imgprogressBar.setVisibility(View.VISIBLE);

        holder.position = position;
        if(novels.get(position)==null) {
            new NovelLoader(position, holder).execute(novel);
        }
        else
        {
            populate(novels.get(position),holder);
        }

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
        TextView txtLastCheck;
        CheckBox chkIsWatched;
        ImageView ivNovelCover;
        int position;
        ProgressBar imgprogressBar;
    }

    public void setResourceId(int id) {
        this.layoutResourceId = id;
    }

    private static void populate(NovelCollectionModel novelCollectionModel, NovelCollectionHolder holder)
    {
        PageModel novelpage = null;
        try {
            novelpage = novelCollectionModel.getPageModel();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(holder.ivNovelCover != null)
        {
            holder.ivNovelCover.setVisibility(View.VISIBLE);
            holder.imgprogressBar.setVisibility(View.GONE);
            if(novelCollectionModel!=null&&novelCollectionModel.getCoverBitmap()!=null) {
                holder.ivNovelCover.setImageBitmap(novelCollectionModel.getCoverBitmap());
            }
            else
            {
                holder.ivNovelCover.setImageResource(R.drawable.dummy_2);
            }
        }

        if (holder.txtStausVol !=null){
            if(novelpage==null)
            {
                holder.txtStausVol.setText("N/A");
            }
            else
            {
                holder.txtStausVol.setText(getCategory(novelpage));
            }
        }
    }

    private static String getCategory(PageModel novelpage)
    {
        ArrayList<String> categories = novelpage.getCategories();
        for(String category:categories)
        {
            if(category.contains("Project"))
            {
                return category.replace("Category:","").replace("Projects","Project");
            }
        }
        return "";
    }

    private static class NovelLoader extends AsyncTask<PageModel,Void,NovelCollectionModel>
    {
        int position;
        private NovelCollectionHolder holder;
        NovelLoader(int position, NovelCollectionHolder holder)
        {
            this.position = position;
            this.holder = holder;
        }

        @Override
        protected NovelCollectionModel doInBackground(PageModel... pageModels) {
            try {
                return NovelsDao.getInstance().getNovelDetails(pageModels[0],null,false);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(NovelCollectionModel novelCollectionModel) {
            super.onPostExecute(novelCollectionModel);
            if(holder.position == position) {
                novels.put(position,novelCollectionModel);
                populate(novelCollectionModel,holder);
            }
        }
    }


}
