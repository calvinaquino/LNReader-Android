package com.erakk.lnreader.model;

import java.util.ArrayList;
import java.util.Date;

public class BookModel {
	private int id;
	private String title;
	private ArrayList<PageModel> chapterCollection;
	private String page;
	
	private Date lastUpdate;
	private Date lastCheck;
	private int order;	
	
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
		return chapterCollection;
	}
	public void setChapterCollection(ArrayList<PageModel> chapterCollection) {
		this.chapterCollection = chapterCollection;
	}
	
	public String toString() {
		return title;
	}
}
