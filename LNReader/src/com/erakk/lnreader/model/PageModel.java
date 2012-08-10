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
	private String parent;
	private Date lastCheck;
	
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
	
}
