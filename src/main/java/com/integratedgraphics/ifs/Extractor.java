package com.integratedgraphics.ifs;

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
import org.iupac.fairspec.api.IFSSerializerI;
import org.iupac.fairspec.assoc.IFSStructureDataAssociation;
import org.iupac.fairspec.common.IFSConst;
import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.common.IFSRepresentation;
import org.iupac.fairspec.core.IFSDataObject;
import org.iupac.fairspec.core.IFSDataObjectCollection;
import org.iupac.fairspec.core.IFSObject;
import org.iupac.fairspec.core.IFSRepresentableObject;
import org.iupac.fairspec.spec.IFSSpecData;
import org.iupac.fairspec.spec.IFSSpecDataFindingAid;
import org.iupac.fairspec.spec.IFSStructureSpec;
import org.iupac.fairspec.spec.IFSStructureSpecCollection;
import org.iupac.fairspec.struc.IFSStructure;
import org.iupac.fairspec.struc.IFSStructureRepresentation;
import org.iupac.fairspec.util.IFSDefaultJSONSerializer;
import org.iupac.fairspec.util.IFSDefaultStructurePropertyManager;
import org.iupac.fairspec.util.Util;

import com.integratedgraphics.ifs.api.IFSVendorPluginI;
import com.integratedgraphics.ifs.util.PubInfoExtractor;

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
	private static final String version = "0.0.1-alpha_2021_07_2";

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

	public static final String NEW_SPEC_KEY = "*NEW_SPEC*";

	protected static Pattern objectDefPattern = Pattern.compile("\\{([^:]+)::([^}]+)\\}");
	protected static Pattern pStarDotStar;

	protected static boolean debugging = false;
	protected static boolean readOnly = false;

	/**
	 * set true to only create finding aides, not extract file data
	 */
	protected static boolean createFindingAidsOnly = false;

	/**
	 * set true to allow failure to create pub info
	 */
	protected static boolean allowNoPubInfo = false;

	/**
	 * don't even try to read pub info -- debugging
	 */
	protected static boolean skipPubInfo = false;

	/**
	 * set true to zip up the extracted collection, placing that in the target
	 * directory
	 */
	protected static boolean createZippedCollection = false;

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
	 * extract version from IFS-extract.json
	 */
	protected String extractVersion;

	/**
	 * objects found in IFS-extract.json
	 */
	protected List<ObjectParser> objectParsers;

	/**
	 * Saving the zip contents from the ZIP file referred to by an IFS-extract
	 * {object} value.
	 * 
	 */
	protected static Map<String, Map<String, ZipEntry>> IFSZipContents = new LinkedHashMap<>();

	/**
	 * an optional local source directory to use instead of the one indicated in
	 * IFS-extract.json
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

	protected List<Object> products = new ArrayList<>();
	/**
	 * working local name, without the rootPath, as found in _IFS_manifest.json
	 */
	private String localizedName;

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
	private Map<String, IFSRepresentableObject<?>> htLocalizedNameToObject = new LinkedHashMap<>();

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

	private String localizedURL;

	private boolean haveExtracted;

	private String ifsid;

	public static final String CDX_FILE_DATA = "_struc.cdx";

	public static final String MOL_FILE_DATA = "_struc.mol";

	public static final String PNG_FILE_DATA = "_struc.png";

	public static final String STRUC_FILE_DATA_KEY = "_struc.";

	public Extractor() {
		clearZipCache();
		getStructurePropertyManager();
		noOutput = (createFindingAidsOnly || readOnly);
	}

	/**
	 * @return the FindingAid as a string
	 */
	public final String extractAndCreateFindingAid(File ifsExtractScriptFile, String localSourceDir, File targetDir,
			String findingAidFileNameRoot) throws IOException, IFSException {

		// first create objects, a List<String>

		getObjectParsersForFile(ifsExtractScriptFile);
		String puburi = null;
		Map<String, Object> pubCrossrefInfo = null;
		try {
			puburi = (String) findingAid.getPropertyValue(IFSConst.IFS_PROP_COLLECTION_SOURCE_PUBLICATION_URI);
			if (puburi != null && !skipPubInfo) {
				pubCrossrefInfo = PubInfoExtractor.getPubInfo(puburi);
				findingAid.setPubInfo(pubCrossrefInfo);
			}
		} catch (IOException e) {
			logErr("Could not access " + PubInfoExtractor.getCrossrefUrl(puburi));
			e.printStackTrace();
		}
		if (pubCrossrefInfo == null && !allowNoPubInfo) {
			logErr("Finding aid does not contain PubInfo! No internet? cannot continue");
			return null;
		}

		setLocalSourceDir(localSourceDir);
		// options here to set cache and rezip options -- debugging only!
		setCachePattern(null);
		setRezipCachePattern(null, null);

		// now actually do the extraction.

		extractObjects(targetDir);

		System.out.println("Extractor serializing...");
		return findingAid.createSerialization((noOutput && !createFindingAidsOnly ? null : targetDir),
				findingAidFileNameRoot, createZippedCollection ? products : null, getSerializer());
	}

	/**
	 * Implementing subclass could use a different serializer.
	 * 
	 * @return a serializer
	 */
	protected IFSSerializerI getSerializer() {
		return new IFSDefaultJSONSerializer();
	}

	public void setLocalSourceDir(String sourceDir) {
		if (sourceDir != null && sourceDir.indexOf("://") < 0)
			sourceDir = "file:///" + sourceDir;
		this.sourceDir = sourceDir;
	}

	///////// Vendor-related methods /////////

	/**
	 * Cache the property change created by an IFSVendorPluginI class. This method
	 * is callback from IFSVendorPluginI classes only.
	 */
	@Override
	public void addProperty(String key, Object val) {
		propertyList.add(new Object[] { localizedName, key, val });
		if (key.startsWith(STRUC_FILE_DATA_KEY)) {
			Object[] oval = (Object[]) val;
			// this will add InChI, SMILES, and InChIKey if a MOL or SDF file
			getStructurePropertyManager().processRepresentation((String) oval[1], (byte[]) oval[0]);
		}
	}

	@Override
	public IFSSpecDataFindingAid getFindingAid() {
		return findingAid;
	}

	/**
	 * Set the regex string assembing all vendor requests. Each vendor's pattern
	 * will be surrounded by (?<param0> ... ), (?<param1> ... ), etc. Here we wrap
	 * them all with (?<param>....), then add on our non-vender checks, and finally
	 * wrap all this using (?<type>...).
	 */
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
		cache = new LinkedHashMap<String, IFSRepresentation>();
	}

	/**
	 * The regex pattern uses param0, param1, etc., to indicated parameters for
	 * different vendors. This method looks through the activeVendor list to retrieve
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
		if (findingAid != null)
			throw new IFSException("Only one finding aid per instance of Extractor is allowed (for now).");

		findingAid = newFindingAid();

		Map<String, Object> jsonMap = (Map<String, Object>) new JSJSONParser().parse(script, false);
		if (debugging)
			log(jsonMap.toString());
		extractVersion = (String) jsonMap.get("IFS-extract-version");
		log(extractVersion);
		List<ObjectParser> objectParsers = getObjects((List<Map<String, Object>>) jsonMap.get("keys"));
		log(objectParsers.size() + " extractor regex strings");

		log("! license: " + findingAid.getPropertyValue(IFSConst.IFS_PROP_COLLECTION_DATA_LICENSE_NAME) + " at "
				+ findingAid.getPropertyValue(IFSConst.IFS_PROP_COLLECTION_DATA_LICENSE_URI));

		return objectParsers;
	}

	private IFSSpecDataFindingAid newFindingAid() throws IFSException {
		return new IFSSpecDataFindingAid(null, codeSource + " " + version);
	}

	/**
	 * Make all variable substitutions in IFS-extract.js.
	 * 
	 * @return list of ObjectParsers that have successfully parsed the {object}
	 *         lines of the file
	 * @throws IFSException
	 */
	protected List<ObjectParser> getObjects(List<Map<String, Object>> pathway) throws IFSException {

		// input:

//		{"IFS-extract-version":"0.1.0-alpha","keys":[
//         {"example":"compound directories containing unidentified bruker files and hrms zip file containing .pdf"},
//         {"journal":"acs.orglett"},{"hash":"0c00571"},
//         {"figshareid":"21975525"},
//         
//         {"ifsid=IFS.property.collection.id":"{journal}.{hash}"},
//         {"IFS.property.collection.source.publication.uri":"https://doi.org/10.1021/{ifsid}"},
//         {"IFS.property.collection.data.license.uri":"https://creativecommons.org/licenses/by-nc/4.0"},
//         {"IFS.property.collection.data.license.name":"cc-by-nc-4.0"},
//         
//         {"data0":"{IFS.property.collection.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/{ifsid}/suppl_file/ol{hash}_si_002.zip}"},
//         {"data":"{IFS.property.collection.source.data.uri::https://ndownloader.figshare.com/files/{figshareid}}"},
//
//         {"path":"{data}|FID for Publication/{id=IFS.property.struc.compound.id::*}.zip|"},
//         {"IFS.property.collection.object":"{path}{IFS.representation.spec.nmr.vendor.dataset::{IFS.property.spec.nmr.expt.id::<id>/{xpt=::*}}.zip|{xpt}/*/}"},
//         {"IFS.property.collection.object":"{path}<id>/{IFS.representation.struc.mol.2d::<id>.mol}"},
//         {"IFS.property.collection.object":"{path}{IFS.representation.spec.hrms.pdf::{IFS.property.spec.hrms.expt.id::<id>/HRMS.zip|**/*}.pdf}"}
//        ]}

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
				String keyDef = null;
				int pt = key.indexOf("=");
				if (pt > 0) {
					keyDef = key.substring(0, pt);
					key = key.substring(pt + 1);
				}
				if (key.startsWith("IFS.property")) {
					switch (key) {
					case IFSConst.IFS_PROP_COLLECTION_ID:
						ifsid = val;
						findingAid.setID(val);
						break;
					case IFSConst.IFS_PROP_COLLECTION_OBJECT:
						parsers.add(newObjectParser(val));
						continue;
					}
					findingAid.setPropertyValue(key, val);
					if (keyDef == null)
						continue;
				}
				keys.addLast("{" + (keyDef == null ? key : keyDef) + "}");
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
	public IFSSpecDataFindingAid extractObjects(File targetDir) throws IFSException, IOException {
		if (haveExtracted)
			throw new IFSException("Only one extraction per instance of Extractor is allowed (for now).");
		haveExtracted = true;
		if (targetDir == null)
			throw new IFSException("The target directory may not be null.");
		if (cache == null)
			setCachePattern(null);
		if (rezipCache == null)
			setRezipCachePattern(null, null);
		this.targetDir = targetDir;
		targetDir.mkdir();

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

		// Note that some files have multiple objects.
		// These may come from multiple sources, or they may be from the same source.
		propertyList = new ArrayList<>();

		for (int i = 0; i < objectParsers.size(); i++) {

			ObjectParser parser = objectParsers.get(i);
			dataSource = parser.dataSource;
			if (!dataSource.equals(lastURL)) {
				findingAid.addSource(dataSource);
				lastURL = dataSource;
			}
			// localize the URL if we are using a local copy of a remote resource.

			localizedURL = localizeURL(dataSource);
			if (debugging)
				log("opening " + localizedURL);
			lastRootPath = initializeCollection(lastRootPath);

			// At this point we now have all spectra ready to be associated with
			// struc!tures.

			// 2.1
			log("! PHASE 2.1 \n" + localizedURL + "\n" + parser.sData);
			boolean haveData = parseZipFileNamesForObjects(parser);

			// 2.2
			log("! PHASE 2.2 rezip haveData=" + haveData);
			if (haveData)
				rezipFilesAndExtractProperties();

			log("found " + ifsObjectCount + " IFS objects");

		}

		// update object vendor properties

		updateObjectProperties();

		// update object type and len records

		addCachedRepresentationsToObjects();

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
			for (IFSDataObject<?> spec : c) {
				if (bs.get(spec.getIndex())) {
					found.add((IFSSpecData) spec);
					log("! removing duplicate spec reference " + spec.getName() + " for "
							+ assoc.getFirstStructure().getName());
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
			log("! " + n + " objects removed");
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
				log("! " + n + " objects removed");
		}
	}

	/**
	 * Initialize the paths.
	 * 
	 * @param lastRootPath and manifest files
	 * @return
	 * @throws IOException
	 */
	private String initializeCollection(String lastRootPath) throws IOException {

		String zipPath = localizedURL.substring(localizedURL.lastIndexOf(":") + 1);
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
			products.add(targetDir + "/" + rootPath);
			saveCollectionManifests(true);
		}

		return lastRootPath;
	}

	/**
	 * Parse the zip file using an object parser.
	 * 
	 * @param parser
	 * @return true if have spectra objects
	 * @throws IOException
	 * @throws IFSException
	 */
	private boolean parseZipFileNamesForObjects(ObjectParser parser) throws IOException, IFSException {
		boolean haveData = false;

		// first build the file list
		String key = localizedURL;
		Map<String, ZipEntry> zipFiles = IFSZipContents.get(key);
		if (zipFiles == null) {
			// Scan URL zip stream for files.
			log("! retrieving " + localizedURL);
			URL url = new URL(localizedURL);// getURLWithCachedBytes(zipPath); // BH carry over bytes if we have them
											// for JS
			InputStream stream;
			long len;
			if ("file".equals(url.getProtocol())) {
				stream = url.openStream();
				len = new File(url.getPath()).length();
			} else {
				File tempFile = File.createTempFile("extract", ".zip");
				localizedURL = "file:///" + tempFile.getAbsolutePath();
				log("! saving " + url + " as " + tempFile);
				Util.getLimitedStreamBytes(url.openStream(), -1, new FileOutputStream(tempFile), true, true);
				log("! saved " + tempFile.length() + " bytes");
				len = tempFile.length();
				stream = new FileInputStream(tempFile);
			}
			findingAid.setCurrentURLLength(len);
			zipFiles = readZipContentsIteratively(stream, new LinkedHashMap<String, ZipEntry>(), "", false);
			IFSZipContents.put(key, zipFiles);
		}
		// next, we process those names

		for (String ifsPath : zipFiles.keySet()) {
			IFSObject<?> obj = addIFSObjectsForName(parser, ifsPath);
			if (obj != null) {
				System.out.println(ifsPath);
				ifsObjectCount++;
				if (obj instanceof IFSDataObject || obj instanceof IFSStructureSpec)
					haveData = true;
			}
		}
		return haveData;
	}

	/**
	 * Get a new ObjectParser for this data. Note that this method may be overridden
	 * if desired.
	 * 
	 * @param sData
	 * @return
	 * @throws IFSException
	 */
	protected ObjectParser newObjectParser(String sObj) throws IFSException {
		return new ObjectParser(sObj);
	}

	/**
	 * Process all entries in a zip file, looking for files to extract and
	 * directories to rezip.
	 * 
	 * @param is
	 * @param fileNames
	 * @param basePath
	 * @param doRezip
	 * @return
	 * @throws IOException
	 */
	protected Map<String, ZipEntry> readZipContentsIteratively(InputStream is, Map<String, ZipEntry> fileNames,
			String basePath, boolean doRezip) throws IOException {
		if (debugging && basePath.length() > 0)
			log("! opening " + basePath);
		boolean isTopLevel = (basePath.length() == 0);
		ZipInputStream zis = new ZipInputStream(is);
		ZipEntry zipEntry = null;
		ZipEntry nextEntry = null;
		int n = 0;
		int phase = (doRezip ? 2 : 1);
		while ((zipEntry = (nextEntry == null ? zis.getNextEntry() : nextEntry)) != null) {
			n++;
			nextEntry = null;
			String ifsPath = basePath + zipEntry.getName();
			if (zipEntry.isDirectory()) {
				log("Phase " + phase + " checking zip directory: " + n + " " + ifsPath);
			} else if (!zipEntry.isDirectory() && zipEntry.getSize() == 0) {

				continue;
			}

			if (junkPattern.matcher(ifsPath).find()) {
				// Test 9: acs.orglett.0c01153/22284726,22284729 MACOSX,
				// acs.joc.0c00770/22567817
				addFileToFileLists(ifsPath, LOG_IGNORED, zipEntry.getSize());
				continue;
			}
			if (debugging)
				log("reading zip entry: " + n + " " + ifsPath);

			if (fileNames != null) {
				fileNames.put(ifsPath, zipEntry); // Java has no use for the ZipEntry, but JavaScript can read it.
			}
			if (ifsPath.endsWith(".zip")) {
				readZipContentsIteratively(zis, fileNames, ifsPath + "|", doRezip);
			} else if (doRezip) {
				nextEntry = processRezipEntry(basePath, ifsPath, zis, zipEntry);
			} else {
				processZipEntry(ifsPath, zis, zipEntry);
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
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	private void rezipFilesAndExtractProperties() throws MalformedURLException, IOException {
		if (rezipCache != null && rezipCache.size() > 0) {
			lastRezipPath = null;
			getNextRezipName();
			readZipContentsIteratively(new URL(localizedURL).openStream(), null, "", true);
		}
	}

	class AWrap {

		private byte[] a;

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

	private Map<AWrap, IFSStructure> htStructureRepCache;

	int test = 0;
	byte[] b0, b1;

	/**
	 * Process the properties in propertyList after the IFSObject objects have een
	 * created for all resources.
	 * 
	 * @throws IFSException
	 * @throws IOException
	 */
	private void updateObjectProperties() throws IFSException, IOException {
		String lastLocal = null;
		IFSSpecData localSpec = null;
		IFSStructure struc = null;
		for (int i = 0, n = propertyList.size(); i < n; i++) {
			Object[] a = propertyList.get(i);
			String localizedName = (String) a[0];
			boolean isNew = (!localizedName.equals(lastLocal));
			if (isNew) {
				lastLocal = localizedName;
			}
			String key = (String) a[1];
			Object value = a[2];
			IFSRepresentableObject<?> spec;
			// link to the originating spec representation -- xxx.mnova, xxx.zip
			spec = htLocalizedNameToObject.get(localizedName);
			if (isNew && spec instanceof IFSSpecData)
				localSpec = (IFSSpecData) spec;
			if (spec == null) {
				logErr("manifest not found for " + localizedName);
				continue;
			}
			if (IFSConst.isRepresentation(key)) {
				// from reportVendor -- Bruker adds this for thumb.png and pdf files.
				String ifsPath = value.toString();
				addRepresentation(ifsPath, key, spec);
				continue;
			}
			if (key.equals(NEW_SPEC_KEY)) {
				String idExtension = (String) value;
				IFSSpecData newSpec = findingAid.cloneSpec(localSpec, idExtension);
				spec = newSpec;
				struc = findingAid.firstStructureForSpec(localSpec);
				if (struc != null)
					findingAid.associate(idExtension, struc, newSpec);
				htLocalizedNameToObject.put(localizedName, spec);
				continue;
			}
			if (key.startsWith(STRUC_FILE_DATA_KEY)) {
				Object[] oval = (Object[]) value;
				byte[] bytes = (byte[]) oval[0];
				String ifsPath = (String) oval[1];
				String ifsRepType = IFSDefaultStructurePropertyManager.getType(key.substring(key.length() - 3), bytes);
				if (htStructureRepCache == null)
					htStructureRepCache = new HashMap<>();
				AWrap w = new AWrap(bytes);
				IFSStructure cachedStruc = htStructureRepCache.get(w);
				if (cachedStruc == null) {
					File f = getAbsoluteFileTarget(ifsPath);
					writeBytesToFile(bytes, f);
					struc = findingAid.firstStructureForSpec(localSpec);
					if (struc != null) {
						htStructureRepCache.put(w, struc);
						// MNova 1 page, 1 spec, 1 structure Test #5
						addFileAndCacheRepresentation(ifsPath, null, bytes.length, ifsRepType, null);
						linkLocalizedNameToObject(localizePath(ifsPath), ifsRepType, struc);
						// struc.add(new IFSStructureRepresentation(rep));
						continue;
					}
					// but we will need to find these as well.
//					struc = findingAid.addStructureForSpec(rootPath, ifsRepType, fileName, localizedName + fileName);
				} else {
					// what now?
				}
				continue;
			}
			spec.setPropertyValue(key, value);

		}
		propertyList.clear();
		htStructureRepCache = null;
	}

	private void addRepresentation(String ifsPath, String key, IFSRepresentableObject<?> spec) {
		String localizedName = localizePath(ifsPath);
		linkLocalizedNameToObject(localizedName, null, spec);
		spec.addRepresentation(ifsPath, localizedName, key, key);
//		IFSRepresentation rep = 
//		rep.setSubtype(key);
	}

	/**
	 * Set the type and len fields for structure and spec data
	 */
	private void addCachedRepresentationsToObjects() {

		for (String localizedName : cache.keySet()) {
			IFSRepresentableObject<?> obj = htLocalizedNameToObject.get(localizedName);
			if (obj == null) {
				logErr("manifest not found for " + localizedName);
			} else {
				IFSRepresentation r = cache.get(localizedName);
				String ifsPath = r.getRef().getOrigin().toString();
				String type = r.getType();
				String subtype = r.getSubtype();
				IFSRepresentation r1 = obj.addRepresentation(ifsPath, localizedName, r.getType(), null);
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
		if (!isOpen) {
			if (createFindingAidsOnly || readOnly) {
				if (lstIgnored.size() > 0) {
					logErr("ignored " + lstIgnored.size() + " files");
				}
			} else {
				writeBytesToFile(extractScript.getBytes(), getAbsoluteFileTarget("_IFS_extract.json"));
				outputListJSON(lstManifest, getAbsoluteFileTarget("_IFS_manifest.json"), "manifest");
				outputListJSON(lstIgnored, getAbsoluteFileTarget("_IFS_ignored.json"), "ignored");
			}
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
	 * @param ifsPath
	 * @return one of IFSStructureSpec, IFSSpecData, IFSStructure, in that order,
	 *         depending upon availability
	 * 
	 * @throws IFSException
	 */
	private IFSObject<?> addIFSObjectsForName(ObjectParser parser, String ifsPath) throws IFSException {
		Matcher m = parser.p.matcher(ifsPath);
		if (!m.find())
			return null;
		findingAid.beginAddingObjects(ifsPath);
		if (debugging)
			log("adding IFSObjects for " + ifsPath);

		// If an IFSSpecData object is added, then it will also be added to
		// htManifestNameToSpecData

		for (String key : parser.keys.keySet()) {
			String param = parser.keys.get(key);
			if (param.length() > 0) {
				String id = m.group(key);
				final String localizedName = localizePath(ifsPath);
				IFSRepresentableObject<?> obj = findingAid.addObject(rootPath, param, id, localizedName);
				linkLocalizedNameToObject(localizedName, param, obj);
				if (debugging)
					log("found " + param + " " + id);
				;
			}
		}
		return findingAid.endAddingObjects();
	}

	/**
	 * Link a representation with the given local name and type to an object such as
	 * a spectrum or structure. Later in the process, this representation will be
	 * added to the object.
	 * 
	 * @param localizedName
	 * @param type
	 * @param obj
	 */
	private void linkLocalizedNameToObject(String localizedName, String type, IFSRepresentableObject<?> obj) {
		if (type == null || IFSConst.isRepresentation(type)) {
			htLocalizedNameToObject.put(localizedName, obj);
		}
	}

	protected void logErr(String msg) {
		msg = "!! " 
				+ ifsid + " " + rootPath + " ERR: " + msg;
		log(msg);
		errorLog += msg + "\n";
	}

	protected static String errorLog = "";

	public static int testID = -1;

	/**
	 * Just a very simple logger. Messages that start with "!" are always logged;
	 * others are logged if debugging is set to true.
	 * 
	 * 
	 * @param msg
	 */
	protected static void log(String msg) {
		boolean isErr = msg.startsWith("!!");
		boolean isAlert = msg.startsWith("!");
		if(testID >= 0)
			msg =  "test " + testID + ": " + msg; 
		if (Util.logStream != null) {
			try {
				Util.logStream.write((msg + "\n").getBytes());
			} catch (IOException e) {
			}
		}
		if (isErr) {
			System.err.println(msg);
		} else if (isAlert) {
			System.out.println(msg);
		}
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

	/**
	 * Process an entry for rezipping, jumping to the next unrelated entry.
	 * 
	 * When a ZipEntry is a directory and has been identified as a SpecData object,
	 * we need to catalog and rezip that file.
	 * 
	 * Create a new zip file that reconfigures the file directory to contain what we
	 * want it to.
	 * 
	 * Note that the rezipping process takes two passes, because the first pass has
	 * most likely already passed by one or more files associated with this
	 * rezipping project.
	 * 
	 * 
	 * @param baseName
	 * @param ifsPath
	 * @param zis
	 * @param entry
	 * @return next (unrelated) entry
	 * @throws IOException
	 */
	private ZipEntry processRezipEntry(String baseName, String ifsPath, ZipInputStream zis, ZipEntry entry)
			throws IOException {
		if (!ifsPath.equals(currentRezipPath)) {
			String localizedName = localizePath(ifsPath);
			if (!entry.isDirectory() && !lstIgnored.contains(ifsPath) && !lstManifest.contains(localizedName)) {
				addFileToFileLists(ifsPath, LOG_IGNORED, entry.getSize());
				logErr("ignoring " + ifsPath);
			}
			return null;
		}
		IFSVendorPluginI vendor = currentRezipVendor;
		String dirName = entry.getName();
		log("! rezipping " + ifsPath + " for " + entry + " " + new File(entry.getName()).getName());
		File outFile = getAbsoluteFileTarget(ifsPath + ".zip");
		final String localizedName = localizePath(ifsPath);
		OutputStream fos = (noOutput ? new ByteArrayOutputStream() : new FileOutputStream(outFile));
		ZipOutputStream zos = new ZipOutputStream(fos);
		String parent = new File(entry.getName()).getParent();
		int lenOffset = (parent == null ? 0 : parent.length() + 1);
		// because Bruker directories must start with a number
		String newDir = vendor.getRezipPrefix(dirName.substring(lenOffset, dirName.length() - 1)); // trimming trailing
																									// // '/'
		if (newDir == null) {
			newDir = "";
		} else {
			newDir = newDir + "/";
			lenOffset = dirName.length();
		}
		Matcher m = null;
		this.localizedName = localizedName;
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
			boolean doInclude = (vendor == null || vendor.doRezipInclude(this, baseName, entryName));
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
					this.localizedName = localizedName;
					if (mgr == null || mgr == vendor) {
						vendor.accept(null, ifsPath + outName, bytes);
					} else {
						mgr.accept(this, ifsPath + outName, bytes);
					}
				}
				if (doInclude)
					zos.closeEntry();
			}
		}
		vendor.endRezip();
		zos.close();
		fos.close();
		String dataType = vendor.processRepresentation(ifsPath + ".zip", null);
		len = (noOutput ? ((ByteArrayOutputStream) fos).size() : outFile.length());
		IFSRepresentation r = findingAid.getSpecDataRepresentation(ifsPath);
		if (r == null) {
			System.out.println("! r not found for " + ifsPath);
			// could be just structure at this point
		} else {
			r.setLength(len);
		}
		cacheFileRepresentation(ifsPath, localizedName, len, dataType, "application/zip");
		addFileToFileLists(localizedName, LOG_OUTPUT, len);
		getNextRezipName();
		return entry;
	}

	/**
	 * Pull the next rezip parent directory name off the stack, setting the
	 * currentRezipPath and currentRezipVendor fields.
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
	 * @param ifsPath  path to this entry including | and / but not rootPath
	 * @param zis
	 * @param zipEntry
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	protected void processZipEntry(String ifsPath, InputStream zis, ZipEntry zipEntry)
			throws FileNotFoundException, IOException {
		long len = zipEntry.getSize();
		Matcher m;

		// check for files that should be pulled out - these might be JDX files, for
		// example.
		// "param" appears if a vendor has flagged these files for parameter extraction.

		if (cachePattern != null && (m = cachePattern.matcher(ifsPath)).find()) {
			IFSPropertyManagerI v = getPropertyManager(m);
			boolean doCheck = (v != null);
			boolean doExtract = (!doCheck || v.doExtract(ifsPath));

			System.out.println("! caching " + ifsPath);
//			1. we don't have params 
//		      - generic file, just save it.  doExtract and not doCheck
//			2. we have params and there is extraction
//		      - save file and also check it for parameters  doExtract and doCheck
//			3. we have params but no extraction  !doCheck  and !doExtract
//		      - ignore completely

			File f = getAbsoluteFileTarget(ifsPath);
			OutputStream os = (!doExtract ? null
					: doCheck || noOutput ? new ByteArrayOutputStream() : new FileOutputStream(f));
			if (os != null)
				Util.getLimitedStreamBytes(zis, len, os, false, true);
			String localizedName = localizePath(ifsPath);
			if (doExtract) {
				String type = null;
				if (doCheck || noOutput) {
					byte[] bytes = ((ByteArrayOutputStream) os).toByteArray();
					len = bytes.length;
					if (doCheck) {
						// set this.localizedName for parameters
						// preserve this.localizedName, as we might be in a rezip.
						// as, for example, a JDX file within a Bruker dataset
						writeBytesToFile(bytes, f);
						String oldLocal = this.localizedName;
						this.localizedName = localizedName;
						// indicating "this" here notifies the vendor plugin that
						// this is a one-shot file, not a collection.
						type = v.accept(this, ifsPath, bytes);
						this.localizedName = oldLocal;
					}
				} else {
					len = f.length();
				}
				addFileAndCacheRepresentation(ifsPath, localizedName, len, type, m.group("type"));
			}
		}

		// here we look for the "trigger" file within a zip file that indicates that we
		// (may) have a certain vendor's files that need looking into. The case in point
		// is finding a procs file within a Bruker data set. Or, in principle, an acqus
		// file and just an FID but no pdata/ directory. But for now we want to see that
		// processed data.

		if (rezipCachePattern != null && (m = rezipCachePattern.matcher(ifsPath)).find()) {

			// e.g. exptno/./pdata/procs

			IFSVendorPluginI v = getVendorForRezip(m);
			ifsPath = m.group("path" + v.getIndex());
			if (ifsPath.equals(lastRezipPath)) {
				log("duplicate path " + ifsPath);
			} else {
				lastRezipPath = ifsPath;
				IFSRepresentation ref = new CacheRepresentation(
						new IFSReference(ifsPath, localizePath(ifsPath), rootPath), v, len, null, "application/zip");
				rezipCache.add(ref);
				log("rezip pattern found " + ifsPath);
			}
		}

	}

	/**
	 * 
	 * @param ifsPath
	 * @param localizedName        ifsPath with localized / and |
	 * @param len
	 * @param ifsType              IFS.representation....
	 * @param fileNameForMediaType
	 * @return
	 */
	private IFSRepresentation addFileAndCacheRepresentation(String ifsPath, String localizedName, long len,
			String ifsType, String fileNameForMediaType) {
		if (localizedName == null)
			localizedName = localizePath(ifsPath);
		if (fileNameForMediaType == null)
			fileNameForMediaType = localizedName;
		addFileToFileLists(localizedName, LOG_OUTPUT, len);
		String subtype = IFSSpecDataFindingAid.mediaTypeFromName(fileNameForMediaType);
		return cacheFileRepresentation(ifsPath, localizedName, len, ifsType, subtype);
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
	 * Should be no throwing of Exceptions here -- we know if we have (?<param>...)
	 * groups.
	 * 
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
	 * Cache file representation for this resource, associating it with a media type
	 * if we can.
	 * 
	 * @param ifsPath       slash-based reference to object
	 * @param localizedName ifsPath with / and | removed.
	 * @param len
	 * @param type
	 * @param subtype       a media type, typically
	 * 
	 * @return
	 */
	private IFSRepresentation cacheFileRepresentation(String ifsPath, String localizedName, long len, String type,
			String subtype) {
		if (subtype == null)
			subtype = IFSSpecDataFindingAid.mediaTypeFromName(localizedName);
		CacheRepresentation rep = new CacheRepresentation(new IFSReference(ifsPath, localizedName, rootPath), null, len,
				type, subtype);
		cache.put(localizedName, rep);
		return rep;
	}

	/**
	 * Get the full OS file path for FileOutputStream
	 * 
	 * @param fname
	 * @return
	 */
	protected File getAbsoluteFileTarget(String fname) {
		return new File(targetDir + "/" + rootPath + "/" + localizePath(fname));
	}

	/**
	 * Clean up the zip entry name to remove '|', '/', ' ', and add ".zip" if there
	 * is a trailing '/' in the name.
	 * 
	 * @param path
	 * @return
	 */
	public static String localizePath(String path) {
		boolean isDir = path.endsWith("/");
		return path.replace('|', '_').replace('/', '_').replace('#', '_').replace(' ', '_') + (isDir ? ".zip" : "");
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

		private static final String TEMP_KEYVAL_IN = REGEX_UNQUOTE + "(?" + TEMP_KEYVAL_IN_CHAR;

		private static final String TEMP_KEYVAL_OUT = TEMP_KEYVAL_OUT_CHAR + REGEX_QUOTE;

		/**
		 * for example *-*.zip -->
		 */
		private static final String TEMP_ANY_SEP_ANY_GROUPS = REGEX_UNQUOTE + "(" + "[^\5]+(?:\5[^\5]+)"
				+ TEMP_STAR_CHAR + ")" + REGEX_QUOTE;

		private static final String TEMP_ANY_DIRECTORIES = REGEX_UNQUOTE + "(?:[^/]+/)" + TEMP_STAR_CHAR + REGEX_QUOTE;

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
			dataSource = getIFSExtractValue(sObj, IFSConst.IFS_PROP_COLLECTION_SOURCE_DATA_URI, pt);
			if (dataSource == null)
				throw new IFSException(
						"No {" + IFSConst.IFS_PROP_COLLECTION_SOURCE_DATA_URI + "::...} found in " + sObj);
			sData = sObj.substring(pt[0] + 1); // skip first "|"
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
			// {IFS.property.spec.nmr.expt.id::xxx} becomes
			// \\E(?<IFS0nmr0param0expt>\\Qxxx\\E)\\Q
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

			s = PT.rep(s, "**/", TEMP_ANY_DIRECTORIES);

			Matcher m;
			// *-* becomes \\E([^-]+(?:-[^-]+)*)\\Q and matches a-b-c
			if (s.indexOf("*") != s.lastIndexOf("*")) {
				if (pStarDotStar == null)
					pStarDotStar = Pattern.compile("\\*(.)\\*");
				while ((m = pStarDotStar.matcher(s)).find()) {
					String schar = m.group(1);
					s = PT.rep(s, "*" + schar + "*",
							TEMP_ANY_SEP_ANY_GROUPS.replace(TEMP_ANY_SEP_ANY_CHAR, schar.charAt(0)));
				}
			}
			// * becomes \\E.+\\Q

			s = PT.rep(s, "*", REGEX_ANY_NOT_PIPE_OR_DIR);

			// {id=IFS.property.spec.nmr.expt.id::xxx} becomes \\E(?<id>\\Qxxx\\E)\\Q
			// {IFS.property.spec.nmr.expt.id::xxx} becomes
			// \\E(?<IFS0nmr0param0expt>\\Qxxx\\E)\\Q
			// <id> becomes \\k<id>

			s = compileIFSDefs(s, true, true);

			// restore '*'
			s = s.replace(TEMP_STAR_CHAR, '*');

			// restore regex
			// wrap with quotes and constraints ^\\Q...\\E$

			s = "^" + REGEX_QUOTE + protectRegex(s) + REGEX_UNQUOTE + "$";

			// \\Q\\E in result is removed

			s = PT.rep(s, REGEX_EMPTY_QUOTE, "");

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
				s = PT.rep(s, pv,
						(replaceK ? TEMP_KEYVAL_IN + key + TEMP_KEYVAL_OUT : REGEX_KEYDEF_START + key + REGEX_KV_END)
								+ val + REGEX_END_PARENS);
			}
			if (isFull && (s.indexOf("<") >= 0 || s.indexOf(TEMP_KEYVAL_IN_CHAR) >= 0)) {
				// now fix k< references and revert \3 \4
				s = PT.rep(s, "<", REGEX_KEYVAL_START);
				s = PT.rep(s, ">", REGEX_KV_END).replace(TEMP_KEYVAL_IN_CHAR, '<').replace(TEMP_KEYVAL_OUT_CHAR, '>');
			}
			return s;
		}

		/**
		 * fix up {regex::...} phrases in IFS-extract.json. First pass initialization
		 * clips out regex sections so that they are not processed by ObjectParser;
		 * second pass puts it all together.
		 * 
		 * 
		 * @param s the string to protect; null for second pass
		 * @return
		 * @throws IFSException
		 */
		protected String protectRegex(String s) throws IFSException {
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
					String rx = getIFSExtractValue(s, "regex", pt);
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

	}

}
