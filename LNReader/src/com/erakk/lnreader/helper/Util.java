package com.erakk.lnreader.helper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.channels.FileChannel;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.StatFs;
import android.util.Log;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.R;
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
	public static String formatDateForDisplay(Context context, Date date) {
		// validate input
		if(date == null)
			return "No Date available";

		// Setup
		Date now = new Date();
		long dif = now.getTime() - date.getTime();
		if (dif < 0) return "Unknown";

		dif /= 1000; // convert from milliseconds to seconds
		if (dif < 60)
			return context.getResources().getString(R.string.timespan_seconds); // <1 minute ago

		dif /= 60; // convert from seconds to minutes
		if (dif < 60)
			return context.getResources().getQuantityString(R.plurals.timespan_minutes, (int) dif, (int) dif);

		dif /= 60; // convert from minutes to hours
		if (dif < 24)
			return context.getResources().getQuantityString(R.plurals.timespan_hours, (int) dif, (int) dif);

		dif /= 24; // convert from hours to days
		if (dif < 7)
			return context.getResources().getQuantityString(R.plurals.timespan_days, (int) dif, (int) dif);

		dif /= 7; // convert from days to weeks
		if (dif < 30)
			return context.getResources().getQuantityString(R.plurals.timespan_weeks, (int) dif, (int) dif);

		dif /= 30; // convert from weeks to months
		if (dif < 12)
			return context.getResources().getQuantityString(R.plurals.timespan_months, (int) dif, (int) dif);

		dif /= 12; // convert from months to years
		return context.getResources().getQuantityString(R.plurals.timespan_years, (int) dif, (int) dif);
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
			if (!dst.exists()) {
				Log.w(TAG, "Destination File doesn't exists, try to create dummy file");
				boolean result = dst.createNewFile();
				if (!result) Log.e(TAG, "Failed ot create file");
			}
			outChannel = new FileOutputStream(dst).getChannel();
			inChannel.transferTo(0, inChannel.size(), outChannel);
		} finally {
			if (inChannel != null) inChannel.close();
			if (outChannel != null) outChannel.close();
		}
	}

	/**
	 * http://stackoverflow.com/questions/6350158/check-arraylist-for-instance-
	 * of-object
	 * 
	 * @param arrayList
	 * @param clazz
	 * @return
	 */
	public static boolean isInstanceOf(Collection<?> arrayList, Class<?> clazz) {
		for (Object o : arrayList) {
			if (o != null && o.getClass() == clazz) { return true; }
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
		if (input == null || input.length() == 0) return true;
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

	public static List<File> getListFiles(File parentDir, Long[] totalSize, ICallbackNotifier callback) {
		ArrayList<File> inFiles = new ArrayList<File>();
		File[] files = parentDir.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				inFiles.addAll(getListFiles(file, totalSize, callback));
			} else {
				inFiles.add(file);
				totalSize[0] += file.length();
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
			String message = LNReaderApplication.getInstance().getApplicationContext().getResources()
					.getString(R.string.zip_files_task_progress_count, fileCount, total, absPath);
			Log.d(TAG, message);
			if (callback != null) callback.onProgressCallback(new CallbackEventData(message, "Util.zipFiles()"));
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
			if (callback != null) {
				String message = LNReaderApplication.getInstance().getApplicationContext().getResources()
						.getString(R.string.unzip_files_task_progress_count, fileCount, filename);
				callback.onProgressCallback(new CallbackEventData(message, "Util.unzipFiles()"));
			}
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

	public static String calculateCRC32(String input) {
		CRC32 crc = new CRC32();
		crc.reset();
		crc.update(input.getBytes());
		return Long.toHexString(crc.getValue());
	}

	/**
	 * http://stackoverflow.com/a/3758880
	 * 
	 * @param bytes
	 * @param si
	 * @return
	 */
	public static String humanReadableByteCount(long bytes, boolean si) {
		int unit = si ? 1000 : 1024;
		if (bytes < unit) return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
		return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}

	@SuppressLint("NewApi")
	public static long getFreeSpace(File path) {
		long availableSpace = -1L;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
			availableSpace = path.getFreeSpace();
		}

		if(availableSpace <= 0) {
			try {
				StatFs stat = new StatFs(path.getPath());
				stat.restat(path.getPath());

				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
					availableSpace =  stat.getAvailableBytes();
				else
					availableSpace = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();
			} catch (Exception e) {
				Log.e(TAG, "Failed to get free space.", e);
			}
		}
		return availableSpace;

	}

	public static String convertStreamToString(InputStream is) throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) {
			sb.append(line).append("\n");
		}
		reader.close();
		return sb.toString();
	}

	public static String getStringFromFile(String filePath) throws Exception {
		File fl = new File(filePath);
		FileInputStream fin = new FileInputStream(fl);
		String ret = convertStreamToString(fin);
		// Make sure you close all streams.
		fin.close();
		return ret;
	}


	/***
	 * http://stackoverflow.com/a/7410956
	 */
	public final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
		@Override
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	};

	public static SSLSocketFactory initUnSecureSSL() throws IOException {
		SSLSocketFactory sslSocketFactory = null;
		// Create a trust manager that does not validate certificate chains
		final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

			@Override
			public void checkClientTrusted(final X509Certificate[] chain, final String authType) {
			}

			@Override
			public void checkServerTrusted(final X509Certificate[] chain, final String authType) {
			}

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}
		} };

		// Install the all-trusting trust manager
		final SSLContext sslContext;
		try {
			sslContext = SSLContext.getInstance("TLS");
			//sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
			// Create an ssl socket factory with our all-trusting manager
			sslSocketFactory = sslContext.getSocketFactory();
		} catch (NoSuchAlgorithmException e) {
			throw new IOException("Can't create unsecure trust manager");
		} catch (KeyManagementException e) {
			throw new IOException("Can't create unsecure trust manager");
		}
		return sslSocketFactory;
	}
}
