package com.integratedgraphics.ifs.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javajs.util.JSJSONParser;
import swingjs.api.JSUtilI;

@SuppressWarnings("deprecation")
public class Util {

	public static boolean isJS = /** @j2sNative true || */ false;

	public static JSUtilI jsutil;

	static {
		try {
			if (isJS) {
				jsutil = ((JSUtilI) Class.forName("swingjs.JSUtil").newInstance());
			}
		} catch (Exception e) {
		}
	}
	/**
	 * Ret the URL contents as a string
	 * 
	 * @param url
	 * @return byte[]
	 * 
	 * @author hansonr
	 */
	public static void getURLContentsAsync(URL url, Function<byte[], Void> whenDone) {
		try {
			if (isJS) {
				// Java 9! return new String(url.openStream().readAllBytes());
				getURLBytesAsync(url, whenDone);
				return;
			}
			whenDone.apply(getLimitedStreamBytes(url.openStream(), -1, null, true, true));
			return;
		} catch (IOException e) {
			e.printStackTrace();
			whenDone.apply(null);
		}
	}


	public static byte[] getURLBytesAsync(URL url, Function<byte[], Void> whenDone) throws IOException {
		if (whenDone == null)
			return getLimitedStreamBytes(url.openStream(), 0, null, true, true);
		jsutil.getURLBytesAsync(url, whenDone);
		return null;
	}

	// from javajs.Rdr
	public static byte[] getLimitedStreamBytes(InputStream is, long n, OutputStream out, boolean andCloseInput, boolean andCloseOutput)
			throws IOException {

		// Note: You cannot use InputStream.available() to reliably read
		// zip data from the web.

		boolean toOut = (out != null);
		int buflen = (n > 0 && n < 1024 ? (int) n : 1024);
		byte[] buf = new byte[buflen];
		byte[] bytes = (out == null ? new byte[n < 0 ? 4096 : (int) n] : null);
		int len = 0;
		int totalLen = 0;
		if (n < 0)
			n = Integer.MAX_VALUE;
		while (totalLen < n && (len = is.read(buf, 0, buflen)) > 0) {
			totalLen += len;
			if (toOut) {
				out.write(buf, 0, len);
			} else {
				if (totalLen > bytes.length)
					bytes = Arrays.copyOf(bytes, totalLen * 2);
				System.arraycopy(buf, 0, bytes, totalLen - len, len);
				if (n != Integer.MAX_VALUE && totalLen + buflen > bytes.length)
					buflen = bytes.length - totalLen;
			}
		}
		if (andCloseInput) {
			try {
				is.close();
			} catch (IOException e) {
				// ignore
			}
		}
		if (toOut) {
			if (andCloseOutput)
			try {
				out.close();
			} catch (IOException e) {
				// ignore
			}
			return null;
		}
		if (totalLen == bytes.length)
			return bytes;
		buf = new byte[totalLen];
		System.arraycopy(bytes, 0, buf, 0, totalLen);
		return buf;
	}


	public static byte[] getURLBytes(String url) throws MalformedURLException, IOException {
		return getLimitedStreamBytes(new URL(url).openStream(), -1, null, true, true);
	}


	public static String getURLContentsAsString(String url) throws MalformedURLException, IOException {
		return new String(getURLBytes(url));
	}


	/**
	 * Write bytes to a file. Failure to write could leave a dangling object.
	 * 
	 * @param bytes
	 * @param fileTarget
	 * @throws IOException
	 */
	public static void writeBytesToFile(byte[] bytes, File fileTarget) throws IOException {
		FileOutputStream fos = new FileOutputStream(fileTarget);
		fos.write(bytes);
		fos.close();
	}


	public static void setLogging(String fname) {
		try {
			if (fname == null) {
				if (Util.logStream != null)
					Util.logStream.close();
				return;
			}
			Util.logStream = new FileOutputStream(fname);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public static OutputStream logStream;


	public static String getResource(Class<?> c, String fileName) throws FileNotFoundException, IOException {
		return new String(getLimitedStreamBytes(c.getResourceAsStream(fileName), -1, null, true, true));
	}


	public static Map<String, Object> getJSONResource(Class<?> c, String fileName) throws FileNotFoundException, IOException {
		return new JSJSONParser().parseMap(getResource(c, fileName), false);
	}


	public static Map<String, Object> getJSONURL(String url) throws MalformedURLException, IOException {
		return new JSJSONParser().parseMap(Util.getURLContentsAsString(url), false);
	}


	public static long zip(String fileName, String prefix, String filePrefix, List<String> products) throws IOException {
		ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(fileName));
		for (int i = 0; i < products.size(); i++) {
			File f = new File(products.get(i));
			copyFiles(zos, f, new File(fileName).getParentFile(), prefix, filePrefix);
		}
		zos.close();
		return new File(fileName).length();
	}

	private static void copyFiles(ZipOutputStream zos, File dirOrFile, File target, String prefix, String filePrefix) throws IOException {
		File[] files = (dirOrFile.isDirectory() ? dirOrFile.listFiles() : new File[] { dirOrFile });
		if (files == null)
			return;
		if (dirOrFile.isDirectory()) 
			zipAdd(zos, dirOrFile, prefix, filePrefix);
		for (int i = 0; i < files.length; i++) {
			File f = files[i];
			if (f == null) {
				//
			} else if (f.isDirectory()) {
				copyFiles(zos, f, new File(target, f.getName()), prefix, filePrefix);
			} else if (f.length() > 0) {
				zipAdd(zos, f, prefix, filePrefix);
			}
		}
	}

	private static void zipAdd(ZipOutputStream zos, File f, String prefix, String filePrefix) throws IOException {
		boolean isDir = f.isDirectory();
		String name = f.getPath();
		if (filePrefix != null && name.startsWith(filePrefix))
			name = name.substring(filePrefix.length());
		else
			name = name.substring(prefix.length());
		name = name.replace('\\','/') + (isDir ? "/" : "");
		ZipEntry e = new ZipEntry(name);
		e.setSize(f.isDirectory() ? 0 : f.length());
		zos.putNextEntry(e);
		if (!f.isDirectory())
			getLimitedStreamBytes(new FileInputStream(f), -1, zos, true, false);
		zos.closeEntry();
	}

}
