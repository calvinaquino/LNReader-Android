package com.erakk.lnreader.model;

import java.util.ArrayList;

public class BookModel {
	private String title;
	private ArrayList<PageModel> chapterCollection;
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
}
