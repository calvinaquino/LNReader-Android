//package com.nandaka.bakareaderclone.model;
package com.erakk.lnreader.model;

import android.util.Log;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.Util;

import java.util.ArrayList;
import java.util.Date;

public class PageModel {
    public static final String TYPE_NOVEL = "Novel";
    public static final String TYPE_OTHER = "Other";
    public static final String TYPE_CONTENT = "Content";
    public static final String TYPE_TOS = "Copyrights";
    private static final String TAG = PageModel.class.toString();

    private int id = -1;
    private String page;
    private String language; /* Attribute for language marker */
    private String title;
    private String type;
    private Date lastUpdate;
    private String parent;
    private PageModel parentPageModel;
    private PageModel pageModel;
    private Date lastCheck;
    private boolean isWatched;
    private boolean isFinishedRead = false;
    private boolean isDownloaded;
    private BookModel book;
    private int order;
    private String status;
    private boolean isHighlighted = false;
    private boolean isMissing = false;
    private boolean isExternal = false;
    private int wikiId = -1;
    private ArrayList<String> categories;

    private String redirectedTo;

    private int updateCount;

    // not saved to db
    private boolean isUpdated = false;

    public PageModel() {
    }

    public PageModel(String pageModel) {
        this.page = pageModel;
    }

    /**
     * Used by other models, will auto download if not exists.
     *
     * @param page
     * @return null if not exists and failed to download.
     * @throws Exception
     */
    public static PageModel getPageModelByName(String page) throws Exception {
        PageModel tempPage = new PageModel();
        tempPage.setPage(page);
        tempPage = NovelsDao.getInstance().getPageModel(tempPage, null);
        return tempPage;
    }

    /**
     * Used by other models, will not autodownload.
     *
     * @param page
     * @return
     * @throws Exception
     */
    public static PageModel getExistingPageModelByName(String page) throws Exception {
        PageModel tempPage = new PageModel();
        tempPage.setPage(page);
        tempPage = NovelsDao.getInstance().getExistingPageModel(tempPage, null);
        return tempPage;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public String getLanguage() {
        if (language == null)
            language = Constants.LANG_ENGLISH;
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public Date getLastCheck() {
        return lastCheck;
    }

    public void setLastCheck(Date lastCheck) {
        this.lastCheck = lastCheck;
    }

    public boolean isWatched() {
        return isWatched;
    }

    public void setWatched(boolean isWatched) {
        this.isWatched = isWatched;
    }

    public boolean isFinishedRead() {
        return isFinishedRead;
    }

    public void setFinishedRead(boolean isFinishedRead) {
        this.isFinishedRead = isFinishedRead;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public PageModel getParentPageModel() throws Exception {
        if (this.parentPageModel == null) {
            // get the page for parent
            String tempParent = this.parent;
            int divIndex = parent.indexOf(Constants.NOVEL_BOOK_DIVIDER);
            if (this.type.contentEquals(TYPE_CONTENT) && divIndex > 0) {
                tempParent = parent.substring(0, divIndex);
            }
            this.parentPageModel = PageModel.getPageModelByName(tempParent);
        }
        return parentPageModel;
    }

    public void setParentPageModel(PageModel parentPageModel) {
        this.parentPageModel = parentPageModel;
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

    public boolean isDownloaded() {
        return isDownloaded;
    }

    public void setDownloaded(boolean isDownloaded) {
        this.isDownloaded = isDownloaded;
    }

    public String getBookTitle() {
        if (parent == null)
            return "";

        return parent.substring(parent.indexOf(Constants.NOVEL_BOOK_DIVIDER) + Constants.NOVEL_BOOK_DIVIDER.length());
    }

    public BookModel getBook(boolean autoDownload) {
        if (this.getType() != null && this.getType().equals(TYPE_CONTENT)) {
            if (this.book == null) {
                NovelsDao dao = NovelsDao.getInstance();
                try {
                    NovelCollectionModel details = dao.getNovelDetails(getParentPageModel(), null, autoDownload);
                    if (details != null) {
                        for (BookModel tempBook : details.getBookCollections()) {
                            if (tempBook != null && tempBook.getTitle().equals(getBookTitle())) {
                                this.book = tempBook;
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Unable to get book for: " + getPage(), e);
                }
            }
            return this.book;
        }
        return null;
    }

    public void setBook(BookModel book) {
        this.book = book;
    }

    public boolean isUpdated() {
        return isUpdated;
    }

    public void setUpdated(boolean isUpdated) {
        this.isUpdated = isUpdated;
    }

    public String getRedirectedTo() {
        return redirectedTo;
    }

    public void setRedirectedTo(String redirectedTo) {
        this.redirectedTo = redirectedTo;
    }

    public boolean isHighlighted() {
        return isHighlighted;
    }

    public void setHighlighted(boolean isHighlighted) {
        this.isHighlighted = isHighlighted;
    }

    public boolean isMissing() {
        return isMissing;
    }

    public void setMissing(boolean isMissing) {
        this.isMissing = isMissing;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isTeaser() {
        if (status != null && status.length() > 0) {
            return status.contains(Constants.STATUS_TEASER);
        }
        return false;
    }

    public boolean isStalled() {
        if (status != null && status.length() > 0) {
            return status.contains(Constants.STATUS_STALLED);
        }
        return false;
    }

    public boolean isAbandoned() {
        if (status != null && status.length() > 0) {
            return status.contains(Constants.STATUS_ABANDONED);
        }
        return false;
    }

    public boolean isPending() {
        if (status != null && status.length() > 0) {
            return status.contains(Constants.STATUS_PENDING);
        }
        return false;
    }

    public boolean isExternal() {
        return isExternal;
    }

    public void setExternal(boolean isExternal) {
        this.isExternal = isExternal;
    }

    public int getUpdateCount() {
        return updateCount;
    }

    public void setUpdateCount(int updateCount) {
        this.updateCount = updateCount;
    }

    public boolean isRedlink() {
        if (Util.isStringNullOrEmpty(this.page))
            return false;
        return this.page.endsWith("&action=edit&redlink=1");
    }

    public int getWikiId() {
        return wikiId;
    }

    public void setWikiId(int wikiId) {
        this.wikiId = wikiId;
    }

    public ArrayList<String> getCategories() {
        return categories;
    }

    public void setCategories(ArrayList<String> categories) {
        this.categories = categories;
    }
}
