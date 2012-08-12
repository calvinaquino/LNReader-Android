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
				
		parseNovelSynopsis(doc, novel);		
		parseNovelCover(doc, novel);
				
		// parse the collection
		ArrayList<BookModel> books = new ArrayList<BookModel>();
		try{
			Elements h2s = doc.select("h2");
			for(Iterator<Element> i = h2s.iterator(); i.hasNext();){
				Element h2 = i.next();
				Elements span = h2.select("span");
				if(span.size() > 0 && span.first().id().contains("_by_")) {
					Log.d(TAG, "h2: " +h2.text());
					
					// parse book
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
				}
			}			
			novel.setBookCollections(books);
		} catch(Exception e) {e.printStackTrace();}
		return novel;
	}

	private static void parseNovelCover(Document doc, NovelCollectionModel novel) {
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
			novel.setCoverUrl(new URL(imageUrl));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Log.d(TAG, novel.getCover());
	}

	private static void parseNovelSynopsis(Document doc,
			NovelCollectionModel novel) {
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
		Log.d(TAG, novel.getSynopsis());
	}
}
