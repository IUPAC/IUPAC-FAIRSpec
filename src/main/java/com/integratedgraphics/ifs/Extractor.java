package com.integratedgraphics.ifs;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.iupac.fairspec.api.IFSExtractorI;
import org.iupac.fairspec.api.IFSPropertyManagerI;
import org.iupac.fairspec.assoc.IFSStructureDataAssociation;
import org.iupac.fairspec.common.IFSConst;
import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.common.IFSRepresentation;
import org.iupac.fairspec.core.IFSDataObject;
import org.iupac.fairspec.core.IFSDataObjectCollection;
import org.iupac.fairspec.core.IFSFindingAid;
import org.iupac.fairspec.core.IFSObject;
import org.iupac.fairspec.core.IFSRepresentableObject;
import org.iupac.fairspec.spec.IFSSpecData;
import org.iupac.fairspec.spec.IFSSpecDataFindingAid;
import org.iupac.fairspec.spec.IFSStructureSpec;
import org.iupac.fairspec.spec.IFSStructureSpecCollection;
import org.iupac.fairspec.util.IFSDefaultJSONSerializer;
import org.iupac.fairspec.util.IFSDefaultStructurePropertyManager;

import com.integratedgraphics.ifs.api.IFSVendorPluginI;
import com.integratedgraphics.ifs.util.PubInfoExtractor;
import com.integratedgraphics.ifs.util.Util;

import javajs.util.JSJSONParser;
import javajs.util.Lst;
import javajs.util.PT;

/**
 * Copyright 2021 Integrated Graphics and Robert M. Hanson
 * 
 * A class to handle the extraction of objects from a "raw" dataset following
 * the sequence:
 * 
 * initialize(ifsExtractScriptFile)`
 * 
 * setLocalSourceDir(sourceDir)
 * 
 * setCachePattern(pattern)
 * 
 * setRezipCachePattern(pattern)
 * 
 * extractObjects(targetDir);
 * 
 * see ExtractorTest and IFSConst for these values
 * 
 * @author hansonr
 *
 */
public class Extractor implements IFSExtractorI {

	private class CacheRepresentation extends IFSRepresentation {

		public CacheRepresentation(IFSReference ifsReference, Object o, long len, String type, String subtype) {
			super(ifsReference, o, len, type, subtype);
		}

	}


	static {
		IFSVendorPluginI.init();
	}
	private static final String version = "0.0.1-alpha_2021_06_21";

	private static final String codeSource = "https://github.com/BobHanson/IUPAC-FAIRSpec/blob/main/src/main/java/com/integratedgraphics/ifs/Extractor.java";

	/**
	 * patterns to ignore completely.
	 */
	private final static Pattern junkPattern = Pattern.compile(IFSConst.junkFilePattern);

	public final static int EXTRACT_MODE_CHECK_ONLY = 1;
	public final static int EXTRACT_MODE_CREATE_CACHE = 2;
	public final static int EXTRACT_MODE_REPACKAGE_ZIP = 4;

	protected static final int LOG_IGNORED = 1;
	protected static final int LOG_OUTPUT = 2;


	protected static Pattern objectDefPattern = Pattern.compile("\\{([^:]+)::([^}]+)\\}");
	protected static Pattern pStarDotStar;

	protected static boolean debugging = false;
	protected static boolean readOnly = false;

	/**
	 * set true to only create finding aides, not extract file data
	 */
	protected static boolean createFindingAidsOnly = false;
	
	protected static String logfile;

	/**
	 * the finding aid - only one per instance
	 */
	protected IFSSpecDataFindingAid findingAid;

	/**
	 * the IFS-extract.json script
	 */
	private String extractScript;

	/**
	 * derived from the extraction variable {isfid} in IFS-extract.json, and put in
	 * the id field of the FindingAid
	 */
	private String findingAidId;

	/**
	 * IFS-extract.json {license}
	 */
	private String license;

	/**
	 * from IFS-extract.json {license}
	 */
	private String dataLicenseURI;

	/**
	 * from IFS-extract.json {license}
	 */
	private String dataLicenseName;

	
	/**
	 * form IFS-extract.json {puburi} IFS.findingaid.source.publication.uri
	 */
	private String puburi;

	/**
	 * map of information created form parsing the crossref uri+xml metadata
	 */
	private Map<String, Object> pubCrossrefInfo;
	
	/**
	 * extract version from IFS-extract.json
	 */
	protected String extractVersion;
	
	/**
	 * objects found in IFS-extract.json
	 */
	protected List<ObjectParser> objectParsers;

	/**
	 * Saving the zip contents from the ZIP file referred to by an IFS-extract {object} value.
	 * 
	 */
	protected static Map<String, Map<String, ZipEntry>> IFSZipContents = new LinkedHashMap<>();

	/**
	 * an optional local source directory to use instead of the one indicated in IFS-extract.json
	 */
	protected String sourceDir;

	/**
	 * a required target directory 
	 */
	protected File targetDir;

	/**
	 * files matched will be cached in the target directory
	 */
	protected Pattern cachePattern;

	/**
	 * vendors have supplied cacheRegex patterns
	 */
	private boolean cachePatternHasVendors;

	/**
	 * the path of the resource directory, for example 2228432 for Figshare
	 * resources
	 */
	protected String rootPath;

	/**
	 * working local name, without the rootPath, as found in _IFS_manifest.json
	 */
	private String localName;


	/**
	 * rezip data saved as an ISFRepresentation (for no particularly good reason)
	 */
	private IFSRepresentation currentRezipRepresentation;

	/**
	 * path to this resource in the original zip file
	 */
	private String currentRezipPath;

	/**
	 * vendor association with this rezipping
	 */
	private IFSVendorPluginI currentRezipVendor;

	/**
	 * last path to this rezip top-level resource
	 */
	private String lastRezipPath;


	/**
	 * the number of bytes extracted
	 */
	protected long extractedByteCount;

	/**
	 * the number of IFSObjects created
	 */
	private int ifsObjectCount;

	
	/**
	 * cache of top-level resources to be rezipped 
	 */
	protected List<IFSRepresentation> rezipCache;
	
	/**
	 * list of files extracted
	 */
	protected List<String> lstManifest = new ArrayList<>();

	/**
	 * list of files ignored -- probably MACOSX trash or Google desktop.ini trash
	 */
	protected List<String> lstIgnored = new ArrayList<>();

	/**
	 * working map from manifest names to structure or data object
	 */
	private Map<String, IFSRepresentableObject<?>> htManifestNameToObject = new LinkedHashMap<>();
		
	/**
	 * working memory cache of representations
	 */
	protected Map<String, IFSRepresentation> cache;

	/**
	 * a list of properties that vendors have indicated need addition, keyed by the
	 * zip path for the resource
	 */
	private List<Object[]> propertyList;
	
	/**
	 * the URL to the original source of this data, as indicated in IFS-extract.json
	 * as
	 */
	private String dataSource;


	/**
	 * bitset of activeVendors that are set for rezipping -- probably 1 
	 */
	private BitSet bsRezipVendors = new BitSet();
	
	/**
	 * bitset of activeVendors that are set for property parsing  
	 */
	private BitSet bsPropertyVendors = new BitSet();

	/**
	 * files matched will be cached as zip files
	 */
	protected Pattern rezipCachePattern;
	
	/**
	 * number of files manifested
	 */
	protected int manifestCount;
	
	/**
	 * number of files ignored
	 */
	protected int ignoredCount;
	
	/**
	 * number of bytes ignored
	 */
	protected long ignoredByteCount;

	/**
	 * the structure property manager for this extractor
	 * 
	 */
	private IFSPropertyManagerI structurePropertyManager;

	/**
	 * produce no output other than a log file
	 */
	private boolean noOutput;
	

	
	public Extractor() {
		clearZipCache();
		getStructurePropertyManager();
		noOutput = (createFindingAidsOnly || readOnly);
	}

	@Override
	public final boolean extractAndCreateFindingAid(File ifsExtractScriptFile, String localSourceDir, File targetDir, String findingAidFileName) throws IOException, IFSException {
		
		// first create objects, a List<String>
		
		getObjectParsersForFile(ifsExtractScriptFile);
		
		if (pubCrossrefInfo == null) {
			log("!! Finding aid does not contain PubInfo! No internet? cannot continue");
			return false;
		}
				
		setLocalSourceDir(localSourceDir);
		// options here to set cache and rezip options -- debugging only!
		setCachePattern(null);
		setRezipCachePattern(null, null);

		// now actually do the extraction.
		
		extractObjects(targetDir);

		String path = targetDir + "/" + findingAidFileName;
		String s = new IFSDefaultJSONSerializer().serialize(findingAid);
		writeBytesToFile(s.getBytes(), new File(path));
		return true;
	}

	/**
	 * Get the IFSFindingAid associated with this Extractor instance.
	 */
	@Override
	public IFSFindingAid getFindingAid() {
		return findingAid;
	}

	@Override
	public void setLocalSourceDir(String sourceDir) {
		this.sourceDir = sourceDir;
	}

	
	/////////  Vendor-related methods /////////
	
	/**
	 * Cache the property change created by an IFSVendorPluginI class. This method
	 * is callback from IFSVendorPluginI classes only.
	 */
	@Override
	public void addProperty(String key, Object val) {
		propertyList.add(new Object[] { localName, key, val });
	}

	/**
	 * Set the regex string assembing all vendor requests. Each vendor's pattern will
	 * be surrounded by (?<param0> ... ), (?<param1> ... ), etc. Here we wrap
	 * them all with (?<param>....), then add on our non-vender checks, and finally wrap all this using (?<type>...).
	 */
	@Override
	public void setCachePattern(String sp) {
		if (sp == null)
			sp = IFSConst.defaultCachePattern + "|" + structurePropertyManager.getParamRegex();
		String s = "";
		for (int i = 0; i < IFSVendorPluginI.activeVendors.size(); i++) {
			String cp = IFSVendorPluginI.activeVendors.get(i).vcache;
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
		cachePattern = Pattern.compile("(?<type>" + s + ")");
		cache = new HashMap<String, IFSRepresentation>();
	}

	/**
	 * The regex pattern uses param0, param1, etc., to indicated parameters for
	 * different vendors. This method looks through the activeVendor list to retieve
	 * the match, avoiding throwing any regex exceptions due to missing group names.
	 * 
	 * (Couldn't Java have supplied a check method for group names???)
	 * 
	 * @param m
	 * @return
	 */
	private IFSPropertyManagerI getPropertyManager(Matcher m) {
		if (m.group("struc") != null)
			return structurePropertyManager;		
		for (int i = bsPropertyVendors.nextSetBit(0); i >= 0; i = bsPropertyVendors.nextSetBit(i + 1)) {
			String ret = m.group("param" + i);
			if (ret != null && ret.length() > 0) {
				return IFSVendorPluginI.activeVendors.get(i).vendor;
			}
		}
		return null;
	}

	/**
	 * Set the file match zip cache pattern.
	 * 
	 * @param procs
	 * @param toExclude
	 */
	@Override
	public void setRezipCachePattern(String procs, String toExclude) {
		String s = "";

		for (int i = 0; i < IFSVendorPluginI.activeVendors.size(); i++) {
			String cp = IFSVendorPluginI.activeVendors.get(i).vrezip;
			if (cp != null) {
				bsRezipVendors.set(i);
				s = s + "|" + cp;
			}
		}
		s += (procs == null ? "" : "|" + procs);
		if (s.length() > 0)
			s = s.substring(1);
		rezipCachePattern = Pattern.compile(s);
		// rezipCacheExcludePattern = (toExclude == null ? null :
		// Pattern.compile(toExclude));
		rezipCache = new ArrayList<>();
	}

	///////// PHASE 1: Reading the IFS-extract.json file ////////

	/**
	 * get a new structure property manager to handle processing of MOL, SDF, and
	 * CDX files, primarily. Can be overridden.
	 * 
	 * @return
	 */
	protected IFSPropertyManagerI getStructurePropertyManager() {
		return (structurePropertyManager == null
				? (structurePropertyManager = new IFSDefaultStructurePropertyManager(this))
				: structurePropertyManager);
	}

	/**
	 * Get all {object} data from IFS-extract.json.
	 * 
	 * @param ifsExtractScript
	 * @return list of {objects}
	 * @throws IOException
	 * @throws IFSException 
	 */
	@Override
	public List<ObjectParser> getObjectParsersForFile(File ifsExtractScript) throws IOException, IFSException {
		log("! Extracting " + ifsExtractScript.getAbsolutePath());
		return getObjectsForStream(ifsExtractScript.toURI().toURL().openStream());
	}

	/**
	 * Get all {object} data from IFS-extract.json.
	 * 
	 * @param ifsExtractScript
	 * @return list of {objects}
	 * @throws IOException
	 * @throws IFSException 
	 */
	public List<ObjectParser> getObjectsForStream(InputStream is) throws IOException, IFSException {
		extractScript = new String(Util.getLimitedStreamBytes(is, -1, null, true, true));
		objectParsers = parseScript(extractScript);
		return objectParsers;
	}

	/**
	 * Parse the script form an IFS-extract.js JSON file starting with the creation
	 * of a Map by JSJSONParser.
	 * 
	 * @param script
	 * @return parsed list of objects from an IFS-extract.js JSON
	 * @throws IOException
	 * @throws IFSException 
	 */
	@SuppressWarnings("unchecked")
	protected List<ObjectParser> parseScript(String script) throws IOException, IFSException {
		Map<String, Object> jsonMap = (Map<String, Object>) new JSJSONParser().parse(script, false);
		if (debugging)
			log(jsonMap.toString());
		extractVersion = (String) jsonMap.get("IFS-extract-version");
		log(extractVersion);
		List<ObjectParser> objectParsers = getObjects((List<Map<String, Object>>) jsonMap.get("keys"));
		log(objectParsers.size() + " digital objects found");
		return objectParsers;
	}

	/**
	 * Make all variable substitutions in IFS-extract.js.
	 * 
	 * @return list of ObjectParsers that have successfully parsed the {object} lines of the file
	 * @throws IFSException 
	 */
	protected List<ObjectParser> getObjects(List<Map<String, Object>> pathway) throws IFSException {

		// input:

		// {"IFS-extract-version":"0.1.0-alpha","pathway":[
		// {"hash":"0c00571"},
		// {"pubid":"acs.orglett.{hash}"},
		// {"src":"IFS.findingaid.source.publication.uri::https://doi.org/10.1021/{pubid}"},
		// {"data":"{IFS.findingaid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/{pubid}/suppl_file/ol{hash}_si_002.zip"},
		//
		// {"path":"{data}|FID for
		// Publication/{id=IFS.property.struc.compound.id::*}.zip|{id}"},
		// {"objects":"{path}/{IFS.representation.struc.mol.2d::{id}.mol}"},
		// {"objects":"{path}/{IFS.representation.spec.nmr.vendor.dataset::{IFS.property.spec.nmr.expt.id::*}-NMR.zip}"},
		// {"objects":"{path}/HRMS.zip|{IFS.representation.spec.hrms.pdf::**/*.pdf}"},
		// ]}

		// output:

		// [
		// "{IFS.findingaid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/acs.orglett.0c00571/suppl_file/ol0c00571_si_002.zip}|FID
		// for
		// Publication/{id=IFS.property.struc.compound.id::*}.zip|{id}/{IFS.representation.struc.mol.2d::{id}.mol}"
		// "{IFS.findingaid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/acs.orglett.0c00571/suppl_file/ol0c00571_si_002.zip}|FID
		// for
		// Publication/{id=IFS.property.struc.compound.id::*}.zip|{id}/{IFS.representation.spec.nmr.vendor.dataset::{IFS.property.spec.nmr.expt.id::*}-NMR.zip}"
		// "{IFS.findingaid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/acs.orglett.0c00571/suppl_file/ol0c00571_si_002.zip}|FID
		// for
		// Publication/{id=IFS.property.struc.compound.id::*}.zip|{id}/HRMS.zip|{IFS.representation.spec.hrms.pdf::**/*.pdf}"
		// ]

		Lst<String> keys = new Lst<>();
		Lst<String> values = new Lst<>();
		List<ObjectParser> parsers = new ArrayList<>();
		for (int i = 0; i < pathway.size(); i++) {
			Map<String, Object> def = pathway.get(i);
			for (Entry<String, Object> e : def.entrySet()) {
				String key = e.getKey();
				String val = (String) e.getValue();
				if (val.indexOf("{") >= 0) {
					String s = PT.replaceStrings(val, keys, values);
					if (!s.equals(val)) {
						if (debugging)
							log(val + "\n" + s + "\n");
						e.setValue(s);
					}
					val = s;
				}
				switch (key) {
				case "puburi":
					try {
						puburi = getIFSExtractValue(val, "IFS.findingaid.source.publication.uri", null);
						if (puburi != null) {
							pubCrossrefInfo = PubInfoExtractor.getPubInfo(puburi);
							if (pubCrossrefInfo != null) {
								log("! crossref url " + pubCrossrefInfo.get("crossrefUrl"));
								log("! crossref metadata: \n" + pubCrossrefInfo.get("metadata"));
							}
						}
					} catch (IFSException | IOException e1) {
						System.out.println("Could not access " + PubInfoExtractor.getCrossrefUrl(puburi));
						e1.printStackTrace();
					}
					log(puburi);
					continue;
				case "license":
					license = val;
					continue;
				case "objects":
					parsers.add(newObjectParser(val));
					continue;
				case "ifsid":
					findingAidId = val;
					break;
				}
				keys.addLast("{" + key + "}");
				values.addLast(val);
			}
		}
		return parsers;
	}

	

	///////// PHASE 2: Parsing the ZIP file and extracting objects from it ////////

	/**
	 * Find and extract all objects of interest from a ZIP file.
	 * 
	 */
	@Override
	public IFSSpecDataFindingAid extractObjects(File targetDir) throws IFSException, IOException {
		if (findingAid != null)
			throw new IFSException("Only one extraction per instance of Extractor is allowed (for now).");
		if (targetDir == null)
			throw new IFSException("The target directory may not be null.");
		if (cache == null)
			setCachePattern(null);
		if (rezipCache == null)
			setRezipCachePattern(null, null);
		this.targetDir = targetDir;
		targetDir.mkdir();

		// "{IFS.findingaid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/acs.orglett.0c00571/suppl_file/ol0c00571_si_002.zip}|FID
		// for
		// Publication/{id=IFS.property.struc.compound.id::*}.zip|{id}/{IFS.representation.struc.mol.2d::{id}.mol}"
		// "{IFS.findingaid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/acs.orglett.0c00571/suppl_file/ol0c00571_si_002.zip}|FID
		// for
		// Publication/{id=IFS.property.struc.compound.id::*}.zip|{id}/{IFS.representation.struc.mol.2d::{id}.mol}"

// [parse first node]

		// {IFS.findingaid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/acs.orglett.0c00571/suppl_file/ol0c00571_si_002.zip}

// [start a new finding aid and download this resource]

		// |

// [get file list]

		// (\Q^FID for
		// Publication/\E)(\Q{id=IFS.property.struc.compound.id::*}\E)(\Q.zip\E)

// [pass to StructureIterator]		
// [find matches and add structures to finding aid structure collection]

		// |

// [get file list]
		// [for each structure...]

		// \Q{id}/\E(\Q{IFS.representation.struc.mol.2d::{id}.mol}/E)"

		// add this representation to this structure

		// "{IFS.findingaid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/acs.orglett.0c00571/suppl_file/ol0c00571_si_002.zip}|FID
		// for
		// Publication/{id=IFS.property.struc.compound.id::*}.zip|{id}/{IFS.representation.spec.nmr.vendor.dataset::{IFS.property.spec.nmr.expt.id::*}-NMR.zip}"

		// [parse first node]

		// {IFS.findingaid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/acs.orglett.0c00571/suppl_file/ol0c00571_si_002.zip}

		// [start a new finding aid and download this resource]

		// |

		// [get file list]

		// FID for Publication/{id=IFS.property.struc.compound.id::*}.zip

		// [pass to StructureIterator]
		// [find matches and add structures to finding aid structure collection if
		// necessary

		// |

		// [get file list]

		// {id}/{IFS.representation.spec.nmr.vendor.dataset::{IFS.property.spec.nmr.expt.id::*}-NMR.zip}

		// [pass to SpecDataIterator]
		// [find matches and add NMR spec data to finding aid spec data collection; also
		// add struc+spec to finding aid StrucSpecCollection]

		// bruker directories identified with / just before vendor dataset closing }
		// standard ^....$
		// * at end just removes $
		// *. becomes [^.]
		// . becomes [.]
		// **/*.pdf becomes (?:[^/]+/)*).+\Q.pdf\E

//		String s = "test/ok/here/1c.pdf";    // test/**/*.pdf
//		Pattern p = Pattern.compile("^\\Qtest\\E/(?:[^/]+/)*(.+\\Q.pdf\\E)$");
//		Matcher m = p.matcher(s);
//		log(m.find() ? m.groupCount() + " " + m.group(0) + " -- " + m.group(1) : "");

		log("=====");

		if (sourceDir != null)
			log("extractObjects from " + sourceDir);
		log("extractObjects to " + targetDir.getAbsolutePath());

		String lastRootPath = null;
		String lastURL = null;
		findingAid = null;

		// Note that some files have multiple objects.
		// These may come from multiple sources, or they may be from the same source.
		propertyList = new ArrayList<>();

		if (license != null) {
			dataLicenseURI = getIFSExtractValue(license, IFSConst.IFS_FINDINGAID_DATA_LICENSE_URI, null);
			dataLicenseName = getIFSExtractValue(license, IFSConst.IFS_FINDINGAID_DATA_LICENSE_NAME, null);
			log("! IFS.findingaid.data.license: " + dataLicenseName + " " + dataLicenseURI);
		}
		for (int i = 0; i < objectParsers.size(); i++) {

			ObjectParser parser = objectParsers.get(i);
			dataSource = parser.dataSource;
			lastURL = setFindingAidAndGlobals(parser.sObj, dataSource, lastURL);
			// localize the URL if we are using a local copy of a remote resource.

			String sURL = localizeURL(dataSource);
			if (debugging)
				log("opening " + sURL);
			lastRootPath = initializeCollection(sURL, lastRootPath);

			// At this point we now have all spectra ready to be associated with structures.

			// 2.1
			log("! PHASE 2.1 \n" + sURL + "\n" + parser.sData);
			boolean haveData = parseZipFileNamesForObjects(sURL, parser);
			// 2.2
			log("! PHASE 2.2 rezip haveData=" + haveData);
			if (haveData)
				rezipFilesAndExtractProperties(sURL);

			log("found " + ifsObjectCount + " IFS objects");

		}

		// update object vendor properties
		
		updateObjectProperties();

		// update object type and len records
		
		updateObjectLengthAndType();

		removeDuplicateSpecData();
		removeUnmanifestedRepresentations();
		
		saveCollectionManifests(false);
		
		findingAid.finalizeExtraction();
		return findingAid;
	}

	private void removeDuplicateSpecData() {
		BitSet bs = new BitSet();
		IFSStructureSpecCollection ssc = findingAid.getStructureSpecCollection();
		boolean isFound = false;
		int n = 0;
		for (IFSStructureDataAssociation assoc : ssc) {
			IFSDataObjectCollection<IFSDataObject<?>> c = assoc.getDataObjectCollection();
			List<IFSSpecData> found = new ArrayList<>();
			for (IFSDataObject<?>spec : c) {
				if (bs.get(spec.getIndex())) {
					found.add((IFSSpecData) spec);
					log("! removing duplicate spec reference " + spec.getName() + " for " + assoc.getFirstStructure().getName());
					isFound = true;
				} else {
					bs.set(spec.getIndex());
				}
			}
			n += found.size();
			if (found.size() > 0) {
				c.removeAll(found);
			}
		}
		if (isFound) {
			n += findingAid.cleanStructureSpecs();
		}
		if (n > 0)
			log ("! " + n + " objects removed");
	}

	/**
	 * The issue here is that sometimes we have to identify directories that are not
	 * going to be zipped up in the end, because they do not match the rezip
	 * trigger.
	 */
	private void removeUnmanifestedRepresentations() {
		boolean isRemoved = false;
		for (IFSSpecData spec : findingAid.getSpecDataCollection()) {
			List<IFSRepresentation> lstRepRemoved = new ArrayList<>();
			for (IFSRepresentation rep : spec) {
				if (rep.getLength() == 0) {
					lstRepRemoved.add(rep);
					log("! removing 0-length representation " + rep);
				}
			}
			spec.removeAll(lstRepRemoved);
			if (spec.size() == 0) {
				isRemoved = true;
				log("! preliminary spec data " + spec + " removed - no representations");
			}
		}
		if (isRemoved) {
			int n = findingAid.cleanStructureSpecs();
			if (n > 0)
				log ("! " + n + " objects removed");
		}
	}

	/**
	 * Set up the global findingAid and give it a starting license property.
	 * 
	 * @param sAid
	 * @param unlocalizedURL
	 * @param lastURL
	 * @return
	 * @throws IFSException
	 */
	private String setFindingAidAndGlobals(String sAid, String unlocalizedURL, String lastURL) throws IFSException {
		if (findingAid == null) {
			findingAid = new IFSSpecDataFindingAid(findingAidId, unlocalizedURL);
			if (dataLicenseURI != null) {
				findingAid.setPropertyValue(IFSConst.IFS_FINDINGAID_DATA_LICENSE_URI, dataLicenseURI);
			}
			if (dataLicenseName != null) {
				findingAid.setPropertyValue(IFSConst.IFS_FINDINGAID_DATA_LICENSE_NAME, dataLicenseName);
			}
			
			findingAid.setPubInfo(pubCrossrefInfo);

		} else if (!unlocalizedURL.equals(lastURL)) {
			findingAid.addUrl(unlocalizedURL);
			lastURL = unlocalizedURL;
		}
		return lastURL;
	}

	/**
	 * Initialize the paths. 
	 * 
	 * @param sURL
	 * @param lastRootPath and manifest files
	 * @return
	 * @throws IOException
	 */
	private String initializeCollection(String sURL, String lastRootPath) throws IOException {

		String zipPath = sURL.substring(sURL.lastIndexOf(":") + 1);
		String rootPath = new File(zipPath).getName();

		// remove ".zip" if present in the overall name

		if (rootPath.endsWith(".zip"))
			rootPath = rootPath.substring(0, rootPath.indexOf(".zip"));

		new File(targetDir + "/" + rootPath).mkdir();

		if (lastRootPath != null && !rootPath.equals(lastRootPath)) {
			// close last collection logs
			saveCollectionManifests(false);
		}
		if (!rootPath.equals(lastRootPath)) {
			// open a new log
			this.rootPath = lastRootPath = rootPath;
			saveCollectionManifests(true);
		}

		return lastRootPath;
	}

	/**
	 * Parse the zip file using an object parser. 
	 * 
	 * @param zipPath
	 * @param parser
	 * @return true if have spectra objects
	 * @throws IOException
	 * @throws IFSException
	 */
	private boolean parseZipFileNamesForObjects(String zipPath, ObjectParser parser) throws IOException, IFSException {
		boolean haveData = false;
		
		// first build the file list
		Map<String, ZipEntry> zipFiles = IFSZipContents.get(zipPath);
		if (zipFiles == null) {
			// Scan URL zip stream for files.
			log("! retrieving " + zipPath);
			URL url = new URL(zipPath);// getURLWithCachedBytes(zipPath); // BH carry over bytes if we have them for JS
			zipFiles = readZipContentsIteratively(url.openStream(), new LinkedHashMap<String, ZipEntry>(), "", false);
			IFSZipContents.put(zipPath, zipFiles);
		}
		// next, we process those names
		
		for (String zipName : zipFiles.keySet()) {
			IFSObject<?> obj = addIFSObjectsForName(parser, zipName);
			if (obj != null) {
				System.out.println(zipName);
				ifsObjectCount++;
				if (obj instanceof IFSDataObject || obj instanceof IFSStructureSpec)
					haveData = true;
			}
		}
		return haveData;
	}

	/**
	 * Get a new ObjectParser for this data. Note that this method may be overridden if desired.
	 * 
	 * @param sData
	 * @return
	 * @throws IFSException
	 */
	protected ObjectParser newObjectParser(String sObj) throws IFSException {
		return new ObjectParser(sObj);
	}

	protected Map<String, ZipEntry> readZipContentsIteratively(InputStream is, Map<String, ZipEntry> fileNames,
			String baseName, boolean doRezip) throws IOException {
		if (debugging && baseName.length() > 0)
			log("! opening " + baseName);
		boolean isTopLevel = (baseName.length() == 0);
		ZipInputStream zis = new ZipInputStream(is);
		ZipEntry zipEntry = null;
		ZipEntry rezipEntry = null;
		int n = 0;
		int phase = (doRezip ? 2 : 1);
		while ((zipEntry = (rezipEntry == null ? zis.getNextEntry() : rezipEntry)) != null) {
			n++;
			rezipEntry = null;
			String zipName = baseName + zipEntry.getName();
			if (zipEntry.isDirectory()) {
				log("Phase " + phase + " checking zip directory: " + n + " " + zipName);
			} else if (!zipEntry.isDirectory() && zipEntry.getSize() == 0) {

				continue;
			}

			if (junkPattern.matcher(zipName).find()) {
				// Test 9: acs.orglett.0c01153/22284726,22284729 MACOSX,
				// acs.joc.0c00770/22567817
				addFileToFileLists(zipName, LOG_IGNORED, zipEntry.getSize());
				continue;
			}
			if (debugging)
				log("reading zip entry: " + n + " " + zipName);

			if (fileNames != null) {
				fileNames.put(zipName, zipEntry); // Java has no use for the ZipEntry, but JavaScript can read it.
			}
			if (zipName.endsWith(".zip")) {
				readZipContentsIteratively(zis, fileNames, zipName + "|", doRezip);
			} else if (doRezip) {
				rezipEntry = processRezipEntry(baseName, zipName, zis, zipEntry);
			} else {
				processZipEntry(zipName, zis, zipEntry);
			}
		}
		if (isTopLevel)
			zis.close();
		return fileNames;
	}

	/**
	 * An important feature of Extractor is that it will repackage zip files,
	 * removing resources that are totally unnecessary and extracting properties
	 * using IFSVendorPluginI services.
	 * 
	 * @param sURL
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	private void rezipFilesAndExtractProperties(String sURL) throws MalformedURLException, IOException {
		if (rezipCache != null && rezipCache.size() > 0) {
			lastRezipPath = null;
			getNextRezipName();
			readZipContentsIteratively(new URL(sURL).openStream(), null, "", true);
		}
	}

	/**
	 * Process the properties in propertyList after the IFSObject objects have een
	 * created for all resources.
	 */
	private void updateObjectProperties() {
		for (Object[] a : propertyList) {
			String localName = (String) a[0];
			String param = (String) a[1];
			Object value = a[2];
			IFSRepresentableObject<?> spec = htManifestNameToObject.get(localName);
			if (spec == null) {
				log("!! manifest not found for " + localName);
			} else if (param.indexOf(".representation.") >= 0) {
				String zipName = value.toString();
				localName = getLocalName(zipName);
				linkManifestNameToObject(localName, spec, param);
				IFSRepresentation rep = spec.getRepresentation(zipName, localName, true, param, null);
				rep.setSubtype(param);
			} else {
				spec.setPropertyValue(param, value);
			}
		}
		propertyList.clear();
	}

	/**
	 * Set the type and len fields for structure and spec data
	 */
	private void updateObjectLengthAndType() {

		for (Entry<String, IFSRepresentation> e : cache.entrySet()) {
			String localName = e.getKey();
			IFSRepresentableObject<?> obj = htManifestNameToObject.get(localName);
//			System.out.println(localName);
			if (obj == null) {
				log("!! manifest not found for " + localName);
			} else {
				IFSRepresentation r = e.getValue();
				String zipName = r.getRef().getOrigin().toString();
				String type = r.getType();
				String subtype = r.getSubtype();				
				IFSRepresentation r1 = obj.getRepresentation(zipName, localName, true, r.getType(), null);
//				if (type != null && !type.equals(r1.getType())
//						|| subtype != null && !subtype.equals(r1.getSubtype()))
//						System.out.println("changed " + type + " " + subtype);
				if (type != null)
					r1.setType(type);
				if (subtype != null)
					r1.setSubtype(subtype);
				r1.setLength(r.getLength());
			}

		}
	}
	/**
	 * Write the _IFS_manifest.json, _IFS_ignored.json and _IFS_extract.json files.
	 * 
	 * 
	 * @param isOpen if true, starting -- just clear the lists; if false, write the
	 *               files
	 * @throws IOException
	 */
	protected void saveCollectionManifests(boolean isOpen) throws IOException {
		if (!isOpen && !createFindingAidsOnly && !readOnly) {
			writeBytesToFile(extractScript.getBytes(), getFileTarget("_IFS_extract.json"));
			outputListJSON(lstManifest, getFileTarget("_IFS_manifest.json"), "manifest");
			outputListJSON(lstIgnored, getFileTarget("_IFS_ignored.json"), "ignored");
		}
		lstManifest.clear();
		lstIgnored.clear();
	}

	/**
	 * Output standardized JSON to the _IFS_* files.
	 * 
	 * @param lst
	 * @param fileTarget
	 * @param type
	 * @throws IOException
	 */
	@SuppressWarnings("deprecation")
	protected void outputListJSON(List<String> lst, File fileTarget, String type) throws IOException {
		log("! saved " + fileTarget + " (" + lst.size() + " items)");
		// Date d = new Date();
		// all of a sudden, on 2021.06.13 at 1 PM
		// file:/C:/Program%20Files/Java/jdk1.8.0_251/jre/lib/sunrsasign.jar cannot be
		// found when
		// converting d.toString() due to a check in Date.toString for daylight savings
		// time!

		StringBuffer sb = new StringBuffer();
		sb.append("{" + "\"IFS.fairspec.version\":\"" + IFSConst.IFS_FAIRSpec_version + "\",\n");
		if (dataLicenseURI != null) {
			sb.append("\"IFS.fairspec.data.license.uri\":\"" + dataLicenseURI + "\",\n");
		}
		if (dataLicenseName != null) {
			sb.append("\"IFS.fairspec.data.license.name\":\"" + dataLicenseName + "\",\n");
		}
		sb.append("\"IFS.extractor.version\":\"" + version + "\",\n")
				.append("\"IFS.extractor.code\":\"" + codeSource + "\",\n")
				.append("\"IFS.extractor.list.type\":\"" + type + "\",\n")
				.append("\"IFS.extractor.scirpt\":\"_IFS_extract.json\",\n")
				.append("\"IFS.extractor.source\":\"" + dataSource + "\",\n")
				.append("\"IFS.extractor.creation.date\":\"" + findingAid.getDate().toGMTString() + "\",\n")
				.append("\"IFS.extractor.count\":" + lst.size() + ",\n").append("\"IFS.extractor.list\":\n" + "[\n");
		String sep = "";
		if (type.equals("manifest")) {
			// list the zip files first
			for (String name : lst) {
				if (type.equals("manifest") && name.endsWith(".zip")) {
					sb.append((sep + "\"" + name + "\"\n"));
//					sb.append(sep).append(getManifestEntry(name));
					sep = ",";
				}
			}
			for (String name : lst) {
				if (type.equals("manifest") && !name.endsWith(".zip")) {
					sb.append((sep + "\"" + name + "\"\n"));
//					sb.append(sep).append(getManifestEntry(name));
					sep = ",";
				}
			}
		} else {
			for (String name : lst) {
				sb.append((sep + "\"" + name + "\"\n"));
				sep = ",";
			}
		}
		sb.append("]}\n");
		writeBytesToFile(sb.toString().getBytes(), fileTarget);
	}

	/**
	 * Use the regex ObjectParser to match a file name with a pattern defined in the
	 * IFS-extract.json description. This will result in the formation of one or
	 * more IFSObjects -- an IFSAanalysis, IFSStructureSpecCollection,
	 * IFSSpecDataObject, or IFSStructure, for instance. But that will probably
	 * change.
	 * 
	 * The parser specifically looks for Matcher groups, regex (?<xxxx>...), that
	 * have been created by the ObjectParser from an object line such as:
	 * 
	 * {IFS.representation.spec.nmr.vendor.dataset::{IFS.property.struc.compound.id::*-*}-{IFS.property.spec.nmr.expt.id::*}.jdf}
	 *
	 * 
	 * 
	 * @param parser
	 * @param zipName
	 * @return one of IFSStructureSpec, IFSSpecData, IFSStructure, in that order,
	 *         depending upon availability
	 * 
	 * @throws IFSException
	 */
	private IFSObject<?> addIFSObjectsForName(ObjectParser parser, String zipName) throws IFSException {
		Matcher m = parser.p.matcher(zipName);
		if (!m.find())
			return null;
		findingAid.beginAddObject(zipName);
		if (debugging)
			log("adding IFSObjects for " + zipName);

		// If an IFSSpecData object is added, then it will also be added to
		// htManifestNameToSpecData

		for (String key : parser.keys.keySet()) {
			String param = parser.keys.get(key);
			String value = m.group(key);
			final String localName = getLocalName(zipName);
			IFSRepresentableObject<?> obj = findingAid.addObject(rootPath, param, value, localName);
			linkManifestNameToObject(localName, obj, param);
			if (debugging)
				log("found " + param + " " + value);
			;
		}
		return findingAid.endAddObject();
	}

	/**
	 * 
	 * @param localName
	 * @param obj
	 * @param param
	 */
	private void linkManifestNameToObject(String localName, IFSRepresentableObject<?> obj, String param) {
		if (IFSConst.isRepresentation(param)) {
			htManifestNameToObject.put(localName, obj);
		}
	}

	/**
	 * Just a very simple logger. Messages that start with "!" are always logged;
	 * others are logged if debugging is set to true.
	 * 
	 * 
	 * @param msg
	 */
	protected static void log(String msg) {
		if (Util.logStream != null)
			try {
				Util.logStream.write((msg + "\n").getBytes());
			} catch (IOException e) {
			}
		if (msg.startsWith("!!"))
			System.err.println(msg);
		else if (debugging || msg.startsWith("!"))
			System.out.println(msg);
	}

	/**
	 * For testing (or for whatever reason zip files are local or should not use the
	 * given source paths), replace https://......./ with sourceDir/
	 * 
	 * @param sUrl
	 * @return localized URL
	 */
	protected String localizeURL(String sUrl) {
		if (sourceDir != null) {
			int pt = sUrl.lastIndexOf("/");
			sUrl = sourceDir + sUrl.substring(pt);
			if (!sUrl.endsWith(".zip"))
				sUrl += ".zip";
		}
		return sUrl;
	}

	/**
	 * Process a {key::value} set. 
	 * 
	 * @param sObj
	 * @param key
	 * @param pt
	 * @return the value for this key
	 * @throws IFSException
	 */
	protected static String getIFSExtractValue(String sObj, String key, int[] pt) throws IFSException {
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
					throw new IFSException("unopened '}' in " + sObj + " at char " + i);
				}
				q = i;
				break;
			}
		}
		if (nBrace > 0) {
			throw new IFSException("unclosed '{' in " + sObj + " at char " + q);
		}
		pt[0] = q;
		return sObj.substring(p, pt[0]++);
	}

	/**
	 * zip contents caching can save time in complex loading.
	 * 
	 */
	public static void clearZipCache() {
		IFSZipContents.clear();
	}


	private ZipEntry processRezipEntry(String baseName, String zipName, ZipInputStream zis, ZipEntry zipEntry) throws IOException {
		if (!zipName.equals(currentRezipPath)) {
			String localName = getLocalName(zipName);
			if (!zipEntry.isDirectory() && !lstIgnored.contains(zipName) && !lstManifest.contains(localName)) {
				addFileToFileLists(zipName, LOG_IGNORED, zipEntry.getSize());
				log("! ignoring " + rootPath + "|" + zipName);
			}
			return null;
		}
		return rezip(currentRezipVendor, zis, baseName, zipName, zipEntry);
	}

	/**
	 * When a ZipEntry is a directory and has been identified as a SpecData object,
	 * we need to catalog and rezip that file.
	 * 
	 * Create a new zip file that reconfigures the file directory to contain what we
	 * want it to.
	 * 
	 * Note that the rezipping process takes two passes, because the first pass has most
	 * likely already passed by one or more files associated with this rezipping
	 * project.
	 * 
	 * 
	 * @param zis
	 * @param zipDirName
	 * @param entry
	 * @return next (unassociated) zip entry
	 * @throws IOException
	 */
	protected ZipEntry rezip(IFSVendorPluginI vendor, ZipInputStream zis, String baseName, String zipDirName, ZipEntry entry)
			throws IOException {
		String dirName = entry.getName();
//		if (r == null) {			
//			// not a SpecData object. (Maybe a MOL file.)
//			while ((entry = zis.getNextEntry()) != null) {
//				if (!entry.getName().startsWith(dirName))
//					break;
//			}
//			return entry;
//		}
		log("! rezipping " + zipDirName + " for " + entry + " " + new File(entry.getName()).getName());
		File outFile = getFileTarget(zipDirName + ".zip");
		final String localName = getLocalName(zipDirName);
		OutputStream fos = (noOutput ? new ByteArrayOutputStream() : new FileOutputStream(outFile));
		ZipOutputStream zos = new ZipOutputStream(fos);
		String parent = new File(entry.getName()).getParent();
		int lenOffset = (parent == null ? 0 : parent.length() + 1);
		String newDir = vendor.getRezipPrefix(dirName.substring(lenOffset, dirName.length() - 1)); // trimming trailing '/'
		if (newDir == null) {
			newDir = "";
		} else {
			newDir = newDir + "/";
			lenOffset = dirName.length();
		}
		Matcher m = null;
		this.localName = localName;
		vendor.startRezip(this);
		long len = 0;
		while ((entry = zis.getNextEntry()) != null) {
			String entryName = entry.getName();
			if (junkPattern.matcher(entryName).find()) {
				// Test 9: acs.orglett.0c01153/22284726,22284729 MACOSX,
				// acs.joc.0c00770/22567817
				continue;
			}
			if (!entryName.startsWith(dirName))
				break;
			if (entry.isDirectory())
				continue;

			IFSPropertyManagerI mgr = null;
			// include in zip?
			boolean doInclude = (vendor == null || vendor.doRezipInclude(baseName, entryName));
			// cache this one? -- could be a different vendor -- JDX inside Bruker
			// directory, for example
			boolean doCache = (cachePattern != null && (m = cachePattern.matcher(entryName)).find()
					&& getParamName(m) != null && ((mgr = getPropertyManager(m)) == null || mgr.doExtract(entryName)));

			boolean doCheck = (doCache || mgr != null);

			len = entry.getSize();
			if (len != 0) {
				OutputStream os;
				if (doCheck) {
					os = new ByteArrayOutputStream();
				} else if (doInclude) {
					os = zos;
				} else {
					continue;
				}
				String outName = newDir + entryName.substring(lenOffset);
				if (doInclude)
					zos.putNextEntry(new ZipEntry(outName));
				Util.getLimitedStreamBytes(zis, len, os, false, false);
				if (doCheck) {
					byte[] bytes = ((ByteArrayOutputStream) os).toByteArray();
					if (doInclude)
						zos.write(bytes);
					this.localName = localName;
					if (mgr == null || mgr == vendor) {
						vendor.accept(null, outName, bytes);
					} else {
						mgr.accept(this, outName, bytes);
					}
				}
				if (doInclude)
					zos.closeEntry();
			}
		}
		vendor.endRezip();
		zos.close();
		fos.close();
		String dataType = vendor.getDatasetType(zipDirName);
		len = (noOutput ? ((ByteArrayOutputStream) fos).size() : outFile.length());
		IFSRepresentation r = findingAid.getSpecDataRepresentation(zipDirName);
		if (r == null)  {
			System.out.println("! r not found for " + zipDirName);
			// could be just structure at this point
		} else {
			r.setLength(len);
		}
		cacheFileRepresentation(localName, zipDirName, len, dataType, "application/zip");
		addFileToFileLists(localName, LOG_OUTPUT, len);
		getNextRezipName();
		return entry;
	}

	/**
	 * Add a record for _IFS_manifest.json or _IFS_ignored.json
	 * 
	 * @param fileName
	 * @param mode
	 * @param len
	 */
	private void addFileToFileLists(String fileName, int mode, long len) {
		switch (mode) {
		case LOG_IGNORED:
			ignoredByteCount += Math.max(len, 0);
			ignoredCount++;
			lstIgnored.add(fileName);
			break;
		case LOG_OUTPUT:
			extractedByteCount += len;
			manifestCount++;
			lstManifest.add(fileName);
			break;
		}
	}

	/**
	 * Pull the next rezip parent directory name off the stack, setting the currentRezipPath and currentRezipVendor fields.
	 * 
	 */
	protected void getNextRezipName() {
		if (rezipCache.size() == 0) {
			currentRezipPath = null;
			currentRezipRepresentation = null;
		} else {
			currentRezipPath = (String) (currentRezipRepresentation = rezipCache.remove(0)).getRef().getOrigin();
			currentRezipVendor = (IFSVendorPluginI) currentRezipRepresentation.getData();
		}
	}

	/**
	 * Check to see what should be done with a zip entry. We can extract it or
	 * ignore it; and we can check it to sees if it is the trigger for extracting a
	 * zip file in a second pass.
	 * 
	 * @param zipName
	 * @param zis
	 * @param zipEntry
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	protected void processZipEntry(String zipName, InputStream zis, ZipEntry zipEntry)
			throws FileNotFoundException, IOException {
		long len = zipEntry.getSize();
		Matcher m;

		// check for files that should be pulled out - these might be JDX files, for
		// example.
		// "param" appears if a vendor has flagged these files for parameter extraction.

		if (cachePattern != null && (m = cachePattern.matcher(zipName)).find()) {
			IFSPropertyManagerI v = getPropertyManager(m);
			boolean doExtract = (v == null || v.doExtract(zipName));
			boolean doCheck = (v != null);

			System.out.println("! caching " + zipName);
//			1. we don't have params 
//		      - generic file, just save it.  doExtract and not doCheck
//			2. we have params and there is extraction
//		      - save file and also check it for parameters  doExtract and doCheck
//			3. we have params but no extraction  !doCheck  and !doExtract
//		      - ignore completely

			File f = getFileTarget(zipName);
			OutputStream os = (!doCheck && !doExtract ? null
					: doCheck || noOutput ? new ByteArrayOutputStream() : new FileOutputStream(f));
			if (os != null)
				Util.getLimitedStreamBytes(zis, len, os, false, true);
			String localName = getLocalName(zipName);
			String refName = f.getAbsolutePath();
			if (doExtract) {
				byte[] bytes = (doCheck || noOutput ? ((ByteArrayOutputStream) os).toByteArray() : null);
				len = (doCheck ||  noOutput ? bytes.length : f.length());
				if (doCheck) {
					// set this.localName for parameters
					// preserve this.localName, as we might be in a rezip.
					// as, for example, a JDX file within a Bruker dataset
					writeBytesToFile(bytes, f);
					String oldLocal = this.localName;
					this.localName = localName;
					// indicating "this" here notifies the vendor plugin that
					// this is a one-shot file, not a collection.
					v.accept(this, refName, bytes);
					this.localName = oldLocal;
				}
				String type = (v == null ? null : v.getDatasetType(refName));
				String subtype = IFSSpecDataFindingAid.mediaTypeFromName(m.group("type"));
				cacheFileRepresentation(localName, zipName, len, type, subtype);
				addFileToFileLists(localName, LOG_OUTPUT, len);
			}
		}

		// here we look for the "trigger" file within a zip file that indicates that we
		// (may) have a certain vendor's files that need looking into. The case in point
		// is
		// finding a procs file within a Bruker data set. Or, in principle, an acqus
		// file and
		// just an FID but no pdata/ directory. But for now we want to see that
		// processed data.

		if (rezipCachePattern != null && (m = rezipCachePattern.matcher(zipName)).find()) {

			// e.g. exptno/./pdata/procs

			IFSVendorPluginI v = getVendorForRezip(m);
			zipName = m.group("path" + v.getIndex());
			if (zipName.equals(lastRezipPath)) {
				log("duplicate path " + zipName);
			} else {
				lastRezipPath = zipName;
				IFSRepresentation ref = new CacheRepresentation(new IFSReference(zipName, getLocalName(zipName), "./" + rootPath),
						v, len, null, "application/zip");
				rezipCache.add(ref);
				log("rezip pattern found " + zipName);
			}
		}

	}

	/**
	 * Find the matching pattern for rezipN where N is the vendor index in
	 * activeVendors. Presumably there will be only one vendor per match. (Two
	 * vendors will not be looking for MOL files, for example.)
	 * 
	 * @param m
	 * @return
	 */
	private IFSVendorPluginI getVendorForRezip(Matcher m) {
		for (int i = bsRezipVendors.nextSetBit(0); i >= 0; i = bsRezipVendors.nextSetBit(i + 1)) {
			String ret = m.group("rezip" + i);
			if (ret != null && ret.length() > 0) {
				return IFSVendorPluginI.activeVendors.get(i).vendor;
			}
		}
		return null;
	}

	/**
	 * Should be no throwing of Exceptions here -- we know if we have (?<param>...) groups.
	 * @param m
	 * @return
	 */
	private String getParamName(Matcher m) {
		try {
			if (cachePatternHasVendors)
				return m.group("param");
		} catch (Exception e) {
		}
		return null;
	}

	/**
	 * Cache file representation for this resource, associating it with a media type if we can. 
	 * 
	 * @param localName
	 * @param zipName
	 * @param len
	 * @param type 
	 * @param subtype a media type, typically
	 */
	private void cacheFileRepresentation(String localName, String zipName, long len, String type, String subtype) {
		if (subtype == null)
			subtype = IFSSpecDataFindingAid.mediaTypeFromName(localName);
		cache.put(localName,
				new CacheRepresentation(new IFSReference(zipName, localName, rootPath), null, len, type, subtype));
	}

	/**
	 * Get the OS file path for FileOutputStream.
	 * 
	 * @param fname
	 * @return
	 */
	protected File getFileTarget(String fname) {
		return new File(targetDir + "/" + rootPath + "/" + getLocalName(fname));
	}

	/**
	 * Clean up the zip name to remove '|', '/', ' ', and add ".zip" if there is a trailing '/' in the name.
	 * 
	 * @param fname
	 * @return
	 */
	protected static String getLocalName(String fname) {
		boolean isDir = fname.endsWith("/");
		return fname.replace('|', '_').replace('/', '_').replace(' ', '_') + (isDir ? ".zip" : "");
	}


	private void writeBytesToFile(byte[] bytes, File f) throws IOException {
		if (!noOutput)
			Util.writeBytesToFile(bytes, f);
	}

	/**
	 * A class for parsing the object string and using regex to match filenames.
	 * This static class may be overridden to give it different capabilities.
	 * 
	 * @author hansonr
	 *
	 */

	public static class ObjectParser {

		protected String sData;

		protected Pattern p;

		protected List<String> regexList;

		protected Map<String, String> keys;

		private String sObj;

		private String dataSource;

		/**
		 * @param sObj
		 * @throws IFSException
		 */
		public ObjectParser(String sObj) throws IFSException {
			this.sObj = sObj;
			int[] pt = new int[1];
			this.dataSource = getIFSExtractValue(sObj, IFSConst.IFS_FINDINGAID_SOURCE_DATA_URI, pt);
			this.sData = sObj.substring(pt[0] + 1); // skip first "|"
			init();
		}

		/**
		 * Prepare pattern and match.
		 * 
		 * @throws IFSException
		 * 
		 * 
		 */
		protected void init() throws IFSException {
			// Using Java and JavaScript named capture groups for in-line defining.
			//
			// {regex::[a-z]} is left unchanged and becomes \\E[a-z]\\Q
			//
			// **/ becomes \\E(?:[^/]+/)*)\\Q
			//
			// *-* becomes \\E([^-]+(?:-[^-]+)*)\\Q and matches a-b-c
			//
			// * becomes \\E.+\\Q
			//
			// {id=IFS.property.spec.nmr.expt.id::xxx} becomes \\E(?<id>\\Qxxx\\E)\\Q
			//
			// {IFS.property.spec.nmr.expt.id::xxx} becomes \\E(?<IFS0nmr0param0expt>\\Qxxx\\E)\\Q
			//
			// <id> becomes \\k<id>
			//
			// generally ... becomes ^\\Q...\\E$
			//
			// \\Q\\E in result is removed
			//
			// so:
			//
			// {IFS.property.spec.nmr.expt.id::*} becomes \\E(?<IFS0nmr0param0expt>.+)\\Q
			//
			// {IFS.representation.spec.nmr.vendor.dataset::{IFS.property.struc.compound.id::*-*}-{IFS.property.spec.nmr.expt.id::*}.jdf}
			//
			// becomes:
			//
			// ^(?<IFS0nmr0representation0vendor0dataset>(?<IFS0structure0param0compound0id>([^-](?:-[^-]+)*))\\Q-\\E(?<IFS0nmr0param0expt>.+)\\Q.jdf\\E)$
			//
			// {id=IFS.property.struc.compound.id::*}.zip|{IFS.representation.spec.nmr.vendor.dataset::{id}_{IFS.property.spec.nmr.expt.id::*}/}
			//
			// becomes:
			//
			// ^(?<id>*)\\Q.zip|\\E(?<IFS0nmr0representation0vendor0dataset>\\k<id>\\Q_\\E(<IFS0nmr0param0expt>*)\\Q/\\E)$

			// so....

			// {regex::[a-z]} is left unchanged and becomes \\E[a-z]\\Q

			String s = protectRegex(null);

			// **/ becomes \\E(?:[^/]+/)*\\Q

			s = PT.rep(s, "**/", "\\E(?:[^/]+/)\2\\Q");

			Matcher m;
			// *-* becomes \\E([^-]+(?:-[^-]+)*)\\Q and matches a-b-c
			if (s.indexOf("*") != s.lastIndexOf("*")) {
				if (pStarDotStar == null)
					pStarDotStar = Pattern.compile("\\*(.)\\*");
				while ((m = pStarDotStar.matcher(s)).find()) {
					String schar = m.group(1);
					s = PT.rep(s, "*" + schar + "*", "\\E([^" + schar + "]+(?:" + schar + "[^" + schar + "]+)\2)\\Q");
				}
			}
			// * becomes \\E.+\\Q

			s = PT.rep(s, "*", "\\E[^|/]+\\Q");

			// {id=IFS.property.spec.nmr.expt.id::xxx} becomes \\E(?<id>\\Qxxx\\E)\\Q
			// {IFS.property.spec.nmr.expt.id::xxx} becomes \\E(?<IFS0nmr0param0expt>\\Qxxx\\E)\\Q
			// <id> becomes \\k<id>

			s = compileIFSDefs(s, true, true);

			// restore '*'
			s = s.replace('\2', '*');

			// restore regex
			// wrap with quotes and constraints ^\\Q...\\E$

			s = "^\\Q" + protectRegex(s) + "\\E$";

			// \\Q\\E in result is removed

			s = PT.rep(s, "\\Q\\E", "");

			log("! pattern: " + s);
			p = Pattern.compile(s);
		}

		/**
		 * Find and regex-ify all {id=IFS.param::value} or {IFS.param::value}.
		 * 
		 * @param s growing regex string
		 * @return regex string with all {...} fixed
		 * @throws IFSException
		 */
		protected String compileIFSDefs(String s, boolean isFull, boolean replaceK) throws IFSException {
			while (s.indexOf("::") >= 0) {
				Matcher m = objectDefPattern.matcher(s);
				if (!m.find())
					return s;
				String param = m.group(1);
				String val = m.group(2);
				String pv = "{" + param + "::" + val + "}";
				if (val.indexOf("::") >= 0)
					val = compileIFSDefs(val, false, replaceK);
				int pt = param.indexOf("=");
				if (pt == 0)
					throw new IFSException("bad {def=key::val} expression: " + param + "::" + val);
				if (keys == null)
					keys = new LinkedHashMap<String, String>();
				String key;
				if (pt > 0) {
					key = param.substring(0, pt);
					param = param.substring(pt + 1);
				} else {
					key = param.replace('.', '0');
				}
				keys.put(key, param);
				String bk = "{" + key + "}";
				if (s.indexOf(bk) >= 0) {
					s = PT.rep(s, bk, "<" + key + ">");
				}
				// escape < and > here
				s = PT.rep(s, pv, (replaceK ? "\\E(?\3" + key + "\4\\Q" : "\\E(?<" + key + ">\\Q") + val + "\\E)\\Q");
			}
			if (isFull && (s.indexOf("<") >= 0 || s.indexOf("\3") >= 0)) {
				// now fix k< references and revert \3 \4
				s = PT.rep(s, "<", "\\E\\k<");
				s = PT.rep(s, ">", ">\\Q").replace('\3', '<').replace('\4', '>');
			}
			return s;
		}

		/**
		 * fix up {regex::...} phrases in IFS-extract.json. First pass isinitialization
		 * clips out regex sections so that they are not processed by ObjectParser;
		 * second pass puts it all together.
		 * 
		 * 
		 * @param s the string to protect; null for second pass
		 * @return
		 * @throws IFSException
		 */
		protected String protectRegex(String s) throws IFSException {
			if (sData.indexOf("{regex::") < 0)
				return (s == null ? sData : s);
			if (s == null) {
				// init
				s = sData;
				regexList = new ArrayList<>();
				int[] pt = new int[1];
				int i = 0;
				while ((pt[0] = s.indexOf("{regex::")) >= 0) {
					// save regex and replace by \0n\1
					int p0 = pt[0];
					String rx = getIFSExtractValue(s, "regex", pt);
					regexList.add("\\E" + rx + "\\Q");
					s = s.substring(0, p0) + "\0" + (i++) + "\1" + s.substring(pt[0]);
				}
			} else {
				// restore regex
				int p;
				while ((p = s.indexOf('\0')) >= 0) {
					int p2 = s.indexOf('\1');
					int i = Integer.parseInt(s.substring(p + 1, p2));
					s = s.substring(0, p) + regexList.get(i) + s.substring(p2 + 1);
				}
			}
			return s;
		}

	}


}
