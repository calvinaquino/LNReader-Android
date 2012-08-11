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
	
	public static NovelCollectionModel ParseNovelDetails(Document doc) {
		NovelCollectionModel novel = new NovelCollectionModel();
		if(doc == null) throw new NullPointerException("Document cannot be null.");
				
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
				
		// parse the collection
		
		return novel;
	}
}
