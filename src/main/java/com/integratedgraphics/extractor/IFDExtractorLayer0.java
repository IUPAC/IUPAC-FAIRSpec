package com.integratedgraphics.extractor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecExtractorHelper;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecExtractorHelper.FileList;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecExtractorHelperI;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecExtractorI;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecFindingAidHelperI;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;
import org.iupac.fairdata.core.IFDProperty;
import org.iupac.fairdata.core.IFDRepresentableObject;
import org.iupac.fairdata.core.IFDRepresentation;
import org.iupac.fairdata.extract.DefaultStructureHelper;

import com.integratedgraphics.extractor.ExtractorAids.ArchiveInputStream;
import com.integratedgraphics.extractor.ExtractorAids.CacheRepresentation;
import com.integratedgraphics.extractor.ExtractorAids.ExtractorResource;
import com.integratedgraphics.extractor.ExtractorAids.ObjectParser;

/**
 * A general class for constants, shared fields, logging and file writing, 
 * and various methods that are utilized in two or more phases.
 * 
 * @author hanso
 *
 */
abstract class IFDExtractorLayer0 extends FindingAidCreator implements FAIRSpecExtractorI {

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

	protected final static int PHASE_2A = 1;
	protected final static int PHASE_2B = 2;
	protected final static int PHASE_2C = 3;
	protected final static int PHASE_2D = 4;

	protected final static Pattern pStarDotStar = Pattern.compile("\\*([^|/])\\*");
	protected final static String SUBST = "=>";

	/**
	 * the resource currrently being processed in Phase 2 or 3.
	 * 
	 */
	protected ExtractorResource extractorResource;

	Map<String, Object> config = null;

	/**
	 * bitset of activeVendors that are set for property parsing
	 */
	protected BitSet bsPropertyVendors = new BitSet();

	/**
	 * bitset of activeVendors that are set for rezipping
	 */
	protected BitSet bsRezipVendors = new BitSet();

	/**
	 * vendors have supplied cacheRegex patterns
	 */
	protected boolean cachePatternHasVendors;

	/**
	 * a list of properties that vendors have indicated need addition, keyed by the
	 * zip path for the resource
	 */
	protected List<Object[]> deferredPropertyList;

	/**
	 * the IFD-extract.json script
	 */
	protected String extractScript;

	protected File extractScriptFile;
	protected String extractScriptFileDir;

	/**
	 * the finding aid helper - only one per instance
	 */
	protected FAIRSpecExtractorHelperI helper;

	protected Map<String, Map<String, Object>> htMetadata;

	/**
	 * working map from manifest names to structure or data object
	 */
	protected Map<String, IFDRepresentableObject<?>> htLocalizedNameToObject = new LinkedHashMap<>();

	protected Map<String, ExtractorResource> htResources = new HashMap<>();

	protected String ifdMetadataFileName = "IFD_METADATA"; // default only

	protected String ignoreRegex, acceptRegex;

	protected String localSourceFile;

	/**
	 * working local name, without the rootPath, as found in _IFD_manifest.json
	 */
	protected String localizedName;

	/**
	 * an optional local source directory to use instead of the one indicated in
	 * IFD-extract.json
	 */
	protected String localSourceDir;

	/**
	 * list of files to always accept, specified an extractor JSON template
	 */
	protected FileList lstAccepted = new FileList(null, "accepted");

	/**
	 * list of files ignored -- probably MACOSX trash or Google desktop.ini trash
	 */
	protected FileList lstIgnored;

	/**
	 * list of files extracted
	 */
	protected FileList lstManifest;

	/**
	 * list of files rejected -- probably MACOSX trash or Google desktop.ini trash
	 */
	protected final FileList lstRejected = new FileList(null, "rejected");

	/**
	 * Track the files written to the collection, even if there is no output. This
	 * allows for removing ZIP files from the finding aid and manifest if they are
	 * not actually written.
	 */
	protected FileList lstWritten = new FileList(null, "written");

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
	protected DefaultStructureHelper structurePropertyManager;

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
	protected Map<String, CacheRepresentation> vendorCache;

	/**
	 * files matched will be cached in the target directory
	 */
	protected Pattern vendorCachePattern;

	@Override
	public void addProperty(String key, Object val) {
		if (val != IFDProperty.NULL)
			log(this.localizedName + " addProperty " + key + "=" + val);
		addDeferredPropertyOrRepresentation(key, val, false, null, null);
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

	public int getErrorCount() {
		return errors;
	}

	/**
	 * Add a record for _IFD_manifest.json or _IFD_ignored.json
	 * 
	 * used in Phases 2 and 3 only
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
	 * Get the full OS file path for FileOutputStream
	 * 
	 * @param originPath
	 * @return
	 */
	protected File getAbsoluteFileTarget(String originPath) {
		return new File(targetDir + "/" + extractorResource.rootPath + "/" + localizePath(originPath));
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
		getStructurePropertyManager();
	}

	protected static boolean isDefaultStructurePath(String val) {
		return (DEFAULT_STRUCTURE_DIR_URI.equals(val) || DEFAULT_STRUCTURE_ZIP_URI.equals(val));
	}
	
	/**
	 * Clean up the zip entry name to remove '/', ' ', and add ".zip" if there
	 * is a trailing '/' in the name. Zip paths marks '|' are switched to '..'.
	 * 
	 * Used in Phases 2 (directly) and 3 (indirectly) only.
	 * 
	 * @param path
	 * @return localized path
	 */
	protected static String localizePath(String path) {
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
	 * For testing (or for whatever reason zip files are local or should not use the
	 * given source paths), replace https://......./ with sourceDir/
	 * 
	 * @param sUrl
	 * @return localized URL
	 * @throws IFDException
	 */
	protected String localizeURL(String sUrl) throws IFDException {
		if (sUrl == null) {
//			if (new File(localSourceFile).isAbsolute())
			sUrl = localSourceFile;
//			else
//				sUrl = localSourceDir + "/" + localSourceFile;
		} else {
			boolean isRelative = sUrl != null && (sUrl.startsWith("./") || sUrl.indexOf("/./") >= 0);
			if (!isRelative && localSourceDir != null) {
				if (FAIRSpecUtilities.isZip(localSourceDir)) {
					sUrl = localSourceDir;
				} else if (localSourceDir.endsWith("/*")) {
					sUrl = localSourceDir.substring(0, localSourceDir.length() - 1);
				} else {
					int pt = sUrl.lastIndexOf("/");
					if (pt >= 0) {
						sUrl = localSourceDir + sUrl.substring(pt);
						if (!FAIRSpecUtilities.isZip(sUrl) && !sUrl.endsWith("/"))
							sUrl += ".zip";
					}
				}

			}
		}
		sUrl = toAbsolutePath(sUrl);

		if (sUrl.indexOf("//") < 0 && !sUrl.startsWith("file:/"))
			sUrl = "file:/" + sUrl;
		return sUrl;
	}

	protected FAIRSpecExtractorHelper newExtractionHelper() throws IFDException {
		return new FAIRSpecExtractorHelper(this, getCodeSource() + " " + getVersion());
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

	protected String toAbsolutePath(String fname) {
		if (fname.startsWith("./"))
			fname = extractScriptFileDir.replace('\\', '/') + fname.substring(1);
		return fname;
	}

	private void setConfiguration() {
		try {
			config = FAIRSpecUtilities.getJSONResource(IFDExtractor.class, "extractor.config.json");
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}

	/**
	 * For Phases 2 and 3 only
	 * @param bytes
	 * @param f
	 * @throws IOException
	 */
	protected void writeBytesToFile(byte[] bytes, File f) throws IOException {
		if (!noOutput)
			FAIRSpecUtilities.writeBytesToFile(bytes, f);
	}

	/**
	 * Transfer an ignored file to the collection as a digital item, provided the
	 * includeIgnoredFiles flag is set. Phases 2 and 3 only.
	 * 
	 * @param originPath
	 * @param ais
	 * @param len
	 * @throws IOException
	 */
	private void writeDigitalItem(String originPath, ArchiveInputStream ais, long len, int mode) throws IOException {
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

}