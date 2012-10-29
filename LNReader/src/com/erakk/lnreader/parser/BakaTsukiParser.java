/**
 * Parse baka-tsuki wiki page
 */
package com.erakk.lnreader.parser;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.util.Log;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.dao.NovelsDao;
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
	 * parse page info from Wiki API
	 * @param pageModel page name
	 * @param doc parsed page for given pageName
	 * @return PageModel status, no parent and type defined
	 */
	public static PageModel parsePageAPI(PageModel pageModel, Document doc) throws Exception {
//		pageModel.setTitle(doc.select("page").first().attr("title"));
//		//2012-08-03T02:41:50Z
//		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
//		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
//		String temp = doc.select("page").first().attr("touched");
//		Date lastUpdate = formatter.parse(temp);
//		pageModel.setLastUpdate(lastUpdate);
//		Log.d(TAG, "parsePageAPI "+ pageModel.getPage() + " Last Update: " + pageModel.getLastUpdate());
		ArrayList<PageModel> temp = new ArrayList<PageModel>();
		temp.add(pageModel);
		temp = parsePageAPI(temp, doc);
		return temp.get(0);				
	}
	
	/**
	 * parse pages info from Wiki API
	 * @param pageModels ArrayList of pages
	 * @param doc parsed page for given pages
	 * @return PageModel status, no parent and type defined
	 */
	public static ArrayList<PageModel> parsePageAPI(ArrayList<PageModel> pageModels, Document doc) throws Exception {
		Elements normalized = doc.select("n");
		Elements redirects = doc.select("r");
		//Log.d(TAG, "parsePageAPI redirected size: " + redirects.size());
		Elements pages = doc.select("page");
		//Log.d(TAG, "parsePageAPI pages size: " + pages.size());
		
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		for(int i = 0; i < pageModels.size(); ++i) {
			PageModel temp = pageModels.get(i);
			
			String to = URLDecoder.decode(temp.getPage(), "utf-8");
			//Log.d(TAG, "parsePageAPI source: " + to);
			
			// get normalized value for this page
			Elements nElements = normalized.select("n[from="+ to + "]");
			if(nElements != null && nElements.size() > 0){
				Element nElement = nElements.first();
				to = nElement.attr("to");
				//Log.d(TAG, "parsePageAPI normalized: " + to);
				
				// check redirects
				if(redirects != null && redirects.size() > 0 ) {
					Elements rElements = redirects.select("r[from="+ to + "]");
					if(rElements != null && rElements.size() > 0) {
						Element rElement = rElements.first();
						to = rElement.attr("to");
						temp.setRedirectedTo(to);
						Log.w(TAG, "parsePageAPI redirected: " + to);
					}
				}				
			}
			
			Element pElement = pages.select("page[title="+ to + "]").first();
			if(pElement == null) {
				Log.w(TAG, "parsePageAPI "+ temp.getPage() + ": No Info");
			}
			else if(!pElement.hasAttr("missing")) {
				// parse date
				String tempDate = pElement.attr("touched");
				Date lastUpdate = formatter.parse(tempDate);
				temp.setLastUpdate(lastUpdate);
				Log.i(TAG, "parsePageAPI "+ temp.getPage() + " Last Update: " + temp.getLastUpdate());
			}				
			else {
				Log.w(TAG, "parsePageAPI missing page info: " + to);
			}
		}		
		return pageModels;				
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
			//Log.d(TAG, "Found: #p-Light_Novels");
			
			Elements novels = stage.select("li");
			int order = 0;
			for (Iterator<Element> i = novels.iterator(); i .hasNext();) {
				Element novel = i.next();
				Element link = novel.select("a").first();
				PageModel page = new PageModel();
				String tempPage = link.attr("href").replace("/project/index.php?title=","")
						                           .replace(Constants.BASE_URL, "");
				page.setPage(tempPage);
				page.setType(PageModel.TYPE_NOVEL);
				page.setTitle(link.text());
				
				page.setLastUpdate(new Date(0)); // set to min value if never open
				try {
					//always get the page date
					PageModel temp = NovelsDao.getInstance().getPageModel(page, null);
					if(temp != null) {
						page.setLastUpdate(temp.getLastUpdate());
						page.setWatched(temp.isWatched());
						page.setFinishedRead(temp.isFinishedRead());
						page.setDownloaded(temp.isDownloaded());
					}
				} catch (Exception e) {
					Log.e(TAG, "Error when getting pageModel: " + page.getPage(), e);
				}				
				page.setLastCheck(new Date());
				page.setParent("Main_Page");
				page.setOrder(order);
				result.add(page);				
				//Log.d(TAG, "Add: "+ link.text());
				++order;
			}
		}
		return result;
	}	
	
	/**
	 * Parse novel Title, Synopsis, Cover, and Chapter list.
	 * @param doc
	 * @param page
	 * @return
	 */
	public static NovelCollectionModel ParseNovelDetails(Document doc, PageModel page) {
		NovelCollectionModel novel = new NovelCollectionModel();
		if(doc == null) throw new NullPointerException("Document cannot be null.");
		novel.setPage(page.getPage());
		novel.setPageModel(page);

		String redirected = redirectedFrom(doc, page);
		novel.setRedirectTo(redirected);
		
		parseNovelSynopsis(doc, novel);		
		parseNovelCover(doc, novel);				
		parseNovelChapters(doc, novel);
		
		return novel;
	}
	
	/**
	 * Check if the page is redirected. Return null if not.
	 * @param doc
	 * @param page
	 * @return
	 */
	private static String redirectedFrom(Document doc, PageModel page) {
// 		cannot use as the information is in head
//		Elements metaLink = doc.select("link");
//		if(metaLink != null && metaLink.size() > 0) {
//			for(Iterator<Element> i = metaLink.iterator(); i.hasNext();) {
//				Element link = i.next();
//				//Log.d(TAG,"Checking: " + link.html() + " : " + link.attr("rel"));
//				if(link.attributes().get("rel").contains("canonical")) {
//					String redir = link.attr("href").replace("/project/index.php?title=", "")
//							                        .replace(Constants.BASE_URL, "")
//							                        .trim();
//					Log.w(TAG, "Redirected from: " + page.getPage()+ " to: " + redir);
//					return redir;
//				}
//			}
//		}
		if(page.getRedirectedTo() != null) {
			try {
				return URLEncoder.encode(page.getRedirectedTo().replace(" ", "_"), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				Log.e(TAG, "Error when encoding redirected pages", e);
				return null;
			} 
		}
		return null;
	}
	
	/**
	 * Sanitizes a title by removing unnecessary stuff.
	 * @param title
	 * @return
	 */
	private static String sanitize(String title) {
		title = title.replaceAll("<.+?>", "")				 //Strip tags
					 .replaceAll("\\[.+?\\]", "")		 //Strip [___]s
					 .replaceAll("^(.+?)[(\\[].*$", "$1") //Leaves only the text before brackets (might be a bit too aggressive)
					 .trim();

		return title;
	}
	
	private static void parseNovelChapters(Document doc, NovelCollectionModel novel) {
		//Log.d(TAG, "Start parsing book collections for " + novel.getPage());
		// parse the collection
		ArrayList<BookModel> books = new ArrayList<BookModel>();
		try{
			Elements h2s = doc.select("h1,h2");
			for(Iterator<Element> i = h2s.iterator(); i.hasNext();){
				Element h2 = i.next();
				//Log.d(TAG, "checking h2: " +h2.text() + "\n" + h2.id());
				Elements spans = h2.select("span");
				if(spans.size() > 0) {
					// find span with id containing "_by" or 'Full_Text' 
					// or contains with Page Name or "Side_Stor*"
					// or contains "_Series" (Maru-MA)
					// or if redirected, use the redirect page name.
					boolean containsBy = false;
					for(Iterator<Element> iSpan = spans.iterator(); iSpan.hasNext(); ) {
						Element s = iSpan.next();
						//Log.d(TAG, "Checking: " + s.id());
						if(s.id().contains("_by") || 
						   s.id().contains("Full_Text") ||
						   s.id().contains("_Series") || 
						   s.id().contains("_series") ||
						   s.id().contains(novel.getPage()) ||
						   s.id().contains("Side_Stor") ||
						   (novel.getRedirectTo() != null && s.id().contains(novel.getRedirectTo())) ) {
							containsBy = true;
							break;
						}
					}
					if(!containsBy) continue;
					
					//Log.d(TAG, "Found h2: " +h2.text());
					ArrayList<BookModel> tempBooks = parseBooksMethod1(novel, h2);					
					if(tempBooks != null && tempBooks.size() > 0 ) 
					{
						books.addAll(tempBooks);
					}										
					if(books.size() == 0) {
						Log.d(TAG, "No books found, use method 2");
						tempBooks = parseBooksMethod2(novel, h2);
						if(tempBooks != null && tempBooks.size() > 0 ) 
						{
							books.addAll(tempBooks);
						}
					}
					if(books.size() == 0) {
						Log.d(TAG, "No books found, use method 3");
						tempBooks = parseBooksMethod3(novel, h2);
						if(tempBooks != null && tempBooks.size() > 0 ) 
						{
							books.addAll(tempBooks);
						}
					}
				}
			}			
		} catch(Exception e) {
			Log.e(TAG, "Unknown Exception : " + e.getMessage(), e);
		}
		//Log.d(TAG, "Complete parsing book collections: " + books.size());
		
		novel.setBookCollections(validateNovelBooks(books));
	}
	
	/***
	 * Look for <h3> after <h2> containing the volume list.
	 * Treat each li in dl/ul/div as the chapters. 
	 * @param novel
	 * @param h2
	 * @return
	 */
	private static ArrayList<BookModel> parseBooksMethod1(NovelCollectionModel novel, Element h2)
	{
		//Log.d(TAG, "method 1");
		ArrayList<BookModel> books = new ArrayList<BookModel>();
		Element bookElement = h2;
		boolean walkBook = true;
		int bookOrder = 0;
		do{
			bookElement = bookElement.nextElementSibling();
			if(bookElement == null || bookElement.tagName() == "h2") walkBook = false;
			else if(bookElement.tagName() != "h3") {
				Elements h3s = bookElement.select("h3");
				if( h3s != null && h3s.size() > 0 ) {
					for (Element h3 : h3s) {
						bookOrder = processH3(novel, books, h3, bookOrder);
					}					
				}						
			}
			else if(bookElement.tagName() == "h3") {
				bookOrder = processH3(novel, books, bookElement, bookOrder);
			}						
		}while(walkBook);
		return books;
	}

	public static int processH3(NovelCollectionModel novel, ArrayList<BookModel> books, Element bookElement, int bookOrder) {
		//Log.d(TAG, "Found: " +bookElement.text());
		BookModel book = new BookModel();
		book.setTitle(sanitize(bookElement.text()));
		book.setOrder(bookOrder);
		ArrayList<PageModel> chapterCollection = new ArrayList<PageModel>();
		
		// parse the chapters.
		boolean walkChapter = true;
		int chapterOrder = 0;
		Element chapterElement = bookElement;
		do{
			chapterElement = chapterElement.nextElementSibling();
			if(chapterElement == null || 
			   chapterElement.tagName() == "h2" || 
			   chapterElement.tagName() == "h3") {
				walkChapter = false;
			}
			else 
//				if(chapterElement.tagName() == "dl" ||
//					chapterElement.tagName() == "ul" ||
//					chapterElement.tagName() == "div") 
				{
				Elements chapters = chapterElement.select("li");
				for(Iterator<Element> i2 = chapters.iterator(); i2.hasNext();) {
					Element chapter = i2.next();
					if(chapter.tagName() != "li") break;
					Elements links = chapter.select("a");
					if(links.size() > 0) {
						Element link = links.first();
						PageModel p = new PageModel();
						p.setTitle(sanitize(link.text()));	// sanitize title
						String tempPage = link.attr("href").replace("/project/index.php?title=", "")
								                           .replace(Constants.BASE_URL, "");
						p.setPage(tempPage);
						p.setParent(novel.getPage() + Constants.NOVEL_BOOK_DIVIDER + book.getTitle());
						p.setType(PageModel.TYPE_CONTENT);
						p.setOrder(chapterOrder);
						p.setLastUpdate(new Date(0));
						//Log.d(TAG, "chapter: " + p.getTitle() + " = " + p.getPage());
						chapterCollection.add(p);
						++chapterOrder;
					}										
				}
			}
			book.setChapterCollection(chapterCollection);
		}while(walkChapter);
		books.add(book);
		++bookOrder;
		return bookOrder;
	}
		
	
	/***
	 * parse book method 2:
	 * Look for <p> after <h2> containing the chapter list, usually only have 1 book.
	 * See 7_Nights
	 * @param novel
	 * @param h2
	 * @return
	 */
	private static ArrayList<BookModel> parseBooksMethod2(NovelCollectionModel novel, Element h2){
		ArrayList<BookModel> books = new ArrayList<BookModel>();
		Element bookElement = h2;
		boolean walkBook = true;
		int bookOrder = 0;
		do{
			bookElement = bookElement.nextElementSibling();
			if(bookElement == null || bookElement.tagName() == "h2") walkBook = false;
			else if(bookElement.tagName() == "p") {
				//Log.d(TAG, "Found: " + bookElement.text());
				BookModel book = new BookModel();
				book.setTitle(sanitize(bookElement.text()));
				book.setOrder(bookOrder);
				ArrayList<PageModel> chapterCollection = new ArrayList<PageModel>();
				
				// parse the chapters.
				boolean walkChapter = true;
				int chapterOrder = 0;
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
								p.setTitle(sanitize(link.text()));
								String tempPage = link.attr("href").replace("/project/index.php?title=","")
										                           .replace(Constants.BASE_URL, "");
								p.setPage(tempPage);
								p.setParent(novel.getPage() + Constants.NOVEL_BOOK_DIVIDER + book.getTitle());
								p.setType(PageModel.TYPE_CONTENT);
								p.setOrder(chapterOrder);
								p.setLastUpdate(new Date(0));
								//Log.d(TAG, "chapter: " + p.getTitle() + " = " + p.getPage());
								chapterCollection.add(p);
								++chapterOrder;
							}										
						}
					}
					// no subchapter
					if(chapterCollection.size() == 0 ) {
						Elements links = bookElement.select("a");
						if(links.size() > 0) {
							Element link = links.first();
							PageModel p = new PageModel();
							p.setTitle(sanitize(link.text()));
							String tempPage = link.attr("href").replace("/project/index.php?title=","")
									                           .replace(Constants.BASE_URL, "");
							p.setPage(tempPage);
							p.setParent(novel.getPage() + Constants.NOVEL_BOOK_DIVIDER + book.getTitle());
							p.setType(PageModel.TYPE_CONTENT);
							p.setOrder(chapterOrder);
							p.setLastUpdate(new Date(0));
							//Log.d(TAG, "chapter: " + p.getTitle() + " = " + p.getPage());
							chapterCollection.add(p);
							++chapterOrder;
						}
					}
					book.setChapterCollection(chapterCollection);
				}while(walkChapter);
				books.add(book);
				++bookOrder;
			}							
		}while(walkBook);
		return books;
	}
	
	/***
	 * Only have 1 book, chapter list is nested in ul/dl, e.g:Fate/Apocrypha, Gekkou
	 * Parse the li as the chapters.
	 * @param novel
	 * @param h2
	 * @return
	 */
	private static ArrayList<BookModel> parseBooksMethod3(NovelCollectionModel novel, Element h2){
		ArrayList<BookModel> books = new ArrayList<BookModel>();
		Element bookElement = h2;
		boolean walkBook = true;
		int bookOrder = 0;
		do{
			bookElement = bookElement.nextElementSibling();
			if(bookElement == null || bookElement.tagName() == "h2") walkBook = false;
			else if(bookElement.tagName() == "ul" ||
					bookElement.tagName() == "dl") {
				//Log.d(TAG, "Found: " +bookElement.text());
				BookModel book = new BookModel();
				book.setTitle(sanitize(h2.text()));
				book.setOrder(bookOrder);
				ArrayList<PageModel> chapterCollection = new ArrayList<PageModel>();
				
				// parse the chapters.
				int chapterOrder = 0;
				Elements links = bookElement.select("li");
				for(Iterator<Element> i = links.iterator(); i.hasNext();) {
					Element link = i.next();
					PageModel p = new PageModel();
					p.setTitle(sanitize(link.text()));
					// get the url, usually the first one...
					Element as = link.select("a").first();
					String tempPage = as.attr("href").replace("/project/index.php?title=","")
							                         .replace(Constants.BASE_URL, "");
					p.setPage(tempPage);
					p.setParent(novel.getPage() + Constants.NOVEL_BOOK_DIVIDER + book.getTitle());
					p.setType(PageModel.TYPE_CONTENT);
					p.setOrder(chapterOrder);
					p.setLastUpdate(new Date(0));
					//Log.d(TAG, "chapter: " + p.getTitle() + " = " + p.getPage());
					chapterCollection.add(p);
					++chapterOrder;
				}
				book.setChapterCollection(chapterCollection);
				books.add(book);
				++bookOrder;
			}							
		}while(walkBook);
		return books;
	}
	
	private static ArrayList<BookModel> validateNovelBooks(ArrayList<BookModel> books) {
		ArrayList<BookModel> validatedBooks = new ArrayList<BookModel>();
		int bookOrder = 0;
		for(Iterator<BookModel> iBooks = books.iterator(); iBooks.hasNext();){
			BookModel book = iBooks.next();
			BookModel validatedBook = new BookModel();
			
			ArrayList<PageModel> validatedChapters = validateNovelChapters(book);
			
			// check if have any chapters
			if(validatedChapters.size() > 0) {
				validatedBook = book;
				validatedBook.setChapterCollection(validatedChapters);
				validatedBook.setOrder(bookOrder);
				validatedBooks.add(validatedBook);
				//Log.d("validateNovelBooks", "Adding: " + validatedBook.getTitle() + " order: " + validatedBook.getOrder());
				++bookOrder;
			}
		}
		return validatedBooks;
	}

	private static ArrayList<PageModel> validateNovelChapters(BookModel book) {
		ArrayList<PageModel> chapters = book.getChapterCollection();
		ArrayList<PageModel> validatedChapters = new ArrayList<PageModel>();
		int chapterOrder = 0;
		for(Iterator<PageModel> iChapter = chapters.iterator(); iChapter.hasNext();) {
			PageModel chapter = iChapter.next();
			
			if(!(chapter.getPage().contains("redlink=1") ||
			     chapter.getPage().contains("User:"))) {
				chapter.setOrder(chapterOrder);
				validatedChapters.add(chapter);
				//Log.d("validateNovelChapters", "Adding: " + chapter.getPage() + " order: " + chapter.getOrder());
				++chapterOrder;
			}
//			else{
//				Log.d("validateNovelChapters", "Removing: " + chapter.getPage());
//			}
		}
		return validatedChapters;
	}

	private static String parseNovelCover(Document doc, NovelCollectionModel novel) {
		//Log.d(TAG, "Start parsing cover image");
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
		if(imageUrl != null && imageUrl.length() > 0) {
			try {
				URL url = new URL(imageUrl);
				novel.setCoverUrl(url);
			} catch (MalformedURLException e) {
				Log.e(TAG, "Invalid URL: " + imageUrl, e);
			}
		}
		//Log.d(TAG, "Complete parsing cover image");
		return imageUrl;
	}

	private static String parseNovelSynopsis(Document doc, NovelCollectionModel novel) {
		//Log.d(TAG, "Start parsing synopsis");
		// parse the synopsis
		String synopsis = "";
		String source = "#Story_Synopsis";
		// from Story_Synopsis id
		Elements stage = doc.select(source);//.first().parent().nextElementSibling();
		// from main text
		if(stage == null || stage.size() <= 0) {
			source = "#mw-content-text,p";
			stage = doc.select(source);
			Log.i(TAG, "Synopsis From: " + source);
		}
		
		if(stage.size() > 0) {
			Element synopsisE;
			if(source == "#Story_Synopsis") synopsisE = stage.first().parent().nextElementSibling();
			else synopsisE = stage.first().children().first();
		
			boolean processOne = false;
			if(synopsisE == null || synopsisE.select("p").size() == 0) {
				// cannot found any synopsis, take the first available p
				synopsisE = stage.first();
				processOne = true;
			}
			
			int i = 0;
			do{
				if(synopsisE == null) break;
				if(synopsisE.tagName() != "p") {
					synopsisE = synopsisE.nextElementSibling();
					//Log.d(TAG, synopsisE.html());
					continue;
				}
				i++;
				synopsis += synopsisE.text() + "\n";
				synopsisE = synopsisE.nextElementSibling();
				if(synopsisE != null && synopsisE.tagName() != "p" && i > 0) break;
				
				if (i > 10) break;	// limit only first 10 paragraph.
				if(processOne) break;
			}while(true);
		}

		novel.setSynopsis(synopsis);
		//Log.d(TAG, "Completed parsing synopsis.");
		return synopsis;
	}

	public static NovelContentModel ParseNovelContent(Document doc, PageModel page) {
		NovelContentModel content = new NovelContentModel();
		page.setDownloaded(true);
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
				// shouldn't happened
				Log.e(TAG, "Invalid URL: " + urlStr, e);
			}
			images.add(image);
			//Log.d("ParseNovelContent", image.getName() + "==>" + image.getUrl().toString());
		}
		content.setImages(images);
		
		// clean up the text
		String cleanedText = text.replace("src=\"/project/images/", "src=\"file://" + Constants.IMAGE_ROOT + "/project/images/");
		//Log.d("Result", cleanedText);
		content.setContent(cleanedText);
		
		content.setLastXScroll(0);
		content.setLastYScroll(0);
		content.setLastZoom(Constants.DISPLAY_SCALE);
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
			// shouldn't happened
			Log.e(TAG, "Invalid URL: " + Constants.BASE_URL + imageUrl, e);
		}
		return image;
	}
	
	public static ArrayList<String> parseImagesFromContentPage(Document doc) {
		ArrayList<String> result = new ArrayList<String>();
		
		Elements links = doc.select("a");
		for (Element link : links) {
			String href = link.attr("href");
			if(href.contains("/project/index.php?title=File:")) {
				if(!href.startsWith("http")) href = Constants.BASE_URL + href;
				result.add(href);
			}
		}
		
		Log.d(TAG, "Images Found: " + result.size());
		return result;
	}
}
