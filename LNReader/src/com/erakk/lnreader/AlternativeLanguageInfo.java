package com.erakk.lnreader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

import android.util.Log;

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

	private static Hashtable<String, AlternativeLanguageInfo> instance = null; //new Hashtable<String, AlternativeLanguageInfo> ();
	private static Object lock = new Object();

	/* List of methods */
	public static void initHashMap() {
		synchronized (lock) {
			// construct HashTable and populate with proper data with language as the key
			if(instance == null) instance = new Hashtable<String, AlternativeLanguageInfo> ();

			/* In future, this information could be stored in XML file */
			/* French Language */
			instance.put(Constants.LANG_FRENCH , new AlternativeLanguageInfo(
					Constants.LANG_FRENCH, "French", "#Synopsis",
					new ArrayList<String>(Arrays.asList("_par", "Texte_Intégral", "Full_Text", "Série_", "série_", "Tome_", "tome_", "Histoire_", "histoire_",  "Histoires_", "histoires_", "Side_Stor", "Short_Stor", "Material"))));
		    Log.d(TAG,"French Language added");

		    /* Indonesian Language */
			instance.put(Constants.LANG_BAHASA_INDONESIA , new AlternativeLanguageInfo(
					Constants.LANG_BAHASA_INDONESIA, "Indonesian", "#Sinopsis_Cerita",
					new ArrayList<String>(Arrays.asList("_oleh", "Full_Text", "Serial_", "serial_", "Seri_", "seri_", "Cerita_Tambah", "Cerita_Singkat", "Cerita_Pendek", "Side_Stor", "Short_Stor"))));
		    Log.d(TAG,"Bahasa Indonesia Language added");
		}
	}

	public static Hashtable<String, AlternativeLanguageInfo> getAlternativeLanguageInfo() {
		synchronized (lock) {
			/* if instance is null, then initSingleton */
		   if(instance == null || instance.isEmpty()) initHashMap();
		   return instance;
		}
	}

	public AlternativeLanguageInfo(String _language, String _category, String _markerSynopsis, ArrayList<String> _parserInfo) {
	  // set the member variables
	  language = _language;
	  category = _category;
	  markerSynopsis = _markerSynopsis;
	  parserInfo = _parserInfo;
	}

	/* Setter & Getter */
	public String getLanguage(){
		return language;
	}

	public String getCategory(){
		return category;
	}

	public String getCategoryInfo(){
		return "Category:" + category;
	}

	public String getMarkerSynopsis(){
		return markerSynopsis;
	}

	public ArrayList<String> getParserInfo(){
		return parserInfo;
	}

	public void setLanguage(String _language){
		language = _language;
	}

	public void setCategory(String _category){
		category = _category;
	}

	public void setMarkerSynopsis(String _markerSynopsis){
		markerSynopsis = _markerSynopsis;
	}

	public void setParserInfo(ArrayList<String> _parserInfo){
		parserInfo = _parserInfo;
	}

}