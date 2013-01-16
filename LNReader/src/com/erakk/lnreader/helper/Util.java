package com.erakk.lnreader.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Date;

public class Util {

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

}
