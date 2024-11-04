package com.integratedgraphics.extractor;

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
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.iupac.fairdata.api.IFDSerializerI;
import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecCompoundAssociation;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecCompoundCollection;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecExtractorHelper;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecExtractorHelper.FileList;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecExtractorHelperI;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecFindingAidHelper;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecFindingAidHelperI;
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

import com.integratedgraphics.extractor.ExtractorAids.AWrap;
import com.integratedgraphics.extractor.ExtractorAids.ArchiveEntry;
import com.integratedgraphics.extractor.ExtractorAids.ArchiveInputStream;
import com.integratedgraphics.extractor.ExtractorAids.CacheRepresentation;
import com.integratedgraphics.extractor.ExtractorAids.DirectoryInputStream;
import com.integratedgraphics.extractor.ExtractorAids.ExtractorResource;
import com.integratedgraphics.extractor.ExtractorAids.ObjectParser;
import com.integratedgraphics.ifd.api.VendorPluginI;

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
public class MetadataExtractor extends FindingAidCreator implements ExtractorI {

	// TODO: test rootpath and file lists for case with two root paths -- does it
	// make sense that that manifests are cleared?

	// TODO: update GitHub README.md

	protected static final String version = "0.0.6-alpha+2024.11.03";

	// 2024.11.03 version 0.0.6 adding support for DoiCrawler
	// 2024.05.28 version 0.0.5 moved to com.integratedgraphics.extractor.Extractor
	// 2023.01.09 version 0.0.4 adds MNova_Page_Header parameter
	// 2023.01.07 version 0.0.4 adds CDX reading by Jmol
	// 2023.01.01 version 0.0.4 accepts structures automatically from ./structures/
	// and ./structures.zip
	// 2022.12.30 version 0.0.4 ACS 0-7 with structures; fixing rezip issue of
	// Bruker files placed in _IFD.ignored.json
	// 2022.12.29 version 0.0.4 ACS 0-4 with structures; fixing *-* Regex for ACS#4
	// acs.orglett.0c00788
	// 2022.12.27 version 0.0.4 ACS 0-2 working
	// 2022.12.27 version 0.0.4 introduces FAIRSpecCompoundAssociation
	// 2022.12.23 version 0.0.4 fixes from ACS testing, Bruker directories with
	// multiple numbered subdirectories adds "-<n>" to the id
	// 2022.12.14 version 0.0.4 allows for local directory parsing (no zip or
	// tar.gz)
	// 2022.12.13 verison 0.0.4 adds "EXIT" and comment-only "..." for
	// IFD-extract.json
	// 2022.12.10 version 0.0.4 adds CDXML reading by Jmol and conversion of CIF to
	// PNG along with Jmol 15.2.82 fixes for V3000 and XmlChemDrawReader
	// 2022.12.01 version 0.0.4 fixes multi-page MNova with compound association
	// (ACS 22567817#./extract/acs.joc.0c00770)
	// 2022.11.29 version 0.0.4 allows for a representation to be both a structure
	// and a data object
	// 2022.11.27 version 0.0.4 adds parameters from a Metadata file as XLSX or ODS
	// 2022.11.23 version 0.0.3 fixes missing properties in NMR; upgrades to
	// double-precision Jmol-SwingJS JmolDataD.jar
	// 2022.11.21 version 0.0.3 fixes minor details; ICL.v6, ACS.0, ACS.5 working
	// adds command-line arguments, distinguishes REJECTED and IGNORED
	// 2022.11.17 version 0.0.3 allows associations "byID"
	// 2022.11.14 version 0.0.3 "compound identifier" as organizing association
	// 2022.06.09 MNovaMetadataReader CDX export fails due to buffer pointer error.

	protected class ParserIterator implements Iterator<ObjectParser> {

		boolean byResource;
		int i;

		ParserIterator() {
			extractorResource = null;
			thisRootPath = "";
		}

		@Override
		public boolean hasNext() {
			return (i < objectParsers.size() && !doneLocally());
		}

		private boolean doneLocally() {
			if (localSourceDir != null) {
//				return false;
				ObjectParser parser = objectParsers.get(i);
				boolean done =
						!parser.getDataSource().isLocalStructures &&
						(parser.getDataSource().getSourceFile() != objectParsers.get(0).getDataSource().getSourceFile());
				return done;
			}

			return false;
		}

		@Override
		public ObjectParser next() {
			ObjectParser parser = objectParsers.get(i++);
			if (parser.getDataSource() != extractorResource) {
				try {
					phase2InitializeResource(parser.getDataSource(), true);
				} catch (IFDException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return parser;
		}

	}

	/**
	 * This
	 */
	protected final static String SUBST = "=>";

	protected static final String codeSource = "https://github.com/IUPAC/IUPAC-FAIRSpec/blob/main/src/main/java/com/integratedgraphics/ifd/extractor/Extractor.java";

	protected static final int LOG_REJECTED = 0;
	protected static final int LOG_IGNORED = 1;
	protected static final int LOG_OUTPUT = 2;
	protected static final int LOG_ACCEPTED = 3;

	protected final static int PHASE_2A = 1;
	protected final static int PHASE_2B = 2;
	protected final static int PHASE_2C = 3;
	protected final static int PHASE_2D = 4;

	/**
	 * a key for the deferredObjectList that flags a structure with a spectrum;
	 * needs attention, as this was created prior to the idea of a compound
	 * association, and it presumes there are no such associations.
	 * 
	 */
	public static final String NEW_PAGE_KEY = "*NEW_PAGE*";

	/**
	 * a key for the deferredObjectList that indicates we have a new resource
	 * setting
	 */
	public static final String NEW_RESOURCE_KEY = "*NEW_RESOURCE*";

	protected final static Pattern objectDefPattern = Pattern.compile("\\{([^:]+)::([^}]+)\\}");
	protected final static Pattern pStarDotStar = Pattern.compile("\\*([^|/])\\*");

	/**
	 * value to substitute for null from vendors
	 */
	public static final Object NULL = "\1";

	/**
	 * "." here is the Eclipse project extract/ directory
	 * 
	 */
	protected static final String DEFAULT_STRUCTURE_DIR_URI = "./structures/";
	protected static final String DEFAULT_STRUCTURE_ZIP_URI = "./structures.zip";

	Map<String, Object> config = null;

	/**
	 * the finding aid helper - only one per instance
	 */
	private FAIRSpecExtractorHelperI helper;

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
	 * list of files to always accept, specified an extractor JSON template
	 */
	protected FileList lstAccepted = new FileList(null, "accepted");

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
	protected ExtractorResource extractorResource;

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
	protected DefaultStructureHelper structurePropertyManager;

	protected Map<AWrap, IFDStructure> htStructureRepCache;

	protected File currentZipFile;

	protected Map<String, Map<String, Object>> htMetadata;

	protected File extractScriptFile;
	
	protected String extractScriptFileDir;

	protected String userStructureFilePattern;

	protected Map<String, ExtractorResource> htResources = new HashMap<>();

	/**
	 * Slows this down a bit, but allows, for example, a CIF file to be both a
	 * structure and an object
	 */
	protected boolean allowMultipleObjectsForRepresentations = true;

	protected String ignoreRegex, acceptRegex;

	protected List<FileList> rootLists;
	protected String resourceList;

	protected String ifdMetadataFileName = "IFD_METADATA"; // default only

	private String localSourceFile;

	public int getErrorCount() {
		return errors;
	}

	public MetadataExtractor() {
		setConfiguration();
		setDefaultRunParams();
		getStructurePropertyManager();
	}

	protected void setConfiguration() {
		try {
			config = FAIRSpecUtilities.getJSONResource(MetadataExtractor.class, "extractor.config.json");
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
		phase1SetLocalSourceDir(localArchive);
		extractScriptFile = ifdExtractScriptFile;
		extractScriptFileDir = extractScriptFile.getParent();
		phase1GetObjectParsersForFile(ifdExtractScriptFile);
		if (!phase1ProcessPubURI())
			return false;
		// options here to set cache and rezip options -- debugging only!
		phase1SetCachePattern(userStructureFilePattern);
		rezipCachePattern = phase1SetRezipCachePattern(null, null);
		return true;
	}

	protected void phase1SetMetadataTarget(String key, String param) {
		Map<String, Object> pm = htMetadata.remove(key);
		if (pm == null)
			return;
		// switch key to object id key
		log("!Extractor METADATA FOR " + key + " set to " + param);
		htMetadata.put(param, pm);
		loadMetadata(param, pm);
	}

	protected boolean phase1ProcessPubURI() throws IOException {
		String datadoi = (String) helper.getFindingAid()
				.getPropertyValue(IFDConst.IFD_PROPERTY_COLLECTIONSET_SOURCE_DATA_DOI);
		if (datadoi == null)
			datadoi = (String) helper.getFindingAid()
					.getPropertyValue(IFDConst.IFD_PROPERTY_COLLECTIONSET_SOURCE_DATA_URI);
		String pubdoi = (String) helper.getFindingAid()
				.getPropertyValue(IFDConst.IFD_PROPERTY_COLLECTIONSET_SOURCE_PUBLICATION_DOI);
		if (pubdoi == null)
			pubdoi = (String) helper.getFindingAid()
					.getPropertyValue(IFDConst.IFD_PROPERTY_COLLECTIONSET_SOURCE_PUBLICATION_URI);
		return processDOIURLs(pubdoi, datadoi, helper);
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
		if (m == null)
			return null;
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
		extractScript = FAIRSpecUtilities.getFileStringData(ifdExtractScript);
		return objectParsers = phase1ParseScript(extractScript);
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
		return new FAIRSpecExtractorHelper((ExtractorI) this, getCodeSource() + " " + getVersion());
	}

	/**
	 * Make all variable substitutions in IFD-extract.js.
	 * 
	 * @return list of ObjectParsers that have successfully parsed the {object}
	 *         lines of the file
	 * @throws IFDException
	 * @throws IOException 
	 * @throws MalformedURLException 
	 */
	@SuppressWarnings("unchecked")
	protected List<ObjectParser> phase1GetObjectParsers(List<Object> jsonArray) throws IFDException, MalformedURLException, IOException {

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
		List<Object> accepted = new ArrayList<>();
		ExtractorResource source = null;
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

				if (key.equals(FAIRSpecExtractorHelper.FAIRSPEC_EXTRACTOR_ACCEPT)) {
					if (o instanceof String) {
						accepted.add(o);
					} else {
						accepted.addAll((List<Object>) o);
					}
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

				if (key.equals(FAIRSpecExtractorHelper.FAIRSPEC_EXTRACTOR_REFERENCES)) {
					val = FAIRSpecUtilities.getFileStringData(new File(toAbsolutePath(val)));
					log("!processing " + val);
					Map<String, Map<String, Object>> htCompoundFileReferences = new HashMap<>();
					List<Object> jsonMap = (List<Object>) new JSJSONParser().parse(val, false);
					for (int j = jsonMap.size(); --j >= 0;) {
						Map<String, Object> jm = (Map<String, Object>) jsonMap.get(j);
						String file = (String) jm.get("file");
						String cmpd = (String) jm.get("cmpd");
						if (file == null) {
							file = cmpd;
							log("!" + jm.toString());
						}
						htCompoundFileReferences.put(file, jm);
					}
					helper.setCompoundRefMap(htCompoundFileReferences);
				}
				
				if (key.equals(FAIRSpecExtractorHelper.FAIRSPEC_EXTRACTOR_LOCAL_SOURCE_FILE)) {
					localSourceFile = (val.length() == 0 ? null : val);
					continue;
				}
				if (key.equals(IFDConst.IFD_PROPERTY_COLLECTIONSET_SOURCE_DATA_URI)) {
					// allow for a local version (debugging mostly)
					boolean isRemote = val.startsWith("http");
					boolean isRelative = val.startsWith("./"); // as in "./structures"
					if (isRelative)
						localSourceFile = null;
					boolean isFoundLocal = phase1CheckLocalSource(val);
					if (!isFoundLocal) {
						source = null;
						isDefaultStructurePath = isDefaultStructurePath(val);
						String msg = "local source directory does not exist (ignored): " + val;
						if (isDefaultStructurePath)
							logNote(msg, "phase1CheckSource");
						else
							logWarn(msg, "phase1CheckSource");
						if (isDefaultStructurePath)
							continue;
					}
					source = htResources.get(val);

					if (source == null)
						htResources.put(val,
								source = new ExtractorResource(localSourceFile == null || isRelative ? val : null));
					source.localSourceFile = (localSourceFile == null ? null : localizeURL(null, localSourceFile));
					if (isFoundLocal || !isRemote)
						continue;
					// go ahead and add this data source to the metadata
				}

				if (key.equals(FAIRSpecExtractorHelper.FAIRSPEC_EXTRACTOR_OBJECT)) {
					if (source == null) {
						if (isDefaultStructurePath)
							continue;
						throw new IFDException(
								IFDConst.IFD_PROPERTY_COLLECTIONSET_SOURCE_DATA_URI + " was not set before " + val);
					}
					ObjectParser parser = newObjectParser(source, val);
					parser.setReplacements(replacements);
					parsers.add(parser);
					continue;
				}
				if (key.equals(FAIRSpecExtractorHelper.FAIRSPEC_EXTRACTOR_RELATED_METADATA)) {
					ifdMetadataFileName = val;
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
		lstRejected.setAcceptPattern(FAIRSpecUtilities.rep(s, ".", "\\.") + FAIRSpecExtractorHelper.junkFilePattern);
		if (accepted.size() > 0) {
			s = "";
			for (int i = 0; i < accepted.size(); i++) {
				s += "|(" + accepted.get(i) + ")";
			}
			acceptRegex = FAIRSpecUtilities.rep(s.substring(1), ".", "\\.");
			lstAccepted.setAcceptPattern(acceptRegex);
		} else {
			acceptRegex = null;
		}
		if (ignored.size() > 0) {
			s = "";
			for (int i = 0; i < ignored.size(); i++) {
				s += "|(" + ignored.get(i) + ")";
			}
			ignoreRegex = FAIRSpecUtilities.rep(s.substring(1), ".", "\\.");
		} else {
			ignoreRegex = null;
		}
		return parsers;
	}

	static boolean isDefaultStructurePath(String val) {
		return (DEFAULT_STRUCTURE_DIR_URI.equals(val) || DEFAULT_STRUCTURE_ZIP_URI.equals(val));
	}

	protected boolean phase1CheckLocalSource(String val) throws IFDException {
		val = localizeURL(val, localSourceFile);
		System.out.println("checking for source " + val);
		return (!val.startsWith("file:/") || new File(val.substring(6)).exists());
	}

	@SuppressWarnings("unchecked")
	protected void phase1ProcessMetadataElement(Object m) throws IFDException {
		Map<String, Object> map = (Map<String, Object>) m;
		String key = (String) map.get("FOR");
		if (key == null) {
			throw new IFDException("extractor template METADATA element does not contain 'FOR' key in " + m);
		}
		if (htMetadata == null)
			htMetadata = new HashMap<String, Map<String, Object>>();
		htMetadata.put(key, map);
		if (key.startsWith("IFD."))
			loadMetadata(key, map);
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
		// -- set up the rezipCache for vendors that need to do that (Bruker, multiple
		// <n>/)

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

			// Parse the file path, creating association, structure, sample, and spectrum
			// objects.
			// This phase produces the deferredPropertyList, which is processed after
			// all the parsing is done, because sometimes the object is not recognized
			// until a key file (Bruker procs, for example, is found).

			log("!Phase 2b \n" + localizedTopLevelZipURL + "\n" + parser.getStringData());

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
				if (parser.hasData())
					phase2ReadZipContentsIteratively(getTopZipStream(), "", PHASE_2C, null);
			}
		}

		// Vendors may produce new objects that need association or properties of those
		// objects. This happens in Phases 2a, 2b, and 2c

		phase2cProcessDeferredObjectProperties(null);

		// Phase 2d

		log("!Phase 2d read zip contents");

		iter = new ParserIterator();
		while (iter.hasNext()) {
			iter.next();
			phase2ReadZipContentsIteratively(getTopZipStream(), "", PHASE_2D, null);
		}
	}

	protected Map<String, Map<String, ArchiveEntry>> phase2aInitializeZipData() throws IOException, IFDException {

		Map<String, Map<String, ArchiveEntry>> contents = new LinkedHashMap<>();

		// Scan through parsers for resource changes

		ParserIterator iter = new ParserIterator();
		while (iter.hasNext()) {
			ExtractorResource currentSource = extractorResource;
			iter.next();
			if (extractorResource != currentSource) {
				currentSource = extractorResource;
				if (cleanCollectionDir) {
					log("!cleaning directory " + extractorResource.rootPath);
					FileUtils.cleanDirectory(new File(targetDir + "/" + extractorResource.rootPath));
				}
				String source = targetDir + "/" + extractorResource.rootPath;
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

	protected void phase2InitializeResource(ExtractorResource resource, boolean isInit)
			throws IFDException, IOException {
		// localize the URL if we are using a local copy of a remote resource.
		localizedTopLevelZipURL = localizeURL(resource.source, resource.localSourceFile);
		String s = resource.getSourceFile();
		String zipPath = s.substring(s.lastIndexOf(":") + 1);
		File zipFile = new File(zipPath);

		if (isInit) {

			if (debugging)
				log("opening " + localizedTopLevelZipURL);

			String rootPath = zipFile.getName();
			if (rootPath.endsWith(".zip") || rootPath.endsWith(".tgz") || rootPath.endsWith(".rar")
					|| rootPath.endsWith(".tar"))
				rootPath = rootPath.substring(0, rootPath.length() - 4);
			else if (rootPath.endsWith(".tar.gz"))
				rootPath = rootPath.substring(0, rootPath.length() - 7);

			File rootDir = new File(targetDir + "/" + rootPath);
			rootDir.mkdir();
			// open a new log
			resource.rootPath = rootPath;
			resource.setLists(rootPath, ignoreRegex, acceptRegex);

		}
		lstManifest = resource.lstManifest;
		lstIgnored = resource.lstIgnored;
		if (helper.getCurrentSource() != helper.addOrSetSource(resource.getSourceFile(), resource.rootPath)) {
			if (isInit)
				addDeferredPropertyOrRepresentation(NEW_RESOURCE_KEY, resource, false, null, null);
		}
		extractorResource = resource;
		thisRootPath = resource.rootPath;
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
		return (localizedTopLevelZipURL.endsWith("/") ? new DirectoryInputStream(localizedTopLevelZipURL)
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
	protected void phase2bParseZipFileNamesForObjects(ObjectParser parser, Map<String, ArchiveEntry> zipFileMap)
			throws IOException, IFDException {
		// next, we process those names
		for (Entry<String, ArchiveEntry> e : zipFileMap.entrySet()) {
			String originPath = e.getKey();
			String localizedName = localizePath(originPath);
			// Generally we allow a representation (cif for example) to be
			// linked to multiple objects. I can't think of reason not to allow this.
			if (!allowMultipleObjectsForRepresentations && htLocalizedNameToObject.containsKey(localizedName))
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
	protected IFDObject<?> phase2bAddIFDObjectsForName(ObjectParser parser, String originPath, String localizedName,
			long len) throws IFDException, IOException {

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
		for (String key : parser.getKeys().keySet()) {
			keys.add(key);
		}
		for (int i = keys.size(); --i >= 0;) {
			String key = keys.get(i);
			String param = parser.getKeys().get(key);
			if (param.length() > 0) {
				String id = m.group(key);
				log("!found " + param + " " + id);

				if (IFDConst.isDataObject(param)) {
					String s = IFDConst.IFD_DATAOBJECT_FLAG + localizedName;
					if (htLocalizedNameToObject.containsKey(s))
						continue;
				}
				IFDObject<?> obj = helper.addObject(extractorResource.rootPath, param, id, localizedName, len);
				if (obj instanceof IFDRepresentableObject) {
					linkLocalizedNameToObject(localizedName, param, (IFDRepresentableObject<?>) obj);
					if (obj instanceof IFDDataObject)
						parser.setHasData(true);
				} else if (obj instanceof FAIRSpecCompoundAssociation) {
					// this did not work, because we don't really know what is the defining
					// characteristic
					// of an association in terms of path.
					// processDeferredObjectProperties(originPath, (IFDStructureDataAssociation)
					// obj);
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
	protected ObjectParser newObjectParser(ExtractorResource source, String sObj) throws IFDException {
		return new ObjectParser(this, source, sObj);
	}

	/**
	 * Process all entries in a zip file, looking for files to extract and
	 * directories to rezip. This method is called at different phases in the
	 * extraction.
	 * 
	 * 
	 * @param is               the InputStream
	 * @param baseOriginPath   a path ending in "zip|"
	 * @param phase
	 * @param originToEntryMap a map to return of name to ZipEntry; may be null
	 * 
	 * @return originToEntryMap
	 * @throws IOException
	 * @throws IFDException
	 */
	protected Map<String, ArchiveEntry> phase2ReadZipContentsIteratively(InputStream is, String baseOriginPath,
			int phase, Map<String, ArchiveEntry> originToEntryMap) throws IOException, IFDException {
		if (debugging && baseOriginPath.length() > 0)
			log("! opening " + baseOriginPath);
		boolean isTopLevel = (baseOriginPath.length() == 0);
		ArchiveInputStream ais = (isTopLevel ? getArchiveInputStream(is) : new ArchiveInputStream(is, null));
		ArchiveEntry zipEntry = null;
		ArchiveEntry nextEntry = null;
		ArchiveEntry nextRealEntry = null;
		int n = 0;
		boolean first = (phase != PHASE_2D);
		int pt;
		while ((zipEntry = (nextEntry != null ? nextEntry
				: nextRealEntry != null ? nextRealEntry : ais.getNextEntry())) != null) {
			n++;
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
			boolean accepted = false;
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
				if (phase == PHASE_2A && lstAccepted.accept(oPath)) {
					addFileToFileLists(oPath, LOG_ACCEPTED, zipEntry.getSize(), null);
					accepted = true;
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
						phase2aProcessEntry(baseOriginPath, oPath, ais, zipEntry, accepted);
					break;
				case PHASE_2C:
					// rezipping
					if (oPath.equals(currentRezipPath)) {
						// nextRealEntry here may be the first
						// file of the zip, because not all zip files
						// list directories. Some do, but many do not.
						// tar.gz files definitely do not. So in that case,
						// we must pass nextRealEntry as the first entry to write
						// to the rezipper.
						nextEntry = phase2cRezipEntry(baseOriginPath, oPath, ais, zipEntry, nextRealEntry,
								currentRezipVendor);
						nextRealEntry = null;
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

	protected ArchiveInputStream getArchiveInputStream(InputStream is) throws IOException {
		return new ArchiveInputStream(is, extractorResource.getSourceFile());
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
	protected void phase2aProcessEntry(String baseOriginPath, String originPath, InputStream ais, ArchiveEntry zipEntry,
			boolean accept) throws FileNotFoundException, IOException {
		long len = zipEntry.getSize();
		Matcher m = null;

		// check for files that should be pulled out - these might be JDX files, for
		// example.
		// "param" appears if a vendor has flagged these files for parameter extraction.

		boolean isFound = false;
		if (vendorCachePattern != null && (isFound = (m = vendorCachePattern.matcher(originPath)).find()) || accept) {

			PropertyManagerI v = (isFound ? getPropertyManager(m) : null);
			boolean doCheck = (v != null);
			boolean doExtract = (!doCheck || v.doExtract(originPath));

//			1. we don't have params 
//		      - generic file, just save it.  doExtract and not doCheck
//			2. we have params and there is extraction
//		      - save file and also check it for parameters  doExtract and doCheck
//			3. we have params but no extraction  !doCheck  and !doExtract
//		      - ignore completely

			if (doExtract) {
				String ext = (isFound ? m.group("ext") : originPath.substring(originPath.lastIndexOf(".") + 1));
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
						type = v.accept(this, originPath, bytes, true);
						if (type == IFDConst.IFD_PROPERTY_FLAG) {
							List<String[]> props = FAIRSpecUtilities.getIFDPropertyMap(new String(bytes));
							for (int i = 0, n = props.size(); i < n; i++) {
								String[] p = props.get(i);
								addDeferredPropertyOrRepresentation(p[0].substring(3), p[1], false, null, null);
							}
						} else {
							deferredPropertyList.add(null);
							this.localizedName = oldLocal;
							this.originPath = oldOriginPath;
							if (type == null) {
								logWarn("Failed to read " + originPath + " (ignored)", v.getClass().getName());
							} else if (IFDConst.isStructure(type)
									|| type.startsWith(DefaultStructureHelper.STRUC_FILE_DATA_KEY)) {
								return;
							}
						}
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
				CacheRepresentation ref = new CacheRepresentation(new IFDReference(helper.getCurrentSource().getID(),
						originPath, extractorResource.rootPath, localPath), v, len, null, "application/zip");
				// if this is a zip file, the data object will have been set to xxx.zip
				// but we need this to be
				String basePath = (baseOriginPath.endsWith("|")
						? baseOriginPath.substring(0, baseOriginPath.length() - 1)
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
	 * For testing (or for whatever reason zip files are local or should not use the
	 * given source paths), replace https://......./ with sourceDir/
	 * 
	 * @param sUrl
	 * @return localized URL
	 * @throws IFDException
	 */
	protected String localizeURL(String sUrl, String localSourceFile) throws IFDException {
		if (localSourceDir != null) {
			if (isZip(localSourceDir)) {
				sUrl = localSourceDir;
			} else if (localSourceDir.endsWith("/*")) {
				sUrl = localSourceDir.substring(0, localSourceDir.length() - 1);
			} else if (sUrl == null) {
//				if (new File(localSourceFile).isAbsolute())
					sUrl = localSourceFile;
//				else
//					sUrl = localSourceDir + "/" + localSourceFile;
			} else if (!sUrl.startsWith("./")) {
				int pt = sUrl.lastIndexOf("/");
				if (pt >= 0) {
					sUrl = localSourceDir + sUrl.substring(pt);
					if (!isZip(sUrl) && !sUrl.endsWith("/"))
						sUrl += ".zip";
				}
			}

		}
		sUrl = toAbsolutePath(sUrl);

		if (sUrl.indexOf("//") < 0 && !sUrl.startsWith("file:/"))
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
			Object path = (currentRezipRepresentation = rezipCache.remove(0)).getRef().getOriginPath();
			currentRezipPath = (String) path;
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
	 * @param ais
	 * @param entry
	 * @return firstEntry (if the first entry was read in order to start this zip
	 *         operation.
	 * @throws IOException
	 * @throws IFDException
	 */
	protected ArchiveEntry phase2cRezipEntry(String baseName, String oPath, ArchiveInputStream ais, ArchiveEntry entry,
			ArchiveEntry firstEntry, VendorPluginI vendor) throws IOException, IFDException {

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
		String basePath = baseName + (parent == null ? "" : parent);
		if (newDir == null) {
			newDir = "";
			boolean isMultiple = currentRezipRepresentation.isMultiple();
			if (!isMultiple)
				oPath = (parent == null ? basePath.substring(0, basePath.length() - 1) : basePath);
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
			if (this.localizedName == null || isMultiple)
				this.localizedName = localizedName;
			if (isMultiple) {
				addDeferredPropertyOrRepresentation(NEW_PAGE_KEY, new Object[] {
						(obj.getID().endsWith("/" + thisDir) ? null : "_" + thisDir), obj, localizedName }, false, null,
						null);
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
			addProperty(IFDConst.IFD_PROPERTY_DATAOBJECT_NOTE, msg);
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
		while ((entry = (firstEntry == null ? ais.getNextEntry() : firstEntry)) != null) {
			firstEntry = null;
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
			boolean isInlineBytes = false;
			boolean isBytesOnly = false;
			boolean isIFDMetadataFile = entryName.endsWith("/" + ifdMetadataFileName);
			Object[] typeData = (isIFDMetadataFile ? null : vendor.getExtractTypeInfo(this, baseName, entryName));
			// return here is [String type, Boolean] where Boolean.TRUE means include bytes
			// and Boolean.FALSE means don't include file
			if (typeData != null) {
				isInlineBytes = Boolean.TRUE.equals(typeData[1]);
				isBytesOnly = Boolean.FALSE.equals(typeData[1]);
			}
			boolean doInclude = (isIFDMetadataFile || vendor == null
					|| vendor.doRezipInclude(this, baseName, entryName));
			// cache this one? -- could be a different vendor -- JDX inside Bruker;
			// false for MNova within Bruker? TODO: But wouldn't that possibly have
			// structures?
			// directory, for example
			boolean doCache = (!isIFDMetadataFile && vendorCachePattern != null
					&& (m = vendorCachePattern.matcher(entryName)).find() && phase2cGetParamName(m) != null
					&& ((mgr = getPropertyManager(m)) == null || mgr.doExtract(entryName)));
			boolean doCheck = (doCache || mgr != null || isIFDMetadataFile);

			len = entry.getSize();
			if (len == 0 || !doInclude && !doCheck)
				continue;
			OutputStream os = (doCheck || isInlineBytes ? new ByteArrayOutputStream() : zos);
			String outName = newDir + entryName.substring(lenOffset);
			if (doInclude)
				zos.putNextEntry(new ZipEntry(outName));
			FAIRSpecUtilities.getLimitedStreamBytes(ais.getStream(), len, os, false, false);
			byte[] bytes = (doCheck || isInlineBytes ? ((ByteArrayOutputStream) os).toByteArray() : null);
			if (doCheck) {
				if (doInclude)
					zos.write(bytes);
				// have this already; multiple will duplicate the numbered directory
				// this.originPath = oPath + outName;
				this.localizedName = localizedName;
				if (isIFDMetadataFile) {
					addIFDMetadata(new String(bytes));
				} else {
					(mgr == null ? vendor : mgr).accept(null, this.originPath, bytes, false);
				}
			}
			if (doInclude)
				zos.closeEntry();
			if (typeData != null) {
				String key = (String) typeData[0];
				typeData[0] = (isBytesOnly ? null : localName);
				typeData[1] = bytes;
				// extract this file into the collection
				addDeferredPropertyOrRepresentation(key, (isBytesOnly || isInlineBytes ? typeData : localName),
						isInlineBytes, null, null);
			}
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
		if (createZippedCollection && rootPaths != null) {
			products.add(new File(targetDir + "/_IFD_extract.json"));
			products.add(new File(targetDir + "/_IFD_ignored.json"));
			products.add(new File(targetDir + "/_IFD_manifest.json"));
		}
		long[] times = new long[3];
		String serializedFindingAid = helper.createSerialization((readOnly && !createFindingAidOnly ? null : targetDir),
				findingAidFileNameRoot, createZippedCollection ? products : null, ser, times);
		log("!Extractor serialization done " + times[0] + " " + times[1] + " " + times[2] + " ms "
				+ serializedFindingAid.length() + " bytes");
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
				String path = r.getRef().getOriginPath().toString();
				logDigitalItem(path, ckey, "addCachedRepresentationsToObjects");
				try {
					addFileToFileLists(path, LOG_IGNORED, r.getLength(), null);
				} catch (IOException e) {
					// not possible
				}
				continue;
			}
			String originPath = r.getRef().getOriginPath().toString();
			String type = r.getType();
			// type will be null for pdf, for example
			String mediatype = r.getMediaType();
			// suffix is just unique internal ID for the cache
			int pt = ckey.indexOf('\0');
			String localName = (pt > 0 ? ckey.substring(0, pt) : ckey);
			IFDRepresentation r1 = obj.findOrAddRepresentation(r.getRef().getResourceID(), originPath,
					r.getRef().getlocalDir(), localName, null, type, null);
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
					// zip file reference in extact.json could actually reference only an extracted
					// PDF
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
		return name.endsWith(".zip") || name.endsWith(".tgz") || name.endsWith(".tar") || name.endsWith(".rar")
				|| name.endsWith("tar.gz");
	}

	@Override
	public void addProperty(String key, Object val) {
		if (val != NULL)
			log(this.localizedName + " addProperty " + key + "=" + val);
		addDeferredPropertyOrRepresentation(key, val, false, null, null);
	}

	/**
	 * Add metadata from a simple file (default IFD_METADATA) with key=value pairs
	 * one per line.
	 * 
	 * @param data
	 */
	protected void addIFDMetadata(String data) {
		List<String[]> list = FAIRSpecUtilities.getIFDPropertyMap(data);
		for (int i = 0, n = list.size(); i < n; i++) {
			String[] item = list.get(i);
			addProperty(item[0], item[1]);
		}
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
	public void addDeferredPropertyOrRepresentation(String key, Object val, boolean isInline, String mediaType,
			String note) {
		// System.out.println("!!!" + key + " ln=" + localizedName + " op=" +
		// originPath);
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
			Object[] oval = (Object[]) val;
			byte[] bytes = (byte[]) oval[0];
			String name = (String) oval[1]; // must not be null
			String type = (oval.length > 2 ? (String) oval[2] : null);
			String standardInchi = (oval.length > 3 ? (String) oval[3] : null);
			getStructurePropertyManager().processRepresentation(name, bytes, type, standardInchi, false);
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
		IFDRepresentableObject<?> spec = null;
		IFDStructure struc = null;
		IFDSample sample = null;
		IFDDataObject dataObject = null;
		boolean cloning = false;
		for (int i = 0, n = deferredPropertyList.size(); i < n; i++) {
			Object[] a = deferredPropertyList.get(i);
			if (a == null) {
				sample = null;
				dataObject = null; // use localSpec
				continue;
			}
			assoc = null;
			String originPath = (String) a[0];
			String localizedName = (String) a[1];
			String key = (String) a[2];
			Object value = a[3];

//System.out.println("!!!" + key + "   ln=" + localizedName + "    op=" + originPath);

			boolean isInline = (a[4] == Boolean.TRUE);
			if (key == NEW_RESOURCE_KEY) {
				phase2InitializeResource((ExtractorResource) value, false);
				continue;
			}
			cloning = key.equals(NEW_PAGE_KEY);
			boolean isRep = IFDConst.isRepresentation(key);
			String type = FAIRSpecExtractorHelper.getObjectTypeForPropertyOrRepresentationKey(key, true);
			boolean isSample = (type == FAIRSpecFindingAidHelper.ClassTypes.Sample);
			boolean isStructure = (type == FAIRSpecFindingAidHelper.ClassTypes.Structure);
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
			spec = getObjectFromLocalizedName(localizedName, propType);
			if (spec != null && !spec.isValid())
				spec = getClonedData(spec);

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
				String label = (struc == null ? null : struc.getLabel());
				if (label != null && label.startsWith("Structure_"))
					struc = null;
			}
			if (isRep) {
				// from reportVendor-- Bruker adds this for thumb.png and pdf files.
				Object data = null;
				String mediaType = (String) a[5];
				String note = (String) a[6];
				if (!isInline && value instanceof Object[]) {
					// from DefaultStructureHelper - a PNG version of a CIF file, for example.
					Object[] oval = (Object[]) value;
					byte[] bytes = (byte[]) oval[0];
					String oPath = (String) oval[1];
					String localName = localizePath(oPath);
					writeOriginToCollection(oPath, bytes, 0);
					addFileToFileLists(localName, LOG_OUTPUT, bytes.length, null);
					value = localName;
					if ("image/png".equals(mediaType)) {
						data = bytes;
					}
				}
				String keyPath = null;
				if (isInline) {
					if (value instanceof Object[]) {
						keyPath = ((Object[]) value)[0].toString();
						data = ((Object[]) value)[1];
					} else {
						data = value;
					}

				} else {
					keyPath = value.toString();
				}
				// note --- not allowing for AnalysisObject or Sample here
				boolean isStructureKey = IFDConst.isStructure(key);
				if (isStructureKey && struc == null) {
					struc = helper.addStructureForSpec(extractorResource.rootPath, (IFDDataObject) spec, key,
							originPath, localizePath(keyPath), null);
				}
				IFDRepresentableObject<?> obj = (isStructureKey ? struc : getClonedData(spec));
				linkLocalizedNameToObject(keyPath, null, obj);
				IFDRepresentation r = obj.findOrAddRepresentation(helper.getCurrentSource().getID(), originPath,
						extractorResource.rootPath, keyPath, data, key, mediaType);
				if (note != null)
					r.setNote(note);
				if (!isInline)
					setLocalFileLength(r);
				continue;
			}

			// properties only
			if (cloning) {
				String newLocalName = null;
				boolean clearNew = false;
				if (value instanceof String) {
					clearNew = false;
					// clear structure for not allowing an MNova structure to carry to next page?
//					struc = null; 
					if (dataObject == null) {
						dataObject = localSpec;
					} else {
						localSpec = dataObject;
					}
					// e.g. MNova extracted _page=10
				} else {
					// e.g. Bruker created a new object from multiple <n>/ directories
					a = (Object[]) value;
					value = (String) a[0];
					IFDDataObject sp = (IFDDataObject) a[1];
					if (sp.isValid()) {
						clearNew = true;
						localSpec = sp;
						newLocalName = (String) a[2];
					} else {
						localSpec = null;
					}
				}
				String idExtension = (String) value;
				if (assoc == null && localSpec != null)
					assoc = helper.findCompound(null, localSpec);
				System.out.println("cloning for association " + assoc);
				IFDDataObject newSpec;
				if (localSpec == null) {
					newSpec = (IFDDataObject) spec;
				} else {
					newSpec = helper.cloneData(localSpec, idExtension, true);
					mapClonedData(localSpec, newSpec);
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
				String ifdRepType = (oval.length > 2 ? (String) oval[2]
						: DefaultStructureHelper.getType(key.substring(key.lastIndexOf(".") + 1), bytes, true));
				String name = (oval.length > 2 ? null : getStructureNameFromPath(oPath));
				String inchi = (oval.length > 3 ? (String) oval[3] : null);
				// use the byte[] for the structure as a unique identifier.
				if (htStructureRepCache == null)
					htStructureRepCache = new HashMap<>();
				AWrap w = new AWrap(inchi == null || inchi.length() < 2 ? bytes : inchi.getBytes());
				struc = htStructureRepCache.get(w);
				// System.out.println("EXT " + name + " " + w.hashCode() + " " + oPath);
				if (struc == null) {
					writeOriginToCollection(oPath, bytes, 0);
					String localName = localizePath(oPath);
					struc = helper.getFirstStructureForSpec((IFDDataObject) spec, false);
					if (struc == null) {
						struc = helper.addStructureForSpec(extractorResource.rootPath, (IFDDataObject) spec, ifdRepType,
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
				// spec?
				if (key.equals(FAIRSpecExtractorHelper.DATAOBJECT_ORIGINATING_SAMPLE_ID)) {
					helper.addSpecOriginatingSampleRef(extractorResource.rootPath, localSpec, (String) value);
				}
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

	private Map<String, IFDRepresentableObject<?>> htCloneMap = new HashMap<>();

	public void mapClonedData(IFDRepresentableObject<?> oldSpec, IFDRepresentableObject<?> newSpec) {
		htCloneMap.put(oldSpec.getID(), newSpec);
	}

	public IFDRepresentableObject<?> getClonedData(IFDRepresentableObject<?> spec) {
		IFDRepresentableObject<?> d = (spec.isValid() ? null : htCloneMap.get(spec.getID()));
		return (d == null ? spec : d);
	}

	protected IFDRepresentableObject<?> getObjectFromLocalizedName(String name, String type) {
		IFDRepresentableObject<?> obj = (type == null ? null : htLocalizedNameToObject.get(type + name));
		return (obj == null ? htLocalizedNameToObject.get(name) : obj);
	}

	protected void setPropertyIfNotAlreadySet(IFDObject<?> obj, String key, Object value, String originPath) {
		boolean isNull = (value == NULL);
		if (IFDConst.isProperty(key)) {
			// not a parameter and not forcing NULL
			Object v = obj.getPropertyValue(key);
			if (value.equals(v))
				return;
			if (v != null && !isNull) {
				String source = obj.getPropertySource(key);
				logWarn(originPath + " property " + key + " can't set value '" + value + "', as it is already set to '"
						+ v + "' from " + source + " for " + obj, "setPropertyIfNotAlreadySet");
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
	 * Note that manifest and ignored will be in the archive folder(s) comprising
	 * the collection.
	 * 
	 * @param isOpen if true, starting -- just clear the lists; if false, write the
	 *               files
	 * @throws IOException
	 */
	protected void writeRootManifests() throws IOException {
		resourceList = "";
		rootLists = new ArrayList<>();
		for (ExtractorResource r : htResources.values()) {
			resourceList += ";" + r.getSourceFile();
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
		String json = helper.getFileListJSON(name, rootLists, resourceList, extractScriptFile.getName(), ret);
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
	protected void linkLocalizedNameToObject(String localizedName, String type, IFDRepresentableObject<?> obj)
			throws IOException {
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
			fname = extractScriptFileDir.replace('\\', '/') + fname.substring(1);
		return fname;
	}

	/**
	 * get a new structure property manager to handle processing of MOL, SDF, and
	 * CDX files, primarily. Can be overridden.
	 * 
	 * @return
	 */
	protected DefaultStructureHelper getStructurePropertyManager() {
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
		CacheRepresentation rep = new CacheRepresentation(new IFDReference(helper.getCurrentSource().getID(),
				originPath, extractorResource.rootPath, localizedName), null, len, ifdType, mediaType);
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
		return new File(targetDir + "/" + extractorResource.rootPath + "/" + localizePath(originPath));
	}

	/**
	 * Clean up the zip entry name to remove '|', '/', ' ', and add ".zip" if there
	 * is a trailing '/' in the name.
	 * 
	 * @param path
	 * @return
	 */
	protected static String localizePath(String path) {
		if (path.indexOf("structures") >= 0)
			System.out.println("???");
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
		case LOG_ACCEPTED:
			// fileName will be an origin name
			lstAccepted.add(fileName, len);
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
	 * Transfer an ignored file to the collection as a digital item, provided the
	 * includeIgnoredFiles flag is set.
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
		MetadataExtractor extractor = null;
		String flags = null;
		String targetDir0 = targetDir;
		for (int i = i0; i <= i1; i++) {
			extractor = new MetadataExtractor();
			extractor.logToSys("Extractor.runExtraction output to " + new File(targetDir).getAbsolutePath());
			String job = null;
			// ./extract/ should be in the main Eclipse project directory.
			String extractInfo = null;
			if (ifdExtractJSONFilename == null) {

//				"./extract/acs.joc.0c00770/IFD-extract.json#22567817",  // 0 727 files; zips of bruker dirs + mnovas

				job = extractInfo = testSet[i];
				extractor.logToSys("Extractor.runExtraction " + i + " " + job);
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
				json += "\"" + targetDir + "/IFD.findingaid.json\"";
			}
			long t0 = System.currentTimeMillis();

			extractor.testID = i;

			extractor.processFlags(args);
			new File(targetDir).mkdirs();
			flags = "\n first = " + first + " last = " + last + "\n"//
					+ extractor.dumpFlags() + "\n createFindingAidJSONList = " + createFindingAidJSONList //
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
				extractor.logErr("Exception " + e + " " + i, "runExtraction");
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
				warnings += "======== " + i + ": " + extractor.warnings + " warnings for " + targetDir + "\n"
						+ extractor.strWarnings;
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
						String s = FAIRSpecUtilities.rep(json, targetDir0 + "/", "./");
						File f = new File(targetDir0 + "/_IFD_findingaids.json");
						FAIRSpecUtilities.writeBytesToFile(s.getBytes(), f);
						extractor.logToSys("Extractor.runExtraction File " + f.getAbsolutePath() + " created ");
						f = new File(targetDir0 + "/_IFD_findingaids.js");
						FAIRSpecUtilities.writeBytesToFile(("FindingAids=" + s).getBytes(), f);
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

	protected static String getCommandLineHelp() {
		return "\nformat: java -jar IFDExtractor.jar [IFD-extract.json] [sourceArchive] [targetDir] [flags]" //
				+ "\n" + "\nwhere" //
				+ "\n" //
				+ "\n[IFD-extract.json] is the IFD extraction template for this collection" //
				+ "\n[sourceArchive] is the source .zip, .tar.gz, .tar, .tgz, or .rar file" //
				+ "\n[targetDir] is the target directory for the collection (which you are responsible to empty first)" //
				+ "\n" //
				+ "\n" + "[flags] are one or more of:" //
				+ "\n" //
				+ "\n-addPublicationMetadata (only for post-publication-related collections; include ALL Crossref or DataCite metadata)" //
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
		String s = " stopOnAnyFailure = " + stopOnAnyFailure //
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

	@Override
	protected FAIRSpecFindingAidHelperI getHelper() {
		return helper;
	}

}
