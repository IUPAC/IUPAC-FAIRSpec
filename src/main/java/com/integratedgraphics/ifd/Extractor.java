package com.integratedgraphics.ifd;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
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
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.iupac.fairdata.api.IFDSerializerI;
import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecCompoundAssociation;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecCompoundCollection;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecExtractorHelper;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecExtractorHelper.FileList;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecExtractorHelperI;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecFindingAid;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities.SpreadsheetReader;
import org.iupac.fairdata.core.IFDAssociation;
import org.iupac.fairdata.core.IFDFindingAid;
import org.iupac.fairdata.core.IFDObject;
import org.iupac.fairdata.core.IFDReference;
import org.iupac.fairdata.core.IFDRepresentableObject;
import org.iupac.fairdata.core.IFDRepresentation;
import org.iupac.fairdata.dataobject.IFDDataObject;
import org.iupac.fairdata.dataobject.IFDDataObjectCollection;
import org.iupac.fairdata.dataobject.IFDDataObjectRepresentation;
import org.iupac.fairdata.extract.DefaultStructureHelper;
import org.iupac.fairdata.extract.ExtractorI;
import org.iupac.fairdata.extract.PropertyManagerI;
import org.iupac.fairdata.sample.IFDSample;
import org.iupac.fairdata.structure.IFDStructure;
import org.iupac.fairdata.util.IFDDefaultJSONSerializer;
import org.iupac.fairdata.util.JSJSONParser;
import org.iupac.fairdata.util.ZipUtil;

import com.integratedgraphics.ifd.api.VendorPluginI;
import com.integratedgraphics.ifd.util.PubInfoExtractor;

// TODO: check zipping Bruker directories into ZIP

/**
 * Copyright 2021/2022 Integrated Graphics and Robert M. Hanson
 * 
 * A class to handle the extraction of objects from a "raw" dataset by
 * processing the full paths within a ZIP file as directed by an extraction
 * template (from the extract/ folder for the test)
 * 
 * following the sequence:
 * 
 * initialize(ifdExtractScriptFile)
 * 
 * setLocalSourceDir(sourceDir)
 * 
 * setCachePattern(pattern)
 * 
 * setRezipCachePattern(pattern)
 * 
 * extractObjects(targetDir);
 * 
 * Features:
 * 
 * 
 * ... uses template-directed processing of full file paths
 * 
 * ... metadata property information is from
 * org.iupac.common.fairspec.properties
 * 
 * ... creates IFDFAIRSpecFindingAid objects ready for serialization
 * 
 * ... serializes using org.iupac.util.IFDDefaultJSONSerializer
 * 
 * ... zip files are processed recursively
 * 
 * ... zip files other than Bruker directories are unpacked
 * 
 * ... "broken" Bruker directories (those without a simple integer root path)
 * are corrected.
 * 
 * ... binary MNova files are scanned for metadata, PNG, and MOL files (only,
 * not spectra)
 * 
 * ... MNova metadata references page number in file using #page=
 * 
 * 
 * See ExtractorTest and IFDFAIRSpecExtractorHelper for more information.
 * 
 * @author hansonr
 *
 */
public class Extractor implements ExtractorI {

	// TODO: test rootpath and file lists for case with two root paths -- does it
	// make sense that that manifests are cleared?

	// TODO: update GitHub README.md
	
	

	protected static final String version = "0.0.4-alpha+2022.12.30";

	// 2023.01.01 version 0.0.4 accepts structures automatically from ./structures/ and ./structures.zip
	// 2022.12.30 version 0.0.4 ACS 0-7 with structures; fixing rezip issue of Bruker files placed in _IFD.ignored.json
	// 2022.12.29 version 0.0.4 ACS 0-4 with structures; fixing *-* Regex for ACS#4 acs.orglett.0c00788
	// 2022.12.27 version 0.0.4 ACS 0-2 working
	// 2022.12.27 version 0.0.4 introduces FAIRSpecCompoundAssociation
	// 2022.12.23 version 0.0.4 fixes from ACS testing, Bruker directories with multiple numbered subdirectories adds "-<n>" to the id
	// 2022.12.14 version 0.0.4 allows for local directory parsing (no zip or tar.gz)
	// 2022.12.13 verison 0.0.4 adds "EXIT" and comment-only "..." for IFD-extract.json
	// 2022.12.10 version 0.0.4 adds CDXML reading by Jmol and conversion of CIF to PNG along with Jmol 15.2.82 fixes for V3000 and XmlChemDrawReader
	// 2022.12.01 version 0.0.4 fixes multi-page MNova with compound association (ACS 22567817#./extract/acs.joc.0c00770)
	// 2022.11.29 version 0.0.4 allows for a representation to be both a structure and a data object
	// 2022.11.27 version 0.0.4 adds parameters from a Metadata file as XLSX or ODS
	// 2022.11.23 version 0.0.3 fixes missing properties in NMR; upgrades to
	// double-precision Jmol-SwingJS JmolDataD.jar
	// 2022.11.21 version 0.0.3 fixes minor details; ICL.v6, ACS.0, ACS.5 working
	// adds command-line arguments, distinguishes REJECTED and IGNORED
	// 2022.11.17 version 0.0.3 allows associations "byID"
	// 2022.11.14 version 0.0.3 "compound identifier" as organizing association
	// 2022.06.09 MNovaMetadataReader CDX export fails due to buffer pointer error.

	/**
	 * A static class for parsing the object string and using regex to match filenames.
	 * This static class may be overridden to give it different capabilities.
	 * 
	 * @author hansonr
	 *
	 */

	protected static class ObjectParser {

		protected static final String REGEX_QUOTE = "\\Q";
		protected static final String REGEX_UNQUOTE = "\\E";
		protected static final String REGEX_ANY_NOT_PIPE_OR_DIR = REGEX_UNQUOTE + "[^|/]+" + REGEX_QUOTE;
		protected static final String REGEX_KEYDEF_START = REGEX_UNQUOTE + "(?<";
		protected static final String REGEX_KEYVAL_START = REGEX_UNQUOTE + "\\k<";
		protected static final String REGEX_KV_END = ">" + REGEX_QUOTE;
		protected static final String REGEX_END_PARENS = REGEX_UNQUOTE + ")" + REGEX_QUOTE;
		protected static final String REGEX_EMPTY_QUOTE = REGEX_QUOTE + REGEX_UNQUOTE;

		protected static final String RAW_REGEX_TAG = "{regex::";

		protected static final char TEMP_RAW_IN_CHAR = '\0'; // --> <
		protected static final char TEMP_RAW_OUT_CHAR = '\1'; // --> >
		protected static final char TEMP_STAR_CHAR = '\2'; // --> *
		protected static final char TEMP_KEYVAL_IN_CHAR = '\3'; // --> <
		protected static final char TEMP_KEYVAL_OUT_CHAR = '\4'; // --> >
		protected static final char TEMP_ANY_SEP_ANY_CHAR = '\5'; // see below
		protected static final char TEMP_ANY_SEP_ANY_CHAR2 = '\6'; // see below
		protected static final char TEMP_IGNORE = '\7'; //  \\ removed

		protected static final String TEMP_KEYVAL_IN = REGEX_UNQUOTE + "(?" + TEMP_KEYVAL_IN_CHAR;

		protected static final String TEMP_KEYVAL_OUT = TEMP_KEYVAL_OUT_CHAR + REGEX_QUOTE;

		/**
		 * multiple separations by char. 
		 * for example *-*.zip -->
		 */
		protected static final String TEMP_ANY_SEP_ANY_GROUPS = REGEX_UNQUOTE + "(" + "[^|/\5]+(?:\6[^|/\5]+)"
				//+ TEMP_STAR_CHAR
				+ "+"
				+ ")"
				+ REGEX_QUOTE;

		/**
		 * 		// /**\/ --> "/\E(?:[^|/]+/)*\Q"  [ backslash after two asterisks only for this comment ] 
		 *
		 * one or more directories; 
		 * 
		 * test/**\/*.zip  matches test/xxx.zip or test/bbb/aaa/xxx.zip
		 */
		protected static final String TEMP_ANY_DIRECTORIES = REGEX_UNQUOTE + "(?:[^|/]+/)" + TEMP_STAR_CHAR + REGEX_QUOTE;

		protected static final char BACK_SLASH_IGNORED = '\\';
		
		protected static int parserCount;
		
		protected final int index;

		protected int getIndex() {
			return index;
		}
		
		protected String sData;

		protected Pattern p;

		protected List<String> regexList;

		protected Map<String, String> keys;

		protected ExtractorSource dataSource;
		protected Extractor extractor;
		protected List<String[]> assignments;
		protected boolean hasData;
		protected List<Object> replacements;

		/**
		 * @param sObj
		 * @throws IFDException
		 */
		protected ObjectParser(Extractor extractor, ExtractorSource resource, String sObj) throws IFDException {
			this.extractor = extractor;
			this.index = parserCount++;
			dataSource = resource;
			sData = sObj.substring(sObj.charAt(0) == '|' ? 1 : 0); 
			init();
		}

		protected void addAssignment(String val) throws IFDException {
			if (assignments == null)
				assignments = new ArrayList<>();
			int pt = val.indexOf("::") - 1;
			if (pt < 0)
				throw new IFDException(val + " is not of the form xx.xx.xx::definition");
			val = val.substring(1, val.length() - 1);
			String prop = val.substring(0, pt);
			val = val.substring(pt + 2);
			assignments.add(new String[] { prop, val });
		}

		/**
		 * Prepare pattern and match.
		 * 
		 * @throws IFDException
		 * 
		 * 
		 */
		protected void init() throws IFDException {
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
			// it should only be used to break up something like *\-* to be literally a single *-*, not "any number of "-"
			s = s.replace(BACK_SLASH_IGNORED, TEMP_IGNORE);

			// **/ becomes \\E(?:[^/]+/)*\\Q

			s = FAIRSpecUtilities.rep(s, "**/", TEMP_ANY_DIRECTORIES);

			Matcher m;
			// *-* becomes \\E([^-]+(?:-[^-]+)*)\\Q and matches a-b-c
			if (s.indexOf("*") != s.lastIndexOf("*")) {
				while ((m = pStarDotStar.matcher(s)).find()) {
					String schar = m.group(1);
					char c = schar.charAt(0);
					s = FAIRSpecUtilities.rep(s, "*" + schar + "*",
							TEMP_ANY_SEP_ANY_GROUPS
									.replaceAll("" + TEMP_ANY_SEP_ANY_CHAR2, "\\\\Q" + c + "\\\\E")
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
		protected String compileIFDDefs(String s, boolean isFull, boolean replaceK) throws IFDException {
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
		protected String protectRegex(String s) throws IFDException {
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

		@Override
		public String toString() {
			return "[ObjectParser " + this.sData + "]";
		}

		protected Matcher match(String origin) throws IFDException {
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

	}

	protected class ParserIterator implements Iterator<ObjectParser> {

		boolean byResource;
		int i;
		
		ParserIterator() {
			extractorSource = null;
		}
		@Override
		public boolean hasNext() {
			return (i < objectParsers.size());
		}

		@Override
		public ObjectParser next() {
			ObjectParser parser = objectParsers.get(i++);
			if (parser.dataSource != extractorSource) {
				try {
					extractorSource = phase2InitializeSource(parser.dataSource);
				} catch (IFDException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return parser;
		}
		
	}
	/**
	 * A static class to cover both ZipEntry and TAR entries.
	 * 
	 * @author hansonr
	 *
	 */
	protected static class ArchiveEntry {

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

	protected static class DirectoryEntry extends ArchiveEntry {

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
			return (bis != null ? bis: isDir ? null : (bis = new BufferedInputStream(new FileInputStream(file))));
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
	
	protected static class DirIterator implements Iterator<File> {

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
	 * @author hansonr
	 *
	 */
	protected static class DirectoryInputStream extends InputStream {

		
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
	 * A static class to allow for either ZipInputStream or TarArchiveInputStream
	 * 
	 * @author hansonr
	 *
	 */
	protected static class ArchiveInputStream extends InputStream {
		protected ZipInputStream zis;
		protected TarArchiveInputStream tis;
		protected InputStream is;
		protected DirectoryInputStream dis;
		

		ArchiveInputStream(InputStream is) throws IOException {

			if (is instanceof ArchiveInputStream)
				is = new BufferedInputStream(((ArchiveInputStream) is).getStream());
			if (is instanceof DirectoryInputStream) {
				this.is = dis = (DirectoryInputStream) is;
				dis.reset();
			} else if (ZipUtil.isGzipS(is)) {
				this.is = tis = ZipUtil.newTarInputStream(is);
			} else {
				this.is = zis = new ZipInputStream(is);
			}
		}
		
		protected ArchiveEntry getNextEntry() throws IOException {
			if (tis != null) {
				TarArchiveEntry te = tis.getNextTarEntry();
				return (te == null ? null : new ArchiveEntry(te));
			}
			if (zis != null) {
				ZipEntry ze = zis.getNextEntry();
				return (ze == null ? null : new ArchiveEntry(ze));
			}
			return dis.getNextEntry();
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
	 * A static class to provide a temporary representation object for representations that have been found but do not have an object yet.
	 * 
	 * @author hansonr
	 *
	 */
	protected static class CacheRepresentation extends IFDRepresentation {

		protected String rezipOrigin;
		protected boolean isMultiple;

		protected CacheRepresentation(IFDReference ifdReference, Object o, long len, String type, String subtype) {
			super(ifdReference, o, len, type, subtype);
		}

		protected void setRezipOrigin(String path) {
			rezipOrigin = path;
		}

		protected Object getRezipOrigin() {
			return rezipOrigin;
		}

		protected void setIsMultiple() {
			isMultiple = true;
		}
		
		protected boolean isMultiple() {
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
	protected static class AWrap {

		protected byte[] a;

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

	protected static class ExtractorSource {
		String source;
		FileList lstManifest;
		FileList lstIgnored;
		protected String rootPath;
		
		ExtractorSource(String source) {
			this.source = source;
		}
		
		void setLists(String rootPath, String ignore) {
			if (lstManifest != null)
				return;
			lstManifest = new FileList(rootPath, "manifest");
			lstIgnored = new FileList(rootPath, "ignored");
			if (ignore != null)
				lstIgnored.accept(ignore);
		}
	}

	static {
		FAIRSpecFindingAid.loadProperties();
		VendorPluginI.init();
	}

	/**
	 * This 
	 */
	protected final static String SUBST = "=>";
	
	protected static final String codeSource = "https://github.com/IUPAC/IUPAC-FAIRSpec/blob/main/src/main/java/com/integratedgraphics/ifd/Extractor.java";

	protected static final int LOG_REJECTED = 0;
	protected static final int LOG_IGNORED = 1;
	protected static final int LOG_OUTPUT = 2;

	protected final static int PHASE_2A = 1;
	protected final static int PHASE_2B = 2;
	protected final static int PHASE_2C = 3;
	protected final static int PHASE_2D = 4;


	/**
	 * a key for the deferredObjectList that flags a structure with a spectrum;
	 * needs attention, as this was created prior to the idea of a compound association, 
	 * and it presumes there are no such associations. 
	 * 
	 */
	public static final String NEW_PAGE_KEY = "*NEW_PAGE*";

	protected final static Pattern objectDefPattern = Pattern.compile("\\{([^:]+)::([^}]+)\\}");
	protected final static Pattern pStarDotStar = Pattern.compile("\\*([^|/])\\*");

	/**
	 * value to substitute for null from vendors
	 */
	public static final Object NULL = "\1";

	protected static final String DEFAULT_STRUCTURE_DIR_URI = "./structures/";
	protected static final String DEFAULT_STRUCTURE_ZIP_URI = "./structures.zip";


	Map<String, Object> config = null;

	/**
	 * start-up option to create JSON list for multiple
	 */
	protected boolean stopOnAnyFailure;
	protected boolean debugReadOnly;

	protected boolean debugging = false;
	protected boolean readOnly = false;

	/**
	 * set true to only create finding aides, not extract file data
	 */
	protected boolean createFindingAidOnly = false;

	/**
	 * set true to allow failure to create pub info
	 */
	protected boolean allowNoPubInfo = true;

	/**
	 * don't even try to read pub info -- debugging
	 */
	protected boolean skipPubInfo = false;

	/**
	 * set to true add the source metadata from Crossref or DataCite
	 */
	protected boolean addPublicationMetadata = false;

	/**
	 * set true to zip up the extracted collection, placing that in the target
	 * directory
	 */
	protected boolean createZippedCollection = true;

//	protected String logfile;

	/**
	 * the finding aid - only one per instance
	 */
	protected FAIRSpecExtractorHelperI helper;

	/**
	 * the IFD-extract.json script
	 */
	protected String extractScript;

	/**
	 * extract version from IFD-extract.json
	 */
	protected String extractVersion;

	/**
	 * objects found in IFD-extract.json
	 */
	protected List<ObjectParser> objectParsers;

	/**
	 * an optional local source directory to use instead of the one indicated in
	 * IFD-extract.json
	 */
	protected String localSourceDir;

	/**
	 * a required target directory
	 */
	protected File targetDir;

	/**
	 * files matched will be cached in the target directory
	 */
	protected Pattern vendorCachePattern;

	/**
	 * vendors have supplied cacheRegex patterns
	 */
	protected boolean cachePatternHasVendors;

	protected ArrayList<Object> rootPaths = new ArrayList<>();
	/**
	 * working local name, without the rootPath, as found in _IFD_manifest.json
	 */
	protected String localizedName;

	/**
	 * working origin path while checking zip files
	 * 
	 */
	protected String originPath;

	/**
	 * rezip data saved as an ISFRepresentation (for no particularly good reason)
	 */
	protected CacheRepresentation currentRezipRepresentation;

	/**
	 * path to this resource in the original zip file
	 */
	protected String currentRezipPath;

	/**
	 * vendor association with this rezipping
	 */
	protected VendorPluginI currentRezipVendor;

	/**
	 * last path to this rezip top-level resource
	 */
	protected String lastRezipPath;

	/**
	 * the number of bytes extracted
	 */
	protected long extractedByteCount;

	/**
	 * the number of IFDObjects created
	 */
	protected int ifdObjectCount;

	/**
	 * cache of top-level resources to be rezipped
	 */
	protected List<CacheRepresentation> rezipCache;

	/**
	 * Track the files written to the collection, even if there is no output. This
	 * allows for removing ZIP files from the finding aid and manifest if they are
	 * not actually written.
	 */
	protected FileList lstWritten = new FileList(null, "written");
	
	/**
	 * list of files extracted
	 */
	protected FileList lstManifest;

	/**
	 * list of files ignored -- probably MACOSX trash or Google desktop.ini trash
	 */
	protected FileList lstIgnored;

	/**
	 * list of files rejected -- probably MACOSX trash or Google desktop.ini trash
	 */
	protected final FileList lstRejected = new FileList(null, "rejected");

	/**
	 * working map from manifest names to structure or data object
	 */
	protected Map<String, IFDRepresentableObject<?>> htLocalizedNameToObject = new LinkedHashMap<>();

	/**
	 * working map from manifest names to structure or data object
	 */
	protected Map<String, String> htZipRenamed = new LinkedHashMap<>();

	/**
	 * working memory cache of representations keyed to their localized name
	 * (possibly with an extension for a page within the representation, such as an
	 * MNova file. These are identified by vendors and that can create additional
	 * properties or representations from them in Phase 2b that will need to be
	 * processed in Phase 2c.
	 */
	protected Map<String, CacheRepresentation> vendorCache;

	/**
	 * a list of properties that vendors have indicated need addition, keyed by the
	 * zip path for the resource
	 */
	protected List<Object[]> deferredPropertyList;

	
	/**
	 * the URL to the original source of this data, as indicated in IFD-extract.json
	 * as
	 */
	protected ExtractorSource extractorSource;

	/**
	 * bitset of activeVendors that are set for rezipping -- probably 1
	 */
	protected BitSet bsRezipVendors = new BitSet();

	/**
	 * bitset of activeVendors that are set for property parsing
	 */
	protected BitSet bsPropertyVendors = new BitSet();

	/**
	 * files matched will be cached as zip files
	 */
	protected Pattern rezipCachePattern;

	/**
	 * the structure property manager for this extractor
	 * 
	 */
	protected PropertyManagerI structurePropertyManager;

	/**
	 * produce no output other than a log file
	 */
	protected boolean noOutput;

	/**
	 * include ignored files in FAIRSpec collection
	 */

	protected boolean includeIgnoredFiles = true;

	protected String localizedTopLevelZipURL;

	protected boolean haveExtracted;

	protected String ifdid = "";

	protected Map<AWrap, IFDStructure> htStructureRepCache;

	String strWarnings = "";
	
	protected int warnings;

	public int getWarningCount() {
		return warnings;
	}

	protected int errors;

	protected File currentZipFile;

	protected Map<String, Map<String, Object>> htMetadata;

	protected File extractscriptFile;

	protected String userStructureFilePattern;

	protected Map<String, ExtractorSource> htResources = new HashMap<>();

	/**
	 * Slows this down a bit, but allows, for example, a CIF file to 
	 * be both a structure and an object
	 */
	protected boolean allowMultipleObjectsForRepresentations = true;

	protected String ignoreRegex;

	protected boolean isByID;

	protected List<FileList> rootLists; 
	protected String resourceList;


	public int getErrorCount() {
		return errors;
	}

	public Extractor() {
		setConfiguration();
		setDefaultRunParams();
		getStructurePropertyManager();
	}

	protected void setConfiguration() {
		try {
			config = FAIRSpecUtilities.getJSONResource(Extractor.class, "extractor.config.json");
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}

	public void run(String key, File ifdExtractScriptFile, File targetDir, String localsourceArchive)
			throws IOException, IFDException {
		log("!Extractor\n ifdExtractScriptFIle= " + ifdExtractScriptFile + "\n localsourceArchive = "
				+ localsourceArchive + "\n targetDir = " + targetDir.getAbsolutePath());

		String findingAidFileName = (key == null ? "" : key);

		if (extractAndCreateFindingAid(ifdExtractScriptFile, localsourceArchive, targetDir, findingAidFileName) == null
				&& !allowNoPubInfo) {
			throw new IFDException("Extractor failed");
		}

		log("!Extractor extracted " + lstManifest.size() + " files (" + lstManifest.getByteCount() + " bytes)"
				+ "; ignored " + lstIgnored.size() + " files (" + lstIgnored.getByteCount() + " bytes)" + "; rejected "
				+ lstRejected.size() + " files (" + lstRejected.getByteCount() + " bytes)"

		);
	}

	/**
	 * @return the FindingAid as a string
	 */
	public final String extractAndCreateFindingAid(File ifdExtractScriptFile, String localArchive, File targetDir,
			String findingAidFileNameRoot) throws IOException, IFDException {

		// set up the extraction
		
		processPhase1(ifdExtractScriptFile, localArchive);
		FAIRSpecUtilities.refreshLog();

		// now actually do the extraction.

		processPhase2(targetDir);
		FAIRSpecUtilities.refreshLog();

		// finish up all processing
		return processPhase3(findingAidFileNameRoot);
	}

	protected boolean processPhase1(File ifdExtractScriptFile, String localArchive) throws IOException, IFDException {
		// first create objects, a List<String>
		this.extractscriptFile = ifdExtractScriptFile;
		phase1GetObjectParsersForFile(ifdExtractScriptFile);
		if (!phase1ProcessPubURI())
			return false;
		phase1SetLocalSourceDir(localArchive);
		// options here to set cache and rezip options -- debugging only!
		phase1SetCachePattern(userStructureFilePattern);
		rezipCachePattern = phase1SetRezipCachePattern(null, null);
		return true;
	}

	protected void phase1SetMetadataTarget(String key, String param) {
		// TODO Auto-generated method stub extractor.checkForMetadata
		Map<String, Object> pm = htMetadata.remove(key);
		if (pm == null)
			return;
		// switch key to object id key
		log("!Extractor METADATA FOR " + key + " set to " + param);
		htMetadata.put(param, pm);
		loadMetadata(param, pm);
	}

	protected boolean phase1ProcessPubURI() throws IOException {
		String puburi = null;
		Map<String, Object> pubCrossrefInfo = null;
		puburi = (String) helper.getFindingAid()
				.getPropertyValue(IFDConst.IFD_PROPERTY_COLLECTIONSET_SOURCE_PUBLICATION_URI);
		if (puburi != null && !skipPubInfo) {
			pubCrossrefInfo = PubInfoExtractor.getPubInfo(puburi, addPublicationMetadata);
			if (pubCrossrefInfo == null || pubCrossrefInfo.get("title") == null) {
				if (skipPubInfo) {
					logWarn("skipPubInfo == true; Finding aid does not contain PubInfo", "extractAndCreateFindingAid");
				} else {
					if (!allowNoPubInfo) {
						logErr("Finding aid does not contain PubInfo! No internet? cannot continue",
								"extractAndCreateFindingAid");
						return false;
					}
					logWarn("Could not access " + PubInfoExtractor.getCrossrefMetadataUrl(puburi),
							"extractAndCreateFindingAid");
				}
			} else {
				List<Map<String, Object>> list = new ArrayList<>();
				list.add(pubCrossrefInfo);
				helper.getFindingAid().setRelatedTo(list);
			}
		}
		return true;
	}

	/**
	 * Implementing subclass could use a different serializer.
	 * 
	 * @return a serializer
	 */
	protected IFDSerializerI getSerializer() {
		return new IFDDefaultJSONSerializer(isByID);
	}

	protected void phase1SetLocalSourceDir(String sourceDir) {
		if (sourceDir != null && sourceDir.indexOf("://") < 0)
			sourceDir = "file:///" + sourceDir.replace('\\', '/');
		this.localSourceDir = sourceDir;
	}

	///////// ExtractorI methods /////////

	@Override
	public String getVersion() {
		return version;
	}
	
	@Override
	public String getCodeSource() {
		return codeSource;
	}
	
	@Override
	public IFDFindingAid getFindingAid() {
		return helper.getFindingAid();
	}

	/**
	 * Set the regex string assembling all vendor requests.
	 * 
	 * Each vendor's pattern will be surrounded by (?<param0> ... ), (?<param1> ...
	 * ), etc.
	 * 
	 * Here we wrap them all with (?<param>....), then add on our non-vendor checks,
	 * and finally wrap all this using (?<type>...).
	 * 
	 * This includes structure representations handled by DefaultStructureHelper.
	 * 
	 */
	protected void phase1SetCachePattern(String sp) {
		if (sp == null) {
			sp = FAIRSpecExtractorHelper.defaultCachePattern + "|" + structurePropertyManager.getParamRegex();
		} else if (sp.length() == 0) {
			sp = "(?<img>\n)|(?<struc>\n)";
		}

		String s = "";
		for (int i = 0; i < VendorPluginI.activeVendors.size(); i++) {
			String cp = VendorPluginI.activeVendors.get(i).vcache;
			if (cp != null) {
				bsPropertyVendors.set(i);
				s += "|" + cp;
			}
		}

		if (s.length() > 0) {
			s = "(?<param>" + s.substring(1) + ")|" + sp;
			cachePatternHasVendors = true;
		} else {
			s = sp;
		}
		vendorCachePattern = Pattern.compile("(?<ext>" + s + ")");
		vendorCache = new LinkedHashMap<String, CacheRepresentation>();
	}

	/**
	 * The regex pattern uses param0, param1, etc., to indicated parameters for
	 * different vendors. This method looks through the activeVendor list to
	 * retrieve the match, avoiding throwing any regex exceptions due to missing
	 * group names.
	 * 
	 * (Couldn't Java have supplied a check method for group names?)
	 * 
	 * @param m
	 * @return
	 */
	protected PropertyManagerI getPropertyManager(Matcher m) {
		if (m.group("struc") != null)
			return structurePropertyManager;
		for (int i = bsPropertyVendors.nextSetBit(0); i >= 0; i = bsPropertyVendors.nextSetBit(i + 1)) {
			String ret = m.group("param" + i);
			if (ret != null && ret.length() > 0) {
				return VendorPluginI.activeVendors.get(i).vendor;
			}
		}
		return null;
	}

	/**
	 * Set the file match zip cache pattern.
	 * 
	 * @param procs
	 * @param toExclude
	 * @return the rezip pattern
	 */
	protected Pattern phase1SetRezipCachePattern(String procs, String toExclude) {
		String s = "";
		for (int i = 0; i < VendorPluginI.activeVendors.size(); i++) {
			String cp = VendorPluginI.activeVendors.get(i).vrezip;
			if (cp != null) {
				bsRezipVendors.set(i);
				s = s + "|" + cp;
			}
		}
		s += (procs == null ? "" : "|" + procs);
		return (s.length() == 0 ? null : Pattern.compile(s.substring(1)));
	}

	/**
	 * Get all {object} data from IFD-extract.json.
	 * 
	 * @param ifdExtractScript
	 * @return list of {objects}
	 * @throws IOException
	 * @throws IFDException
	 */
	protected List<ObjectParser> phase1GetObjectParsersForFile(File ifdExtractScript) throws IOException, IFDException {
		log("!Extracting " + ifdExtractScript.getAbsolutePath());
		return phase1GetObjectsForStream(ifdExtractScript.toURI().toURL().openStream());
	}

	/**
	 * Get all {object} data from IFD-extract.json.
	 * 
	 * @param ifdExtractScript
	 * @return list of {objects}
	 * @throws IOException
	 * @throws IFDException
	 */
	protected List<ObjectParser> phase1GetObjectsForStream(InputStream is) throws IOException, IFDException {
		extractScript = new String(FAIRSpecUtilities.getLimitedStreamBytes(is, -1, null, true, true));
		objectParsers = phase1ParseScript(extractScript);
		return objectParsers;
	}

	/**
	 * Parse the script form an IFD-extract.js JSON file starting with the creation
	 * of a Map by JSJSONParser.
	 * 
	 * @param script
	 * @return parsed list of objects from an IFD-extract.js JSON
	 * @throws IOException
	 * @throws IFDException
	 */
	@SuppressWarnings("unchecked")
	protected List<ObjectParser> phase1ParseScript(String script) throws IOException, IFDException {
		if (helper != null)
			throw new IFDException("Only one finding aid per instance of Extractor is allowed (for now).");

		helper = newExtractionHelper();

		Map<String, Object> jsonMap = (Map<String, Object>) new JSJSONParser().parse(script, false);
		if (debugging)
			log(jsonMap.toString());
		extractVersion = (String) jsonMap.get(FAIRSpecExtractorHelper.FAIRSPEC_EXTRACT_VERSION);
		if (logging())
			log(extractVersion);
		List<Object> scripts = (List<Object>) jsonMap.get("keys");
		if (config != null) {
			List<Object> defaultScripts = (List<Object>) config.get("defaultScripts");
			if (defaultScripts != null) {
				scripts.addAll(defaultScripts);
			}
		}
		List<ObjectParser> objectParsers = phase1GetObjectParsers(scripts);
		if (logging())
			log(objectParsers.size() + " extractor regex strings");

		log("!license: "
				+ helper.getFindingAid().getPropertyValue(IFDConst.IFD_PROPERTY_COLLECTIONSET_SOURCE_DATA_LICENSE_NAME)
				+ " at "
				+ helper.getFindingAid().getPropertyValue(IFDConst.IFD_PROPERTY_COLLECTIONSET_SOURCE_DATA_LICENSE_URI));

		return objectParsers;
	}

	protected FAIRSpecExtractorHelper newExtractionHelper() throws IFDException {
		return new FAIRSpecExtractorHelper((ExtractorI) this, codeSource + " " + version);
	}

	/**
	 * Make all variable substitutions in IFD-extract.js.
	 * 
	 * @return list of ObjectParsers that have successfully parsed the {object}
	 *         lines of the file
	 * @throws IFDException
	 */
	@SuppressWarnings("unchecked")
	protected List<ObjectParser> phase1GetObjectParsers(List<Object> jsonArray) throws IFDException {

		// input:

		// {"FAIRSpec.extract.version":"0.2.0-alpha","keys":[
		// {"example":"compound directories containing unidentified bruker files and
		// hrms zip file containing .pdf"},
		// {"journal":"acs.orglett"},{"hash":"0c00571"},
		// {"figshareid":"21975525"},
		//
		// {"IFDid=IFD.property.collectionset.id":"{journal}.{hash}"},
		// {"IFD.property.collectionset.source.publication.uri":"https://doi.org/10.1021/{IFDid}"},
		// {"IFD.property.collectionset.source.data.license.uri":"https://creativecommons.org/licenses/by-nc/4.0"},
		// {"IFD.property.collectionset.source.data.license.name":"cc-by-nc-4.0"},
		//
		// {"data0":"{IFD.property.collectionset.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/{IFDid}/suppl_file/ol{hash}_si_002.zip}"},
		// {"data":"{IFD.property.collectionset.source.data.uri::https://ndownloader.figshare.com/files/{figshareid}}"},
		//
		// {"path":"{data}|FID for Publication/{id=IFD.property.sample.label::*}.zip|"},
		// {"FAIRSpec.extractor.object":"{path}{IFD.representation.dataobject.fairspec.nmr.vendor.dataset::{IFD.property.label::<id>/{xpt=::*}}.zip|{xpt}/*/}"},
		// {"FAIRSpec.extractor.object":"{path}<id>/{IFD.representation.structure.mol.2d::<id>.mol}"},
		// {"FAIRSpec.extractor.object":"{path}{IFD.representation.dataobject.fairspec.hrms.document::{IFD.property.label::<id>/HRMS.zip|**/*}.pdf}"}
		// ]}

		List<String> keys = new ArrayList<>();
		List<String> values = new ArrayList<>();
		List<ObjectParser> parsers = new ArrayList<>();
		List<Object> ignored = new ArrayList<>();
		List<Object> rejected = new ArrayList<>();
		ExtractorSource source = null;
		boolean isDefaultStructurePath = false;
		List<Object> replacements = null;

		for (int i = 0; i < jsonArray.size(); i++) {
			Object o = jsonArray.get(i);

			// all aspects here are case-sensitive

			// simple strings are ignored, except for "EXIT"

			if (o instanceof String) {
				if (o.equals(FAIRSpecExtractorHelper.EXIT))
					break;
				// ignore all other strings
				continue;
			}

			Map<String, Object> directives = (Map<String, Object>) o;

			for (Entry<String, Object> e : directives.entrySet()) {

				// {"IFDid=IFD.property.collectionset.id":"{journal}.{hash}"},
				// ..-----------------key---------------...------val-------.

				String key = e.getKey();
				if (key.startsWith("#"))
					continue;
				o = e.getValue();

				// non-String values

				if (key.equals(FAIRSpecExtractorHelper.FAIRSPEC_EXTRACTOR_METADATA)) {
					if (o instanceof Map) {
						phase1ProcessMetadataElement(o);
					} else if (o instanceof List) {
						for (Object m : (List<Object>) o) {
							phase1ProcessMetadataElement(m);
						}
					} else {
						logWarn("extractor template METADATA element is not a map or array",
								"Extractor.getObjectParsers");
					}
					continue;
				}

				if (key.equals(FAIRSpecExtractorHelper.FAIRSPEC_EXTRACTOR_REPLACEMENTS)) {
					replacements = (List<Object>) o;
					continue;
				}

				if (key.equals(FAIRSpecExtractorHelper.FAIRSPEC_EXTRACTOR_REJECT)) {
					if (o instanceof String) {
						rejected.add(o);
					} else {
						rejected.addAll((List<Object>) o);
					}
					continue;
				}

				if (key.equals(FAIRSpecExtractorHelper.FAIRSPEC_EXTRACTOR_IGNORE)) {
					if (o instanceof String) {
						ignored.add(o);
					} else {
						ignored.addAll((List<Object>) o);
					}
					continue;
				}
				// String values

				String val = o.toString();
				if (val.indexOf("{") >= 0) {
					String s = FAIRSpecUtilities.replaceStrings(val, keys, values);
					if (!s.equals(val)) {
						if (debugging)
							log(val + "\n" + s + "\n");
						e.setValue(s);
					}
					val = s;
				}
				log("!" + key + " = " + val);
				String keyDef = null;
				int pt = key.indexOf("=");
				if (pt > 0) {
					keyDef = key.substring(0, pt);
					key = key.substring(pt + 1);
				}
				// {"IFDid=IFD.property.collectionset.id":"{journal}.{hash}"},
				// ..keydef=-----------------key--------

				if (key.equals(IFDConst.IFD_PROPERTY_COLLECTIONSET_SOURCE_DATA_URI)) {
					if (!phase1CheckSource(val)) {
						source = null;
						isDefaultStructurePath = (DEFAULT_STRUCTURE_DIR_URI.equals(val)
								|| DEFAULT_STRUCTURE_ZIP_URI.equals(val));
						continue;
					}
					source = htResources.get(val);
					if (source == null)
						htResources.put(val, source = new ExtractorSource(val));
					continue;
				}

				if (key.equals(FAIRSpecExtractorHelper.FAIRSPEC_EXTRACTOR_OBJECT)) {
					if (source == null) {
						if (isDefaultStructurePath)
							continue;
						throw new IFDException(
								IFDConst.IFD_PROPERTY_COLLECTIONSET_SOURCE_DATA_URI + " was not set before " + val);
					}
					ObjectParser parser = newObjectParser(source, val);
					parser.replacements = replacements;
					parsers.add(parser);
					continue;
				}
				if (key.startsWith(FAIRSpecExtractorHelper.FAIRSPEC_EXTRACTOR_OPTION_FLAG)
						|| key.equals(FAIRSpecExtractorHelper.FAIRSPEC_EXTRACTOR_OPTIONS)) {
					setExtractorOption(key, val);
					continue;
				}
				if (key.startsWith(IFDConst.IFD_PROPERTY_FLAG)) {
					if (key.equals(IFDConst.IFD_PROPERTY_COLLECTIONSET_ID)) {
						ifdid = val;
						helper.getFindingAid().setID(val);
					}
					if (key.equals(IFDConst.IFD_PROPERTY_COLLECTIONSET_BYID)) {
						setExtractorOption(key, val);
						continue;
					}
					helper.getFindingAid().setPropertyValue(key, val);
					if (keyDef == null)
						continue;
				}

				// custom definition
				keys.add(0, "{" + (keyDef == null ? key : keyDef) + "}");
				values.add(0, val);
			}
		}

		String s = "";
		if (rejected.size() > 0) {
			for (int i = 0; i < rejected.size(); i++) {
				s += "(" + rejected.get(i) + ")|";
			}
		}
		lstRejected.setAcceptPattern(s + FAIRSpecExtractorHelper.junkFilePattern);
		if (ignored.size() > 0) {
			s = "";
			for (int i = 0; i < ignored.size(); i++) {
				s += "|(" + ignored.get(i);
			}
			ignoreRegex = s.substring(1);
		} else {
			ignoreRegex = null;
		}
		return parsers;
	}

	protected boolean phase1CheckSource(String val) throws IFDException {
		val = localizeURL(val);
		if (!val.startsWith("file:/"))
			return true;
		File zipFile = new File(val.substring(6));
		if (zipFile.exists())
			return true;
		logWarn("local source directory does not exist (ignored): " + zipFile, "phase1CheckSource");
		return false;
	}

	protected void phase1ProcessMetadataElement(Object m) throws IFDException {
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) m;
		String key = (String) map.get("FOR");
		if (key == null) {
			throw new IFDException("extractor template METADATA element does not contain 'FOR' key in " + m);
		}
		if (!map.containsKey("METADATA_FILE")) {
			throw new IFDException("extractor template METADATA_FILE was not found for " + m);
		}
		if (htMetadata == null)
			htMetadata = new HashMap<String, Map<String, Object>>();
		htMetadata.put(key, map);
		if (key.startsWith("IFD."))
			loadMetadata(key, map);
	}

	protected void setExtractorOption(String key, String val) {
		if (key.equals(IFDConst.IFD_PROPERTY_COLLECTIONSET_BYID)) {
			isByID = val.equalsIgnoreCase("true");
			helper.setById(isByID);
	    } else {
			checkFlags(val);
		}
	}

	///////// PHASE 2: Parsing the ZIP file and extracting objects from it ////////

	/**
	 * Find and extract all objects of interest from a ZIP file.
	 * 
	 */
	protected void processPhase2(File targetDir) throws IFDException, IOException {
		if (haveExtracted)
			throw new IFDException("Only one extraction per instance of Extractor is allowed (for now).");
		haveExtracted = true;
		if (targetDir == null)
			throw new IFDException("The target directory may not be null.");
		this.targetDir = targetDir;
		setupTargetDir();

		// String s = "test/ok/here/1c.pdf"; // test/**/*.pdf
		// Pattern p = Pattern.compile("^\\Qtest\\E/(?:[^/]+/)*(.+\\Q.pdf\\E)$");
		// Matcher m = p.matcher(s);
		// log(m.find() ? m.groupCount() + " " + m.group(0) + " -- " + m.group(1) : "");

		log("=====");

		if (logging()) {
			if (localSourceDir != null)
				log("extractObjects from " + localSourceDir);
			log("extractObjects to " + targetDir.getAbsolutePath());
		}

		// Note that some files have multiple objects.
		// These may come from multiple sources, or they may be from the same source.
		deferredPropertyList = new ArrayList<>();

		// Phase 2a 
		// -- generate the ordered map of the archive contents, by resource.
		// -- set up the rezipCache for vendors that need to do that (Bruker, multiple <n>/)

		rezipCache = new ArrayList<>();
		Map<String, Map<String, ArchiveEntry>> htArchiveContents = phase2aInitializeZipData();
		
		ParserIterator iter = new ParserIterator();
		while (iter.hasNext()) {
			ObjectParser parser = iter.next();

			// There is one parser created for each of the IFD-extract.json
			// "FAIRSpec.extractor.object" records.

			// Phase 2b

			// The file path points to a digital item in the aggregation that
			// potentially could be a digital object in the IUPAC FAIRData Collection.

			// -- StructureHelper identifies structures by file extensions (see
			// ifd.properties),
			// adding deferred properties such as InChI, InChIKey, and SMILES
			// -- Vendor plug-ins such as Bruker "claim" zip files or directories based on
			// contained files, such as "acqu"
			// -- Vendor plub-ins such as MestreNova extract structure byte[]
			// representations and metadata associated with spectra
			// along with paging information, which allows for new associations.

			// Parse the file path, creating association, structure, sample, and spectrum objects.
			// This phase produces the deferredPropertyList, which is processed after
			// all the parsing is done, because sometimes the object is not recognized 
			// until a key file (Bruker procs, for example, is found).

			log("!Phase 2b \n" + localizedTopLevelZipURL + "\n" + parser.sData);

			phase2bParseZipFileNamesForObjects(parser, htArchiveContents.get(localizedTopLevelZipURL));

			if (logging())
				log("!Phase 2b found " + ifdObjectCount + " IFD objects");
		}

		// Phase 2c

		// All objects have been created. 

		// An important feature of Extractor is that it can repackage zip files,
		// removing resources that are totally unnecessary and extracting properties
		// and representations using IFDVendorPluginI services.

		if (rezipCache != null && rezipCache.size() > 0) {
			phase2cGetNextRezipName();
			lastRezipPath = null;			
			iter = new ParserIterator();
			while (iter.hasNext()) {
				ObjectParser parser = iter.next();
				if (parser.hasData)
					phase2ReadZipContentsIteratively(getTopZipStream(), "", PHASE_2C, null);				
			}
		}

		// Vendors may produce new objects that need association or properties of those
		// objects. This happens in Phases 2a, 2b, and 2c

		phase2cProcessDeferredObjectProperties(null);

		// Phase 2d

		log("!Phase 2d check for ignored files");

		iter = new ParserIterator();
		while (iter.hasNext()) {
			iter.next();
			phase2ReadZipContentsIteratively(getTopZipStream(), "", PHASE_2D, null);
		}
	}

	protected Map<String, Map<String, ArchiveEntry>>  phase2aInitializeZipData() throws IOException, IFDException {

		Map<String, Map<String, ArchiveEntry>> contents = new LinkedHashMap<>();
			
		// Scan through parsers for resource changes

		
		ParserIterator iter = new ParserIterator();
		while (iter.hasNext()) {
			ExtractorSource currentSource = extractorSource; 
			iter.next();
			if (extractorSource != currentSource) {
				currentSource = extractorSource;
				if (cleanCollectionDir) {
					log("!cleaning directory " + extractorSource.rootPath);
					FileUtils.cleanDirectory(new File(targetDir + "/" + extractorSource.rootPath));
				}				
				String source = targetDir + "/" + extractorSource.rootPath;
				if (!rootPaths.contains(source))
					rootPaths.add(source);
				// first build the file list
				Map<String, ArchiveEntry> zipFileMap = contents.get(localizedTopLevelZipURL);
				if (zipFileMap == null) {
					// Scan URL zip stream for files.
					log("!retrieving " + localizedTopLevelZipURL);
					URL url = new URL(localizedTopLevelZipURL);
					// for JS
					long[] retLength = new long[1];
					InputStream is = openLocalFileInputStream(url, retLength);
					long len = retLength[0];
					if (len > 0)
						helper.setCurrentResourceByteLength(len);
					zipFileMap = phase2ReadZipContentsIteratively(is, "", PHASE_2A,
							new LinkedHashMap<String, ArchiveEntry>());
					contents.put(localizedTopLevelZipURL, zipFileMap);
				}
			}
		}
		return contents;
	}

	protected ExtractorSource phase2InitializeSource(ExtractorSource source) throws IFDException, IOException {
		// localize the URL if we are using a local copy of a remote resource.

		localizedTopLevelZipURL = localizeURL(source.source);
		
		// remove ".zip" if present in the overall name

		String zipPath = localizedTopLevelZipURL.substring(localizedTopLevelZipURL.lastIndexOf(":") + 1);
		
		File zipFile = new File(zipPath);
		
		if (debugging)
			log("opening " + localizedTopLevelZipURL);


		String rootPath = zipFile.getName();
		if (rootPath.endsWith(".zip") || rootPath.endsWith(".tgz"))
			rootPath = rootPath.substring(0, rootPath.length() - 4);
		else if (rootPath.endsWith(".tar.gz"))
			rootPath = rootPath.substring(0, rootPath.length() - 7);

		File rootDir = new File(targetDir + "/" + rootPath);
		rootDir.mkdir();
		// open a new log
		source.rootPath = rootPath;
		source.setLists(rootPath, ignoreRegex);
		lstManifest = source.lstManifest;
		lstIgnored = source.lstIgnored;
		helper.addOrSetSource(source.source);
		return source;
	}

	protected void setupTargetDir() {
		targetDir.mkdir();
		new File(targetDir + "/_IFD_warnings.txt").delete();
		new File(targetDir + "/_IFD_rejected.json").delete();
		new File(targetDir + "/_IFD_ignored.json").delete();
		new File(targetDir + "/_IFD_manifest.json").delete();
		new File(targetDir + "/IFD.findingaid.json").delete();
		new File(targetDir + "/IFD.collection.zip").delete();
	}

	protected InputStream getTopZipStream() throws MalformedURLException, IOException {
		return (localizedTopLevelZipURL.endsWith("/")
				? new DirectoryInputStream(localizedTopLevelZipURL)
				: new URL(localizedTopLevelZipURL).openStream());
	}

	/**
	 * Parse the zip file using an object parser.
	 * 
	 * @param parser
	 * @param zipFileMap
	 * @return true if have spectra objects
	 * @throws IOException
	 * @throws IFDException
	 */
	protected void phase2bParseZipFileNamesForObjects(ObjectParser parser, Map<String, ArchiveEntry> zipFileMap) throws IOException, IFDException {
		// next, we process those names
		for (Entry<String, ArchiveEntry> e : zipFileMap.entrySet()) {
			String originPath = e.getKey();
			String localizedName = localizePath(originPath);
			// Generally we allow a representation (cif for example) to be
			// linked to multiple objects. I can't think of reason not to allow this.
			if (!allowMultipleObjectsForRepresentations && htLocalizedNameToObject.get(localizedName) != null)
				continue;
			ArchiveEntry zipEntry = e.getValue();
			long len = zipEntry.getSize();
			IFDObject<?> obj = phase2bAddIFDObjectsForName(parser, originPath, localizedName, len);
			if (obj instanceof IFDRepresentableObject) {
				addFileToFileLists(originPath, LOG_OUTPUT, len, null);
				ifdObjectCount++;
			}
		}
	}

	/**
	 * Use the regex ObjectParser to match a file name with a pattern defined in the
	 * IFD-extract.json description. This will result in the formation of one or
	 * more IFDObjects -- an IFDAanalysis, IFDStructureSpecCollection,
	 * IFDDataObjectObject, or IFDStructure, for instance. But that will probably
	 * change.
	 * 
	 * The parser specifically looks for Matcher groups, regex (?<xxxx>...), that
	 * have been created by the ObjectParser from an object line such as:
	 * 
	 * {IFD.representation.spec.nmr.vendor.dataset::{IFD.property.sample.label::*-*}-{IFD.property.dataobject.label::*}.jdf}
	 *
	 * 
	 * 
	 * @param parser
	 * @param originPath
	 * @param localizeName
	 * @return one of IFDStructureSpec, IFDDataObject, IFDStructure, in that order,
	 *         depending upon availability
	 * 
	 * @throws IFDException
	 * @throws IOException 
	 */
	protected IFDObject<?> phase2bAddIFDObjectsForName(ObjectParser parser, String originPath, String localizedName, long len)
			throws IFDException, IOException {

		Matcher m = parser.match(originPath);
		if (!m.find()) {
			return null;
		}
		helper.beginAddingObjects(originPath);
		if (debugging)
			log("adding IFDObjects for " + originPath);

		// If an IFDDataObject object is added, then it will also be added to
		// htManifestNameToSpecData

		List<String> keys = new ArrayList<>();
		for (String key : parser.keys.keySet()) {
			keys.add(key);
		}
		for (int i = keys.size(); --i >= 0;) {
			String key = keys.get(i);
			String param = parser.keys.get(key);
			if (param.length() > 0) {
				String id = m.group(key);
				log("!found " + param + " " + id);
				
				if (IFDConst.isDataObject(param)) {
					String s = IFDConst.IFD_DATAOBJECT_FLAG + localizedName;
					if (htLocalizedNameToObject.get(s) != null)
						continue;
				}
				
				IFDObject<?> obj = helper.addObject(extractorSource.rootPath, param, id, localizedName, len);
				if (obj instanceof IFDRepresentableObject) {
					linkLocalizedNameToObject(localizedName, param, (IFDRepresentableObject<?>) obj);
					if (obj instanceof IFDDataObject)
						parser.hasData = true;
				} else if (obj instanceof FAIRSpecCompoundAssociation) {
					//this did not work, because we don't really know what is the defining characteristic
					//of an association in terms of path. 
					//processDeferredObjectProperties(originPath, (IFDStructureDataAssociation) obj);
				}
				if (debugging)
					log("!found " + param + " " + id);
			}
		}
		return helper.endAddingObjects();
	}

	protected InputStream openLocalFileInputStream(URL url, long[] retLength) throws IOException {
		InputStream is;
		if ("file".equals(url.getProtocol())) {
			currentZipFile = new File(url.getPath());
			if (currentZipFile.isDirectory()) {
				is = new DirectoryInputStream(currentZipFile.toString());				
			} else {
				retLength[0] = currentZipFile.length();
				is = url.openStream();
			}
		} else {
			// for remote operation, we create a local temporary file
			File tempFile = currentZipFile = File.createTempFile("extract", ".zip");
			localizedTopLevelZipURL = "file:///" + tempFile.getAbsolutePath();
			log("!saving " + url + " as " + tempFile);
			FAIRSpecUtilities.getLimitedStreamBytes(url.openStream(), -1, new FileOutputStream(tempFile), true, true);
			log("!saved " + tempFile.length() + " bytes");
			retLength[0] = tempFile.length();
			is = new FileInputStream(tempFile);
		}
		return is;
	}

	/**
	 * Get a new ObjectParser for this data. Note that this method may be overridden
	 * if desired.
	 * 
	 * @param source
	 * @param sObj
	 * @return
	 * @throws IFDException
	 */
	protected ObjectParser newObjectParser(ExtractorSource source, String sObj) throws IFDException {
		return new ObjectParser(this, source, sObj);
	}

	/**
	 * Process all entries in a zip file, looking for files to extract and
	 * directories to rezip. This method is called at different phases in the
	 * extraction.
	 * 
	 * 
	 * @param is
	 * @param baseOriginPath              a path ending in "zip|"
	 * @param phase
	 * @param originToEntryMap a map to return of name to ZipEntry; may
	 *                                    be null
	 * 
	 * @return originToEntryMap 
	 * @throws IOException
	 * @throws IFDException 
	 */
	protected Map<String, ArchiveEntry> phase2ReadZipContentsIteratively(InputStream is,
			String baseOriginPath, int phase, Map<String, ArchiveEntry> originToEntryMap)
			throws IOException, IFDException {
		if (debugging && baseOriginPath.length() > 0)
			log("! opening " + baseOriginPath);
		boolean isTopLevel = (baseOriginPath.length() == 0);
		ArchiveInputStream ais = new ArchiveInputStream(is);
		ArchiveEntry zipEntry = null;
		ArchiveEntry nextEntry = null;
		ArchiveEntry nextRealEntry = null;
		int n = 0;
		boolean first = (phase != PHASE_2D);
		int pt;
		while ((zipEntry = (nextEntry != null ? nextEntry : nextRealEntry != null ? nextRealEntry : ais.getNextEntry())) != null) {
			n++;
			//System.out.println(zipEntry);
			nextEntry = null;
			String name = zipEntry.getName();
			boolean isDir = zipEntry.isDirectory();
			if (first) {
				first = false;
				if (!isDir) {
					if ((pt = name.lastIndexOf('/')) >= 0) {
					// ARGH! Implicit top directory
					nextEntry = new ArchiveEntry(name.substring(0, pt + 1));
					nextRealEntry = zipEntry;
					continue;
					}
				}
			}
			if (!isDir)
				nextRealEntry = null;
			String oPath = baseOriginPath + name;
			if (isDir) {
				if (logging())
					log("Phase 2." + phase + " checking zip directory: " + n + " " + oPath);
			} else if (zipEntry.getSize() == 0) {
				continue;
			} else {
				if (lstRejected.accept(oPath)) {
					// Test 9: acs.orglett.0c01153/22284726,22284729 MACOSX,
					// acs.joc.0c00770/22567817
					if (phase == PHASE_2A)
						addFileToFileLists(oPath, LOG_REJECTED, zipEntry.getSize(), null);
					continue;
				}
				if (lstIgnored.accept(oPath)) {
					// Test 9: acs.orglett.0c01153/22284726,22284729 MACOSX,
					// acs.joc.0c00770/22567817
					if (phase == PHASE_2A)
						addFileToFileLists(oPath, LOG_IGNORED, zipEntry.getSize(), ais);
					continue;
				}
			}
			if (debugging)
				log("reading zip entry: " + n + " " + oPath);

			if (originToEntryMap != null) { 
				// Phase 2a only
				originToEntryMap.put(oPath, zipEntry);
			}
			if (isZip(oPath)) {
				// iteratively check zip files if not in the final checking phase
				phase2ReadZipContentsIteratively(ais, oPath + "|", phase, originToEntryMap);
			} else {
				switch (phase) {
				case PHASE_2A:
					if (!isDir)
						phase2aProcessEntry(baseOriginPath, oPath, ais, zipEntry);
					break;
				case PHASE_2C:
					// rezipping
					if (oPath.equals(currentRezipPath)) {
						nextEntry = phase2cRezipEntry(baseOriginPath, oPath, ais, zipEntry, currentRezipVendor);
						phase2cGetNextRezipName();
						continue;
					}
					break;
				case PHASE_2D:
					// final check
					if (!isDir)
						phase2dCheckOrReject(ais, oPath, zipEntry.getSize());
					break;
				}
			}
			nextEntry = null;
		}
		if (isTopLevel)
			ais.close();
		return originToEntryMap;
	}

	protected void phase2dCheckOrReject(ArchiveInputStream ais, String oPath, long len) throws IOException {
		String localizedName = localizePath(oPath);
		Object obj = htLocalizedNameToObject.get(localizedName);
		if (obj == null) {
			if (!lstIgnored.contains(oPath) && !lstRejected.contains(oPath)) {
				// A file entry has been found that has not been already
				// added to the ignored or rejected list.
				if (lstRejected.accept(oPath)) {
					addFileToFileLists(oPath, LOG_REJECTED, len, null);
				} else {
					addFileToFileLists(oPath, LOG_IGNORED, len, ais);
				}
			}
		} else if (htZipRenamed.containsKey(localizedName)) {
				lstManifest.remove(localizedName, len);
		}
	}

	/**
	 * Phase 2a check to see what should be done with a zip entry. We can extract it
	 * or ignore it; and we can check it to sees if it is the trigger for extracting
	 * a zip file in a second pass.
	 * 
	 * @param originPath path to this entry including | and / but not rootPath
	 * @param ais
	 * @param zipEntry
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	protected void phase2aProcessEntry(String baseOriginPath, String originPath, InputStream ais, ArchiveEntry zipEntry)
			throws FileNotFoundException, IOException {
		long len = zipEntry.getSize();
		Matcher m;
		
		// Matcher mp;

		// check for files that should be pulled out - these might be JDX files, for
		// example.
		// "param" appears if a vendor has flagged these files for parameter extraction.

		
		
		if (vendorCachePattern != null && (m = vendorCachePattern.matcher(originPath)).find()) {
			
			
			PropertyManagerI v = getPropertyManager(m);
			boolean doCheck = (v != null);
			boolean doExtract = (!doCheck || v.doExtract(originPath));
			

//			1. we don't have params 
//		      - generic file, just save it.  doExtract and not doCheck
//			2. we have params and there is extraction
//		      - save file and also check it for parameters  doExtract and doCheck
//			3. we have params but no extraction  !doCheck  and !doExtract
//		      - ignore completely

			if (doExtract) {
				String ext = m.group("ext");
				File f = getAbsoluteFileTarget(originPath);
				OutputStream os = (doCheck || noOutput ? new ByteArrayOutputStream() : new FileOutputStream(f));
				if (os != null)
					FAIRSpecUtilities.getLimitedStreamBytes(ais, len, os, false, true);
				String localizedName = localizePath(originPath);
				String type = null;
				if (!doCheck && !noOutput) {
					len = f.length();
					writeOriginToCollection(originPath, null, len);
				} else {
					// doCheck or noOutput
					byte[] bytes = ((ByteArrayOutputStream) os).toByteArray();
					len = bytes.length;
					if (doCheck) {
// abandoned - useful for pure sample associations? 
//						mp = parser.p.matcher(originPath);
//						if (mp.find()) {
//							addProperty(null, null);
//							for (String key : parser.keys.keySet()) {
//								String param = parser.keys.get(key);
//								if (param.equals(FAIRSpecExtractorHelper.IFD_PROPERTY_SAMPLE_LABEL)) {
//									String label = mp.group(key);
//									this.originPath = originPath;
//									this.localizedName = localizedName;
//									addProperty(param, label);
//								}
//							}
//						}

						// set this.localizedName for parameters
						// preserve this.localizedName, as we might be in a rezip.
						// as, for example, a JDX file within a Bruker dataset
						writeOriginToCollection(originPath, bytes, 0);
						String oldOriginPath = this.originPath;
						String oldLocal = this.localizedName;
						this.originPath = originPath;
						this.localizedName = localizedName;
						// indicating "this" here notifies the vendor plug-in that
						// this is a one-shot file, not a collection.
						type = v.accept(this, originPath, bytes);
						deferredPropertyList.add(null);
						this.localizedName = oldLocal;
						this.originPath = oldOriginPath;
					}
				}
				addFileAndCacheRepresentation(originPath, localizedName, len, type, ext, null);
			}
		}

		// here we look for the "trigger" file within a zip file that indicates that we
		// (may) have a certain vendor's files that need looking into. The case in point
		// is finding a procs file within a Bruker data set. Or, in principle, an acqus
		// file and just an FID but no pdata/ directory. But for now we want to see that
		// processed data.


		if (rezipCachePattern != null && (m = rezipCachePattern.matcher(originPath)).find()) {

			// e.g. exptno/./pdata/procs

			VendorPluginI v = getVendorForRezip(m);
			originPath = m.group("path" + v.getIndex());
			if (originPath.equals(lastRezipPath)) {
				if (logging())
					log("duplicate path " + originPath);
			} else {
				lastRezipPath = originPath;
				String localPath = localizePath(originPath);
				CacheRepresentation ref = new CacheRepresentation(new IFDReference(originPath, extractorSource.rootPath, localPath), v,
						len, null, "application/zip");
				// if this is a zip file, the data object will have been set to xxx.zip
				// but we need this to be 
				String basePath = (baseOriginPath.endsWith("|") ? baseOriginPath.substring(0, baseOriginPath.length() - 1)
						: new File(originPath).getParent() + "/");			
				if (basePath == null)
					basePath = originPath;
				ref.setRezipOrigin(basePath);
				if (rezipCache.size() > 0) {
					 CacheRepresentation r = rezipCache.get(rezipCache.size() - 1);
					 if (r.getRezipOrigin().equals(basePath)) {
						 ref.setIsMultiple();
						 r.setIsMultiple();						 
					 }
				}
				rezipCache.add(ref);
				log("!rezip pattern found " + originPath + " " + ref);
			}
		}

	}

	/**
	 * Starting with "xxxx/xx#page1.mol" return "page1".
	 * 
	 * These will be from MNova processing.
	 * 
	 * @param originPath
	 * @return
	 */
	protected static String getStructureNameFromPath(String originPath) {
		String name = originPath.substring(originPath.lastIndexOf("/") + 1);
		name = name.substring(name.indexOf('#') + 1);
		int pt = name.indexOf('.');
		if (pt >= 0)
			name = name.substring(0, pt);
		return name;
	}

	/**
	 * Ensure that we have a correct length in the metadata for this representation.
	 * as long as it exists, even if we are not writing it in this pass.
	 * 
	 * @param rep
	 */
	protected long setLocalFileLength(IFDRepresentation rep) {
		String name = rep.getRef().getLocalName();
		long len = lstWritten.getLength(name);
		rep.setLength(len);
		return len;
	}

	/**
	 * Indicate that a local path Not 100% clear why these are happening.
	 * 
	 * @param localPath
	 * @param method
	 */
	protected void logDigitalItem(String originPath, String localPath, String method) {
		logWarn("digital item ignored, as it does not fit any template pattern: " + originPath, method);
	}

	protected void logNote(String msg, String method) {
		msg = "!NOTE: " + msg + " -- Extractor." + method 
				+ " " + ifdid + " " + (extractorSource == null ? "" : extractorSource.rootPath);
		log(msg);
	}

	protected void logWarn(String msg, String method) {
		msg = "! WARNING: " + msg + " -- Extractor." + method 
				+ " " + ifdid + " " + (extractorSource == null ? "" : extractorSource.rootPath);
		log(msg);
	}

	protected void logErr(String msg, String method) {
		msg = "!! ERROR: " + msg + " -- Extractor." + method 
				+ " " + ifdid + " " + (extractorSource == null ? "" : extractorSource.rootPath);
		log(msg);
	}

	protected String errorLog = "";

	protected int testID = -1;

	/**
	 * Just a very simple logger. Messages that start with "!" are always logged;
	 * others are logged if debugging is set to true.
	 * 
	 * 
	 * @param msg
	 */
	@Override
	public void log(String msg) {
		if (msg.startsWith("!!")) {
			errors++;
			errorLog += msg + "\n";
		} else if (msg.startsWith("! ")) {
			warnings++;
			errorLog += msg + "\n";
		}
		logToSys(msg);
	}

	protected void logToSys(String msg) {
		if (logging() && msg == "!!") {
			FAIRSpecUtilities.refreshLog();
		}
		boolean toSysErr = msg.startsWith("!!") || msg.startsWith("! ");
		if (toSysErr)
			strWarnings += msg + "\n";
		
		boolean toSysOut = toSysErr || msg.startsWith("!");
		if (testID >= 0)
			msg = "test " + testID + ": " + msg;
		if (logging()) {
			try {
				FAIRSpecUtilities.logStream.write((msg + "\n").getBytes());
			} catch (IOException e) {
			}
		}
		System.out.flush();
		System.err.flush();
		if (toSysErr) {
			System.err.println(msg);
		} else if (toSysOut) {
			System.out.println(msg);
		}
		System.out.flush();
		System.err.flush();
	}

	protected static boolean logging() {
		return FAIRSpecUtilities.logStream != null;
	}

	/**
	 * For testing (or for whatever reason zip files are local or should not use the
	 * given source paths), replace https://......./ with sourceDir/
	 * 
	 * @param sUrl
	 * @return localized URL
	 * @throws IFDException
	 */
	protected String localizeURL(String sUrl) throws IFDException {
		if (localSourceDir != null && localSourceDir.endsWith(".zip")) {
			sUrl = localSourceDir;
		} else if (localSourceDir != null && localSourceDir.endsWith("/*")) {
			sUrl = localSourceDir.substring(0, localSourceDir.length() - 1);
		} else if (localSourceDir != null && !sUrl.startsWith("./")) {
			int pt = sUrl.lastIndexOf("/");
			if (pt >= 0) {
				sUrl = localSourceDir + sUrl.substring(pt);
				if (!sUrl.endsWith(".zip") && !sUrl.endsWith("/"))
					sUrl += ".zip";
			}
		}
		sUrl = toAbsolutePath(sUrl);

		if (sUrl.indexOf("//") < 0)
			sUrl = "file:/" + sUrl;
		return sUrl;
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
	protected static String getIFDExtractValue(String sObj, String key, int[] pt) throws IFDException {
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

	/**
	 * Find the matching pattern for rezipN where N is the vendor index in
	 * activeVendors. Presumably there will be only one vendor per match. (Two
	 * vendors will not be looking for MOL files, for example.)
	 * 
	 * @param m
	 * @return
	 */
	protected VendorPluginI getVendorForRezip(Matcher m) {
		for (int i = bsRezipVendors.nextSetBit(0); i >= 0; i = bsRezipVendors.nextSetBit(i + 1)) {
			String ret = m.group("rezip" + i);
			if (ret != null && ret.length() > 0) {
				return VendorPluginI.activeVendors.get(i).vendor;
			}
		}
		return null;
	}

	/**
	 * Pull the next rezip parent directory name off the stack, setting the
	 * currentRezipPath and currentRezipVendor fields.
	 * 
	 */
	protected void phase2cGetNextRezipName() {
		if (rezipCache.size() == 0) {
			currentRezipPath = null;
			currentRezipRepresentation = null;
		} else {
			currentRezipPath = (String) (currentRezipRepresentation = rezipCache.remove(0)).getRef().getOrigin();
			currentRezipVendor = (VendorPluginI) currentRezipRepresentation.getData();
		}
	}

	/**
	 * Phase 2c. Process an entry for rezipping, jumping to the next unrelated
	 * entry.
	 * 
	 * When a CompressedEntry is a directory and has been identified as a SpecData
	 * object, we need to catalog and rezip that file.
	 * 
	 * Create a new zip file that reconfigures the file directory to contain what we
	 * want it to.
	 * 
	 * Note that the rezipping process takes two passes, because the first pass has
	 * most likely already passed by one or more files associated with this
	 * rezipping project.
	 * 
	 * @param baseName xxxx.zip|
	 * @param oPath
	 * @param zis
	 * @param entry
	 * @return next (unrelated) entry
	 * @throws IOException
	 * @throws IFDException
	 */
	protected ArchiveEntry phase2cRezipEntry(String baseName, String oPath, ArchiveInputStream ais, ArchiveEntry entry,
			VendorPluginI vendor) throws IOException, IFDException {

		// originPath points to the directory containing pdata

		// three possibilities:

		// xxx.zip/name/pdata --> xxx.zip_name.zip 1/pdata (ACS 22567817; localname
		// xxx_zip_name.zip)
		// xxx.zip/63/pdata --> xxx.zip 63/pdata (ICL; localname xxx.zip)
		// xxx.zip/pdata --> xxx.zip 1/pdata (ICL; localname xxx.zip)

		String entryName = entry.getName();
		String dirName = (entry.isDirectory() ? entryName : entryName.substring(0, entryName.lastIndexOf('/') + 1));
		// dirName = 63/ ok
		// or
		// dirName = testing/63/ ok
		// or
		// dirName = testing/ --> testing/1/
		// or
		// dirName = "" --> 1/

//		BUT! there are cases where there are multiple
//		nn/ directories - in which case we need to 
//		generate the directory as xxx.zip..testing_63.zip or
//		maybe do this ALWAYS? Just on second event? 

		String parent = new File(entryName).getParent();
		int lenOffset = (parent == null ? 0 : parent.length() + 1);
		// because Bruker directories must start with a number
		// xxx/1/ is OK
		String thisDir = dirName.substring(lenOffset, dirName.length() - 1);
		String newDir = vendor.getRezipPrefix(thisDir);
		Matcher m = null;
		String localizedName = localizePath(oPath);
		String lNameForObj = localizedName;
		// at this point, there is no object??
		// 8f/HBMC.zip|HMBC/250/ will be under HMBC.zip
		IFDRepresentableObject<?> obj = getObjectFromLocalizedName(lNameForObj, IFDConst.IFD_DATAOBJECT_FLAG);
		if (obj == null) {
			String name;
			if (baseName.endsWith("|")) {
				// was a zip file
				name = baseName.substring(0, baseName.length() - 1);
			} else {
				// was a directory
				name = parent + "/";
			}

			obj = getObjectFromLocalizedName(localizePath(name), IFDConst.IFD_DATAOBJECT_FLAG);
			if (obj == null) {
				obj = getObjectFromLocalizedName(localizePath(name), IFDConst.IFD_DATAOBJECT_FLAG);
				throw new IFDException("phase2cRezipEntry could not find object for " + lNameForObj);
			}
		}
		String basePath = baseName + parent;
		if (newDir == null) {
			newDir = "";
			boolean isMultiple = currentRezipRepresentation.isMultiple();
			if (!isMultiple)
				oPath = basePath;
			if (oPath.endsWith(".zip")) {
				if (lenOffset > 0) {
					htZipRenamed.put(localizePath(basePath), localizedName);
				}
			}
			this.originPath = oPath;
			localizedName = localizePath(oPath);
			if (!localizedName.endsWith(".zip")) {
				oPath += ".zip";
				localizedName += ".zip";
			}
			if (this.localizedName == null)
				this.localizedName = localizedName;
			if (isMultiple) {
				addDeferredPropertyOrRepresentation(NEW_PAGE_KEY, new Object[] { "_" + thisDir, obj, localizedName },
						false, null, null);
			}
		} else {
			newDir += "/";
			lenOffset = dirName.length();
			this.originPath = oPath;
			if (oPath.endsWith(".zip")) {
				if (lenOffset > 0) {
					htZipRenamed.put(localizePath(basePath), localizedName);
				}
			} else {
//				oPath += ".zip";
			}
			if (this.localizedName == null)
				this.localizedName = localizedName;
			String msg = "Extractor correcting " + vendor.getVendorName() + " directory name to " + localizedName + "|"
					+ newDir;
			addProperty(IFDConst.IFD_PROPERTY_DATAOBECT_NOTE, msg);
			log("!" + msg);
		}
		localizedName = localizePath(oPath);
		htLocalizedNameToObject.put(localizedName, obj);
		this.localizedName = localizedName;
		File outFile = getAbsoluteFileTarget(oPath);
		log("!Extractor Phase 2c rezipping " + baseName + entry + " as " + outFile);
		OutputStream fos = (noOutput ? new ByteArrayOutputStream() : new FileOutputStream(outFile));
		ZipOutputStream zos = new ZipOutputStream(fos);
		vendor.startRezip(this);
		long len = 0;
		while ((entry = ais.getNextEntry()) != null) {
			entryName = entry.getName();
			String entryPath = baseName + entryName;
			boolean isDir = entry.isDirectory();
			if (lstRejected.accept(entryPath)) {
				if (!lstRejected.contains(entryPath))
					addFileToFileLists(entryPath, LOG_REJECTED, entry.getSize(), null);
				// Test 9: acs.orglett.0c01153/22284726,22284729 MACOSX,
				// acs.joc.0c00770/22567817
				continue;
			}
			if (!entryName.startsWith(dirName))
				break;
			if (isDir)
				continue;
			PropertyManagerI mgr = null;

			this.originPath = entryPath;
			String localName = localizePath(baseName + entryName);
			// prevent this file from being placed on the ignored list
			htLocalizedNameToObject.put(localName, obj);

			String type = vendor.getExtractType(this, baseName, entryName);
			if (type != null) {
				// extract this file into the collection
				addDeferredPropertyOrRepresentation(type, localName, false, null, null);
			}

			boolean doInclude = (vendor == null || vendor.doRezipInclude(this, baseName, entryName));
			// cache this one? -- could be a different vendor -- JDX inside Bruker;
			// false for MNova within Bruker? TODO: But wouldn't that possibly have
			// structures?
			// directory, for example
			boolean doCache = (vendorCachePattern != null && (m = vendorCachePattern.matcher(entryName)).find()
					&& phase2cGetParamName(m) != null
					&& ((mgr = getPropertyManager(m)) == null || mgr.doExtract(entryName)));
			boolean doCheck = (doCache || mgr != null);

			len = entry.getSize();
			if (len == 0 || !doInclude && !doCheck)
				continue;
			OutputStream os = (doCheck ? new ByteArrayOutputStream() : zos);
			String outName = newDir + entryName.substring(lenOffset);
			if (doInclude)
				zos.putNextEntry(new ZipEntry(outName));
			FAIRSpecUtilities.getLimitedStreamBytes(ais.getStream(), len, os, false, false);
			if (doCheck) {
				byte[] bytes = ((ByteArrayOutputStream) os).toByteArray();
				if (doInclude)
					zos.write(bytes);
				this.originPath = oPath + outName;
				this.localizedName = localizedName;
				if (mgr == null || mgr == vendor) {
					vendor.accept(null, this.originPath, bytes);
				} else {
					mgr.accept(this, this.originPath, bytes);
				}
			}
			if (doInclude)
				zos.closeEntry();
		}
		vendor.endRezip();
		zos.close();
		fos.close();
		String dataType = vendor.processRepresentation(oPath + ".zip", null);
		len = (noOutput ? ((ByteArrayOutputStream) fos).size() : outFile.length());
		writeOriginToCollection(oPath, null, len);
		IFDRepresentation r = helper.getSpecDataRepresentation(localizedName);
		if (r == null) {
			// probably the case, as this renamed representation has not been added yet.
		} else {
			r.setLength(len);
		}
		if (oPath.endsWith(".zip"))
			oPath = oPath.substring(0, oPath.length() - 4); // remove ".zip"
		addFileAndCacheRepresentation(oPath, localizedName, len, dataType, null, "application/zip");
		if (obj != null && !localizedName.equals(lNameForObj)) {
			htLocalizedNameToObject.put(localizedName, obj);
		}
		return entry;
	}

	/**
	 * Should be no throwing of Exceptions here -- we know if we have (?<param>...)
	 * groups.
	 * 
	 * @param m
	 * @return
	 */
	protected String phase2cGetParamName(Matcher m) {
		try {
			if (cachePatternHasVendors)
				return m.group("param");
		} catch (Exception e) {
		}
		return null;
	}



	
	/// Phase 3 ///
	
	protected String processPhase3(String findingAidFileNameRoot) throws IFDException, IOException {
		// Phase 3

		// update object type and len records

		phase3UpdateCachedRepresentations();

		// clean up the collection

		phase3RemoveUnmanifestedRepresentations();
		phase3CheckForDuplicateSpecData();
		phase3RemoveInvalidData();
		
		// write the files and create the finding aid serialization
		
		writeRootManifests();
		phase3FinalizeExtraction();
		return phase3SerializeFindingAid(findingAidFileNameRoot);
	}

	protected void phase3RemoveInvalidData() {
		helper.removeInvalidData();
	}

	protected String phase3SerializeFindingAid(String findingAidFileNameRoot) throws IOException {
		log("!Extractor.extractAndCreateFindingAid serializing...");
		ArrayList<Object> products = rootPaths;
		IFDSerializerI ser = getSerializer();
		if (createZippedCollection) {
			products.add(new File(targetDir + "/_IFD_extract.json"));
			products.add(new File(targetDir + "/_IFD_ignored.json"));
			products.add(new File(targetDir + "/_IFD_manifest.json"));
		}
		long[] times = new long[3];
		String serializedFindingAid = helper.createSerialization((readOnly && !createFindingAidOnly? null : targetDir),
				findingAidFileNameRoot, createZippedCollection ? products : null, ser, times);
		log("!Extractor serialization done " + times[0] + " " + times[1] + " " + times[2] + " ms " + serializedFindingAid.length()
				+ " bytes");
		return serializedFindingAid;
	}

	/**
	 * Set the type and len fields for structure and spec data
	 */
	protected void phase3UpdateCachedRepresentations() {

		for (String ckey : vendorCache.keySet()) {
			CacheRepresentation r = vendorCache.get(ckey);
			IFDRepresentableObject<?> obj = htLocalizedNameToObject.get(ckey);
			if (obj == null) {
				String path = r.getRef().getOrigin().toString();
				logDigitalItem(path, ckey, "addCachedRepresentationsToObjects");
				try {
					addFileToFileLists(path, LOG_IGNORED, r.getLength(), null);
				} catch (IOException e) {
					// not possible
				}
				continue;
			}
			String originPath = r.getRef().getOrigin().toString();
			String type = r.getType();
			// type will be null for pdf, for example
			String mediatype = r.getMediaType();
			// suffix is just unique internal ID for the cache
			int pt = ckey.indexOf('\0');
			String localName = (pt > 0 ? ckey.substring(0, pt) : ckey);
			IFDRepresentation r1 = obj.findOrAddRepresentation(originPath, localName, null, type, null);
			if (type != null && r1.getType() == null)
				r1.setType(type);
			if (mediatype != null && r1.getMediaType() == null)
				r1.setMediaType(mediatype);
			if (r1.getLength() == 0)
				r1.setLength(r.getLength());
		}
	}

	/**
	 * Look for spectra with identical labels, and remove duplicates.
	 * 
	 * If a structure has lost all its associations, remove it.
	 * 
	 * This is experimental. For now, these are NOT ACTUALLY REMOVED. (Issues were
	 * found with same-named PDF files that were embedded in different Bruker
	 * directories but had the same name, which was being assigned the ID
	 * 
	 * 
	 */
	protected void phase3CheckForDuplicateSpecData() {
		BitSet bs = new BitSet();
		FAIRSpecCompoundCollection ssc = helper.getCompoundCollection();
		boolean isFound = false;
		boolean doRemove = false;
		int n = 0;
		// wondering where these duplicates come from.
		Map<Integer, IFDObject<?>> map = new HashMap<>();
		for (IFDAssociation assoc : ssc) {
			IFDDataObjectCollection c = ((FAIRSpecCompoundAssociation) assoc).getDataObjectCollection();
			List<Object> found = new ArrayList<>();
			for (IFDRepresentableObject<?> spec : c) {
				int i = spec.getIndex();
				if (bs.get(i)) {
					found.add((IFDDataObject) spec);
					log("! Extractor found duplicate DataObject reference " + spec + " for " + assoc.getFirstObj1()
							+ " in " + assoc + " and " + map.get(i) + " template order needs changing? ");
					isFound = true;
				} else {
					bs.set(i);
					map.put(i, assoc);
				}
			}
			n += found.size();
			if (found.size() > 0) {
				// log("!! Extractor found the same DataObject ID in : " + found.size());
				// BH not removing these for now.
				if (doRemove)
					c.removeAll(found);
			}
		}
		if (isFound && doRemove) {
			n += helper.removeStructuresWithNoAssociations();
			if (n > 0)
				log("! " + n + " objects removed");
		}
	}

	/**
	 * Remove all data objects that (no longer) have any representations.
	 * 
	 * The issue here is that sometimes we have to identify directories that are not
	 * going to be zipped up in the end, because they do not match the rezip
	 * trigger.
	 */
	protected void phase3RemoveUnmanifestedRepresentations() {
		boolean isRemoved = false;
		for (IFDRepresentableObject<IFDDataObjectRepresentation> spec : helper.getSpecCollection()) {
			List<IFDRepresentation> lstRepRemoved = new ArrayList<>();
			for (Object o : spec) {
				IFDRepresentation rep = (IFDRepresentation) o;
				if (setLocalFileLength(rep) == 0) {
					lstRepRemoved.add(rep);
					// zip file reference in extact.json could actually reference only an extracted PDF
					// this can be normal -- pdf created two different ways, for example.
					// or from MNova, it is standard
//					log("!OK removing 0-length representation " + rep);
				}
			}
			spec.removeAll(lstRepRemoved);
			if (spec.size() == 0) {
				// no representations left -- this must have been temporary only
				spec.setValid(false);
				isRemoved = true;
//				log("!OK preliminary data object " + spec + " removed - no representations");
			}
		}
		if (isRemoved) {
			int n = helper.removeStructuresWithNoAssociations();
			if (n > 0)
				log("!" + n + " objects with no representations removed");
		}
	}

	protected void phase3FinalizeExtraction() {
		log(helper.finalizeExtraction());
	}

	/// generally used

	protected static boolean isZip(String name) {
		return name.endsWith(".zip") || name.endsWith(".tgz") || name.endsWith("tar.gz");
	}

	@Override
	public void addProperty(String key, Object val) {
		if (val != NULL)
			log(this.localizedName + " addProperty " + key + "=" + val);
		addDeferredPropertyOrRepresentation(key, val, false, null, null);
	}

	/**
	 * Cache the property or representation created by an IFDVendorPluginI class or
	 * returned from the DefaultStructureHelper for later processing. This method is
	 * a callback from IFDVendorPluginI classes or
	 * DefaultStructureHelper.processRepresentation(...) only.
	 * 
	 * @param key       representation or property key; the key "_struc" is used by
	 *                  a vendor plugin to pass back both a file name and a byte
	 *                  array to create a new digital object extracted from the
	 *                  original object, for example, from an MNova object
	 *                  extraction
	 * @param val       either a String value or an Object[] with elements byte[]
	 *                  and String name
	 * @param isInline  representation data is being provided as inline-data, to be
	 *                  saved only in the finding aid (InChI, SMILES, InChIKey)
	 * @param mediaType a media type for a representation, or null
	 */
	@Override
	public void addDeferredPropertyOrRepresentation(String key, Object val, boolean isInline, String mediaType, String note) {
		if (key == null) {
			deferredPropertyList.add(null);
			return;
		}
		deferredPropertyList
				.add(new Object[] { originPath, localizedName, key, val, Boolean.valueOf(isInline), mediaType, note });
		if (key.startsWith(DefaultStructureHelper.STRUC_FILE_DATA_KEY)) {
			// Mestrelab vendor plug-in has found a MOL or SDF file in Phase 2b. 
			// val is Object[] {byte[] bytes, String name}
			// Pass data to structure property manager in order
			// to add (by coming right back here) InChI, SMILES, and InChIKey.
			byte[] bytes = (byte[]) ((Object[]) val)[0];
			String name = (String) ((Object[]) val)[1]; // must not be null
			getStructurePropertyManager().processRepresentation(name, bytes);
		}
	}

	/**
	 * Process the properties in deferredPropertyList after the IFDObject objects
	 * have been created for all resources. This includes writing extracted
	 * representations to files.
	 * 
	 * @throws IFDException
	 * @throws IOException
	 */
	protected void phase2cProcessDeferredObjectProperties(String phase2OriginPath) throws IFDException, IOException {
		FAIRSpecCompoundAssociation assoc = null;
		String lastLocal = null;
		IFDDataObject localSpec = null;
		IFDStructure struc = null;
		IFDSample sample = null;
		IFDDataObject originObject = null;
		boolean cloning = false;
		for (int i = 0, n = deferredPropertyList.size(); i < n; i++) {
			Object[] a = deferredPropertyList.get(i);
			if (a == null) {
				sample = null;
				originObject = null;
				continue;
			}
			assoc = null;
			String originPath = (String) a[0];
			String localizedName = (String) a[1];
			String key = (String) a[2];
			cloning = key.equals(NEW_PAGE_KEY);
			boolean isRep = IFDConst.isRepresentation(key);
			Object value = a[3];
			boolean isInline = (a[4] == Boolean.TRUE);
			String type = FAIRSpecExtractorHelper.getObjectTypeForPropertyOrRepresentationKey(key, true);
			boolean isSample = (type == FAIRSpecExtractorHelper.ClassTypes.Sample);
			boolean isStructure = (type == FAIRSpecExtractorHelper.ClassTypes.Structure);
			if (isSample) {
				sample = helper.getSampleByName((String) value);
				continue;
			}
			boolean isNew = !localizedName.equals(lastLocal);
			if (isNew) {
				lastLocal = localizedName;
			}
			// link to the originating spec representation -- xxx.mnova, xxx.zip

			String propType = IFDConst.getObjectTypeFlag(key);
			IFDRepresentableObject<?> spec = getObjectFromLocalizedName(localizedName, propType);

			if (spec == null && !cloning) {
				// TODO: should this be added to the IGNORED list?
				logDigitalItem(originPath, localizedName, "processDeferredObjectProperties");
				continue;
			}
			if (spec instanceof IFDStructure) {
				struc = (IFDStructure) spec;
				spec = null;
			} else if (spec instanceof IFDSample) {
				sample = (IFDSample) spec;
				spec = null;
			} else if (isNew && spec instanceof IFDDataObject) {
				localSpec = (IFDDataObject) spec;
			}
			if (isRep) {
				// from reportVendor-- Bruker adds this for thumb.png and pdf files.
				String mediaType = (String) a[5];
				String note = (String) a[6];
				if (value instanceof Object[]) {
					// from DefaultStructureHelper - a PNG version of a CIF file, for example.
					Object[] oval = (Object[]) value;
					byte[] bytes = (byte[]) oval[0];
					String oPath = (String) oval[1];
					String localName = localizePath(oPath);
					writeOriginToCollection(oPath, bytes, 0);
					addFileToFileLists(localName, LOG_OUTPUT, bytes.length, null);
					value = localName;
				}
				String keyPath = (isInline ? null : value.toString());
				Object data = (isInline ? value : null);
				// note --- not allowing for AnalysisObject or Sample here
				IFDRepresentableObject<?> obj = (IFDConst.isStructure(key) ? struc : spec);
				linkLocalizedNameToObject(keyPath, null, obj);
				IFDRepresentation r = obj.findOrAddRepresentation(originPath, keyPath, data, key, mediaType);
				if (note != null)
					r.setNote(note);
				if (!isInline)
					setLocalFileLength(r);
				continue;
			}

			// properties only
			if (cloning) {
				String newLocalName = null;
				boolean clearNew;
				if (value instanceof String) {
					clearNew = false;
					// not allowing an MNova structure to carry to next page?
					struc = null; 
					if (originObject == null) {
						originObject = localSpec;
					} else {
						localSpec = originObject;
					}
					// e.g. MNova extracted _page=10
				} else {
					clearNew = true;
					// e.g. Bruker created a new object from multiple <n>/ directories
					a = (Object[]) value;
					value = (String) a[0];
					localSpec = (IFDDataObject) a[1];
					newLocalName = (String) a[2];
				}
				String idExtension = (String) value;
				if (assoc == null)
					assoc = helper.findCompound(null, localSpec);
				System.out.println("cloning for association " + assoc);
				IFDDataObject newSpec;
				if (localSpec == null) {
					newSpec = (IFDDataObject) spec;
				} else {
					newSpec = helper.cloneData(localSpec, idExtension, true);
				}
				spec = localSpec = newSpec;
				struc = helper.getFirstStructureForSpec(localSpec, assoc == null);
				if (sample == null)
					sample = helper.getFirstSampleForSpec(localSpec, assoc == null);
				if (assoc == null) {
					if (struc != null) {
						helper.createCompound(struc, newSpec);
						log("!Structure " + struc + " found and associated with " + spec);
					}
					if (sample != null) {
						helper.associateSampleSpec(sample, newSpec);
						log("!Structure " + struc + " found and associated with " + spec);
					}
				} else {
					// we have an association already in Phase 2, and now we need to
					// update that.
					if (clearNew)
						newSpec.clear();
					assoc.addDataObject(newSpec);
				}
				if (struc == null && sample == null) {
					log("!SpecData " + spec + " added " + (assoc == null ? "" : "to " + assoc));
				}
				if (newLocalName != null)
					localizedName = newLocalName;
				htLocalizedNameToObject.put(localizedName, spec); // for internal use
				CacheRepresentation rep = vendorCache.get(localizedName);
				if (newLocalName != localizedName) {
					String ckey = localizedName + idExtension.replace('_', '#') + "\0" + idExtension;
					vendorCache.put(ckey, rep);
					htLocalizedNameToObject.put(ckey, spec);
				}
				continue;
			}
			if (key.startsWith(DefaultStructureHelper.STRUC_FILE_DATA_KEY)) {
				// e.g. extracted xxxx/xxx#page1.mol
				Object[] oval = (Object[]) value;
				byte[] bytes = (byte[]) oval[0];
				String oPath = (String) oval[1];
				String ifdRepType = DefaultStructureHelper.getType(key.substring(key.lastIndexOf(".") + 1), bytes);
				// use the byte[] for the structure as a unique identifier.
				AWrap w = new AWrap(bytes);
				if (htStructureRepCache == null)
					htStructureRepCache = new HashMap<>();
				struc = htStructureRepCache.get(w);
				String name = getStructureNameFromPath(oPath);
				if (struc == null) {
					writeOriginToCollection(oPath, bytes, 0);
					String localName = localizePath(oPath);
					struc = helper.getFirstStructureForSpec((IFDDataObject) spec, false);
					if (struc == null) {
						struc = helper.addStructureForSpec(extractorSource.rootPath, (IFDDataObject) spec, ifdRepType,
								oPath, localName, name);
						
					}
					htStructureRepCache.put(w, struc);
					if (sample == null) {
						assoc = helper.findCompound(struc, (IFDDataObject) spec);
					} else {
						helper.associateSampleStructure(sample, struc);
					}
					// MNova 1 page, 1 spec, 1 structure Test #5
					addFileAndCacheRepresentation(oPath, null, bytes.length, ifdRepType, null, null);
					linkLocalizedNameToObject(localName, ifdRepType, struc);
					log("!Structure " + struc + " created and associated with " + spec);
				} else {
					assoc = helper.findCompound(struc, (IFDDataObject) spec);
					if (assoc == null) {
						assoc = helper.createCompound(struc, (IFDDataObject) spec);
						log("!Structure " + struc + " found and associated with " + spec);
					}
				} 
				if (struc.getID() == null) {
					String id = assoc.getID();
					if (id == null && spec != null) {
						id = spec.getID();
						assoc.setID(id);
					}
					struc.setID(id);
				}
				continue;
			}
			// just a property
			if (isStructure) {
				if (struc == null) {
					logErr("No structure found for " + lastLocal + " " + key, "processDeferredObjectProperies");
					continue; // already added?
				} else {
					setPropertyIfNotAlreadySet(struc, key, value, originPath);
				}
			} else if (isSample) {
				// TODO?
			} else {
				setPropertyIfNotAlreadySet(localSpec, key, value, originPath);
			}
		}
		if (assoc == null) {
			deferredPropertyList.clear();
			htStructureRepCache = null;
		} else if (cloning) {
			// but why is it the lastLocal?
			vendorCache.remove(lastLocal);
		}
	}

	protected IFDRepresentableObject<?> getObjectFromLocalizedName(String name,
			String type) {
		IFDRepresentableObject<?> obj = (type == null ? null : htLocalizedNameToObject.get(type + name));
		return (obj == null ? htLocalizedNameToObject.get(name) : obj);
	}

	protected void setPropertyIfNotAlreadySet(IFDObject<?> obj, String key, Object value, String originPath) {
		if (obj.getID().indexOf("S8") >= 0) {
			System.out.println("EX ?????");
			System.out.println("EX prop " + key + " " + value);
		}
		boolean isNull = (value == NULL);
		if (IFDConst.isProperty(key)) {
			// not a parameter and not forcing NULL
			Object v = obj.getPropertyValue(key);
			if (value.equals(v))
				return;
			if (v != null) {
				String source = obj.getPropertySource(key);
				logWarn(originPath + " property " + key + " can't set value '" + value + "', as it is already set to '"
						+ v + "' from " + source, "setPropertyIfNotAlreadySet");
				return;
			}
		}
		// setting a value to null removes it.
		obj.setPropertyValue(key, (isNull ? null : value), originPath);
	}

	
	protected void resetManifests() {
		
	}
	/**
	 * Write the _IFD_manifest.json, _IFD_ignored.json and _IFD_extract.json files.
	 * 
	 * Note that manifest and ignored will be in the archive folder(s) comprising the collection.
	 * 
	 * @param isOpen if true, starting -- just clear the lists; if false, write the
	 *               files
	 * @throws IOException
	 */
	protected void writeRootManifests() throws IOException {
		resourceList = "";
		rootLists = new ArrayList<>(); 
		for (ExtractorSource r: htResources.values()) {
			resourceList += ";" + r.source;
			rootLists.add(r.lstManifest);
			rootLists.add(r.lstIgnored);
		}
		resourceList = resourceList.substring(1);
		int nign = FileList.getListCount(rootLists, "ignored");
		int nrej = FileList.getListCount(rootLists, "rejected");

		if (noOutput) {
			if (nign > 0) {
				logWarn("ignored " + nign + " files", "writeRootManifests");
			}
			if (nrej > 0) {
				logWarn("rejected " + nrej + " files", "writeRootManifests");
			}
		} else {
			File f = new File(targetDir + "/_IFD_extract.json");
			writeBytesToFile(extractScript.getBytes(), f);

			outputListJSON("manifest", new File(targetDir + "/_IFD_manifest.json"));
			if (nign > 0)
				outputListJSON("ignored", new File(targetDir + "/_IFD_ignored.json"));
			if (nrej > 0)
				outputListJSON("rejected", new File(targetDir + "/_IFD_rejected.json"));
		}
	}
	
	protected void outputListJSON(String name, File file) throws IOException {
		int[] ret = new int[1];
		String json = helper.getListJSON(name, rootLists, resourceList, extractscriptFile.getName(), ret);
		writeBytesToFile(json.getBytes(), file);
		log("!saved " + file + " (" + ret[0] + " items)");
	}

	/**
	 * Link a representation with the given local name and type to an object such as
	 * a spectrum or structure. Later in the process, this representation will be
	 * added to the object.
	 * 
	 * @param localizedName
	 * @param type
	 * @param obj
	 * @throws IOException 
	 */
	protected void linkLocalizedNameToObject(String localizedName, String type, IFDRepresentableObject<?> obj) throws IOException {
		if (localizedName != null && (type == null || IFDConst.isRepresentation(type))) {
			String pre = obj.getObjectFlag();

			htLocalizedNameToObject.put(localizedName, obj);
			htLocalizedNameToObject.put(pre + localizedName, obj);
			String renamed = htZipRenamed.get(localizedName);
			if (renamed != null) {
				htLocalizedNameToObject.put(renamed, obj);
				// deferred representations could be for multiple object types. 
				htLocalizedNameToObject.put(pre + renamed, obj);
			}
		}
	}

	@Override
	public void setNewObjectMetadata(IFDObject<?> o, String param) {
		if (htMetadata != null) {
			String id = o.getID();
			Map<String, Object> map;
			if (id == null || (map = htMetadata.get(param)) == null)
				return;
			if (SpreadsheetReader.hasDataKey(map) || loadMetadata(id, map)) {
				List<Object[]> metadata = SpreadsheetReader.getRowDataForIndex(map, id);
				if (metadata != null) {
					log("!Extractor adding " + metadata.size() + " metadata items for " + param + "=" + id);
					FAIRSpecExtractorHelper.addProperties(o, metadata);
				}
			}
		}
	}

	protected boolean loadMetadata(String param, Map<String, Object> map) {
		String err = null;
		String fname = null, indexKey = null;
		try {
			fname = (String) map.get("METADATA_FILE");
			indexKey = (String) map.get("METADATA_KEY");
			// ./Manifest.xls#Sheet1
			int pt = fname.indexOf("#");
			String sheetRef = null;
			File metadataFile = null;
			if (pt >= 0) {
				sheetRef = fname.substring(pt + 1);
				fname = fname.substring(0, pt);
			}
			fname = toAbsolutePath(fname);
			metadataFile = new File(fname);
			Object data = SpreadsheetReader.getCellData(new FileInputStream(metadataFile), sheetRef, "", true);
			int icol = SpreadsheetReader.setMapData(map, data, indexKey);
			if (icol < 1) {
				logWarn("METADATA file " + fname + " did not have a column titled " + indexKey, "loadMetadata");
			}
		} catch (Exception e) {
			err = e.getMessage();
		} finally {
			if (err != null)
				logWarn(err, "loadMetadata");
		}
		if (!map.containsKey("DATA"))
			map.put("DATA", null);
		return true;
	}

	protected String toAbsolutePath(String fname) {
		if (fname.startsWith("./"))
			fname = extractscriptFile.getParent().toString().replace('\\', '/') + fname.substring(1);
		return fname;
	}

	/**
	 * get a new structure property manager to handle processing of MOL, SDF, and
	 * CDX files, primarily. Can be overridden.
	 * 
	 * @return
	 */
	protected PropertyManagerI getStructurePropertyManager() {
		return (structurePropertyManager == null ? (structurePropertyManager = new DefaultStructureHelper(this))
				: structurePropertyManager);
	}

	/**
	 * Register a digital item as significant and to be included in the collection.
	 * 
	 * Cache file representation for this resource, associating it with a media type
	 * if we can. The representation is a temporary cache-only representation.
	 * 
	 * 
	 * @param originPath
	 * @param localizedName        originPath with localized / and |
	 * @param len
	 * @param ifdType              IFD.representation....
	 * @param fileNameForMediaType
	 * @return temporary CacheRepresentation
	 * @throws IOException not really; just because addFileToFileLists could do that
	 *                     in other cases
	 */
	protected IFDRepresentation addFileAndCacheRepresentation(String originPath, String localizedName, long len,
			String ifdType, String fileNameForMediaType, String mediaType) throws IOException {
		if (localizedName == null)
			localizedName = localizePath(originPath);
		if (mediaType == null) {
			if (fileNameForMediaType == null)
				fileNameForMediaType = localizedName;
			mediaType = FAIRSpecUtilities.mediaTypeFromFileName(fileNameForMediaType);
			if (mediaType == null && fileNameForMediaType != null)
				mediaType = FAIRSpecUtilities.mediaTypeFromFileName(localizedName);
		}
		addFileToFileLists(localizedName, LOG_OUTPUT, len, null);
		CacheRepresentation rep = new CacheRepresentation(new IFDReference(originPath, extractorSource.rootPath, localizedName), null, len,
				ifdType, mediaType);
		vendorCache.put(localizedName, rep);
		return rep;
	}

	/// utilities ///
	/**
	 * Get the full OS file path for FileOutputStream
	 * 
	 * @param originPath
	 * @return
	 */
	protected File getAbsoluteFileTarget(String originPath) {
		return new File(targetDir + "/" + extractorSource.rootPath + "/" + localizePath(originPath));
	}

	/**
	 * Clean up the zip entry name to remove '|', '/', ' ', and add ".zip" if there
	 * is a trailing '/' in the name.
	 * 
	 * @param path
	 * @return
	 */
	protected static String localizePath(String path) {
		path = path.replace('\\', '/');
		boolean isDir = path.endsWith("/");
		if (isDir)
			path = path.substring(0, path.length() - 1);
		int pt = -1;
		while ((pt = path.indexOf('|', pt + 1)) >= 0)
			path = path.substring(0, pt) + ".." + path.substring(++pt);
		return path.replace('/', '_').replace('#', '_').replace(' ', '_') + (isDir ? ".zip" : "");
	}

	/**
	 * Add a record for _IFD_manifest.json or _IFD_ignored.json
	 * 
	 * @param fileName localized only for LOG_OUTPUT, otherwise an origin path
	 * @param mode
	 * @param len
	 * @throws IOException
	 */
	protected void addFileToFileLists(String fileName, int mode, long len, ArchiveInputStream ais) throws IOException {
		switch (mode) {
		case LOG_IGNORED:
			// fileName will be an origin name
			writeDigitalItem(fileName, ais, len, mode);
			break;
		case LOG_REJECTED:
			// fileName will be an origin name
			lstRejected.add(fileName, len);
			break;
		case LOG_OUTPUT:
			// fileName will be a localized file name
			// in Phase 2c, this will be zip files
			if (ais == null) {
				lstManifest.add(fileName, len);
			} else {
				writeDigitalItem(fileName, ais, len, mode);
			} 
			break;
		}
	}

	/**
	 * Transfer an ignored file to the collection as a digital item, provided the includeIgnoredFiles flag is set.
	 * 
	 * @param originPath
	 * @param ais
	 * @param len
	 * @throws IOException
	 */
	protected void writeDigitalItem(String originPath, ArchiveInputStream ais, long len, int mode) throws IOException {
		String localizedName = localizePath(originPath);
		switch (mode) {
		case LOG_IGNORED:
			lstIgnored.add(localizedName, len);
			if (noOutput || !includeIgnoredFiles || ais == null)
				return;
			break;
		case LOG_OUTPUT:
			lstManifest.add(localizedName, len);
			break;
		}
		File f = getAbsoluteFileTarget(localizedName);
		FAIRSpecUtilities.getLimitedStreamBytes(ais, len, new FileOutputStream(f), false, true);
	}

	protected void writeOriginToCollection(String originPath, byte[] bytes, long len) throws IOException {
		lstWritten.add(localizePath(originPath), (bytes == null ? len : bytes.length));
		if (!noOutput && bytes != null)
			writeBytesToFile(bytes, getAbsoluteFileTarget(originPath));
	}

	protected void writeBytesToFile(byte[] bytes, File f) throws IOException {
		if (!noOutput)
			FAIRSpecUtilities.writeBytesToFile(bytes, f);
	}

	/**
	 * Minimal command-line interface for now. There are several flags set from
	 * ExtractorTest. Right now these are not included in the options, and we also
	 * need to use proper -x or --xxxx flags.
	 * 
	 * Just haven't implemented that yet.
	 * 
	 * @param args [0] extractionFile.json, [1] sourcePath, [2] targetDir
	 * 
	 */
	public static void main(String[] args) {

		if (args.length == 0) {
			System.out.println(getCommandLineHelp());
			return;
		}
		// just run one IFD-extract.json
		runExtraction(args, null, -1, -1);
	}

	/**
	 * Run a full extraction based on arguments, possibly a test set
	 * 
	 * @param args    [IFD-extractFile.json, source.zip, targetDirectory, flag1,
	 *                flag2, ...]
	 * @param testSet set of extract codes
	 * @param first
	 * @param last
	 */
	protected static void runExtraction(String[] args, String[] testSet, int first, int last) {

		System.out.println(Arrays.toString(args));

		int i0 = Math.max(0, Math.min(first, last));
		int i1 = Math.max(0, Math.max(first, last));
		int failed = 0;

		String sourceArchive = null;
		String targetDir = null;
		String ifdExtractJSONFilename;
		switch (args.length) {
		default:
		case 3:
			targetDir = args[2];
			//$FALL-THROUGH$
		case 2:
			sourceArchive = args[1];
			//$FALL-THROUGH$
		case 1:
			ifdExtractJSONFilename = args[0];
			break;
		case 0:
			ifdExtractJSONFilename = null;
		}
		if (ifdExtractJSONFilename == null && testSet == null)
			throw new NullPointerException("No IFD-extract.json or test set?");
//		if (sourceArchive == null)
//			throw new NullPointerException("No source file or directory??");
		if (targetDir == null)
			throw new NullPointerException("No targetDir");
		new File(targetDir).mkdirs();
		FAIRSpecUtilities.setLogging(targetDir + "/extractor.log");

		String json = null;

		int n = 0;
		int nWarnings = 0;
		int nErrors = 0;
		String warnings = "";
		boolean createFindingAidJSONList = false;
		Extractor extractor = null;
		String flags = null;
		String targetDir0 = targetDir;
		for (int itest = i0; itest <= i1; itest++) {
			extractor = new Extractor();
			extractor.logToSys("Extractor.runExtraction output to " + new File(targetDir).getAbsolutePath());
			String job = null;
			// ./extract/ should be in the main Eclipse project directory.
			String extractInfo = null;
			if (ifdExtractJSONFilename == null) {

//				"./extract/acs.joc.0c00770/IFD-extract.json#22567817",  // 0 727 files; zips of bruker dirs + mnovas

				job = extractInfo = testSet[itest];
				extractor.logToSys("Extractor.runExtraction " + itest + " " + job);
				int pt = extractInfo.indexOf("#");
				if (pt == 0) {
					ifdExtractJSONFilename = null;
					System.out.println("Ignoring " + extractInfo);
					continue;
				} else if (pt > 0) {
					ifdExtractJSONFilename = extractInfo.substring(0, pt);
				} else {
					ifdExtractJSONFilename = extractInfo;
				}
				String targetSubDirectory = new File(ifdExtractJSONFilename).getParentFile().getName();
				if (targetSubDirectory.length() > 0)
					targetDir = targetDir0 + "/" + targetSubDirectory;
			}
			n++;
			if (extractInfo != null) {
				if (json == null) {
					json = "{\"findingaids\":[\n";
				} else {
					json += ",\n";
				}
				json += "\"" + extractInfo + "\"";
			}
			long t0 = System.currentTimeMillis();

			extractor.testID = itest;

			extractor.processFlags(args);
			new File(targetDir).mkdirs();
			flags = "\n first = " + first + " last = " + last + "\n"//
					+ extractor.dumpFlags()
					+ "\n createFindingAidJSONList = " + createFindingAidJSONList //
					+ "\n IFD version " + IFDConst.IFD_VERSION + "\n";
			// false for testing and you don't want to mess up _IFD_findingaids.json
			createFindingAidJSONList = !extractor.debugReadOnly && (first != last || first < 0);
//			if (first == last && first >= 0) {
//				createFindingAidJSONList = false;
//			}

			try {
				File ifdExtractScriptFile = new File(ifdExtractJSONFilename).getAbsoluteFile();
				File targetPath = new File(targetDir).getAbsoluteFile();
				String sourcePath = (sourceArchive == null ? null : new File(sourceArchive).getAbsolutePath());
				extractor.run(extractInfo, ifdExtractScriptFile, targetPath, sourcePath);
				extractor.logToSys("Extractor.runExtraction ok " + extractInfo);
			} catch (Exception e) {
				failed++;
				extractor.logErr("Exception " + e + " " + itest, "runExtraction");
				e.printStackTrace();
				if (extractor.stopOnAnyFailure)
					break;
			}
			nWarnings += extractor.warnings;
			nErrors += extractor.errors;
			extractor.logToSys(
					"!Extractor.runExtraction job " + job + " time/sec=" + (System.currentTimeMillis() - t0) / 1000.0);
			ifdExtractJSONFilename = null;
			if (extractor.warnings > 0) {
				warnings += "=========" + extractor.warnings + " warnings for " + targetDir + "\n" + extractor.strWarnings;
				try {
					FAIRSpecUtilities.writeBytesToFile((warnings).getBytes(),
							new File(targetDir0 + "/_IFD_warnings.txt"));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}
		json += "\n]}\n";
		if (extractor != null) {
			if (failed == 0) {
				try {
					if (createFindingAidJSONList && !extractor.readOnly && json != null) {
						File f = new File(targetDir0 + "/_IFD_findingaids.json");
						FAIRSpecUtilities.writeBytesToFile(json.getBytes(), f);
						extractor
								.logToSys("Extractor.runExtraction File " + f.getAbsolutePath() + " created \n" + json);
					} else {
						extractor
								.logToSys("Extractor.runExtraction _IFD_findingaids.json was not created for\n" + json);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (nWarnings > 0) {
				try {
					FAIRSpecUtilities.writeBytesToFile((warnings + nWarnings + " warnings\n").getBytes(),
							new File(targetDir0 + "/_IFD_warnings.txt"));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			extractor.logToSys("");
			System.err.flush();
			System.out.flush();
			System.err.println(extractor.errorLog);
			System.err.flush();
			System.out.flush();
			extractor.logToSys("!Extractor.runExtraction flags " + flags);
			extractor.logToSys("!Extractor " + (failed == 0 ? "done" : "failed") + " total=" + n + " failed=" + failed
					+ " errors=" + nErrors + " warnings=" + nWarnings);
		}

		FAIRSpecUtilities.setLogging(null);
	}

	protected void setDefaultRunParams() {
		// normally false:

		System.out.flush();
		debugReadOnly = false; // quick settings - no file creation

		addPublicationMetadata = false; // true to place metadata into the finding aid

		cleanCollectionDir = true;

		// normally true:

		stopOnAnyFailure = true; // set false to allow continuing after an error.

		debugging = false; // true for verbose listing of all files
		createFindingAidOnly = false; // true if extraction files already exist or you otherwise don't want not write

		allowNoPubInfo = true;// debugReadOnly; // true to allow no internet connection and so no pub calls

		setDerivedFlags();

	}

	protected boolean dataciteUp = true;

	protected boolean cleanCollectionDir = true;

	protected void setDerivedFlags() {

		// this next is independent of readOnly
		createZippedCollection = createZippedCollection && !debugReadOnly; // false to bypass final creation of an
																			// _IFD_collection.zip file

		readOnly |= debugReadOnly; // for testing; when true, no output other than a log file is produced
		noOutput = (createFindingAidOnly || readOnly);
		skipPubInfo = !dataciteUp || debugReadOnly; // true to allow no internet connection and so no pub calls
	}

	protected void processFlags(String[] args) {
		String flags = "";
		for (int i = 3; i < args.length; i++) {
			if (args[i] != null)
				flags += "-" + args[i] + ";";
		}
		checkFlags(flags);
		setDerivedFlags();

	}

	protected void checkFlags(String flags) {
		flags = flags.toLowerCase();
		if (flags.indexOf("-") < 0)
			flags = "-" + flags.replaceAll("\\;", "-;") + ";";

		if (flags.indexOf("-addpublicationmetadata;") >= 0) {
			addPublicationMetadata = true;
		}

		if (flags.indexOf("-byid;") >= 0) {
			setExtractorOption(IFDConst.IFD_PROPERTY_COLLECTIONSET_BYID, "true");
		}

		if (flags.indexOf("-datacitedown;") >= 0) {
			dataciteUp = false;
		}

		if (flags.indexOf("-debugging;") >= 0) {
			debugging = true;
		}

		if (flags.indexOf("-debugreadonly;") >= 0) {
			debugReadOnly = true;
		}

		if (flags.indexOf("-findingaidonly;") >= 0) {
			createFindingAidOnly = true;
		}

		if (flags.indexOf("-noclean;") >= 0) {
			cleanCollectionDir = false;
		}

		if (flags.indexOf("-noignored;") >= 0) {
			includeIgnoredFiles = false;
		}

		if (flags.indexOf("-nopubinfo;") >= 0) {
			skipPubInfo = true;
		}

		if (flags.indexOf("-nostoponfailure;") >= 0) {
			stopOnAnyFailure = false;
		}

		if (flags.indexOf("-nozip;") >= 0) {
			createZippedCollection = false;
		}

		if (flags.indexOf("-readonly;") >= 0) {
			readOnly = true;
		}
		if (flags.indexOf("-requirepubinfo;") >= 0) {
			allowNoPubInfo = false;
		}

// not working 
//		int pt = flags.indexOf("-structurepattern="); 
//		if (pt >= 0) {
//			userStructureFilePattern = flags.substring(flags.indexOf("=", pt) + 1, flags.indexOf(";", pt));
//		}
	}

	protected static String getCommandLineHelp() {
		return "\nformat: java -jar IFDExtractor.jar [IFD-extract.json] [sourceArchive] [targetDir] [flags]" //
				+ "\n"
				+ "\nwhere" // 
				+ "\n" // 
				+ "\n[IFD-extract.json] is the IFD extraction template for this collection" //
				+ "\n[sourceArchive] is the source .zip, .tar.gz, or .tgz file" //
				+ "\n[targetDir] is the target directory for the collection (which you are responsible to empty first)" //
				+ "\n" // 
				+ "\n" + "[flags] are one or more of:" //
				+ "\n" //
				+ "\n-addPublicationMetadata (only for post-publication-related collections)" //
				+ "\n-byID (order compounds by ID, not by index; overrides IFD_extract.json setting)"
				+ "\n-dataciteDown (only for post-publication-related collections)" // 
				+ "\n-debugging (lots of messages)" //
				+ "\n-debugReadonly (readonly, no publicationmetadata)" //
				+ "\n-findingAidOnly (only create a finding aid)" //
				+ "\n-noclean (don't empty the destination collection directory before extraction; allows additional files to be zipped)" //
				+ "\n-noignored (don't include ignored files -- treat them as REJECTED)" //
				+ "\n-nopubinfo (ignore all publication info)" //
				+ "\n-nostopOnFailure (continue if there is an error)" //
				+ "\n-nozip (don't zip up the target directory)" // 
				+ "\n-readonly (just create a log file)" //
				+ "\n-requirePubInfo (throw an error is datacite cannot be reached; post-publication-related collections only)";
	}

	protected String dumpFlags() {
		String s =  " stopOnAnyFailure = " + stopOnAnyFailure //
		+ "\n debugging = " + debugging //
		+ "\n readOnly = " + readOnly //
		+ "\n debugReadOnly = " + debugReadOnly //
		+ "\n allowNoPubInfo = " + !allowNoPubInfo //
		+ "\n skipPubInfo = " + skipPubInfo //
		+ "\n skipPubInfo = " + skipPubInfo //
		+ "\n sourceArchive = " + localSourceDir //
		+ "\n targetDir = " + targetDir //
		+ "\n createZippedCollection = " + createZippedCollection; //
		return s;
	}

}
