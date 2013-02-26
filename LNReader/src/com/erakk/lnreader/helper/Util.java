package com.erakk.lnreader.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

public class Util {

	/**
	 * Show date/time difference in words.
	 * @param date
	 * @return
	 */
	public static String formatDateForDisplay(Date date) {
		String since= "";
		//Setup
		Date now = new Date();
		long dif = now.getTime() - date.getTime();
		dif = dif / 3600000; // convert from ms to hours
		if(dif<0) {
			since = "invalid";
		}
		else if(dif<24) {
			since = "hour";
		}
		else if (dif<168) {
			dif/=24;
			since = "day";
		}
		else if (dif<720) {
			dif/=168;
			since = "week";
		}
		else if (dif<8760) {
			dif/=720;
			since = "month";
		}
		else {
			dif/=8760;
			since = "year";
		}
		if (dif < 0) return since;
		else if (dif == 1) return dif + " " + since + " ago ";// + date.toLocaleString();
		else return dif + " " + since + "s ago ";// + date.toLocaleString();
	}
	
	/**
	 * Copy file
	 * @param src
	 * @param dst
	 * @throws IOException
	 */
	public static void copyFile(File src, File dst) throws IOException
	{
		FileChannel inChannel = null;
		FileChannel outChannel = null;
	    try
	    {
	    	inChannel = new FileInputStream(src).getChannel();
		    outChannel = new FileOutputStream(dst).getChannel();
	        inChannel.transferTo(0, inChannel.size(), outChannel);
	    }
	    finally
	    {
	        if (inChannel != null)
	            inChannel.close();
	        if (outChannel != null)
	            outChannel.close();
	    }
	}

	/**
	 * http://stackoverflow.com/questions/6350158/check-arraylist-for-instance-of-object
	 * @param arrayList
	 * @param clazz
	 * @return
	 */
	public static boolean isInstanceOf(Collection<?> arrayList, Class<?> clazz)
	{
	    for(Object o : arrayList)
	    {
	        if (o != null && o.getClass() == clazz)
	        {
	            return true;
	        }
	    }
	    return false;
	}
	
	/**
	 * Join collection with given separator into string.
	 * @param s
	 * @param delimiter
	 * @return
	 */
	public static String join(Collection<?> s, String delimiter) {
	     StringBuilder builder = new StringBuilder();
	     Iterator<?> iter = s.iterator();
	     while (iter.hasNext()) {
	         builder.append(iter.next());
	         if (!iter.hasNext()) {
	           break;                  
	         }
	         builder.append(delimiter);
	     }
	     return builder.toString();
	 }
	

	public static String UrlEncode(String param) throws UnsupportedEncodingException {
		if(!param.contains("%")) {
			param = URLEncoder.encode(param, "utf-8");
		}
		return param;
	}
	
	public static boolean isStringNullOrEmpty(String input) {
		if(input == null || input.length() == 0) return true;
		return false;
	}
	
	/**
	 * Remove | \ ? * < " : > + [ ] / ' from filename
	 * @param filename
	 * @return
	 */
	public static String sanitizeFilename(String filename) {
		return filename.replaceAll("[\\|\\\\?*<\\\":>+\\[\\]']", "_");		
	}
	
	public static int tryParseInt(String input, int def) {
		try{
			return Integer.parseInt(input);
		}
		catch(NumberFormatException ex) {
			return def;
		}
	}
}
