// Name might be changed

package com.erakk.lnreader.model;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.erakk.lnreader.Constants;

public class NovelCollectionModel {
	private int id;
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public PageModel getPageModel() {
		return pageModel;
	}
	public void setPageModel(PageModel pageModel) {
		this.pageModel = pageModel;
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
		if(this.coverUrl == null) {
			try {
				this.coverUrl = new URL(this.cover);
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return coverUrl;
	}
	
	public void setCoverUrl(URL coverUri) {
		this.coverUrl = coverUri;
	}

	public Bitmap getCoverBitmap() {
		if(coverBitmap == null) {
			try{
			String filepath = Constants.IMAGE_ROOT + getCoverUrl().getFile();
			Log.d("GetCover", filepath);
			this.coverBitmap = BitmapFactory.decodeFile(filepath);
			}catch(Exception e){
				e.printStackTrace();
				Log.e("GetCover", e.getClass().toString() + ": " + e.getMessage());
			}
//			DownloadImageTask t = new DownloadImageTask(); 
//			t.execute(new URL[] {coverUrl});
//			
//			try {
//				AsyncTaskResult<Bitmap> result = t.get();
//				if(result.getError() == null) {
//					coverBitmap = result.getResult();
//				}
//			} catch (Exception e) {
//				 TODO Auto-generated catch block
//				e.printStackTrace();
//			} 			
		}
		// Redimension image so they all have a constant size
		//coverBitmap = Bitmap.createScaledBitmap(coverBitmap, 200, 300, true); 
		return coverBitmap;
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

	public void setPage(String page) {
		this.page = page;
	}
	public String getPage() {
		return this.page;
	}

	private PageModel pageModel;	
	private String page;
	private String cover;
	private URL coverUrl;
	private Bitmap coverBitmap;
	private String synopsis;	
	private ArrayList<BookModel> bookCollections; 
	
	private Date lastUpdate;
	private Date lastCheck;
	
	public String toString() {
		return page;
	}
	
}
