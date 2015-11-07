package com.erakk.lnreader.UI.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UI.activity.DisplayLightNovelContentActivity;
import com.erakk.lnreader.UI.activity.NovelListContainerActivity;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.adapter.UpdateInfoModelAdapter;
import com.erakk.lnreader.callback.ICallbackEventData;
import com.erakk.lnreader.callback.IExtendedCallbackNotifier;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.model.UpdateInfoModel;
import com.erakk.lnreader.model.UpdateTypeEnum;
import com.erakk.lnreader.task.AsyncTaskResult;
import com.erakk.lnreader.task.LoadUpdatesTask;

import java.util.ArrayList;
import java.util.Arrays;


public class UpdateInfoFragment extends Fragment implements IExtendedCallbackNotifier<AsyncTaskResult<?>> {

    private static final String TAG = UpdateInfoFragment.class.toString();
    private ArrayList<UpdateInfoModel> updateList;
    private ListView updateListView;
    private UpdateInfoModelAdapter adapter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public UpdateInfoFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_update_history, null);
        updateListView = (ListView) view.findViewById(R.id.update_list);
        updateListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                UpdateInfoModel item = updateList.get(arg2);
                openChapter(item);
            }
        });
        updateContent();
        LNReaderApplication.getInstance().setUpdateServiceListener(this);
        registerForContextMenu(updateListView);
        getActivity().setTitle(getResources().getString(R.string.updates));
        LNReaderApplication.getInstance().setUpdateServiceListener(this);
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_update_history, menu);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_update_history, menu);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        UpdateInfoModel chapter = updateList.get(info.position);
        if (chapter.getUpdateType() == UpdateTypeEnum.NewNovel) {
            menu.findItem(R.id.menu_open_chapter).setVisible(false);
        } else {
            menu.findItem(R.id.menu_open_chapter).setVisible(true);
        }
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.menu_open_chapter:
                if (info.position > -1) {
                    UpdateInfoModel chapter = updateList.get(info.position);
                    openChapter(chapter);
                }
                return true;
            case R.id.menu_open_details:
                if (info.position > -1) {
                    UpdateInfoModel chapter = updateList.get(info.position);
                    openDetails(chapter);
                }
                return true;
            case R.id.menu_update_delete_selected:
                if (info.position > -1) {
                    UpdateInfoModel chapter = updateList.get(info.position);
                    NovelsDao.getInstance().deleteUpdateHistory(chapter);
                    updateContent();
                }
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_clear_all:
                NovelsDao.getInstance().deleteAllUpdateHistory();
                updateContent();
                return true;
            case R.id.menu_clear_selected:
                if (updateList == null) return false;

                for (UpdateInfoModel updateInfo : updateList) {
                    if (updateInfo.isSelected())
                        NovelsDao.getInstance().deleteUpdateHistory(updateInfo);
                }
                updateContent();
                return true;
            case R.id.menu_show_updated:
                if (adapter != null) {
                    item.setChecked(!item.isChecked());
                    adapter.filterUpdated(item.isChecked());
                }
                return true;
            case R.id.menu_show_new:
                if (adapter != null) {
                    item.setChecked(!item.isChecked());
                    adapter.filterNew(item.isChecked());
                }
                return true;
            case R.id.menu_show_deleted:
                if (adapter != null) {
                    item.setChecked(!item.isChecked());
                    adapter.filterDeleted(item.isChecked());
                }
                return true;
            case R.id.menu_show_external:
                if (adapter != null) {
                    item.setChecked(!item.isChecked());
                    adapter.filterExternal(item.isChecked());
                }
                return true;
            case R.id.menu_run_update:
                runUpdate();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // region IExtendedCallbackNotifier implementation

    @Override
    public void onProgressCallback(ICallbackEventData message) {
        if (!this.isVisible())
            return;

        final LinearLayout p = (LinearLayout) getActivity().findViewById(R.id.layout_update_status);
        if (p != null) {
            p.setVisibility(View.VISIBLE);

            TextView txtUpdate = (TextView) getActivity().findViewById(R.id.txtUpdate);
            txtUpdate.setText("Update Status: " + message.getMessage());

            ProgressBar progress = (ProgressBar) getActivity().findViewById(R.id.download_progress_bar);
            if (message.getPercentage() < 100) {
                progress.setIndeterminate(false);
                progress.setMax(100);
                progress.setProgress(message.getPercentage());
                progress.setProgress(0);
                progress.setProgress(message.getPercentage());
                progress.setMax(100);
            } else {
                p.setVisibility(View.GONE);
                updateContent();
            }
        }
    }

    @Override
    public void onCompleteCallback(ICallbackEventData message, AsyncTaskResult<?> result) {
        if (!this.isAdded() || this.isDetached() || this.isRemoving()) return;

        if (result.getResultType() == UpdateInfoModel[].class) {
            try {
                UpdateInfoModel[] temp = (UpdateInfoModel[]) result.getResult();
                updateList = new ArrayList<UpdateInfoModel>(Arrays.asList(temp));

                int resourceId = R.layout.item_update;
                adapter = new UpdateInfoModelAdapter(getActivity(), resourceId, updateList);
                updateListView.setAdapter(adapter);

            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
                Toast.makeText(LNReaderApplication.getInstance(), getResources().getString(R.string.error_update) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            // from run update
            onProgressCallback(message);
        }
    }

    @Override
    public boolean downloadListSetup(String taskId, String message, int setupType, boolean hasError) {
        // TODO Auto-generated method stub
        return false;
    }

    // endregion

    // region private method

    private void openChapter(UpdateInfoModel item) {
        // TODO: change to fragment
        Intent intent = null;
        if (item.getUpdateType() == UpdateTypeEnum.NewNovel) {
            intent = new Intent(getActivity(), NovelListContainerActivity.class);
            intent.putExtra(Constants.EXTRA_ONLY_WATCHED, false);
            intent.putExtra(Constants.EXTRA_PAGE, item.getUpdatePage());
        } else if (item.getUpdateType() == UpdateTypeEnum.New ||
                item.getUpdateType() == UpdateTypeEnum.Updated ||
                item.getUpdateType() == UpdateTypeEnum.UpdateTos) {

            if (item.isExternal() && !UIHelper.isUseInternalWebView(getActivity())) {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.getUpdatePage()));
            } else {
                intent = new Intent(getActivity(), DisplayLightNovelContentActivity.class);
                intent.putExtra(Constants.EXTRA_PAGE, item.getUpdatePage());
            }
        }

        if (intent != null)
            startActivity(intent);
    }

    private void openDetails(UpdateInfoModel item) {
        Intent intent = new Intent(getActivity(), NovelListContainerActivity.class);
        intent.putExtra(Constants.EXTRA_ONLY_WATCHED, false);
        if (item.getUpdateType() == UpdateTypeEnum.NewNovel) {
            intent.putExtra(Constants.EXTRA_PAGE, item.getUpdatePage());
        } else if (item.getUpdateType() == UpdateTypeEnum.New ||
                item.getUpdateType() == UpdateTypeEnum.Updated ||
                item.getUpdateType() == UpdateTypeEnum.Deleted) {
            try {
                String parent = item.getUpdatePageModel().getParent();
                String details = parent.split(Constants.NOVEL_BOOK_DIVIDER)[0];
                intent.putExtra(Constants.EXTRA_PAGE, details);
            } catch (Exception ex) {
                Log.e(TAG, "Failed to get parent page model", ex);
                intent = null;
            }
        }

        if (intent != null)
            startActivity(intent);
    }

    private void runUpdate() {
        LinearLayout panel = (LinearLayout) getActivity().findViewById(R.id.layout_update_status);
        if (panel != null) {
            panel.setVisibility(View.VISIBLE);
            LNReaderApplication.getInstance().runUpdateService(true, this);
            TextView txtUpdate = (TextView) getActivity().findViewById(R.id.txtUpdate);
            txtUpdate.setText("Update Status: " + getResources().getString(R.string.running));
            ProgressBar progress = (ProgressBar) getActivity().findViewById(R.id.download_progress_bar);
            progress.setIndeterminate(true);
        }
    }

    public void updateContent() {
        LoadUpdatesTask task = new LoadUpdatesTask(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        else
            task.execute();
    }
    // endregion
}
