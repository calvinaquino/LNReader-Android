package com.erakk.lnreader.model;

import java.util.ArrayList;
import java.util.Date;

public class BookModel {
	private String title;
	private ArrayList<PageModel> chapterCollection;
	
	private Date lastUpdate;
	private Date lastCheck;
	
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
