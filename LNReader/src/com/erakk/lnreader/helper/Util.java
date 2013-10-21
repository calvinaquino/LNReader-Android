package com.erakk.lnreader.helper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import android.util.Log;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.callback.CallbackEventData;
import com.erakk.lnreader.callback.ICallbackNotifier;

public class Util {

	private static final String TAG = Util.class.toString();

	/**
	 * Show date/time difference in words.
	 * 
	 * @param date
	 * @return
	 */
	public static String formatDateForDisplay(Date date) {
		String since = "";
		// Setup
		Date now = new Date();
		long dif = now.getTime() - date.getTime();
		dif = dif / 3600000; // convert from ms to hours
		if (dif < 0) {
			since = "invalid";
		} else if (dif < 24) {
			since = "hour";
		} else if (dif < 168) {
			dif /= 24;
			since = "day";
		} else if (dif < 720) {
			dif /= 168;
			since = "week";
		} else if (dif < 8760) {
			dif /= 720;
			since = "month";
		} else {
			dif /= 8760;
			since = "year";
		}
		if (dif < 0)
			return since;
		else if (dif == 1)
			return dif + " " + since + " ago ";// + date.toLocaleString();
		else
			return dif + " " + since + "s ago ";// + date.toLocaleString();
	}

	/**
	 * Copy file
	 * 
	 * @param src
	 * @param dst
	 * @throws IOException
	 */
	public static void copyFile(File src, File dst) throws IOException {
		FileChannel inChannel = null;
		FileChannel outChannel = null;
		try {
			inChannel = new FileInputStream(src).getChannel();
			outChannel = new FileOutputStream(dst).getChannel();
			inChannel.transferTo(0, inChannel.size(), outChannel);
		} finally {
			if (inChannel != null)
				inChannel.close();
			if (outChannel != null)
				outChannel.close();
		}
	}

	/**
	 * http://stackoverflow.com/questions/6350158/check-arraylist-for-instance-of-object
	 * 
	 * @param arrayList
	 * @param clazz
	 * @return
	 */
	public static boolean isInstanceOf(Collection<?> arrayList, Class<?> clazz) {
		for (Object o : arrayList) {
			if (o != null && o.getClass() == clazz) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Join collection with given separator into string.
	 * 
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
		if (!param.contains("%")) {
			param = URLEncoder.encode(param, "utf-8");
		}
		return param;
	}

	public static boolean isStringNullOrEmpty(String input) {
		if (input == null || input.length() == 0)
			return true;
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
		try {
			return Integer.parseInt(input);
		} catch (NumberFormatException ex) {
			return def;
		}
	}

	public static List<File> getListFiles(File parentDir, ICallbackNotifier callback) {
		ArrayList<File> inFiles = new ArrayList<File>();
		File[] files = parentDir.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				String message = "Getting files from: " + file.getAbsolutePath();
				Log.d(TAG, message);
				// if(callback != null)
				// callback.onCallback(new CallbackEventData(message));
				inFiles.addAll(getListFiles(file, callback));
			} else {
				inFiles.add(file);
			}
		}
		return inFiles;
	}

	public static void zipFiles(List<File> filenames, String zipFile, String replacedRootPath, ICallbackNotifier callback) throws IOException {
		BufferedInputStream origin = null;
		FileOutputStream dest = new FileOutputStream(zipFile);
		ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
		byte data[] = new byte[Constants.BUFFER];
		int fileCount = 1;
		int total = filenames.size();
		Log.d(TAG, "Start zipping to: " + zipFile);
		for (File file : filenames) {
			String absPath = file.getAbsolutePath();
			String message = String.format("Zipping %s [%s of %s]", absPath, fileCount, total);
			Log.d(TAG, message);
			FileInputStream fi = new FileInputStream(file);
			origin = new BufferedInputStream(fi, Constants.BUFFER);
			ZipEntry entry = new ZipEntry(absPath.replace(replacedRootPath, ""));
			out.putNextEntry(entry);
			int count;
			while ((count = origin.read(data, 0, Constants.BUFFER)) != -1) {
				out.write(data, 0, count);
			}
			origin.close();
			++fileCount;
		}
		out.close();
		Log.d(TAG, "Completed zipping to: " + zipFile);
	}

	public static void unzipFiles(String zipName, String rootPath, ICallbackNotifier callback) throws FileNotFoundException, IOException {
		InputStream is = new FileInputStream(zipName);
		ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));
		ZipEntry ze;
		byte[] buffer = new byte[Constants.BUFFER];
		int count;

		Log.d(TAG, "Start unzipping: " + zipName + " to: " + rootPath);
		// create root path
		File root = new File(rootPath);
		root.mkdirs();

		int fileCount = 1;
		while ((ze = zis.getNextEntry()) != null) {
			String filename = rootPath + ze.getName();

			// Need to create directories if not exists, or
			// it will generate an Exception...
			if (ze.isDirectory()) {
				Log.d(TAG, "Creating dir1: " + filename);
				File fmd = new File(filename);
				fmd.mkdirs();
				continue;
			}
			// check if target dir exist
			String dir = filename.substring(0, filename.lastIndexOf("/"));
			File rootDir = new File(dir);
			if (!rootDir.exists()) {
				Log.d(TAG, "Creating dir2: " + dir);
				rootDir.mkdirs();
			}

			Log.d(TAG, "Unzipping: " + filename);
			if (callback != null)
				callback.onCallback(new CallbackEventData("Unzipping #" + fileCount + ": " + filename));
			FileOutputStream fout = new FileOutputStream(filename);

			while ((count = zis.read(buffer)) != -1) {
				fout.write(buffer, 0, count);
			}

			fout.close();
			zis.closeEntry();
			++fileCount;
		}

		zis.close();
		Log.d(TAG, "Completed unzipping to: " + zipName);
	}
}
