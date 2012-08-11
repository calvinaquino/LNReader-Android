// Name might be changed

package com.erakk.lnreader.model;

import java.util.ArrayList;

import android.net.Uri;

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
	
	public Uri getCoverUri() {
		return coverUri;
	}
	public void setCoverUri(Uri coverUri) {
		this.coverUri = coverUri;
	}

	private String cover;
	private Uri coverUri;
	private String synopsis;	
	private ArrayList<BookModel> bookCollections; 
	
}
