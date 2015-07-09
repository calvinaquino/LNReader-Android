package com.erakk.lnreader.parser;

import android.preference.PreferenceManager;
import android.util.Log;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.Util;
import com.erakk.lnreader.model.BookModel;
import com.erakk.lnreader.model.ImageModel;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.PageModel;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class CommonParser {

    private static final String TAG = CommonParser.class.toString();

    /**
     * Set Up image path
     *
     * @param content
     * @return
     */
    public static String replaceImagePath(String content) {
        String root = UIHelper.getImageRoot(LNReaderApplication.getInstance().getApplicationContext());

        // standard image
        String imagePath = "src=\"file://" + root + "/project/images/";
        content = content.replace("src=\"/project/images/", imagePath);

        // /project/thumb.php?f=Biblia1_011.png&width=300
        // thumb.ph]
        String thumbPath = "src=\"file://" + root + "/project/thumb.php_";
        content = content.replace("src=\"/project/thumb.php?", thumbPath);

        return content;
    }

    /**
     * Get all img element
     *
     * @param doc
     * @return
     */
    public static ArrayList<ImageModel> processImagesFromContent(Document doc) {
        String baseUrl = UIHelper.getBaseUrl(LNReaderApplication.getInstance().getApplicationContext());
        Elements imageElements = doc.select("img");
        ArrayList<ImageModel> images = new ArrayList<ImageModel>();

        for (Element imageElement : imageElements) {
            ImageModel image = new ImageModel();
            String urlStr = imageElement.attr("src").replace("/project/", baseUrl + "/project/");
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
        Log.v(TAG, "Before: " + title);
        title = title.replaceAll("<.+?>", "") // Strip tags
                .replaceAll("\\[.+?\\]", "") // Strip [___]s
                .replaceAll("- PDF", "").replaceAll("\\(PDF\\)", "") // Strip (PDF)
                        // Strip - (Full Text)
                .replaceAll("- (Full Text)", "").replaceAll("- \\(.*Full Text.*\\)", "").replace("\\(.*Full Text.*\\)", "");
        Log.v(TAG, "After: " + title);
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
        for (PageModel chapter : chapters) {

            if (chapter.getPage().contains("User:") // user page
                    || chapter.getPage().contains("Special:BookSources")// ISBN handler
                // || chapter.getPage().contains("redlink=1") // missing page
                    ) {
                Log.d(TAG, "Skipping: " + chapter.getPage());
                continue;
            } else {
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
        for (BookModel book : books) {
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
     * @param pageModel page name
     * @param doc       parsed page for given pageName
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
     * @param pageModels ArrayList of pages
     * @param doc        parsed page for given pages
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
            Log.v(TAG, "parsePageAPI source: " + to);
            if (Util.isStringNullOrEmpty(to)) {
                Log.e(TAG, "Empty source detected for url: " + url);
                continue;
            }

            // get normalized value for this page
            Elements nElements = normalized.select("n[from=" + to + "]");
            if (nElements != null && nElements.size() > 0) {
                Element nElement = nElements.first();
                to = nElement.attr("to");
                Log.v(TAG, "parsePageAPI normalized: " + to);
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
                    Log.w(TAG, "parsePageAPI redirected: " + to);
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
                // parse date, default use touched attr, if rev not available
                String tempDate = pElement.attr("touched");
                Element rev = pElement.select("rev").first();
                if (rev != null) {
                    tempDate = rev.attr("timestamp");
                    Log.v(TAG, "Using timestamp from revision");
                }

                int wikiId = -1;
                try {
                    wikiId = Integer.parseInt(pElement.attr("pageid"));
                } catch (NumberFormatException nex) {
                    Log.e(TAG, String.format("Invalid pageid: '%s' for %s", pElement.attr("pageid"), temp.getPage()));
                }

                if (!Util.isStringNullOrEmpty(tempDate)) {
                    Date lastUpdate = formatter.parse(tempDate);
                    temp.setLastUpdate(lastUpdate);
                    temp.setMissing(false);
                    temp.setWikiId(wikiId);
                    if (Util.isStringNullOrEmpty(temp.getTitle()))
                        temp.setTitle(to);
                    Log.d(TAG, String.format("parsePageAPI [%s]%s Last Update: %s ", temp.getPage(), temp.getWikiId(), temp.getLastUpdate()));
                } else {
                    Log.w(TAG, "parsePageAPI " + temp.getPage() + " No Last Update Information!");
                }
            } else {
                temp.setMissing(true);
                Log.w(TAG, "parsePageAPI missing page info: " + to);
            }
            if (temp.getPage().contains("redlink=1")) {
                temp.setMissing(true);
            }
        }
        return pageModels;
    }

    /**
     * Get the url for the big image http://www.baka-tsuki.org/project/index.php?title=File:xxx
     *
     * @param imageUrl
     * @return
     */
    public static String getImageFilePageFromImageUrl(String imageUrl) {
        String pageUrl = "";
        // http://www.baka-tsuki.org/project/images/4/4a/Bakemonogatari_Up.png
        // http://www.baka-tsuki.org/project/images/thumb/4/4a/Bakemonogatari_Up.png/200px-Bakemonogatari_Up.png
        // http://www.baka-tsuki.org/project/index.php?title=File:Bakemonogatari_Up.png
        // http://www.baka-tsuki.org/project/thumb.php?f=KNT_V01_NewCover.jpg&width=250
        String[] tokens = imageUrl.split("/");
        if (imageUrl.contains("/thumb/")) {
            // from thumbnail
            pageUrl = tokens[8];
        } else if (imageUrl.contains("/thumb.php?")) {
            String[] temp = imageUrl.split("f=");
            temp = temp[1].split("&");
            pageUrl = temp[0];
        } else {
            // from full page
            pageUrl = tokens[7];
        }
        pageUrl = UIHelper.getBaseUrl(LNReaderApplication.getInstance()) + "/project/index.php?title=File:" + pageUrl;
        return pageUrl;
    }

    /**
     * Get the image model from /project/index.php?title=File:xxx
     *
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
     *
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
                if (!result.contains(href))
                    result.add(href);
            }
        }

        Log.d(TAG, "Images Found: " + result.size());
        return result;
    }

    /**
     * Process &lt;a&gt; to chapter
     *
     * @param title
     * @param parent
     * @param chapterOrder
     * @param link
     * @param language
     * @return
     */
    public static PageModel processA(String title, String parent, int chapterOrder, Element link, String language) {
        String href = link.attr("href");

        // handle redlink
        if (!UIHelper.getUpdateIncludeRedlink(LNReaderApplication.getInstance().getApplicationContext()) && href.contains("&redlink=1")) {
            return null;
        }

        PageModel p = new PageModel();
        p.setTitle(CommonParser.sanitize(title, false));
        p.setParent(parent);
        p.setType(PageModel.TYPE_CONTENT);
        p.setOrder(chapterOrder);
        p.setLastUpdate(new Date(0));
        p.setLanguage(language);

        // External link
        if (link.className().contains("external text")) {
            p.setExternal(true);
            p.setPage(Util.SanitizeBaseUrl(href, false));
            // Log.d(TAG, "Found external link for " + p.getTitle() + ": " + link.attr("href"));
        } else {
            p.setExternal(false);
            String tempPage = normalizeInternalUrl(href);
            p.setPage(tempPage);
        }
        return p;
    }

    /**
     * Process li to chapters.
     *
     * @param li
     * @param parent
     * @param chapterOrder
     * @return
     */
    public static ArrayList<PageModel> processLI(Element li, String parent, int chapterOrder, String language) {
        ArrayList<PageModel> pageModels = new ArrayList<>();

        Elements links = li.select("a");
        if (links != null && links.size() > 0) {
            for (Element link : links) {
                // skip if User_talk:
                if (link.attr("href").contains("User_talk:")) {
                    continue;
                }

                // if parent of the link is li element, use only the link text
                String linkText = link.text();
                if (link.parent() != li)
                    linkText = li.text();

                PageModel p = processA(linkText, parent, chapterOrder, link, language);
                if (p != null)
                    pageModels.add(p);
            }
        }
        return pageModels;
    }

    /**
     * Get the volume name and parse the chapter list.
     *
     * @param novel
     * @param books
     * @param bookElement
     * @param bookOrder
     * @return
     */
    public static int processH3(NovelCollectionModel novel, ArrayList<BookModel> books, Element bookElement, int bookOrder, String language) {
        // Log.d(TAG, "Found: " +bookElement.text());
        BookModel book = new BookModel();
        if (bookElement.html().contains("href")) {
            book.setTitle(CommonParser.sanitize(bookElement.text(), true));
        } else {
            book.setTitle(CommonParser.sanitize(bookElement.text(), false));
        }

        String parent = novel.getPage() + Constants.NOVEL_BOOK_DIVIDER + book.getTitle();
        book.setOrder(bookOrder);

        ArrayList<PageModel> chapterCollection = parseChapters(novel, bookElement, language, parent);

        if (chapterCollection.size() == 0) {
            Elements bookLinks = bookElement.select("a");
            if (bookLinks != null) {
                for (Element a : bookLinks) {
                    Log.e(TAG, "Got linked Volume without chapter list: " + a.text() + " => " + a.attr("href"));
                    if (a.attr("href").startsWith(Constants.ROOT_URL) || a.attr("href").startsWith(UIHelper.getBaseUrl(LNReaderApplication.getInstance()))) {
                        PageModel p = processA(a.text(), parent, 0, a, language);
                        if (p != null) {
                            Log.i(TAG, "Added chapter list: " + a.text() + " => " + a.attr("href"));
                            chapterCollection.add(p);
                            break;
                        }
                    }
                }
            }
        }
        book.setChapterCollection(chapterCollection);

        books.add(book);
        ++bookOrder;
        return bookOrder;
    }

    /**
     * Parse chapter from element containing li element.
     *
     * @param novel
     * @param bookElement
     * @param language
     * @param parent
     * @return
     */
    public static ArrayList<PageModel> parseChapters(NovelCollectionModel novel, Element bookElement, String language, String parent) {
        ArrayList<PageModel> chapterCollection = new ArrayList<PageModel>();

        // parse the chapters.
        boolean walkChapter = true;
        int chapterOrder = 0;
        Element chapterElement = bookElement;
        do {
            chapterElement = chapterElement.nextElementSibling();
            if (chapterElement == null
                    || chapterElement.tagName() == "h2"
                    || chapterElement.tagName() == "h3"
                    || chapterElement.tagName() == "h4") {
                walkChapter = false;
            } else {
                Elements chapters = chapterElement.select("li");
                for (Element chapter : chapters) {
                    ArrayList<PageModel> pageModels = processLI(chapter, parent, chapterOrder, language);

                    for (PageModel p : pageModels) {
                        if (p != null) {
                            chapterCollection.add(p);
                            ++chapterOrder;
                        }
                    }
                }
            }
        } while (walkChapter);
        return chapterCollection;
    }

    /**
     * Remove http(s)://www.baka-tsuki.org/project/index.php?title=
     *
     * @param url
     * @return
     */
    public static String normalizeInternalUrl(String url) {
        return url.replace("/project/index.php?title=", "").replace(Constants.ROOT_HTTPS, "").replace(Constants.ROOT_HTTP, "").replace(Constants.ROOT_URL, "");
    }


    /**
     * Parse novel cover from the first element of img with css class .thumbimage
     *
     * @param doc
     * @param novel
     * @return
     */
    public static String parseNovelCover(Document doc, NovelCollectionModel novel) {
        String imageUrl = "";
        Elements images = doc.select(".thumbimage");
        if (images.size() > 0) {
            imageUrl = images.first().attr("src");
            if (!imageUrl.startsWith("http")) {
                imageUrl = "http://www.baka-tsuki.org" + imageUrl;
            }

            // http://www.baka-tsuki.org/project/images/thumb/f/f5/Daimaou_v01_cover.jpg/294px-Daimaou_v01_cover.jpg
            if (UIHelper.isUseBigCover(LNReaderApplication.getInstance())) {
                if (imageUrl.contains("/thumb/")) {
                    imageUrl = imageUrl.replace("/thumb/", "/");
                    imageUrl = imageUrl.substring(0, imageUrl.lastIndexOf("/"));
                } else if (imageUrl.contains(".php?")) {
                    // http://www.baka-tsuki.org/project/thumb.php?f=KNT_V01_NewCover.jpg&width=250
                    // http://www.baka-tsuki.org/project/index.php?title=File:Znt_novel_cover.jpg
                    // need to check the original file
                    String filePage = getImageFilePageFromImageUrl(imageUrl);
                    ImageModel image = new ImageModel();
                    image.setName(filePage);
                    try {
                        image = NovelsDao.getInstance().getImageModelFromInternet(image, null);
                        imageUrl = image.getUrl().toString();
                    } catch (Exception ex) {
                        Log.e(TAG, "Failed parsing big cover: " + filePage, ex);
                    }
                }
            }

            Log.d(TAG, "Cover: " + imageUrl);
        }
        novel.setCover(imageUrl);

        if (imageUrl != null && imageUrl.length() > 0) {
            try {
                URL url = new URL(imageUrl);
                novel.setCoverUrl(url);
            } catch (MalformedURLException e) {
                Log.e(TAG, "Invalid URL: " + imageUrl, e);
            }
        }
        // Log.d(TAG, "Complete parsing cover image");
        return imageUrl;
    }

}
