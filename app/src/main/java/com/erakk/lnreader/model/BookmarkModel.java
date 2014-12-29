package com.erakk.lnreader.model;

import java.util.Date;

import android.util.Log;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.Util;

public class BookmarkModel {
	private static final String TAG = BookmarkModel.class.toString();
	private int id = -1;
	private String page;
	private int pIndex;
	private String excerpt;
	private Date creationDate;

	private PageModel pageModel;
	private String bookmarkTitle = null;
	private String subTitle = null;

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

	public int getpIndex() {
		return pIndex;
	}

	public void setpIndex(int pIndex) {
		this.pIndex = pIndex;
	}

	public String getExcerpt() {
		return excerpt;
	}

	public void setExcerpt(String excerpt) {
		this.excerpt = excerpt;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public PageModel getPageModel() throws Exception {
		if (pageModel == null) {
			PageModel tempPage = new PageModel();
			tempPage.setPage(this.page);
			pageModel = NovelsDao.getInstance().getPageModel(tempPage, null, false);
		}
		return pageModel;
	}

	public void setPageModel(PageModel pageModel) {
		this.pageModel = pageModel;
	}

	private boolean selected;

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	/***
	 * Get Book Title
	 * 
	 * @return
	 */
	public String getBookmarkTitle() {
		if (bookmarkTitle == null) {
			bookmarkTitle = "*DELETED CHAPTER*";
			Log.d(TAG, "Page: " + page);
			try {
				pageModel = getPageModel();
				if (pageModel != null) {
					PageModel parentPage = pageModel.getParentPageModel();
					bookmarkTitle = parentPage.getTitle();
				}
			} catch (Exception ex) {
				Log.e(TAG, "Failed to get pageModel: " + page, ex);
			}
		}
		return bookmarkTitle;
	}

	public void setBookmarkTitle(String bookmarkTitle) {
		this.bookmarkTitle = bookmarkTitle;
	}

	/***
	 * Get Book title and chapter if possible.
	 * 
	 * @return
	 */
	public String getSubTitle() {
		if (subTitle == null) {
			Log.d(TAG, "Page: " + page);
			subTitle = page;
			try {
				pageModel = getPageModel();
				if (pageModel != null) {
					subTitle = pageModel.getTitle();

					String bookTitle = pageModel.getParent().substring(pageModel.getParent().indexOf(Constants.NOVEL_BOOK_DIVIDER) + Constants.NOVEL_BOOK_DIVIDER.length());
					if (!Util.isStringNullOrEmpty(bookTitle)) {
						subTitle = String.format("(%s) %s", bookTitle, subTitle);
					}
					else {
						BookModel book = pageModel.getBook(false);
						if (book != null)
							subTitle = String.format("(%s) %s", book.getTitle(), subTitle);
					}
				}
			} catch (Exception ex) {
				Log.e(TAG, "Failed to get subtitle for : " + page, ex);
			}
		}
		return subTitle;
	}

	public void setSubTitle(String subTitle) {
		this.subTitle = subTitle;
	}
}
