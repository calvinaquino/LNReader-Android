package com.erakk.lnreader.model;

import java.util.ArrayList;
import java.util.Date;

public class NovelContentModel {
	private int id;
	private String content;
	private String page;
	private PageModel pageModel;
	
	private int lastXScroll;
	private int lastYScroll;
	private double lastZoom;
	
	private Date lastUpdate;
	private Date lastCheck;
	
	private ArrayList<ImageModel> images;
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public String getPage() {
		return page;
	}
	public void setPage(String page) {
		this.page = page;
	}
	public PageModel getPageModel() {
		return pageModel;
	}
	public void setPageModel(PageModel pageModel) {
		this.pageModel = pageModel;
	}
	public int getLastXScroll() {
		return lastXScroll;
	}
	public void setLastXScroll(int lastXScroll) {
		this.lastXScroll = lastXScroll;
	}
	public int getLastYScroll() {
		return lastYScroll;
	}
	public void setLastYScroll(int lastYScroll) {
		this.lastYScroll = lastYScroll;
	}
	public double getLastZoom() {
		return lastZoom;
	}
	public void setLastZoom(double lastZoom) {
		this.lastZoom = lastZoom;
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
	public ArrayList<ImageModel> getImages() {
		return images;
	}
	public void setImages(ArrayList<ImageModel> images) {
		this.images = images;
	}
	
	
}
