// Name might be changed

package com.erakk.lnreader.model;

import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import com.erakk.lnreader.helper.AsyncTaskResult;
import com.erakk.lnreader.helper.DownloadImageTask;

import android.graphics.Bitmap;
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
	
	public URL getCoverUrl() {
		return coverUrl;
	}
	public void setCoverUrl(URL coverUri) {
		this.coverUrl = coverUri;
	}

	public Bitmap getCoverBitmap() {
		if(coverBitmap == null) {
			DownloadImageTask t = new DownloadImageTask(); 
			t.execute(new URL[] {coverUrl});
			
			try {
				AsyncTaskResult<Bitmap> result = t.get();
				if(result.getError() == null) {
					coverBitmap = result.getResult();
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 			
		}
		return coverBitmap;
	}

	private String cover;
	private URL coverUrl;
	private Bitmap coverBitmap;
	private String synopsis;	
	private ArrayList<BookModel> bookCollections; 
	
}
