/**
 * Parse baka-tsuki wiki page
 */
package com.erakk.lnreader.parser;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.net.Uri;
import android.provider.Browser.BookmarkColumns;
import android.util.Log;

import com.erakk.lnreader.model.BookModel;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.PageModel;

/**
 * @author Nandaka
 *
 */
public class BakaTsukiParser {

	
	private static final String TAG = "Parser";

	/**
	 * @param doc parsed page from Main_Page
	 * @return list of novels in PageModel
	 */
	public static ArrayList<PageModel> ParseNovelList(Document doc) {
		ArrayList<PageModel> result = new ArrayList<PageModel>();
		
		if(doc == null) throw new NullPointerException("Document cannot be null.");
		
		Element stage = doc.select("#p-Light_Novels").first();
		if (stage != null) {
			Log.d(TAG, "Found: #p-Light_Novels");
			
			Elements novels = stage.select("li");
			for (Iterator<Element> i = novels.iterator(); i .hasNext();) {
				Element novel = i.next();
				Element link = novel.select("a").first();
				
				PageModel page = new PageModel();
				page.setPage(link.attr("href").replace("/project/index.php?title=", ""));
				page.setType(PageModel.TYPE_NOVEL);
				page.setTitle(link.text());
				page.setLastUpdate(new Date());
				page.setLastCheck(new Date());
				page.setParent("Main_Page");
				
				result.add(page);
				Log.d(TAG, "Add: "+ link.text());
			}
		}
		return result;
	}
	
	public static NovelCollectionModel ParseNovelDetails(Document doc, PageModel page) {
		NovelCollectionModel novel = new NovelCollectionModel();
		if(doc == null) throw new NullPointerException("Document cannot be null.");
		novel.setPage(page.getPage());
		novel.setPageModel(page);
		parseNovelSynopsis(doc, novel);		
		parseNovelCover(doc, novel);				
		parseNovelChapters(doc, novel);
		
		return novel;
	}

	private static ArrayList<BookModel> parseNovelChapters(Document doc, NovelCollectionModel novel) {
		Log.d(TAG, "Start parsing book collections");
		// parse the collection
		ArrayList<BookModel> books = new ArrayList<BookModel>();
		try{
			Elements h2s = doc.select("h2");
			for(Iterator<Element> i = h2s.iterator(); i.hasNext();){
				Element h2 = i.next();
				//Log.d(TAG, "checking h2: " +h2.text() + "\n" + h2.id());
				Elements spans = h2.select("span");
				if(spans.size() > 0) {
					// find span with id containing "_by_"
					boolean containsBy = false;
					for(Iterator<Element> iSpan = spans.iterator(); iSpan.hasNext(); ) {
						Element s = iSpan.next();
						if(s.id().contains("_by")) {
							containsBy = true;
							break;
						}
					}
					if(!containsBy) continue;
					
					Log.d(TAG, "Found h2: " +h2.text());
					
					/*
					 * parse book method 1:
					 * Look for <h3> after <h2> containing the volume list.
					 */
					Element bookElement = h2;
					boolean walkBook = true;
					do{
						bookElement = bookElement.nextElementSibling();
						if(bookElement.tagName() == "h2") walkBook = false;
						else if(bookElement.tagName() == "h3") {
							Log.d(TAG, "Found: " +bookElement.text());
							BookModel book = new BookModel();
							book.setTitle(bookElement.text()); // TODO: need to sanitize the title.
							ArrayList<PageModel> chapterCollection = new ArrayList<PageModel>();
							
							// parse the chapters.
							boolean walkChapter = true;
							Element chapterElement = bookElement;
							do{
								chapterElement = chapterElement.nextElementSibling();
								if(chapterElement.tagName() == "h3") walkChapter = false;
								else if(chapterElement.tagName() == "dl" ||
										chapterElement.tagName() == "ul" ||
										chapterElement.tagName() == "div") {
									Elements chapters = chapterElement.select("li");
									for(Iterator<Element> i2 = chapters.iterator(); i2.hasNext();) {
										Element chapter = i2.next();
										Elements links = chapter.select("a");
										if(links.size() > 0) {
											Element link = links.first();
											PageModel p = new PageModel();
											p.setTitle(link.text());
											p.setPage(link.attr("href").replace("project/index.php?title=",""));
											p.setParent(novel.getPage() + "%" + book.getTitle());
											p.setType(PageModel.TYPE_CONTENT);
											Log.d(TAG, "chapter: " + link.text());
											chapterCollection.add(p);
										}										
									}
								}
								book.setChapterCollection(chapterCollection);
							}while(walkChapter);
							books.add(book);
						}						
					}while(walkBook);
					
					/*
					 * parse book method 2:
					 * Look for <p> after <h2> containing the chapter list, usually only have 1 book.
					 * See 7_Nights
					 */
					if(books.size() == 0) {
						Log.d(TAG, "No books found, use method 2");
						bookElement = h2;
						walkBook = true;
						do{
							bookElement = bookElement.nextElementSibling();
							if(bookElement.tagName() == "h2") walkBook = false;
							else if(bookElement.tagName() == "p") {
								Log.d(TAG, "Found: " +bookElement.text());
								BookModel book = new BookModel();
								book.setTitle(bookElement.text());
								ArrayList<PageModel> chapterCollection = new ArrayList<PageModel>();
								
								// parse the chapters.
								boolean walkChapter = true;
								Element chapterElement = bookElement;
								do{	
									chapterElement = chapterElement.nextElementSibling();
									if(chapterElement == null) walkChapter = false;
									else if(chapterElement.tagName() == "p") walkChapter = false;
									else if(chapterElement.tagName() == "dl") {
										Elements chapters = chapterElement.select("li");
										for(Iterator<Element> i2 = chapters.iterator(); i2.hasNext();) {
											Element chapter = i2.next();
											Elements links = chapter.select("a");
											if(links.size() > 0) {
												Element link = links.first();
												PageModel p = new PageModel();
												p.setTitle(link.text());
												p.setPage(link.attr("href").replace("project/index.php?title=",""));
												p.setParent(novel.getPage() + "%" + book.getTitle());
												p.setType(PageModel.TYPE_CONTENT);
												Log.d(TAG, "chapter: " + link.text());
												chapterCollection.add(p);
											}										
										}
									}
									// no subchapter
									if(chapterCollection.size() == 0 ) {
										Elements links = bookElement.select("a");
										if(links.size() > 0) {
											Element link = links.first();
											PageModel p = new PageModel();
											p.setTitle(link.text());
											p.setPage(link.attr("href").replace("project/index.php?title=",""));
											Log.d(TAG, "chapter: " + link.text());
											chapterCollection.add(p);
										}
									}
									book.setChapterCollection(chapterCollection);
								}while(walkChapter);
								books.add(book);
							}							
						}while(walkBook);
					}
				}
			}			
			novel.setBookCollections(books);
		} catch(Exception e) {e.printStackTrace();}
		Log.d(TAG, "Complete parsing book collections: " + books.size());
		return books;
	}

	private static String parseNovelCover(Document doc, NovelCollectionModel novel) {
		Log.d(TAG, "Start parsing cover image");
		// parse the cover image
		String imageUrl = "";
		Elements images = doc.select(".thumbimage");
		if(images.size() > 0){
			imageUrl = images.first().attr("src");
			if(!imageUrl.startsWith("http")) {
				imageUrl = "http://www.baka-tsuki.org" + imageUrl;
			}
		}
		novel.setCover(imageUrl);
		try {
			URL url = new URL(imageUrl);
			novel.setCoverUrl(url);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Log.d(TAG, "Complete parsing cover image");
		return imageUrl;
	}

	private static String parseNovelSynopsis(Document doc, NovelCollectionModel novel) {
		Log.d(TAG, "Start parsing synopsis");
		// parse the synopsis
		String synopsis = "";
		String source = "#Story_Synopsis";
		// from Story_Synopsis id
		Elements stage = doc.select(source);//.first().parent().nextElementSibling();
		// from main text
		if(stage == null || stage.size() <= 0) {
			source = "#mw-content-text,p";
			stage = doc.select(source);
			Log.d(TAG, "From: " + source);
		}
		
		if(stage.size() > 0) {
			Element synopsisE;
			if(source == "#Story_Synopsis") synopsisE = stage.first().parent().nextElementSibling();
			else synopsisE = stage.first().children().first();
		
			int i = 0;
			do{
				if(synopsisE == null) break;
				if(synopsisE.tagName() != "p") {
					synopsisE = synopsisE.nextElementSibling();
					Log.d(TAG, synopsisE.html());
					continue;
				}
				i++;
				synopsis += synopsisE.text() + "\n";
				synopsisE = synopsisE.nextElementSibling();
				if(synopsisE != null && synopsisE.tagName() != "p" && i > 0) break;
				
				if (i > 10) break;	// limit only first 10 paragraph.
			}while(true);
		}

		novel.setSynopsis(synopsis);
		Log.d(TAG, "Completed parsing synopsis.");
		return synopsis;
	}
}
