/***
 * https://unknowns.googlecode.com/svn/trunk/MHTView/src/org/hld/mht/MHTUtil.java
 */

package com.erakk.lnreader.helper;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class MHTUtil {
	private static enum TransferEncoding {
		Null, Base64, QuotedPrintable
	};

	private static String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

	public static String exportHtml(String mhtPath, String exportDir,
			String htmlName) throws Exception {
		return new MHT().export(mhtPath, exportDir, htmlName);
	}

	public static String exportHtml(String mhtPath, String exportDir)
			throws Exception {
		return new MHT().export(mhtPath, exportDir);
	}

	public static String exportHtml(String mhtPath) throws Exception {
		return exportHtml(mhtPath, new File(mhtPath).getParent());
	}

	public static TransferEncoding checkTransferEncoding(String transferEncoding) {
		if ("base64".equalsIgnoreCase(transferEncoding))
			return TransferEncoding.Base64;
		else if ("quoted-printable".equalsIgnoreCase(transferEncoding))
			return TransferEncoding.QuotedPrintable;
		else
			return TransferEncoding.Null;
	}

	public static byte[] decodeB(byte[] bytes) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int i = 0;
		int len = bytes.length;
		while (i < len) {
			char[] four = new char[4];
			int j = 0;
			while (j < 4) {
				byte b = bytes[i++];
				if (b != '\r' || b != '\n')
					four[j++] = (char) b;
			}
			int k;
			if (four[3] == '=') {
				if (four[2] == '=') {
					k = 1;
				} else {
					k = 2;
				}
			} else {
				k = 3;
			}
			int aux = 0;
			for (j = 0; j < 4; j++) {
				if (four[j] != '=') {
					aux = aux | (chars.indexOf(four[j]) << (6 * (3 - j)));
				}
			}
			for (j = 0; j < k; j++) {
				out.write((aux >>> (8 * (2 - j))) & 0xFF);
			}
		}
		out.close();
		return out.toByteArray();
	}

	public static byte[] decodeQ(byte[] bytes) throws IOException {
		int len = bytes.length;
		int length = 0;
		for (int i = 0; i < len; i++) {
			byte b = bytes[i];
			if (b == '=') {
				i++;
				if (i == len)
					break;
				b = bytes[i];
				if (b == '\r' || b == '\n') {
					b = bytes[++i];
					if (b != '\n') {
						i--;
					}
					continue;
				}
				int result = -Character.digit(b, 16);
				result *= 16;
				result -= Character.digit(bytes[++i], 16);
				bytes[length++] = (byte) -result;
			} else {
				bytes[length++] = b;
			}
		}
		byte[] result = new byte[length];
		System.arraycopy(bytes, 0, result, 0, length);
		return result;
	}

	public static String decodeQ(String str, String charsetName)
			throws IOException {
		byte[] bs = str.getBytes();
		int len = bs.length;
		int length = 0;
		for (int i = 0; i < len; i++) {
			byte b = bs[i];
			if (b == '=') {
				i++;
				if (i == len)
					break;
				b = bs[i];
				if (b == '\r' || b == '\n') {
					b = bs[++i];
					if (b != '\n') {
						i--;
					}
					continue;
				}
				int result = -Character.digit(b, 16);
				result *= 16;
				result -= Character.digit(bs[++i], 16);
				bs[length++] = (byte) -result;
			} else {
				bs[length++] = b;
			}
		}
		return new String(bs, 0, length, charsetName);
	}

	private static class MHT {
		private String boundary;
		private String content;
		private String url;
		private File file;
		private File dir;
		private String lastReadTempString;
		private final Map<String, String> replaceMap = new HashMap<String, String>();
		private final List<String> entityNameList = new ArrayList<String>();
		private static final String NEWLINES = "\r\n";
		private static final byte[] NEWLINES_BYTES = NEWLINES.getBytes();

		private MHT() {
		}

		private String export(String mhtPath, String exportDir)
				throws Exception {
			File mht = new File(mhtPath);
			// è§£æžmhtå‰å…ˆåˆ¤æ–­è¦è¾“å‡ºçš„htmlæ–‡ä»¶æ˜¯å¦å·²å­˜åœ¨
			String fileName = mht.getName();
			int index = fileName.lastIndexOf('.');
			if (index > 0)
				fileName = fileName.substring(0, index);
			MessageDigest digest = MessageDigest.getInstance("MD5");
			StringBuilder sb = new StringBuilder("_");
			for (byte b : digest.digest((mht.getPath() + ":" + mht.length()
					+ ":" + mht.lastModified()).getBytes())) {
				sb.append(Integer.toHexString(0xFF & b));
			}
			fileName += sb.toString();
			if (exportDir == null)
				exportDir = "";
			if (exportDir.length() > 0 && !exportDir.equals(File.separatorChar))
				exportDir += File.separatorChar;
			file = new File(exportDir + fileName + ".html");

			if (file.isFile())
				return file.getAbsolutePath();
			else if (file.exists()) {
				fileName = fileName + "_" + System.currentTimeMillis();
				file = new File(exportDir + fileName + ".html");
			}
			return export(mhtPath, exportDir, file.getName());
		}

		private String export(String mhtPath, String exportDir, String filename)
				throws Exception {
			File mht = new File(mhtPath);
			file = new File(exportDir + "/" + filename + ".html");
			exportDir = exportDir + "/" + filename + File.separatorChar;
			dir = new File(exportDir);
			// å¼€å§‹è§£æžmhtæ–‡ä»¶
			BufferedReader in = null;
			try {
				in = new BufferedReader(new FileReader(mht));
				String temp;
				// èŽ·å–mhtçš„boundary
				while ((temp = in.readLine()) != null) {
					int i = temp.indexOf("boundary");
					if (i > -1) {
						int start = temp.indexOf("\"", i);
						int end = 0;
						if (start > -1) {
							start++;
							end = temp.indexOf("\"", start);
						} else {
							start = temp.indexOf("=", i);
							start++;
							end = temp.length();
						}
						boundary = temp.substring(start, end);
						break;
					}
				}
				// è§£æžå„ä¸ªå†…å®¹
				splitEntity(in, boundary);
			} finally {
				if (in != null)
					try {
						in.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
			}
			if (content == null) {
				throw new Exception("mht format error");
			}
			// å¤„ç†htmlå†…å®¹
			Iterator<Entry<String, String>> iterator = replaceMap.entrySet()
					.iterator();
			while (iterator.hasNext()) {
				Entry<String, String> entry = iterator.next();
				content = content.replace(entry.getKey(), entry.getValue());
			}
			content = content.replaceAll(
					"<base\\s+href\\s*=\\s*\".*\".*((/>)|(</base>))", "");
			if (url != null)
				content = content.replace(url, file.getName());
			BufferedOutputStream out = new BufferedOutputStream(
					new FileOutputStream(file));
			out.write(content.getBytes());
			out.close();
			return file.getAbsolutePath();
		}

		private void splitEntity(BufferedReader in, String boundary)
				throws Exception {
			if (boundary == null)
				return;
			lastReadTempString = in.readLine();
			while (lastReadTempString != null) {
				if (lastReadTempString.startsWith(boundary, 2)
						&& !lastReadTempString.endsWith("--")) {
					try {
						addEntity(in, boundary);
					} catch (Exception e) {
						e.printStackTrace();
						lastReadTempString = in.readLine();
					}
				} else {
					lastReadTempString = in.readLine();
				}
			}
		}

		private void addEntity(BufferedReader in, String boundary)
				throws Exception {
			Entity entity = new Entity();
			while ((lastReadTempString = in.readLine()) != null) {
				if (entity.type == null
						&& lastReadTempString.startsWith("Content-Type: ")) {
					if (lastReadTempString.contains("multipart")) {
						while ((lastReadTempString = in.readLine()) != null) {
							if (lastReadTempString.length() == 0)
								throw new Exception("unknown mht format");
							char c = lastReadTempString.charAt(0);
							if (c == '\t' || c == ' ') {
								int i = lastReadTempString.indexOf("boundary");
								if (i > -1) {
									int start = lastReadTempString.indexOf(
											"\"", i);
									int end = 0;
									if (start > -1) {
										start++;
										end = lastReadTempString.indexOf("\"",
												start);
									} else {
										start = lastReadTempString.indexOf("=",
												i) + 1;
										end = lastReadTempString.length();
									}
									splitEntity(in,
											lastReadTempString.substring(start,
													end));
									return;
								}
							} else {
								throw new Exception("unknown mht format");
							}
						}
					}
					int i = lastReadTempString.indexOf(";");
					if (i == -1) {
						entity.type = lastReadTempString.substring(14);
						continue;
					} else {
						entity.type = lastReadTempString.substring(14, i);
						i = lastReadTempString.indexOf("charset", i);
						if (i > -1) {
							i = lastReadTempString.indexOf("\"", i) + 1;
							entity.charset = lastReadTempString.substring(i,
									lastReadTempString.indexOf("\"", i));
							continue;
						}
						while ((lastReadTempString = in.readLine()) != null) {
							if (lastReadTempString.length() == 0)
								break;
							char c = lastReadTempString.charAt(0);
							if (c == '\t' || c == ' ') {
								i = lastReadTempString.indexOf("charset");
								if (i > -1) {
									i = lastReadTempString.indexOf("\"", i) + 1;
									entity.charset = lastReadTempString
											.substring(i, lastReadTempString
													.indexOf("\"", i));
									lastReadTempString = in.readLine();
									break;
								}
							} else {
								break;
							}
						}
					}
				}
				if (entity.transferEncoding == null
						&& lastReadTempString
						.startsWith("Content-Transfer-Encoding: ")) {
					entity.transferEncoding = lastReadTempString.substring(27);
				} else if (entity.location == null
						&& lastReadTempString.startsWith("Content-Location: ")) {
					entity.location = lastReadTempString.substring(18);
				} else if (lastReadTempString.length() == 0)
					break;
			}
			if (content == null && entity.type != null
					&& entity.type.contains("text")) {
				TransferEncoding transferEncoding = checkTransferEncoding(entity.transferEncoding);
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				readEntity(in, out, transferEncoding, boundary);
				if (entity.charset == null)
					content = out.toString();
				else
					content = out.toString(entity.charset);
				url = entity.location;
			} else if (entity.location != null
					&& !replaceMap.containsKey(entity.location)) {
				TransferEncoding transferEncoding = checkTransferEncoding(entity.transferEncoding);
				if (dir.mkdirs())
					new File(dir, ".nomedia").createNewFile();
				int i1 = entity.location.indexOf("?");
				int i2;
				if (i1 > -1)
					i2 = entity.location.lastIndexOf("/", i1);
				else {
					i1 = entity.location.length();
					i2 = entity.location.lastIndexOf("/");
				}
				String entityName = (i2 > -1 ? entity.location.substring(
						i2 + 1, i1) : "");
				if (entityName.length() == 0)
					entityName = String.valueOf(System.currentTimeMillis());
				entityName = entityName.toLowerCase();
				while (entityNameList.contains(entityName)) {
					entityName = entityName + "_" + System.nanoTime();
				}
				File entityFile = new File(dir, entityName);
				BufferedOutputStream out = new BufferedOutputStream(
						new FileOutputStream(entityFile));
				readEntity(in, out, transferEncoding, boundary);
				replaceMap.put(entity.location, dir.getName() + "/"
						+ entityName);
			}
		}

		private void readEntity(BufferedReader in, OutputStream out,
				TransferEncoding transferEncoding, String boundary)
						throws IOException {
			while ((lastReadTempString = in.readLine()) != null) {
				if (lastReadTempString.startsWith(boundary, 2))
					break;
				switch (transferEncoding) {
				case Base64:
					out.write(decodeB(lastReadTempString.getBytes()));
					break;
				case QuotedPrintable:
					out.write(decodeQ(lastReadTempString.getBytes()));
					if (!lastReadTempString.endsWith("="))
						out.write(NEWLINES_BYTES);
					break;
				default:
					out.write(lastReadTempString.getBytes());
					out.write(NEWLINES_BYTES);
					break;
				}
			}
			out.close();
		}

		private class Entity {
			private String type;
			private String charset;
			private String transferEncoding;
			private String location;
		}
	}
}