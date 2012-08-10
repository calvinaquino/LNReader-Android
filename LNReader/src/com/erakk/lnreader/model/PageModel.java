//package com.nandaka.bakareaderclone.model;
package com.erakk.lnreader.model;

import java.util.Date;

public class PageModel {
	public static final String TYPE_NOVEL = "Novel";
	public static final String TYPE_OTHER = "Other";
	public static final String TYPE_CONTENT = "Content";
	
	private String page;
	private String title;
	private String type;
	private Date lastUpdate;
	
	public String getPage() {
		return page;
	}
	public void setPage(String page) {
		this.page = page;
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
	
	public String toString() {
		return "Page: " + page + "; Title: " + title;
	}
	
}
