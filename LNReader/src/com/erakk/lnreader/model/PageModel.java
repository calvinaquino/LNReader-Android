//package com.nandaka.bakareaderclone.model;
package com.erakk.lnreader.model;

import java.util.Date;
import java.util.Iterator;

import android.util.Log;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.dao.NovelsDao;

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

	private String redirectedTo;

	private int updateCount;

	// not saved to db
	private boolean isUpdated = false;

	public PageModel() {
	}

	public PageModel(String pageModel) {
		this.page = pageModel;
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
			NovelsDao dao = NovelsDao.getInstance();
			PageModel tempPage = new PageModel();
			if (this.type.contentEquals(TYPE_CONTENT)) {
				String tempParent = parent.substring(0, parent.indexOf(Constants.NOVEL_BOOK_DIVIDER));
				tempPage.setPage(tempParent);
			}
			else {
				tempPage.setPage(this.parent);
			}
			this.parentPageModel = dao.getPageModel(tempPage, null);
		}
		return parentPageModel;
	}

	public void setParentPageModel(PageModel parentPageModel) {
		this.parentPageModel = parentPageModel;
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

	public boolean isDownloaded() {
		return isDownloaded;
	}

	public void setDownloaded(boolean isDownloaded) {
		this.isDownloaded = isDownloaded;
	}

	public BookModel getBook() {
		if (this.getType() != null && this.getType().equals(TYPE_CONTENT)) {
			if (this.book == null) {
				NovelsDao dao = NovelsDao.getInstance();
				try {
					String bookTitle = parent.substring(parent.indexOf(Constants.NOVEL_BOOK_DIVIDER) + Constants.NOVEL_BOOK_DIVIDER.length());
					NovelCollectionModel details = dao.getNovelDetails(getParentPageModel(), null);
					for (Iterator<BookModel> iBook = details.getBookCollections().iterator(); iBook.hasNext();) {
						BookModel tempBook = iBook.next();
						if (tempBook.getTitle().equals(bookTitle)) {
							this.book = tempBook;
							break;
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
}
