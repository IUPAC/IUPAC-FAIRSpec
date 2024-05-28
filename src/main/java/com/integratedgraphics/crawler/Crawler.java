package com.integratedgraphics.crawler;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import com.integratedgraphics.util.XmlReader;

import javajs.util.PT;
import javajs.util.Rdr;

/**
 * A DataCite metadata crawler, resulting in the production of an IUPAC FAIRSpec
 * FindingAid for a repository-based or distributed data collection.
 *
 * @author Bob Hanson
 *
 */

public class Crawler extends XmlReader {
	
	private final static String DOWNLOAD_TYPES = ";png;jpg;jpeg;cdxml;mol;cif;xls;xlsx;mnova;mnpub;";
	public final static String DOI_ORG = "https://doi.org/";
	public final static String DATACITE_METADATA = "https://data.datacite.org/application/vnd.datacite.datacite+xml/";
	public final static String FAIRSPEC_SCHEME_URI = "http://iupac.org/ifd";
	public final static String FAIRSPEC_SUBJECT_SCHEME = "IFD";
	private static final String OUTDIR = "c:/temp/iupac/crawler";

	private final static String testPID = "10.14469/hpc/10386";

	private URL dataCiteMetadataURL;
	private int urlDepth;
	private List<String> ifdList;
	private String ifdLine = "";
	private int nReps;

	private boolean isTop = true;
	private boolean skipping = false;
	private boolean isSilent = false;

	private int xmlDepth = 0;
	private static String indent = "                              ";
	private Stack<Map<String, String>> thisAttrs = new Stack<>();
	private Stack<Map<String, List<String>>> thisRelated = new Stack<>();
	private String pidPath;
	private Runnable output;
	private File dataDir, fileDir;
	private long startTime;

	public Crawler(String pid) {
		String thisPID = (pid == null ? testPID : pid);
		try {
			dataCiteMetadataURL = getMetadataURL(thisPID);
		} catch (MalformedURLException e) {
			addException(e);
		}
	}

	private boolean startCrawling(File dataDir, File fileDir, Runnable output) {
		this.startTime = System.currentTimeMillis();
		this.dataDir = dataDir;
		this.fileDir = fileDir;
		this.output = output;
		log = new StringBuffer();
		ifdList = new ArrayList<String>();
		nextMetadata(null);
		return true;
		
	}

	private URL getMetadataURL(String pid) throws MalformedURLException {
		System.out.println(pid);
		return new URL(DATACITE_METADATA + pid);
	}

	private URL newURL(String s) throws MalformedURLException {
		s = PT.rep(s, "&amp;", "&");
		return new URL(s);
	}

	private boolean nextMetadata(URL url) {
		urlDepth++;
		if (url == null) {
			url = dataCiteMetadataURL;
		}
		isTop = (urlDepth == 1);
		skipping = !isTop;
		String currentPath = pidPath;
		String s = url.toString().replace(DATACITE_METADATA, "");
		String sfile = cleanFileName(s) + ".xml";
		int pt = s.lastIndexOf('/');
		String dir = s.substring(0, pt + 1);
		String file = s.substring(pt + 1);
		newLine();
		if (pidPath == null) {
			pidPath = s + ">";
		} else if (s.indexOf(dir) == 0) {
			pidPath += ">" + file + ">";
		} else {
			pidPath += ">" + s + ">";
		}
		logAttr("open", url.toString());
		InputStream is;
		try {
			File dataFile = new File(dataDir, sfile);
			boolean haveData = dataFile.exists();
			System.out.println("reading " + (haveData ? dataFile : url.toString()));
			if (haveData) {
				is = new FileInputStream(dataFile);
			} else {
				URLConnection con = url.openConnection();
				is = con.getInputStream();
			}
			byte[] metadata = getBytesAndClose(is);
			if (!haveData)
				putToFile(dataFile, (byte[]) metadata);
			is = new ByteArrayInputStream((byte[]) metadata);
			parseXML(is);
			is.close();
		} catch (Exception e) {
			addException(e);
		}
		logAttr("close", url.toString());
		newLine();
		pidPath = currentPath;
		output.run();
		urlDepth--;
		return true;
	}

	private static byte[] getBytesAndClose(InputStream is) throws IOException {
		return (byte[]) Rdr.getStreamAsBytes(new BufferedInputStream(is), null);
	}

	private static String cleanFileName(String s) {
		return s.replaceAll("[\\/?&:+=]", "_");
	}

	/**
	 * process a representation
	 * 
	 * @param s
	 * @throws IOException
	 */
	private void processRepresentation(URL url) throws IOException {
		urlDepth++;
		nReps++;
		newLine();
		ifdLine += ">R";
		System.out.println("getContentHeaders: " + url);
		File headerFile = new File(dataDir, cleanFileName(url.toString()) + ".txt");
		boolean haveFile = headerFile.exists();
		String length = null, fileName = null;
		byte[] bytes = null;
		if (haveFile) {
			bytes = getBytesAndClose(new FileInputStream(headerFile));
			appendRepresentationHeaderAttrs(bytes);
			String data = new String(bytes);
			fileName = data.substring(data.indexOf("filename=") + 9);
		} else {
			int len = ifdLine.length();
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("HEAD");
			Map<String, List<String>> map = con.getHeaderFields();
			String content = map.get("Content-Type").get(0);
			addAttrLast("URL", url.toString());
			addAttrLast("mediaType", content);
			List<String> item = map.get("Content-Disposition");
			if (item != null) {
				List<String> list = map.get("Content-Length");
				if (list != null && !list.isEmpty())
					length = list.get(0);
				fileName = PT.getQuotedOrUnquotedAttribute(item.toString(), "filename");
			}
			length = "" + putToFile(headerFile, ifdLine.substring(len).getBytes());
			if (length != null) {
				addAttrLast("length", length);
			}
			if (fileName != null)
				addAttrLast("filename", fileName);
		}
		if (fileName != null) {
			downloadCheck(url, new File(fileDir, cleanFileName(fileName)));
		}
		newLine();
		urlDepth--;
	}

	private void downloadCheck(URL url, File file) throws IOException {
		long modTime = file.lastModified();
		System.out.println(file);
		if (modTime > 0 && modTime < startTime) {
			// existed before we started crawling
			return;
		}
		String s = file.getName();
		int pt = s.lastIndexOf(".");
		if (pt > 0 && PT.isOneOf(s.substring(pt + 1), DOWNLOAD_TYPES)) {
			if (modTime > 0) {
				System.err.println("replacing " + file.getName() + " with " + url);
			}
			URLConnection con = url.openConnection();
			InputStream is = con.getInputStream();
			putToFile(file, getBytesAndClose(is));
		}
	}

	private void parseXML(InputStream content) throws Exception {
		ifdLine = "";
		thisRelated.push(new LinkedHashMap<String, List<String>>());
		BufferedReader reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(content), "UTF-8"));
		//$FALL-THROUGH$
		parseXML(reader);
		newLine();
		content.close();
		processRelated(thisRelated.pop());
	}

	
	private void processRelated(Map<String, List<String>> map) {
		try {
			List<String> list = map.get("DOI");
			if (list != null) {
				for (int i = 0; i < list.size(); i++) {
					nextMetadata(getMetadataURL(list.get(i)));					
				}
			}
			list = map.get("URL");
			if (list != null) {
				for (int i = 0; i < list.size(); i++) {
					processRepresentation(newURL(list.get(i)));
				}
			}
		} catch (Exception e) {
			addException(e);
		}
	}

	@Override
	protected void processStartElement(String localName, String nodeName) {
		xmlDepth++;
		boolean isData = true;
		switch (localName) {
		case "creators":
		case "dates":
		case "sizes":
		case "formats":
		case "version":
		case "contributors":
		case "publisher":
		case "publicationyear":
		case "rightsList":
			skipping = true;
			break;
		case "resourcetype":
			skipping = false;
			break;
		case "relatedidentifiers":
		case "subjects":
		case "title":
			isData = false;
			skipping = false;
			break;
		case "relatedidentifier":
		case "subject":
		case "description":
			// deferring to end
			isData = false;
			skipping = false;
			break;
		}
		logAttr("item", localName);
		for (Entry<String, String> e : atts.entrySet()) {
			String key = e.getKey();
			String val = e.getValue();
			if (isData && !skipping) {
				if (!isTop)
					System.out.println(key);
				addAttrLast(key, val);
			} else {
				logAttr(key, val);
			}
		}
		switch (localName) {
		case "resourcetype":
			switch (atts.get("resourcetypegeneral")) {
			case "Collection":
				pidPath += "C";
				break;
			case "Dataset":
				pidPath += "D";
			}
			break;
		case "relatedidentifier":
		case "description":
		case "subject":
			thisAttrs.push(atts);
			break;
		}
	}

	@Override
	protected void processEndElement(String localName) {
		String s = chars.toString().trim();
		chars.setLength(0);
		Map<String, String> attrs;
		switch (localName) {
		case "description":
			if (s.length() > 0) {
				addAttrLast("description", s);
			}
			break;
		case "title":
			if (s.length() > 0) {
				addAttrFirst("title", s);
			}
			hack("title", s);
			System.out.println(localName + "=" + s);
			break;
		case "subject":
			attrs = getAttributes(true);
			processSubject(attrs, s);
			break;
		case "relatedidentifier":
			if (s.length() > 0) {
				logAttr("relatedidentifier", s);
			}
			addRelatedIdentifier(getAttributes(true), s);
			break;
		default:
			if (!skipping && s.length() > 0) {
				logAttr("value", s);
				addAttrLast(localName, s);
			}
			break;
		}
		xmlDepth--;
	}

	private void processSubject(Map<String, String> attrs, String s) {
//			<subjects>
//			    <subject 
//			schemeURI="http://iupac.org/ifd" 
//			subjectScheme="IFD" 
//			valueURI="http://iupac.org/ifd/IFD.compound.id">21</subject>
//			</subjects>
//
		String key = attrs.get("subjectscheme");
		switch (key) {
		case FAIRSPEC_SUBJECT_SCHEME:
			key = attrs.get("valueuri");
			if (key.startsWith(FAIRSPEC_SCHEME_URI)) {
				key = key.substring(key.lastIndexOf('/') + 1);
			}
			break;			
		}
		if (key != null) {
			addAttrLast(key, s);
		}
	}

	private void addRelatedIdentifier(Map<String, String> attrs, String s) {
		String type = attrs.get("relatedidentifiertype"); // DOI or URL
		String generalType = attrs.get("relationtypegeneral");
		switch (attrs.get("relationtype")) {
		case "References":
			switch (generalType) {
			case "JournalArticle":
				type = "PUB" + type;
				addAttrLast(type, s);
				break;
			}
			break;
		case "HasPart":
			break;
		default:
			type = null;
			break;
		}
		if (type != null) {
			Map<String, List<String>> map = thisRelated.get(thisRelated.size() - 1);
			List<String> list = map.get(type);
			if (list == null)
				map.put(type, list = new ArrayList<String>());
			list.add(s);
		}
	}

	private void hack(String key, String val) {
		switch (key) {
		case "description":
			if (key.startsWith("DOI: ")) {
				
			}
			break;
		case "title":
			if (key.startsWith("Compound ")) {
				Map<String, String> attrs = new HashMap<>();
				String id = "" + PT.parseInt(key.substring(10));
				attrs.put("subjectscheme", "IFD");
				attrs.put("valueuri", "IFD.compound.id");
				processSubject(attrs, id);
				
//				decided this needs to be supplemented by:
//
//					<subjects>
//					    <subject 
//					schemeURI="http://iupac.org/ifd" 
//					subjectScheme="IFD" 
//					valueURI="http://iupac.org/ifd/IFD.compound.id">21</subject>
//					</subjects>
//
//

			}
			break;
		}
	}

	private Map<String, String> getAttributes(boolean andPop) {
		return (andPop ? thisAttrs.pop() : thisAttrs.get(thisAttrs.size() - 1));
	}

	@Override
	protected void endDocument() {
		// to something here?
	}

	private void appendRepresentationHeaderAttrs(byte[] bytes) {
		String s = new String(bytes);
		ifdLine += s;
		log.append(s);
	}

	private void addAttrLast(String key, String value) {
		ifdLine += "\t" + key + "=" + cleanData(value);
		logAttr(key, value);
	}
	
	private void addAttrFirst(String key, String value) {
		ifdLine = "\t" + key + "=" + cleanData(value) + ifdLine;
		logAttr(key, value);
	}
	
	private static String cleanData(String s) {
		return s.replaceAll("\t", "\\\\t").replaceAll("\r\n", "\\\\n").replaceAll("\n", "\\\\n");
	}

	private void newLine() {
		if (ifdLine.length() > 0) {
			String line = (pidPath == null ? "" : pidPath) + ifdLine;
			ifdList.add(line);
			System.out.println(line);
		}
		ifdLine = "";
	}
	private void logAttr(String key, String value) {
		appendLog(key + "=" + value);

	}

	private void addException(Exception e) {
		e.printStackTrace();
		logAttr("exception", e.getClass().getName() + ": " + e.getMessage());
	}

	private void appendLog(String line) {
		if (isSilent || skipping)
			return;
		line = line.trim();
		if (xmlDepth * 2 > indent.length())
			indent += indent;
		log.append('\n').append(urlDepth).append(".").append(xmlDepth).append(indent.substring(0, xmlDepth * 2))
		.append(line);
	}

	public static void main(String[] args) {
		Crawler crawler = new Crawler(args.length > 0 ? args[0] : null);
		String outdir = args.length > 1 ? args[1] : OUTDIR;
		File parent = new File(outdir);
		File dataDir = new File(outdir, "metadata");
		dataDir.mkdirs();
		File fileDir = new File(outdir, "files");
		fileDir.mkdir();
		long t = System.currentTimeMillis();
		crawler.startCrawling(dataDir, fileDir, () -> {
			File f = new File(parent, "crawler.log");
			System.out.println("writing " + crawler.log.length() + " bytes " + f.getAbsolutePath());
			putToFile(f, crawler.log.toString().getBytes());
			f = new File(parent, "crawler.ifd.txt");
			System.out.println("writing " + crawler.ifdList.size() + " entries " + f.getAbsolutePath());
			System.out.println(crawler.nReps + " representations");
			try {
				FileOutputStream fos = new FileOutputStream(f);
				for (int i = 0; i < crawler.ifdList.size(); i++) {
					fos.write(crawler.ifdList.get(i).getBytes());
					fos.write((byte)'\n');
				}
				fos.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		System.out.println("done " + (System.currentTimeMillis() - t)/1000 + " sec");
	}

	private static int putToFile(File f, byte[] bytes) {
		if (bytes == null || bytes.length == 0)
			return 0;
		try {
			FileOutputStream fos = new FileOutputStream(f);
			fos.write(bytes);
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
		return bytes.length;
	}

}
