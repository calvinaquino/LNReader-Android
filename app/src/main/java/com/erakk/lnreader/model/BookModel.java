package com.erakk.lnreader.model;

import java.util.ArrayList;
import java.util.Date;

import com.erakk.lnreader.dao.NovelsDao;

public class BookModel {
	private int id = -1;
	private String title;
	private ArrayList<PageModel> chapterCollection;
	private String page;

	private Date lastUpdate;
	private Date lastCheck;
	private int order;

	private NovelCollectionModel parent;

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
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public int getOrder() {
		return order;
	}
	public void setOrder(int order) {
		this.order = order;
	}
	public ArrayList<PageModel> getChapterCollection() {
		if(chapterCollection == null){
			chapterCollection = NovelsDao.getInstance().getChapterCollection(page, title, this);
		}
		return chapterCollection;
	}
	public void setChapterCollection(ArrayList<PageModel> chapterCollection) {
		this.chapterCollection = chapterCollection;
	}

	public NovelCollectionModel getParent() {
		return parent;
	}
	public void setParent(NovelCollectionModel parent) {
		this.parent = parent;
	}

	@Override
	public String toString() {
		return title;
	}
}
