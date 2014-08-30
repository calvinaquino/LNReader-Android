/**
 * Parse baka-tsuki wiki page
 */
package com.erakk.lnreader.parser;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.util.Log;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.helper.BakaReaderException;
import com.erakk.lnreader.helper.Util;
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

	private static final String TAG = BakaTsukiParser.class.toString();

	/***
	 * Parse "#mw-pages" English Novels.
	 * 
	 * @param doc
	 * @param parent
	 * @param status
	 * @return
	 */
	public static ArrayList<PageModel> parseGenericNovelList(Document doc, String parent, String status) {
		ArrayList<PageModel> result = new ArrayList<PageModel>();

		if (doc == null)
			throw new NullPointerException("Document cannot be null.");

		Element stage = doc.select("#mw-pages").first();
		int order = 0;
		if (stage != null) {
			Elements list = stage.select("li");
			for (Element element : list) {
				Element link = element.select("a").first();
				PageModel page = new PageModel();
				page.setParent(parent);
				String tempPage = CommonParser.normalizeInternalUrl(link.attr("href"));
				page.setPage(tempPage);
				page.setLanguage(Constants.LANG_ENGLISH);
				page.setType(PageModel.TYPE_NOVEL);
				page.setTitle(link.text());
				page.setStatus(status);
				page.setOrder(order);

				result.add(page);
				++order;
			}
		}

		return result;
	}

	/**
	 * Parse novel Title, Synopsis, Cover, and Chapter list.
	 * 
	 * @param doc
	 * @param page
	 * @return
	 */
	public static NovelCollectionModel ParseNovelDetails(Document doc, PageModel page) {
		NovelCollectionModel novel = new NovelCollectionModel();
		if (doc == null)
			throw new NullPointerException("Document cannot be null.");
		novel.setPage(page.getPage());
		novel.setPageModel(page);

		String redirected = CommonParser.redirectedFrom(doc, page);
		novel.setRedirectTo(redirected);

		parseNovelSynopsis(doc, novel);
		parseNovelCover(doc, novel);
		parseNovelChapters(doc, novel);

		parseNovelStatus(doc, page);

		return novel;
	}

	public static NovelContentModel ParseNovelContent(Document doc, PageModel page) throws Exception {
		NovelContentModel content = new NovelContentModel();
		page.setDownloaded(true);
		content.setPage(page.getPage());
		content.setPageModel(page);

		Element textElement = doc.select("text").first();
		String text = "";
		if (textElement != null) {
			text = textElement.text();
		} else {
			textElement = doc.select(".noarticletext, error[code=missingtitle]").first();
			if (textElement != null) {
				if (!Util.isStringNullOrEmpty(textElement.attr("info")))
					text = textElement.attr("info");
				else
					text = textElement.html();
				page.setMissing(true);
				Log.e(TAG, "Chapter is missing/deleted: " + page.getPage());
			}
			else {
				Log.d(TAG, "Content: \r\n" + doc.html());
				throw new BakaReaderException("Empty Content", BakaReaderException.EMPTY_CONTENT);
			}
		}

		// get valid image list
		Document imgDoc = Jsoup.parse(text);
		ArrayList<ImageModel> images = CommonParser.getAllImagesFromContent(imgDoc, UIHelper.getBaseUrl(LNReaderApplication.getInstance().getApplicationContext()));
		content.setImages(images);
		content.setContent(CommonParser.replaceImagePath(text));

		content.setLastXScroll(0);
		content.setLastYScroll(0);
		content.setLastZoom(Constants.DISPLAY_SCALE);
		return content;
	}

	private static PageModel parseNovelStatus(Document doc, PageModel page) {
		boolean isTeaser = page.isTeaser();
		boolean isStalled = page.isStalled();
		boolean isAbandoned = page.isAbandoned();
		boolean isPending = page.isPending();

		// Template:STALLED
		Elements links = doc.select("a[title=Template:STALLED]");
		if (links != null && links.size() > 0) {
			isStalled = true;
			Log.i(TAG, "Novel is stalled: " + page.getPage());
		} else
			isStalled = false;

		// Template:Abandoned
		links = doc.select("a[title=Template:Abandoned]");
		if (links != null && links.size() > 0) {
			isAbandoned = true;
			Log.i(TAG, "Novel is abandoned: " + page.getPage());
		} else
			isAbandoned = false;

		// Template:Warning:ATP
		links = doc.select("a[title=Template:Warning:ATP]");
		if (links != null && links.size() > 0) {
			isPending = true;
			Log.i(TAG, "Novel is pending authorization: " + page.getPage());
		} else
			isPending = false;

		// Teaser => parent = Category:Teasers
		if (page.getParent().equalsIgnoreCase(Constants.ROOT_TEASER)) {
			isTeaser = true;
			Log.i(TAG, "Novel is Teaser Project: " + page.getPage());
		} else
			isTeaser = false;

		// update the status
		ArrayList<String> statuses = new ArrayList<String>();
		if (isTeaser)
			statuses.add(Constants.STATUS_TEASER);
		if (isStalled)
			statuses.add(Constants.STATUS_STALLED);
		if (isAbandoned)
			statuses.add(Constants.STATUS_ABANDONED);
		if (isPending)
			statuses.add(Constants.STATUS_PENDING);

		page.setStatus(Util.join(statuses, "|"));

		return page;
	}

	/***
	 * find span with id containing "_by" or 'Full_Text'
	 * or contains with Page Name or "Side_Stor*" or "Short_Stor*"
	 * or Official_Parody_Stories
	 * or contains "_Series" (Maru-MA)
	 * or if redirected, use the redirect page name.
	 * 
	 * @param s
	 * @param novel
	 * @param language
	 * @return
	 */
	private static boolean validateH2(Element s, NovelCollectionModel novel, String language) {
		if (language.equalsIgnoreCase(Constants.LANG_ENGLISH)) {
			String rules[] = { novel.getPage(), novel.getRedirectTo(), "_by", "Full_Text", "_Series", "_series", "Side_Stor", "Short_Stor", "Parody_Stor" };
			for (String rule : rules) {
				if (!Util.isStringNullOrEmpty(rule) && s.id().contains(rule)) {
					return true;
				}
			}
		}

		return false;
	}

	private static void parseNovelChapters(Document doc, NovelCollectionModel novel) {
		// Log.d(TAG, "Start parsing book collections for " + novel.getPage());
		// parse the collection
		ArrayList<BookModel> books = new ArrayList<BookModel>();
		boolean oneBookOnly = false;
		try {
			Elements h2s = doc.select("h1,h2");
			for (Iterator<Element> i = h2s.iterator(); i.hasNext();) {
				Element h2 = i.next();
				// Log.d(TAG, "checking h2: " +h2.text() + "\n" + h2.id());
				Elements spans = h2.select("span");
				if (spans.size() > 0) {
					boolean containsBy = false;
					for (Element s : spans) {
						Log.d(TAG, "Checking: " + s.id());
						if (validateH2(s, novel, Constants.LANG_ENGLISH)) {
							containsBy = true;
							Log.d(TAG, "Got valid id: " + s.id());
							break;
						}
						Log.d(TAG, "Not valid id: " + s.id());
					}
					if (!containsBy) {
						continue;
					}

					// Log.d(TAG, "Found h2: " +h2.text());
					ArrayList<BookModel> tempBooks = parseBooksMethod1(novel, h2);
					if (tempBooks != null && tempBooks.size() > 0) {
						books.addAll(tempBooks);
					}
					if (books.size() == 0 || (oneBookOnly && tempBooks.size() == 0)) {
						Log.d(TAG, "No books found, use method 2: Only have 1 book, chapter in <p> tag.");
						tempBooks = parseBooksMethod2(novel, h2);
						if (tempBooks != null && tempBooks.size() > 0) {
							oneBookOnly = true;
							books.addAll(tempBooks);
						}
					}
					if (books.size() == 0 || (oneBookOnly && tempBooks.size() == 0)) {
						Log.d(TAG, "No books found, use method 3: Only have 1 book.");
						tempBooks = parseBooksMethod3(novel, h2);
						if (tempBooks != null && tempBooks.size() > 0) {
							oneBookOnly = true;
							books.addAll(tempBooks);
						}
					}
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "Unknown Exception for " + novel.getPage() + ": " + e.getMessage(), e);
		}
		// Log.d(TAG, "Complete parsing book collections: " + books.size());

		novel.setBookCollections(CommonParser.validateNovelBooks(books));
	}

	/***
	 * Look for <h3>after
	 * <h2>containing the volume list. Treat each li in dl/ul/div as the chapters.
	 * 
	 * @param novel
	 * @param h2
	 * @return
	 */
	private static ArrayList<BookModel> parseBooksMethod1(NovelCollectionModel novel, Element h2) {
		// Log.d(TAG, "method 1");
		ArrayList<BookModel> books = new ArrayList<BookModel>();
		Element bookElement = h2;
		boolean walkBook = true;
		int bookOrder = 0;
		do {
			bookElement = bookElement.nextElementSibling();
			if (bookElement == null || bookElement.tagName() == "h2")
				walkBook = false;
			else if (bookElement.tagName() != "h3") {
				Elements h3s = bookElement.select("h3");
				if (h3s != null && h3s.size() > 0) {
					for (Element h3 : h3s) {
						bookOrder = CommonParser.processH3(novel, books, h3, bookOrder, Constants.LANG_ENGLISH);
					}
				}
			} else if (bookElement.tagName() == "h3") {
				bookOrder = CommonParser.processH3(novel, books, bookElement, bookOrder, Constants.LANG_ENGLISH);
			}
		} while (walkBook);
		return books;
	}

	/***
	 * parse book method 2:
	 * Look for &lt;p&gt; after &lt;h2&gt; containing the chapter list, usually only have 1 book.
	 * See 7_Nights
	 * 
	 * @param novel
	 * @param h2
	 * @return
	 */
	private static ArrayList<BookModel> parseBooksMethod2(NovelCollectionModel novel, Element h2) {
		ArrayList<BookModel> books = new ArrayList<BookModel>();
		Element bookElement = h2;
		boolean walkBook = true;
		int bookOrder = 0;
		do {
			bookElement = bookElement.nextElementSibling();
			if (bookElement == null || bookElement.tagName() == "h2")
				walkBook = false;
			else if (bookElement.tagName() == "p") {
				// Log.d(TAG, "Found: " + bookElement.text());
				BookModel book = new BookModel();

				if (bookElement.html().contains("href"))
					// title contains link
					book.setTitle(CommonParser.sanitize(bookElement.text(), true));
				else
					book.setTitle(CommonParser.sanitize(bookElement.text(), false));
				book.setOrder(bookOrder);
				ArrayList<PageModel> chapterCollection = new ArrayList<PageModel>();
				String parent = novel.getPage() + Constants.NOVEL_BOOK_DIVIDER + book.getTitle();

				// parse the chapters.
				boolean walkChapter = true;
				int chapterOrder = 0;
				Element chapterElement = bookElement;
				do {
					chapterElement = chapterElement.nextElementSibling();
					if (chapterElement == null)
						walkChapter = false;
					else if (chapterElement.tagName() == "p")
						walkChapter = false;
					else if (chapterElement.tagName() == "dl" || chapterElement.tagName() == "ul" || chapterElement.tagName() == "div") {
						Elements chapters = chapterElement.select("li");
						for (Element chapter : chapters) {
							PageModel p = CommonParser.processLI(chapter, parent, chapterOrder, Constants.LANG_ENGLISH);
							if (p != null) {
								chapterCollection.add(p);
								++chapterOrder;
							}
						}
					}
					// no subchapter
					if (chapterCollection.size() == 0) {
						Elements links = bookElement.select("a");
						if (links.size() > 0) {
							Element link = links.first();
							PageModel p = CommonParser.processA(link.text(), parent, chapterOrder, link, Constants.LANG_ENGLISH);
							// Log.d(TAG, "chapter: " + p.getTitle() + " = " + p.getPage());
							if (p != null) {
								chapterCollection.add(p);
								++chapterOrder;
							}
						}
					}
					book.setChapterCollection(chapterCollection);
				} while (walkChapter);
				books.add(book);
				++bookOrder;
			}
		} while (walkBook);
		return books;
	}

	/***
	 * Only have 1 book, chapter list is nested in ul/dl, e.g:Fate/Apocrypha, Gekkou
	 * Parse the li as the chapters.
	 * 
	 * @param novel
	 * @param h2
	 * @return
	 */
	private static ArrayList<BookModel> parseBooksMethod3(NovelCollectionModel novel, Element h2) {
		ArrayList<BookModel> books = new ArrayList<BookModel>();
		Element bookElement = h2;
		boolean walkBook = true;
		int bookOrder = 0;
		do {
			bookElement = bookElement.nextElementSibling();
			if (bookElement == null || bookElement.tagName() == "h2")
				walkBook = false;
			else if (bookElement.tagName() == "ul" || bookElement.tagName() == "dl") {
				// Log.d(TAG, "Found: " +bookElement.text());
				BookModel book = new BookModel();
				if (h2.html().contains("href"))
					// title contains link
					book.setTitle(CommonParser.sanitize(h2.text(), true));
				else
					book.setTitle(CommonParser.sanitize(h2.text(), false));
				book.setOrder(bookOrder);
				ArrayList<PageModel> chapterCollection = new ArrayList<PageModel>();
				String parent = novel.getPage() + Constants.NOVEL_BOOK_DIVIDER + book.getTitle();

				// parse the chapters.
				int chapterOrder = 0;
				Elements chapters = bookElement.select("li");
				for (Element chapter : chapters) {
					PageModel p = CommonParser.processLI(chapter, parent, chapterOrder, Constants.LANG_ENGLISH);
					if (p != null) {
						chapterCollection.add(p);
						++chapterOrder;
					}
				}
				book.setChapterCollection(chapterCollection);
				books.add(book);
				++bookOrder;
			}
		} while (walkBook);
		return books;
	}

	private static void parseNovelCover(Document doc, NovelCollectionModel novel) {
		// Log.d(TAG, "Start parsing cover image");
		// parse the cover image
		String imageUrl = "";
		Elements images = doc.select(".thumbimage");

		if (images.size() > 0) {
			imageUrl = images.first().attr("src");
			if (!imageUrl.startsWith("http")) {
				imageUrl = "http://www.baka-tsuki.org" + imageUrl;
			}
			Log.d(TAG, "Cover: " + imageUrl);
		}
		if (imageUrl != null && imageUrl.length() > 0) {
			try {
				URL url = new URL(imageUrl);
				novel.setCoverUrl(url);
				novel.setCover(url.toString());
			} catch (MalformedURLException e) {
				Log.e(TAG, "Invalid URL: " + imageUrl, e);
			}
		}
		else {
			novel.setCoverUrl(null);
			novel.setCover("");
		}
		// Log.d(TAG, "Complete parsing cover image");
	}

	private static String parseNovelSynopsis(Document doc, NovelCollectionModel novel) {
		// Log.d(TAG, "Start parsing synopsis");
		// parse the synopsis
		String synopsis = "";
		String source = "[id*=Synopsis]";// "#Story_Synopsis";
		// from Story_Synopsis id
		Elements stage = doc.select(source);// .first().parent().nextElementSibling();
		// from main text
		if (stage == null || stage.size() <= 0) {
			source = "#mw-content-text,p";
			stage = doc.select(source);
			Log.i(TAG, "Synopsis From: " + source);
		}

		if (stage.size() > 0) {
			Element synopsisE;
			if (source == "[id*=Synopsis]")// "#Story_Synopsis")
				synopsisE = stage.first().parent().nextElementSibling();
			else
				synopsisE = stage.first().children().first();

			boolean processOne = false;
			if (synopsisE == null || synopsisE.select("p").size() == 0) {
				// cannot found any synopsis, take the first available p
				synopsisE = stage.first();
				processOne = true;
			}

			int i = 0;
			do {
				if (synopsisE == null)
					break;
				if (synopsisE.tagName() != "p") {
					synopsisE = synopsisE.nextElementSibling();
					// Log.d(TAG, synopsisE.html());
					continue;
				}
				i++;
				synopsis += synopsisE.text() + "\n";
				synopsisE = synopsisE.nextElementSibling();
				if (synopsisE != null && synopsisE.tagName() != "p" && i > 0)
					break;

				if (i > 10)
					break; // limit only first 10 paragraph.
				if (processOne)
					break;
			} while (true);
		}

		novel.setSynopsis(synopsis);
		// Log.d(TAG, "Completed parsing synopsis.");
		return synopsis;
	}
}
