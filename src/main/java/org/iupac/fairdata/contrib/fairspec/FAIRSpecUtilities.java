package org.iupac.fairdata.contrib.fairspec;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.util.JSJSONParser;

/**
 * A class to contain various generally useful utility methods in association
 * with the extraction of data and metadata, and serialization of FAIRSpec
 * Finding Aids
 * 
 * @author hansonr
 *
 */
public class FAIRSpecUtilities {

	private static String logFile;

	public static byte[] getLimitedStreamBytes(InputStream is, long n, OutputStream out, boolean andCloseInput,
			boolean andCloseOutput) throws IOException {

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
		setLogging(fname, false);
	}

	public static void setLogging(String fname, boolean refresh) {
		try {
			if (fname == null || refresh) {
				if (logStream != null) {
					logStream.close();
					logStream = null;
				}
				if (!refresh)
					return;

			}
			if (!refresh)
				logFile = fname;
			logStream = new FileOutputStream(logFile, refresh);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static OutputStream logStream;

	public static String getResource(Class<?> c, String fileName) throws FileNotFoundException, IOException {
		return new String(getLimitedStreamBytes(c.getResourceAsStream(fileName), -1, null, true, true));
	}

	public static Map<String, Object> getJSONResource(Class<?> c, String fileName)
			throws FileNotFoundException, IOException {
		return new JSJSONParser().parseMap(getResource(c, fileName), false);
	}

	public static Map<String, Object> getJSONURL(String url) throws MalformedURLException, IOException {
		return new JSJSONParser().parseMap(getURLContentsAsString(url), false);
	}

	/**
	 * Zip up a set list of directories, files, and/or byte arrays.
	 * 
	 * @param fileName
	 * @param prefixLength to ignore
	 * @param products     a list of [String or (byte[] followed by String)]
	 * @return number of bytes in zip file
	 * @throws IOException
	 */
	public static long zip(String fileName, int prefixLength, List<Object> products) throws IOException {
		if (prefixLength < 0)
			prefixLength = 0;
		ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(fileName));
		for (int i = 0; i < products.size(); i++) {
			Object o = products.get(i);
			if (o instanceof File) {
				File f = (File) o;
				String name = f.getName();
				if (f.exists())
					zipAddEntry(zos, name, new FileInputStream(f), f.length());
			} else if (o instanceof byte[]) {
				byte[] bytes = (byte[]) o;
				String name = (String) products.get(++i);
				zipAddEntry(zos, name, new ByteArrayInputStream(bytes), bytes.length);
			} else {
				File f = new File(o.toString());
				copyFiles(zos, f, new File(fileName).getParentFile(), prefixLength);
			}
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
	private static void copyFiles(ZipOutputStream zos, File dirOrFile, File target, int prefixLength)
			throws IOException {
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
	 * @param zos          open ZipOutputStream
	 * @param entryFile    file to copy
	 * @param prefixLength to ignore in name
	 * @throws IOException
	 */
	private static void zipAddFile(ZipOutputStream zos, File entryFile, int prefixLength) throws IOException {
		boolean isDir = entryFile.isDirectory();
		String name = entryFile.getPath().substring(prefixLength).replace('\\', '/') + (isDir ? "/" : "");
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

	public static String mediaTypeFromFileName(String fname) {
		int pt = Math.max(fname.lastIndexOf('/'), fname.lastIndexOf('.'));
		String t = IFDConst.getMediaTypesForExtension(fname.substring(pt + 1));
		return (t == null ? "?" : t);
	}

	private final static String escapable = "\\\\\tt\rr\nn\"\"";

	public static String esc(String str) {
		if (str == null || str.length() == 0)
			return "\"\"";
		boolean haveEscape = false;
		int i = 0;
		for (; i < escapable.length(); i += 2)
			if (str.indexOf(escapable.charAt(i)) >= 0) {
				haveEscape = true;
				break;
			}
		if (haveEscape)
			while (i < escapable.length()) {
				int pt = -1;
				char ch = escapable.charAt(i++);
				char ch2 = escapable.charAt(i++);
				StringBuffer sb = new StringBuffer();
				int pt0 = 0;
				while ((pt = str.indexOf(ch, pt + 1)) >= 0) {
					sb.append(str.substring(pt0, pt)).append('\\').append(ch2);
					pt0 = pt + 1;
				}
				sb.append(str.substring(pt0, str.length()));
				str = sb.toString();
			}
		return "\"" + escUnicode(str) + "\"";
	}

	public static String escUnicode(String str) {
		for (int i = str.length(); --i >= 0;)
			if (str.charAt(i) > 0x7F) {
				String s = "0000" + Integer.toHexString(str.charAt(i));
				str = str.substring(0, i) + "\\u" + s.substring(s.length() - 4) + str.substring(i + 1);
			}
		return str;
	}

	/**
	 * Does a clean ITERATIVE replace of strFrom in str with strTo. Thus,
	 * rep("Testttt", "tt","t") becomes "Test".
	 * 
	 * @param str
	 * @param strFrom
	 * @param strTo
	 * @return replaced string
	 */
	public static String rep(String str, String strFrom, String strTo) {
		if (str == null || strFrom.length() == 0 || str.indexOf(strFrom) < 0)
			return str;
		boolean isOnce = (strTo.indexOf(strFrom) >= 0);
		do {
			str = str.replace(strFrom, strTo);
		} while (!isOnce && str.indexOf(strFrom) >= 0);
		return str;
	}

	public static String replaceStrings(String s, List<String> list, List<String> newList) {
		int n = list.size();
		for (int i = 0; i < n; i++) {
			String name = list.get(i);
			String newName = newList.get(i);
			if (!newName.equals(name))
				s = rep(s, name, newName);
		}
		return s;
	}

	/**
	 * This class accepts an input stream for an XLSX or ODS file and creates an
	 * Object[][] array of the cell data.
	 * 
	 * String data are stripped of all XML tags.
	 * 
	 * Currently, data as String is returned. It is not clear how to generally make
	 * this generate decimal data.
	 * 
	 * @author hansonr
	 *
	 */
	public static class SpreadsheetReader {

		/**
		 * Get cell data for
		 * 
		 * @param is
		 * @param sheetName
		 * @param emptyValue  default value for "empty" cell, typically empty string or
		 *                    null
		 * @param closeStream
		 * @return
		 * @throws IOException
		 */
		public static Object[][] getCellData(InputStream is, String sheetName, String emptyValue, boolean closeStream)
				throws IOException {
			ZipInputStream zis = null;
			Map<Integer, String> sparseData = null;
			int[] retMaxRC = new int[2];
			try {
				zis = new ZipInputStream(is);
				String xlsSheetXML = null;
				String xlsSharedXML = null;
				String odsXML = null;
				String xlsSheetName = sheetName;
				if (xlsSheetName == null)
					xlsSheetName = "sheet1";
				xlsSheetName += ".xml";
				ZipEntry entry;
				while ((entry = zis.getNextEntry()) != null && (xlsSheetXML == null || xlsSharedXML == null)) {
					String name = entry.getName();
					if (name.equals("content.xml")) {
						odsXML = new String(
								FAIRSpecUtilities.getLimitedStreamBytes(zis, entry.getSize(), null, false, true),
								"UTF-8");
						break;
					} else if (name.endsWith(xlsSheetName)) {
						xlsSheetXML = new String(
								FAIRSpecUtilities.getLimitedStreamBytes(zis, entry.getSize(), null, false, true),
								"UTF-8");
					} else if (name.endsWith("sharedStrings.xml")) {
						xlsSharedXML = new String(
								FAIRSpecUtilities.getLimitedStreamBytes(zis, entry.getSize(), null, false, true),
								"UTF-8");
					}
				}
				if (xlsSheetXML == null && odsXML == null)
					throw new IOException("SheetReader - no sheet named " + sheetName + " found");
				sparseData = (odsXML != null ? processODSData(odsXML, sheetName, retMaxRC)
						: processXLSXData(xlsSheetXML, xlsSharedXML, retMaxRC));
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (zis != null && closeStream)
					zis.close();
			}
			return sparseDataToObjectArray(sparseData, retMaxRC[0], retMaxRC[1], emptyValue);
		}

		/**
		 * Convert sparse Map data to Object[][] array. Empty cells (null or "") are
		 * converted to the specified value.
		 * 
		 * @param rrrc       ccData
		 * @param nRows
		 * @param nCols
		 * @param emptyValue
		 * @return
		 */
		private static Object[][] sparseDataToObjectArray(Map<Integer, String> rrrcccData, int nRows, int nCols,
				String emptyValue) {
			Object[][] cells = new Object[nRows][nCols];
			for (int r = 0; r < nRows; r++)
				for (int c = 0; c < nCols; c++) {
					Object val = rrrcccData.get(encodeRC(r + 1, c + 1));
					if (val == null || "".equals(val))
						val = emptyValue;
					cells[r][c] = val;
				}
			return cells;
		}

		private static Map<Integer, String> processODSData(String odsXML, String sheetName, int[] retMaxRC) {
			if (sheetName == null)
				sheetName = "Sheet 1";
			sheetName = sheetName.replace(' ', '_');
			Map<Integer, String> sparseData = new LinkedHashMap<>();
			String[] sheets = split(odsXML, "<table:table ");
			for (int i = 1; i < sheets.length; i++) {
				String xml = sheets[i];
				if (!sheetName.equals(getQuotedAttribute(xml, "table:name")))
					continue;
				String[] rows = split(xml, "<table:table-row ");
				retMaxRC[0] = rows.length;
				for (int r = 1; r < rows.length; r++) {
					int ncolEmpty = 0;
					String[] cols = split(rows[r], "<table:table-cell ");
					if (cols.length > retMaxRC[1])
						retMaxRC[1] = cols.length;
					for (int c = 1, pc = 1; c < cols.length; c++, pc++) {
						xml = cols[c];
						String s = getQuotedAttribute(xml, "table:number-columns-repeated");
						if (s != null) {
							ncolEmpty = Integer.parseInt(s);
							continue;
						}
						xml = xml.substring(xml.indexOf(">") + 1);
						pc += ncolEmpty;
						ncolEmpty = 0;
						sparseData.put(Integer.valueOf(encodeRC(r, pc)), stripXMLStyles(xml));
					}
				}
				break;
			}
			return sparseData;
		}

		private static Map<Integer, String> processXLSXData(String sheetXML, String sharedXML, int[] retMaxRC) {
			String[] sharedStrings = null;
			if (sharedXML != null) {
				String[] tokens = sharedXML.split("\\<si\\>\\<t\\>");
				sharedStrings = new String[tokens.length - 1];
				for (int i = 1; i < tokens.length; i++) {
					sharedStrings[i - 1] = tokens[i].substring(0, tokens[i].indexOf("</t>"));
				}
			}
			// ArrayList<ArrayList<String>> cells = new ArrayList<ArrayList<String>>();
			Map<Integer, String> sparseData = new LinkedHashMap<>();
			String[] tokens = sheetXML.split("\\<c r");
			for (int i = 1; i < tokens.length; i++) {
				String val = "";
				int pt = tokens[i].indexOf("</c>");
				String cell = tokens[i].substring(0, pt < 0 ? tokens[i].indexOf("/>") : pt);
				String cr = cell.substring(2, cell.indexOf('"', 3));
				int rowCol = getRRRCCC(cr);
				if (pt >= 0) {
					int r = rcToRow(rowCol);
					int c = rcToCol(rowCol);
					if (r > retMaxRC[0])
						retMaxRC[0] = r;
					if (c > retMaxRC[1])
						retMaxRC[1] = c;
					boolean isShared = (cell.indexOf("t=\"s\"") >= 0);
					pt = cell.indexOf("<v>");
					val = cell.substring(pt + 3);
					val = val.substring(0, val.indexOf("</v>"));
					if (isShared) {
						val = sharedStrings[Integer.parseInt(val)];
					}
					// TODO what about formatting?
					// nonbreaking spaces can be here
					val = stripXMLStyles(val);
				}
				if (val.length() > 0)
					sparseData.put(Integer.valueOf(rowCol), val);
			}
			return sparseData;
		}

		/**
		 * get rid of all font, color, and other extraneous values such as non-breaking
		 * spaces, then trim
		 * 
		 * @param xml
		 * @return
		 */
		private static String stripXMLStyles(String xml) {
			if (xml.length() == 0)
				return null;
			String[] a = split(xml, ">");
			StringBuffer sb = new StringBuffer(a[0]);
			for (int i = 1; i < a.length; i++) {
				sb.append(a[i].substring(0, a[i].indexOf("<")));
			}
			// remove non-breaking spaces and trim
			return sb.toString().replace('\u00A0', ' ').trim();
		}

		private static int encodeRC(int r, int c) {
			return r * 1000 + c;
		}

		public static int rcToRow(int rc) {
			return rc / 1000;
		}

		public static int rcToCol(int rc) {
			return rc % 1000;
		}

		public static int getRRRCCC(String cr) {
			int r = 0;
			int c = 0;
			for (int i = 0, n = cr.length(); i < n; i++) {
				char ch = cr.charAt(i);
				if (ch >= 'A') {
					c = c * 26 + ((int) ch) - 64;
				} else {
					r = r * 10 + ((int) ch) - 48;
				}
			}
			return encodeRC(r, c);
		}

		public static boolean hasDataKey(Map<String, Object> map) {
			return map.containsKey("DATA");
		}

		/**
		 * Finds the column with the first row value equal to indexKey and then adds
		 * that data to the map with key "DATA" and also adds the column number
		 * (1-based) as the value for key "INDEX_COL". Returns the column number.
		 * 
		 * @param map
		 * @param data     Object[][] data presumably from
		 * @param indexKey
		 * @return the column number for this indexKey
		 * @throws ClassCastException
		 */
		public static int setMapData(Map<String, Object> map, Object data, String indexKey) throws ClassCastException {
			int icol = 1;
			if (!(data instanceof Object[][]))
				throw new ClassCastException("Data must be Object[][]");
			Object[] row0 = ((Object[][]) data)[0];
			for (int c = 0; c < row0.length; c++) {
				if (indexKey == null || row0[c].equals(indexKey)) {
					icol = c + 1;
					break;
				}
			}
			map.put("INDEX_COL", Integer.valueOf(icol));
			map.put("DATA", data);
			return icol;
		}

		public static List<Object[]> getRowDataForIndex(Map<String, Object> map, String index) {
			Object[][] cellData = (Object[][]) map.get("DATA");
			int icol = ((Integer) map.get("INDEX_COL")).intValue() - 1;
			return getRowData(cellData, icol, index);
		}

		private static List<Object[]> getRowData(Object[][] cellData, int icol, String index) {
			int ncol = 0;
			if (cellData == null || icol < 0 || icol >= (ncol = cellData[0].length))
				return null;
			List<Object[]> data = new ArrayList<Object[]>();
			Object[] headerRow = cellData[0];
			for (int r = 1; r < cellData.length; r++) {
				Object[] rowData = cellData[r];
				if (index.equals(rowData[icol])) {
					for (int i = 0; i < ncol; i++) {
						if (i != icol && rowData[i] != null)
							data.add(new Object[] { headerRow[i], rowData[i] });
					}
					// don't break here, as there may be more than one row with a given index;
					// break;
				}
			}
			return data.size() == 0 ? null : data;
		}

		public static void dumpData(Object[][] data) {
			StringBuffer sb = new StringBuffer();
			for (int r = 0; r < data.length; r++) {
				Object[] row = data[r];
				for (int c = 0; c < row.length; c++) {
					sb.append(row[c]).append('\t');
				}
				sb.append("\n");
			}
			System.out.println(sb.toString());
		}

		public static void test() {
			try {
				FileInputStream fis = new FileInputStream(new File("c:/temp/manifest.xlsx"));
				Object[][] data = SpreadsheetReader.getCellData(fis, "sheet1", "", true);
				SpreadsheetReader.dumpData(data);
				List<Object[]> list = getRowData(data, 0, "15");
				System.out.println(list.size());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	public static void main(String[] args) {
		SpreadsheetReader.test();
	}

	public static void refreshLog() {
		if (logStream != null)
			setLogging(null, true);
	}

	  /**
	   * 
	   *  proper splitting, even for Java 1.3 -- if the text ends in the run,
	   *  no new line is appended.
	   * 
	   * @param text
	   * @param run
	   * @return  String array
	   */
	  public static String[] split(String text, String run) {
	    if (text.length() == 0)
	      return new String[0];
	    int n = 1;
	    int i = text.indexOf(run);
	    String[] lines;
	    int runLen = run.length();
	    if (i < 0 || runLen == 0) {
	      lines = new String[1];
	      lines[0] = text;
	      return lines;
	    }
	    int len = text.length() - runLen;
	    for (; i >= 0 && i < len; n++)
	      i = text.indexOf(run, i + runLen);
	    lines = new String[n];
	    i = 0;
	    int ipt = 0;
	    int pt = 0;
	    for (; (ipt = text.indexOf(run, i)) >= 0 && pt + 1 < n;) {
	      lines[pt++] = text.substring(i, ipt);
	      i = ipt + runLen;
	    }
	    if (text.indexOf(run, len) != len)
	      len += runLen;
	    lines[pt] = text.substring(i, len);
	    return lines;
	  }

	  public static String getQuotedAttribute(String info, String name) {
		    int i = info.indexOf(name + "=\"");
		    if (i < 0)
		    	return null;
		    i += name.length() + 2;
		    int pt = i + 1;
		    int len = info.length();
		    while (++i < len && info.charAt(i) != '"')
		      if (info.charAt(i) == '\\')
		        i++;
		    return info.substring(pt, i);
		  }

	  static {
	  System.out.println(getQuotedAttribute("<test aref=\"test\">", "aref"));
	  }
	  
}
