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
import java.util.TreeMap;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecCompoundAssociation;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecFindingAid;
import org.iupac.fairdata.core.IFDObject;
import org.iupac.fairdata.core.IFDReference;

import com.integratedgraphics.extractor.Extractor;
import com.integratedgraphics.extractor.PubInfoExtractor;
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

	private class CrawlerExtractor extends Extractor {

		private FAIRSpecCompoundAssociation thisCompound;
		
		private FAIRSpecFindingAid findingAid;

		CrawlerExtractor() {
			createZippedCollection = false;
			try {
				helper = newExtractionHelper();
				findingAid = helper.getFindingAid();
			} catch (IFDException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		@Override
		public void log(String msg) {
			super.log(msg);
			appendLog("FindingAidHelper: " + msg);
		}

		@Override
		public String getVersion() {
			return Extractor.version + "(Crawler " + version + ")";
		}

		@Override
		public String getCodeSource() {
			return Extractor.codeSource + "(" + codeSource + ")";
		}

		public void processPubCrossRef(String url) throws IOException {
			findingAid.setPropertyValue(IFDConst.IFD_PROPERTY_COLLECTIONSET_SOURCE_PUBLICATION_URI, url);
			super.phase1ProcessPubURI();
		}
		public String generateFindingAid(File fileDir) throws IOException {
			targetDir = fileDir;
			return phase3SerializeFindingAid(null);			
		}
		
		public FAIRSpecCompoundAssociation addCompound(String id) throws IFDException {
			return thisCompound = helper.createCompound(id);
		}
		
	}

	protected static final String codeSource = "https://github.com/IUPAC/IUPAC-FAIRSpec/blob/main/src/main/java/com/integratedgraphics/ifd/crawler/crawler.java";

	protected static final String version = "0.0.1-alpha+2024.05.28";

	// 2024.05.28 version 0.0.1 initial version used for ICL repository crawling

	private final static String DOWNLOAD_TYPES = ";png;jpg;jpeg;cdxml;mol;cif;xls;xlsx;mnova;mnpub;";
	public final static String DATACITE_METADATA = "https://data.datacite.org/application/vnd.datacite.datacite+xml/";
	public final static String FAIRSPEC_SCHEME_URI = "http://iupac.org/ifd";
	public final static String FAIRDATA_SUBJECT_SCHEME = "IFD";
	private static final String OUTDIR = "c:/temp/iupac/crawler";

	private final static String testPID = "10.14469/hpc/10386";

	private static final String DOI_ORG = PubInfoExtractor.DOI_ORG;

	private static final String FAIRSPEC_COMPOUND_ID = "IFD.property.fairspec.compound.id";

	private static final String IFD_INCHI = "IFD.representation.structure.inchi";
	private static final String IFD_INCHIKEY = "IFD.representation.structure.inchikey";

	private static final String IFD_SMILES = "IFD.representation.structure.smiles";
	private static final String FAIRSPEC_TYPE = "IFD.property.dataobject.fairspec.type";

	private static final String TAB_PROPERTY_KEY = "\t" + IFDConst.IFD_PROPERTY_FLAG;
	
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
	private String thisCompoundID;
	private String thisExperimentType;
	private String thisDOI;


	private Map<String, String> hackMap = new HashMap<>();

	{
		hackMap.put("inchi", IFD_INCHI);
		hackMap.put("SMILES", IFD_SMILES);
		hackMap.put("inchikey", IFD_INCHIKEY);
		hackMap.put("NMR_Solvent", "IFD.property.dataobject.fairspec.nmr.expt_solvent");
		hackMap.put("NMR_Nucleus", "IFD.property.dataobject.fairspec.nmr.expt_nucl1");
		hackMap.put("NMR_Nucleus1", "IFD.property.dataobject.fairspec.nmr.expt_nucl1");
		hackMap.put("NMR_Nucleus2", "IFD.property.dataobject.fairspec.nmr.expt_nucl2");
		hackMap.put("NMR_Expt", "IFD.property.dataobject.fairspec.nmr.expt_name");
		hackMap.put("IFD.IR", "");
	};

	public Crawler(String pid) {
		thisDOI = (pid == null ? testPID : pid);
		try {
			dataCiteMetadataURL = getMetadataURL(thisDOI);
		} catch (MalformedURLException e) {
			addException(e);
		}
	}

	private boolean crawl(File dataDir, File fileDir, Runnable output) {
		this.startTime = System.currentTimeMillis();
		this.dataDir = dataDir;
		this.fileDir = fileDir;
		this.output = output;
		log = new StringBuffer();
		ifdList = new ArrayList<String>();
		nextMetadata(null);
		output.run();
		createFindingAid(DOI_ORG + thisDOI);
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
				putToFile((byte[]) metadata, dataFile);
			is = new ByteArrayInputStream((byte[]) metadata);
			parseXML(is);
			is.close();
		} catch (Exception e) {
			addException(e);
		}
		logAttr("close", url.toString());
		newLine();
		pidPath = currentPath;
//		output.run();
		urlDepth--;
		return true;
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
		this.thisExperimentType = null;
	}

	private static byte[] getBytesAndClose(InputStream is) throws IOException {
		return (byte[]) Rdr.getStreamAsBytes(new BufferedInputStream(is), null);
	}

	private static String cleanFileName(String s) {
		return s.replaceAll("[\\/?&:+=]", "_");
	}

	private long totalLength;
	
	/**
	 * process a representation
	 * 
	 * @param s
	 * @throws IOException
	 */
	private void processRepresentation(URL url, Map<String, String> repMap) throws IOException {
		urlDepth++;
		nReps++;
		newLine();
		ifdLine += ">R";
		File headerFile = new File(dataDir, cleanFileName(url.toString()) + ".txt");
		boolean haveFile = headerFile.exists();
		String length = null, fileName = null;
		byte[] bytes = null;
		if (haveFile) {
			bytes = getBytesAndClose(new FileInputStream(headerFile));
			appendRepresentationHeaderAttrs(bytes);
			String data = new String(bytes);
			int pt = data.indexOf("filename=");
			if (pt >= 0)
				fileName = data.substring(pt + 9);
			pt = data.indexOf("length=");
			if (pt >= 0)
				totalLength += PT.parseInt(data.substring(pt + 7, data.indexOf('\t',pt))); 
		} else {
			int len = ifdLine.length();
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("HEAD");
			Map<String, List<String>> map = con.getHeaderFields();
			String content = map.get("Content-Type").get(0);
			addAttrLast("URL", url.toString());
			addAttrLast("mediaType", content);
			List<String> item = map.get("Content-Disposition");
			fileName = null;
			if (item != null) {
				List<String> list = map.get("Content-Length");
				if (list != null && !list.isEmpty()) {
					length = list.get(0);
					addAttrLast("length", length);
				}
				fileName = PT.getQuotedOrUnquotedAttribute(item.toString(), "filename");
				addAttrLast("filename", fileName);
			}
			len = putToFile(ifdLine.substring(len).getBytes(), headerFile);
		}
		if (fileName != null) {
			System.out.println("getContentHeaders: " + url + " " + length);
			downloadCheck(url, new File(fileDir, cleanFileName(fileName)));
			repMap.put(fileName, ifdLine);
		}
		ifdLine = "";
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
				addError("replacing " + file.getName() + " with " + url);
			}
			URLConnection con = url.openConnection();
			InputStream is = con.getInputStream();
			putToFile(getBytesAndClose(is), file);
		}
	}

	private void processSubject(Map<String, String> attrs, String s) {
//		<subjects>
//		    <subject 
//		schemeURI="http://iupac.org/ifd" 
//		subjectScheme="IFD" 
//		valueURI="http://iupac.org/ifd/IFD.compound.id">21</subject>
//		</subjects>
//
		String key = attrs.get("subjectscheme");
		if (key != null) {
			switch (key) {
			case FAIRDATA_SUBJECT_SCHEME:
				key = attrs.get("valueuri");
				if (key.startsWith(FAIRSPEC_SCHEME_URI)) {
					key = key.substring(key.lastIndexOf('/') + 1);
				}
				addAttrFirst(key, s);
				break;
			default:
				String ifdName = hackMap.get(key);
				if (ifdName != null) {
					if (ifdName.length() == 0)
						return;
					key = ifdName;
					switch (key) {
					case FAIRSPEC_COMPOUND_ID:
						addAttrFirst(key, s);
						return;
					case IFD_INCHI:
						if (s.indexOf("=") < 0) {
							addError("invalid inchi for Compound " + thisCompoundID);
							return;
						}
					}
				}
				addAttrLast(key, s);
				break;
			}
		}
	}

	private void addRelatedIdentifier(Map<String, String> attrs, String s) {
		String type = attrs.get("relatedidentifiertype"); // DOI or URL
		String generalType = attrs.get("relationtypegeneral");
		switch (attrs.get("relationtype")) {
		case "HasPart":
			break;
		case "IsReferencedBy":
			// only interested in JournalArticle
			if (generalType == null)
				return;
			switch (generalType) {
			case "JournalArticle":
				type = "PUB" + type;
				addAttrLast(type, s);
				break;
			default:
				return;
			}
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

	private int dataIndex = 0;
	/**
	 * Process a hack for ICL preliminary repository files.
	 * 
	 * description DOI:.... to relatedIdentifier
	 * 
	 * title "Compound xx:..." adds subject IFD.property.fairspec.compound.id
	 * 
	 * @param key
	 * @param val
	 * @return
	 */
	private boolean hack(String key, String val) {
		if (val.length() < 3)
			return false;
		Map<String, String> attrs;
		switch (key) {
		case "description":
			switch (val.substring(0, 3)) {
			case "DOI":
//		    replace this with:
//			<relatedIdentifier 
//				relatedIdentifierType="DOI" 
//				relationType="IsReferencedBy"
//				relationTypeGeneral="JournalArticle">10.1021/acs.inorgchem.3c01506</relatedIdentifier>

				String doi = val.substring(5).trim();
				attrs = new HashMap<>();
				attrs.put("relatedidentifiertype", "DOI");
				attrs.put("relationtype", "IsReferencedBy");
				attrs.put("relationtypegeneral", "JournalArticle");
				addRelatedIdentifier(attrs, doi);
				return true;
			}
			break;
		case "title":
			String type = null;
			switch (val.substring(0, 3)) {
			case "Com":
				if (val.startsWith("Compound ")) {

					// decided this needs to be supplemented by:
//
//				<subjects>
//				    <subject 
//				schemeURI="http://iupac.org/ifd" 
//				subjectScheme="IFD" 
//				valueURI="http://iupac.org/ifd/IFD.compound.id">21</subject>
//				</subjects>
					String id = "" + PT.parseInt(val.substring(9));
					thisCompoundID = id;
					addIFDSubjectAttribute(FAIRSPEC_COMPOUND_ID, id);
					addAttrLast(IFDConst.IFD_PROPERTY_LABEL, val);
				}
				return true;
			case "NMR":
				type = "nmr";
				break;
			case "IR ":
				type = "ir";
				break;
			case "Pri"://Primary crystal...
			case "Cry"://Crystal...
				type = "xray";
				break;
			}
			if (type != null) {
				this.thisExperimentType = type;
				addIFDSubjectAttribute("IFD.property.dataobject.fairspec.type", "" + type);
				return true;
			}
			break;
		}
		return false;
	}

	private StringBuffer errorBuffer = new StringBuffer();

	private FAIRSpecFindingAid findingAid;

	private CrawlerExtractor extractor;

	/**
	 * change >C to >D
	 */
	private void fixPIDPathForDataset() {
		pidPath = pidPath.substring(0, pidPath.length() - 1) + "D";
		addError("Dataset was Collection: " + pidPath);
	}

	private void processRelated(Map<String, List<String>> map) {
		try {
			List<String> list = map.get("DOI");
			if (list != null) {
				for (int i = 0; i < list.size(); i++) {
					nextMetadata(getMetadataURL(list.get(i)));
				}
			}
			pidPath = "";
			list = map.get("URL");
			if (list != null) {
				Map<String, String> repMap = new TreeMap<>();
				for (int i = 0; i < list.size(); i++) {
					processRepresentation(newURL(list.get(i)), repMap);
				}
				for (Entry<String, String> e : repMap.entrySet()) {
					ifdLine = e.getValue();
					newLine();
				}
			}
		} catch (Exception e) {
			addException(e);
		}
	}

	protected void createFindingAid(String url) {
		extractor = new CrawlerExtractor();
		findingAid = (FAIRSpecFindingAid) extractor.getFindingAid();
		// add DOI for repository
		addDOI(findingAid.getCollectionSet(), url);
		try {
			String line = ifdList.get(0);
			String pubdoi = getTabField(line, "PUBDOI");
			if (pubdoi != null)
				extractor.processPubCrossRef(pubdoi);
			for (int i = 1, n = ifdList.size(); i < n; i++) {
				line = ifdList.get(i);
				System.out.println(line);
				if (line.startsWith(">R")) {
					addRepresentation(line);
				} else if (line.indexOf(FAIRSPEC_COMPOUND_ID) >= 0) {
					addCompound(line);		
				} else if (line.indexOf(FAIRSPEC_TYPE) >= 0) {
					addDataObject(line);
				}
			}
			extractor.generateFindingAid(fileDir);
		} catch (Exception e) {
			addException(e);
		}
	}
	
	private void addDOI(IFDObject<?> o, String url) {
		o.setReference(new IFDReference(null, null, null, null, url));
	}

	private void addCompound(String line) throws IFDException {
		FAIRSpecCompoundAssociation c = extractor.addCompound(null);
		addProperties(c, line);
	}

	private void addProperties(IFDObject<?> c, String line) {
		for (int pt = -1; (pt = line.indexOf(TAB_PROPERTY_KEY, pt + 1)) >= 0;) {
			int pt1 = line.indexOf('=', pt + 1);
			int pt2 = line.indexOf('\t', pt + 1);
			String key = line.substring(pt, pt1);
			String val = line.substring(pt1 + 1, pt2 < 0 ? line.length() : pt2);
			c.setPropertyValue(key, val);
		}
	}

	private void addDataObject(String line) {		
		String type = "IFD.property.dataobject.fairspec.type";
		type = getTabField(line, type);
	}

	private void addRepresentation(String line) {
		// TODO Auto-generated method stub
		
	}

	protected String getTabField(String line, String key) {
		int pt = line.indexOf("\t" + key + "=");
		if (pt < 0)
			return null;
		pt = pt + key.length() + 2;
		int pt1 = line.indexOf('\t', pt);
		return (pt1 < 0 ? line.substring(pt) : line.substring(pt, pt1));
	}


	// from XMLReader

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
		case "identifier":
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
		case "identifier":
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
			if (s.length() > 0 && !hack("description", s)) {
				addAttrLast(IFDConst.IFD_PROPERTY_DESCRIPTION, s);
			}
			break;
		case "title":
			System.out.println(localName + "=" + s);
			if (s.length() > 0) {
				if (!hack("title", s)) {
					addAttrFirst(IFDConst.IFD_PROPERTY_LABEL, s);
				}
			}
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

	/**
	 * 
	 * @param key e.g. "IFD.property.fairspec.compound.id"
	 * @param val the value as a string
	 */
	private void addIFDSubjectAttribute(String key, String val) {
		Map<String, String> attrs = new HashMap<>();
		attrs.put("subjectscheme", FAIRDATA_SUBJECT_SCHEME);
		attrs.put("valueuri", key);
		processSubject(attrs, val);
	}

	private Map<String, String> getAttributes(boolean andPop) {
		return (andPop ? thisAttrs.pop() : thisAttrs.get(thisAttrs.size() - 1));
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

	private void addError(String err) {
		System.err.println(err);
		errorBuffer.append(err + "\n");
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

	private static int putToFile(byte[] bytes, File f) {
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
	
	public static void main(String[] args) {
		Crawler crawler = new Crawler(args.length > 0 ? args[0] : null);
		String outdir = args.length > 1 ? args[1] : OUTDIR;
		File parent = new File(outdir);
		File dataDir = new File(outdir, "metadata");
		dataDir.mkdirs();
		File fileDir = new File(outdir, "files");
		fileDir.mkdir();
		long t = System.currentTimeMillis();
		crawler.crawl(dataDir, fileDir, () -> {
			File f = new File(parent, "crawler.log");
			System.out.println("writing " + crawler.log.length() + " bytes " + f.getAbsolutePath());
			putToFile(crawler.log.toString().getBytes(), f);
			f = new File(parent, "crawler.ifd.txt");
			System.out.println("writing " + crawler.ifdList.size() + " entries " + f.getAbsolutePath());
			System.out.println(crawler.nReps + " representations");
			try {
				FileOutputStream fos = new FileOutputStream(f);
				for (int i = 0; i < crawler.ifdList.size(); i++) {
					fos.write(crawler.ifdList.get(i).getBytes());
					fos.write((byte) '\n');
				}
				fos.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		if (crawler.errorBuffer.length() > 0) {
			System.err.println(crawler.errorBuffer.toString());
		}
		System.out.println("done len = " + crawler.totalLength + " bytes " + (System.currentTimeMillis() - t) / 1000 + " sec");
	}

}
