package com.erakk.lnreader.model;

import android.util.Log;

import com.erakk.lnreader.dao.NovelsDao;

import java.util.ArrayList;
import java.util.Date;

public class NovelContentModel {
    private static final String TAG = NovelContentModel.class.toString();
    private int id = -1;
    private String content;
    private String page;
    private PageModel pageModel;

    private int lastXScroll;
    private int lastYScroll;
    private double lastZoom;

    private Date lastUpdate;
    private Date lastCheck;

    private boolean isUpdatingFromInternet;

    private ArrayList<ImageModel> images;

    private ArrayList<BookmarkModel> bookmarks;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public void refreshPageModel() throws Exception {
        // try to disable the checking, may hit performance issue?
        // refer to https://github.com/calvinaquino/LNReader-Android/issues/177
        //if(this.pageModel == null) {
        NovelsDao dao = NovelsDao.getInstance();
        PageModel tempPage = new PageModel();
        tempPage.setPage(this.page);
        this.pageModel = dao.getPageModel(tempPage, null);
        //}
    }

    public PageModel getPageModel() throws Exception {
        if (this.pageModel == null) {
            NovelsDao dao = NovelsDao.getInstance();
            PageModel tempPage = new PageModel();
            tempPage.setPage(this.page);
            this.pageModel = dao.getPageModel(tempPage, null);
        }
        return pageModel;
    }

    public void setPageModel(PageModel pageModel) {
        this.pageModel = pageModel;
    }

    public int getLastXScroll() {
        return lastXScroll;
    }

    public void setLastXScroll(int lastXScroll) {
        this.lastXScroll = lastXScroll;
    }

    public int getLastYScroll() {
        return lastYScroll;
    }

    public void setLastYScroll(int lastYScroll) {
        this.lastYScroll = lastYScroll;
    }

    public double getLastZoom() {
        return lastZoom;
    }

    public void setLastZoom(double lastZoom) {
        this.lastZoom = lastZoom;
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

    public ArrayList<ImageModel> getImages() {
        return images;
    }

    public void setImages(ArrayList<ImageModel> images) {
        this.images = images;
    }

    public boolean isUpdatingFromInternet() {
        return isUpdatingFromInternet;
    }

    public void setUpdatingFromInternet(boolean isUpdating) {
        this.isUpdatingFromInternet = isUpdating;
    }

    public ArrayList<BookmarkModel> getBookmarks() {
        if (this.bookmarks == null) {
            try {
                this.bookmarks = NovelsDao.getInstance().getBookmarks(getPageModel());
            } catch (Exception e) {
                Log.e(TAG, "Error when getting bookmarks: " + e.getMessage(), e);
            }
        }
        return bookmarks;
    }

    public void setBookmarks(ArrayList<BookmarkModel> bookmarks) {
        this.bookmarks = bookmarks;
    }
}
