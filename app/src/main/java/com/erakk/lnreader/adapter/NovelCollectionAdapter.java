package com.erakk.lnreader.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.Util;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.PageModel;

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
    private HashMap<String, NovelCollectionModel> novels;

    public NovelCollectionAdapter(Context context, int resourceId, List<PageModel> objects) {
        super(context, resourceId, objects);
        this.layoutResourceId = resourceId;
        this.context = context;
        this.data = objects;
        novels = new HashMap<>();
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
            holder.txtStatusVol = (TextView) row.findViewById(R.id.novel_status_volumes);
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

        // Set volume and status
        String txtVolume = novel.getVolumes() + " Volume" + (novel.getVolumes() > 1 ? "s" : "");
        String txtCategories = "";
        for (String category : novel.getCategories()) {
            if (category.contains("Project")) {
                txtCategories =  " | " + category.substring(0, category.indexOf("Project")).replace("Category:", "");
                break;
            }
        }
        holder.txtStatusVol.setText(txtVolume + txtCategories);

        // cover related
        holder.ivNovelCover.setImageResource(R.drawable.dummy_1);
        holder.ivNovelCover.setVisibility(View.GONE);
        holder.imgprogressBar.setVisibility(View.VISIBLE);
        holder.position = position;

        if(UIHelper.isLoadCover(getContext())) {
            if (novels.get(novel.getTitle()) == null) {
                new NovelLoader(position, holder).execute(novel);
            } else {
                populate(novels.get(novel.getTitle()), holder);
            }
        }
        else {
            holder.imgprogressBar.setVisibility(View.INVISIBLE);
        }

        return row;
    }

    public List<PageModel> allData;

    public void filterData(String query) {

        // keep the original data
        if (allData == null || allData.size() < data.size()) {
            allData = new ArrayList<PageModel>();
            allData.addAll(data);
        }
        if (Util.isStringNullOrEmpty(query)) {
            // restore data
            data.clear();
            if (allData.size() > data.size()) {
                data.addAll(allData);
            }
        } else {
            query = query.toLowerCase();
            this.clear();
            data.clear();
            for (PageModel item : allData) {
                if (item.getTitle().toLowerCase().contains(query)) data.add(item);
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
        TextView txtStatusVol;
        TextView txtLastCheck;
        CheckBox chkIsWatched;
        ImageView ivNovelCover;
        int position;
        ProgressBar imgprogressBar;
    }

    public void setResourceId(int id) {
        this.layoutResourceId = id;
    }

    private void populate(NovelCollectionModel novelCollectionModel, NovelCollectionHolder holder) {
//        PageModel novelpage = null;
//        try {
//            novelpage = novelCollectionModel.getPageModel();
//        } catch (Exception e) {
//            Log.e(TAG, e.getMessage(), e);
//        }
        if (holder.ivNovelCover != null) {
            holder.ivNovelCover.setVisibility(View.VISIBLE);
            holder.imgprogressBar.setVisibility(View.GONE);
            if (novelCollectionModel != null && novelCollectionModel.getCoverBitmap() != null) {
                holder.ivNovelCover.setImageBitmap(novelCollectionModel.getCoverBitmap());
            } else {
                holder.ivNovelCover.setImageResource(R.drawable.dummy_2);
            }
        }
//  moved to query
//        if (holder.txtStatusVol != null) {
//            if (novelpage == null) {
//                holder.txtStatusVol.setText("N/A");
//            } else {
//                String category = getCategory(novelpage);
//                int volumes = novelCollectionModel.getBookCollections().size();
//                if (category.isEmpty()) {
//                    holder.txtStatusVol.setText(volumes + " Volume" + (volumes > 1 ? "s" : ""));
//                } else {
//                    holder.txtStatusVol.setText(category + " | " + volumes + " Volume" + (volumes > 1 ? "s" : ""));
//                }
//            }
//        }
    }

//    private String getCategory(PageModel novelpage) {
//        ArrayList<String> categories = novelpage.getCategories();
//        for (String category : categories) {
//            if (category.contains("Project")) {
//                return category.substring(0, category.indexOf("Project")).replace("Category:", "");
//            }
//        }
//        return "";
//    }

    private class NovelLoader extends AsyncTask<PageModel, Void, NovelCollectionModel> {
        int position;
        private NovelCollectionHolder holder;
        PageModel p;

        NovelLoader(int position, NovelCollectionHolder holder) {
            this.position = position;
            this.holder = holder;
        }

        @Override
        protected NovelCollectionModel doInBackground(PageModel... pageModels) {
            try {
                p = pageModels[0];
                return NovelsDao.getInstance().getNovelDetails(p, null, false);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(NovelCollectionModel novelCollectionModel) {
            super.onPostExecute(novelCollectionModel);
            if (holder.position == position) {
                novels.put(p.getTitle(), novelCollectionModel);
                populate(novelCollectionModel, holder);
            }
        }
    }
}
