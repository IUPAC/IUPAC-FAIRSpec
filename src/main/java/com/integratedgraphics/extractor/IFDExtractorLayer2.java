package com.integratedgraphics.extractor;

import java.io.BufferedInputStream;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecCompoundAssociation;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecExtractorHelper;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecExtractorHelper.FileList;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecFindingAidHelper;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities.SpreadsheetReader;
import org.iupac.fairdata.core.IFDAssociation;
import org.iupac.fairdata.core.IFDObject;
import org.iupac.fairdata.core.IFDProperty;
import org.iupac.fairdata.core.IFDReference;
import org.iupac.fairdata.core.IFDRepresentableObject;
import org.iupac.fairdata.core.IFDRepresentation;
import org.iupac.fairdata.dataobject.IFDDataObject;
import org.iupac.fairdata.extract.DefaultStructureHelper;
import org.iupac.fairdata.extract.PropertyManagerI;
import org.iupac.fairdata.sample.IFDSample;
import org.iupac.fairdata.structure.IFDStructure;
import org.iupac.fairdata.structure.IFDStructureRepresentation;

import com.integratedgraphics.extractor.ExtractorUtils.AWrap;
import com.integratedgraphics.extractor.ExtractorUtils.ArchiveEntry;
import com.integratedgraphics.extractor.ExtractorUtils.ArchiveInputStream;
import com.integratedgraphics.extractor.ExtractorUtils.CacheRepresentation;
import com.integratedgraphics.extractor.ExtractorUtils.DirectoryInputStream;
import com.integratedgraphics.extractor.ExtractorUtils.ExtractorResource;
import com.integratedgraphics.extractor.ExtractorUtils.ObjectParser;
import com.integratedgraphics.ifd.api.VendorPluginI;

/**
 * Phase 2: Carry out the actual extraction of metadata.
 * 
 * Set up the target directory for files:
 * 
 * _IFD_warnings.txt, _IFD_rejected.json, _IFD_ignored.json, _IFD_manifest.json,
 * IFD.findingaid.json, and IFD.collection.zip
 * 
 * Set up the rezipCache for vendors that need to do that (Bruker) where we want
 * to fix the directory structure and break multiple numbered directories into
 * separate zip files. The metadata for these vendors will be handled in Phase
 * 2c.
 * 
 * Since some files (MNova, e.g.) contain multiple objects that are mixtures of
 * structure and spectrum representations, we do two full scans (2a and 2b).
 * 
 * Phase 2a:
 * 
 * This pass generates the ordered map of the archive contents, by resource, for
 * use in the next phases. During this initial phase, the CollectionSet is not
 * built.
 * 
 * Process files in the FAIRSpec-ready collection, extracting metadata and
 * representations, but do not create any Finding Aid objects or collections.
 * 
 * Scan zip entries or directories for representations of structures and
 * spectral data, extracting representations such as MOL, JDX, PDF, embedded MNova.
 * 
 * Some file structures need to be fixed. Specifically, the name of the Bruker
 * directory containing acqu must be a simple integer, or the dataset cannot be
 * read back into TopSpin. Not all authors realize this. So we flag those for
 * fixing. These datasets are identified here, but they are not (re)zipped until Phase 2c. 
 * 
 * Files may come from more than one source (two FigShare ZIP files, for
 * example).
 * 
 * Create the files for the IUPAC FAIRSpec Collection, leveling the directories.
 * For example,
 * 
 * 33_33-acetone-d6_33-in_acetone-d6_13C_33-in_acetone-d6_13C.jdx
 * 
 * and
 * 
 * 28_28-1H_28-1H.zip..20_pdata_1_Mar02-2022_20_1.pdf
 * 
 * where ".." means "extracted from within this file"
 * 
 * Rezippable data sets are NOT included in the IUPAC FAIRSpec Collection yet.
 * They are added in Phase 2c.
 * 
 * Iterate through all parser objects, loooking for objects.
 * 
 * There is one parser object created for each of the IFD-extract.json
 * "FAIRSpec.extractor.object" records.
 *
 * Vendor plug-ins such as MestreNova extract structure byte[] representations
 * (MOL, PNG) and metadata associated with spectra along with paging
 * information, which allows for new associations.
 * 
 * Parse the file path, creating association, structure, sample, and spectrum
 * objects. This phase produces the deferredPropertyList, which is processed
 * after all the parsing is done because sometimes the object is not recognized
 * until a key file (Bruker procs, for example, is found.
 * 
 *
 *
 * Phase 2b:
 * 
 * During this phase, the Finding Aid CollectionSet is created and populated.
 * 
 * Run through the FAIRSpec-ready collection again, this time creating objects
 * in the Finding Aid.
 * 
 * This pass will also add metadata from a spreadsheet file.
 * 
 * Phase 2c:
 * 
 * All objects and representations other than rezippable datasets have been
 * created.
 * 
 * An important feature of Extractor is that it can repackage zip files,
 * removing resources that are totally unnecessary and extracting properties and
 * representations using IFDVendorPluginI services.
 * 
 * If there is rezipping to be done, we iterate over all files again, this time
 * doing all the fixes and creating new ZIP files in the IUPAC FAIRSpec
 * Collection.
 * 
 * During this process, properties from the rezippable datasets will be
 * extracted and added to the deferred property list at the beginning of the
 * file, before any other properties.
 * 
 * Phase 2d:
 * 
 * Process the deferred properties and extracted representations.
 * 
 * Phase 2e:
 * 
 * Ensure that all files in the FAIRSpec-ready collection have been processed,
 * or flag them as rejected or ignored.
 * 
 * @author Bob Hanson (hansonr@stolaf.edu)
 *
 */
abstract class IFDExtractorLayer2 extends IFDExtractorLayer1 {

	private final static String PHASE_2A = "2a";
	//private final static String PHASE_2B = "2b";
	private final static String PHASE_2C = "2c";
	//private final static String PHASE_2D = "2d";
	private final static String PHASE_2E = "2e";

	/**
	 * list of files extracted
	 */
	protected FileList lstManifest;

	/**
	 * the resource currrently being processed in Phase 2 or 3.
	 * 
	 */
	protected ExtractorResource extractorResource;

	/**
	 * working map from manifest names to structure or data object
	 */
	private Map<String, IFDRepresentableObject<?>> htLocalizedNameToObject = new LinkedHashMap<>();

	/**
	 * retrieves a clone data source or a cloned object from an id 
	 */
	private Map<String, IFDRepresentableObject<?>> htCloneMap = new HashMap<>();

	/**
	 * a list of properties that vendors have indicated need addition, keyed by the
	 * zip path for the resource
	 */
	private List<Object[]> deferredPropertyList;

	/**
	 * an insertion pointer into the deferredPropertyList;
	 * reset to 0 after Phase 2b.
	 */
	private int deferredPropertyPointer;

	/**
	 * a key for the deferredObjectList that indicates we have a new resource
	 * setting
	 */
	private static final String NEW_RESOURCE_KEY = "*NEW_RESOURCE*";

	private class ParserIterator implements Iterator<ObjectParser> {

		int i;

		ParserIterator() {
			extractorResource = null;
			thisRootPath = "";
		}

		@Override
		public boolean hasNext() {
			return (i < objectParsers.size());
		}

		@Override
		public ObjectParser next() {
			return objectParsers.get(i++);
		}

	}

	/**
	 * rezip data saved as an ISFRepresentation (for no particularly good reason)
	 */
	private ExtractorUtils.CacheRepresentation currentRezipRepresentation;

	/**
	 * path to this resource in the original zip file
	 */
	private String currentRezipPath;

	/**
	 * vendor association with this rezipping
	 */
	private VendorPluginI currentRezipVendor;

	/**
	 * last path to this rezip top-level resource
	 */
	private String lastRezipPath;

//	/**
//	 * not used, typically 0
//	 * 
//	 * the number of IFDObjects created
//	 */
//	private int ifdObjectCount;

	/**
	 * cache of top-level resources to be rezipped
	 */
	private List<ExtractorUtils.CacheRepresentation> rezipCache;

	/**
	 * working map from manifest names to structure or data object
	 */
	private Map<String, String> htZipRenamed = new LinkedHashMap<>();

	private File currentZipFile;

	/**
	 * Slows this down a bit, but allows, for example, a CIF file to be both a
	 * structure and an object
	 */
	private boolean allowMultipleObjectsForRepresentations = true;
	
	/**
	 * The main extraction phase. Find and extract all objects of interest from a ZIP
	 * file.
	 * 
	 */
	protected void processPhase2(File targetDir) throws IFDException, IOException {
		if (haveExtracted)
			throw new IFDException("Only one extraction per instance of Extractor is allowed (for now).");
		haveExtracted = true;
		if (targetDir == null)
			throw new IFDException("The target directory may not be null.");
		setupTargetDir(targetDir);

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
		rezipCache = new ArrayList<>();

		// There is one parser created for each of the IFD-extract.json
		// "FAIRSpec.extractor.object" records.
		// Each of phases 2a, 2b, 2c, and 2d iterate over these records.
		

		Map<String, Map<String, ArchiveEntry>> htArchiveContents = new LinkedHashMap<>();

		// Phase 2a

		// scan the FAIRSpec-ready collection
		//
		// Create the IUPAC FAIRSpec Data Collection.
		//
		// Extract all metadata and embedded representations,
		// saving them for later proccessing as "deferred". 
		// 
		// The finding aid objects themselves are not created yet. 
		//
		phase2aIterate(htArchiveContents);
		
		checkStopAfter("2a");

		// Phase 2b

		log("!Phase 2b create IFDObjects from the FAIRSpec-ready collection");
		

		// Now run through the FAIRSpec-ready collection again, this time
		// creating objects in the Finding Aid.
		//
		// This will also add metadata from a spreadsheet file.


		phase2bIterate(htArchiveContents);

		checkStopAfter("2b");


		// Phase 2c

		log("!Phase 2c process all rezippable datasets and create their IUPAC FAIRSpec Data Collection");
		

		// All objects have been created.

		// An important feature of Extractor is that it can repackage zip files,
		// removing resources that are totally unnecessary and extracting properties
		// and representations using IFDVendorPluginI services.

		deferredPropertyPointer = 0;
		if (rezipCache != null && rezipCache.size() > 0) {
			phase2cGetNextRezipName();
			lastRezipPath = null;
			phase2cIterate();
		}

		checkStopAfter("2c");


		// Phase 2d

		log("!Phase 2d process all deferred object properties");
		
		phase2dProcessDeferredObjectProperties(null);

		checkStopAfter("2d");


		// Phase 2e

		// ensure that all files in the FAIRSpec-ready collection have been processed, or flag them as rejected or ignored
		
		log("!Phase 2e check rejected files and update lists");
		
		phase2eIterate();

		return;
	}

	private void phase2aIterate(Map<String, Map<String, ArchiveEntry>> contents) throws IOException, IFDException {

		// Scan through parsers for resource changes

		ParserIterator iter = new ParserIterator();
		log("!Phase 2a initializing data sources ");
		while (iter.hasNext()) {
			ExtractorResource currentSource = extractorResource;
			nextParser(iter);
			if (extractorResource != currentSource) {
				currentSource = extractorResource;
				if (cleanCollectionDir) {
					File dir = new File(targetDir + "/" + extractorResource.rootPath);
					log("!Phase 2a cleaning directory " + dir);
					FileUtils.cleanDirectory(dir);
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
						faHelper.setCurrentResourceByteLength(len);
					zipFileMap = phase2ReadFAIRSpecReadyCollectionIteratively(is, "", PHASE_2A,
							new LinkedHashMap<String, ArchiveEntry>());
					contents.put(localizedTopLevelZipURL, zipFileMap);
				}
			}
		}
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

	private Map<String, ArchiveEntry> phase2ReadFAIRSpecReadyCollectionIteratively(InputStream is, String baseOriginPath,
			String phase, Map<String, ArchiveEntry> originToEntryMap) throws IOException, IFDException {
		if (debugging && baseOriginPath.length() > 0)
			log("! opening " + baseOriginPath);
		boolean isTopLevel = (baseOriginPath.length() == 0);
		ArchiveInputStream ais = new ArchiveInputStream(is, isTopLevel ? extractorResource.getSourceFile() : null);
		ArchiveEntry zipEntry = null;
		ArchiveEntry nextEntry = null;
		ArchiveEntry nextRealEntry = null;
		int n = 0;
		boolean first = (phase != PHASE_2E);
		int pt;
		while ((zipEntry = (nextEntry != null ? nextEntry
				: nextRealEntry != null ? nextRealEntry : ais.getNextEntry())) != null) {
			n++;
			nextEntry = null;
			String name = zipEntry.getName();
//			System.out.println(">>>>" + name);
			if (name == null)
				continue;
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
					log("Phase " + phase + " checking zip directory: " + n + " " + oPath);
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
			if (FAIRSpecUtilities.isZip(oPath)) {
				// iteratively check zip files if not in the final checking phase
				phase2ReadFAIRSpecReadyCollectionIteratively(ais, oPath + "|", phase, originToEntryMap);
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
				case PHASE_2E:
					// final check
					if (!isDir)
						phase2eCheckOrReject(ais, oPath, zipEntry.getSize());
					break;
				}
			}
			nextEntry = null;
		}
		if (isTopLevel)
			ais.close();
		return originToEntryMap;
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
	private void phase2aProcessEntry(String baseOriginPath, String originPath, InputStream ais, ArchiveEntry zipEntry,
			boolean accept) throws FileNotFoundException, IOException {
		long len = zipEntry.getSize();
		Matcher m = null;

		// check for files that should be pulled out - these might be JDX files, for
		// example.
		// "param" appears if a vendor has flagged these files for parameter extraction.

		boolean isFound = false;
		if (vendorCachePattern != null && (isFound = (m = vendorCachePattern.matcher(originPath)).find()) || accept) {

			PropertyManagerI v = (isFound ? getPropertyManager(m, true, true) : null);
			boolean doCheck = (v != null);
			boolean isStructure = (doCheck && v == structurePropertyManager);
			boolean doExtract = (!doCheck || v.doExtract(originPath));

//			1. we don't have params, but we want the file extracted 
//		      - generic file, just save it.  doExtract and not doCheck
//			2. we have params and there is extraction
//		      - save file and also check it for parameters  doExtract and doCheck
//			3. nothing to check and no extraction  !doCheck  and !doExtract
//		      - ignore completely

			if (doExtract) {
				String ext = (isFound ? m.group("ext") : originPath.substring(originPath.lastIndexOf(".") + 1));
				File f = getAbsoluteFileTarget(originPath);
				boolean embed = (insitu || embedPDF && originPath.endsWith(".pdf"));
				boolean toByteArray = (doCheck || noOutput || embed);
				OutputStream os = (toByteArray ? new ByteArrayOutputStream() : new FileOutputStream(f));
				if (os != null)
					FAIRSpecUtilities.getLimitedStreamBytes(ais, len, os, false, true);
				String localizedName = localizePath(originPath);
				String type = null;
				byte[] bytes = null;
				if (toByteArray) {
					// doCheck or noOutput or insitu
					bytes = ((ByteArrayOutputStream) os).toByteArray();
					len = bytes.length;
					if (doCheck) {
						// set this.localizedName for parameters
						// preserve this.localizedName, as we might be in a rezip.
						// as, for example, a JDX file within a Bruker dataset
						if (!embed)
							writeOriginToCollection(originPath, bytes, 0);
						String oldOriginPath = this.originPath;
						String oldLocal = this.localizedName;
						this.originPath = originPath;
						this.localizedName = localizedName;
						// indicating "this" here notifies the vendor plug-in that
						// this is a one-shot file, not a collection.
						if (logging())
							log("Phase 2a accepting " + originPath);
						type = (isStructure && hasStructureFor(bytes) ? IFDProperty.NULL : v.accept(this, originPath, bytes));
							
//						if (type == IFDConst.IFD_PROPERTY_FLAG) {
//							// Q: What triggers this? Nothing?
//							// could be accepting a metadata file?
//							addIFDMetadata(new String(bytes));
//						    // this is now handled in Phase 2c
//						} else {
						deferredPropertyList.add(null);
						this.localizedName = oldLocal;
						this.originPath = oldOriginPath;
						if (type == null) {
							logWarn("Failed to read " + originPath + " (ignored)", v.getClass().getName());
						} else if (type == IFDProperty.NULL) {
							lstIgnored.add(originPath, len);
							logDigitalItemIgnored(originPath, localizedName, "equivalent structure", "phase2a");
							return;
						} else if (IFDConst.isStructure(type)
								|| type.startsWith(DefaultStructureHelper.STRUC_FILE_DATA_KEY)) {
							return;
						}
//						}
					}
				} else {
					len = f.length();
					writeOriginToCollection(originPath, null, len);
				}
				addFileAndCacheRepresentation(originPath, localizedName, embed ? bytes : null, len, type, ext, null);
			}
		}

		// here we look for the "trigger" file within a zip file that indicates that we
		// (may) have a certain vendor's files that need looking into. The case in point
		// is finding a procs file within a Bruker data set. Or, in principle, an acqus
		// file and just an FID but no pdata/ directory. But for now we want to see that
		// processed data.

		if (rezipCachePattern != null && (m = rezipCachePattern.matcher(originPath)).find()) {

			// e.g. exptno/./pdata/procs

			VendorPluginI v = phase2aGetVendorForRezip(m);
			originPath = m.group("path" + v.getIndex());
			if (originPath.equals(lastRezipPath)) {
				if (logging())
					log("duplicate path " + originPath);
			} else {
				lastRezipPath = originPath;
				String localPath = localizePath(originPath);
				CacheRepresentation rep = new CacheRepresentation(new IFDReference(faHelper.getCurrentSource().getID(),
						originPath, extractorResource.rootPath, localPath), v, len, null, "application/zip");
				// if this is a zip file, the data object will have been set to xxx.zip
				// but we need this to be
				String basePath = (baseOriginPath.endsWith("|")
						? baseOriginPath.substring(0, baseOriginPath.length() - 1)
						: new File(originPath).getParent() + "/");
				if (basePath == null)
					basePath = originPath;
				rep.setRezipOrigin(basePath);
				if (rezipCache.size() > 0) {
					CacheRepresentation r = rezipCache.get(rezipCache.size() - 1);
					if (r.getRezipOrigin().equals(basePath)) {
						rep.setIsMultiple();
						r.setIsMultiple();
					}
				}
				rezipCache.add(rep);
				log("!rezip pattern found " + originPath + " " + rep);
			}
		}

	}

	private void addProperties(List<IFDProperty> props) {
		for (int i = 0, n = props.size(); i < n; i++) {
			IFDProperty p = props.get(i);
			addDeferredPropertyOrRepresentation(p.getName(), p.getValue(), false, null, null, null);
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
	private VendorPluginI phase2aGetVendorForRezip(Matcher m) {
		for (int i = bsRezipVendors.nextSetBit(0); i >= 0; i = bsRezipVendors.nextSetBit(i + 1)) {
			String ret = m.group("rezip" + i);
			if (ret != null && ret.length() > 0) {
				return VendorPluginI.activeVendors.get(i).vendor;
			}
		}
		return null;
	}

	private void phase2bIterate(Map<String, Map<String, ArchiveEntry>> htArchiveContents) throws IOException, IFDException {
		ParserIterator iter = new ParserIterator();
		while (iter.hasNext()) {
			ObjectParser parser = nextParser(iter);
  
			log("!Phase 2b \n" + localizedTopLevelZipURL + "\n" + parser.getStringData());

			phase2bParseZipFileNamesForObjects(parser, htArchiveContents.get(localizedTopLevelZipURL));
			// what exactly is the ifdObjectCount?
//			if (logging())
//				log("!Phase 2b found " + ifdObjectCount + " IFD objects");
		}	
//		return true;//(ifdObjectCount != 0);
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
	 * @param keyList
	 * @param originPath
	 * @param localizeName
	 * @return one of IFDStructureSpec, IFDDataObject, IFDStructure, in that order,
	 *         depending upon availability
	 * 
	 * @throws IFDException
	 * @throws IOException
	 */
	private IFDObject<?> phase2bAddIFDObjectsForName(ObjectParser parser, List<String> keyList, String originPath,
			String localizedName, long len) throws IFDException, IOException {

		Matcher m = parser.match(originPath);
		if (!m.find()) {
			return null;
		}
		helper.beginAddingObjects(originPath);
		if (debugging)
			log("adding IFDObjects for " + originPath);

		// If an IFDDataObject object is added,
		// then it will also be added to htManifestNameToSpecData

		for (int i = keyList.size(); --i >= 0;) {
			String key = keyList.get(i);
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

	/**
	 * Parse the zip file using an object parser.
	 * 
	 * @param parser
	 * @param zipFileMap
	 * @return true if have spectra objects
	 * @throws IOException
	 * @throws IFDException 
	 */
	private void phase2bParseZipFileNamesForObjects(ObjectParser parser, Map<String, ArchiveEntry> zipFileMap)
			throws IOException, IFDException {
		
		List<String> keyList = parser.getKeyList();
		//System.out.println("EXL2b " + keyList);
		// for example, {compound=IFD.property.fairspec.compound.id::*}/{IFD.representation.structure.cdxml::{IFD.property.structure.id::*}.cdxml}
		// produces three keys:
		//
		// [compound, IFD0representation0structure0cdxml, IFD0property0structure0id]
		

		// next, we process those names
		for (Entry<String, ArchiveEntry> e : zipFileMap.entrySet()) {
			String originPath = e.getKey();
			String localizedName = localizePath(originPath);
			// Generally we allow a representation (cif for example) to be
			// linked to multiple objects. I can't think of reason not to allow this.
			if (!allowMultipleObjectsForRepresentations && htLocalizedNameToObject.containsKey(localizedName)
					|| lstIgnored.contains(originPath))
				continue;
			ArchiveEntry zipEntry = e.getValue();
			long len = zipEntry.getSize();
					

			IFDObject<?> obj = phase2bAddIFDObjectsForName(parser, keyList, originPath, localizedName, len);
			if (obj instanceof IFDRepresentableObject) {
				addFileToFileLists(originPath, LOG_OUTPUT, len, null);
				//ifdObjectCount++;
			}
		}
	}

	private void phase2cIterate() throws MalformedURLException, IOException, IFDException {
		ParserIterator iter = new ParserIterator();
		while (iter.hasNext()) {
			ObjectParser parser = nextParser(iter);
			if (parser.hasData())
				phase2ReadFAIRSpecReadyCollectionIteratively(getTopZipStream(), "", PHASE_2C, null);
		}
	}

	/**
	 * Pull the next rezip parent directory name off the stack, setting the
	 * currentRezipPath and currentRezipVendor fields.
	 * 
	 */
	private void phase2cGetNextRezipName() {
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
	private ArchiveEntry phase2cRezipEntry(String baseName, String oPath, ArchiveInputStream ais, ArchiveEntry entry,
			ArchiveEntry firstEntry, VendorPluginI rezipVendor) throws IOException, IFDException {

		// originPath points to the directory containing pdata

		// three possibilities:

		// xxx.zip/name/pdata --> xxx.zip_name.zip 1/pdata (ACS 22567817; localname
		// xxx_zip_name.zip)
		// xxx.zip/63/pdata --> xxx.zip 63/pdata (ICL; localname xxx.zip)
		// xxx.zip/pdata --> xxx.zip 1/pdata (ICL; localname xxx.zip)
		// xxx.zip/zzz/1/pdata --> xxx_1.zip/1/pdata (ICL; localname xxx.zip)
		// [new data object]
		// xxx.zip/zzz/2/pdata --> xxx_1.zip/2/pdata (ICL; localname xxx.zip)

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
		String newDir = rezipVendor.getRezipPrefix(thisDir);
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
				throw new IFDException("phase2cRezipEntry could not find object for " + lNameForObj);
			}
		}
		String basePath = baseName + (parent == null ? "" : parent);
		if (newDir == null) {
			newDir = "";
			boolean isMultiple = currentRezipRepresentation.isMultiple();
			if (isMultiple) {
				System.out.println("isMultiple!!" + currentRezipRepresentation);
			} else {
				oPath = (parent == null ? basePath.substring(0, basePath.length() - 1) : basePath);
			}
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
						(obj.getIDorIndex().endsWith("/" + thisDir) ? null : "_" + thisDir), obj, localizedName }, false, null,
						null, "P2 rezip");
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
			String msg = "Extractor correcting " + rezipVendor.getVendorName() + " directory name to " + localizedName + "|"
					+ newDir;
			addProperty(IFDConst.IFD_PROPERTY_DATAOBJECT_NOTE, msg);
			log("!" + msg);
		}
		localizedName = localizePath(oPath);
		htLocalizedNameToObject.put(localizedName, obj);
		this.localizedName = localizedName;
		File outFile = getAbsoluteFileTarget(oPath);
		log("!Extractor Phase 2c rezipping " + baseName + entry + " as " + outFile);
		OutputStream fos = (insitu || noOutput ? new ByteArrayOutputStream() : new FileOutputStream(outFile));
		ZipOutputStream zos = (insitu ? null : new ZipOutputStream(fos));
		rezipVendor.initializeDataSet(this);
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
			Object[] typeData = (isIFDMetadataFile ? null : rezipVendor.getExtractTypeInfo(this, baseName, entryName));
			// return here is [String type, Boolean] where Boolean.TRUE means include bytes
			// and Boolean.FALSE means don't include file
			if (typeData != null) {
				isInlineBytes = Boolean.TRUE.equals(typeData[1]);
				isBytesOnly = Boolean.FALSE.equals(typeData[1]);
			}
			boolean doInclude = (isIFDMetadataFile || rezipVendor == null
					|| rezipVendor.doRezipInclude(this, baseName, entryName));
			// cache this one? -- could be a different vendor -- JDX inside Bruker;
			// false for MNova within Bruker? TODO: But wouldn't that possibly have
			// structures?
			// directory, for example
			boolean doCache = (!isIFDMetadataFile && vendorCachePattern != null
					&& (m = vendorCachePattern.matcher(entryName)).find() && phase2cGetParamName(m) != null
					&& ((mgr = getPropertyManager(m, true, true)) == null || mgr.doExtract(entryName)));
			boolean doCheck = (doCache || mgr != null || isIFDMetadataFile);

			len = entry.getSize();
			if (len == 0 || !doInclude && !doCheck)
				continue;
			boolean getBytes = (doCheck || isInlineBytes || insitu);
			OutputStream os = (getBytes ? new ByteArrayOutputStream() : zos);
			String outName = newDir + entryName.substring(lenOffset);
			if (doInclude && !insitu)
				zos.putNextEntry(new ZipEntry(outName));
			FAIRSpecUtilities.getLimitedStreamBytes(ais.getStream(), len, os, false, false);
			byte[] bytes = (getBytes ? ((ByteArrayOutputStream) os).toByteArray() : null);
			if (doCheck) {
				if (doInclude && !insitu)
					zos.write(bytes);
				// have this already; multiple will duplicate the numbered directory
				// this.originPath = oPath + outName;
				this.localizedName = localizedName;
				if (isIFDMetadataFile) {
					addIFDMetadata(new String(bytes));
				} else {
					(mgr == null ? rezipVendor : mgr).accept(null, this.originPath, bytes);
				}
			}
			if (doInclude && !insitu)
				zos.closeEntry();
			if (typeData != null) {
				String key = (String) typeData[0];
				typeData[0] = (isBytesOnly ? null : localName);
				typeData[1] = bytes;
				// extract this file into the collection
				addDeferredPropertyOrRepresentation(key, (isBytesOnly || isInlineBytes ? typeData : localName),
						isInlineBytes || insitu, null, null, "P2c rezipTD");
			}
		}
		rezipVendor.endDataSet();
		if (zos != null)
			zos.close();
		fos.close();
		String dataType = rezipVendor.getVendorDataSetKey();
		len = (noOutput || insitu ? ((ByteArrayOutputStream) fos).size() : outFile.length());
		if (!insitu)
			writeOriginToCollection(oPath, null, len);
		IFDRepresentation r = faHelper.getSpecDataRepresentation(localizedName);
		if (r == null) {
			// probably the case, as this renamed representation has not been added yet.
		} else if (!insitu){
			r.setLength(len);
		}
		if (oPath.endsWith(".zip"))
			oPath = oPath.substring(0, oPath.length() - 4); // remove ".zip"
		addFileAndCacheRepresentation(oPath, localizedName, null, len, dataType, null, "application/zip");
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
	private String phase2cGetParamName(Matcher m) {
		try {
			if (cachePatternHasVendors)
				return m.group("param");
		} catch (Exception e) {
		}
		return null;
	}

	/**
	 * Process the properties in deferredPropertyList after the IFDObject objects
	 * have been created for all resources. This includes writing extracted
	 * representations to files.
	 * 
	 * @throws IFDException
	 * @throws IOException
	 */
	private void phase2dProcessDeferredObjectProperties(String phase2OriginPath) throws IFDException, IOException {
		IFDSample sample = null;
		IFDStructure struc = null;
		IFDDataObject cloneOriginObject = null;
		IFDDataObject localSpec = null;
		IFDRepresentableObject<?> spec = null;
		IFDAssociation assoc = null;
		String lastLocal = null;
		String thisPageInvalidatedStructureRepKey = null;
		String pageCompoundID = null;
		boolean cloning = false;
		for (int i = 0, n = deferredPropertyList.size(); i < n; i++) {
			Object[] a = deferredPropertyList.get(i);
			if (a == null) {
				sample = null;
				cloneOriginObject = null;
				continue;
			}
			assoc = null;
			String originPath = (String) a[0];
			String localizedName = (String) a[1];
			String key = (String) a[2];
			Object value = a[3];
			boolean isInline = (a[4] == Boolean.TRUE);
			switch (key) {
			case NEW_RESOURCE_KEY:
				initializeResource((ExtractorResource) value, false);
				continue;
			case NEW_PAGE_KEY:
				pageCompoundID = null;
				thisPageInvalidatedStructureRepKey = null;
				if (value == IFDProperty.NULL) {
					// done reading MNova file
					cloning = false;
					cloneOriginObject = null;
					continue;
				}
				cloning = true;
				break;
			case IFDExtractor.PAGE_ID_PROPERTY_SOURCE:
				if (pageCompoundID == null)
					pageCompoundID = inferCompoundID((String) value);
				continue;
			}
			boolean isStructureHelperStructureKey = key.startsWith(DefaultStructureHelper.STRUC_FILE_DATA_KEY);
			boolean isRep = IFDConst.isRepresentation(key);
			boolean isStructureRep = (isRep && IFDConst.isStructure(key));
			boolean isWritableRepresentation = (isRep && !isInline && value instanceof Object[]);
			String type = FAIRSpecFindingAidHelper.getObjectTypeForPropertyOrRepresentationKey(key, true);
			boolean isSample = (type == FAIRSpecFindingAidHelper.ClassTypes.Sample);
			boolean isStructure = (type == FAIRSpecFindingAidHelper.ClassTypes.Structure);
			if (isSample) {
				sample = faHelper.getSampleByName((String) value);
				continue;
			}
			// note that MNOVA here would be the same as previous.
			boolean isNew = !localizedName.equals(lastLocal);
			if (isNew) {
				lastLocal = localizedName;
			}
			// link to the originating spec representation -- xxx.mnova, xxx.zip

			// this could pick up the first spectrum in an mnova file.
			String propType = IFDConst.getObjectTypeFlag(key);
			spec = getObjectFromLocalizedName(localizedName, propType);
			if (spec != null && !spec.isValid()) {
				// this is a second -- get first
				// spec = getClonedData(spec);
			}

			if (spec == null && !cloning) {
				// if (!isWritableRepresentation) {
				// just here to notify of an issue
				logDigitalItemIgnored(originPath, localizedName, "no spec data to associate this structure with", "processDeferredObjectProperties");
				continue;
				// }
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
				// Mestrec will add #page1.cdx or #page1.mol
				Object data = null;
				String mediaType = (String) a[5];
				String note = (String) a[6];
				if (isWritableRepresentation) {
					// from DefaultStructureHelper - a PNG version of a CIF file, for example.
					Object[] oval = (Object[]) value;
					byte[] bytes = (byte[]) oval[0];
					String oPath = (String) oval[1];
					String localName = localizePath(oPath);
					if (thisPageInvalidatedStructureRepKey != null && localName.startsWith(thisPageInvalidatedStructureRepKey)) {
						// "NMR DATA/product/3a-H.mnova#page1.cdx.mol"
						// but 3a-C and 3a-H might have the same cdx on different pages
						continue;
					}
					if (!insitu)
						writeOriginToCollection(oPath, bytes, 0);
					if (extractorResource.isDefaultStructurePath) {
						IFDStructureRepresentation rep = (IFDStructureRepresentation) faHelper.getStructureCollection().getRepresentation("" + extractorResource.id, localName);
						if (rep != null)
							rep.setData(bytes);
					}
					addFileToFileLists(localName, LOG_OUTPUT, bytes.length, null);
					value = localName;
					if (insitu || "image/png".equals(mediaType)) {
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
				if (isStructureRep && struc == null) {
					struc = helper.addStructureForSpec(extractorResource.rootPath, (IFDDataObject) spec, key,
							originPath, localizePath(keyPath), null);
				}
				IFDRepresentableObject<?> obj = (isStructureRep ? struc : phase2dGetClonedData(spec));
				linkLocalizedNameToObject(keyPath, null, obj);
				IFDRepresentation r = obj.findOrAddRepresentation(faHelper.getCurrentSource().getID(), originPath,
						extractorResource.rootPath, keyPath, data, key, mediaType);
				if (note != null)
					r.addNote(note);
				if (!isInline)
					setLocalFileLength(r);
				continue;
			}

			// properties only
			if (cloning && key == NEW_PAGE_KEY) {
				String newLocalName = null;
				boolean removeAllRepresentations = false;
				boolean replaceOld = true;
				IFDDataObject specToClone = null;
				// we will clone localSpec
				if (value instanceof String) {
					// e.g. MNova extracted _page=10
					// clear structure for not allowing an MNova structure to carry to next page?
//					struc = null; 
						if (cloneOriginObject == null) {
							// first time through, get the parent MNova object
							// which has the origin information and at least one property.
							specToClone = cloneOriginObject = localSpec;
						} else {
							specToClone = localSpec = cloneOriginObject;
						}
				} else if (value instanceof Object[]) {
					// e.g. Bruker created a new object from multiple <n>/ directories
					a = (Object[]) value;
					value = (String) a[0];
					IFDDataObject sp = (IFDDataObject) a[1];
					removeAllRepresentations = true;
					replaceOld = sp.isValid(); // will be true first time only
					specToClone = localSpec = sp;
					newLocalName = (String) a[2];
				} else {
					System.out.println("L2 ???? How can this be??");
				}
				
				String idExtension = (String) value;
				if (assoc == null && specToClone != null)
					assoc = faHelper.findCompound(null, specToClone);
				IFDDataObject newSpec;
				if (specToClone == null) {
					// I don't think we can get here..
					// second Bruker directory, for example
					specToClone = localSpec = (IFDDataObject) spec;
					replaceOld = false;
				} else {
					// internal Bruker directories
					// or MNova extract
				}
				//specToClone.dumpProperties("local");
				newSpec = faHelper.cloneData(specToClone, idExtension, replaceOld);
				htCloneMap.put(specToClone.getIDorIndex(), newSpec);
				htCloneMap.put("$" + newSpec.getIDorIndex(), specToClone);
				spec = localSpec = newSpec;
				//newSpec.dumpProperties("new");
				struc = faHelper.getFirstStructureForSpec(localSpec, assoc == null);
				if (sample == null)
					sample = faHelper.getFirstSampleForSpec(localSpec, assoc == null);
				if (assoc == null) {
					if (struc != null) {
						faHelper.createCompound(struc, newSpec);
						log("!Structure " + struc + " found and associated with " + spec);
					}
					if (sample != null) {
						faHelper.associateSampleSpec(sample, newSpec);
						log("!Structure " + struc + " found and associated with " + spec);
					}
				} else {
					// we have an association already in Phase 2, and now we need to
					// update that.
					if (removeAllRepresentations)
						newSpec.clear();
					((FAIRSpecCompoundAssociation)assoc).addDataObject(newSpec);
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
			if (isStructureHelperStructureKey) {
				// _struc.cdx,png,mol
				// e.g. extracted xxxx/xxx#page1.mol
				Object[] oval = (Object[]) value;
				byte[] bytes = (byte[]) oval[0];
				String oPath = (String) oval[1];
				String localName = localizePath(oPath);
				String ifdRepType = (oval.length > 2 ? (String) oval[2] : null);
				if (ifdRepType == null)
					ifdRepType = DefaultStructureHelper.getType(key.substring(key.lastIndexOf(".") + 1), bytes, true);
				String name = (oval.length > 2 ? null : phase2dGetStructureNameFromPath(oPath));
				String inchi = (oval.length > 3 ? (String) oval[3] : null);
				// use the byte[] for the structure as a unique identifier.
				AWrap w = new AWrap();
				struc = getCachedStructure(w, bytes, inchi);
				if (struc == null) {
					thisPageInvalidatedStructureRepKey = null;
					// this representation has not been associated with an object yet
					// will have pageN for mnova
					struc = faHelper.getFirstStructureForSpec((IFDDataObject) spec, false);
					if (struc == null) {
						struc = helper.addStructureForSpec(extractorResource.rootPath, (IFDDataObject) spec, ifdRepType,
								oPath, localName, name);
					}
					cacheStructure(w, struc);
					if (!insitu)
						writeOriginToCollection(oPath, bytes, 0);
					if (extractorResource.isDefaultStructurePath) {
						IFDStructureRepresentation rep = (IFDStructureRepresentation) faHelper.getStructureCollection().getRepresentation("" + extractorResource.id, localName);
						if (rep != null)
							rep.setData(bytes);
					}
					
					addFileAndCacheRepresentation(oPath, localName, insitu ? bytes : null, bytes.length, ifdRepType, null,
							null);
					linkLocalizedNameToObject(localName, null, struc);
					if (sample == null) {
						assoc = faHelper.findCompound(struc, (IFDDataObject) spec);
					} else {
						assoc = faHelper.associateSampleStructure(sample, struc);
					}
					// MNova 1 page, 1 spec, 1 structure Test #5
					log("!Structure " + struc + " created and associated with " + spec);
				} else {
					// structure has already been found for this representation
					// invalidate this (cdx/mol) representation and all derived representations (.mol, .png)  
					thisPageInvalidatedStructureRepKey = localName + ".";
					invalidateCachedRepresentation(oPath, localName, struc);
					assoc = faHelper.findCompound(struc, (IFDDataObject) spec);
					if (assoc == null) {
						assoc = faHelper.createCompound(struc, (IFDDataObject) spec);
						log("!Structure " + struc + " found and associated with " + spec);
					}
				}
				if (pageCompoundID != null) {
					assoc.setID(pageCompoundID);
					pageCompoundID = null;
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
					phase2dSetPropertyIfNotAlreadySet(struc, key, value, originPath);
				}
			} else if (isSample) {
				// TODO?
			} else {
				// spec?
				if (key.equals(FAIRSpecExtractorHelper.DATAOBJECT_ORIGINATING_SAMPLE_ID)) {
					helper.addSpecOriginatingSampleRef(extractorResource.rootPath, localSpec, (String) value);
				}
				phase2dSetPropertyIfNotAlreadySet(localSpec, key, value, originPath);
			}
		}
		if (assoc == null) {
			deferredPropertyList.clear();
			deferredPropertyPointer = 0;
			htStructureRepCache = null;
		} else if (cloning) {
			// but why is it the lastLocal?
			vendorCache.remove(lastLocal);
		}
	}

	private static String inferCompoundID(String s) {
	    s = s.toUpperCase();
	    int pt = s.indexOf("COMPOUND ");
	    if (pt < 0)
	    	return null;
	    s = s.substring(pt + 9);
	    pt = s.indexOf(" ");
	    return (pt < 0 ? s : s.substring(0, pt));
	}

	/**
	 * Starting with "xxxx/xx#page1.mol" return "page1".
	 * 
	 * These will be from MNova processing.
	 * 
	 * @param originPath
	 * @return
	 */
	private static String phase2dGetStructureNameFromPath(String originPath) {
		String name = originPath.substring(originPath.lastIndexOf("/") + 1);
		name = name.substring(name.indexOf('#') + 1);
		int pt = name.indexOf('.');
		if (pt >= 0)
			name = name.substring(0, pt);
		return name;
	}

	/**
	 * Get the clone source
	 * @param spec
	 * @return
	 */
	private IFDRepresentableObject<?> phase2dGetClonedData(IFDRepresentableObject<?> spec) {
		if (htCloneMap.isEmpty())
			return spec;
		IFDRepresentableObject<?> d = (spec.isValid() ? null : htCloneMap.get(spec.getIDorIndex()));
		return (d == null ? spec : d);
	}

	private void phase2dSetPropertyIfNotAlreadySet(IFDObject<?> obj, String key, Object value, String originPath) {
		Object currentValue = faHelper.setPropertyValueNotAlreadySet(obj, key, value, originPath);
		if (currentValue != null && !currentValue.equals(value)) {
			String msg = originPath + " property " + key + " can't set value '" + value + "', as it is already set to '"
					+ currentValue + "' from " + obj.getPropertySource(key) + " for " + obj;
			if (key.endsWith("timestamp")) {
				// timestamp is not a big deal
				log(msg);
			} else {
				logWarn(msg, "setPropertyIfNotAlreadySet");

			}
		}
	}

	private void phase2eIterate() throws MalformedURLException, IOException, IFDException {
		ParserIterator iter = new ParserIterator();
		while (iter.hasNext()) {
			nextParser(iter);
			phase2ReadFAIRSpecReadyCollectionIteratively(getTopZipStream(), "", PHASE_2E, null);
		}
	}

	private void phase2eCheckOrReject(ArchiveInputStream ais, String oPath, long len) throws IOException {
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
	 * From DefaultVendorPlugin.addProperty(String, Object)
	 * 
	 * or 
	 * 
	 * phase2cRezipEntry (for IFDConst.IFD_PROPERTY_DATAOBJECT_NOTE)
	 * 
	 * 
	 */
	@Override
	public void addProperty(String key, Object val) {
		if (val != IFDProperty.NULL)
			log(this.localizedName + " addProperty " + key + "=" + val);
		addDeferredPropertyOrRepresentation(key, val, false, null, null, "L0 vndaddprop");
	}

	/**
	 * Cache the property or representation created by an IFDVendorPluginI class or
	 * returned from the DefaultStructureHelper for later processing. This method is
	 * 
	 * addProperty(...) from IFDVendorPluginI.accept(...)
	 * 
	 * addProperties(...) from phase2aProcessEntry(...) or phase2cRezipEntry 
	 * 
	 * initializeResource(...) [NEW_RESOURCE_KEY]
	 * 
	 * phase2cRezipEntry(...) [NEW_PAGE_KEY]
	 * 
	 * DefaultStructureHelper.processRepresentation(...) only.
	 * 
	 * @param key       representation or property key; the key "_struc" is used by
	 *                  a vendor plugin to pass back both a file name and a byte
	 *                  array to create a new digital object extracted from the
	 *                  original object, for example, from an MNova object
	 *                  extraction; special keys include NEW_RESOURCE_KEY and NEW_PAGE_KEY
	 * @param val       either a String value or an Object[] with elements byte[]
	 *                  and String name
	 * @param mediaType a media type for a representation, or null
	 * @param isInline  representation data is being provided as inline-data, to be
	 *                  saved only in the finding aid (InChI, SMILES, InChIKey)
	 */
	@Override
	public void addDeferredPropertyOrRepresentation(String key, Object val, boolean isInline, String mediaType,
			String note, String method) {
		
		//System.out.println("L2 adddef >>>" +key + " src=" + method + " " + val);
		if (key == null) {
			deferredPropertyList.add(deferredPropertyPointer++, null);
			return;
		}
		deferredPropertyList
				.add(deferredPropertyPointer++, new Object[] { originPath, localizedName, key, val, Boolean.valueOf(isInline), mediaType, note, method });
		if (key.startsWith(DefaultStructureHelper.STRUC_FILE_DATA_KEY)) {
			// Phase 2a has identified a structure before a compound has been established in Phase 2b.
			// Mestrelab vendor plug-in has found a MOL or SDF file in Phase 2b.
			
			// val is Object[] {byte[] bytes, String name}
			// Pass data to structure property manager in order
			// to add (by coming right back here) InChI, SMILES, and InChIKey.
			Object[] oval = (Object[]) val;
			byte[] bytes = (byte[]) oval[0];
			String name = (String) oval[1]; // must not be null
			String type = (oval.length > 2 ? (String) oval[2] : null);
			String standardInChI = (oval.length > 3 ? (String) oval[3] : null);
			standardInChI = getStructurePropertyManager().processStructureRepresentation(name, bytes, type, standardInChI, false, true);
			if (standardInChI != null) {
				oval[3] = standardInChI;
			}
		}
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
	private CacheRepresentation addFileAndCacheRepresentation(String originPath, String localizedName, Object data, long len,
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
		CacheRepresentation rep = new CacheRepresentation(new IFDReference(faHelper.getCurrentSource().getID(),
				originPath, extractorResource.rootPath, localizedName), data, len, ifdType, mediaType);
		vendorCache.put(localizedName, rep);
		return rep;
	}

	/**
	 * As duplicate structure representation has been found. 
	 * Perhaps from two different pages in an MNova file.
	 * 
	 * @param localName
	 * @param struc
	 */
	private void invalidateCachedRepresentation(String originPath, String localizedName, IFDStructure struc) {
		CacheRepresentation rep = new CacheRepresentation(new IFDReference(faHelper.getCurrentSource().getID(),
				originPath, extractorResource.rootPath, localizedName), null, 0, null, null);
		rep.isValid = false;
		vendorCache.put(localizedName, rep);
	}



	/**
	 * Add metadata from a simple file (default IFD_METADATA) with key=value pairs
	 * one per line.
	 * 
	 * @param data
	 */
	private void addIFDMetadata(String data) {
		addProperties(FAIRSpecUtilities.getIFDPropertyList(data));
	}

//	private IFDRepresentableObject<?> getClonedDataSource(IFDRepresentableObject<?> spec) {
//		IFDRepresentableObject<?> d = (spec.isValid() ? null : htCloneMap.get("$" + spec.getID()));
//		return d;
//	}

	/**
	 * phases 2c and 2d, and 3a
	 * 
	 * @param name
	 * @param type
	 * @return
	 */
	protected IFDRepresentableObject<?> getObjectFromLocalizedName(String name, String type) {
		IFDRepresentableObject<?> obj = (type == null ? null : htLocalizedNameToObject.get(type + name));
		return (obj == null ? htLocalizedNameToObject.get(name) : obj);
	}

	/**
	 * phase 2c and 2e
	 * 
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	private InputStream getTopZipStream() throws MalformedURLException, IOException {
		return (localizedTopLevelZipURL.endsWith("/") ? new DirectoryInputStream(localizedTopLevelZipURL)
				: new URL(localizedTopLevelZipURL).openStream());
	}

	/**
	 * Set up fields for a new source of files. This method sets fields,
	 * particularly localizedTopLevelZipURL and extractorResource. (thisRootPath is
	 * only used for logging.)
	 * 
	 * @param resource
	 * @param isInit
	 * @throws IFDException
	 * @throws IOException
	 */
	private void initializeResource(ExtractorResource resource, boolean isInit) throws IFDException, IOException {
		// localize the URL if we are using a local copy of a remote resource.
		localizedTopLevelZipURL = localizeURL(resource.getSourceFile());
		String s = resource.getSourceFile();
		String zipPath = s.substring(s.lastIndexOf(":") + 1);
		if (isInit) {

			if (debugging)
				log("opening " + localizedTopLevelZipURL);
			String rootPath = resource.createZipRootPath(zipPath);
			if (!insitu) {
				new File(targetDir + "/" + rootPath).mkdir();
			}
			resource.setLists(rootPath, ignoreRegex, acceptRegex);

		}
		lstManifest = resource.lstManifest;
		lstIgnored = resource.lstIgnored;
		if (faHelper.getCurrentSource() != faHelper.addOrSetSource(resource.getRemoteSource(), resource.rootPath)) {
			if (isInit)
				addDeferredPropertyOrRepresentation(NEW_RESOURCE_KEY, resource, false, null, null, null);
		}
		extractorResource = resource;

		// only used for logging:
		thisRootPath = resource.rootPath;
	}

	/**
	 * Link one or more local names and type to an object such as
	 * a spectrum or structure. Later in the process, this representation will be
	 * added to the object.
	 * 
	 * @param localizedName
	 * @param type
	 * @param obj
	 * @throws IOException
	 */
	private void linkLocalizedNameToObject(String localizedName, String type, IFDRepresentableObject<?> obj)
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

	private ObjectParser nextParser(ParserIterator iter) {
		ObjectParser parser = iter.next();
		if (parser.getDataSource() != extractorResource) {
			try {
				initializeResource(parser.getDataSource(), true);
			} catch (IFDException | IOException e) {
				e.printStackTrace();
			}
		}
		return parser;
	}

	private InputStream openLocalFileInputStream(URL url, long[] retLength) throws IOException {
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
			extractorResource.setTemp(localizedTopLevelZipURL);
			is = new BufferedInputStream(new FileInputStream(tempFile));
		}
		return is;
	}

	/**
	 * A new 
	 * Pull a full row from a file such as Manifest.xlsx with headers including a
	 * compound numbers, for example.
	 * 
	 * Called from FAIRSpecExtractorHelper.addObject and checkAddNewObject
	 * 
	 * @param o an IFDObject such as a Structure, DataObject, or Compound
	 * @param idKey one of the .id keys for the object
	 */
	@Override
	public void setSpreadSheetMetadata(IFDObject<?> o, String idKey) {
		Map<String, Object> map;
		List<Object[]> metadata;
		if (htSpreadsheetMetadata == null || o.getID() == null || (map = htSpreadsheetMetadata.get(idKey)) == null
				|| !SpreadsheetReader.hasDataKey(map)
				|| (metadata = SpreadsheetReader.getRowDataForIndex(map, o.getIDorIndex())) == null)
			return;
		log("!Extractor adding " + metadata.size() + " metadata items for " + idKey + "=" + o.getIDorIndex());
		FAIRSpecExtractorHelper.addProperties(o, metadata);
	}

	private void setupTargetDir(File dir) {
		dir.mkdir();
		new File(dir + "/_IFD_warnings.txt").delete();
		new File(dir + "/_IFD_rejected.json").delete();
		new File(dir + "/_IFD_ignored.json").delete();
		new File(dir + "/_IFD_manifest.json").delete();
		new File(dir + "/IFD.findingaid.json").delete();
		new File(dir + "/IFD.collection.zip").delete();
		this.targetDir = dir;
	}

	/**
	 * Phase 2a, 2c, and 2d
	 * 
	 * @param originPath
	 * @param bytes
	 * @param len
	 * @throws IOException
	 */
	private byte[] writeOriginToCollection(String originPath, byte[] bytes, long len) throws IOException {
		lstWritten.add(localizePath(originPath), (bytes == null ? len : bytes.length));
		if (insitu) {
			return bytes; 
		}
		if (!noOutput && bytes != null)
			writeBytesToFile(bytes, getAbsoluteFileTarget(originPath));
		return null;
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
			  lstIgnored.add(fileName, len);
			  // though ignored, still added to collection
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
			if (noOutput || !includeIgnoredFiles || ais == null)
				return;
			break;
		case LOG_OUTPUT:
			lstManifest.add(localizedName, len);
			break;
		}
		if (insitu)
			return;
		File f = getAbsoluteFileTarget(localizedName);
		FAIRSpecUtilities.getLimitedStreamBytes(ais, len, new FileOutputStream(f), false, true);
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
	 * Ensure that we have a correct length in the metadata for this representation.
	 * as long as it exists, even if we are not writing it in this pass.
	 * 
	 * @param rep
	 */
	protected long setLocalFileLength(IFDRepresentation rep) {
		String name = rep.getRef().getLocalName();
		if (name.indexOf("#") >= 0)
			return -1;
		long len = lstWritten.getLength(name);
		rep.setLength(len);
		return len;
	}

	/**
	 * Get the full OS file path for FileOutputStream
	 * 
	 * @param originPath
	 * @return
	 */
	private File getAbsoluteFileTarget(String originPath) {
		return new File(targetDir + "/" + extractorResource.rootPath + "/" + localizePath(originPath));
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
	private static String localizePath(String path) {
		path = path.replace('\\', '/');
		boolean isDir = path.endsWith("/");
		if (isDir)
			path = path.substring(0, path.length() - 1);
		int pt = -1;
		while ((pt = path.indexOf('|', pt + 1)) >= 0)
			path = path.substring(0, pt) + ".." + path.substring(++pt);
		return path.replace('/', '_').replace('#', '_').replace(' ', '_') + (isDir ? ".zip" : "");
	}

}
