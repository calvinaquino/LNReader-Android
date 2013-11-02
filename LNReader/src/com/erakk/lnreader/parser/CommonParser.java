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
import java.util.Locale;
import java.util.TimeZone;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.preference.PreferenceManager;
import android.util.Log;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.helper.Util;
import com.erakk.lnreader.model.BookModel;
import com.erakk.lnreader.model.ImageModel;
import com.erakk.lnreader.model.PageModel;

public class CommonParser {

	private static final String TAG = CommonParser.class.toString();

	/**
	 * Set Up image path
	 * 
	 * @param content
	 * @return
	 */
	public static String replaceImagePath(String content) {
		String imagePath = "src=\"file://" + UIHelper.getImageRoot(LNReaderApplication.getInstance().getApplicationContext()) + "/project/images/";
		content = content.replace("src=\"/project/images/", imagePath);
		return content;
	}

	/**
	 * Get all img element and update the src from /project/ to rootImagePath/project/
	 * @param doc
	 * @param rootImagePath
	 * @return
	 */
	public static ArrayList<ImageModel> getAllImagesFromContent(Document doc, String rootImagePath) {
		Elements imageElements = doc.select("img");
		ArrayList<ImageModel> images = new ArrayList<ImageModel>();
		for (Element imageElement : imageElements) {
			ImageModel image = new ImageModel();
			String urlStr = imageElement.attr("src").replace("/project/", rootImagePath + "/project/");
			//imageElement.attr("src", urlStr);
			String name = urlStr.substring(urlStr.lastIndexOf("/"));
			image.setName(name);
			try {
				image.setUrl(new URL(urlStr));
			} catch (MalformedURLException e) {
				// shouldn't happened
				Log.e(TAG, "Invalid URL: " + urlStr, e);
			}
			images.add(image);
			// Log.d("ParseNovelContent", image.getName() + "==>" + image.getUrl().toString());
		}
		return images;
	}

	/**
	 * Sanitizes a title by removing unnecessary stuff.
	 * 
	 * @param title
	 * @return
	 */
	public static String sanitize(String title, boolean isAggresive) {
		Log.d(TAG, "Before: " + title);
		title = title.replaceAll("<.+?>", "") // Strip tags
				.replaceAll("\\[.+?\\]", "") // Strip [___]s
				.replaceAll("- PDF", "").replaceAll("\\(PDF\\)", "") // Strip (PDF)
				// Strip - (Full Text)
				.replaceAll("- (Full Text)", "").replaceAll("- \\(.*Full Text.*\\)", "").replace("\\(.*Full Text.*\\)", "");
		Log.d(TAG, "After: " + title);
		if (isAggresive) {
			if (PreferenceManager.getDefaultSharedPreferences(LNReaderApplication.getInstance().getApplicationContext()).getBoolean(Constants.PREF_AGGRESIVE_TITLE_CLEAN_UP, true)) {
				// Leaves only the text before brackets (might be a bit too aggressive)
				title = title.replaceAll("^(.+?)[(\\[].*$", "$1");
				Log.d(TAG, "After Aggresive: " + title);
			}
		}
		return title.trim();
	}

	/**
	 * Remove redlink, user, and ISBN page
	 * 
	 * @param book
	 * @return
	 */
	public static ArrayList<PageModel> validateNovelChapters(BookModel book) {
		ArrayList<PageModel> chapters = book.getChapterCollection();
		ArrayList<PageModel> validatedChapters = new ArrayList<PageModel>();
		int chapterOrder = 0;
		for (Iterator<PageModel> iChapter = chapters.iterator(); iChapter.hasNext();) {
			PageModel chapter = iChapter.next();
			
			// redlink=1 means chapter is missing, commented out to include missing chapters
			if (!(//chapter.getPage().contains("redlink=1") || // missing page
					chapter.getPage().contains("User:") || // user page
					chapter.getPage().contains("Special:BookSources") // ISBN handler
					)) {
				chapter.setOrder(chapterOrder);
				validatedChapters.add(chapter);
				++chapterOrder;
			}
		}
		return validatedChapters;
	}

	/**
	 * Remove invalid chapter from volumes
	 * 
	 * @param books
	 * @return
	 */
	public static ArrayList<BookModel> validateNovelBooks(ArrayList<BookModel> books) {
		ArrayList<BookModel> validatedBooks = new ArrayList<BookModel>();
		int bookOrder = 0;
		for (Iterator<BookModel> iBooks = books.iterator(); iBooks.hasNext();) {
			BookModel book = iBooks.next();
			BookModel validatedBook = new BookModel();

			ArrayList<PageModel> validatedChapters = validateNovelChapters(book);

			// check if have any chapters
			if (validatedChapters.size() > 0) {
				validatedBook = book;
				validatedBook.setChapterCollection(validatedChapters);
				validatedBook.setOrder(bookOrder);
				validatedBooks.add(validatedBook);
				// Log.d("validateNovelBooks", "Adding: " + validatedBook.getTitle() + " order: " +
				// validatedBook.getOrder());
				++bookOrder;
			}
		}
		return validatedBooks;
	}

	/**
	 * Check if the page is redirected. Return null if not.
	 * 
	 * @param doc
	 * @param page
	 * @return
	 */
	public static String redirectedFrom(Document doc, PageModel page) {
		if (page.getRedirectedTo() != null) {
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
	 * parse page info from Wiki API
	 * 
	 * @param pageModel
	 *            page name
	 * @param doc
	 *            parsed page for given pageName
	 * @return PageModel status, no parent and type defined
	 */
	public static PageModel parsePageAPI(PageModel pageModel, Document doc, String url) throws Exception {
		ArrayList<PageModel> temp = new ArrayList<PageModel>();
		temp.add(pageModel);
		temp = parsePageAPI(temp, doc, url);
		return temp.get(0);
	}

	/**
	 * parse pages info from Wiki API
	 * 
	 * @param pageModels
	 *            ArrayList of pages
	 * @param doc
	 *            parsed page for given pages
	 * @return PageModel status, no parent and type defined
	 */
	public static ArrayList<PageModel> parsePageAPI(ArrayList<PageModel> pageModels, Document doc, String url) throws Exception {
		Elements normalized = doc.select("n");
		Elements redirects = doc.select("r");
		// Log.d(TAG, "parsePageAPI redirected size: " + redirects.size());
		Elements pages = doc.select("page");
		Log.d(TAG, "parsePageAPI pages size: " + pages.size());

		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));

		for (int i = 0; i < pageModels.size(); ++i) {
			PageModel temp = pageModels.get(i);

			String to = URLDecoder.decode(temp.getPage(), "utf-8");
			Log.d(TAG, "parsePageAPI source: " + to);
			if (Util.isStringNullOrEmpty(to)) {
				Log.e(TAG, "Empty source detected for url: " + url);
				continue;
			}

			// get normalized value for this page
			Elements nElements = normalized.select("n[from=" + to + "]");
			if (nElements != null && nElements.size() > 0) {
				Element nElement = nElements.first();
				to = nElement.attr("to");
				Log.d(TAG, "parsePageAPI normalized: " + to);
				if (Util.isStringNullOrEmpty(to)) {
					Log.e(TAG, "Empty normalized source detected for url: " + url);
					continue;
				}
			}

			// check redirects
			if (redirects != null && redirects.size() > 0) {
				Elements rElements = redirects.select("r[from=" + to + "]");
				if (rElements != null && rElements.size() > 0) {
					Element rElement = rElements.first();
					to = rElement.attr("to");
					temp.setRedirectedTo(to);
					Log.i(TAG, "parsePageAPI redirected: " + to);
					if (Util.isStringNullOrEmpty(to)) {
						Log.e(TAG, "Empty redirected source detected for url: " + url);
						continue;
					}
				}
			}

			Element pElement = pages.select("page[title=" + to + "]").first();
			if (pElement == null) {
				Log.w(TAG, "parsePageAPI " + temp.getPage() + ": No Info, please check the url: " + url);
			} else if (!pElement.hasAttr("missing")) {
				// parse date
				String tempDate = pElement.attr("touched");
				if (!Util.isStringNullOrEmpty(tempDate)) {
					Date lastUpdate = formatter.parse(tempDate);
					temp.setLastUpdate(lastUpdate);
					temp.setMissing(false);
					Log.i(TAG, "parsePageAPI " + temp.getPage() + " Last Update: " + temp.getLastUpdate());
				} else {
					Log.w(TAG, "parsePageAPI " + temp.getPage() + " No Last Update Information!");
				}
			} else {
				temp.setMissing(true);
				Log.w(TAG, "parsePageAPI missing page info: " + to);
			}
		}
		return pageModels;
	}

	/**
	 * Get the url for the big image http://www.baka-tsuki.org/project/index.php?title=File:xxx
	 * @param imageUrl
	 * @return
	 */
	public static String getImageFilePageFromImageUrl(String imageUrl) {
		String pageUrl = "";
		// http://www.baka-tsuki.org/project/images/4/4a/Bakemonogatari_Up.png
		// http://www.baka-tsuki.org/project/images/thumb/4/4a/Bakemonogatari_Up.png/200px-Bakemonogatari_Up.png
		// http://www.baka-tsuki.org/project/index.php?title=File:Bakemonogatari_Up.png
		String[] tokens = imageUrl.split("/");
		if (imageUrl.contains("/thumb/")) {
			// from thumbnail
			pageUrl = tokens[8];
		} else {
			// from full page
			pageUrl = tokens[7];
		}
		pageUrl = UIHelper.getBaseUrl(LNReaderApplication.getInstance()) + "/project/index.php?title=File:" + pageUrl;
		return pageUrl;
	}

	/**
	 * Get the image model from /project/index.php?title=File:xxx
	 * @param doc
	 * @return
	 */
	public static ImageModel parseImagePage(Document doc) {
		ImageModel image = new ImageModel();

		Element mainContent = doc.select("#mw-content-text").first();
		Element fullMedia = mainContent.select(".fullMedia").first();
		String imageUrl = fullMedia.select("a").first().attr("href");

		try {
			image.setUrl(new URL(UIHelper.getBaseUrl(LNReaderApplication.getInstance().getApplicationContext()) + imageUrl));
		} catch (MalformedURLException e) {
			// shouldn't happened
			Log.e(TAG, "Invalid URL: " + UIHelper.getBaseUrl(LNReaderApplication.getInstance().getApplicationContext()) + imageUrl, e);
		}
		return image;
	}

	/**
	 * Get all /project/index.php?title=File:xxx from content
	 * @param doc
	 * @return
	 */
	public static ArrayList<String> parseImagesFromContentPage(Document doc) {
		ArrayList<String> result = new ArrayList<String>();

		Elements links = doc.select("a");
		for (Element link : links) {
			String href = link.attr("href");
			if (href.contains("/project/index.php?title=File:")) {
				if (!href.startsWith("http"))
					href = UIHelper.getBaseUrl(LNReaderApplication.getInstance().getApplicationContext()) + href;
				result.add(href);
			}
		}

		Log.d(TAG, "Images Found: " + result.size());
		return result;
	}
}
