package com.integratedgraphics.extractor;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecExtractorHelper.FileList;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecFindingAidHelper;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;
import org.iupac.fairdata.core.IFDReference;
import org.iupac.fairdata.core.IFDRepresentation;
import org.iupac.fairdata.util.ZipUtil;

import com.integratedgraphics.zip.ZipInputStream;
import com.junrar.Archive;
import com.junrar.exception.RarException;
import com.junrar.rarfile.FileHeader;

/**
 * A set of static classes for use by MetadataExtractor, primarily
 * 
 * @author hansonr@stolaf.edu
 *
 */
public class ExtractorUtils {

	/**
	 * The ObjectParser class is key to the extractor. It uses the {....} syntax of
	 * the IFD-extract.json configuration file, reading key/value pairs.
	 * 
	 * Each IFD.extract.object item in this file generates a parser that can
	 * identify compound, structure, and spectrum identifiers and properties.
	 * 
	 * As a ZIP file or directory is read, each full file path, including
	 * directories, will be parsed by regex patterns generated her from that file.
	 * 
	 * The regex pattern matches are then used to create or find the corresponding
	 * compound, structure, and spectrum objects within the growing metadata model
	 * instancce.
	 * 
	 * Alternatively (or in addition), if automation has been used, this specialized
	 * automationParser will 
	 * 
	 * A static class for parsing the object string and using regex to match
	 * filenames. This static class may be overridden to give it different
	 * capabilities.
	 * 
	 * @author hansonr
	 *
	 */

	public static class ObjectParser {

		public static final int PATH        = 0;
		public static final int CMPD_ID     = 1;
		public static final int SPEC_ID     = 2;
		public static final int CMPD_ID_COL = 3;
		public static final int CMPD_PATH   = 4;
		public static final int TYPE        = 5; // keep this last;not used yet, if ever

		private final static Pattern pStarDotStar = Pattern.compile("\\*([^|/])\\*");
		private final static Pattern objectDefPattern = Pattern.compile("\\{([^:]+)::([^}]+)\\}");

		private static final String REGEX_QUOTE = "\\Q";
		private static final String REGEX_UNQUOTE = "\\E";
		private static final String REGEX_ANY_NOT_PIPE_OR_DIR = REGEX_UNQUOTE + "[^|/]+" + REGEX_QUOTE;
		private static final String REGEX_KEYDEF_START = REGEX_UNQUOTE + "(?<";
		private static final String REGEX_KEYVAL_START = REGEX_UNQUOTE + "\\k<";
		private static final String REGEX_KV_END = ">" + REGEX_QUOTE;
		private static final String REGEX_END_PARENS = REGEX_UNQUOTE + ")" + REGEX_QUOTE;
		private static final String REGEX_EMPTY_QUOTE = REGEX_QUOTE + REGEX_UNQUOTE;

		private static final String RAW_REGEX_TAG = "{regex::";

		private static final char TEMP_RAW_IN_CHAR = '\0'; // --> <
		private static final char TEMP_RAW_OUT_CHAR = '\1'; // --> >
		private static final char TEMP_STAR_CHAR = '\2'; // --> *
		private static final char TEMP_KEYVAL_IN_CHAR = '\3'; // --> <
		private static final char TEMP_KEYVAL_OUT_CHAR = '\4'; // --> >
		private static final char TEMP_ANY_SEP_ANY_CHAR = '\5'; // see below
		private static final char TEMP_ANY_SEP_ANY_CHAR2 = '\6'; // see below
		private static final char TEMP_IGNORE = '\7'; // \\ removed

		private static final String TEMP_KEYVAL_IN = REGEX_UNQUOTE + "(?" + TEMP_KEYVAL_IN_CHAR;

		private static final String TEMP_KEYVAL_OUT = TEMP_KEYVAL_OUT_CHAR + REGEX_QUOTE;

		/**
		 * multiple separations by char. for example *-*.zip -->
		 */
		private static final String TEMP_ANY_SEP_ANY_GROUPS = REGEX_UNQUOTE + "(" + "[^|/\5]+(?:\6[^|/\5]+)"
		// + TEMP_STAR_CHAR
				+ "+" + ")" + REGEX_QUOTE;

		/**
		 * // /**\/ --> "/\E(?:[^|/]+/)*\Q" [ backslash after two asterisks only for
		 * this comment ]
		 *
		 * one or more directories;
		 * 
		 * test/**\/*.zip matches test/xxx.zip or test/bbb/aaa/xxx.zip
		 */
		private static final String TEMP_ANY_DIRECTORIES = REGEX_UNQUOTE + "(?:[^|/]+/)" + TEMP_STAR_CHAR + REGEX_QUOTE;

		private static final char BACK_SLASH_IGNORED = '\\';

		private static int parserCount;

		private final int index;

		public int getIndex() {
			return index;
		}

		private String sData;

		private Pattern p;

		private List<String> regexList;

		private Map<String, String> keys;

		protected IFDExtractorMain extractor;

		protected ExtractorResource dataSource;
		protected boolean hasData;

		private List<Object> replacements;
		private ArrayList<String> keyList;
		
		public boolean isAutomationParser;

		/**
		 * @param sObj
		 * @throws IFDException
		 */
		public ObjectParser(IFDExtractorMain extractor, ExtractorResource resource, String sObj) throws IFDException {
			this.extractor = extractor;
			index = parserCount++;
			dataSource = resource;
			sData = sObj.substring(sObj.charAt(0) == '|' ? 1 : 0);
			init();
		}
		
		@Override
		public boolean equals(Object o) {
			return (o instanceof ObjectParser
					&& ((ObjectParser)o).sData.equals(sData) 
					&& ((ObjectParser)o).dataSource.equals(dataSource));
		}

		/**
		 * Prepare pattern and match.
		 * 
		 * @throws IFDException
		 * 
		 * 
		 */
		private void init() throws IFDException {
			// Using Java and JavaScript named capture groups for in-line defining.
			//
			// {regex::[a-z]} is left unchanged and becomes \\E[a-z]\\Q
			//
			// **/ becomes \\E(?:[^/]+/)*)\\Q
			//
			// *-* becomes \\E([^-]+(?:-[^-]+)+)\\Q and matches a-b-c
			//
			// * becomes \\E.+\\Q
			//
			// {id=IFD.property.dataobject.label::xxx} becomes \\E(?<id>\\Qxxx\\E)\\Q
			//
			// {IFD.property.dataobject.label::xxx} becomes
			// \\E(?<IFD0nmr0param0expt>\\Qxxx\\E)\\Q
			//
			// <id> becomes \\k<id>
			//
			// generally ... becomes ^\\Q...\\E$
			//
			// \\Q\\E in result is removed
			//
			// so:
			//
			// {IFD.property.dataobject.label::*} becomes \\E(?<IFD0nmr0param0expt>.+)\\Q
			//
			// {IFD.representation.spec.nmr.dataobject.dataset::{IFD.property.sample.label::*-*}-{IFD.property.dataobject.label::*}.jdf}
			//
			// becomes:
			//
			// ^(?<IFD0nmr0representation0vendor0dataset>(?<IFD0structure0param0compound0id>([^-](?:-[^-]+)*))\\Q-\\E(?<IFD0nmr0param0expt>.+)\\Q.jdf\\E)$
			//
			// {id=IFD.property.sample.label::*}.zip|{IFD.representation.spec.nmr.dataobject.dataset::{id}_{IFD.property.dataobject.label::*}/}
			//
			// becomes:
			//
			// ^(?<id>*)\\Q.zip|\\E(?<IFD0nmr0representation0vendor0dataset>\\k<id>\\Q_\\E(<IFD0nmr0param0expt>*)\\Q/\\E)$

			// so....

			// {regex::[a-z]} is left unchanged and becomes \\E[a-z]\\Q

			String s = protectRegex(null);

			// \ is ignored and removed at the end
			// it should only be used to break up something like *\-* to be literally a
			// single *-*, not "any number of "-"
			s = s.replace(BACK_SLASH_IGNORED, TEMP_IGNORE);

			// **/ becomes \\E(?:[^/]+/)*\\Q

			s = FAIRSpecUtilities.rep(s, "**/", TEMP_ANY_DIRECTORIES);

			// * becomes \\E.+\\Q

			s = FAIRSpecUtilities.rep(s, "*", REGEX_ANY_NOT_PIPE_OR_DIR);

			// {id=IFD.property.dataobject.label::xxx} becomes \\E(?<id>\\Qxxx\\E)\\Q
			// {IFD.property.dataobject.label::xxx} becomes
			// \\E(?<IFD0nmr0param0expt>\\Qxxx\\E)\\Q
			// <id> becomes \\k<id>

			int pt = -1;
			while ((pt = s.indexOf("]+\\Q^")) >= 0) {
				char c = s.charAt(pt + 5);
				s = s.substring(0, pt) + c + "]+\\Q" + s.substring(pt + 6);
			}
			s = compileIFDDefs(s, true, true);

			// restore '*'
			s = s.replace(TEMP_STAR_CHAR, '*');

			// restore regex
			// wrap with quotes and constraints ^\\Q...\\E$

			s = "^" + REGEX_QUOTE + protectRegex(s) + REGEX_UNQUOTE + "$";

			// \\Q\\E in result is removed

			s = FAIRSpecUtilities.rep(s, REGEX_EMPTY_QUOTE, "");

			s = FAIRSpecUtilities.rep(s, "" + TEMP_IGNORE, "");

			extractor.log("!Extractor.ObjectParser pattern: " + s);
			p = Pattern.compile(s);
		}

		/**
		 * Find and regex-ify all {id=IFD.param::value} or {IFD.param::value}.
		 * 
		 * @param s growing regex string
		 * @return regex string with all {...} fixed
		 * @throws IFDException
		 */
		private String compileIFDDefs(String s, boolean isFull, boolean replaceK) throws IFDException {
			int pt;
			while (s.indexOf("::") >= 0) {
				Matcher m = objectDefPattern.matcher(s);
				if (!m.find())
					break;
				String param = m.group(1);
				String val = m.group(2);
				String pv = "{" + param + "::" + val + "}";
				if (val.indexOf("::") >= 0)
					val = compileIFDDefs(val, false, replaceK);
				pt = param.indexOf("=");
				if (pt == 0)
					throw new IFDException("bad {def=key::val} expression: " + param + "::" + val);
				if (keys == null)
					keys = new LinkedHashMap<String, String>();
				String key = null;
				if (pt > 0) {
					key = param.substring(0, pt);
					param = param.substring(pt + 1);
				}
				param = FAIRSpecFindingAidHelper.updateKey(param);
				if (key == null)
					key = param.replace('.', '0').replace('_', '1');
				keys.put(key, param);
				String bk = "{" + key + "}";
				if (s.indexOf(bk) >= 0) {
					s = FAIRSpecUtilities.rep(s, bk, "<" + key + ">");
				}
				// escape < and > here

				s = FAIRSpecUtilities.rep(s, pv,
						(replaceK ? TEMP_KEYVAL_IN + key + TEMP_KEYVAL_OUT : REGEX_KEYDEF_START + key + REGEX_KV_END)
								+ val + REGEX_END_PARENS);
			}
			if (isFull && (s.indexOf("<") >= 0 || s.indexOf(TEMP_KEYVAL_IN_CHAR) >= 0)) {
				// now fix k< references and revert \3 \4
				s = FAIRSpecUtilities.rep(s, "<", REGEX_KEYVAL_START);
				s = FAIRSpecUtilities.rep(s, ">", REGEX_KV_END).replace(TEMP_KEYVAL_IN_CHAR, '<')
						.replace(TEMP_KEYVAL_OUT_CHAR, '>');
			}
			return s;
		}

		/**
		 * fix up {regex::...} phrases in IFD-extract.json. First pass initialization
		 * clips out regex sections so that they are not processed by ObjectParser;
		 * second pass puts it all together.
		 * 
		 * 
		 * @param s the string to protect; null for second pass
		 * @return
		 * @throws IFDException
		 */
		private String protectRegex(String s) throws IFDException {
			if (sData.indexOf(RAW_REGEX_TAG) < 0)
				return (s == null ? sData : s);
			if (s == null) {
				// init
				s = sData;
				regexList = new ArrayList<>();
				int[] pt = new int[1];
				int i = 0;
				while ((pt[0] = s.indexOf(RAW_REGEX_TAG)) >= 0) {
					// save regex and replace by \0n\1
					int p0 = pt[0];
					String rx = getIFDExtractValue(s, "regex", pt);
					regexList.add(REGEX_UNQUOTE + rx + REGEX_QUOTE);
					s = s.substring(0, p0) + TEMP_RAW_IN_CHAR + (i++) + TEMP_RAW_OUT_CHAR + s.substring(pt[0]);
				}
			} else {
				// restore regex
				int p;
				while ((p = s.indexOf(TEMP_RAW_IN_CHAR)) >= 0) {
					int p2 = s.indexOf(TEMP_RAW_OUT_CHAR);
					int i = Integer.parseInt(s.substring(p + 1, p2));
					s = s.substring(0, p) + regexList.get(i) + s.substring(p2 + 1);
				}
			}
			return s;
		}

		/**
		 * Process a {key::value} set.
		 * 
		 * @param sObj
		 * @param key
		 * @param pt
		 * @return the value for this key
		 * @throws IFDException
		 */
		private static String getIFDExtractValue(String sObj, String key, int[] pt) throws IFDException {
			key = "{" + key + "::";
			if (pt == null)
				pt = new int[1];
			int p = sObj.indexOf(key, pt[0]);
			if (p < 0)
				return null;
			int q = -1;
			int nBrace = 1;
			p += key.length();
			int len = sObj.length();
			for (int i = p; i < len && nBrace > 0; i++) {
				switch (sObj.charAt(i)) {
				case '{':
					q = i;
					nBrace++;
					break;
				case '}':
					if (--nBrace < 0) {
						throw new IFDException("unopened '}' in " + sObj + " at char " + i);
					}
					q = i;
					break;
				}
			}
			if (nBrace > 0) {
				throw new IFDException("unclosed '{' in " + sObj + " at char " + q);
			}
			pt[0] = q;
			return sObj.substring(p, pt[0]++);
		}

		@Override
		public String toString() {
			return "[ObjectParser " + this.sData + "]";
		}
		
		public Matcher match(String origin) throws IFDException {
			if (replacements != null) {
				try {
					for (int i = replacements.size(); --i >= 0;) {
						@SuppressWarnings("unchecked")
						List<Object> sub = (List<Object>) replacements.get(i);
						origin = FAIRSpecUtilities.rep(origin, (String) sub.get(0), (String) sub.get(1));
					}
				} catch (Exception e) {
					throw new IFDException("Error in subsitution for sub: " + e);
				}
			}
			return p.matcher(origin);
		}

		public ObjectParser setReplacements(List<Object> replacements) {
			this.replacements = replacements;
			return this;
		}

		public ExtractorResource getDataSource() {
			return dataSource;
		}

		public String getStringData() {
			return sData;
		}

		public boolean hasData() {
			return hasData;
		}

		public Map<String, String> getKeys() {
			return keys;
		}

		public void setHasData(boolean b) {
			hasData = b;
		}

		public List<String> getKeyList() {
			if (keyList == null) {
				keyList = new ArrayList<String>();
				if (getKeys() != null)
					for (String key : getKeys().keySet()) {
						keyList.add(key);
					}

			}
			return keyList;
		}

	}

	public static class AutomationParser extends ObjectParser {

		List<String[]> automationData;
		
		boolean automationProcessed;

		public AutomationParser(IFDExtractorMain extractor, ExtractorResource resource, List<Object> replacements, String rootPath, String[][] data)
				throws IFDException {
			super(extractor, resource, "_");
			setReplacements(replacements);
			setAutomation(rootPath, data);
			hasData = true;
			isAutomationParser = true;
		}

		public static String[][] readAutomationData(String data) throws IOException {
			String[][] a = (data == null ? null : FAIRSpecUtilities.SpreadsheetReader.getTSVData(data, "path", "compound_id", "dataobject_id", "cmpd_id_col", "cmpd_path", "type"));
			for (int i = a.length; --i >= 0;) {
				String[] info = a[i];
				String path = info[PATH];
				if (path == null)
					continue;
				int pt = path.indexOf("|");
				if (pt >= 0)
					path = path.replace("|", "__/");
				String col = info[CMPD_ID_COL];
				try {
					int icol = (int) Double.parseDouble(col) + 1;
					String[] parts = path.split("/");
					StringBuffer sb = new StringBuffer();
					for (int j = 1; j <= icol; j++) {
						sb.append(parts[j]);
						if (!parts[j].endsWith("|"))
							sb.append('/');
					}
					info[CMPD_PATH] = sb.toString().replace("__/", "|");
					System.out.println(info[CMPD_ID] + " " + info[CMPD_PATH]);
				} catch (Exception e) {
					e.printStackTrace();
					
				}
			}
			return a;
		}

		/**
		 * Filter the automation list for this parser based on the root directory of the path.
		 * There will be one parser per originating zip file or directory structure. 
		 * Called upon reading of IFD.extractor.automation.resource_id record.
		 * 
		 * @param rootPath
		 * @param data
		 * @return
		 */
		private ObjectParser setAutomation(String rootPath, String[][] data) {
			if (automationData != null)
				return this;
			dataSource.rootPath = rootPath;
			automationData = new ArrayList<>();
			if (data == null)
				System.out.println("????");
			String root = rootPath + "/";
			//TODO if necessary automationCompoundColumn = new int[data.length];
			for (int i = 0; i < data.length; i++) {
				String[] info = data[i]; 
				String path = info[PATH];
				if (path == null) {
					extractor.logWarn("null path for " + rootPath + " " + Arrays.toString(info), "AutomationParser");
				} else if (path.startsWith(root)) {
					info[PATH] = info[PATH].substring(root.length());
					automationData.add(info);
				}
			}
			return this;
		}

		/**
		 * Find the nearest path to this object that can identify a compound for it.
		 * 
		 * @param originPath
		 * @return compound id or null
		 */
		public String getAutomationCompoundIDFromPath(String originPath) {
			for (int i = automationData.size(); --i >= 0;) {
				String[] info = automationData.get(i);
				System.out.println(info[CMPD_PATH] + "\n" + originPath);
				if (info[CMPD_PATH] != null && originPath.startsWith(info[CMPD_PATH]))
					return info[CMPD_ID];
			}
			return null;
		}

	}
	
	/**
	 * A static class to cover both ZipEntry and TAR entries.
	 * 
	 * @author hansonr
	 *
	 */
	public static class ArchiveEntry {

		protected String name;
		protected long size;

		public ArchiveEntry(String name, long size) {
			this.name = name;
			this.size = size;
		}

		public ArchiveEntry(ZipEntry ze) {
			name = ze.getName();
			size = ze.getSize();
		}

		public ArchiveEntry(TarArchiveEntry te) {
			name = te.getName();
			size = te.getSize();
		}

		public ArchiveEntry(FileHeader fh) {
			name = fh.getFileName();
			name = name.replace('\\', '/');
			if (fh.isDirectory() && !name.endsWith("/"))
				name += "/";
			size = fh.getUnpSize();
		}

		public ArchiveEntry(String name) {
			this.name = name;
		}

		public ArchiveEntry() {
			// in case there is an error
		}

		public boolean isDirectory() {
			return name.endsWith("/");
		}

		public String getName() {
			return name;
		}

		public long getSize() {
			return size;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	public static class DirectoryEntry extends ArchiveEntry {

		protected File file;
		protected boolean isDir;
		protected BufferedInputStream bis;

		public DirectoryEntry(String name, File file) {
			super(null, 0);
			this.file = file;
			isDir = file.isDirectory();
			this.name = ExtractorUtils.fixPath(name, false) + (isDir ? "/" : "");
			size = (isDir ? 0 : file.length());
		}

		protected BufferedInputStream getInputStream() throws FileNotFoundException {
			return (bis != null ? bis : isDir ? null : (bis = new BufferedInputStream(new FileInputStream(file))));
		}

		protected void close() {
			if (bis != null) {
				try {
					bis.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public static class DirIterator implements Iterator<File> {

		private File dir;
		private File[] list;
		private int pt = -1, n;
		private DirIterator iter;

		DirIterator(File dir) {
			this.dir = dir;
			list = dir.listFiles();
			n = list.length;
		}

		@Override
		public boolean hasNext() {
			return (pt < n || iter != null && iter.hasNext());
		}

		@Override
		public File next() {
			File f;
			if (iter != null) {
				if (iter.hasNext())
					return iter.next();
				iter = null;
			}
			if (pt < 0) {
				pt = 0;
				f = dir;
			} else {
				f = list[pt++];
				if (f.isDirectory()) {
					return (iter = new DirIterator(f)).next();
				}
			}
			return f;
		}
	}

	/**
	 * A static class to provide a single input stream for a set of files.
	 * 
	 * @author hansonr
	 *
	 */
	public static class DirectoryInputStream extends InputStream {

		File dir;
		int offset;
		DirIterator iter;
		protected DirectoryEntry entry;

		public DirectoryInputStream(String dir) {
			if (dir.startsWith("file:/"))
				dir = dir.substring(6);
			this.dir = new File(dir);
			offset = this.dir.getAbsolutePath().length() + 1;
			reset();
		}

		@Override
		public void reset() {
			iter = new DirIterator(this.dir);
			if (iter.hasNext())
				iter.next(); // skip path itself
		}

		@Override
		public int read() throws IOException {
			return (entry == null ? -1 : entry.getInputStream().read());
		}

		@Override
		public int read(byte b[], int off, int len) throws IOException {
			return (entry == null ? -1 : entry.getInputStream().read(b, off, len));
		}

		@Override
		public void close() throws IOException {
			closeEntry();
			iter = null;
		}

		protected ArchiveEntry getNextEntry() throws FileNotFoundException {
			closeEntry();
			if (!iter.hasNext())
				return null;
			File f = iter.next();
			String name = f.getAbsolutePath().substring(offset);
			return entry = new DirectoryEntry(name, f);
		}

		protected void closeEntry() {
			if (entry != null) {
				entry.close();
				entry = null;
			}
		}

	}

	/**
	 * A static class to provide a single input stream for a set of files.
	 * 
	 * @author hansonr
	 *
	 */
	public static class RARInputStream extends InputStream {

		private Archive rar;
		private List<FileHeader> rarList = new ArrayList<>();
		private int rarPt = 0;
		private RARArchiveEntry entry;

		public RARInputStream(InputStream is) throws IOException {
			try {
				rar = new Archive(is);
			} catch (RarException | IOException e) {
				throw new IOException(e);
			}
			FileHeader fh;
			List<FileHeader> list = new ArrayList<>();
			while ((fh = rar.nextFileHeader()) != null) {
				list.add(fh);
			}
			list.sort(new Comparator<FileHeader>() {

				@Override
				public int compare(FileHeader o1, FileHeader o2) {
					return o1.getFileName().compareTo(o2.getFileName());
				}

			});
			rarList = list;

			reset();
		}

		@Override
		public void reset() {
			rarPt = 0;
		}

		@Override
		public int read() throws IOException {
			return (entry == null ? -1 : entry.getInputStream().read());
		}

		@Override
		public int read(byte b[], int off, int len) throws IOException {
			return (entry == null ? -1 : entry.getInputStream().read(b, off, len));
		}

		@Override
		public void close() throws IOException {
			closeEntry();
			super.close();
		}

		protected RARArchiveEntry getNextEntry() throws FileNotFoundException {
			closeEntry();
			if (rarPt >= rarList.size())
				return null;
			return entry = new RARArchiveEntry(rar, rarList.get(rarPt++));
		}

		protected void closeEntry() {
			if (entry != null) {
				entry.close();
				entry = null;
			}
		}

	}

	static class RARArchiveEntry extends ArchiveEntry {

		private BufferedInputStream is;
		private FileHeader fh;
		private Archive rar;

		protected RARArchiveEntry(Archive rar, FileHeader fh) {
			super(fh);
			this.rar = rar;
			this.fh = fh;
		}

		public void close() {
			fh = null;
		}

		public BufferedInputStream getInputStream() throws IOException {
			if (is == null) {
				try {
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					rar.extractFile(fh, bos);
					byte[] bytes = bos.toByteArray();
					bos.close();
					is = new BufferedInputStream(new ByteArrayInputStream(bytes));
				} catch (Exception e) {
					throw new IOException(e);
				}
			}
			return is;
		}

	}

	/**
	 * for ZipFile creation
	 */

	private static String tempDir = "c:/temp/";

	public static String setTempDir(String dir) {
		if (!dir.endsWith("/"))
			dir += "/";
		tempDir = dir;
		return new File(tempDir).getAbsolutePath();
	}
	/**
	 * optionally disable ZipFile creation in Layer 2 for testing
	 */

	private static boolean useZipFile = true;

	public static void useZipFile(boolean tf) {
		useZipFile = tf;
	}
	
	private static int maxLevel = 0;

	public static int clearTempFiles() {
		for (int i = 1; i <= maxLevel; i++) {
			File f = new File(tempDir + "temp" + i + ".zip");
			if (f.exists())
				f.delete();
		}
		int ret = maxLevel;
		maxLevel = 0;
		return ret;
	}

	
	/**
	 * A static class to allow for either ZipInputStream or TarArchiveInputStream
	 * 
	 * @author hansonr
	 *
	 */
	public static class ArchiveInputStream extends InputStream {
		private ZipInputStream zis;
		private TarArchiveInputStream tis;
		private DirectoryInputStream dis;
		private RARInputStream ris;
		protected InputStream is;
		
		private ZipFile zf;
		private Iterator<ZipEntry> zfiter;
		/**
		 * We have to sort these. See jo4c02737 -- all the directories are first! 
		 */
		private TreeMap<String, ZipEntry> zfmap;
		String err;
		
		public ArchiveInputStream(InputStream is, String fname, int level) throws IOException {
			if (is instanceof ArchiveInputStream)
				is = new BufferedInputStream(((ArchiveInputStream) is).getStream());
			if (is instanceof DirectoryInputStream) {
				this.is = dis = (DirectoryInputStream) is;
				dis.reset();
			} else if (is instanceof CrawlerInputStream) {
				this.is = is;
			} else if (is instanceof ZipInputStream) {
				this.is = is;
			} else if (ZipUtil.isZipS(is)) {
				if (useZipFile && level >= 0) {
					if (fname == null) {
						if (maxLevel < level)
							maxLevel = level;
						fname = tempDir + "temp" + level + ".zip";
			    		FileOutputStream fos = new FileOutputStream(fname);
			    		FAIRSpecUtilities.getLimitedStreamBytes(is, -1, fos, false, true);	
					} else if (fname.startsWith("file:///"))
						fname = fname.substring(8);
					 	readZipFile(fname);
				} else {
					this.is = zis = new ZipInputStream(is);
				}
			} else if (ZipUtil.isGzipS(is)) {
				this.is = tis = ZipUtil.newTarGZInputStream(is);
			} else if (fname != null && fname.endsWith(".tar")) {
				this.is = tis = ZipUtil.newTarInputStream(is);
			} else if (fname != null && (fname.endsWith(".rar")
					|| ZipUtil.isRAR(is))) {
				this.is = ris = new RARInputStream(is);
			}
		}

		private void readZipFile(String fname) throws IOException {
			zf = new ZipFile(fname);
			Enumeration<? extends ZipEntry> e = zf.entries();
			zfmap = new TreeMap<String, ZipEntry>();
			String name = null;
			while (e.hasMoreElements()) {
				try {
					ZipEntry entry = e.nextElement();
					name = entry.getName();
					if (isValidName(name))
						zfmap.put(name, entry);
				} catch (Exception ex) {
					ex.printStackTrace();
					err = ex.getMessage() + " reading zip file " + fname + " near " + name;
					System.err.println(err);
				}
			}
			zfiter = zfmap.values().iterator();
		}

		/**
		 * A class to allow crawler-based files to be extracted as though they were a
		 * zip file. This would allow better extraction of MNova files during crawling.
		 * 
		 * not fully implemented
		 * 
		 * @author hanso
		 *
		 */
		public static class CrawlerInputStream extends InputStream {

			long len;

			private CrawlerEntry entry;

			private static class CrawlerEntry extends ArchiveEntry {

				/**
				 * may contain additional IFD.xxx metadata
				 */
				private String pidDescription;
				private File file;
				private Object subdir;
				private String compoundID;
				private String dataType;
				private String surl;

				public CrawlerEntry(String name, String surl, String compoundID, String dataType, String subdir,
						String pidDescription, File f, long size) {
					super(null, size);
					this.dataType = dataType;
					this.name = name;
					this.surl = surl;
					System.out.println(f.getName() + " " + surl);
					System.out.println(name);
					this.compoundID = compoundID;
					this.subdir = subdir;
					this.pidDescription = pidDescription;
					this.file = f;
				}
			}

			private List<CrawlerEntry> fileList = new ArrayList<CrawlerEntry>();
			private int pt = 0;

			private InputStream is;

			public CrawlerInputStream() throws IOException {
				super();
			}

			public void addFile(String name, String surl, String dataType, String compoundID, String subdir,
					String pidDescription, File f, long len) {
				fileList.add(new CrawlerEntry(name, surl, dataType, compoundID, subdir, pidDescription, f, len));
				this.len += len;
			}

			protected long getLength() {
				return len;
			}

			@Override
			public void reset() {
				pt = 0;
				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
					}
					is = null;
				}
			}

			protected CrawlerEntry getNextEntry() throws IOException {
				if (is != null)
					is.close();
				if (pt >= fileList.size())
					return null;
				entry = fileList.get(pt++);
				is = new FileInputStream(entry.file);
				return entry;
			}

			@Override
			public int read() throws IOException {
				return is.read();
			}

		}

		/**
		 * Override this method to implement a custom archive reader.
		 * 
		 * @return
		 * @throws IOException
		 */
		public ArchiveEntry getNextEntry() throws IOException {
			if (ris != null) {
				RARArchiveEntry re = ris.getNextEntry();
				while (re != null && !isValidName(re.getName()))
					re = ris.getNextEntry();
				return re;
			}
			if (tis != null) {
				TarArchiveEntry te = tis.getNextTarEntry();
				while (te != null && !isValidName(te.getName()))
					te = tis.getNextTarEntry();
				return (te == null ? null : new ArchiveEntry(te));
			}
			if (zis != null) {
				try {
					ZipEntry ze = zis.getNextEntry();
					return (ze == null ? null : new ArchiveEntry(ze));
				} catch (ZipException e) {
					return new ArchiveEntry();
				}
			}
			if (zfiter != null) {
				try {
					ZipEntry ze = (zfiter.hasNext() ? zfiter.next() : null); 
					if (ze == null)
				      return null;
					is = zf.getInputStream(ze);					
					return new ArchiveEntry(ze);
				} catch (Exception e) {
					return new ArchiveEntry();
				}
			}
			if (dis != null) {
				ArchiveEntry e = dis.getNextEntry();
				while (e != null && !isValidName(e.getName()))
					e = dis.getNextEntry();
			}
			return null;
		}

		@Override
		public void close() throws IOException {
			if (dis != null)
				dis.close();
			if (is != null)
				is.close();
			if (zf != null) {
				zfiter = null;
				zfmap = null;
				zf.close();
			}
		}

		public InputStream getStream() {
			return is;
		}

		@Override
		public int read() throws IOException {
			return is.read();
		}

		@Override
		public int read(byte b[], int off, int len) throws IOException {
			try {
				return is.read(b, off, len);
			} catch (IOException e) {
				is.read(b, off, len);
				throw e;
			}
		}

	}

	public static class ExtractorResource {

		public int id, tempID;

		private String source;
		private String localSourceFile;

		public String rootPath;

		public FileList lstManifest;
		public FileList lstIgnored;
		public FileList lstAccepted;

		public boolean isDefaultStructurePath;

		public ExtractorResource(int id, String source, boolean isDefaultStructurePath) {
			this.id = id;
			this.source = source;
			this.isDefaultStructurePath = isDefaultStructurePath;
		}

		public String getLocalSourceFileName() {
			return localSourceFile;
		}

		public void setLocalSourceFileName(String name) {
			localSourceFile = name;
		}

		public void setLists(String rootPath, String ignore, String accept) {
			if (lstManifest != null)
				return;
			lstManifest = new FileList(rootPath, "manifest", null);
			lstIgnored = new FileList(rootPath, "ignored", null);
			lstAccepted = new FileList(rootPath, "accepted", null);
			if (ignore != null)
				lstIgnored.setAcceptPattern(ignore);
			if (accept != null)
				lstAccepted.setAcceptPattern(accept);
		}

		@Override
		public String toString() {
			return "[ExtractorSource " + getSourceFile() + " => " + rootPath + "]";
		}

		public String getSourceFile() {
			return (localSourceFile == null ? source : localSourceFile);
		}

		public void setTemp(String name) {
			localSourceFile = name;
			tempID = id;
		}

		public boolean isTempFile() {
			return tempID > 0;
		}

		public String createZipRootPath(String zipPath) {
			if (rootPath != null)
				return rootPath;
			if (tempID > 0) {
				zipPath = "resource" + tempID;
			}
			String rootPath = new File(zipPath).getName();
			if (rootPath.toLowerCase().endsWith(".zip") || rootPath.endsWith(".tgz") || rootPath.endsWith(".rar")
					|| rootPath.endsWith(".tar"))
				rootPath = rootPath.substring(0, rootPath.length() - 4);
			else if (rootPath.endsWith(".tar.gz"))
				rootPath = rootPath.substring(0, rootPath.length() - 7);
			this.rootPath = rootPath;
			return rootPath;
		}

		public String getAutomationPath(String originPath) {
			return rootPath + "/" + originPath;
		}
		public String getRemoteSource() {
			return (source == null ? localSourceFile : source);
		}

	}

	/**
	 * A static class that provides a byte array wrapper that allows using them to
	 * be keys in a HashMap.
	 * 
	 * Used here for checking if to structure files are identical. For example, two
	 * different structures pulled from two different pages of an MNova file.
	 * 
	 * 
	 * @author hansonr
	 *
	 */
	public static class AWrap {

		private byte[] a;

		public AWrap() {
		}

		public AWrap(byte[] bytes) {
			setBytes(bytes);
		}

		public void setBytes(byte[] bytes) {
			a = bytes;
		}

		@Override
		public boolean equals(Object o) {
			AWrap b = (AWrap) o;
			return Arrays.equals(a, b.a);
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(a);
		}

	}

	/**
	 * A static class to provide a temporary representation object for
	 * representations that have been found but do not have an object yet.
	 * 
	 * @author hansonr
	 *
	 */
	public static class CacheRepresentation extends IFDRepresentation {

		private String rezipOrigin;
		boolean isValid = true;

		public CacheRepresentation(IFDReference ifdReference, Object o, long len, String type, String mediaType) {
			super(ifdReference, o, len, type, mediaType);
		}

		public void setRezipOrigin(String path) {
			rezipOrigin = path;
		}

		public Object getRezipOrigin() {
			return rezipOrigin;
		}

	}
	
	public static File getJarFile(Class<?> classInJar) throws Exception {
		java.security.CodeSource codeSource = classInJar.getProtectionDomain().getCodeSource();
		File jarFile = null;
		if (codeSource.getLocation() != null) {
//      jarFile = new File(codeSource.getLocation().getPath());
			jarFile = new File(codeSource.getLocation().toURI());
		} else {
			String path = classInJar.getResource(classInJar.getSimpleName() + ".class").getPath(); //$NON-NLS-1$
			String jarFilePath = path.substring(path.indexOf(":") + 1, path.indexOf("!")); //$NON-NLS-1$ //$NON-NLS-2$
			jarFilePath = java.net.URLDecoder.decode(jarFilePath, "UTF-8"); //$NON-NLS-1$
			jarFile = new File(jarFilePath);
		}
		return jarFile;
	}

	
	/**
	 * Reject all names that start with __MACOSX or have /. in them.
	 * 
	 * @param name
	 * @return true to retain
	 */
	public static boolean isValidName(String name) {
		return (!name.startsWith("__MACOSX") && name.indexOf("/.") < 0);
	}


	/**
	 * Convert all Windows '\\' to UNIX '/' and add or remove '/' from the end of the name, as desired
	 * @param path
	 * @param asDir true to return path with trailing '/'
	 * @return fixed path
	 */
	public static String fixPath(String path, boolean asDir) {
		if (path == null)
			return null;
		path = path.replace('\\', '/');
		boolean hasSlash = path.endsWith("/");
		if (hasSlash == asDir)
			return path;
		if (asDir)
			path += "/";
		else
			path = path.substring(0, path.length() - 1);
		return path;
	}


	/**
	 * Convert '\\' and '/' to inderscore and remove any trailing '/'.
	 * 
	 * @param path
	 * @return revised path
	 */
	public static String pathToFileName(String path) {
		path = fixPath(path, false);
		return path.replace('\\', '_').replace('/', '_');
	}
}
