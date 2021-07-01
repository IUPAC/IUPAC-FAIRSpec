package org.iupac.fairspec.util;

import java.io.ByteArrayInputStream;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javajs.util.JSJSONParser;

/**
 * A class to contain various generally useful utility methods in association with the
 * extraction of data and metadata, and serialization of IFS Finding Aids
 * @author hansonr
 *
 */
public class Util {

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


	/**
	 * Zip up a set list of directories, files, and/or byte arrays.
	 * 
	 * @param fileName
	 * @param prefixLength to ignore 
	 * @param products a list of [String or (byte[] followed by String)]
	 * @return number of bytes in zip file
	 * @throws IOException
	 */
	public static long zip(String fileName, int prefixLength, List<Object> products) throws IOException {
		if (prefixLength < 0)
			prefixLength = 0;
		ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(fileName));
		for (int i = 0; i < products.size(); i++) {
			Object o = products.get(i);
			if (o instanceof byte[]) {
				byte[] bytes = (byte[]) o;
				String name = (String) products.get(++i);
				zipAddEntry(zos, name, new ByteArrayInputStream(bytes), bytes.length);
			}
			File f = new File(o.toString());
			copyFiles(zos, f, new File(fileName).getParentFile(), prefixLength);
		}
		zos.close();
		return new File(fileName).length();
	}

	/**
	 * Copy files or directories into a ZIP file
	 * 
	 * @param zos          ZipOutputStream
	 * @param dirOrFile    directory or file
	 * @param target       target file directory
	 * @param prefixLength to ignore in absolute path
	 * @throws IOException
	 */
	private static void copyFiles(ZipOutputStream zos, File dirOrFile, File target, int prefixLength) throws IOException {
		File[] files = (dirOrFile.isDirectory() ? dirOrFile.listFiles() : new File[] { dirOrFile });
		if (files == null)
			return;
		if (dirOrFile.isDirectory()) 
			zipAddFile(zos, dirOrFile, prefixLength);
		for (int i = 0; i < files.length; i++) {
			File f = files[i];
			if (f == null) {
				//
			} else if (f.isDirectory()) {
				copyFiles(zos, f, new File(target, f.getName()), prefixLength);
			} else if (f.length() > 0) {
				zipAddFile(zos, f, prefixLength);
			}
		}
	}

	/**
	 * add this file with to the zip output stream, ignoring prefixLength in name
	 * 
	 * @param zos open ZipOutputStream
	 * @param entryFile  file to copy
	 * @param prefixLength  to ignore in name
	 * @throws IOException
	 */
	private static void zipAddFile(ZipOutputStream zos, File entryFile, int prefixLength) throws IOException {
		boolean isDir = entryFile.isDirectory();
		String name = entryFile.getPath().substring(prefixLength).replace('\\','/') + (isDir ? "/" : "");
		if (entryFile.isDirectory()) {
			zipAddEntry(zos, name, null, 0);
		} else {
			zipAddEntry(zos, name, new FileInputStream(entryFile), entryFile.length());
		}
	}

	/**
	 * 
	 * @param zos  ZipOutputStream
	 * @param name entry name
	 * @param is   InputStream
	 * @param len  uncompressed length in bytes
	 * @throws IOException
	 */
	private static void zipAddEntry(ZipOutputStream zos, String name, InputStream is, long len) throws IOException {
		ZipEntry e = new ZipEntry(name);
		e.setSize(len);
		zos.putNextEntry(e);
		if (is != null)
			getLimitedStreamBytes(is, -1, zos, true, false);
		zos.closeEntry();
	}

}
