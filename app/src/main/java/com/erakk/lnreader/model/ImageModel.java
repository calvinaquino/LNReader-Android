package com.erakk.lnreader.model;

import java.net.URL;
import java.util.Date;

public class ImageModel {
	private int id = -1;
	private String name;
	private String path;
	private URL url;
	private Date lastUpdate;
	private Date lastCheck;

	private String referer;
	private boolean isBigImage;

	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public URL getUrl() {
		return url;
	}
	public void setUrl(URL url) {
		this.url = url;
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
	public String getReferer() {
		return referer;
	}
	public void setReferer(String referer) {
		this.referer = referer;
	}
	public boolean isBigImage() {
		return isBigImage;
	}
	public void setBigImage(boolean isBigImage) {
		this.isBigImage = isBigImage;
	}
	@Override
	public String toString(){
		return name;
	}
}
