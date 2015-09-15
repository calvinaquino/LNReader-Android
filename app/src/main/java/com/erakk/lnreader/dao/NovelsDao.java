/**
 *
 */
package com.erakk.lnreader.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.util.Log;

import com.erakk.lnreader.AlternativeLanguageInfo;
import com.erakk.lnreader.Constants;
import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.callback.CallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;
import com.erakk.lnreader.helper.BakaReaderException;
import com.erakk.lnreader.helper.DBHelper;
import com.erakk.lnreader.helper.Util;
import com.erakk.lnreader.helper.db.BookModelHelper;
import com.erakk.lnreader.helper.db.BookmarkModelHelper;
import com.erakk.lnreader.helper.db.FindMissingModelHelper;
import com.erakk.lnreader.helper.db.ImageModelHelper;
import com.erakk.lnreader.helper.db.NovelCollectionModelHelper;
import com.erakk.lnreader.helper.db.NovelContentModelHelper;
import com.erakk.lnreader.helper.db.NovelContentUserHelperModel;
import com.erakk.lnreader.helper.db.PageCategoriesHelper;
import com.erakk.lnreader.helper.db.PageModelHelper;
import com.erakk.lnreader.helper.db.UpdateInfoModelHelper;
import com.erakk.lnreader.model.BookModel;
import com.erakk.lnreader.model.BookmarkModel;
import com.erakk.lnreader.model.FindMissingModel;
import com.erakk.lnreader.model.ImageModel;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.NovelContentModel;
import com.erakk.lnreader.model.NovelContentUserModel;
import com.erakk.lnreader.model.PageModel;
import com.erakk.lnreader.model.UpdateInfoModel;
import com.erakk.lnreader.parser.BakaTsukiParser;
import com.erakk.lnreader.parser.BakaTsukiParserAlternative;
import com.erakk.lnreader.parser.CommonParser;
import com.erakk.lnreader.task.DownloadFileTask;

import org.jsoup.Connection.Response;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * @author Nandaka
 */
public class NovelsDao {
    private static final String TAG = NovelsDao.class.toString();
    private static final DBHelper dbh;
    private static final Context context;
    private static final NovelsDao instance;

    static {
        context = LNReaderApplication.getInstance().getApplicationContext();
        dbh = new DBHelper(context);
        instance = new NovelsDao();
    }

    public static NovelsDao getInstance() {
        return instance;
    }

    private NovelsDao() {
    }

    // region db

//    public DBHelper getDBHelper() {
//        return dbh;
//    }

    public void deleteDB() {
        synchronized (dbh) {
            SQLiteDatabase db = dbh.getWritableDatabase();
            try {
                dbh.deletePagesDB(db);
            } finally {
                db.close();
            }
        }
    }

    public String copyDB(boolean makeBackup, String filename) throws IOException {
        synchronized (dbh) {
            String filePath;
            filePath = dbh.copyDB(context, makeBackup, filename);
            return filePath;
        }
    }

    public String checkDB() {
        synchronized (dbh) {
            SQLiteDatabase db = dbh.getWritableDatabase();
            return dbh.checkDB(db);
        }
    }

    // public void temp() {
    // synchronized (dbh) {
    // SQLiteDatabase db = dbh.getWritableDatabase();
    // db.execSQL("DROP TABLE IF EXISTS " + dbh.TABLE_NOVEL_BOOKMARK);
    // db.execSQL(dbh.DATABASE_CREATE_NOVEL_BOOKMARK);
    // }
    // }

    // endregion

    // region Novel Listing related

    public ArrayList<PageModel> getNovels(ICallbackNotifier notifier, boolean alphOrder) throws Exception {
        return getNovelHelper(notifier, Constants.ROOT_NOVEL_ENGLISH, null, alphOrder);
    }

    public ArrayList<PageModel> getNovelsFromInternet(ICallbackNotifier notifier) throws Exception {
        checkInternetConnection();
        if (notifier != null) {
            String message = context.getResources().getString(R.string.load_novel_list_download);
            notifier.onProgressCallback(new CallbackEventData(message, TAG));
        }

        return getNovelsHelperFromInternet(notifier, Constants.ROOT_NOVEL_ENGLISH, null);
    }

    public ArrayList<PageModel> getWatchedNovel() {
        ArrayList<PageModel> watchedNovel = null;
        synchronized (dbh) {
            SQLiteDatabase db = null;
            try {
                long start = java.lang.System.currentTimeMillis();
                db = dbh.getReadableDatabase();
                watchedNovel = dbh.getAllWatchedNovel(db, true, isQuickLoad());
                Log.i(TAG, String.format("DB Loading Time - Watched Novel: %s", java.lang.System.currentTimeMillis() - start));
            } finally {
                if (db != null)
                    db.close();
            }
        }
        return watchedNovel;
    }

    public ArrayList<PageModel> getTeaser(ICallbackNotifier notifier, boolean alphOrder) throws Exception {
        return getNovelHelper(notifier, Constants.ROOT_TEASER, Constants.STATUS_TEASER, alphOrder);
    }

    public ArrayList<PageModel> getTeaserFromInternet(ICallbackNotifier notifier) throws Exception {
        checkInternetConnection();
        if (notifier != null) {
            String message = context.getResources().getString(R.string.load_novel_list_download);
            notifier.onProgressCallback(new CallbackEventData(message, TAG));
        }

        return getNovelsHelperFromInternet(notifier, Constants.ROOT_TEASER, Constants.STATUS_TEASER);
    }

    public ArrayList<PageModel> getOriginal(ICallbackNotifier notifier, boolean alphOrder) throws Exception {
        return getNovelHelper(notifier, Constants.ROOT_ORIGINAL, Constants.STATUS_ORIGINAL, alphOrder);
    }

    public ArrayList<PageModel> getOriginalFromInternet(ICallbackNotifier notifier) throws Exception {
        checkInternetConnection();
        if (notifier != null) {
            String message = context.getResources().getString(R.string.load_novel_list_download);
            notifier.onProgressCallback(new CallbackEventData(message, TAG));
        }

        return getNovelsHelperFromInternet(notifier, Constants.ROOT_ORIGINAL, Constants.STATUS_ORIGINAL);
    }

    public ArrayList<PageModel> getAlternative(ICallbackNotifier notifier, boolean alphOrder, String language) throws Exception {
        SQLiteDatabase db = null;
        PageModel page = null;
        ArrayList<PageModel> list = null;
        synchronized (dbh) {
            try {
                long start = java.lang.System.currentTimeMillis();
                db = dbh.getReadableDatabase();
                page = PageModelHelper.getAlternativePage(dbh, db, language);
                if (page != null) {
                    list = dbh.getAllAlternative(db, alphOrder, isQuickLoad(), language);
                    Log.d(TAG, "Found: " + list.size());
                }
                Log.i(TAG, String.format("DB Loading Time - Alt Novel %s: %s", language, java.lang.System.currentTimeMillis() - start));
            } finally {
                if (db != null)
                    db.close();
            }
        }

        if (page == null) {
            return getAlternativeFromInternet(notifier, language);
        }

        return list;
    }

    /**
     * Due to several changes in baka-tsuki pages as April 2014, crawling recursively through all subcategories
     *
     * @param notifier
     * @param language
     * @return
     * @throws Exception
     */
    public ArrayList<PageModel> getAlternativeFromInternet(final ICallbackNotifier notifier, String language) throws Exception {
        checkInternetConnection();
        if (notifier != null) {
            String message = context.getResources().getString(R.string.load_novel_list_alternate, language);
            notifier.onProgressCallback(new CallbackEventData(message, TAG));
        }

        // parse Information
        PageModel modelPage = new PageModel();

        if (language != null) {
            modelPage.setPage(AlternativeLanguageInfo.getAlternativeLanguageInfo().get(language).getCategoryInfo());
            modelPage.setTitle(context.getResources().getString(R.string.title_novels_page_alternate, language));
            modelPage.setLanguage(language);
        }

        modelPage = getPageModel(modelPage, notifier);
        modelPage.setType(PageModel.TYPE_NOVEL);

        // update page model
        synchronized (dbh) {
            SQLiteDatabase db = dbh.getWritableDatabase();
            modelPage = PageModelHelper.insertOrUpdatePageModel(dbh, db, modelPage, true);
            Log.d(TAG, "Updated " + language);
        }

        // get all sub-categories
        final ArrayList<Document> global_docs = new ArrayList<Document>(); // list of all documents
        ArrayList<String> links = null;
        String url = null;
        if (language != null)
            url = UIHelper.getBaseUrl(LNReaderApplication.getInstance().getApplicationContext()) + "/project/index.php?title=" + AlternativeLanguageInfo.getAlternativeLanguageInfo().get(language).getCategoryInfo();
        int retry = 0;
        while (retry < getRetry()) {
            try {
                Response response = connect(url, retry);
                Document doc = response.parse();
                global_docs.add(doc); // add main category page to global_docs
                links = BakaTsukiParserAlternative.CrawlAlternativeCategory(doc);
                Log.d(TAG, "Found from internet: " + links.size() + " Sub-Category");

                if (notifier != null) {
                    String message = context.getResources().getString(R.string.load_subcategory_list_finished, links.size());
                    notifier.onProgressCallback(new CallbackEventData(message, TAG));
                }
                break;
            } catch (EOFException eof) {
                ++retry;
                if (notifier != null) {
                    String message = context.getResources().getString(R.string.load_novel_retry, retry, getRetry(), eof.getMessage());
                    notifier.onProgressCallback(new CallbackEventData(message, TAG));
                }
                if (retry >= getRetry())
                    throw eof;
            } catch (IOException eof) {
                ++retry;
                String message = context.getResources().getString(R.string.load_novel_retry, retry, getRetry(), eof.getMessage());
                if (notifier != null) {
                    notifier.onProgressCallback(new CallbackEventData(message, TAG));
                }
                Log.d(TAG, message, eof);
                if (retry >= getRetry())
                    throw eof;
            }
        }

        // get all documents from pre-defined links
        final ArrayList<String> getLinks = new ArrayList<String>(links);
        final Object sync_lock = new Object(); // locking's object for global_docs
        final Object sync_lock_log = new Object(); // locking's object for error message

        // Since there're only about 1 to 4 sub-categories, we'll create all threads at once
        if (links != null) {

            Thread[] threads = null;
            if (getLinks.size() > 0)
                threads = new Thread[getLinks.size()];

            for (int i = 0; i < getLinks.size(); i++) {
                final int access_index = i;
                threads[access_index] = new Thread() {
                    @Override
                    public void run() {
                        int retry = 0;
                        while (retry < getRetry()) {
                            try {
                                String url = UIHelper.getBaseUrl(LNReaderApplication.getInstance().getApplicationContext()) + "/project/index.php?title=" + getLinks.get(access_index);
                                if (url != null) {
                                    Response response = connect(url, retry);
                                    Document doc = response.parse();
                                    synchronized (sync_lock) {
                                        global_docs.add(doc); // add main category page to global_docs
                                        Log.d(TAG, "Found from internet: " + url + " page.");
                                    }
                                }

                                break;
                            } catch (EOFException eof) {
                                ++retry;
                                synchronized (sync_lock_log) {
                                    if (retry >= getRetry())
                                        Log.d(TAG, "Timeout when accessing " + getLinks.get(access_index));
                                }
                            } catch (IOException eof) {
                                ++retry;

                                synchronized (sync_lock_log) {
                                    if (retry >= getRetry())
                                        Log.d(TAG, "Timeout when accessing " + getLinks.get(access_index));
                                }
                            }
                        }
                    }
                };
                threads[access_index].start(); // start threads
            }

            if (getLinks.size() != 0) {
                for (int i = 0; i < getLinks.size(); i++)
                    threads[i].join();
            }

        }

        Log.d(TAG, "Number of global_docs found: " + global_docs.size());

        // parse all documents (ensure no double entities)
        ArrayList<PageModel> list = new ArrayList<PageModel>();
        for (Document current_doc : global_docs) {
            ArrayList<PageModel> temp_list = BakaTsukiParserAlternative.ParseAlternativeList(current_doc, language);
            for (PageModel current_list : temp_list) {
                boolean isExist = false;
                for (PageModel pointed_list : list) {
                    if (pointed_list.getPage().equals(current_list.getPage())) {
                        isExist = true;
                        break;
                    }
                }
                if (!isExist)
                    list.add(current_list);
            }
        }

        // sorts alphabetically
        Collections.sort(list, new Comparator<PageModel>() {
            @Override
            public int compare(PageModel p1, PageModel p2) {
                return p1.getPage().compareTo(p2.getPage());
            }

        });

        // set orderings and last update
        list = getUpdateInfo(list, notifier);
        Date dt = new Date();
        for (int i = 0; i < list.size(); i++) {
            PageModel p = list.get(i);
            p.setOrder(i);
            p.setLastCheck(dt);
        }

        // get mediawiki page info
        getUpdateInfo(list, notifier);

        // save lists
        synchronized (dbh) {
            SQLiteDatabase db = dbh.getWritableDatabase();
            for (PageModel pageModel : list) {
                pageModel = PageModelHelper.insertOrUpdatePageModel(dbh, db, pageModel, true);
                Log.d(TAG, "Updated " + language + " novel: " + pageModel.getPage());
            }
        }

        return list;
    }

    /**
     * Get Novel list from db. If not exists, get it from internet
     *
     * @param notifier
     * @param parent
     * @param alphOrder
     * @param status
     * @return
     * @throws Exception
     */
    private ArrayList<PageModel> getNovelHelper(ICallbackNotifier notifier, final String parent, final String status, boolean alphOrder) throws Exception {
        SQLiteDatabase db = null;
        ArrayList<PageModel> list = null;

        synchronized (dbh) {
            try {
                long start = java.lang.System.currentTimeMillis();
                db = dbh.getReadableDatabase();
                list = dbh.getAllNovelsByCategory(db, alphOrder, isQuickLoad(), new String[]{parent});
                Log.d(TAG, "Found: " + list.size());

                Log.i(TAG, String.format("DB Loading Time - %s: %s", parent, java.lang.System.currentTimeMillis() - start));
            } finally {
                if (db != null)
                    db.close();
            }
        }

        if (parent == null) {
            return getNovelsHelperFromInternet(notifier, parent, status);
        }

        return list;
    }

    /**
     * Get novel list from Internet based on given parent page,
     * e.g.:
     * - "Category:Teaser"
     * - "Category:Original_novel"
     * - "Category:Light_novel_(English)"
     * Please define the parent page in Constants.
     *
     * @param notifier
     * @param parent
     * @param status
     * @return
     * @throws Exception
     * @throws EOFException
     * @throws IOException
     */
    private ArrayList<PageModel> getNovelsHelperFromInternet(ICallbackNotifier notifier, final String parent, final String status) throws Exception, EOFException, IOException {
        Date date = new Date();
        PageModel parentPage = new PageModel();
        parentPage.setPage(parent);
        parentPage.setLanguage(Constants.LANG_ENGLISH);
        parentPage.setTitle(parent);
        parentPage = getPageModel(parentPage, notifier);
        parentPage.setType(PageModel.TYPE_OTHER);
        parentPage.setLastCheck(date);

        // update page model
        synchronized (dbh) {
            SQLiteDatabase db = dbh.getWritableDatabase();
            parentPage = PageModelHelper.insertOrUpdatePageModel(dbh, db, parentPage, true);
            Log.d(TAG, "Updated " + parent);
        }

        // get list
        String url = UIHelper.getBaseUrl(LNReaderApplication.getInstance().getApplicationContext()) + "/project/index.php?title=" + parent;
        Log.d(TAG, "Url: " + url);

        ArrayList<PageModel> list = null;
        int retry = 0;
        while (retry < getRetry()) {
            try {
                Response response = connect(url, retry);
                Document doc = response.parse();

                list = BakaTsukiParser.parseGenericNovelList(doc, parent, status);
                for (PageModel pageModel : list) {
                    pageModel.setParent(parent);
                    pageModel.setParentPageModel(parentPage);
                }
                Log.d(TAG, "Found from internet: " + list.size() + " for " + parent);

                list = getUpdateInfo(list, notifier);
                for (PageModel pageModel : list) {
                    pageModel.setLastCheck(date);
                }

                if (notifier != null) {
                    String message = context.getResources().getString(R.string.load_novel_list_finished, list.size());
                    notifier.onProgressCallback(new CallbackEventData(message, TAG));
                }
                break;
            } catch (EOFException eof) {
                ++retry;
                if (notifier != null) {
                    String message = context.getResources().getString(R.string.load_novel_retry, retry, getRetry(), eof.getMessage());
                    notifier.onProgressCallback(new CallbackEventData(message, TAG));
                }
                if (retry >= getRetry())
                    throw eof;
            } catch (IOException eof) {
                ++retry;
                String message = context.getResources().getString(R.string.load_novel_retry, retry, getRetry(), eof.getMessage());
                if (notifier != null) {
                    notifier.onProgressCallback(new CallbackEventData(message, TAG));
                }
                Log.d(TAG, message, eof);
                if (retry >= getRetry())
                    throw eof;
            }
        }

        // save list
        synchronized (dbh) {
            SQLiteDatabase db = dbh.getWritableDatabase();
            for (PageModel pageModel : list) {
                pageModel = PageModelHelper.insertOrUpdatePageModel(dbh, db, pageModel, true);
                Log.d(TAG, "Updated: " + pageModel.getPage());
            }
        }
        synchronized (dbh) {
            SQLiteDatabase db = dbh.getReadableDatabase();
            // reload data from db
            list = dbh.getAllNovelsByCategory(db, isAlphabeticalOrder(), isQuickLoad(), new String[]{parent});
        }

        return list;
    }

    public int deleteNovel(PageModel novel) {
        synchronized (dbh) {
            SQLiteDatabase db = dbh.getWritableDatabase();
            return NovelCollectionModelHelper.deleteNovel(dbh, db, novel);
        }
    }

    // endregion

    // region PageModel related

    /**
     * Get page model from db. If autoDownload = true, get the pageModel from
     * internet if not exists.
     *
     * @param page
     * @param notifier
     * @param autoDownload
     * @return
     * @throws Exception
     */
    private PageModel getPageModel(PageModel page, ICallbackNotifier notifier, boolean autoDownload) throws Exception {
        PageModel pageModel = null;
        synchronized (dbh) {
            SQLiteDatabase db = dbh.getReadableDatabase();
            try {
                pageModel = PageModelHelper.getPageModel(dbh, db, page.getPage());
            } finally {
                db.close();
            }
        }
        if (pageModel == null && autoDownload) {
            pageModel = getPageModelFromInternet(page, notifier);
        }
        return pageModel;
    }

    /**
     * Get page model from db. Get the pageModel from internet if not exists.
     *
     * @param page
     * @param notifier
     * @return
     * @throws Exception
     */
    public PageModel getPageModel(PageModel page, ICallbackNotifier notifier) throws Exception {
        return getPageModel(page, notifier, true);
    }

    /**
     * Return pageModel, null if not exist.
     *
     * @param page
     * @param notifier
     * @return
     * @throws Exception
     */
    public PageModel getExistingPageModel(PageModel page, ICallbackNotifier notifier) throws Exception {
        return getPageModel(page, notifier, false);
    }

    public PageModel getPageModelFromInternet(PageModel page, ICallbackNotifier notifier) throws Exception {
        checkInternetConnection();
        Log.d(TAG, "PageModel = " + page.getPage());

        int retry = 0;
        while (retry < getRetry()) {
            try {
                if (notifier != null) {
                    String message = context.getResources().getString(R.string.load_novel_list_fetch, page.getTitle());
                    notifier.onProgressCallback(new CallbackEventData(message, TAG));
                }
                String encodedTitle = Util.UrlEncode(page.getPage().trim());
                String fullUrl = String.format(Constants.API_URL_INFO, UIHelper.getBaseUrl(LNReaderApplication.getInstance().getApplicationContext()), encodedTitle);
                Response response = connect(fullUrl, retry);
                PageModel pageModel = null;
                String lang = page.getLanguage();
                if (lang != null)
                    pageModel = CommonParser.parsePageAPI(page, response.parse(), fullUrl);
                pageModel.setFinishedRead(page.isFinishedRead());
                pageModel.setWatched(page.isWatched());

                synchronized (dbh) {
                    // save to db and get saved value
                    SQLiteDatabase db = dbh.getWritableDatabase();
                    try {
                        pageModel = PageModelHelper.insertOrUpdatePageModel(dbh, db, pageModel, false);
                    } finally {
                        db.close();
                    }
                }
                return pageModel;
            } catch (EOFException eof) {
                ++retry;
                if (notifier != null) {
                    String message = context.getResources().getString(R.string.load_novel_retry, retry, getRetry(), eof.getMessage());
                    notifier.onProgressCallback(new CallbackEventData(message, TAG));
                }
                if (retry >= getRetry())
                    throw eof;
            } catch (IOException eof) {
                ++retry;
                String message = context.getResources().getString(R.string.load_novel_retry, retry, getRetry(), eof.getMessage());
                if (notifier != null) {
                    notifier.onProgressCallback(new CallbackEventData(message, TAG));
                }
                Log.d(TAG, message, eof);
                if (retry >= getRetry())
                    throw eof;
            }
        }
        return null;
    }

    public PageModel updatePageModel(PageModel page) {
        PageModel pageModel = null;
        synchronized (dbh) {
            SQLiteDatabase db = dbh.getWritableDatabase();
            try {
                pageModel = PageModelHelper.insertOrUpdatePageModel(dbh, db, page, false);
            } finally {
                db.close();
            }
        }
        return pageModel;
    }

    public int deletePage(PageModel page) {
        synchronized (dbh) {
            // get from db
            SQLiteDatabase db = dbh.getReadableDatabase();
            try {
                return PageModelHelper.deletePageModel(dbh, db, page);
            } finally {
                db.close();
            }
        }
    }


    public PageModel getUpdateInfo(PageModel pageModel, ICallbackNotifier notifier) throws Exception {
        ArrayList<PageModel> pageModels = new ArrayList<PageModel>();
        pageModels.add(pageModel);
        pageModels = getUpdateInfo(pageModels, notifier);
        return pageModels.get(0);
    }

    /***
     * Bulk update page info through wiki API - LastUpdateInfo. - Redirected. -
     * Missing flag.
     *
     * @param pageModels
     * @param notifier
     * @return
     * @throws Exception
     */
    public ArrayList<PageModel> getUpdateInfo(ArrayList<PageModel> pageModels, ICallbackNotifier notifier) throws Exception {
        ArrayList<PageModel> resultPageModel = new ArrayList<PageModel>();
        ArrayList<PageModel> noInfoPageModel = new ArrayList<PageModel>();
        ArrayList<PageModel> externalPageModel = new ArrayList<PageModel>();

        int i = 0;
        int pageCounter = 0;
        int retry = 0;
        while (i < pageModels.size()) {
            int apiPageCount = 1;
            ArrayList<PageModel> checkedPageModel = new ArrayList<PageModel>();
            String titles = "";

            while (i < pageModels.size() && apiPageCount < 50) {
                if (pageModels.get(i).isExternal()) {
                    pageModels.get(i).setMissing(false);
                    externalPageModel.add(pageModels.get(i));
                    ++i;
                    continue;
                }
                if (pageModels.get(i).isMissing() || pageModels.get(i).getPage().endsWith("&action=edit&redlink=1")) {
                    pageModels.get(i).setMissing(true);
                    noInfoPageModel.add(pageModels.get(i));
                    ++i;
                    continue;
                }
                if (titles.length() + pageModels.get(i).getPage().length() < 2000) {
                    titles += "|" + Util.UrlEncode(pageModels.get(i).getPage().trim());
                    checkedPageModel.add(pageModels.get(i));
                    ++i;
                    ++apiPageCount;
                } else {
                    break;
                }
            }

            // request the page
            while (retry < getRetry()) {
                try {
                    String url = String.format(Constants.API_URL_INFO, UIHelper.getBaseUrl(LNReaderApplication.getInstance().getApplicationContext()), titles);
                    // Log.d(TAG, "Trying to get: " + baseUrl + titles);
                    Response response = connect(url, retry);
                    Document doc = response.parse();
                    ArrayList<PageModel> updatedPageModels = CommonParser.parsePageAPI(checkedPageModel, doc, url);

                    SQLiteDatabase db = dbh.getWritableDatabase();
                    for (PageModel updatedPageModel : updatedPageModels) {
                        if (updatedPageModel.getCategories() != null && updatedPageModel.getCategories().size() > 0) {
                            PageCategoriesHelper.insertCategoryByPage(dbh, db, updatedPageModel.getPage(), updatedPageModel.getCategories());
                        }
                    }

                    resultPageModel.addAll(updatedPageModels);

                    if (notifier != null) {
                        pageCounter += checkedPageModel.size();
                        String message = context.getResources().getString(R.string.load_novel_chapters_download_info, pageCounter, pageModels.size());
                        notifier.onProgressCallback(new CallbackEventData(message, TAG));
                    }
                    break;
                } catch (EOFException eof) {
                    ++retry;
                    if (notifier != null) {
                        String message = context.getResources().getString(R.string.load_novel_retry, retry, getRetry(), eof.getMessage());
                        notifier.onProgressCallback(new CallbackEventData(message, TAG));
                    }
                    if (retry >= getRetry())
                        throw eof;
                } catch (IOException eof) {
                    ++retry;
                    String message = context.getResources().getString(R.string.load_novel_retry, retry, getRetry(), eof.getMessage());
                    if (notifier != null) {
                        notifier.onProgressCallback(new CallbackEventData(message, TAG));
                    }
                    Log.d(TAG, message, eof);
                    if (retry >= getRetry())
                        throw eof;
                }
            }
        }

        resultPageModel.addAll(noInfoPageModel);
        if (notifier != null) {
            pageCounter += noInfoPageModel.size();
            String message = context.getResources().getString(R.string.load_novel_chapters_download_info, pageCounter, pageModels.size());
            notifier.onProgressCallback(new CallbackEventData(message, TAG));
        }

        if (UIHelper.getUpdateIncludeExternal(context)) {
            for (PageModel page : externalPageModel) {
                getExternalUpdateInfo(page);
                if (notifier != null) {
                    pageCounter++;
                    String message = context.getResources().getString(R.string.load_novel_chapters_download_external, pageCounter, pageModels.size());
                    notifier.onProgressCallback(new CallbackEventData(message, TAG));
                }
            }
            resultPageModel.addAll(externalPageModel);
        }

        return resultPageModel;
    }

    public void getExternalUpdateInfo(PageModel page) {
        int retry;
        Map<String, String> headers = null;
        // Date: Wed, 13 Nov 2013 13:08:35 GMT
        DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy kk:mm:ss z", Locale.US);
        retry = 0;
        while (retry < getRetry()) {
            try {
                headers = connect(page.getPage(), retry).headers();
                break;
            } catch (HttpStatusException hex) {
                if (hex.getStatusCode() == 404) {
                    Log.w(TAG, "Page not Found (404) for: " + page.getPage());
                    page.setMissing(true);
                    break;
                }
                Log.e(TAG, "Error when getting updated date for: " + page.getPage(), hex);
            } catch (Exception e) {
                Log.e(TAG, "Error when getting updated date for: " + page.getPage(), e);
                ++retry;
            }
        }
        if (headers != null) {
            String dateStr = null;
            if (headers.containsKey("Last-Modified")) {
                dateStr = headers.get("Last-Modified");
            } else if (headers.containsKey("Date")) {
                dateStr = headers.get("Date");
            }
            if (!Util.isStringNullOrEmpty(dateStr)) {
                try {
                    Log.d(TAG, "External Novel last update: " + dateStr);
                    page.setLastUpdate(df.parse(dateStr));
                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse date for: " + page.getPage(), e);
                }
            }
        }
        page.setLastCheck(new Date());
    }

    // endregion

    // region NovelCollectionModel

    public NovelCollectionModel getNovelDetails(PageModel page, ICallbackNotifier notifier, boolean autoDownload) throws Exception {
        NovelCollectionModel novel = null;
        synchronized (dbh) {
            SQLiteDatabase db = dbh.getReadableDatabase();
            try {
                novel = NovelCollectionModelHelper.getNovelDetails(dbh, db, page.getPage());
            } finally {
                db.close();
            }
        }
        if (novel == null && autoDownload) {
            novel = getNovelDetailsFromInternet(page, notifier);
        }
        return novel;
    }

    public NovelCollectionModel getNovelDetailsFromInternet(PageModel page, ICallbackNotifier notifier) throws Exception {
        checkInternetConnection();
        Log.d(TAG, "Getting Novel Details from internet: " + page.getPage());
        NovelCollectionModel novel = null;

        int retry = 0;
        while (retry < getRetry()) {
            try {
                if (notifier != null) {
                    String message = context.getResources().getString(R.string.load_novel_details_download, page.getPage());
                    notifier.onProgressCallback(new CallbackEventData(message, TAG));
                }
                String encodedTitle = Util.UrlEncode(page.getPage());
                String fullUrl = UIHelper.getBaseUrl(LNReaderApplication.getInstance().getApplicationContext()) + "/project/index.php?action=render&title=" + encodedTitle;
                Response response = connect(fullUrl, retry);
                Document doc = response.parse();
                /*
                 * Add your section of alternative language here, create own
				 * parser for each language for modularity reason
				 */
                if (!page.getLanguage().equals(Constants.LANG_ENGLISH))
                    novel = BakaTsukiParserAlternative.ParseNovelDetails(doc, page);
                else
                    novel = BakaTsukiParser.ParseNovelDetails(doc, page);
            } catch (HttpStatusException hex) {
                ++retry;
                if (notifier != null) {
                    String message = context.getResources().getString(R.string.load_novel_retry, retry, getRetry(), hex.getMessage());
                    notifier.onProgressCallback(new CallbackEventData(message, TAG));
                }
                if (hex.getStatusCode() == 404) {
                    Log.w(TAG, "Page not Found (404) for: " + page.getPage());
                    page.setMissing(true);
                    throw hex;
                }
                if (retry >= getRetry())
                    throw hex;
            } catch (EOFException eof) {
                ++retry;
                if (notifier != null) {
                    String message = context.getResources().getString(R.string.load_novel_retry, retry, getRetry(), eof.getMessage());
                    notifier.onProgressCallback(new CallbackEventData(message, TAG));
                }
                if (retry >= getRetry())
                    throw eof;
            } catch (IOException eof) {
                ++retry;
                String message = context.getResources().getString(R.string.load_novel_retry, retry, getRetry(), eof.getMessage());
                if (notifier != null) {
                    notifier.onProgressCallback(new CallbackEventData(message, TAG));
                }
                Log.d(TAG, message, eof);
                if (retry >= getRetry())
                    throw eof;
            }

            // Novel details' Page Model
            if (novel != null) {
                page = updateNovelDetailsPageModel(page, notifier, novel);

                synchronized (dbh) {
                    // insert to DB and get saved value
                    SQLiteDatabase db = dbh.getWritableDatabase();
                    try {
                        db.beginTransaction();
                        novel = NovelCollectionModelHelper.insertNovelDetails(dbh, db, novel);
                        db.setTransactionSuccessful();
                    } finally {
                        db.endTransaction();
                        db.close();
                    }
                }

                // update info for each chapters
                if (notifier != null) {
                    String message = context.getResources().getString(R.string.load_novel_chapters_fetch, page.getPage());
                    notifier.onProgressCallback(new CallbackEventData(message, TAG));
                }
                ArrayList<PageModel> chapters = getUpdateInfo(novel.getFlattedChapterList(), notifier);

                if (notifier != null) {
                    String message = context.getResources().getString(R.string.load_novel_chapters_saving);
                    notifier.onProgressCallback(new CallbackEventData(message, TAG));
                }
                for (PageModel pageModel : chapters) {
                    if (pageModel.getPage().endsWith("&action=edit&redlink=1")) {
                        pageModel.setMissing(true);
                    }
                    pageModel = updatePageModel(pageModel);
                }

                // download cover image
                if (novel.getCoverUrl() != null) {
                    if (notifier != null) {
                        String message = context.getResources().getString(R.string.load_novel_cover_image);
                        notifier.onProgressCallback(new CallbackEventData(message, TAG));
                    }
                    DownloadFileTask task = new DownloadFileTask(novel.getCoverUrl(), notifier);
                    ImageModel image = task.downloadImage();
                    // TODO: need to save to db?
                    Log.d(TAG, "Cover Image: " + image.toString());
                }

                Log.d(TAG, "Complete getting Novel Details from internet: " + page.getPage());
                break;
            }
        }
        return novel;
    }

    public PageModel updateNovelDetailsPageModel(PageModel page, ICallbackNotifier notifier, NovelCollectionModel novel) throws Exception {
        if (notifier != null) {
            String message = context.getResources().getString(R.string.load_novel_details_update, page.getPage());
            notifier.onProgressCallback(new CallbackEventData(message, TAG));
        }
        PageModel novelPageTemp = getPageModelFromInternet(page, notifier);
        if (novelPageTemp != null) {
            page = getUpdateInfo(page, notifier);
            page.setLastUpdate(novelPageTemp.getLastUpdate());
            page.setLastCheck(new Date());
            novel.setLastUpdate(novelPageTemp.getLastUpdate());
            novel.setLastCheck(new Date());
        } else {
            page.setLastUpdate(new Date(0));
            page.setLastCheck(new Date());
            novel.setLastUpdate(new Date(0));
            novel.setLastCheck(new Date());
        }
        // save the changes
        synchronized (dbh) {
            SQLiteDatabase db = dbh.getWritableDatabase();
            try {
                page = PageModelHelper.insertOrUpdatePageModel(dbh, db, page, true);
            } finally {
                db.close();
            }
        }
        return page;
    }

    public int deleteBooks(BookModel bookDel) {
        synchronized (dbh) {
            // get from db
            SQLiteDatabase db = dbh.getReadableDatabase();
            try {
                BookModel tempBook = null;
                if (bookDel.getId() > 0) {
                    tempBook = BookModelHelper.getBookModel(dbh, db, bookDel.getId());
                } else if (!Util.isStringNullOrEmpty(bookDel.getPage()) && !Util.isStringNullOrEmpty(bookDel.getTitle())) {
                    tempBook = BookModelHelper.getBookModel(dbh, db, bookDel.getPage(), bookDel.getTitle());
                }
                if (tempBook != null) {
                    return BookModelHelper.deleteBookModel(dbh, db, tempBook);
                }
            } finally {
                db.close();
            }
        }
        return 0;
    }

    public ArrayList<PageModel> getChapterCollection(String page, String title, BookModel book) {
        synchronized (dbh) {
            // get from db
            SQLiteDatabase db = dbh.getReadableDatabase();
            try {
                return NovelCollectionModelHelper.getChapterCollection(dbh, db, page + Constants.NOVEL_BOOK_DIVIDER + title, book);
            } finally {
                db.close();
            }
        }
    }

    public void deleteBookCache(BookModel bookDel) {
        for (PageModel p : bookDel.getChapterCollection()) {
            deleteChapterCache(p);
        }
    }
    // endregion

    // region Novel Contents

    public ArrayList<PageModel> getAllContentPageModel() {
        ArrayList<PageModel> result = null;
        synchronized (dbh) {
            // get from db
            SQLiteDatabase db = dbh.getReadableDatabase();
            try {
                result = PageModelHelper.getAllContentPageModel(dbh, db);
            } finally {
                db.close();
            }
        }
        return result;
    }

    public NovelContentModel getNovelContent(PageModel page, boolean download, ICallbackNotifier notifier) throws Exception {
        NovelContentModel content = null;

        synchronized (dbh) {
            // get from db
            SQLiteDatabase db = dbh.getReadableDatabase();
            try {
                content = NovelContentModelHelper.getNovelContent(dbh, db, page.getPage());
            } finally {
                db.close();
            }
        }
        // get from Internet;
        if (download && content == null) {
            Log.d("getNovelContent", "Get from Internet: " + page.getPage());
            content = getNovelContentFromInternet(page, notifier);
        }

        return content;
    }

    public NovelContentModel getNovelContentFromInternet(PageModel page, ICallbackNotifier notifier) throws Exception {
        checkInternetConnection();

        String oldTitle = page.getTitle();

        NovelContentModel content = new NovelContentModel();
        int retry = 0;
        Document doc = null;
        while (retry < getRetry()) {
            try {
                String encodedUrl = String.format(Constants.API_URL_CONTENT, UIHelper.getBaseUrl(LNReaderApplication.getInstance().getApplicationContext()), Util.UrlEncode(page.getPage()));
                if (!page.getPage().endsWith(Constants.API_REDLINK)) {
                    Response response = connect(encodedUrl, retry);
                    doc = response.parse();
                    page.setMissing(false);
                } else {
                    Log.w(TAG, "redlink page: " + page.getPage());
                    String titleClean = page.getPage().replace(Constants.API_REDLINK, "");
                    doc = Jsoup.parse("<div class=\"noarticletext\">" +
                            "<p>There is currently no text in this page." +
                            "You can <a href=\"/project/index.php?title=Special:Search/" + titleClean + "\" title=\"Special:Search/" + titleClean + "\">search for this page title</a> in other pages," +
                            "or <span class=\"plainlinks\"><a rel=\"nofollow\" class=\"external text\" href=\"https://www.baka-tsuki.org/project/index.php?title=Special:Log&amp;page=" + titleClean + "\">search the related logs</a></span>." +
                            "</p>" +
                            "</div>");
                    page.setMissing(true);
                }
                content = BakaTsukiParser.ParseNovelContent(doc, page);
                content.setUpdatingFromInternet(true);
                break;
            } catch (EOFException eof) {
                ++retry;
                if (notifier != null) {
                    String message = context.getResources().getString(R.string.load_novel_retry, retry, getRetry(), eof.getMessage());
                    notifier.onProgressCallback(new CallbackEventData(message, TAG));
                }
                if (retry >= getRetry())
                    throw eof;
            } catch (IOException eof) {
                ++retry;
                String message = context.getResources().getString(R.string.load_novel_retry, retry, getRetry(), eof.getMessage());
                if (notifier != null) {
                    notifier.onProgressCallback(new CallbackEventData(message, TAG));
                }
                Log.d(TAG, message, eof);
                if (retry >= getRetry())
                    throw eof;
            }
        }
        if (doc != null) {
            // download all attached images
            for (ImageModel image : content.getImages()) {
                if (notifier != null) {
                    String message = context.getResources().getString(R.string.load_novel_image_download, image.getUrl());
                    notifier.onProgressCallback(new CallbackEventData(message, TAG));
                }
                DownloadFileTask task = new DownloadFileTask(image.getUrl(), notifier);
                image = task.downloadImage();
                // TODO: need to save image to db? mostly thumbnail only
            }

            // download linked big images
            boolean isDownloadBigImage = PreferenceManager.getDefaultSharedPreferences(LNReaderApplication.getInstance()).getBoolean(Constants.PREF_DOWLOAD_BIG_IMAGE, false);
            if (isDownloadBigImage) {
                Document imageDoc = Jsoup.parse(content.getContent());
                ArrayList<String> images = CommonParser.parseImagesFromContentPage(imageDoc);
                for (String imageUrl : images) {
                    ImageModel bigImage = new ImageModel();
                    bigImage.setBigImage(true);
                    bigImage.setName(imageUrl);
                    bigImage.setReferer(imageUrl);
                    bigImage = getImageModel(bigImage, notifier);
                }
            }

            // get last updated info
            PageModel contentPageModelTemp = getPageModelFromInternet(content.getPageModel(), notifier);
            if (contentPageModelTemp != null) {
                // overwrite the old title
                content.getPageModel().setTitle(oldTitle);
                // syncronize the date
                content.getPageModel().setLastUpdate(contentPageModelTemp.getLastUpdate());
                content.getPageModel().setLastCheck(new Date());
                content.setLastUpdate(contentPageModelTemp.getLastUpdate());
                content.setLastCheck(new Date());
            }

            // page model will be also saved in insertNovelContent()
            synchronized (dbh) {
                // save to DB, and get the saved value
                SQLiteDatabase db = dbh.getWritableDatabase();
                try {
                    // TODO: somehow using transaction cause problem...
                    db.beginTransaction();
                    content = NovelContentModelHelper.insertNovelContent(dbh, db, content, page, false);
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                    db.close();
                }
            }
        }
        return content;
    }

    public NovelContentUserModel getNovelContentUserModel(String page, ICallbackNotifier notifier) throws Exception {
        NovelContentUserModel content = null;

        synchronized (dbh) {
            // get from db
            SQLiteDatabase db = dbh.getReadableDatabase();
            try {
                content = NovelContentUserHelperModel.getNovelContentUserModel(dbh, db, page);
            } finally {
                db.close();
            }
        }

        return content;
    }

    public NovelContentUserModel updateNovelContentUserModel(NovelContentUserModel page, ICallbackNotifier notifier) throws Exception {
        if (notifier != null) {
            String message = "Updating user data: " + page.getPage();
            notifier.onProgressCallback(new CallbackEventData(message, TAG));
        }
        // save the changes
        synchronized (dbh) {
            SQLiteDatabase db = dbh.getWritableDatabase();
            try {
                page = NovelContentUserHelperModel.insertModel(dbh, db, page);
            } finally {
                db.close();
            }
        }
        return page;
    }

    public NovelContentModel updateNovelContent(NovelContentModel content, boolean forceUpdateContent) throws Exception {
        synchronized (dbh) {
            SQLiteDatabase db = dbh.getWritableDatabase();
            try {
                content = NovelContentModelHelper.insertNovelContent(dbh, db, content, content.getPageModel(), forceUpdateContent);
            } finally {
                db.close();
            }
        }
        return content;
    }

    public int deleteNovelContent(PageModel ref) {
        int result = 0;
        synchronized (dbh) {
            SQLiteDatabase db = dbh.getWritableDatabase();
            try {
                result = NovelContentModelHelper.deleteNovelContent(dbh, db, ref);
            } finally {
                db.close();
            }
        }
        return result;
    }

    public void deleteChapterCache(PageModel chapter) {
        deleteNovelContent(chapter);

        // Set isDownloaded to false
        chapter.setDownloaded(false);
        updatePageModel(chapter);
    }
    // endregion

    // region ImageModel

    /**
     * Get image from db, if not exist will try to download from internet
     *
     * @param image
     * @param notifier
     * @return
     * @throws Exception
     */
    public ImageModel getImageModel(ImageModel image, ICallbackNotifier notifier) throws Exception {
        if (image == null || image.getName() == null)
            throw new BakaReaderException("Empty Image!", BakaReaderException.EMPTY_IMAGE);
        ImageModel imageTemp = null;
        synchronized (dbh) {
            SQLiteDatabase db = dbh.getReadableDatabase();
            try {
                imageTemp = ImageModelHelper.getImage(dbh, db, image);

                if (imageTemp == null) {
                    if (image.getReferer() == null)
                        image.setReferer(image.getName());
                    Log.d(TAG, "Image not found, might need to check by referer: " + image.getName() + ", referer: " + image.getReferer());
                    imageTemp = ImageModelHelper.getImageByReferer(dbh, db, image);
                }
            } finally {
                db.close();
            }
        }
        boolean downloadBigImage = false;
        if (imageTemp == null) {
            Log.i(TAG, "Image not found in DB, getting data from internet: " + image.getName());
            downloadBigImage = true;
        } else if (!new File(imageTemp.getPath()).exists()) {
            try {
                Log.i(TAG, "Image found in DB, but doesn't exist in path: " + imageTemp.getPath()
                        + "\nAttempting URLDecoding method with default charset:" + java.nio.charset.Charset.defaultCharset().displayName());
                if (!new File(java.net.URLDecoder.decode(imageTemp.getPath(), java.nio.charset.Charset.defaultCharset().displayName())).exists()) {
                    Log.i(TAG, "Image found in DB, but doesn't exist in URL decoded path: " + java.net.URLDecoder.decode(imageTemp.getPath(), java.nio.charset.Charset.defaultCharset().displayName()));
                    downloadBigImage = true;
                } // else Log.i(TAG, "Image found in DB with URL decoded path: " +
                // java.net.URLDecoder.decode(imageTemp.getPath(),
                // java.nio.charset.Charset.defaultCharset().displayName()));

            } catch (Exception e) {
                Log.i(TAG, "Image found in DB, but path string seems to be broken: " + imageTemp.getPath()
                        + " Charset:" + java.nio.charset.Charset.defaultCharset().displayName());
                downloadBigImage = true;
            }
        }
        if (downloadBigImage) {
            Log.d(TAG, "Downloading big image from internet: " + image.getName());
            imageTemp = getImageModelFromInternet(image, notifier);
        }

        return imageTemp;
    }

    /**
     * Get image from internet from File:xxx
     *
     * @param image
     * @param notifier
     * @return
     * @throws Exception
     */
    public ImageModel getImageModelFromInternet(ImageModel image, ICallbackNotifier notifier) throws Exception {
        checkInternetConnection();
        String url = image.getName();
        if (!url.startsWith("http"))
            url = UIHelper.getBaseUrl(LNReaderApplication.getInstance().getApplicationContext()) + url;

        if (notifier != null) {
            String message = context.getResources().getString(R.string.load_novel_image_fetch, url);
            notifier.onProgressCallback(new CallbackEventData(message, TAG));
        }

        int retry = 0;
        while (retry < getRetry()) {
            try {
                Response response = connect(url, retry);
                Document doc = response.parse();

                // only return the full image url
                image = CommonParser.parseImagePage(doc);

                DownloadFileTask downloader = new DownloadFileTask(image.getUrl(), notifier);
                image = downloader.downloadImage();
                image.setReferer(url);

                image = insertImage(image);
                break;
            } catch (EOFException eof) {
                if (notifier != null) {
                    String message = context.getResources().getString(R.string.load_novel_retry, retry, getRetry(), eof.getMessage());
                    notifier.onProgressCallback(new CallbackEventData(message, TAG));
                }
                ++retry;
                if (retry >= getRetry())
                    throw eof;
            }
        }
        return image;
    }

    public ImageModel insertImage(ImageModel image) {
        synchronized (dbh) {
            // save to db and get the saved value
            SQLiteDatabase db = dbh.getWritableDatabase();
            try {
                image = ImageModelHelper.insertImage(dbh, db, image);
            } finally {
                db.close();
            }
        }
        return image;
    }

    public ArrayList<ImageModel> getAllImages() {
        ArrayList<ImageModel> result;
        synchronized (dbh) {
            SQLiteDatabase db = dbh.getReadableDatabase();
            result = ImageModelHelper.getAllImages(dbh, db);
        }
        return result;
    }

    // endregion

    // region bookmarks
    public ArrayList<BookmarkModel> getBookmarks(PageModel novel) {
        ArrayList<BookmarkModel> bookmarks = new ArrayList<BookmarkModel>();
        synchronized (dbh) {
            SQLiteDatabase db = dbh.getReadableDatabase();
            bookmarks = BookmarkModelHelper.getBookmarks(dbh, db, novel);
        }
        return bookmarks;
    }

    public int addBookmark(BookmarkModel bookmark) {
        synchronized (dbh) {
            SQLiteDatabase db = dbh.getWritableDatabase();
            return BookmarkModelHelper.insertBookmark(dbh, db, bookmark);
        }
    }

    public int deleteBookmark(BookmarkModel bookmark) {
        synchronized (dbh) {
            SQLiteDatabase db = dbh.getWritableDatabase();
            return BookmarkModelHelper.deleteBookmark(dbh, db, bookmark);
        }
    }

    public ArrayList<BookmarkModel> getAllBookmarks(boolean isOrderByDate) {
        ArrayList<BookmarkModel> bookmarks = new ArrayList<BookmarkModel>();
        synchronized (dbh) {
            SQLiteDatabase db = dbh.getReadableDatabase();
            bookmarks = BookmarkModelHelper.getAllBookmarks(dbh, db, isOrderByDate);
        }
        return bookmarks;
    }
    // endregion

    // region updates
    public boolean isContentUpdated(PageModel page) {
        synchronized (dbh) {
            SQLiteDatabase db = dbh.getReadableDatabase();
            return dbh.isContentUpdated(db, page);
        }
    }

    public int isNovelUpdated(PageModel page) {
        synchronized (dbh) {
            SQLiteDatabase db = dbh.getReadableDatabase();
            return dbh.isNovelUpdated(db, page);
        }
    }

    public ArrayList<UpdateInfoModel> getAllUpdateHistory() {
        synchronized (dbh) {
            SQLiteDatabase db = dbh.getReadableDatabase();
            ArrayList<UpdateInfoModel> updates = UpdateInfoModelHelper.getAllUpdateHistory(dbh, db);
            for (UpdateInfoModel update : updates) {
                try {
                    update.getUpdatePageModel();
                } catch (Exception e) {
                    Log.e(TAG, "Unable to get pagemodel for: " + update.getUpdatePage(), e);
                }
            }

            return updates;
        }
    }

    public void deleteAllUpdateHistory() {
        synchronized (dbh) {
            SQLiteDatabase db = dbh.getWritableDatabase();
            UpdateInfoModelHelper.deleteAllUpdateHistory(db);
        }
    }

    public void deleteUpdateHistory(UpdateInfoModel updateInfo) {
        synchronized (dbh) {
            SQLiteDatabase db = dbh.getWritableDatabase();
            UpdateInfoModelHelper.deleteUpdateHistory(dbh, db, updateInfo);
        }
    }

    public void insertUpdateHistory(UpdateInfoModel update) {
        synchronized (dbh) {
            SQLiteDatabase db = dbh.getWritableDatabase();
            UpdateInfoModelHelper.insertUpdateHistory(dbh, db, update);
        }
    }

    // endregion

    // region others
    public ArrayList<PageModel> doSearch(String searchStr, boolean isNovelOnly, ArrayList<String> languageList) {
        if (searchStr == null || searchStr.length() < 3)
            return null;

        ArrayList<PageModel> result;
        synchronized (dbh) {
            SQLiteDatabase db = dbh.getReadableDatabase();
            result = dbh.doSearch(db, searchStr, isNovelOnly, languageList);
        }
        return result;
    }

    public void resetZoomLevel(ICallbackNotifier notifier) throws Exception {
        if (notifier != null) {
            String message = "Resetting default zoom level";
            notifier.onProgressCallback(new CallbackEventData(message, TAG));
        }
        // save the changes
        synchronized (dbh) {
            SQLiteDatabase db = dbh.getWritableDatabase();
            try {
                int result = NovelContentUserHelperModel.resetZoomLevel(dbh, db);
                Log.d(TAG, "Affected: " + result);
            } finally {
                db.close();
            }
        }
    }

    // endregion

    // region private methods

    private int getRetry() {
        return UIHelper.getIntFromPreferences(Constants.PREF_RETRY, 3);
    }

    private int getTimeout(int retry) {
        boolean increaseRetry = PreferenceManager.getDefaultSharedPreferences(LNReaderApplication.getInstance().getApplicationContext()).getBoolean(Constants.PREF_INCREASE_RETRY, false);
        int timeout = UIHelper.getIntFromPreferences(Constants.PREF_TIMEOUT, 60) * 1000;
        if (increaseRetry) {
            timeout = timeout * (retry + 1);
        }
        return timeout;
    }

    /**
     * Get if need to use internal app keystore.
     *
     * @return
     */
    private boolean getUseAppKeystore() {
        boolean result = UIHelper.getUseAppKeystore(LNReaderApplication.getInstance().getApplicationContext());
        if (result) {
            Log.d(TAG, "Using app keystore");
        }
        return result;
    }

    private boolean isQuickLoad() {
        return UIHelper.getQuickLoad(LNReaderApplication.getInstance().getApplicationContext());
    }

    private boolean isAlphabeticalOrder() {
        return UIHelper.isAlphabeticalOrder(LNReaderApplication.getInstance().getApplicationContext());
    }

    // endregion

    // region storage
    public ArrayList<FindMissingModel> getMissingItems(String extra) {
        ArrayList<FindMissingModel> list = null;

        if (extra.equalsIgnoreCase(Constants.PREF_REDLINK_CHAPTER)) {
            synchronized (dbh) {
                SQLiteDatabase db = dbh.getReadableDatabase();
                list = FindMissingModelHelper.getAllRedlinkChapter(dbh, db);
            }
        } else if (extra.equalsIgnoreCase(Constants.PREF_MISSING_CHAPTER)) {
            synchronized (dbh) {
                SQLiteDatabase db = dbh.getReadableDatabase();
                list = FindMissingModelHelper.getAllMissingChapter(dbh, db);
            }
        } else if (extra.equalsIgnoreCase(Constants.PREF_EMPTY_BOOK)) {
            synchronized (dbh) {
                SQLiteDatabase db = dbh.getReadableDatabase();
                list = FindMissingModelHelper.getAllEmptyBook(dbh, db);
            }
        } else if (extra.equalsIgnoreCase(Constants.PREF_EMPTY_NOVEL)) {
            synchronized (dbh) {
                SQLiteDatabase db = dbh.getReadableDatabase();
                list = FindMissingModelHelper.getAllEmptyNovel(dbh, db);
            }
        } else {
            list = new ArrayList<FindMissingModel>();
            FindMissingModel dummy = new FindMissingModel();
            dummy.setTitle("Dummy Title: " + extra);
            dummy.setDetails("Dummy Details");
            list.add(dummy);
        }
        return list;
    }

    public int deleteMissingItem(FindMissingModel missing, String mode) {
        if (mode.equalsIgnoreCase(Constants.PREF_REDLINK_CHAPTER)) {
            return deletePage(new PageModel(missing.getPage()));
        } else if (mode.equalsIgnoreCase(Constants.PREF_MISSING_CHAPTER)) {
            return deletePage(new PageModel(missing.getPage()));
        } else if (mode.equalsIgnoreCase(Constants.PREF_EMPTY_BOOK)) {
            BookModel book = new BookModel();
            book.setPage(missing.getPage());
            book.setTitle(missing.getDetails());
            // Log.d(TAG, "Delete Book: " + book.getPage() + " " + book.getTitle());
            return deleteBooks(book);
        } else if (mode.equalsIgnoreCase(Constants.PREF_EMPTY_NOVEL)) {
            return deleteNovel(new PageModel(missing.getPage()));
        }
        return 0;
    }

    // endregion

    // region network
    private void checkInternetConnection() throws BakaReaderException {
        if (!LNReaderApplication.getInstance().isOnline())
            throw new BakaReaderException("OFFLINE (No Internet Connection)", BakaReaderException.OFFLINE);
    }

    private Response connect(String url, int retry) throws IOException {
        // allow to use its keystore.
        return Jsoup.connect(url).validateTLSCertificates(!getUseAppKeystore()).timeout(getTimeout(retry)).execute();
    }

    // endregion


}