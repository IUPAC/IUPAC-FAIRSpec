package com.integratedgraphics.extractor;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;

import javax.imageio.ImageIO;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.common.IFDUtil;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecFindingAid;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecFindingAidHelper;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;
import org.iupac.fairdata.core.IFDObject;
import org.iupac.fairdata.core.IFDReference;
import org.iupac.fairdata.dataobject.IFDDataObject;
import org.iupac.fairdata.extract.DefaultStructureHelper;
import org.iupac.fairdata.structure.IFDStructureRepresentation;
import org.openscience.cdk.interfaces.IAtomContainer;

import com.integratedgraphics.extractor.ExtractorUtils.ArchiveInputStream.CrawlerInputStream;
import com.integratedgraphics.util.XmlReader;

import swingjs.CDK;

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
 * IFD.findingaid.json The IUPAC FAIRSpec Finding Aid
 * 
 * _ifd_fileURLMap.txt A concise listing of the digital items
 * 
 * crawler.log A log of the crawl
 * 
 * Metadata is cached in the outputDirectory/metadata/ folder
 * 
 * @author Bob Hanson
 *
 */

public class DOICrawler extends FindingAidCreator {

	protected static final String codeSource = "https://github.com/IUPAC/IUPAC-FAIRSpec/blob/main/src/main/java/com/integratedgraphics/extractor/DOICrawler.java";

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
	protected static final String DATACITE_DESCRIPTION = "description";
	protected static final String DATACITE_TITLE = "title";
	protected static final String DATACITE_SUBJECT = "subject";
	protected static final String DATACITE_REFERENCES = "References";
	protected static final String DATACITE_RELATEDIDENTIFIER = "relatedidentifier";

	/**
	 * A class to allow some adjustments. See ICLDOICrawler.
	 * 
	 * @author Bob Hanson (hansonr@stolaf.edu)
	 *
	 */
	protected interface DOICustomizer {

		String customizeSubjectKey(String key);

		boolean customizeText(String key, String val);

		boolean ignoreURL(String url);

	}

	@SuppressWarnings("serial")
	protected static class DoiRecord extends TreeMap<String, Object> {
		static int test;
		public String dataObjectType;
		public boolean hasRepresentations;
		public String mediaType;
		public LinkedHashMap<String, List<String>> relatedItems;
		String compoundID;
		IFDReference ifdRef;
		byte[] data;
		int length;
		String pidPath;
		char type = '?';
		protected List<DoiRecord> itemList;
		protected String label;
		protected String sortKey;

		int thisTest;
		private File localFile;
		public String pidDescription;

		DoiRecord(String id, String url, String dirName, String localName, File localFile) {
			thisTest = ++test;
			compoundID = id;
			ifdRef = new IFDReference(null, url, dirName, localName);
			if (url.startsWith(DOI_ORG))
				ifdRef.setDOI(url);
			else
				ifdRef.setURL(url);
			//System.out.println("DOIC id=" + id + " localFile=" + localFile);
			this.localFile = localFile;
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

		public String getSortKey(String ckey, String dkey) {
			if (sortKey == null) {
				switch (type) {
				case DOI_COMP:
					return "C_" + IFDUtil.getNumericalSortKey(compoundID);
				case DOI_DATA:
					return sortKey = dkey = ckey + "_" + pidPath;
				case DOI_REP:
					return sortKey = dkey + "_" + ifdRef.getLocalName();
				case DOI_TOP:
					return sortKey = "A_";
				default:
					return sortKey = "Z_" + ckey + "?";
				}
			}
			return sortKey;
		}

		@Override
		public String toString() {
			return "[doiRecord#" + thisTest + " " + type + (dataObjectType != null ? "." + dataObjectType : "")
					+ " cmpd=" + compoundID + ": " + ifdRef
					// + " " + super.toString()
					+ "]" + pidDescription;
		}

		void addItem(DoiRecord rep) {
			if (itemList == null)
				itemList = new ArrayList<>();
			itemList.add(rep);
		}

		void addProperty(String key, String val) {
			switch (key) {
			case "resourcetype":
			case "resourcetypegeneral":
				return;
			}
			if (key.endsWith(".label"))
				label = val;
			put(key, val);
		}

		@Override
		public Object put(String key, Object val) {
			if (key.startsWith(FAIRSPEC_DATAOBJECT_FLAG)) {
				String type = key.substring(FAIRSPEC_DATAOBJECT_FLAG.length());
				dataObjectType = type.substring(0, type.indexOf('.'));
			}
			if (val instanceof String)
				val = cleanData((String) val);
			return super.put(key, val);
		}

		/**
		 * 
		 * @param c
		 * @param rec
		 */
		protected void addProperties(IFDObject<?> c, DOICrawler crawler) {
			boolean isData = (type == DOI_DATA);
			boolean isDOI = false;
			String identifier = null;
			for (Entry<String, Object> e : entrySet()) {
				String key = e.getKey();
				Object value = e.getValue();
				switch (key) {
				case "schemalocation":
					continue;
				case "identifier":
					identifier = value.toString();
					continue;
				case "identifiertype":
					isDOI = value.equals("DOI");
					continue;
				}

				boolean isStructureProp = false, isCompoundProp = false;
				// yes, assignments here
				if (!(isCompoundProp = key.contains(FAIRSPEC_COMPOUND_FLAG))
						&& !(isStructureProp = key.contains(IFDConst.IFD_STRUCTURE_FLAG))
						&& !(key.contains(IFDConst.IFD_DATAOBJECT_FLAG))) {
				}
				if (isData && key.indexOf("compound") >= 0)
					System.out.println(key);
				if (isData && isCompoundProp) {
					// sometimes a collection doubles as a dataset
				} else if (isStructureProp) {
					// System.out.println("addProp " + c.getClass().getSimpleName() + " " + key +
					// "=" + value);
					crawler.addStructureRepresentationsFromProperties(key, value);
				} else {
					c.setPropertyValue(key, value, compoundID);
				}
			}
			if (identifier != null) {
				if (isDOI) {
					if (!identifier.startsWith("https://"))
						identifier = DOIInfoExtractor.DOI_ORG + identifier;
					c.setPropertyValue(IFDConst.IFD_PROPERTY_DOI, identifier);
				} else {
					c.setPropertyValue("identifier", identifier);
				}
			}
		}

	}

	private static class DOIXMLReader extends XmlReader {

		private DOICrawler crawler;
		private DOICustomizer customizer;
		private boolean skipping;
		private Stack<Map<String, String>> thisAttrs = new Stack<>();

		public DOIXMLReader(DOICrawler crawler, DOICustomizer customizer, boolean isTop, StringBuffer log) {
			super(log);
			// if not isTop, we skip the initial business
			skipping = !isTop;
			this.crawler = crawler;
			this.customizer = customizer;
		}

		protected Map<String, String> getAttributes(boolean andPop) {
			return (andPop ? thisAttrs.pop() : thisAttrs.get(thisAttrs.size() - 1));
		}

		@Override
		protected void processEndElement(String localName) {
			String s = chars.toString().trim();
			chars.setLength(0);
			Map<String, String> attrs;
			switch (localName) {
			case DATACITE_DESCRIPTION:
				if (s.length() > 0 && !crawler.customizeText(DATACITE_DESCRIPTION, s)) {
					crawler.addAttr(IFDConst.IFD_PROPERTY_DESCRIPTION, s);
				}
				break;
			case DATACITE_TITLE:
				if (s.length() > 0) {
					if (!crawler.customizeText(DATACITE_TITLE, s)) {
						crawler.addAttr(IFDConst.IFD_PROPERTY_LABEL, s);
					}
				}
				break;
			case DATACITE_SUBJECT:
				attrs = getAttributes(true);
				addSubject(attrs, s);
				break;
			case DATACITE_RELATEDIDENTIFIER:
				if (s.length() > 0) {
					crawler.logAttr(DATACITE_RELATEDIDENTIFIER, s);
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
			case DATACITE_TITLE:
				isData = false;
				skipping = false;
				break;
			case DATACITE_RELATEDIDENTIFIER:
			case DATACITE_SUBJECT:
			case DATACITE_DESCRIPTION:
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

		private void addSubject(Map<String, String> attrs, String s) {
			// proposed plan:
			//
			// <subjects>
			// <subject
			// subjectScheme="IFD"
			// schemeURI="http://iupac.org/ifd"
			// valueURI="http://iupac.org/ifd/IFD.compound.id">21</subject>
			// </subjects>
			//
			String key = attrs.get("subjectscheme");
			if (key == null) {
				crawler.customizeText(DATACITE_SUBJECT, s);
			} else {
				switch (key) {
				case FAIRDATA_SUBJECT_SCHEME:
					key = attrs.get("valueuri");
					if (key.startsWith(IFD_SCHEME_URI)) {
						key = key.substring(key.lastIndexOf('/') + 1);
					}
					break;
				default:
					key = customizeSubectKey(key);
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

		private String customizeSubectKey(String key) {
			return (customizer == null ? key : customizer.customizeSubjectKey(key));
		}

	}

//	public final static String DATACITE_METADATA = "https://data.datacite.org/application/vnd.datacite.datacite+xml/";
	public final static String DATACITE_METADATA = "https://api.datacite.org/application/vnd.datacite.datacite+xml/";

	public final static String FAIRDATA_SUBJECT_SCHEME = "IFD";

	public final static String IFD_SCHEME_URI = "http://iupac.org/ifd";

	protected static final String DEFAULT_OUTDIR = ".";// "c:/temp/iupac/crawler";
	protected static final char DOI_COMP = 'C';
	protected static final char DOI_DATA = 'D';
	protected static final char DOI_REP = 'R';
	protected static final char DOI_TOP = 'T';

	protected static final String DOI_ORG = DOIInfoExtractor.DOI_ORG;

	protected final static String DOWNLOAD_TYPES = ";zip;jdx;png;pdf;a2r;jpg;jpeg;cdxml;mol;cif;xls;xlsx;mnova;mnpub;";
	protected static final String FAIRSPEC_COMPOUND_FLAG = "IFD.property.fairspec.compound.";
	protected static final String FAIRSPEC_COMPOUND_ID = "IFD.property.fairspec.compound.id";
	protected static final String FAIRSPEC_DATAOBJECT_FLAG = "IFD.property.dataobject.fairspec.";

	protected static final String IFD_STANDARDINCHI = "IFD.representation.structure.standard_inchi";
	protected static final String IFD_FIXEDHINCHI = "IFD.representation.structure.fixedh_inchi";
	protected static final String IFD_INCHI = "IFD.representation.structure.inchi";
	protected static final String IFD_INCHIKEY = "IFD.representation.structure.inchikey";
	protected static final String IFD_SMILES = "IFD.representation.structure.smiles";
	protected static final String IFD_IMAGE_PNG = "IFD.representation.structure.png";

	private static String indent = "                              ";

	private static Comparator<DoiRecord> sorter = new Comparator<DoiRecord>() {

		@Override
		public int compare(DoiRecord o1, DoiRecord o2) {
			return (o1.getSortKey(null, null).compareTo(o2.getSortKey(null, null)));
		}

	};

	protected static String cleanData(String s) {
		return s.replaceAll("\t", "\\\\t").replaceAll("\r\n", "\\\\n").replaceAll("\n", "\\\\n");
	}

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

	// inputs

	private static String getHeaderAttr(String data, String name) {

		int pt = data.indexOf(name + "=");
		if (pt < 0)
			return null;
		int pt2 = data.indexOf('\t', pt);
		return data.substring(pt + name.length() + 1, pt2 > 0 ? pt2 : data.length());
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
	 * the current doiRecord during XML parsing
	 */
	public DoiRecord doiRecord;

	// XML parser fields

	protected StringBuffer log = new StringBuffer();
	protected int xmlDepth;

	// iterative nesting of DOI reference

	private DOICustomizer customizer;
	private URL dataCiteMetadataURL;

	/**
	 * set to false using -nodownload flag
	 */
	private boolean doDownload = true;
	/**
	 * doiList is what we are generating during XML parsing
	 */
	private List<DoiRecord> doiList;

	/**
	 * a map to convert the ICL archive's keys to proper IFD.property keys
	 */

	private String faId;

	private FAIRSpecFindingAid findingAid;
	private int ids = 0;

	private String initialDOI;
	private boolean isSilent = false;

	/**
	 * set true to process NMR datasets for properties.
	 */
	protected boolean extractSpecProperties = false;

	private boolean isTop = true;

	private int nReps;

	private String pidPath;
	protected String pidDescription;

	private long startTime;

	private String subdir;

	private String thisCompoundID;

	private IFDDataObject thisDataObject;

	private String thisDataObjectType;

	private File topDir, dataDir, fileDir;

	private long totalLength;

	private int urlDepth;

	private Stack<String> urlStack;

	private IFDObject<?> thisCompound;

	//private HashMap<String, File> localMap;

	private Set<String> ifdKeysFound;

	private Set<String> ignoreKeys;

	/**
	 * set true to create pid descriptions that include IFD property and
	 * representation keys; this will make messages much more verbose; designed for
	 * possibly just using the extractor with myInputStream
	 */
	private boolean usingCrawlerInputStream;

	private CrawlerInputStream myInputStream;

	protected void setIgnoreKeys(String[] keys) {
		this.ignoreKeys = new HashSet<>();
		for (int i = keys.length; --i >= 0;)
			ignoreKeys.add(keys[i]);
	}

	/**
	 * insitu indicates we want a self-contained finding aid; the landing page will
	 * not load repository images in the case of a Crawler
	 * 
	 * @param args [initialDOI, outputDirectory -insitu]
	 */
	public DOICrawler(String[] args) {
		int arg0 = (args.length > 0 && args[0] == null ? 1 : 0);
		initialDOI = (args.length == arg0 ? null : args[arg0]);
		String flags = processFlags(args, "-nozip");
		if (flags.indexOf("-nodownload;") >= 0) {
			doDownload = false;
		}
		if (flags.indexOf("-extractspecproperties;") >= 0) {
			extractSpecProperties = initializePropertyExtraction();
		}

		if (flags.indexOf("-addifdtypes;") >= 0) {
			faHelper.setDoIFDTypeSerialization(true);
		}
		try {
			dataCiteMetadataURL = getMetadataURL(initialDOI);
		} catch (MalformedURLException e) {
			addException(e);
		}
		String outdir = args.length > 1 ? args[1] : DEFAULT_OUTDIR;
		topDir = new File(outdir);
//		if (insitu)
//			localMap = new HashMap<>();
	}

	/**
	 * For future use in structure file checking.
	 * 
	 * @param key
	 * @param val       in the case of a representation, this will be an Object[]
	 *                  consisting of [ bytes, fileName, ifdStructureType,
	 *                  standardInchi|?, mediaType ]
	 * @param isInLine
	 * @param mediaType
	 * @param method
	 */
	@Override
	public void addDeferredPropertyOrRepresentation(String key, Object val, boolean isInLine, String mediaType,
			String note, String method) {
		// TODO Auto-generated method stub

	}

	public boolean crawl(File topDir, File dataDir, File fileDir) {
		this.topDir = topDir;
		this.dataDir = dataDir;
		this.fileDir = fileDir;
		return crawl();
	}

	boolean crawl() {
		// set extractSpecProperties true to retrieving data files
		// otherwise, URLs are just touched using the "head" option in https
		faId = initialDOI.replace('/', '_');
		targetPath = new File(topDir, faId);
		targetPath.mkdirs();
		faHelper = new FAIRSpecFindingAidHelper(getCodeSource() + " " + getVersion());
		findingAid = faHelper.getFindingAid();
		findingAid.setID(faId);
		if (doDownload && fileDir == null) {
			fileDir = new File(targetPath + "_data", "files");
		} else if (fileDir == null) {
			doDownload = false;
		}
		if (dataDir == null)
			dataDir = new File(targetPath + "_data", "metadata");
		log = new StringBuffer();
		startTime = System.currentTimeMillis();
		doiList = new ArrayList<DoiRecord>();
		urlStack = new Stack<String>();
		ifdKeysFound = new HashSet<String>();
		pidDescription = "";
		try {
			if (usingCrawlerInputStream)
				crawlerInputStream = myInputStream = new CrawlerInputStream();
		} catch (IOException e) {
		}
		dataDir.mkdirs();
		nextDOI(dataCiteMetadataURL);
		outputListAndLog(targetPath);
		createFindingAid();
		createExtractorFilesJSON(-1, -1, true);
		System.out.println(
				"done len = " + totalLength + " bytes " + (System.currentTimeMillis() - startTime) / 1000 + " sec");
		return true;
	}

	public void doEndXMLElement(String localName, StringBuffer chars) {
	}

	@Override
	public String getCodeSource() {
		return codeSource;
	}

	@Override
	public String getVersion() {
		return "DoiCrawler " + version;
	}

	public String newCompound(String id) {
		thisCompoundID = doiRecord.compoundID = id;
		doiRecord.type = DOI_COMP;
		System.out.println("!!!!DOICrawler.newCompound " + id);
		return id;
	}

	public void setCustomizer(DOICustomizer customizer) {
		this.customizer = customizer;
	}

	public void setDataObjectType(String type) {
		doiRecord.dataObjectType = type;
		addPidDescription("IFD.dataobject.type", type);
	}

	public void setResourceType(char doiType) {
		pidPath += doiType;
		if (doiRecord.type == '?')
			doiRecord.type = doiType;
	}

	protected void addAttr(String key, String value) {

//		IFD.property.dataobject.fairspec.comp.description
//		IFD.property.dataobject.fairspec.xrd.description
//		IFD.property.dataobject.fairspec.ir.description
//		IFD.Comp.IR
//		IFD.Comp.NMR
		// "compound.id"

		if (ignoreKeys != null && ignoreKeys.contains(key)) {
			log("! ignoring unknown key " + key);
			return;
		}
		doiRecord.addProperty(key, cleanData(value));
		logAttr(key, value);
	}

	protected void addException(Throwable e) {
		e.printStackTrace();
		logAttr("exception", e.getClass().getName() + ": " + e.getMessage());
	}

	/**
	 * generate from InChI or SMILES the Fixed-H InChI, InChIKey, and CDK-SMILES
	 * also generate a 2D PNG image that will be saved in the finding aid.
	 * 
	 * @param key
	 * @param value
	 */
	protected void addStructureRepresentationsFromProperties(String key, Object value) {
		// structure props found in dataset collection
		faHelper.createStructureRepresentation(key, null, value, value.toString().length(), key, null);
		IAtomContainer mol = null;
		String from = "";
		String err = null;
		try {
			switch (key) {
			case IFD_SMILES:
				mol = CDK.getCDKMoleculeFromSmiles(value.toString());
				from = "from SMILES";
				if (mol == null) {
					err = "invalid! SMILES could not be interpreted: " + value;
				}
				break;
			case IFD_INCHI:
				mol = CDK.getCDKMoleculeFromInChI(value.toString(), "fixamide fixacid");
				from = "from InChI";
				if (mol == null) {
					err = "invalid! InChI could not be interpreted: " + value;
				}
				break;
			default:
				return;
			}
			String cdkSmiles = null, inchi = null, fixedHInchi = null;
			if (err == null) {
				inchi = CDK.getInChIFromCDKMolecule(mol, "");
				if (inchi == null) {
					err = inchi = "invalid! InChI could not be generated from " + value;
				}
			}
			if (err == null)
				fixedHInchi = CDK.getInChIFromCDKMolecule(mol, "FixedH");
			else
				inchi = err;
			IFDStructureRepresentation ref;
			if (!value.equals(inchi)) {
				ref = faHelper.createStructureRepresentation(null, null, inchi, 0, IFD_STANDARDINCHI, null);
				ref.addNote(null);
				ref.addNote(from);
				if (err == null) {
					try {
						String inchiKey = CDK.getInChIKey(mol, "");
						ref = faHelper.createStructureRepresentation(null, null, inchiKey, 0, IFD_INCHIKEY, null);
						ref.addNote(null);
						ref.addNote(from);
					} catch (Throwable t) {
						System.out.println(t);
					}
				}
			}
			if (fixedHInchi != null && !fixedHInchi.equals(value)) {
				ref = faHelper.createStructureRepresentation(null, null, fixedHInchi, 0, IFD_FIXEDHINCHI, null);
				ref.addNote(null);
				ref.addNote(from);
			}
			if (mol != null) {
				cdkSmiles = CDK.getSmilesFromCDKMolecule(mol);
				if (!value.equals(cdkSmiles)) {
					if (cdkSmiles == null || cdkSmiles.length() == 0) {
						err = cdkSmiles = "invalid! SMILES could not be calculated for " + value;
					}
				}
				ref = faHelper.createStructureRepresentation(null, null, cdkSmiles, 0, IFD_SMILES, null);
				ref.addNote(null);
				ref.addNote(from);
				if (mol.getAtomCount() != 0) {
					addStructureImage(CDK.getImageFromCDKMolecule(mol, false), from);
				}
			}
			if (err != null)
				logErr(err, "addStructureRepresentationsFromProperties");
		} catch (Exception e) {
			e.printStackTrace();
			log("!!error processing " + value);
		}
	}

	private void addStructureImage(BufferedImage bi, String note) {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ImageIO.write(bi, "png", bos);
			byte[] bytes = bos.toByteArray();
			int width = bi.getWidth();
			int height = bi.getHeight();
			if (width > 0 && height > 0) {
				IFDStructureRepresentation ref = faHelper.createStructureRepresentation(null, null, bytes, 0,
						IFD_IMAGE_PNG, "image/png");
				ref.addNote(null);
				ref.addNote(note + " [" + width + "," + height + "]");
			}
		} catch (IOException e) {
			logErr("error creating image " + note, "addImage");
		}
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

	protected void createFindingAid() {
		String url = DOI_ORG + initialDOI;
		// add DOI for repository
		// TODO how do we get the URL of the finding aid into the finding aid?
		// findingAid.setDOI(url);
		findingAid.getCollectionSet().setURL(url);
		try {
			DoiRecord rec = doiList.get(0);
			Object pubdoi = rec.remove("PUBDOI");
			String datadoi = url;
			processDOIURLs(pubdoi == null ? null : pubdoi.toString(), datadoi, faHelper);
			nestRecords();
			processRecords(null, doiList);
//			if (insitu) {
//				internalizeAllStructureImages();
//			}
			String aid = faHelper.generateFindingAid(targetPath);
			if (aid != null && createLandingPage) {
				buildSite(targetPath);
			}

		} catch (Throwable e) {
			addException(e);
		}
	}

//	private void internalizeAllStructureImages() {
//		IFDStructureCollection c = faHelper.getStructureCollection();
//		for (int i = c.size(); --i >= 0;) {
//			IFDStructure s = c.get(i);
//			for (int j = s.size(); --j >= 0;) {
//				IFDStructureRepresentation r = s.get(j);
//				String m = (r.getData() == null ? r.getMediaType() : null);
//				if (FAIRSpecUtilities.isImageMediaType(m)) {
//					String key = r.getRef().getURL();
//					System.out.println(key);
//					File localFile = localMap.get(key);
//					if (localFile != null) {
//						try {
//							byte[] bytes = FAIRSpecUtilities.getBytesAndClose(new FileInputStream(localFile));
//							r.setData(bytes);
//						} catch (IOException e) {
//							logErr("Could not open local file " + localFile, "internalizeAllStructureImages");
//							e.printStackTrace();
//						}
//					}
//				}
//			}
//
//		}
//	}

	protected boolean customizeText(String key, String val) {
		switch (key) {
		case DATACITE_SUBJECT:
			switch (val) {
			// ccdc subject
			case "Crystal Structure":
				setDataObjectType("xrd");
				return false;
			}
			break;
		case DATACITE_REFERENCES:
			if (val.indexOf("/ccdc.") >= 0)
				return true;
		}
		return (customizer != null && customizer.customizeText(key, val));
	}

	protected String getTabField(String line, String key) {
		int pt = line.indexOf("\t" + key + "=");
		if (pt < 0)
			return null;
		pt = pt + key.length() + 2;
		int pt1 = line.indexOf('\t', pt);
		return (pt1 < 0 ? line.substring(pt) : line.substring(pt, pt1));
	}

	protected void logAttr(String key, String value) {
		appendLog(key + "=" + value);
		if (key.startsWith("IFD.") && urlDepth > 1) {
			ifdKeysFound.add(key);
			// skip main entry
			addPidDescription(key, value);
		}

	}

	private void addPidDescription(String key, String value) {
		if (usingCrawlerInputStream)
			pidDescription += key + "=" + value.replace('\n', ' ').replace('\r', ' ') + "_;_";
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
				String id = rec.compoundID;
				if (id == thisCompoundID) {
					if (rec.dataObjectType == null)
						o = thisCompound;
				} else {
					thisCompoundID = rec.compoundID;
					thisCompound = o = faHelper.createCompound(thisCompoundID);
					if (o.getDOI() == null) {
						o.setDOI(rec.ifdRef.getDOI());
						o.setURL(rec.ifdRef.getURL());
						// o.setReference(rec.ifdRef);
					}
				}
				thisDataObject = null;
				if (rec.itemList != null) {
					if (rec.dataObjectType != null) {
						o = setDataObject(rec);
					}
					processRecords(rec.dataObjectType, rec.itemList);
				}
				break;
			//$FALL-THROUGH$
			case DOI_DATA:
				o = setDataObject(rec);
				break;
			case DOI_REP:
				String structureType = null;
				if (rec.dataObjectType == null) {
					// isStructure
					String localName = rec.ifdRef.getLocalName();
					String ext = localName.substring(localName.lastIndexOf(".") + 1);
					structureType = DefaultStructureHelper.getType(ext, null, false);
					if (structureType != null) {
						String key = rec.localFile.getAbsolutePath();
						faHelper.createStructureRepresentation(key, rec.ifdRef, rec.data, rec.length, structureType,
								rec.mediaType);
					}
				}

				if (structureType == null) {
					if (thisDataObject == null) {
						log("!!no data object for " + rec.ifdRef.getLocalName() + " " + rec.ifdRef.getURL());
					} else {
						if (thisDataObjectType == null) {
							thisDataObjectType = "unknown";
							log("!!no data type found for " + rec);
						}
						if (!thisDataObjectType.startsWith(FAIRSPEC_DATAOBJECT_FLAG))
							thisDataObjectType = FAIRSPEC_DATAOBJECT_FLAG + thisDataObjectType;
						rec.dataObjectType = thisDataObjectType;
						faHelper.createDataObjectRepresentation(rec.ifdRef, rec.data, rec.length, rec.dataObjectType,
								rec.mediaType);
					}
				}
				break;
			}
			if (o != null) {
				System.out.println(i + " for " + faHelper.getThisCompound() + "\n " + faHelper.getCurrentStructure()
						+ "\n " + faHelper.getCurrentSpecData());
				rec.addProperties(o, this);
			}
		}
	}

	private IFDObject<?> setDataObject(DoiRecord rec) {
		IFDDataObject o = thisDataObject = faHelper.createDataObject("" + ++ids, rec.dataObjectType);
		o.setDOI(rec.ifdRef.getDOI());
		o.setURL(rec.ifdRef.getURL());
		thisDataObjectType = rec.dataObjectType;
		return o;
	}

	private void addRecord(DoiRecord doiRecord) {
		doiRecord.pidPath = pidPath;
		System.out.println("adding " + doiRecord);
		doiList.add(doiRecord);
	}

	// from XMLReader

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

	/**
	 * We could download the files for checking or extracting metadata.
	 * 
	 * @param url
	 * @param dir
	 * @param file
	 * @throws IOException
	 */
	private boolean downloadCheck(URL url, File dir, File file) throws IOException {
		long modTime = file.lastModified();
		System.out.println("DOICrawler checking for download of " + file);
		if (modTime > 0 && modTime < startTime) {
			// existed before we started crawling
			return true;
		}
		String s = file.getName();
		int pt = s.lastIndexOf(".");
		if (pt > 0 && FAIRSpecUtilities.isOneOf(s.substring(pt + 1), DOWNLOAD_TYPES)) {
			if (modTime > 0) {
				log("! replacing " + file.getName() + " with " + url);
			}
			URLConnection con = url.openConnection();
			InputStream is = con.getInputStream();
			dir.mkdirs();		
			int n = FAIRSpecUtilities.putToFile(FAIRSpecUtilities.getBytesAndClose(is), file);
			if (n == 0) {
				log("!!URL returns 0 bytes! for " + file.getName() + " at " + url);
			} else if (extractSpecProperties) {
				return true;
			}
		}
		return false;
	}

	private boolean ignoreURL(String url) {
		if (customizer != null && customizer.ignoreURL(url)) {
			log("DOICrawler customizer ignoring " + url);
			return true;
		}
		return false;
	}

	// logging

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

	/**
	 * Iterative loop for DOI traversal
	 * 
	 * @param doi starting with null
	 * @return false if ignored
	 */
	private boolean nextDOI(URL doi) {
		String currentPath = pidPath;
		String currentDesc = pidDescription;
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
			DoiRecord doiRecord = new DoiRecord(thisCompoundID, url, null, null, null);
			addRecord(doiRecord);
			if (doi.equals(dataCiteMetadataURL)) {
				doiRecord.type = DOI_TOP;
			}
			parseXML(doiRecord, isTop, is);
			if (doiRecord.type == DOI_DATA && doiRecord.dataObjectType == null && doiRecord.hasRepresentations) {
				doiRecord.dataObjectType = "unknown";
				log("!!No data type found for " + doiRecord);
			}
		} catch (Exception e) {
			addException(e);
		}
		logAttr("close", doi.toString());
		popURLStack(currentPath, currentDesc);
		return true;
	}

	private void outputListAndLog(File dir) {
		File f = new File(dir, "_IFD_fileURLMap.txt");
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
		String keys = getFoundKeysAsString();
		System.out.println(keys);
		log.append(keys);
		f = new File(dir, "crawler.log");
		log.append(errorLog);
		System.out.println("writing " + log.length() + " bytes " + f.getAbsolutePath());
		FAIRSpecUtilities.putToFile(log.toString().getBytes(), f);
	}

	private String getFoundKeysAsString() {
		String s = "";
		for (String key : ifdKeysFound) {
			s += key + "\n";
		}
		return s;
	}

	/**
	 * Parse the XML document for desired information.
	 * 
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

	private void popURLStack(String currentPath, String currentDesc) {
		pidPath = currentPath;
		pidDescription = currentDesc;
		urlStack.pop();
		urlDepth--;
	}

	private void processRelated(Map<String, List<String>> map) {
		try {
			List<String> list = map.get("URL");
			if (list != null) {
				Set<String> fileNames = new HashSet<>();
				for (int i = list.size(); --i >= 0;) {
					processRepresentation(subdir, newURL(list.get(i)), fileNames);
				}
				// System.out.println(doiRecord);
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

	/**
	 * Process a representation. Uses the HEAD method for https connections to get
	 * the local file name and the length of the file. We do not need to actually
	 * get the file, but if we wanted to, this code could be adapted to extract
	 * metadata from the files as well.
	 * 
	 * @param s
	 * @throws IOException
	 */
	private void processRepresentation(String subdir, URL url, Set<String> fileNames) throws IOException {
		urlDepth++;
		nReps++;
		File headerFile = new File(dataDir, FAIRSpecUtilities.cleanFileName(url.toString()) + ".txt");
		String fileName = null, mediaType = null;
		int len = 0;
		if (headerFile.exists()) {
			byte[] bytes = FAIRSpecUtilities.getBytesAndClose(new FileInputStream(headerFile));
			String data = new String(bytes);
			fileName = getHeaderAttr(data, "filename");
			String length = getHeaderAttr(data, "length");
			mediaType = getHeaderAttr(data, "mediaType");
			if (length != null) {
				len = IFDUtil.parsePositiveInt(length);
			}
		} else {
			// get fileName, mediaType, and length
			// note that length may be 0 (unknown)
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
					log("!!HEAD " + url + " no Content-Length!");
				}
				fileName = FAIRSpecUtilities.getQuotedOrUnquotedAttribute(item.toString(), "filename");
				s += "\tfilename=" + fileName;
			}
			FAIRSpecUtilities.putToFile(s.getBytes(), headerFile);
		}
		if (fileName != null) {
			// a simple name, such as "3.mol"
			if (fileNames.contains(fileName)) {
				// because within a diretory, only the last-given file 
				// of a given name is the valid one, at least at ICL
				log("!skipping duplicate file " + fileName + " for " + subdir);
			} else {
				fileNames.add(fileName);
				File localDir = null;
				File localFile = null;
				if (fileDir != null) {
					localDir = new File(fileDir, subdir);
					localFile = new File(localDir, FAIRSpecUtilities.cleanFileName(fileName));
				}
				String surl = url.toString();
				DoiRecord rec = new DoiRecord(thisCompoundID, surl, null, fileName, localFile);
				rec.type = DOI_REP;
				rec.length = len;
				String dataType = doiRecord.dataObjectType;
				rec.dataObjectType = dataType;
				rec.mediaType = mediaType;
				boolean insituImage = (insitu && FAIRSpecUtilities.isImageMediaType(mediaType));
				boolean doDownload = (extractSpecProperties || insituImage);				
				boolean downloaded = (doDownload && downloadCheck(url, localDir, localFile));
				if (downloaded) {
					if (insituImage) {
						rec.data = FAIRSpecUtilities.getBytesAndClose(new FileInputStream(localFile));
						//localMap.put(surl, localFile);
					}
					if (extractSpecProperties) {
						crawlerExtractSpecProperties(localFile);
					}
				}
				addRecord(rec);
				String name = thisCompoundID + "/" + dataType + "/" + pidDescription
						+ localFile.toString().replace('\\', '/');
				// log("!!!!" + name);
				if (usingCrawlerInputStream)
					myInputStream.addFile(name, surl, thisCompoundID, dataType, subdir, pidDescription, localFile, len);
				totalLength += len;
			}
		}
		urlDepth--;
	}

	/**
	 * Callback from a vendor plugin with a property.
	 */
	@Override
	public void addProperty(String key, Object val) {
		doiRecord.put(key, val);
	}

	public static void main(String[] args) {
		// we don't start this on its own right now
		ICLDOICrawler.main(args);
	}

}
