package com.integratedgraphics.extractor;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecExtractorHelper;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecFindingAid;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecFindingAidHelperI;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;
import org.iupac.fairdata.core.IFDFindingAid;
import org.iupac.fairdata.core.IFDObject;
import org.iupac.fairdata.extract.DefaultStructureHelper;
import org.iupac.fairdata.extract.MetadataReceiverI;
import org.iupac.fairdata.extract.PropertyManagerI;

import com.integratedgraphics.extractor.ExtractorUtils.ArchiveEntry;
import com.integratedgraphics.extractor.ExtractorUtils.ArchiveInputStream;
import com.integratedgraphics.html.PageCreator;
import com.integratedgraphics.ifd.api.VendorPluginI;

/**
 * This abstract class backs MetadataExtractor and DOICrawler,
 * both of which create finding aids. 
 * 
 * @author hansonr@stolaf.edu
 *
 */
public abstract class FindingAidCreator implements MetadataReceiverI {

	public static final String version = "0.0.7-beta+2025.02.17";
	// 2025.02.17 version 0.0.7-beta+2025.02.17 integrates the crawler
	// 2024.12.02 version 0.0.6 fully refactored, revised; adds creation of landing
	// page and -nolandingpage -nolaunch flags
	// 2024.11.03 version 0.0.6 adding support for DOICrawler
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

	static {
		FAIRSpecFindingAid.loadProperties();
		VendorPluginI.init();
	}

	
	/**
	 * start-up option to create JSON list for multiple
	 */
	public boolean stopOnAnyFailure;
	public boolean debugReadOnly;

	public boolean createLandingPage = true;
	public boolean launchLandingPage = true;


	protected boolean debugging = false;
	public boolean readOnly = false;

	protected boolean isByID = true; // forcing

	protected boolean isByIDSet;

	/**
	 * set true to only create finding aides, not extract file data
	 */
	protected boolean createFindingAidOnly = false;

	/**
	 * set true to allow failure to create pub info
	 */
	protected boolean allowNoPubInfo = false;

	/**
	 * don't even try to read pub info -- debugging
	 */
	protected boolean skipPubInfo = false;

	/**
	 * set to true add the source metadata from Crossref or DataCite
	 */
	protected boolean addPublicationMetadata = false;

	/**
	 * setting insitu true generates an entirely self-contained finding aid, with
	 * no local files at all, only origin files, without any rezipping, 
	 * in the origin directory. The target directory only contains ancillary files.
	 */
	protected boolean insitu = false;

	/**
	 * set true to zip up the extracted collection, placing that in the target
	 * directory
	 */
	protected boolean createZippedCollection = true;

	/**
	 * produce no output other than a log file
	 */
	protected boolean noOutput;

	/**
	 * include ignored files in FAIRSpec collection
	 */

	protected boolean includeIgnoredFiles = true;

	
	protected FAIRSpecFindingAidHelperI faHelper;

	protected FAIRSpecFindingAidHelperI getHelper() {
		return faHelper;
	}


	/**
	 * the structure property manager for this extractor
	 * 
	 */
	private DefaultStructureHelper structurePropertyManager;

	protected String localizedTopLevelZipURL;

	protected boolean haveExtracted;

	protected String errorLog = "";

	public int testID = -1;
	
	protected String thisRootPath;

	protected File targetPath;

	public void setTargetPath(File targetPath) {
		this.targetPath = targetPath;
	}

	public String strWarnings = "";

	public int warnings;

	public int getWarningCount() {
		return warnings;
	}

	public int errors;


	protected boolean dataciteUp = true;

	protected boolean cleanCollectionDir = true;

	public boolean assetsOnly;

	protected String ifdid = "";

	protected void setDefaultRunParams() {
		// normally false:

		System.out.flush();
		debugReadOnly = false; // quick settings - no file creation

		addPublicationMetadata = false; // true to place full Crossref or DataCite metadata into the finding aid

		
		// normally true:

		launchLandingPage = true;
		
		createLandingPage = true;
		
		cleanCollectionDir = true;

		stopOnAnyFailure = true; // set false to allow continuing after an error.

		debugging = false; // true for verbose listing of all files
		createFindingAidOnly = false; // true if extraction files already exist or you otherwise don't want not write

		allowNoPubInfo = true;// debugReadOnly; // true to allow no internet connection and so no pub calls

		setDerivedFlags();

	}

	protected void setDerivedFlags() {

		// this next is independent of readOnly
		// false to bypass final creation of an
		// _IFD_collection.zip file
		createZippedCollection = createZippedCollection && !insitu && !debugReadOnly;
		
		cleanCollectionDir &= !insitu;
		readOnly |= debugReadOnly; // for testing; when true, no output other than a log file is produced
		noOutput = (createFindingAidOnly || readOnly);
		skipPubInfo = !dataciteUp || debugReadOnly; // true to allow no internet connection and so no pub calls
		
		createLandingPage &= !readOnly;
		launchLandingPage &= createLandingPage && !assetsOnly;

	}

	/**
	 * Just convert all args[0..n] to a string concatenation of "-" args[i] ";".
	 * Some of the early flags may not be actual flags, but that does not matter.
	 * 
	 * @param args
	 * @param moreFlags flags that will override anything in args[]
	 * @return
	 */
	public String processFlags(String[] args, String moreFlags) {
		if (moreFlags == null)
			moreFlags = "";
		moreFlags += ";";
		for (int i = 0; i < args.length; i++) {
			if (args[i] != null && args[i].startsWith("-")) {
				moreFlags += args[i] + ";";				
				args[i] = null;
			}
		}
		moreFlags = checkFlags(moreFlags);
		setDerivedFlags();
		return moreFlags;
	}

	protected String checkFlags(String flags) {
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

		if (flags.indexOf("-nolandingpage;") >= 0) {
			createLandingPage = false;
		}

		if (flags.indexOf("-nolaunch;") >= 0) {
			launchLandingPage = false;
		}

		if (flags.indexOf("-nozip;") >= 0) {
			createZippedCollection = false;
		}

		if (flags.indexOf("-insitu;") >= 0) {
			insitu = true;
		}

		if (flags.indexOf("-readonly;") >= 0) {
			readOnly = true;
		}
		if (flags.indexOf("-requirepubinfo;") >= 0) {
			allowNoPubInfo = false;
		}
		if (flags.indexOf("-assetsonly;") >= 0) {
			assetsOnly = true;
		}

// not working 
//		int pt = flags.indexOf("-structurepattern="); 
//		if (pt >= 0) {
//			userStructureFilePattern = flags.substring(flags.indexOf("=", pt) + 1, flags.indexOf(";", pt));
//		}
		
		return flags;
	}

	protected static String getFlagEquals(String flags, String flag) {
		int pt = flags.indexOf(flag + "=");
		if (pt < 0)
			return null;
		String val = flags.substring(pt + flag.length() + 1);
		return val.substring(0, val.indexOf(";"));
	}


	/**
	 * Set options from command-line, IFD-extract.json, and extractor.config.json.
	 * 
	 * Note that setting isByID is only allowed once. Thus:
	 * 
	 * - extractor.config.json overrides built-in defaults - IFD-extract.json
	 * overrides extractor.config.json
	 * 
	 * - command line overrides IFD-extract.json
	 * 
	 * @param key
	 * @param val
	 */
	protected void setExtractorOption(String key, String val) {
		if (!isByIDSet && key.equals(IFDConst.IFD_PROPERTY_COLLECTIONSET_BYID)) {
			isByID = val.equalsIgnoreCase("true");
			isByIDSet = true;
			getHelper().setById(isByID);
		} else {
			checkFlags(val);
		}
	}

	/**
	 * @param pubdoi
	 * @param datadoi
	 * @param helper
	 * @return
	 * @throws IOException
	 */
	protected boolean processDOIURLs(String pubdoi, String datadoi, FAIRSpecFindingAidHelperI helper) throws IOException {
		if (skipPubInfo)
			return true;
		List<Map<String, Object>> list = new ArrayList<>();
		
		if (pubdoi != null) {
			String err = helper.addRelatedInfo(pubdoi, addPublicationMetadata, list, DOIInfoExtractor.CROSSREF);
			if (err != null) {
				logWarn(err, "processDOIURLs");
				if (!allowNoPubInfo)
					return false;				
			}
		}
		if (datadoi != null && !skipPubInfo) {
			String err = helper.addRelatedInfo(datadoi, addPublicationMetadata, list, 
					DOIInfoExtractor.DATACITE);
			if (err != null) {
				logWarn(err, "processDOIURLs");
				if (!allowNoPubInfo)
					return false;
			}
		}
		return true;
	}

	protected boolean processPubURI() throws IOException {
		String datadoi = (String) faHelper.getFindingAid()
				.getPropertyValue(IFDConst.IFD_PROPERTY_COLLECTIONSET_SOURCE_DATA_DOI);
		if (datadoi == null)
			datadoi = (String) faHelper.getFindingAid()
					.getPropertyValue(IFDConst.IFD_PROPERTY_COLLECTIONSET_SOURCE_DATA_URI);
		String pubdoi = (String) faHelper.getFindingAid()
				.getPropertyValue(IFDConst.IFD_PROPERTY_COLLECTIONSET_SOURCE_PUBLICATION_DOI);
		if (pubdoi == null)
			pubdoi = (String) faHelper.getFindingAid()
					.getPropertyValue(IFDConst.IFD_PROPERTY_COLLECTIONSET_SOURCE_PUBLICATION_URI);
		return processDOIURLs(pubdoi, datadoi, faHelper);
	}

	/**
	 * Indicate that a local path is being include in the 
	 * collection but doesn't appear in the finding aid.
	 * 
	 * From Phase 2c and Phase 3
	 * 
	 * Not 100% clear why these are happening.
	 * 
	 * @param localPath
	 * @param method
	 */
	protected void logDigitalItemIgnored(String originPath, String localPath, String why, String method) {
		logWarn("digital item ignored, because " + why + ": " + originPath, method);
	}

	protected void logNote(String msg, String method) {
		msg = "!NOTE: " + msg + " -- Extractor." + method + " " + ifdid + " "
				+ thisRootPath;
		log(msg);
	}

	protected void logWarn(String msg, String method) {
		msg = "! WARNING: " + msg + " -- Extractor." + method + " " + ifdid + " "
				+ thisRootPath;
		log(msg);
	}

	public void logErr(String msg, String method) {
		msg = "!! ERROR: " + msg + " -- Extractor." + method + " " + ifdid + " "
				+ thisRootPath;
		log(msg);
	}

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

	public void logToSys(String msg) {
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

	@Override
	public IFDFindingAid getFindingAid() {
		return getHelper().getFindingAid();
	}

	protected void buildSite(File htmlPath) {
		try {
			PageCreator.buildSite(htmlPath, true, launchLandingPage);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void setNewObjectMetadata(IFDObject<?> o, String param) {
		// not used 
	}

	
	//////// methods for extraction of metadata by vendor plugins
	
	/**
	 * bitset of activeVendors that are set for property parsing
	 * 
	 * set in phase 1; used in phase 2a and 2c
	 */
	protected BitSet bsPropertyVendors = new BitSet();

	/**
	 * bitset of activeVendors that are set for rezipping
	 * 
	 * set in phase 1; used in phase 2a
	 */
	protected BitSet bsRezipVendors = new BitSet();

	/**
	 * vendors have supplied cacheRegex patterns
	 * 
	 * set in phase 1; used in phase 2c
	 */
	protected boolean cachePatternHasVendors;

	/**
	 * vendors have supplied cacheRegex patterns
	 * 
	 * set in phase 1; used in phase 2c
	 */
	protected boolean cachePatternHasStructures;

	/**
	 * files matched will be cached in the target directory
	 */
	protected Pattern vendorCachePattern;

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
	 * phases 2a and 2c
	 * 
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
	protected PropertyManagerI getPropertyManager(Matcher m, boolean allowStruc) {
		if (m == null)
			return null;
		if (m.group("struc") != null)
			return (allowStruc ? getStructurePropertyManager() : null);
		for (int i = bsPropertyVendors.nextSetBit(0); i >= 0; i = bsPropertyVendors.nextSetBit(i + 1)) {
			String ret = m.group("param" + i);
			if (ret != null && ret.length() > 0) {
				return VendorPluginI.activeVendors.get(i).vendor;
			}
		}
		return null;
	}

	/**
	 * A simpler method of extraction than what IFDExtractor uses; no rezipping here.
	 * Used by Crawler. 
	 * 
	 * @param f
	 */
	public void extractSpecProperties(File f) {
		if (vendorCachePattern == null)
			return;
		VendorPluginI vendor = null;
		String localPath = f.getAbsolutePath();
		ArchiveInputStream ais = null;
		try {
			if (!FAIRSpecUtilities.isZip(localPath)){
				Matcher m = vendorCachePattern.matcher(localPath);
				if (m.find()) {
					vendor = (VendorPluginI) getPropertyManager(m, false);
					if (vendor == null || vendor.isDerived())
						return;
					byte[] bytes = FAIRSpecUtilities.getBytesAndClose(new FileInputStream(f));
					vendor.accept(this, localPath, bytes);
				}
			    return;
			}
			// zip file -- check all contents
			vendor = getVendorForZipFile(localPath);
			if (vendor == null)
				return;
			vendor.initializeDataSet(this);
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(localPath));
			ais = new ArchiveInputStream(bis, null);
			ArchiveEntry zipEntry = null;
			while ((zipEntry = ais.getNextEntry()) != null) {
				String name = zipEntry.getName();
				long len = zipEntry.getSize();
				if (len <= 0 || name == null || name.length() == 0
						|| zipEntry.isDirectory()) {
					continue;
				}				
				byte[] bytes = FAIRSpecUtilities.getLimitedStreamBytes(ais, len, null, false, false);
				Matcher m = vendorCachePattern.matcher(name);
				if (m.find() && vendor == getPropertyManager(m, false))
					vendor.accept(this, name, bytes);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (vendor != null)
				vendor.endDataSet();
			if (ais != null) {
				try {
					ais.close();
				} catch (IOException e) {
				}
			}
		}
		
	}

	/**
	 * Check for zip entry name match, such as props or acqui.
	 * 
	 * @param localPath
	 * @return vendor PropertyManagerI
	 */
	private VendorPluginI getVendorForZipFile(String localPath) {
		ArchiveInputStream ais = null;
		try {
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(localPath));
			ais = new ArchiveInputStream(bis, null);
			ArchiveEntry zipEntry = null;
			while ((zipEntry = ais.getNextEntry()) != null) {
				String name = zipEntry.getName();
				if (name == null || name.length() == 0
						|| zipEntry.isDirectory()) {
					continue;
				}
				Matcher m = vendorCachePattern.matcher(name);
				if (m.find())
					return (VendorPluginI) getPropertyManager(m, false);
			}
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			if (ais != null) {
				try {
					ais.close();
				} catch (IOException e) {
				}
			}
		}
	}
	
	/**
	 * not implemented as something that can be adjusted; currently:
	 * 
	 * "(?<img>\\.pdf$|\\.png$)|(?<struc>(?<mol>\\.mol$|\\.sdf$)|(?<cdx>\\.cdx$|\\.cdxml$)|(?<cif>\\.cif$)|(?<cml>\\.cml$))"
	 * 
	 * A combination of FAIRSpecExtractionHelper.defaultCachePattern and
	 * ifd.properties.IFD_DEFAULT_STRUCTURE_FILE_PATTERN
	 */
	private String userStructureFilePattern;

	/**
	 * files matched will be cached as zip files
	 */
	protected Pattern extractionCachePattern;

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

	public boolean initializePropertyExtraction() {
		// options here to set cache and rezip options -- debugging only!
		String sp = userStructureFilePattern;
		if (sp == null) {
			sp = FAIRSpecExtractorHelper.defaultCachePattern + "|" + getStructurePropertyManager().getParamRegex();
		} else if (sp.length() == 0) {
			sp = "(?<img>\n)|(?<struc>\n)";
		}
		cachePatternHasStructures = (sp.indexOf("<struc>") >= 0);
		String s = "";
		for (int i = 0; i < VendorPluginI.activeVendors.size(); i++) {
			String cp = VendorPluginI.activeVendors.get(i).vcache;
			if (cp != null) {
				bsPropertyVendors.set(i);
				s += "|" + cp;
			}
		}
//		String procs = null; // for debugging only
//		if (procs != null) {
//			s += "|" + procs;
//		}
		extractionCachePattern = (s.length() == 0 ? null : Pattern.compile(s.substring(1)));
		if (s.length() > 0) {
			s = "(?<param>" + s.substring(1) + ")|" + sp;
			cachePatternHasVendors = true;
		} else {
			s = sp;
		}
		vendorCachePattern = Pattern.compile("(?<ext>" + s + ")");
		return (extractionCachePattern != null);
	}

}
