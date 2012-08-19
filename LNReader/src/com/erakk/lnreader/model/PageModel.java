//package com.nandaka.bakareaderclone.model;
package com.erakk.lnreader.model;

import java.util.Date;

public class PageModel {
	public static final String TYPE_NOVEL = "Novel";
	public static final String TYPE_OTHER = "Other";
	public static final String TYPE_CONTENT = "Content";
	
	private int id;
	private String page;
	private String title;
	private String type;
	private Date lastUpdate;
	private String parent;
	private Date lastCheck;
	private boolean isWatched;
	private boolean isFinishedRead;
	
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
		return title;
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
	
}
