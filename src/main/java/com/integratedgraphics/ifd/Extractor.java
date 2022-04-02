package com.integratedgraphics.ifd;

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

import org.iupac.fairdata.api.IFDExtractorI;
import org.iupac.fairdata.api.IFDPropertyManagerI;
import org.iupac.fairdata.api.IFDSerializerI;
import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.common.IFDReference;
import org.iupac.fairdata.contrib.IFDDefaultStructureHelper;
import org.iupac.fairdata.contrib.IFDFAIRSpecExtractorHelper;
import org.iupac.fairdata.core.IFDAssociation;
import org.iupac.fairdata.core.IFDCollection;
import org.iupac.fairdata.core.IFDFAIRDataFindingAid;
import org.iupac.fairdata.core.IFDObject;
import org.iupac.fairdata.core.IFDRepresentableObject;
import org.iupac.fairdata.core.IFDRepresentation;
import org.iupac.fairdata.dataobject.IFDDataObject;
import org.iupac.fairdata.derived.IFDStructureDataAssociationCollection;
import org.iupac.fairdata.structure.IFDStructure;
import org.iupac.fairdata.util.IFDDefaultJSONSerializer;
import org.iupac.fairdata.util.IFDUtilities;

import com.integratedgraphics.ifd.api.IFDVendorPluginI;
import com.integratedgraphics.ifd.util.PubInfoExtractor;

import javajs.util.JSJSONParser;
import javajs.util.Lst;
import javajs.util.PT;

/**
 * Copyright 2021/2022 Integrated Graphics and Robert M. Hanson
 * 
 * A class to handle the extraction of objects from a "raw" dataset
 * by processing the full paths within a ZIP file as directed by 
 * an extraction template (from the extract/ folder for the test)
 * 
 * following the sequence:
 * 
 * initialize(ifdExtractScriptFile)`
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
 * ... metadata property information is from org.iupac.common.fairspec.properties
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
 * ... binary MNova files are scanned for metadata, PNG, and MOL files (only, not spectra)
 * 
 * ... MNova metadata references page number in file using #page=
 * 
 *  
 * See ExtractorTest and IFDFAIRSpecExtractorHelper for more information.
 * 
 * @author hansonr
 *
 */
public class Extractor implements IFDExtractorI {

	private static class CacheRepresentation extends IFDRepresentation {

		public CacheRepresentation(IFDReference ifdReference, Object o, long len, String type, String subtype) {
			super(ifdReference, o, len, type, subtype);
		}

	}

	static {
		IFDVendorPluginI.init();
	}
	private static final String version = "0.0.2-alpha+2002.04.02";

	private static final String codeSource = "https://github.com/IUPAC/IUPAC-FAIRSpec/blob/main/src/main/java/com/integratedgraphics/ifd/Extractor.java";

	/**
	 * patterns to ignore completely.
	 */
	private final static Pattern junkPattern = Pattern.compile(IFDFAIRSpecExtractorHelper.junkFilePattern);

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
	protected static boolean allowNoPubInfo = true;

	/**
	 * don't even try to read pub info -- debugging
	 */
	protected static boolean skipPubInfo = false;

	/**
	 * set to true add the source metadata from Crossref or DataCite
	 */
	protected static boolean addPublicationMetadata = false;

	/**
	 * set true to zip up the extracted collection, placing that in the target
	 * directory
	 */
	protected static boolean createZippedCollection = false;

	protected static String logfile;

	/**
	 * the finding aid - only one per instance
	 */
	protected IFDFAIRSpecExtractorHelper helper;

	/**
	 * the IFD-extract.json script
	 */
	private String extractScript;

	/**
	 * extract version from IFD-extract.json
	 */
	protected String extractVersion;

	/**
	 * objects found in IFD-extract.json
	 */
	protected List<ObjectParser> objectParsers;

	/**
	 * Saving the zip contents from the ZIP file referred to by an IFD-extract
	 * {object} value.
	 * 
	 */
	protected static Map<String, Map<String, ZipEntry>> IFDZipContents = new LinkedHashMap<>();

	/**
	 * an optional local source directory to use instead of the one indicated in
	 * IFD-extract.json
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
	 * working local name, without the rootPath, as found in _IFD_manifest.json
	 */
	private String localizedName;

	/**
	 * rezip data saved as an ISFRepresentation (for no particularly good reason)
	 */
	private IFDRepresentation currentRezipRepresentation;

	/**
	 * path to this resource in the original zip file
	 */
	private String currentRezipPath;

	/**
	 * vendor association with this rezipping
	 */
	private IFDVendorPluginI currentRezipVendor;

	/**
	 * last path to this rezip top-level resource
	 */
	private String lastRezipPath;

	/**
	 * the number of bytes extracted
	 */
	protected long extractedByteCount;

	/**
	 * the number of IFDObjects created
	 */
	private int ifdObjectCount;

	/**
	 * cache of top-level resources to be rezipped
	 */
	protected List<IFDRepresentation> rezipCache;

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
	private Map<String, IFDRepresentableObject<?>> htLocalizedNameToObject = new LinkedHashMap<>();

	/**
	 * working memory cache of representations
	 */
	protected Map<String, IFDRepresentation> cache;

	/**
	 * a list of properties that vendors have indicated need addition, keyed by the
	 * zip path for the resource
	 */
	private List<Object[]> propertyList;

	/**
	 * the URL to the original source of this data, as indicated in IFD-extract.json
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
	private IFDPropertyManagerI structurePropertyManager;

	/**
	 * produce no output other than a log file
	 */
	private boolean noOutput;

	private String localizedURL;

	private boolean haveExtracted;

	private String ifdid;

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
	public final String extractAndCreateFindingAid(File ifdExtractScriptFile, String localSourceDir, File targetDir,
			String findingAidFileNameRoot) throws IOException, IFDException {

		// first create objects, a List<String>

		getObjectParsersForFile(ifdExtractScriptFile);
		String puburi = null;
		Map<String, Object> pubCrossrefInfo = null;
		puburi = (String) helper.getFindingAid().getPropertyValue(IFDConst.IFD_PROP_FAIRDATA_COLLECTION_SOURCE_PUBLICATION_URI);
		if (puburi != null && !skipPubInfo) {
			pubCrossrefInfo = PubInfoExtractor.getPubInfo(puburi, addPublicationMetadata);
			if (pubCrossrefInfo == null || pubCrossrefInfo.get("title") == null) {
				if (skipPubInfo) {
					logWarn("skipPubInfo == true; Finding aid does not contain PubInfo", "extractAndCreateFindingAid");
				} else {
					if (!allowNoPubInfo) {
						logErr("Finding aid does not contain PubInfo! No internet? cannot continue", "extractAndCreateFindingAid");
						return null;
					}
					logWarn("Could not access " + PubInfoExtractor.getCrossrefMetadataUrl(puburi), "extractAndCreateFindingAid");
				}
			} else {
				List<Map<String, Object>> list = new ArrayList<>();
				list.add(pubCrossrefInfo);
				helper.getFindingAid().setPubInfo(list);
			}
		}
		setLocalSourceDir(localSourceDir);
		// options here to set cache and rezip options -- debugging only!
		setCachePattern(null);
		setRezipCachePattern(null, null);

		// now actually do the extraction.

		extractObjects(targetDir);

		System.out.println("Extractor.extractAndCreateFindingAid serializing...");
		return helper.createSerialization((noOutput && !createFindingAidsOnly ? null : targetDir),
				findingAidFileNameRoot, createZippedCollection ? products : null, getSerializer());
	}

	/**
	 * Implementing subclass could use a different serializer.
	 * 
	 * @return a serializer
	 */
	protected IFDSerializerI getSerializer() {
		return new IFDDefaultJSONSerializer();
	}

	public void setLocalSourceDir(String sourceDir) {
		if (sourceDir != null && sourceDir.indexOf("://") < 0)
			sourceDir = "file:///" + sourceDir;
		this.sourceDir = sourceDir;
	}

	///////// Vendor-related methods /////////

	/**
	 * Cache the property change created by an IFDVendorPluginI class. This method
	 * is callback from IFDVendorPluginI classes only.
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
	public IFDFAIRDataFindingAid getFindingAid() {
		return helper.getFindingAid();
	}

	/**
	 * Set the regex string assembing all vendor requests. Each vendor's pattern
	 * will be surrounded by (?<param0> ... ), (?<param1> ... ), etc. Here we wrap
	 * them all with (?<param>....), then add on our non-vender checks, and finally
	 * wrap all this using (?<type>...).
	 */
	public void setCachePattern(String sp) {
		if (sp == null)
			sp = IFDFAIRSpecExtractorHelper.defaultCachePattern + "|" + structurePropertyManager.getParamRegex();
		String s = "";
		for (int i = 0; i < IFDVendorPluginI.activeVendors.size(); i++) {
			String cp = IFDVendorPluginI.activeVendors.get(i).vcache;
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
		cache = new LinkedHashMap<String, IFDRepresentation>();
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
	private IFDPropertyManagerI getPropertyManager(Matcher m) {
		if (m.group("struc") != null)
			return structurePropertyManager;
		for (int i = bsPropertyVendors.nextSetBit(0); i >= 0; i = bsPropertyVendors.nextSetBit(i + 1)) {
			String ret = m.group("param" + i);
			if (ret != null && ret.length() > 0) {
				return IFDVendorPluginI.activeVendors.get(i).vendor;
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

		for (int i = 0; i < IFDVendorPluginI.activeVendors.size(); i++) {
			String cp = IFDVendorPluginI.activeVendors.get(i).vrezip;
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

	///////// PHASE 1: Reading the IFD-extract.json file ////////

	/**
	 * get a new structure property manager to handle processing of MOL, SDF, and
	 * CDX files, primarily. Can be overridden.
	 * 
	 * @return
	 */
	protected IFDPropertyManagerI getStructurePropertyManager() {
		return (structurePropertyManager == null
				? (structurePropertyManager = new IFDDefaultStructureHelper(this))
				: structurePropertyManager);
	}

	/**
	 * Get all {object} data from IFD-extract.json.
	 * 
	 * @param ifdExtractScript
	 * @return list of {objects}
	 * @throws IOException
	 * @throws IFDException
	 */
	public List<ObjectParser> getObjectParsersForFile(File ifdExtractScript) throws IOException, IFDException {
		log("! Extracting " + ifdExtractScript.getAbsolutePath());
		return getObjectsForStream(ifdExtractScript.toURI().toURL().openStream());
	}

	/**
	 * Get all {object} data from IFD-extract.json.
	 * 
	 * @param ifdExtractScript
	 * @return list of {objects}
	 * @throws IOException
	 * @throws IFDException
	 */
	public List<ObjectParser> getObjectsForStream(InputStream is) throws IOException, IFDException {
		extractScript = new String(IFDUtilities.getLimitedStreamBytes(is, -1, null, true, true));
		objectParsers = parseScript(extractScript);
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
	protected List<ObjectParser> parseScript(String script) throws IOException, IFDException {
		if (helper != null)
			throw new IFDException("Only one finding aid per instance of Extractor is allowed (for now).");

		helper = newExtractionHelper();

		Map<String, Object> jsonMap = (Map<String, Object>) new JSJSONParser().parse(script, false);
		if (debugging)
			log(jsonMap.toString());
		extractVersion = (String) jsonMap.get("IFD-extract-version");
		log(extractVersion);
		List<ObjectParser> objectParsers = getObjects((List<Map<String, Object>>) jsonMap.get("keys"));
		log(objectParsers.size() + " extractor regex strings");

		log("! license: " + helper.getFindingAid().getPropertyValue(IFDConst.IFD_PROP_FAIRDATA_COLLECTION_DATA_LICENSE_NAME) + " at "
				+ helper.getFindingAid().getPropertyValue(IFDConst.IFD_PROP_FAIRDATA_COLLECTION_DATA_LICENSE_URI));

		return objectParsers;
	}

	private IFDFAIRSpecExtractorHelper newExtractionHelper() throws IFDException {
		return new IFDFAIRSpecExtractorHelper(codeSource + " " + version);
		
	}

	/**
	 * Make all variable substitutions in IFD-extract.js.
	 * 
	 * @return list of ObjectParsers that have successfully parsed the {object}
	 *         lines of the file
	 * @throws IFDException
	 */
	protected List<ObjectParser> getObjects(List<Map<String, Object>> pathway) throws IFDException {

		// input:

//		{"IFD-extract-version":"0.1.0-alpha","keys":[
//         {"example":"compound directories containing unidentified bruker files and hrms zip file containing .pdf"},
//         {"journal":"acs.orglett"},{"hash":"0c00571"},
//         {"figshareid":"21975525"},
//         
//         {"ifdid=IFD.property.collection.id":"{journal}.{hash}"},
//         {"IFD.property.collection.source.publication.uri":"https://doi.org/10.1021/{ifdid}"},
//         {"IFD.property.collection.data.license.uri":"https://creativecommons.org/licenses/by-nc/4.0"},
//         {"IFD.property.collection.data.license.name":"cc-by-nc-4.0"},
//         
//         {"data0":"{IFD.property.collection.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/{ifdid}/suppl_file/ol{hash}_si_002.zip}"},
//         {"data":"{IFD.property.collection.source.data.uri::https://ndownloader.figshare.com/files/{figshareid}}"},
//
//         {"path":"{data}|FID for Publication/{id=IFD.property.structure.compound.id::*}.zip|"},
//         {"IFD.property.collection.object":"{path}{IFD.representation.spec.nmr.vendor.dataset::{IFD.property.spec.nmr.expt.label::<id>/{xpt=::*}}.zip|{xpt}/*/}"},
//         {"IFD.property.collection.object":"{path}<id>/{IFD.representation.structure.mol.2d::<id>.mol}"},
//         {"IFD.property.collection.object":"{path}{IFD.representation.spec.hrms.pdf::{IFD.property.spec.hrms.expt.label::<id>/HRMS.zip|**/*}.pdf}"}
//        ]}

		Lst<String> keys = new Lst<>();
		Lst<String> values = new Lst<>();
		List<ObjectParser> parsers = new ArrayList<>();
		for (int i = 0; i < pathway.size(); i++) {
			Map<String, Object> def = pathway.get(i);
			for (Entry<String, Object> e : def.entrySet()) {

				//{"IFDid=IFD.property.fairdata.collection.id":"{journal}.{hash}"},
				 //..-----------------key---------------------...------val-------.
				
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
				 //{"IFDid=IFD.property.fairdata.collection.id":"{journal}.{hash}"},
				 //..keydef.------------------key-------------

				if (key.equals(IFDConst.IFD_EXTRACTOR_OBJECT)) {
					parsers.add(newObjectParser(val));
					continue;
				}
				if (key.startsWith("IFD.property")) {
					if (key.equals(IFDConst.IFD_PROP_FAIRDATA_COLLECTION_ID)) {
						ifdid = val;
						helper.getFindingAid().setID(val);
					}
					helper.getFindingAid().setPropertyValue(key, val);
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
	public IFDFAIRSpecExtractorHelper extractObjects(File targetDir) throws IFDException, IOException {
		if (haveExtracted)
			throw new IFDException("Only one extraction per instance of Extractor is allowed (for now).");
		haveExtracted = true;
		if (targetDir == null)
			throw new IFDException("The target directory may not be null.");
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
				helper.getFindingAid().addOrReturnSource(dataSource);
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

			log("found " + ifdObjectCount + " IFD objects");

		}

		// update object vendor properties

		updateObjectProperties();

		// update object type and len records

		addCachedRepresentationsToObjects();

		removeDuplicateSpecData();
		removeUnmanifestedRepresentations();

		saveCollectionManifests(false);

		helper.finalizeExtraction();
		return helper;
	}

	private void removeDuplicateSpecData() {
		BitSet bs = new BitSet();
		IFDStructureDataAssociationCollection ssc = helper.getStructureDataCollection();
		boolean isFound = false;
		int n = 0;
		for (IFDAssociation assoc : ssc) {
			IFDCollection<IFDObject<?>> c = assoc.get(1);
			List<IFDDataObject> found = new ArrayList<>();
			for (IFDObject<?> spec : c) {
				if (bs.get(spec.getIndex())) {
					found.add((IFDDataObject) spec);
					log("! removing duplicate spec reference " + spec.getName() + " for "
							+ assoc.getFirstObj1().getName());
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
			n += helper.removeStructuresWithNoAssociations();
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
		for (IFDObject<?> spec : helper.getDataObjectCollection()) {
			List<IFDRepresentation> lstRepRemoved = new ArrayList<>();
			for (Object o : spec) {
				IFDRepresentation rep = (IFDRepresentation) o;
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
			int n = helper.removeStructuresWithNoAssociations();
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
	 * @throws IFDException
	 */
	private boolean parseZipFileNamesForObjects(ObjectParser parser) throws IOException, IFDException {
		boolean haveData = false;

		// first build the file list
		String key = localizedURL;
		Map<String, ZipEntry> zipFiles = IFDZipContents.get(key);
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
				IFDUtilities.getLimitedStreamBytes(url.openStream(), -1, new FileOutputStream(tempFile), true, true);
				log("! saved " + tempFile.length() + " bytes");
				len = tempFile.length();
				stream = new FileInputStream(tempFile);
			}
			helper.getFindingAid().setCurrentSourceLength(len);
			zipFiles = readZipContentsIteratively(stream, new LinkedHashMap<String, ZipEntry>(), "", false);
			IFDZipContents.put(key, zipFiles);
		}
		// next, we process those names

		for (String ifdPath : zipFiles.keySet()) {
			IFDObject<?> obj = addIFDObjectsForName(parser, ifdPath);
			if (obj != null) {
				System.out.println("Extractor.parseZip " + ifdPath);
				ifdObjectCount++;
				if (obj instanceof IFDDataObject || obj instanceof IFDAssociation)
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
	 * @throws IFDException
	 */
	protected ObjectParser newObjectParser(String sObj) throws IFDException {
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
			String ifdPath = basePath + zipEntry.getName();
			if (zipEntry.isDirectory()) {
				log("Phase " + phase + " checking zip directory: " + n + " " + ifdPath);
			} else if (!zipEntry.isDirectory() && zipEntry.getSize() == 0) {

				continue;
			}

			if (junkPattern.matcher(ifdPath).find()) {
				// Test 9: acs.orglett.0c01153/22284726,22284729 MACOSX,
				// acs.joc.0c00770/22567817
				addFileToFileLists(ifdPath, LOG_IGNORED, zipEntry.getSize());
				continue;
			}
			if (debugging)
				log("reading zip entry: " + n + " " + ifdPath);

			if (fileNames != null) {
				fileNames.put(ifdPath, zipEntry); // Java has no use for the ZipEntry, but JavaScript can read it.
			}
			if (ifdPath.endsWith(".zip")) {
				readZipContentsIteratively(zis, fileNames, ifdPath + "|", doRezip);
			} else if (doRezip) {
				nextEntry = processRezipEntry(basePath, ifdPath, zis, zipEntry);
			} else {
				processZipEntry(ifdPath, zis, zipEntry);
			}
		}
		if (isTopLevel)
			zis.close();
		return fileNames;
	}

	/**
	 * An important feature of Extractor is that it will repackage zip files,
	 * removing resources that are totally unnecessary and extracting properties
	 * using IFDVendorPluginI services.
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

	private Map<AWrap, IFDStructure> htStructureRepCache;

	int test = 0;
	byte[] b0, b1;

	/**
	 * Process the properties in propertyList after the IFDObject objects have een
	 * created for all resources.
	 * 
	 * @throws IFDException
	 * @throws IOException
	 */
	private void updateObjectProperties() throws IFDException, IOException {
		String lastLocal = null;
		IFDDataObject localSpec = null;
		IFDStructure struc = null;
		for (int i = 0, n = propertyList.size(); i < n; i++) {
			Object[] a = propertyList.get(i);
			String localizedName = (String) a[0];
			boolean isNew = !localizedName.equals(lastLocal);
			if (isNew) {
				lastLocal = localizedName;
			}
			String key = (String) a[1];
			Object value = a[2];
			boolean isStructure = (IFDFAIRSpecExtractorHelper.getObjectTypeForName(key, true) == IFDFAIRSpecExtractorHelper.ClassTypes.Structure);
			System.out.println("Extractor.updateObjectProperties " + key + " " + value);
			// link to the originating spec representation -- xxx.mnova, xxx.zip
			IFDRepresentableObject<?> spec = htLocalizedNameToObject.get(localizedName);
			if (spec == null) {
				logErr("manifest not found for " + localizedName, "addCachedRepresentation");
				continue;
			} else if (spec instanceof IFDStructure) {
				struc = (IFDStructure) spec;
				spec = null;
			} else if (isNew && spec instanceof IFDDataObject) {
				localSpec = (IFDDataObject) spec;
			}
			if (IFDConst.isRepresentation(key)) {
				// from reportVendor -- Bruker adds this for thumb.png and pdf files.
				String ifdPath = value.toString();
				addRepresentation(ifdPath, key, spec);
				continue;
			}
			if (key.equals(NEW_SPEC_KEY)) {
				// _page=10
				String idExtension = (String) value;
				IFDDataObject newSpec = helper.getDataObjectCollection().cloneData(localSpec, localSpec.getID() + idExtension);
				spec = newSpec;
				struc = helper.getFirstStructureForSpec(localSpec, true);
				if (struc != null) {					
					helper.associate(idExtension, struc, newSpec);
					log("!Structure " + struc + " found and associated with " + spec);
				} else {
					log("!SpecData " + spec + " added ");
				}
				IFDRepresentation rep = cache.get(localizedName);
				String ckey = localizedName + idExtension.replace('_', '#') + "\0" + localSpec.getID();
				cache.put(ckey, rep);
				htLocalizedNameToObject.put(localizedName, spec); // for internal use
				htLocalizedNameToObject.put(ckey, spec);
				continue;
			}
			if (key.startsWith(STRUC_FILE_DATA_KEY)) {
				Object[] oval = (Object[]) value;
				byte[] bytes = (byte[]) oval[0];
				String ifdPath = (String) oval[1];
				String ifdRepType = IFDDefaultStructureHelper.getType(key.substring(key.length() - 3), bytes);
				if (htStructureRepCache == null)
					htStructureRepCache = new HashMap<>();
				AWrap w = new AWrap(bytes);
				struc = htStructureRepCache.get(w);
				String name = getStructureNameFromPath(ifdPath);
				if (struc == null) {
					File f = getAbsoluteFileTarget(ifdPath);
					writeBytesToFile(bytes, f);
					String localName = localizePath(ifdPath);
					struc = helper.getFirstStructureForSpec(spec, false);
					if (struc == null) {
						struc = helper.addStructureForSpec(rootPath, (IFDDataObject) spec, ifdRepType, ifdPath,
								localName, name);
					}
					htStructureRepCache.put(w, struc);
					// MNova 1 page, 1 spec, 1 structure Test #5
					addFileAndCacheRepresentation(ifdPath, null, bytes.length, ifdRepType, null);
					linkLocalizedNameToObject(localName, ifdRepType, struc);
					log("!Structure " + struc + " created and associated with " + spec);
				} else if (helper.getAssociation(struc, (IFDDataObject) spec) == null) {
					helper.associate(name, struc, (IFDDataObject) spec);
					log("!Structure " + struc + " found and associated with " + spec);
				}
				continue;
			}
			if (isStructure) {
				if (struc == null) {
					logErr("No structure found for " + lastLocal + " " + key, "updateObjectProperies");
					continue; // already added?
				} else {
					struc.setPropertyValue(key, value);
				}
			} else {
				spec.setPropertyValue(key, value);
			}
		}
		propertyList.clear();
		htStructureRepCache = null;
	}

	

	public static String getStructureNameFromPath(String ifdPath) {
		String name = ifdPath.substring(ifdPath.lastIndexOf("/") + 1);
		name = name.substring(name.indexOf('#') + 1);
		int pt = name.indexOf('.');
		if (pt >= 0)
			name = name.substring(0, pt);
		return name;
	}
	
	private void addRepresentation(String ifdPath, String key, IFDRepresentableObject<?> spec) {
		String localizedName = localizePath(ifdPath);
		linkLocalizedNameToObject(localizedName, null, spec);
		setLocalFileLength(spec.addRepresentation(ifdPath, localizedName, key, key));
//		IFDRepresentation rep = 
//		rep.setSubtype(key);
	}


	/**
	 * Ensure that we have a correct length in the metadata for this representation. 
	 * as long as it exists, even if we are not writing it in this pass.
	 * @param rep
	 */
	private void setLocalFileLength(IFDRepresentation rep) {
		File f =getAbsoluteFileTarget(rep.getRef().getLocalName());
		long len = (f.exists() ? f.length() : 0);
		rep.setLength(len);
	}

	/**
	 * Set the type and len fields for structure and spec data
	 */
	private void addCachedRepresentationsToObjects() {

		for (String ckey : cache.keySet()) {
			IFDRepresentableObject<?> obj = htLocalizedNameToObject.get(ckey);
			if (obj == null) {
				logErr("manifest not found for " + ckey, "addCachedRepresentations");
			} else {
				copyCachedRepresentation(ckey, obj);
			}

		}
	}

	private void copyCachedRepresentation(String ckey, IFDRepresentableObject<?> obj) {
		IFDRepresentation r = cache.get(ckey);		
		String ifdPath = r.getRef().getOrigin().toString();
		String type = r.getType();
		String subtype = r.getSubtype();
		// suffix is just unique internal ID
		int pt = ckey.indexOf('\0');
		if (pt > 0)
			ckey = ckey.substring(0, pt);
		IFDRepresentation r1 = obj.addRepresentation(ifdPath, ckey, r.getType(), null);
		if (type != null)
			r1.setType(type);
		if (subtype != null)
			r1.setSubtype(subtype);
		r1.setLength(r.getLength());
	}

	/**
	 * Write the _IFD_manifest.json, _IFD_ignored.json and _IFD_extract.json files.
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
					logWarn("ignored " + lstIgnored.size() + " files", "saveCollectionManifests");
				}
			} else {
				writeBytesToFile(extractScript.getBytes(), getAbsoluteFileTarget("_IFD_extract.json"));
				outputListJSON(lstManifest, getAbsoluteFileTarget("_IFD_manifest.json"), "manifest");
				outputListJSON(lstIgnored, getAbsoluteFileTarget("_IFD_ignored.json"), "ignored");
			}
		}
		lstManifest.clear();
		lstIgnored.clear();
	}

	/**
	 * Output standardized JSON to the _IFD_* files.
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
		sb.append("{" + "\"IFD.fairdata.version\":\"" + IFDConst.IFD_VERSION + "\",\n");
		sb.append("\"IFD.extractor.version\":\"" + version + "\",\n")
				.append("\"IFD.extractor.code\":\"" + codeSource + "\",\n")
				.append("\"IFD.extractor.list.type\":\"" + type + "\",\n")
				.append("\"IFD.extractor.scirpt\":\"_IFD_extract.json\",\n")
				.append("\"IFD.extractor.source\":\"" + dataSource + "\",\n")
				.append("\"IFD.extractor.creation.date\":\"" + helper.getFindingAid().getDate().toGMTString() + "\",\n")
				.append("\"IFD.extractor.count\":" + lst.size() + ",\n").append("\"IFD.extractor.list\":\n" + "[\n");
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
	 * IFD-extract.json description. This will result in the formation of one or
	 * more IFDObjects -- an IFDAanalysis, IFDStructureSpecCollection,
	 * IFDDataObjectObject, or IFDStructure, for instance. But that will probably
	 * change.
	 * 
	 * The parser specifically looks for Matcher groups, regex (?<xxxx>...), that
	 * have been created by the ObjectParser from an object line such as:
	 * 
	 * {IFD.representation.spec.nmr.vendor.dataset::{IFD.property.structure.compound.id::*-*}-{IFD.property.spec.nmr.expt.label::*}.jdf}
	 *
	 * 
	 * 
	 * @param parser
	 * @param ifdPath
	 * @return one of IFDStructureSpec, IFDDataObject, IFDStructure, in that order,
	 *         depending upon availability
	 * 
	 * @throws IFDException
	 */
	private IFDObject<?> addIFDObjectsForName(ObjectParser parser, String ifdPath) throws IFDException {
		Matcher m = parser.p.matcher(ifdPath);
		if (!m.find())
			return null;
		helper.beginAddingObjects(ifdPath);
		if (debugging)
			log("adding IFDObjects for " + ifdPath);

		// If an IFDDataObject object is added, then it will also be added to
		// htManifestNameToSpecData

		for (String key : parser.keys.keySet()) {
			String param = parser.keys.get(key);
			if (param.length() > 0) {
				String id = m.group(key);
				final String localizedName = localizePath(ifdPath);
				IFDRepresentableObject<?> obj = helper.addObject(rootPath, param, id, localizedName);				
				linkLocalizedNameToObject(localizedName, param, obj);
				if (debugging)
					log("found " + param + " " + id);
				;
			}
		}
		return helper.endAddingObjects();
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
	private void linkLocalizedNameToObject(String localizedName, String type, IFDRepresentableObject<?> obj) {
		if (type == null || IFDConst.isRepresentation(type)) {
			htLocalizedNameToObject.put(localizedName, obj);
		}
	}

	protected void logWarn(String msg, String method) {
		msg = "!! Extractor." + method + " "  
				+ ifdid + " " + rootPath + " WARN: " + msg;
		log(msg);
		errorLog += msg + "\n";
	}

	protected void logErr(String msg, String method) {
		msg = "!! Extractor." + method + " "  
				+ ifdid + " " + rootPath + " ERR: " + msg;
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
		if (IFDUtilities.logStream != null) {
			try {
				IFDUtilities.logStream.write((msg + "\n").getBytes());
			} catch (IOException e) {
			}
		}
		System.out.flush();
		System.err.flush();
		if (isErr) {
			System.err.println(msg);
		} else if (isAlert) {
			System.out.println(msg);
		}
		System.out.flush();
		System.err.flush();
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
	 * zip contents caching can save time in complex loading.
	 * 
	 */
	public static void clearZipCache() {
		IFDZipContents.clear();
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
	 * @param ifdPath
	 * @param zis
	 * @param entry
	 * @return next (unrelated) entry
	 * @throws IOException
	 */
	private ZipEntry processRezipEntry(String baseName, String ifdPath, ZipInputStream zis, ZipEntry entry)
			throws IOException {
		if (!ifdPath.equals(currentRezipPath)) {
			String localizedName = localizePath(ifdPath);
			if (!entry.isDirectory() && !lstIgnored.contains(ifdPath) && !lstManifest.contains(localizedName)) {
				addFileToFileLists(ifdPath, LOG_IGNORED, entry.getSize());
				logWarn("ignoring " + ifdPath, "processRezipEntry");
			}
			return null;
		}
		IFDVendorPluginI vendor = currentRezipVendor;
		String dirName = entry.getName();
		log("! rezipping " + ifdPath + " for " + entry + " " + new File(entry.getName()).getName());
		File outFile = getAbsoluteFileTarget(ifdPath + ".zip");
		final String localizedName = localizePath(ifdPath);
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

			IFDPropertyManagerI mgr = null;
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
				IFDUtilities.getLimitedStreamBytes(zis, len, os, false, false);
				if (doCheck) {
					byte[] bytes = ((ByteArrayOutputStream) os).toByteArray();
					if (doInclude)
						zos.write(bytes);
					this.localizedName = localizedName;
					if (mgr == null || mgr == vendor) {
						vendor.accept(null, ifdPath + outName, bytes);
					} else {
						mgr.accept(this, ifdPath + outName, bytes);
					}
				}
				if (doInclude)
					zos.closeEntry();
			}
		}
		vendor.endRezip();
		zos.close();
		fos.close();
		String dataType = vendor.processRepresentation(ifdPath + ".zip", null);
		len = (noOutput ? ((ByteArrayOutputStream) fos).size() : outFile.length());
		IFDRepresentation r = helper.getSpecDataRepresentation(ifdPath);
		if (r == null) {
			System.out.println("!Extractor.processRezipEntry rep not found for " + ifdPath);
			// could be just structure at this point
		} else {
			r.setLength(len);
		}
		cacheFileRepresentation(ifdPath, localizedName, len, dataType, "application/zip");
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
			currentRezipVendor = (IFDVendorPluginI) currentRezipRepresentation.getData();
		}
	}

	/**
	 * Check to see what should be done with a zip entry. We can extract it or
	 * ignore it; and we can check it to sees if it is the trigger for extracting a
	 * zip file in a second pass.
	 * 
	 * @param ifdPath  path to this entry including | and / but not rootPath
	 * @param zis
	 * @param zipEntry
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	protected void processZipEntry(String ifdPath, InputStream zis, ZipEntry zipEntry)
			throws FileNotFoundException, IOException {
		long len = zipEntry.getSize();
		Matcher m;

		// check for files that should be pulled out - these might be JDX files, for
		// example.
		// "param" appears if a vendor has flagged these files for parameter extraction.

		if (cachePattern != null && (m = cachePattern.matcher(ifdPath)).find()) {
			IFDPropertyManagerI v = getPropertyManager(m);
			boolean doCheck = (v != null);
			boolean doExtract = (!doCheck || v.doExtract(ifdPath));

			System.out.println("!Extractor.processZipEntry caching " + ifdPath);
//			1. we don't have params 
//		      - generic file, just save it.  doExtract and not doCheck
//			2. we have params and there is extraction
//		      - save file and also check it for parameters  doExtract and doCheck
//			3. we have params but no extraction  !doCheck  and !doExtract
//		      - ignore completely

			File f = getAbsoluteFileTarget(ifdPath);
			OutputStream os = (!doExtract ? null
					: doCheck || noOutput ? new ByteArrayOutputStream() : new FileOutputStream(f));
			if (os != null)
				IFDUtilities.getLimitedStreamBytes(zis, len, os, false, true);
			String localizedName = localizePath(ifdPath);
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
						type = v.accept(this, ifdPath, bytes);
						this.localizedName = oldLocal;
					}
				} else {
					len = f.length();
				}
				addFileAndCacheRepresentation(ifdPath, localizedName, len, type, m.group("type"));
			}
		}

		// here we look for the "trigger" file within a zip file that indicates that we
		// (may) have a certain vendor's files that need looking into. The case in point
		// is finding a procs file within a Bruker data set. Or, in principle, an acqus
		// file and just an FID but no pdata/ directory. But for now we want to see that
		// processed data.

		if (rezipCachePattern != null && (m = rezipCachePattern.matcher(ifdPath)).find()) {

			// e.g. exptno/./pdata/procs

			IFDVendorPluginI v = getVendorForRezip(m);
			ifdPath = m.group("path" + v.getIndex());
			if (ifdPath.equals(lastRezipPath)) {
				log("duplicate path " + ifdPath);
			} else {
				lastRezipPath = ifdPath;
				IFDRepresentation ref = new CacheRepresentation(
						new IFDReference(ifdPath, localizePath(ifdPath), rootPath), v, len, null, "application/zip");
				rezipCache.add(ref);
				log("rezip pattern found " + ifdPath);
			}
		}

	}

	/**
	 * 
	 * @param ifdPath
	 * @param localizedName        ifdPath with localized / and |
	 * @param len
	 * @param ifdType              IFD.representation....
	 * @param fileNameForMediaType
	 * @return
	 */
	private IFDRepresentation addFileAndCacheRepresentation(String ifdPath, String localizedName, long len,
			String ifdType, String fileNameForMediaType) {
		if (localizedName == null)
			localizedName = localizePath(ifdPath);
		if (fileNameForMediaType == null)
			fileNameForMediaType = localizedName;
		addFileToFileLists(localizedName, LOG_OUTPUT, len);
		String subtype = IFDDefaultStructureHelper.mediaTypeFromName(fileNameForMediaType);
		return cacheFileRepresentation(ifdPath, localizedName, len, ifdType, subtype);
	}

	/**
	 * Find the matching pattern for rezipN where N is the vendor index in
	 * activeVendors. Presumably there will be only one vendor per match. (Two
	 * vendors will not be looking for MOL files, for example.)
	 * 
	 * @param m
	 * @return
	 */
	private IFDVendorPluginI getVendorForRezip(Matcher m) {
		for (int i = bsRezipVendors.nextSetBit(0); i >= 0; i = bsRezipVendors.nextSetBit(i + 1)) {
			String ret = m.group("rezip" + i);
			if (ret != null && ret.length() > 0) {
				return IFDVendorPluginI.activeVendors.get(i).vendor;
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
	 * @param ifdPath       slash-based reference to object
	 * @param localizedName ifdPath with / and | removed.
	 * @param len
	 * @param type
	 * @param subtype       a media type, typically
	 * 
	 * @return
	 */
	private IFDRepresentation cacheFileRepresentation(String ifdPath, String localizedName, long len, String type,
			String subtype) {
		if (subtype == null)
			subtype = IFDDefaultStructureHelper.mediaTypeFromName(localizedName);
		CacheRepresentation rep = new CacheRepresentation(new IFDReference(ifdPath, localizedName, rootPath), null, len,
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
	 * Add a record for _IFD_manifest.json or _IFD_ignored.json
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
			IFDUtilities.writeBytesToFile(bytes, f);
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

		private String dataSource;

		/**
		 * @param sObj
		 * @throws IFDException
		 */
		public ObjectParser(String sObj) throws IFDException {
			int[] pt = new int[1];
			dataSource = getIFDExtractValue(sObj, IFDConst.IFD_PROP_FAIRDATA_COLLECTION_SOURCE_DATA_URI, pt);
			if (dataSource == null)
				throw new IFDException(
						"No {" + IFDConst.IFD_PROP_FAIRDATA_COLLECTION_SOURCE_DATA_URI + "::...} found in " + sObj);
			sData = sObj.substring(pt[0] + 1); // skip first "|"
			init();
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
			// *-* becomes \\E([^-]+(?:-[^-]+)*)\\Q and matches a-b-c
			//
			// * becomes \\E.+\\Q
			//
			// {id=IFD.property.spec.nmr.expt.label::xxx} becomes \\E(?<id>\\Qxxx\\E)\\Q
			//
			// {IFD.property.spec.nmr.expt.label::xxx} becomes
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
			// {IFD.property.spec.nmr.expt.label::*} becomes \\E(?<IFD0nmr0param0expt>.+)\\Q
			//
			// {IFD.representation.spec.nmr.vendor.dataset::{IFD.property.structure.compound.id::*-*}-{IFD.property.spec.nmr.expt.label::*}.jdf}
			//
			// becomes:
			//
			// ^(?<IFD0nmr0representation0vendor0dataset>(?<IFD0structure0param0compound0id>([^-](?:-[^-]+)*))\\Q-\\E(?<IFD0nmr0param0expt>.+)\\Q.jdf\\E)$
			//
			// {id=IFD.property.structure.compound.id::*}.zip|{IFD.representation.spec.nmr.vendor.dataset::{id}_{IFD.property.spec.nmr.expt.label::*}/}
			//
			// becomes:
			//
			// ^(?<id>*)\\Q.zip|\\E(?<IFD0nmr0representation0vendor0dataset>\\k<id>\\Q_\\E(<IFD0nmr0param0expt>*)\\Q/\\E)$

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

			// {id=IFD.property.spec.nmr.expt.label::xxx} becomes \\E(?<id>\\Qxxx\\E)\\Q
			// {IFD.property.spec.nmr.expt.label::xxx} becomes
			// \\E(?<IFD0nmr0param0expt>\\Qxxx\\E)\\Q
			// <id> becomes \\k<id>

			s = compileIFDDefs(s, true, true);

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
		 * Find and regex-ify all {id=IFD.param::value} or {IFD.param::value}.
		 * 
		 * @param s growing regex string
		 * @return regex string with all {...} fixed
		 * @throws IFDException
		 */
		protected String compileIFDDefs(String s, boolean isFull, boolean replaceK) throws IFDException {
			while (s.indexOf("::") >= 0) {
				Matcher m = objectDefPattern.matcher(s);
				if (!m.find())
					return s;
				String param = m.group(1);
				String val = m.group(2);
				String pv = "{" + param + "::" + val + "}";
				if (val.indexOf("::") >= 0)
					val = compileIFDDefs(val, false, replaceK);
				int pt = param.indexOf("=");
				if (pt == 0)
					throw new IFDException("bad {def=key::val} expression: " + param + "::" + val);
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

	}

}
