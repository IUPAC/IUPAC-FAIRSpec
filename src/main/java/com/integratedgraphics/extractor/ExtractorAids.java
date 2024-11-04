package com.integratedgraphics.extractor;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecExtractorHelper.FileList;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;
import org.iupac.fairdata.core.IFDReference;
import org.iupac.fairdata.core.IFDRepresentation;
import org.iupac.fairdata.util.ZipUtil;

import com.junrar.Archive;
import com.junrar.exception.RarException;
import com.junrar.rarfile.FileHeader;

/**
 * A set of static classes for use by MetadataExtractor, primarily
 * @author hansonr@stolaf.edu
 *
 */
public class ExtractorAids {

	/**
	 * A static class for parsing the object string and using regex to match
	 * filenames. This static class may be overridden to give it different
	 * capabilities.
	 * 
	 * @author hansonr
	 *
	 */
	
	public static class ObjectParser {
	
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
		private static final String TEMP_ANY_DIRECTORIES = REGEX_UNQUOTE + "(?:[^|/]+/)" + TEMP_STAR_CHAR
				+ REGEX_QUOTE;
	
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
	
		private ExtractorResource dataSource;
		private MetadataExtractor extractor;
		private boolean hasData;
		private List<Object> replacements;
	
		/**
		 * @param sObj
		 * @throws IFDException
		 */
		public ObjectParser(MetadataExtractor extractor, ExtractorResource resource, String sObj) throws IFDException {
			this.extractor = extractor;
			index = parserCount++;
			dataSource = resource;
			sData = sObj.substring(sObj.charAt(0) == '|' ? 1 : 0);
			init();
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
			// {IFD.representation.spec.nmr.vendor.dataset::{IFD.property.sample.label::*-*}-{IFD.property.dataobject.label::*}.jdf}
			//
			// becomes:
			//
			// ^(?<IFD0nmr0representation0vendor0dataset>(?<IFD0structure0param0compound0id>([^-](?:-[^-]+)*))\\Q-\\E(?<IFD0nmr0param0expt>.+)\\Q.jdf\\E)$
			//
			// {id=IFD.property.sample.label::*}.zip|{IFD.representation.spec.nmr.vendor.dataset::{id}_{IFD.property.dataobject.label::*}/}
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
	
			Matcher m;
			// *-* becomes \\E([^-]+(?:-[^-]+)*)\\Q and matches a-b-c
			if (s.indexOf("*") != s.lastIndexOf("*")) {
				while ((m = MetadataExtractor.pStarDotStar.matcher(s)).find()) {
					String schar = m.group(1);
					char c = schar.charAt(0);
					s = FAIRSpecUtilities.rep(s, "*" + schar + "*",
							TEMP_ANY_SEP_ANY_GROUPS.replaceAll("" + TEMP_ANY_SEP_ANY_CHAR2, "\\\\Q" + c + "\\\\E")
									.replace(TEMP_ANY_SEP_ANY_CHAR, c));
				}
			}
			// * becomes \\E.+\\Q
	
			s = FAIRSpecUtilities.rep(s, "*", REGEX_ANY_NOT_PIPE_OR_DIR);
	
			// {id=IFD.property.dataobject.label::xxx} becomes \\E(?<id>\\Qxxx\\E)\\Q
			// {IFD.property.dataobject.label::xxx} becomes
			// \\E(?<IFD0nmr0param0expt>\\Qxxx\\E)\\Q
			// <id> becomes \\k<id>
	
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
				Matcher m = MetadataExtractor.objectDefPattern.matcher(s);
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
				String key;
				if (pt > 0) {
					key = param.substring(0, pt);
					param = param.substring(pt + 1);
					if (extractor.htMetadata != null && extractor.htMetadata.containsKey(key)) {
						extractor.phase1SetMetadataTarget(key, param);
					}
				} else {
					key = param.replace('.', '0').replace('_', '1');
				}
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
					String rx = MetadataExtractor.getIFDExtractValue(s, "regex", pt);
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

		public void setReplacements(List<Object> replacements) {
			this.replacements = replacements;
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
	
		protected ArchiveEntry(String name, long size) {
			this.name = name;
			this.size = size;
		}
	
		protected ArchiveEntry(ZipEntry ze) {
			name = ze.getName();
			size = ze.getSize();
		}
	
		protected ArchiveEntry(TarArchiveEntry te) {
			name = te.getName();
			size = te.getSize();
		}
	
		protected ArchiveEntry(FileHeader fh) {
			name = fh.getFileName();
			size = fh.getUnpSize();
		}
	
		protected ArchiveEntry(String name) {
			this.name = name;
		}
	
		protected boolean isDirectory() {
			return name.endsWith("/");
		}
	
		protected String getName() {
			return name;
		}
	
		protected long getSize() {
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
			this.name = name.replace('\\', '/') + (isDir ? "/" : "");
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
	
		protected File dir;
		protected File[] list;
		protected int pt = -1, n;
		protected DirIterator iter;
	
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
			close();
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
					System.out.println("extracting " + fh.getFileName() + " " + bytes.length);
					is = new BufferedInputStream(new ByteArrayInputStream(bytes));
				} catch (Exception e) {
					throw new IOException(e);
				}
			}
			return is;
		}
	
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
		private InputStream is;
		private DirectoryInputStream dis;
		private RARInputStream ris;
	
		protected ArchiveInputStream() throws IOException {
			this(null, null);
		}
	
		ArchiveInputStream(InputStream is, String fname) throws IOException {
	
			if (is instanceof ArchiveInputStream)
				is = new BufferedInputStream(((ArchiveInputStream) is).getStream());
			if (is instanceof DirectoryInputStream) {
				this.is = dis = (DirectoryInputStream) is;
				dis.reset();
			} else if (is instanceof ZipInputStream) {
				this.is = zis = new ZipInputStream(is);
			} else if (ZipUtil.isGzipS(is)) {
				this.is = tis = ZipUtil.newTarGZInputStream(is);
			} else if (fname != null && fname.endsWith(".tar")) {
				this.is = tis = ZipUtil.newTarInputStream(is);
			} else if (fname != null && fname.endsWith(".rar")) {
				this.is = ris = new RARInputStream(is);
			}
		}
	
		/**
		 * Override this method to implement a custom archive reader.
		 * 
		 * @return
		 * @throws IOException
		 */
		protected ArchiveEntry getNextEntry() throws IOException {
			if (ris != null) {
				return ris.getNextEntry();
			}
			if (tis != null) {
				TarArchiveEntry te = tis.getNextTarEntry();
				return (te == null ? null : new ArchiveEntry(te));
			}
			if (zis != null) {
				ZipEntry ze = zis.getNextEntry();
				return (ze == null ? null : new ArchiveEntry(ze));
			}
			if (dis != null)
				return dis.getNextEntry();
			return null;
		}
	
		@Override
		public void close() throws IOException {
			if (dis != null)
				dis.close();
			if (is != null)
				is.close();
		}
	
		protected InputStream getStream() {
			return is;
		}
	
		@Override
		public int read() throws IOException {
			return is.read();
		}
	
		@Override
		public int read(byte b[], int off, int len) throws IOException {
			return is.read(b, off, len);
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
	
		protected String rezipOrigin;
		public boolean isMultiple;
	
		public CacheRepresentation(IFDReference ifdReference, Object o, long len, String type, String subtype) {
			super(ifdReference, o, len, type, subtype);
		}
	
		public void setRezipOrigin(String path) {
			rezipOrigin = path;
		}
	
		public Object getRezipOrigin() {
			return rezipOrigin;
		}
	
		public void setIsMultiple() {
			isMultiple = true;
		}
	
		public boolean isMultiple() {
			return isMultiple;
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
	
		public byte[] a;
	
		AWrap(byte[] b) {
			a = b;
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

	public static class ExtractorResource {
		public boolean isLocalStructures;
		public String source;
		public FileList lstManifest;
		public FileList lstIgnored;
		public FileList lstAccepted;
		public String rootPath;
		public String ifdResource;
		public String localSourceFile;
	
		public ExtractorResource(String source) {
			this.source = source;
			isLocalStructures = MetadataExtractor.isDefaultStructurePath(source);
		}
	
		public void setLists(String rootPath, String ignore, String accept) {
			if (lstManifest != null)
				return;
			lstManifest = new FileList(rootPath, "manifest");
			lstIgnored = new FileList(rootPath, "ignored");
			lstAccepted = new FileList(rootPath, "accepted");
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
	}

}
