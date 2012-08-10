// Name might be changed

package com.erakk.lnreader.model;

import java.util.ArrayList;

public class NovelCollectionModel {
	private PageModel page;	
	public PageModel getPage() {
		return page;
	}
	public void setPage(PageModel page) {
		this.page = page;
	}
	public String getCover() {
		return cover;
	}
	public void setCover(String cover) {
		this.cover = cover;
	}
	public String getSynopsis() {
		return synopsis;
	}
	public void setSynopsis(String synopsis) {
		this.synopsis = synopsis;
	}
	public ArrayList<BookModel> getBookCollections() {
		return bookCollections;
	}
	public void setBookCollections(ArrayList<BookModel> bookCollections) {
		this.bookCollections = bookCollections;
	}
	private String cover;
	private String synopsis;	
	private ArrayList<BookModel> bookCollections; 
	
}
