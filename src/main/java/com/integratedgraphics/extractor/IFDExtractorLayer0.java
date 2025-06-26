package com.integratedgraphics.extractor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecExtractorHelper;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecExtractorHelper.FileList;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecExtractorHelperI;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecFindingAidHelperI;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;
import org.iupac.fairdata.extract.DefaultStructureHelper;

import com.integratedgraphics.extractor.ExtractorUtils.ExtractorResource;
import com.integratedgraphics.extractor.ExtractorUtils.ObjectParser;
import com.integratedgraphics.ifd.api.VendorPluginI;

/**
 * A general class for constants, setting the configuration, 
 * shared fields, logging and file writing, 
 * 
 * @author hanso
 *
 */
abstract class IFDExtractorLayer0 extends FindingAidCreator {
 
	/**
	 * a key for the deferredObjectList that flags a structure with a spectrum;
	 * needs attention, as this was created prior to the idea of a compound
	 * association, and it presumes there are no such associations.
	 * 
	 */
	public static final String NEW_PAGE_KEY = "*NEW_PAGE*";

	/**
	 * "." here is the Eclipse project extract/ directory
	 * 
	 */
	protected static final String DEFAULT_STRUCTURE_DIR_URI = "./structures/";
	protected static final String DEFAULT_STRUCTURE_ZIP_URI = "./structures.zip";
	protected static final int LOG_ACCEPTED = 3;

	protected static final int LOG_IGNORED = 1;
	protected static final int LOG_OUTPUT = 2;
	protected static final int LOG_REJECTED = 0;
	protected final static Pattern objectDefPattern = Pattern.compile("\\{([^:]+)::([^}]+)\\}");

	protected final static Pattern pStarDotStar = Pattern.compile("\\*([^|/])\\*");
	protected final static String SUBST = "=>";

	protected static final String FAIRSPEC_EXTRACTOR_REFERENCES = IFDConst.getProp("FAIRSPEC_EXTRACTOR_REFERENCES");


	/**
	 * debugging help
	 */
	protected String stopAfter;

	/**
	 * debugging help, from -stopafter:xx flag
	 * 
	 * where xx is 1,2,2a,2b,2c,2d,3a,3b,3c
	 * 
	 * @param what
	 */
	protected void checkStopAfter(String what) {
		log("!" + what 
				+ " #struc=" + faHelper.getStructureCollection().size()
				+ " #spec=" + faHelper.getSpecCollection().size()
				+ " #cmpd=" + faHelper.getCompoundCollection().size()
				);
		boolean stopping = what.equals(stopAfter);
		if (stopping) {
			log("stopping after " + what);
			System.exit(0);
		}
	}
		

	/**
	 * contents of the file "extractor.config.json"
	 */
	protected Map<String, Object> config = null;

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
	 * a JSON-derived map
	 * 
	 * set in phase 1; used in phase 3
	 */
	protected Map<String, Map<String, Object>> htURLReferences;

	/**
	 * the IFD-extract.json script
	 */
	protected String extractScript;

	/**
	 * the file containing extract script JSON
	 * 
	 */
	protected File extractScriptFile;

	/**
	 * the directory containing the extract script file
	 */
	protected String extractScriptFileDir;

	/**
	 * the finding aid helper - only one per instance
	 */
	protected FAIRSpecExtractorHelperI helper;

	/**
	 * a map of metadata key/value pairs created from 
	 * reading a spreadsheet file
	 * 
	 * <code>
		{"FAIRSpec.extractor.metadata":[
       	  {"FOR":"IFD.property.fairspec.compound.id",
         		"METADATA_FILE":"./Manifest.xlsx",
         		"METADATA_KEY":"TM compound number"
       	  }
  		]},

	 * 
	 * set in phase 1; used in phase 2b
	 * 
	 */
	protected Map<String, Map<String, Object>> htSpreadsheetMetadata;

	/**
	 * resource map
	 * 
	 * set in phase 1; used in phase 3
	 * 
	 */
	protected Map<String, ExtractorResource> htResources = new HashMap<>();

	
	/**
	 * a simple file (default IFD_METADATA) with key=value pairs
	 * one per line.
	 * 
	 * set in phase 1; used in phase 2c
	 */
	protected String ifdMetadataFileName = "IFD_METADATA"; 

	/**
	 * regex patters for ignoring and accepting files
	 * 
	 * set in phase 1; used in phase 2
	 */
	protected String ignoreRegex, acceptRegex;

	/**
	 * a local zip file or directory that overrides the IFD-extract.json file
	 * 
	 * phase 1 only
	 */
	protected String localSourceFile;

	/**
	 * working local name, without the rootPath, as found in _IFD_manifest.json
	 * 
	 * phase 2 only
	 */
	protected String localizedName;

	/**
	 * an optional local source directory to use instead of the one indicated in
	 * IFD-extract.json
	 * 
	 * phases 1 and 2
	 */
	protected String localSourceDir;

	/**
	 * list of files to always accept, specified an extractor JSON template
	 * 
	 * phases 1 and 2
	 */
	protected FileList lstAccepted = new FileList(null, "accepted", null);

	/**
	 * list of files ignored -- probably MACOSX trash or Google desktop.ini trash
	 */
	protected FileList lstIgnored;

	/**
	 * list of files rejected -- probably MACOSX trash or Google desktop.ini trash
	 */
	protected final FileList lstRejected = new FileList(null, "rejected", ".");

	/**
	 * Track the files written to the collection, even if there is no output. This
	 * allows for removing ZIP files from the finding aid and manifest if they are
	 * not actually written.
	 */
	protected FileList lstWritten = new FileList(null, "written", null);

	/**
	 * objects found in IFD-extract.json
	 */
	protected List<ObjectParser> objectParsers;
	
	/**
	 * working origin path while checking zip files
	 * 
	 */
	protected String originPath;

	/**
	 * files matched will be cached as zip files
	 */
	protected Pattern rezipCachePattern;

	protected ArrayList<Object> rootPaths = new ArrayList<>();

	/**
	 * the structure property manager for this extractor
	 * 
	 */
	private DefaultStructureHelper structurePropertyManager;

	/**
	 * a required target directory
	 */
	protected File targetDir;

	/**
	 * working memory cache of representations keyed to their localized name
	 * (possibly with an extension for a page within the representation, such as an
	 * MNova file. These are identified by vendors and that can create additional
	 * properties or representations from them in Phase 2b that will need to be
	 * processed in Phase 2c.
	 */
	protected Map<String, ExtractorUtils.CacheRepresentation> vendorCache;

	/**
	 * files matched will be cached in the target directory
	 */
	protected Pattern vendorCachePattern;

	public int getErrorCount() {
		return errors;
	}

	@Override
	protected FAIRSpecFindingAidHelperI getHelper() {
		return faHelper;
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

	protected void initializeExtractor() {
		setConfiguration();
		setDefaultRunParams();
	}

	protected FAIRSpecExtractorHelper newExtractionHelper() throws IFDException {
		return new FAIRSpecExtractorHelper(this, getCodeSource() + " " + getVersion());
	}

	private void setConfiguration() {
		try {
			config = FAIRSpecUtilities.getJSONResource(IFDExtractor.class, "extractor.config.json");
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}
	
	protected void setPropertyVendors(String sp) {
		cachePatternHasStructures = (sp.indexOf("<struc>") >= 0);
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
		vendorCache = new LinkedHashMap<String, ExtractorUtils.CacheRepresentation>();
	}
	
	

}
