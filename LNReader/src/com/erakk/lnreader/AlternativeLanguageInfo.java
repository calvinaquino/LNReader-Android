package com.erakk.lnreader;

import java.util.ArrayList;
import java.util.Hashtable;

import org.xmlpull.v1.XmlPullParser;

import android.content.res.XmlResourceParser;
import android.util.Log;

import com.erakk.lnreader.helper.Util;

/*
 * Author : freedomofkeima
 */

public class AlternativeLanguageInfo {

	/* List of attributes */
	private static final String TAG = AlternativeLanguageInfo.class.toString();
	private String language;
	private String category;
	private String markerSynopsis; /* Marker for Synopsis */
	private ArrayList<String> parserInfo;

	private static Hashtable<String, AlternativeLanguageInfo> instance;
	private static Object lock = new Object();

	/* List of methods */
	private static void initHashMap() {
		synchronized (lock) {
			// construct HashTable and populate with proper data with language as the key
			if (instance == null)
				instance = new Hashtable<String, AlternativeLanguageInfo>();

			try {
				XmlResourceParser xpp = LNReaderApplication.getInstance().getResources().getXml(R.xml.parse_lang_info);
				xpp.next();
				int eventType = xpp.getEventType();
				String _language = null;
				String _category = null;
				String _markerSynopsis = null;
				ArrayList<String> _parserInfo = new ArrayList<String>();
				boolean startLang = false, startCat = false, startMark = false, startRule = false;
				while (eventType != XmlPullParser.END_DOCUMENT)
				{
					if (eventType == XmlPullParser.START_TAG)
					{
						if (xpp.getName().equalsIgnoreCase("Language")) {
							startLang = true;
						}
						else if (xpp.getName().equalsIgnoreCase("Category")) {
							startCat = true;
						}
						else if (xpp.getName().equalsIgnoreCase("MarkerSynopsis")) {
							startMark = true;
						}
						else if (xpp.getName().equalsIgnoreCase("Rule")) {
							startRule = true;
						}
					}
					else if (eventType == XmlPullParser.END_TAG)
					{
						if (xpp.getName().equalsIgnoreCase("LanguageInfo")) {
							AlternativeLanguageInfo temp = new AlternativeLanguageInfo(_language, _category, _markerSynopsis, _parserInfo);
							instance.put(_language, temp);
							Log.d(TAG, "Language added: " + temp.toString());
							_language = null;
							_category = null;
							_markerSynopsis = null;
							_parserInfo = new ArrayList<String>();
						}

					}
					else if (eventType == XmlPullParser.TEXT)
					{
						if (startLang) {
							_language = xpp.getText();
							startLang = false;
						}
						else if (startCat) {
							_category = xpp.getText();
							startCat = false;
						}
						else if (startMark) {
							_markerSynopsis = xpp.getText();
							startMark = false;
						}
						else if (startRule) {
							_parserInfo.add(xpp.getText());
							startRule = false;
						}
					}
					eventType = xpp.next();
				}
			} catch (Exception ex) {
				Log.e(TAG, ex.getMessage(), ex);
			}

		}
	}

	public static Hashtable<String, AlternativeLanguageInfo> getAlternativeLanguageInfo() {
		synchronized (lock) {
			/* if instance is null, then initHashMap */
			if (instance == null || instance.isEmpty())
				initHashMap();
			return instance;
		}
	}

	private AlternativeLanguageInfo(String _language, String _category, String _markerSynopsis, ArrayList<String> _parserInfo) {
		// set the member variables
		language = _language;
		category = _category;
		markerSynopsis = _markerSynopsis;
		parserInfo = _parserInfo;
	}

	@Override
	public String toString() {
		return String.format("language = %s, category = %s, markerSynopsis = %s, parserInfo = {%s}", language, category, markerSynopsis, Util.join(parserInfo, ","));
	}

	/* Setter & Getter */
	public String getLanguage() {
		return language;
	}

	public String getCategory() {
		return category;
	}

	public String getCategoryInfo() {
		return "Category:" + category;
	}

	public String getMarkerSynopsis() {
		return markerSynopsis;
	}

	public ArrayList<String> getParserInfo() {
		return parserInfo;
	}

	public void setLanguage(String _language) {
		language = _language;
	}

	public void setCategory(String _category) {
		category = _category;
	}

	public void setMarkerSynopsis(String _markerSynopsis) {
		markerSynopsis = _markerSynopsis;
	}

	public void setParserInfo(ArrayList<String> _parserInfo) {
		parserInfo = _parserInfo;
	}

}