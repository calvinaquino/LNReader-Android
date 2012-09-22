//package com.nandaka.bakareaderclone.model;
package com.erakk.lnreader.model;

import java.util.Date;
import java.util.Iterator;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.dao.NovelsDao;

public class PageModel{
	public static final String TYPE_NOVEL = "Novel";
	public static final String TYPE_OTHER = "Other";
	public static final String TYPE_CONTENT = "Content";
	
	private int id;
	private String page;
	private String title;
	private String type;
	private Date lastUpdate;
	private String parent;
	private PageModel parentPageModel;
	private Date lastCheck;
	private boolean isWatched;
	private boolean isFinishedRead;
	private boolean isDownloaded;
	private BookModel book;
	private int order;

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
	public int getOrder() {
		return order;
	}
	public void setOrder(int order) {
		this.order = order;
	}
	public PageModel getParentPageModel() throws Exception {
		if(this.parentPageModel == null) {
			NovelsDao dao = NovelsDao.getInstance();
			if(this.type.contentEquals(TYPE_CONTENT)) {
				String tempParent = parent.substring(0, parent.indexOf(Constants.NOVEL_BOOK_DIVIDER));
				this.parentPageModel = dao.getPageModel(tempParent, null);
			}
			else {
				this.parentPageModel = dao.getPageModel(this.parent, null);
			}
		}
		return parentPageModel;
	}
	public void setParentPageModel(PageModel parentPageModel) {
		this.parentPageModel = parentPageModel;
	}
	public boolean isDownloaded() {
		return isDownloaded;
	}
	public void setDownloaded(boolean isDownloaded) {
		this.isDownloaded = isDownloaded;
	}
	
	public BookModel getBook() {
		if(this.getType().equals(TYPE_CONTENT)) {
			if(this.book == null) {
				NovelsDao dao = NovelsDao.getInstance();
				try {
					String bookTitle = parent.substring(parent.indexOf(Constants.NOVEL_BOOK_DIVIDER)+1);
					NovelCollectionModel details = dao.getNovelDetails(getParentPageModel(), null);
					for(Iterator<BookModel> iBook = details.getBookCollections().iterator();iBook.hasNext();) {
						BookModel tempBook = iBook.next();
						if(tempBook.getTitle().equals(bookTitle)){
							this.book = tempBook;
							break;
						}						
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}				
			}
			return this.book;
		}
		return null;
	}
	public void setBook(BookModel book) {
		this.book = book;
	}
}
