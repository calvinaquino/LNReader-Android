/**
 * Parse baka-tsuki wiki page
 */
package com.erakk.lnreader.parser;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.util.Log;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.model.BookModel;
import com.erakk.lnreader.model.ImageModel;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.NovelContentModel;
import com.erakk.lnreader.model.PageModel;

/**
 * @author Nandaka
 *
 */
public class BakaTsukiParser {

	
	private static final String TAG = "Parser";

	/**
	 * @param pageName page name
	 * @param doc parsed page for given pageName
	 * @return PageModel status, no parent and type defined
	 */
	public static PageModel parsePageAPI(String pageName, Document doc) throws Exception {
		PageModel pageModel = new PageModel();
		pageModel.setPage(pageName);
		pageModel.setTitle(doc.select("page").first().attr("title"));
		Log.d(TAG, "parsePageAPI Title: " + pageModel.getTitle());
		
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); 		//2012-08-03T02:41:50Z
		Date lastUpdate = formatter.parse(doc.select("page").first().attr("touched"));
		pageModel.setLastUpdate(lastUpdate);
		Log.d(TAG, "parsePageAPI Last Update: " + pageModel.getLastUpdate());
		
		//pageModel.setType(PageModel.TYPE_OTHER);
		return pageModel;				
	}
	
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
	
	
	/**
	 * @param doc
	 * @param page
	 * @return
	 */
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
										if(chapter.tagName() != "li") break;
										Elements links = chapter.select("a");
										if(links.size() > 0) {
											Element link = links.first();
											PageModel p = new PageModel();
											p.setTitle(link.text());
											p.setPage(link.attr("href").replace("/project/index.php?title=",""));
											p.setParent(novel.getPage() + "%" + book.getTitle());
											p.setType(PageModel.TYPE_CONTENT);
											Log.d(TAG, "chapter: " + p.getTitle() + " = " + p.getPage());
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
									else if(chapterElement.tagName() == "dl" ||
											chapterElement.tagName() == "ul" ||
											chapterElement.tagName() == "div") {
										Elements chapters = chapterElement.select("li");
										for(Iterator<Element> i2 = chapters.iterator(); i2.hasNext();) {
											Element chapter = i2.next();
											if(chapter.tagName() != "li") break;
											Elements links = chapter.select("a");
											if(links.size() > 0) {
												Element link = links.first();
												PageModel p = new PageModel();
												p.setTitle(link.text());
												p.setPage(link.attr("href").replace("/project/index.php?title=",""));
												p.setParent(novel.getPage() + "%" + book.getTitle());
												p.setType(PageModel.TYPE_CONTENT);
												Log.d(TAG, "chapter: " + p.getTitle() + " = " + p.getPage());
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
											p.setPage(link.attr("href").replace("/project/index.php?title=",""));
											p.setParent(novel.getPage() + "%" + book.getTitle());
											p.setType(PageModel.TYPE_CONTENT);
											Log.d(TAG, "chapter: " + p.getTitle() + " = " + p.getPage());
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
		
		books = validateNovelChapters(books);
		return books;
	}
	
	private static ArrayList<BookModel> validateNovelChapters(ArrayList<BookModel> books) {
		ArrayList<BookModel> validatedBooks = new ArrayList<BookModel>();
		for(Iterator<BookModel> iBooks = books.iterator(); iBooks.hasNext();){
			BookModel book = iBooks.next();
			BookModel validatedBook = new BookModel();
			
			ArrayList<PageModel> chapters = book.getChapterCollection();
			ArrayList<PageModel> validatedChapters = new ArrayList<PageModel>(); 
			for(Iterator<PageModel> iChapter = chapters.iterator(); iChapter.hasNext();) {
				PageModel chapter = iChapter.next();
				if(!chapter.getPage().contains("redlink=1") &&
				   !chapter.getPage().contains("title=User:")) {
					validatedChapters.add(chapter);
				}
			}
			
			// check if have any chapters
			if(validatedChapters.size() > 0) {
				validatedBook = book;
				validatedBook.setChapterCollection(validatedChapters);
				validatedBooks.add(validatedBook);
			}
		}
		return validatedBooks;
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

	public static NovelContentModel ParseNovelContent(Document doc, PageModel page) {
		NovelContentModel content = new NovelContentModel();
		content.setPage(page.getPage());
		content.setPageModel(page);
		
		Element textElement = doc.select("text").first();
		String text = textElement.text();
		
		// get valid image list
		// Elements imageElements = doc.select("img");
		Document imgDoc = Jsoup.parse(text);
		Elements imageElements = imgDoc.select("img");
		ArrayList<ImageModel> images = new ArrayList<ImageModel>();
		for(Iterator<Element> i = imageElements.iterator(); i.hasNext();) {
			ImageModel image = new ImageModel();
			Element imageElement = i.next();
			String urlStr = imageElement.attr("src").replace("/project/", Constants.BASE_URL + "/project/");
			String name = urlStr.substring(urlStr.lastIndexOf("/"));
			image.setName(name);
			try {
				image.setUrl(new URL(urlStr));
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			images.add(image);
			Log.d("ParseNovelContent", image.getName() + "==>" + image.getUrl().toString());
		}
		content.setImages(images);
		
		// clean up the text
		String cleanedText = text.replace("src=\"/project/images/", "src=\"file://" + Constants.IMAGE_ROOT + "/project/images/");
		Log.d("Result", cleanedText);
		content.setContent(cleanedText);
		
		content.setLastXScroll(0);
		content.setLastYScroll(0);
		content.setLastZoom(1);
		return content;
	}
	
	public static ImageModel parseImagePage(Document doc){
		ImageModel image = new ImageModel();
		
		Element mainContent = doc.select("#mw-content-text").first();
		Element fullMedia = mainContent.select(".fullMedia").first();
		String imageUrl = fullMedia.select("a").first().attr("href");
		
		try {
			image.setUrl(new URL(Constants.BASE_URL + imageUrl));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return image;
	}
}
