/**
 * Parse baka-tsuki wiki page
 */
package com.erakk.lnreader.parser;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.util.Log;

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
	
	public static NovelCollectionModel ParseNovelDetails(Document Doc) {
		NovelCollectionModel novel = new NovelCollectionModel();
		
		// parse the synopsis
		
		// parse the cover image
		
		// parse the collection
		
		return novel;
	}
}
