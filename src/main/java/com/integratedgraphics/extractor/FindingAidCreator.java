package com.integratedgraphics.extractor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecFindingAid;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecFindingAidHelperI;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;
import org.iupac.fairdata.core.IFDFindingAid;
import org.iupac.fairdata.extract.MetadataReceiverI;

import com.integratedgraphics.ifd.api.VendorPluginI;

/**
 * This abstract class backs MetadataExtractor and DOICrawler,
 * both of which create finding aids. 
 * 
 * @author hansonr@stolaf.edu
 *
 */
public abstract class FindingAidCreator implements MetadataReceiverI {

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


	protected String localizedTopLevelZipURL;

	protected boolean haveExtracted;

	protected String errorLog = "";

	public int testID = -1;
	
	protected String thisRootPath;


	public String strWarnings = "";

	public int warnings;

	public int getWarningCount() {
		return warnings;
	}

	public int errors;


	protected boolean dataciteUp = true;

	protected boolean cleanCollectionDir = true;

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
		createZippedCollection = createZippedCollection && !debugReadOnly;
		
		readOnly |= debugReadOnly; // for testing; when true, no output other than a log file is produced
		noOutput = (createFindingAidOnly || readOnly);
		skipPubInfo = !dataciteUp || debugReadOnly; // true to allow no internet connection and so no pub calls
		
		createLandingPage &= !readOnly;
		launchLandingPage &= createLandingPage;

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
			if (args[i] != null)
				moreFlags += "-" + args[i] + ";";
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
		
		return flags;
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

}
