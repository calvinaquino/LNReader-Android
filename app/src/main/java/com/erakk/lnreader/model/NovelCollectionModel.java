package com.erakk.lnreader.model;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.helper.Util;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;

public class NovelCollectionModel {
    private static final String TAG = NovelCollectionModel.class.toString();
    private int id = -1;
    private PageModel pageModel;
    private String page;
    private String cover;
    private URL coverUrl;
    private Bitmap coverBitmap;
    private String synopsis;
    private ArrayList<BookModel> bookCollections;

    private String redirectTo;

    private Date lastUpdate;
    private Date lastCheck;
    private ArrayList<PageModel> _FlattedChapterList;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public PageModel getPageModel() throws Exception {
        if (this.pageModel == null) {
            this.pageModel = PageModel.getPageModelByName(this.page);
        }
        return pageModel;
    }

    public void setPageModel(PageModel pageModel) {
        this.pageModel = pageModel;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public void setSynopsis(String synopsis) {
        this.synopsis = synopsis;
    }

    public ArrayList<BookModel> getBookCollections() {
        return bookCollections;
    }

    public void setBookCollections(ArrayList<BookModel> bookCollections) {
        this.bookCollections = bookCollections;
    }

    public URL getCoverUrl() {
        if (this.coverUrl == null && this.cover != null && this.cover.length() > 0) {
            try {
                this.coverUrl = new URL(this.cover);
            } catch (MalformedURLException e) {
                Log.e(TAG, "Invalid url: " + this.cover, e);
            }
        }
        return coverUrl;
    }

    public void setCoverUrl(URL coverUri) {
        this.coverUrl = coverUri;
    }

    public Bitmap getCoverBitmap() {
        if (coverBitmap == null) {
            try {
                // TODO: maybe it is better to use ImageModel
                if (getCoverUrl() != null) {
                    @SuppressWarnings("deprecation")
                    String filepath = UIHelper.getImageRoot(LNReaderApplication.getInstance().getApplicationContext()) + Util.sanitizeFilename(URLDecoder.decode(getCoverUrl().getFile()));
                    Log.d("GetCover", filepath);
                    this.coverBitmap = BitmapFactory.decodeFile(filepath);
                }
            } catch (Exception e) {
                Log.e("GetCover", e.getClass().toString() + ": " + e.getMessage(), e);
            }
        }
        // Redimension image so they all have a constant size
        // coverBitmap = Bitmap.createScaledBitmap(coverBitmap, 200, 300, true);
        return coverBitmap;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public Date getLastCheck() {
        return lastCheck;
    }

    public void setLastCheck(Date lastCheck) {
        this.lastCheck = lastCheck;
    }

    public String getPage() {
        return this.page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    @Override
    public String toString() {
        return page;
    }

    public ArrayList<PageModel> getFlattedChapterList() {
        if (_FlattedChapterList == null) {
            _FlattedChapterList = new ArrayList<PageModel>();
            for (BookModel b : bookCollections) {
                for (PageModel temp : b.getChapterCollection()) {
                    _FlattedChapterList.add(temp);
                }
            }
        }
        return _FlattedChapterList;
    }

    public PageModel getNext(String page, boolean includeMissing, boolean includeRedlink) {
        if (Util.isStringNullOrEmpty(page))
            return null;

        int index = getCurrentIndex(page);
        PageModel next = null;

        if (index != -1) {
            index++;
            while (index < getFlattedChapterList().size()) {
                PageModel temp = getFlattedChapterList().get(index);
                if (!includeRedlink && temp.isRedlink()) {
                    index++;
                    continue;
                } else if (!includeMissing && temp.isMissing()) {
                    index++;
                    continue;
                }
                next = temp;
                break;
            }
        }

        return next;
    }

    public PageModel getPrev(String page, boolean includeMissing, boolean includeRedlink) {
        if (Util.isStringNullOrEmpty(page))
            return null;

        int index = getCurrentIndex(page);
        PageModel prev = null;
        if (index != -1) {
            index--;
            while (index >= 0) {
                PageModel temp = getFlattedChapterList().get(index);
                if (!includeRedlink && temp.isRedlink()) {
                    index--;
                    continue;
                } else if (!includeMissing && temp.isMissing()) {
                    index--;
                    continue;
                }

                prev = temp;
                break;
            }
        }

        return prev;
    }

    private int getCurrentIndex(String page) {
        int index = -1;
        for (PageModel temp : getFlattedChapterList()) {
            if (temp.getPage().contentEquals(page)) {
                index = getFlattedChapterList().indexOf(temp);
                break;
            }
        }
        return index;
    }

    public String getRedirectTo() {
        return redirectTo;
    }

    public void setRedirectTo(String redirectTo) {
        this.redirectTo = redirectTo;
    }

}
