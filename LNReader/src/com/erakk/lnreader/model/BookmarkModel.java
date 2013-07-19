package com.erakk.lnreader.model;

import java.util.Date;

import com.erakk.lnreader.dao.NovelsDao;

public class BookmarkModel {
	private int id = -1;
	private String page;
	private String subPage;
	private int pIndex;
	private String excerpt;
	private Date creationDate;

	private PageModel pageModel;

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

	public String getSubPage() {
		return subPage;
	}

	public void setSubPage(String subPage) {
		this.subPage = subPage;
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
			pageModel = NovelsDao.getInstance().getPageModel(tempPage, null);
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
}
