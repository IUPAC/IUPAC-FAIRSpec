package com.integratedgraphics.extractor;

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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.TreeMap;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecFindingAid;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecFindingAidHelper;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;
import org.iupac.fairdata.core.IFDObject;
import org.iupac.fairdata.core.IFDReference;
import org.iupac.fairdata.dataobject.IFDDataObject;
import org.iupac.fairdata.extract.DefaultStructureHelper;

import com.integratedgraphics.util.XmlReader;

/**
 * A DataCite metadata crawler, resulting in the production of an IUPAC FAIRSpec
 * FindingAid for a repository-based or distributed data collection.
 * 
 * This class is rather specific to the Imperial College London high performance
 * computing archive. But it may serve as a template for others that have
 * implemented DataCite metadata extensively.
 * 
 * The class utilizes a SAX XML reader.
 * 
 * In particular, the following six XML elements are handled:
 * 
 * title, description,
 * 
 * identifier, resourcetype, relatedidentifier,
 * 
 * subject
 * 
 * Program operation:
 * 
 * Start-up String[] arguments: [initialDOI,outputDirectory]
 * 
 * By default three files are created in the output directory:
 * 
 * IFD.findingaid.json   The IUPAC FAIRSpec Finding Aid
 * 
 * ifd-fileURLMap.txt	 A concise listing of the digital items
 * 
 * crawler.log           A log of the crawl
 * 
 * Metadata is cached in the outputDirectory/metadata/ folder
 * 
 * @author Bob Hanson
 *
 */

public class DOICrawler extends FindingAidCreator {

	/**
	 * Imperial College 
	 * 
	 * https://data.hpc.imperial.ac.uk/resolve/?doi=10386
	 * 
	 * https://doi.org/10.14469/hpc/10386
	 * 
	 * is the only archive tested
	 * 
	 */
	protected final static String TEST_PID = "10.14469/hpc/10386";

	/**
	 * A class to allow some adjustments. See ICLDOICrawler.
	 * 
	 * @author hanso
	 *
	 */
	public interface DOICustomizer {

		boolean customizeText(String key, String val);
		String customizeKey(String key);
		boolean ignoreURL(String url);
		
	}


	protected static final String DEFAULT_OUTDIR = "c:/temp/iupac/crawler";
	
	protected StringBuffer log = new StringBuffer();

	protected static final String codeSource = "https://github.com/IUPAC/IUPAC-FAIRSpec/blob/main/src/main/java/com/integratedgraphics/extractor/DOICrawler.java";

	protected static final String version = "0.0.5-alpha+2024.11.04";

	// 2024.05.28 version 0.0.1 initial version used for ICL repository crawling

	protected final static String DOWNLOAD_TYPES = ";zip;jdx;png;pdf;a2r;jpg;jpeg;cdxml;mol;cif;xls;xlsx;mnova;mnpub;";
	public final static String DATACITE_METADATA = "https://data.datacite.org/application/vnd.datacite.datacite+xml/";
	public final static String FAIRSPEC_SCHEME_URI = "http://iupac.org/ifd";
	public final static String FAIRDATA_SUBJECT_SCHEME = "IFD";

	protected static final String DOI_ORG = DOIInfoExtractor.DOI_ORG;

	protected static final String FAIRSPEC_COMPOUND_ID = "IFD.property.fairspec.compound.id";

	protected static final String IFD_INCHI = "IFD.representation.structure.inchi";
	protected static final String IFD_INCHIKEY = "IFD.representation.structure.inchikey";
	protected static final String FAIRSPEC_DATAOBJECT_FLAG = "IFD.property.dataobject.fairspec.";
	protected static final String IFD_SMILES = "IFD.representation.structure.smiles";

	protected static final char DOI_REP = 'R';
	protected static final char DOI_COMP = 'C';
	protected static final char DOI_DATA = 'D';
	protected static final char DOI_TOP = 'T';
	
	protected static class DoiRecord {

		String compoundID;
		char type; 
		IFDReference ifdRef;
		Map<String, String> properties = new TreeMap<>();
		String pidPath;
		int length;
		public String mediaType;
		public String dataObjectType;
	
		protected List<DoiRecord> itemList;
		protected String sortKey;
		protected String label;
		public LinkedHashMap<String, List<String>> relatedItems;
		public boolean hasRepresentations;

		DoiRecord(String id, String url, String dirName, String localName) {
			compoundID = id;
			ifdRef = new IFDReference(null, url, dirName, localName);
			if (url.startsWith(DOI_ORG))
				ifdRef.setDOI(url);
			else
				ifdRef.setURL(url);
		}

		void addItem(DoiRecord rep) {
			if (itemList == null)
				itemList = new ArrayList<>();
			itemList.add(rep);
		}
		
		void addProperty(String key, String val) {
			if (key.endsWith(".label"))
				label = val;
			properties.put(key, val);
		}

		public byte[] getBytes() {
			String s = pidPath;
			switch (type) {
			case DOI_REP:
				s += ">R";
				break;
			}
	    	s += "\t" + ifdRef + (label == null ? "" : "\t" + label);
			return s.getBytes();
		}

		@Override
		public String toString() {
			return "[doiRecord " + type + (type == DOI_DATA ? "." + dataObjectType : "") + " " + compoundID + ": " + ifdRef.getLocalName() + " mt=" + mediaType + " " + properties + "]";
		}

		public String getSortKey(String ckey, String dkey) {
			if (sortKey == null) {
				switch (type) {
				case DOI_COMP:
					int num = FAIRSpecUtilities.parseInt(compoundID);
					if (num == Integer.MIN_VALUE) {
						sortKey = (compoundID + "__________"); 
					} else {
						sortKey = "" + num;
						sortKey = ("0000000000" + compoundID).substring(sortKey.length());
					}
					return sortKey = "C_" + sortKey;
				case DOI_DATA:
					return sortKey = dkey = ckey + "_" + pidPath;
				case DOI_REP:
					return sortKey = dkey + "_"+ ifdRef.getLocalName();
				case DOI_TOP:
					return sortKey = "A_";
				default:
					return sortKey = "Z_" + ckey + "?";
				}
			}
			return sortKey;
		}

	}
	
	private static String indent = "                              ";

	// inputs
	
	/**
	 * set to false using -nodownload flag
	 */
	private boolean doDownload = true;
	
	/**
	 * set to true if all related URL groups are of the same compound
	 */
    private boolean byCompound = false;
	
	private String initialDOI;
	private File topDir, dataDir, fileDir;

	// XML parser fields
	
	private URL dataCiteMetadataURL;
	protected int xmlDepth;
	private String pidPath;

	// iterative nesting of DOI reference
	
	private Stack<String> urlStack;
	private int urlDepth;

	/**
	 * doiList is what we are generating during XML parsing
	 */
	private List<DoiRecord> doiList;
	/**
	 * the current doiRecord during XML parsing
	 */
	private DoiRecord doiRecord;

	private int nReps;

	private long startTime;	
	private long totalLength;

	private boolean isTop = true;
	private boolean isSilent = false;

	private String thisCompoundID;
	private String thisDataObjectType;


	

	/**
	 * a map to convert the ICL archive's keys to proper IFD.property keys
	 */

	private File faDir;

	private String faId;

	private String subdir;

	private DOICustomizer customizer;

	private static class DOIXMLReader extends XmlReader {

		private DOICrawler crawler;
		private boolean skipping;
		private Stack<Map<String, String>> thisAttrs = new Stack<>();
		private DOICustomizer customizer;
		protected Map<String, String> getAttributes(boolean andPop) {
			return (andPop ? thisAttrs.pop() : thisAttrs.get(thisAttrs.size() - 1));
		}
		
		public DOIXMLReader(DOICrawler crawler, DOICustomizer customizer, boolean isTop, StringBuffer log) {
			super(log);
			// if not isTop, we skip the initial business
			skipping = !isTop;
			this.crawler = crawler;
			this.customizer = customizer;
		}

		@Override
		protected void processStartElement(String localName, String nodeName) {
			crawler.xmlDepth++;
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
			if (!skipping)
				crawler.logAttr("item", localName);
			for (Entry<String, String> e : atts.entrySet()) {
				String key = e.getKey();
				String val = e.getValue();
				if (isData && !skipping) {
					crawler.addAttr(key, val);
				} else {
					crawler.logAttr(key, val);
				}
			}
			switch (localName) {
			case "resourcetype":
				switch (atts.get("resourcetypegeneral")) {
				case "Collection":
					crawler.setResourceType(DOI_COMP);
					break;
				case "Dataset":
					crawler.setResourceType(DOI_DATA);
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
				if (s.length() > 0 && !crawler.customizeText("description", s)) {
					crawler.addAttr(IFDConst.IFD_PROPERTY_DESCRIPTION, s);
				}
				break;
			case "title":
				if (s.length() > 0) {
					if (!crawler.customizeText("title", s)) {
						crawler.addAttr(IFDConst.IFD_PROPERTY_LABEL, s);
					}
				}
				break;
			case "subject":
				attrs = getAttributes(true);
				addSubject(attrs, s);
				break;
			case "relatedidentifier":
				if (s.length() > 0) {
					crawler.logAttr("relatedidentifier", s);
				}
				crawler.addRelatedIdentifier(getAttributes(true), s);
				break;
			default:
				if (!skipping && s.length() > 0) {
					crawler.logAttr("value", s);
					crawler.addAttr(localName, s);
				}
				break;
			}
			crawler.xmlDepth--;
		}

		private void addSubject(Map<String, String> attrs, String s) {
//			<subjects>
//			    <subject 
//			schemeURI="http://iupac.org/ifd" 
//			subjectScheme="IFD" 
//			valueURI="http://iupac.org/ifd/IFD.compound.id">21</subject>
//			</subjects>
	//
			String key = attrs.get("subjectscheme");
			if (key == null) {
				crawler.customizeText("subject", s);
			} else {
				switch (key) {
				case FAIRDATA_SUBJECT_SCHEME:
					key = attrs.get("valueuri");
					if (key.startsWith(FAIRSPEC_SCHEME_URI)) {
						key = key.substring(key.lastIndexOf('/') + 1);
					}
					break;
				default:
					key = customizeKey(key);
					break;
				}
				if (key.startsWith(FAIRSPEC_DATAOBJECT_FLAG)) {
					String type = key.substring(FAIRSPEC_DATAOBJECT_FLAG.length());
					type = type.substring(0, type.indexOf('.'));
					crawler.setDataObjectType(type);
				}
				crawler.addAttr(key, s);
			}
		}

		private String customizeKey(String key) {
			return (customizer == null ? key : customizer.customizeKey(key));
		}

		
	}

	public void doEndXMLElement(String localName, StringBuffer chars) {
	}

	public void setResourceType(char doiType) {
		pidPath += doiType;
		if (doiRecord.type == '\0')
			doiRecord.type = doiType;
	}

	public void setDataObjectType(String type) {
		doiRecord.dataObjectType = type;
	}

	public String newCompound(String val) {
		String id = "" + FAIRSpecUtilities.parseInt(val.substring(9));
		thisCompoundID = doiRecord.compoundID = id;
		doiRecord.type = DOI_COMP;
		return id;
	}

	@Override
	public String getVersion() {
		return "DoiCrawler " + version;
	}

	@Override
	public String getCodeSource() {
		return codeSource;
	}

	/**
	 * 
	 * @param args [initialDOI, outputDirectory]
	 */
	public DOICrawler(String... args) {
		initialDOI = (args.length == 0 ? TEST_PID : args[0]);
		String flags = processFlags(args, "-nozip");
		if (flags.indexOf("-nodownload;") >= 0) {
			doDownload = false;
		}
		if (flags.indexOf("-bycompound;") >= 0) {
			byCompound = true;
		}

	try {
			dataCiteMetadataURL = getMetadataURL(initialDOI);
		} catch (MalformedURLException e) {
			addException(e);
		}		
		String outdir = args.length > 1 ? args[1] : DEFAULT_OUTDIR;
		topDir = new File(outdir);
	}

	public void setCustomizer(DOICustomizer customizer) {
		this.customizer = customizer;
	}
	
	public boolean crawl(File topDir, File dataDir, File fileDir) {
		this.topDir = topDir;
		this.dataDir = dataDir;
		this.fileDir = fileDir;
		return crawl();
	}

	public boolean crawl() {
		// not retrieving data files
		// but URLs are touched using the "head" option in https
		faId = initialDOI.replace('/', '_');
		faDir = new File(topDir, faId);
		faDir.mkdirs();
		faHelper = new FAIRSpecFindingAidHelper(getCodeSource() + " " + getVersion());
		findingAid = faHelper.getFindingAid();
		findingAid.setID(faId);
		if (doDownload && fileDir == null) {
			fileDir = new File(faDir, "files");
		} else if (fileDir == null) {
			doDownload = false;
		}
		if (dataDir == null)
			dataDir = new File(faDir, "metadata");
		log = new StringBuffer();
		startTime = System.currentTimeMillis();
		doiList = new ArrayList<DoiRecord>();
		//repMap = new TreeMap<>();
		urlStack = new Stack<String>();
		dataDir.mkdirs();
		if (fileDir != null)
			fileDir.mkdirs();
		nextDOI(dataCiteMetadataURL);
		outputListAndLog(faDir);
		createFindingAid();
		if (errorBuffer.length() > 0) {
			System.err.println(errorBuffer.toString());
		}
		System.out.println(
				"done len = " + totalLength + " bytes " + (System.currentTimeMillis() - startTime) / 1000 + " sec");
		return true;
	}

	private void outputListAndLog(File dir) {
		File f = new File(dir, "ifd-fileURLMap.txt");
		System.out.println("writing " + doiList.size() + " entries " + f.getAbsolutePath());
		System.out.println(nReps + " representations");
		try {
			FileOutputStream fos = new FileOutputStream(f);
			for (int i = 0; i < doiList.size(); i++) {
				fos.write(doiList.get(i).getBytes());
				fos.write((byte) '\n');
			}
			fos.close();
		} catch (Exception e) {
			addException(e);
		}
		f = new File(dir, "crawler.log");
		System.out.println("writing " + log.length() + " bytes " + f.getAbsolutePath());
		FAIRSpecUtilities.putToFile(log.toString().getBytes(), f);
	}

	private static URL getMetadataURL(String pid) throws MalformedURLException {
		System.out.println(pid);
		return new URL(DATACITE_METADATA + pid);
	}

	private static URL newURL(String s) throws MalformedURLException {
		s = FAIRSpecUtilities.rep(s, "&amp;", "&");
		return new URL(s);
	}

	/**
	 * Iterative loop for DOI traversal
	 * 
	 * @param doi starting with null
	 * @return false if ignored
	 */
	private boolean nextDOI(URL doi) {
		String currentPath = pidPath;
		String s = doi.toString().replace(DATACITE_METADATA, "");
		urlStack.push(DOI_ORG + s);
		urlDepth++;
		isTop = (urlDepth == 1);
		String sfile = FAIRSpecUtilities.cleanFileName(s) + ".xml";
		int pt = s.lastIndexOf('/');
		String dir = s.substring(0, pt + 1);
		String file = s.substring(pt + 1);
		subdir = file;
		if (pidPath == null) {
			pidPath = s + ">";
		} else if (s.indexOf(dir) == 0) {
			pidPath += ">" + file + ">";
		} else {
			pidPath += ">" + s + ">";
		}
		logAttr("open", doi.toString());
		InputStream is;
		try {
			File dataFile = new File(dataDir, sfile);
			boolean haveData = dataFile.exists();
			System.out.println("reading " + (haveData ? dataFile : doi.toString()));
			if (haveData) {
				is = new FileInputStream(dataFile);
			} else {
				URLConnection con = doi.openConnection();
				is = con.getInputStream();
			}
			byte[] metadata = FAIRSpecUtilities.getBytesAndClose(is);
			if (!haveData)
				FAIRSpecUtilities.putToFile((byte[]) metadata, dataFile);
			is = new ByteArrayInputStream((byte[]) metadata);
			String url = DOI_ORG + s;
			if (ignoreURL(url))
				return false;
			DoiRecord doiRecord = new DoiRecord(thisCompoundID, url, dataDir.toString(), sfile);
			addRecord(doiRecord);
			if (doi.equals(dataCiteMetadataURL)) {
				doiRecord.type = DOI_TOP;	
			}
			parseXML(doiRecord, isTop, is);
			if (doiRecord.type == DOI_DATA && doiRecord.dataObjectType == null 
					&& doiRecord.hasRepresentations) {
				doiRecord.dataObjectType = "unknown";
				addError("!No data type found for " + doiRecord);
			}
		} catch (Exception e) {
			addException(e);
		}
		logAttr("close", doi.toString());
		popURLStack(currentPath);
		return true;
	}

	private boolean ignoreURL(String url) {
		if (customizer != null && customizer.ignoreURL(url)) {
			log("DOICrawler customizer ignoring " + url);
			return true;
		}
		return false;
	}

	protected boolean customizeText(String key, String val) {
		switch (key) {
		case "subject":
			switch (val) {
			// ccdc subject
			case "Crystal Structure":
				setDataObjectType("xrd");
				return false;
			}
			break;
		case "References":
			if (val.indexOf("/ccdc.") >= 0)
				return true;
		}
		return (customizer != null && customizer.customizeText(key, val));
	}
	

	private void popURLStack(String currentPath) {
		pidPath = currentPath;
		urlStack.pop();
		urlDepth--;
	}

	/**
	 * Parse the XML document for desired information.
	 * @param doiRecord 
	 * 
	 * @param content
	 * @throws Exception
	 */
	private void parseXML(DoiRecord doiRecord, boolean isTop, InputStream content) throws Exception {
		this.doiRecord = doiRecord;
		doiRecord.relatedItems = new LinkedHashMap<String, List<String>>(); 
		BufferedReader reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(content), "UTF-8"));
		new DOIXMLReader(this, customizer, isTop, log).parseXML(reader);
		content.close();
		processRelated(doiRecord.relatedItems);
	}

	/**
	 * Process a representation. Uses the HEAD method for https connections
	 * to get the local file name and the length of the file. 
	 * We do not need to actually get the file, but if we wanted to, 
	 * this code could be adapted to extract metadata from the files as well.
	 * 
	 * @param s
	 * @throws IOException
	 */
	private void processRepresentation(String subdir, URL url, Map<String, DoiRecord> fMap) throws IOException {
		urlDepth++;
		nReps++;
		File headerFile = new File(dataDir, FAIRSpecUtilities.cleanFileName(url.toString()) + ".txt");
		boolean haveHeaderFile = headerFile.exists();
		String fileName = null, mediaType = null;
		byte[] bytes = null;
		int len = 0;
		if (haveHeaderFile) {
			bytes = FAIRSpecUtilities.getBytesAndClose(new FileInputStream(headerFile));
			String data = new String(bytes);
			fileName = getHeaderAttr(data, "filename");
			String length = getHeaderAttr(data, "length");
			mediaType = getHeaderAttr(data, "mediaType");
			if (length != null) {
				len = FAIRSpecUtilities.parseInt(length);
				totalLength += len;
			}
		} else {
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("HEAD");
			Map<String, List<String>> map = con.getHeaderFields();
			mediaType = map.get("Content-Type").get(0);
			String s = "\tURL=" + url;
			s += "\tmediaType=" + mediaType;
			List<String> item = map.get("Content-Disposition");
			fileName = null;
			if (item != null) {
				List<String> list = map.get("Content-Length");
				if (list != null && !list.isEmpty()) {
					s += "\tlength=" + list.get(0);
				} else {
					addError("!HEAD " + url + " no Content-Length!");
				}
				fileName = FAIRSpecUtilities.getQuotedOrUnquotedAttribute(item.toString(), "filename");
				s += "\tfilename=" + fileName;
			}
			FAIRSpecUtilities.putToFile(s.getBytes(), headerFile);
		}
		if (fileName != null) {
			if (fileDir != null) {
				File subDir = new File(fileDir, subdir);
				subDir.mkdirs();
				downloadCheck(url, new File(subDir, FAIRSpecUtilities.cleanFileName(fileName)));
			}
			DoiRecord f0 = fMap.remove(fileName);
			if (f0 != null)
				log("!removed " + f0);
			String surl = url.toString();
			//repMap.put(thisCompoundID + "|" + fileName, surl);
			DoiRecord rec = new DoiRecord(thisCompoundID, surl, null, fileName);
			rec.length = len;
			rec.mediaType = mediaType;
			rec.type = DOI_REP;
			fMap.put(fileName, rec);
		}
		urlDepth--;
	}

	private static String getHeaderAttr(String data, String name) {

		int pt = data.indexOf(name + "=");
		if (pt < 0)
			return null;
		int pt2 = data.indexOf('\t', pt);
		return data.substring(pt + name.length() + 1, pt2 > 0 ? pt2 : data.length());
	}

	/**
	 * We could download the files for checking or extracting
	 * metadata. We choose not to do that in this demonstration.
	 * 
	 * @param url
	 * @param file
	 * @throws IOException
	 */
	private void downloadCheck(URL url, File file) throws IOException {
		long modTime = file.lastModified();
		System.out.println(file);
		if (modTime > 0 && modTime < startTime) {
			// existed before we started crawling
			return;
		}
		String s = file.getName();
		int pt = s.lastIndexOf(".");
		if (pt > 0 && FAIRSpecUtilities.isOneOf(s.substring(pt + 1), DOWNLOAD_TYPES)) {
			if (modTime > 0) {
				addError("replacing " + file.getName() + " with " + url);
			}
			URLConnection con = url.openConnection();
			InputStream is = con.getInputStream();
			int n = FAIRSpecUtilities.putToFile(FAIRSpecUtilities.getBytesAndClose(is), file);
			if (n == 0)
				addError("!URL returns 0 bytes! " + url);
		}
	}

	private void addRelatedIdentifier(Map<String, String> attrs, String s) {
		String type = attrs.get("relatedidentifiertype"); // DOI or URL
		String generalType = attrs.get("resourcetypegeneral");
		switch (attrs.get("relationtype")) {
		case "HasPart":
			break;
		case "References":
			// only interested in JournalArticle
			if (generalType == null) {
				if (customizeText("References", s)) {
					break; // treat as "HasPart"
				}
				return;
			}
			switch (generalType) {
			case "JournalArticle":
				if ("DOI".equals(type))
					s = DOI_ORG + s;
				addAttr("PUB" + type, s);
				break;
			}
			return;
		default:
			type = null;
			break;
		}
		if (type != null) {
			if (ignoreURL(s))
				return;
			Map<String, List<String>> map = doiRecord.relatedItems;
			List<String> list = map.get(type);
			if (list == null)
				map.put(type, list = new ArrayList<String>());
			list.add(s);
		}
	}

	private StringBuffer errorBuffer = new StringBuffer();

	private FAIRSpecFindingAid findingAid;

//	/**
//	 * change >C to >D
//	 */
//	private void fixPIDPathForDataset() {
//		pidPath = pidPath.substring(0, pidPath.length() - 1) + "D";
//		addError("Dataset was Collection: " + pidPath);
//	}

	private void processRelated(Map<String, List<String>> map) {
		try {
			List<String> list = map.get("URL");
			if (list != null) {
				Map<String, DoiRecord> fMap = new TreeMap<>();
				for (int i = 0; i < list.size(); i++) {
					processRepresentation(subdir, newURL(list.get(i)), fMap);
				}
				for (Entry<String, DoiRecord> e : fMap.entrySet()) {
					addRecord(e.getValue());
				}
				doiRecord.hasRepresentations = true;
			}
			list = map.get("DOI");
			if (list != null) {
				for (int i = 0; i < list.size(); i++) {
					nextDOI(getMetadataURL(list.get(i)));
				}
			}
		} catch (Exception e) {
			addException(e);
		}
	}

	protected void createFindingAid() {
		String url = DOI_ORG + initialDOI;
		// add DOI for repository
		IFDReference r = new IFDReference();
		r.setDOI(url);
		findingAid.getCollectionSet().setReference(r);
		try {
			DoiRecord rec = doiList.get(0);
			String pubdoi = rec.properties.remove("PUBDOI");
			String datadoi = url;
			processDOIURLs(pubdoi, datadoi, faHelper);
			nestRecords();
			processRecords(null, doiList);
			faHelper.generateFindingAid(faDir);
		} catch (Exception e) {
			addException(e);
		}
	}
	
	private void nestRecords() {
		for (int i = 0; i < doiList.size(); i++) {
			DoiRecord rec = doiList.get(i);
			if (rec.type == DOI_COMP) {
				while (i + 1 < doiList.size()) {
					if (doiList.get(i + 1).type == 'C') {
						break;
					} 
					rec.addItem(doiList.remove(i + 1));
				}
			}
		}
	}

	int ids = 0;

	private IFDDataObject thisDataObject;

	private static Comparator<DoiRecord> sorter = new Comparator<DoiRecord>() {

		@Override
		public int compare(DoiRecord o1, DoiRecord o2) {
			return (o1.getSortKey(null, null).compareTo(o2.getSortKey(null, null)));
		}
		
	};

	protected static void sortRecords(List<DoiRecord> doiList) {
		String ckey = null, dkey = null;
		for (int i = 0; i < doiList.size(); i++) {
			DoiRecord rec = doiList.get(i);
			switch (rec.type) {
			case DOI_COMP:
				ckey = rec.getSortKey(null, null);
				break;
			case DOI_DATA:
				dkey = rec.getSortKey(ckey, null);
				break;
			case DOI_REP:
				rec.getSortKey(ckey, dkey);
				break;
			case DOI_TOP:
			default:
				rec.getSortKey(ckey, null);
				break;
			}
		}
		doiList.sort(sorter);
	}

	protected void processRecords(String dataObjectType, List<DoiRecord> doiList) throws IFDException {
		sortRecords(doiList);
		for (int i = 0; i < doiList.size(); i++) {
			DoiRecord rec = doiList.get(i);
			IFDObject<?> o = null;
			switch (rec.type) {
			case DOI_TOP:
				o = faHelper.getFindingAid().getCollectionSet();
				break;
			case DOI_COMP:
				thisCompoundID = rec.compoundID;
				o = faHelper.createCompound(thisCompoundID);
				o.setDOI(rec.ifdRef.getDOI());
				o.setURL(rec.ifdRef.getURL());
				o.setReference(rec.ifdRef);
				thisDataObject = null;
				if (rec.itemList != null)
					processRecords(rec.dataObjectType, rec.itemList);
				break;
			case DOI_DATA:
				o = thisDataObject = faHelper.createDataObject("" + ++ids, rec.dataObjectType);
				o.setDOI(rec.ifdRef.getDOI());
				o.setURL(rec.ifdRef.getURL());
				thisDataObjectType = rec.dataObjectType;
				break;
			case DOI_REP:
				String structureType = null;
				if (rec.dataObjectType == null) {
					// isStructure
					String ext = rec.ifdRef.getLocalName();
					ext = ext.substring(ext.lastIndexOf(".") + 1);
					structureType = DefaultStructureHelper.getType(ext, null, false);
					if (structureType != null) {
						System.out.println(rec.ifdRef);
						faHelper.createStructureRepresentation(rec.ifdRef, null, rec.length, structureType, rec.mediaType);
					}
				} 
				
				if (structureType == null) {
					if (thisDataObject == null) {
						log("!!no data object for " + rec.ifdRef.getLocalName() + " " + rec.ifdRef.getURL());
					} else {
						if (thisDataObjectType == null) {
							thisDataObjectType = "unknown";
							addError("!no data type found for " + rec);
						}
						if (!thisDataObjectType.startsWith(FAIRSPEC_DATAOBJECT_FLAG))
							thisDataObjectType = FAIRSPEC_DATAOBJECT_FLAG + thisDataObjectType;
						rec.dataObjectType = thisDataObjectType;
						faHelper.createDataObjectRepresentation(rec.ifdRef, null, rec.length, rec.dataObjectType, rec.mediaType);
					}
				}
				break;
			}
			if (o != null) {
				addProperties(o, rec);
			}
		}
	}
	/**
	 * 
	 * @param c
	 * @param rec
	 */
	protected void addProperties(IFDObject<?> c, DoiRecord rec) {
		boolean isData = (rec.type == DOI_DATA);
		for (Entry<String, String> e : rec.properties.entrySet()) {
			String key = e.getKey();
			String value = e.getValue();
			switch (key) {
			case "schemalocation":
				continue;
			}
			if (isData && key.contains(IFDConst.IFD_STRUCTURE_FLAG)) {
				// structure props found in dataset collection
				faHelper.createStructureRepresentation(null, value, value.length(), key, null);
			} else {
				c.setPropertyValue(key, value, rec.compoundID);
			}
		}
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

	protected void addAttr(String key, String value) {
		doiRecord.addProperty(key, cleanData(value));
		logAttr(key, value);
	}


	protected static String cleanData(String s) {
		return s.replaceAll("\t", "\\\\t").replaceAll("\r\n", "\\\\n").replaceAll("\n", "\\\\n");
	}

	private void addRecord(DoiRecord doiRecord) {
		doiRecord.pidPath = pidPath;
		doiList.add(doiRecord);
	}
	
	// logging
	
	protected void addError(String err) {
		System.err.println(err);
		errorBuffer.append(err + "\n");
	}

	protected void logAttr(String key, String value) {
		appendLog(key + "=" + value);
	}

	protected void addException(Exception e) {
		e.printStackTrace();
		logAttr("exception", e.getClass().getName() + ": " + e.getMessage());
	}

	protected void appendLog(String line) {
		if (isSilent)
			return;
		line = line.trim();
		if (xmlDepth * 2 > indent.length())
			indent += indent;
		log.append('\n').append(urlDepth).append(".").append(xmlDepth).append(indent.substring(0, xmlDepth * 2))
				.append(line);
	}

	/**
	 * For future use in dataset file checking.
	 */
	@Override
	public void addProperty(String key, Object val) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * For future use in structure file checking.
	 * 
	 * @param key
	 * @param val in the case of a representation, this will be an Object[]
	 * consisting of [ bytes, fileName, ifdStructureType, standardInchi|?, mediaType ]
	 * @param isInLine
	 * @param mediaType
	 * @param note
	 */
	@Override
	public void addPropertyOrRepresentation(String key, Object val, 
			boolean isInLine, String mediaType, String note) {
		// TODO Auto-generated method stub
		
	}

	public static void main(String[] args) {
		if (args.length == 0)
			ICLDOICrawler.main(args);
		else 
			new DOICrawler(args).crawl();
	}


}
