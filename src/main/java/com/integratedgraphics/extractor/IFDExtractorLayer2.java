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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
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
import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities.DeferredProperty;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities.SpreadsheetReader;
import org.iupac.fairdata.core.IFDAssociation;
import org.iupac.fairdata.core.IFDObject;
import org.iupac.fairdata.core.IFDProperty;
import org.iupac.fairdata.core.IFDReference;
import org.iupac.fairdata.core.IFDRepresentableObject;
import org.iupac.fairdata.core.IFDRepresentation;
import org.iupac.fairdata.dataobject.IFDDataObject;
import org.iupac.fairdata.extract.DefaultStructureHelper;
import org.iupac.fairdata.extract.DefaultStructureHelper.StructureData;
import org.iupac.fairdata.extract.PropertyManagerI;
import org.iupac.fairdata.sample.IFDSample;
import org.iupac.fairdata.structure.IFDStructure;
import org.iupac.fairdata.structure.IFDStructureRepresentation;

import com.integratedgraphics.extractor.ExtractorUtils.AWrap;
import com.integratedgraphics.extractor.ExtractorUtils.ArchiveEntry;
import com.integratedgraphics.extractor.ExtractorUtils.ArchiveInputStream;
import com.integratedgraphics.extractor.ExtractorUtils.AutomationParser;
import com.integratedgraphics.extractor.ExtractorUtils.CacheRepresentation;
import com.integratedgraphics.extractor.ExtractorUtils.DirectoryInputStream;
import com.integratedgraphics.extractor.ExtractorUtils.ExtractorResource;
import com.integratedgraphics.extractor.ExtractorUtils.ObjectParser;
import com.integratedgraphics.ifd.api.DataObjectVendorPluginI;

/**
 * Phase 2: Carry out the actual extraction of metadata and creation of the
 * collection model.
 * 
 * Set up the target dirrectory for files:
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
 * built. Metadata is extracted from files such as MNova and Jeol jfl files, but
 * not from datasets that will be rezipped -- specifically Bruker and , and
 * digital item bytes are not checked. Only the filenames are checked.
 * 
 * Process files in the FAIRSpec-ready collection, extracting metadata and
 * representations, but do not create any Finding Aid objects or collections.
 * 
 * Scan zip entries or directories for representations of structures and
 * spectral data, extracting representations such as MOL, JDX, PDF, embedded
 * MNova.
 * 
 * Some file structures need to be fixed. Specifically, the name of the Bruker
 * directory containing acqu must be a simple integer, or the dataset cannot be
 * read back into TopSpin. Not all authors realize this. So we flag those for
 * fixing. These datasets are identified here, but they are not (re)zipped until
 * Phase 2c.
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
	private final static String PHASE_2B = "2b";
	private final static String PHASE_2C = "2c";
	private final static String PHASE_2D = "2d";
	private final static String PHASE_2E = "2e";

	/**
	 * list of files extracted
	 */
	protected FileList lstManifest;

	/**
	 * the resource currently being processed in Phase 2 or 3.
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
	protected Map<String, IFDRepresentableObject<?>> htCloneMap = new HashMap<>();

	/**
	 * a list of properties that vendors have indicated need addition, keyed by the
	 * zip path for the resource
	 */
	private List<DeferredProperty> deferredPropertyList;

	/**
	 * an insertion pointer into the deferredPropertyList; reset to 0 after Phase
	 * 2b.
	 */
	private int deferredPropertyPointer;

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
	 * rezip data saved as an IFDRepresentation
	 */
	private ExtractorUtils.CacheRepresentation currentRezipRepresentation;

	/**
	 * path to this resource in the original zip file
	 */
	private String currentRezipPath;

	private String currentRezipLocalPath;

	/**
	 * vendor association with this rezipping
	 */
	private DataObjectVendorPluginI currentRezipVendor;

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

	@SuppressWarnings("serial")
	private static class RezipCache extends ArrayList<ExtractorUtils.CacheRepresentation> {
		private final static int MAX_DEPTH = 4;

		boolean containsPath(String path) {
			for (int i = 0, n = Math.min(size(), MAX_DEPTH); i < n; i++) {
				if (get(i).getRef().getOriginPath().equals(path))
					return true;
			}
			return false;
		}
	}

	/**
	 * cache of top-level resources to be rezipped
	 */
	private RezipCache rezipCache;

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
	private String lastLocalClonedParent;
	protected File propertyListFile;
	protected boolean dumpPropertyList = true;

	private String thisCompoundDirID;
	private String thisStructureDirID;
	private boolean deferringProperties;

	/**
	 * The main extraction phase. Find and extract all objects of interest from a
	 * ZIP file.
	 * 
	 */
	protected void processPhase2() throws IFDException, IOException {
		if (haveExtracted)
			throw new IFDException("Only one extraction per instance of Extractor is allowed (for now).");
		haveExtracted = true;
		deferringProperties = true;
		// String s = "test/ok/here/1c.pdf"; // test/**/*.pdf
		// Pattern p = Pattern.compile("^\\Qtest\\E/(?:[^/]+/)*(.+\\Q.pdf\\E)$");
		// Matcher m = p.mat cher(s);
		// log(m.find() ? m.groupCount() + " " + m.group(0) + " -- " + m.group(1) : "");

		log("=====");

		if (logging()) {
			if (localSourceDir != null)
				log("extractObjects from " + localSourceDir);
			log("extractObjects to " + targetPath.getAbsolutePath());
		}

		// Note that some files have multiple objects.
		// These may come from multiple sources, or they may be from the same source.
		deferredPropertyList = new ArrayList<>();
		rezipCache = new RezipCache();

		// There is one parser created for each of the IFD-extract.json
		// "FAIRSpec.extractor.object" records.
		// Each of phases 2a-e iterate over these records.

		// Phase 2a -- preliminary extractions
		phase2aProcess();

		// Phase 2b -- create objects, including associations
		phase2bProcess();

		// Phase 2c -- rezipping
		phase2cProcess();

		// Phase 2d - deferred properties processing
		phase2dProcess();

		// Phase 2e -- final check for completeness
		phase2eProcess();
	}

	/**
	 * Phase 2a -- preliminary extraction
	 * 
	 * Start creating the IUPAC FAIRSpec Data Collection.
	 * 
	 * Scan the FAIRSpec-ready collection based on the configuration files
	 * IFD-extract.json and extractor.config.json.
	 * 
	 * Extract data items, for example JEOL .jfd files, MNova files, Bruker data set
	 * .png files, and structure files.
	 * 
	 * Extract all metadata and embedded representations from MNova files, and
	 * generate all related metadata for structure files (InChI, SMILES, PNG)
	 * 
	 * Saving all metadata for later proccessing as "deferred" until after rezipping
	 * has concluded (Phase 2c).
	 * 
	 * The finding aid objects themselves are not created until Phase 2b.
	 * 
	 * 
	 * @throws IOException
	 * @throws IFDException
	 */
	private void phase2aProcess() throws IOException, IFDException {
		setCurrentPhase(PHASE_2A);
		phase2aIterate(htArchiveContents);
		checkStopAfter(PHASE_2A);
	}

	/**
	 * Phase 2b -- create objects, including associations
	 * 
	 * Run through the FAIRSpec-ready collection again, this time just using the zip
	 * entry map, creating objects in the Finding Aid.
	 * 
	 * This will also add metadata from a spreadsheet file.
	 * 
	 * @throws IOException
	 * @throws IFDException
	 */
	private void phase2bProcess() throws IOException, IFDException {
		setCurrentPhase(PHASE_2B);
		log("!Phase 2b create IFDObjects from the FAIRSpec-ready collection");
		phase2bIterate(htArchiveContents);
		checkStopAfter(PHASE_2B);
	}

	/*
	 * Phase 2c -- rezipping
	 * 
	 * All objects have been created.
	 * 
	 * An important feature of Extractor is that it can repackage zip files,
	 * rejecting data items that are totally unnecessary (.icloud, __MACOS,
	 * .desktop).
	 * 
	 * Here we extract additional properties and representations using
	 * IFDVendorPluginI services.
	 * 
	 */
	private void phase2cProcess() throws MalformedURLException, IOException, IFDException {
		setCurrentPhase(PHASE_2C);
		log("!Phase 2c process all rezippable datasets and create their IUPAC FAIRSpec Data Collection");
		if (rezipCache != null && rezipCache.size() > 0) {
			System.out.println("rezip cache:\n" + rezipCache.toString().replace("],", "]\n"));

			// reset the pointer to zero, adding any
			// additional properties BEFORE what is already on the list from Phase 2a or 2b.
			resetDeferredPropertyList(false);
			// System.out.println(rezipCache.toString().replace(',', '\n'));
			phase2cGetNextRezipName();
			lastRezipPath = null;
			phase2cIterate();
			for (int i = rezipCache.size(); --i >= 0;)
				logWarn("rezip not accomplished for " + rezipCache.get(i), "phase2cProcess");
		}
		checkStopAfter(PHASE_2C);
	}

	/**
	 * Phase 2d - deferred properties processing
	 * 
	 * @throws IOException
	 * @throws IFDException
	 * 
	 */
	private void phase2dProcess() throws IFDException, IOException {
		setCurrentPhase(PHASE_2D);
		log("!Phase 2d process all deferred object properties");
		phase2dProcessDeferredObjectProperties(null);
		deferringProperties = false;
		checkStopAfter(PHASE_2D);
	}

	/**
	 * Phase 2e -- final check for completeness
	 * 
	 * Ensure that all files in the FAIRSpec-ready collection have been processed,
	 * or flag them as rejected or ignored.
	 * 
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws IFDException
	 */
	private void phase2eProcess() throws MalformedURLException, IOException, IFDException {
		setCurrentPhase(PHASE_2E);
		log("!Phase 2e check rejected files and update lists");
		phase2eIterate();
		checkStopAfter(PHASE_2E);
	}

	private static boolean isTopZipDir(String originPath) {
		int ptzip = originPath.lastIndexOf('|');
		return (ptzip >= 0 && originPath.indexOf('/', ptzip + 1) < 0);
	}

	/**
	 * Process all entries in a zip file, looking for files to extract and
	 * directories to rezip. This method is called at different phases in the
	 * extraction.
	 * 
	 * 
	 * @param is               the InputStream
	 * @param baseOriginPath   a path ending in "zip|"
	 * @param phase            2a,2c,2e
	 * @param phase2aOriginToEntryMap a map to return of name to ZipEntry; only for Phase
	 *                         2a
	 * 
	 * @throws IOException
	 * @throws IFDException
	 */

	private void phase2ReadFAIRSpecReadyCollectionIteratively(InputStream is, String baseOriginPath, String phase,
			Map<String, ArchiveEntry> phase2aOriginToEntryMap, int level) throws IOException, IFDException {
		if (debugging && baseOriginPath.length() > 0)
			log("! opening " + baseOriginPath);
		boolean isTopLevel = (baseOriginPath.length() == 0);
		ArchiveInputStream ais = new ArchiveInputStream(is, isTopLevel ? extractorResource.getSourceFile() : null,
				level);
		if (ais.err != null) {
			logWarn(ais.err, "phase2ReadFAIRSpecReadyCollectionIterativey");
		}
		ArchiveEntry zipEntry = null;
		ArchiveEntry firstFileEntry = null;
		int n = 0;
		boolean lookingForDirectories = (phase != PHASE_2E);
		int dirPt = 0;
		String firstFileName = null;
		Set<String> dirlist = new HashSet<>();
		boolean haveDirs = true;
		Stack<ArchiveEntry> entryStack = new Stack<>();
		Stack<ArchiveEntry> rezipStack = new Stack<>();
		while ((zipEntry = (entryStack.size() > 0 ? entryStack.remove(0)
				: firstFileEntry != null ? firstFileEntry : ais.getNextEntry())) != null) {
			String name = zipEntry.getName();
			if (name == null)
				continue;
			n++;
			boolean isDir = zipEntry.isDirectory();
			long size = (isDir ? 0 : zipEntry.getSize());
			if (!isDir && !haveDirs) {
				// a zip file with no directories will need to be checked repeatedly
				int pt = name.lastIndexOf('/');
				if (pt > 0) {
					String thisdir = name.substring(0, pt);
					if (!dirlist.contains(thisdir)) {
						dirlist.add(thisdir);
						dirPt = 0;
					}
				}
			}
			lookingForDirectories = (dirPt >= 0 && zipEntry != firstFileEntry);
			if (lookingForDirectories) {
				if (isDir) {
					dirPt = (firstFileName == null ? -1 : firstFileName.indexOf('/', dirPt + 1));
					// still cycling or this is fine
					// set up for next
					if (dirPt > 0) {
						String dirName = firstFileName.substring(0, dirPt + 1);
						entryStack.add(0, new ArchiveEntry(dirName));
					}
				} else {
					haveDirs = false;
					// first time only and not a directory;
					// a/b/c/d...
					dirPt = name.indexOf('/', dirPt + 1);
					if (dirPt > 0) {
						firstFileEntry = zipEntry;
						firstFileName = name;
						// ARGH! Implicit top directory
						// save this file entry
						// set to first directory
						entryStack.add(0, new ArchiveEntry(firstFileName.substring(0, dirPt + 1)));
					} else {
						switch (phase) {
						case PHASE_2C:
							if (baseOriginPath.equals(currentRezipPath)) {
								ArchiveEntry nextEntry = phase2cRezipEntry(baseOriginPath, baseOriginPath, ais,
										zipEntry, zipEntry, currentRezipVendor, level);
								if (nextEntry != null)
									entryStack.add(nextEntry);
								continue;
							}
						}
					}
				}
			}
			String oPath = baseOriginPath + name;
			boolean accepted = false;
			if (isDir) {
				if (haveDirParser) {
					// this is not implemented
					phase2aCheckCompoundStructureDir(baseOriginPath + name);
				}
				if (logging())
					log("Phase " + phase + " checking zip directory: " + n + " " + oPath);
			} else {
				dirPt = -1;
				firstFileEntry = null;
				if (size == 0)
					continue;
				// only PHASE_2A processes accepted
				if (phase == PHASE_2A)
					accepted = phase2aCheckRejected(phase, oPath, size, ais);
			}
			if (debugging)
				log("reading zip entry: " + n + " " + oPath);

			if (phase2aOriginToEntryMap != null) {
				phase2aOriginToEntryMap.put(oPath, zipEntry);
			}
			if (FAIRSpecUtilities.isZip(oPath)) {
				// iteratively check zip files if not in the final checking phase
				phase2ReadFAIRSpecReadyCollectionIteratively(ais, oPath + "|", phase, phase2aOriginToEntryMap, level + 1);
			} else {
				switch (phase) {
				case PHASE_2A:
					if (!isDir) {
						phase2aProcessEntry(baseOriginPath, oPath, ais, zipEntry, accepted);
					}
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
						// In addition, this might be the "nextEntry" from the
						// last rezipping, in which case it will be on the entryStack.
						if (entryStack.size() > 0)
							firstFileEntry = entryStack.remove(0);
						ArchiveEntry nextEntry = phase2cRezipEntry(baseOriginPath, oPath, ais, zipEntry, firstFileEntry,
								currentRezipVendor, level);
						firstFileEntry = null;
						if (rezipStack.size() > 0)
							entryStack.add(rezipStack.remove(0));
						if (nextEntry != null)
							entryStack.add(nextEntry);
						// System.out.println("next entry is " + nextEntry);
						continue;
					} else if (rezipCache.containsPath(oPath)) {
						// System.out.println(rezipCache.toString().replace(',','\n'));
						// Bruker directory within a Bruker directory!
						// jo4c02622 has a triple nesting. (We allow here for up to four nestings)
						// The rezip path order is
						// /1n/1n 13C/2/2.zip
						// /1n/1n 13C/2.zip
						// /1n/1n 13C.zip
						// because the second "2" comes before "acqu", and thus
						// 2/2/acqu was found before 2/acqu before acqu
						// meaning the inner file will be zipped up before the outer one.
						// This is OK, but then after it is done, the nextEntry will be
						// /1n/1n 13C/2/acqu
						// instead of /1n/1n 13C/2, and the ordering is lost.
						// The result was no further processing of the rezip list.

						// SOLUTION:
						// entryStack --- 0-remove-first stack of pending ZipEntry items
						// rezipStack --- 0-remove-first stack of pengding rezip objects
						//
						// If path is found that is on the stack but not NEXT on the stack (element 0)
						// (a) add this path to the the head of the rezip stack.
						// (b) after the next-run rezipping operation,
						// (b1) place this entry at the head of the entry stack
						// (b2) place the "nextEntry" (already read) from the rezipping operation
						// ("acqu") next on the entry stack
						//
						// The result will be the rezipping carried out in the correct order,
						// each time retrieving first the next rezip directory, then the already-read
						// "nextEntry".
						//

						rezipStack.add(0, zipEntry);
					}
					break;
				case PHASE_2E:
					// final check
					if (!isDir)
						phase2eCheckOrReject(ais, oPath, zipEntry.getSize());
					break;
				}
			}
		}
		if (isTopLevel)
			ais.close();
	}

	/**
	 * Early idea for defining structure and compound directories; but this was
	 * never implemented. Instead, we have the default structures/ dir only.
	 * 
	 * @param dir
	 * @throws IFDException
	 */
	private void phase2aCheckCompoundStructureDir(String dir) throws IFDException {
		int len = dir.length();
		if (compoundDirParser != null) {
			if (compoundDirParser.match(dir).find()) {
				int pt = dir.lastIndexOf("/", len - 2);
				String cdir = thisCompoundDirID = dir.substring(pt, len - 1);
				if (structureDirParser == compoundDirParser) {
					thisStructureDirID = cdir;
					return;
				}
			}
		}
		if (structureDirParser != null) {
			if (structureDirParser.match(dir).find()) {
				int pt = dir.lastIndexOf("/", len - 2);
				thisStructureDirID = dir.substring(pt, len - 1);
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
	private DataObjectVendorPluginI phase2aGetVendorForRezip(Matcher m) {
		for (int i = bsRezipVendors.nextSetBit(0); i >= 0; i = bsRezipVendors.nextSetBit(i + 1)) {
			String ret = m.group("rezip" + i);
			if (ret != null && ret.length() > 0) {
				return DataObjectVendorPluginI.activeVendors.get(i).vendor;
			}
		}
		return null;
	}

	private void phase2aIterate(Map<String, Map<String, ArchiveEntry>> contents) throws IOException, IFDException {

		// Scan through parsers for resource changes

		ParserIterator iter = new ParserIterator();
		log("!Phase 2a initializing data sources ");
		ExtractorResource currentSource = extractorResource;
		while (iter.hasNext()) {
			while (iter.hasNext() && nextParser(iter) != null && extractorResource == currentSource) {
				// skip to next resource
			}
			// this will set the automationParser if it exists
			currentSource = extractorResource;
			if (!phase2aNewExtractorResource())
				continue;

			// first build the file list
			// Scan URL zip stream for files.
			log("!retrieving " + localizedTopLevelZipURL);
			URL url = new URL(localizedTopLevelZipURL);
			// for JS
			long[] retLength = new long[1];
			InputStream is = phase2aOpenLocalZipFileInputStream(url, retLength);
			long len = retLength[0];
			if (len > 0)
				faHelper.setCurrentResourceByteLength(len);
			Map<String, ArchiveEntry> zipFileMap = new LinkedHashMap<String, ArchiveEntry>();
			phase2ReadFAIRSpecReadyCollectionIteratively(is, "", PHASE_2A, zipFileMap, 0);
			contents.put(localizedTopLevelZipURL, zipFileMap);
		}
	}

	private InputStream phase2aOpenLocalZipFileInputStream(URL url, long[] retLength) throws IOException {
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
			log("!downloading " + url + " as " + tempFile);
			FAIRSpecUtilities.getLimitedStreamBytes(url.openStream(), -1, new FileOutputStream(tempFile), true, true);
			log("!downloaded " + tempFile.length() + " bytes");
			retLength[0] = tempFile.length();
			extractorResource.setTemp(localizedTopLevelZipURL);
			is = new BufferedInputStream(new FileInputStream(tempFile));
		}
		return is;
	}

	/**
	 * Add the source to rootPaths only if not already seen.
	 * 
	 * @return true if this is a new resource that needs processing
	 * 
	 * @throws IOException
	 */
	private boolean phase2aNewExtractorResource() throws IOException {
		String source = targetPath + "/" + extractorResource.rootPath;
		if (rootPaths.contains(source))
			return false;
		rootPaths.add(source);
		if (cleanCollectionDir) {
			File dir = new File(source);
			log("!Phase 2a cleaning directory " + dir);
			FileUtils.cleanDirectory(dir);
		}
		return true;
	}

	/**
	 * Phase 2a check to see what should be done with a zip entry. We can extract it
	 * or ignore it; and we can check it to sees if it is the trigger for extracting
	 * a zip file in a second pass.
	 * 
	 * @param originPath path to this entry including | and / but not rootPath
	 * @param ais
	 * @param zipEntry
	 * @param accepted   file name has been accepted by lstAccepted
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws IFDException
	 */
	private void phase2aProcessEntry(String baseOriginPath, String originPath, InputStream ais, ArchiveEntry zipEntry,
			boolean accepted) throws FileNotFoundException, IOException, IFDException {
		long len = zipEntry.getSize();
		Matcher managerMatcher = null;

		// check for files that should be pulled out - these might be JDX files, for
		// example.
		// "param" appears if a vendor has flagged these files for parameter extraction.

		boolean isFound = false;
		if (propertyManagerCachePattern != null
				&& (isFound = (managerMatcher = propertyManagerCachePattern.matcher(originPath)).find()) || accepted) {
			PropertyManagerI mgr = (isFound ? getPropertyManager(managerMatcher, true, true) : null);
			boolean haveManager = (mgr != null);
			boolean isStructure = (haveManager && mgr == structurePropertyManager);
			boolean doExtract = (!haveManager || mgr.doExtract(originPath));
			// Note that we only deal here with "extractable" files.
			// In the case of Bruker datasets, which will be repackaged in Phase 2c,
			// this does NOT include acqu, for example.

//			1. we don't have params, but we want the file extracted 
//		      - generic file, just save it.  doExtract and not doCheck
//			2. we have params and there is extraction
//		      - save file and also check it for parameters  doExtract and doCheck
//			3. nothing to check and no extraction  !doCheck  and !doExtract
//		      - ignore completely

			if (doExtract) {
				String localizedName = localizePath(originPath);
				if (haveManager && automationParser != null) {
					phase2aAutomationEnsureCompoundForManagedPath(originPath, localizedName, mgr.getType());
				}
				String ext = (haveManager ? managerMatcher.group("ext")
						: originPath.substring(originPath.lastIndexOf(".") + 1));
				File f = getAbsoluteFileTarget(originPath);
				boolean embeddableImage = originPath.endsWith(".png");
				boolean embedOnly = (insitu || embedPDF && originPath.endsWith(".pdf"));
				boolean toByteArray = (embeddableImage || haveManager || noOutput || embedOnly);
				boolean doWrite = toByteArray && !embedOnly;
				OutputStream os = (toByteArray ? new ByteArrayOutputStream() : new FileOutputStream(f));
				if (os != null)
					FAIRSpecUtilities.getLimitedStreamBytes(ais, len, os, false, true);
				String type = null;
				byte[] bytes = null;
				if (toByteArray) {
					// haveManager or noOutput or insitu
					bytes = ((ByteArrayOutputStream) os).toByteArray();
					len = bytes.length;
					if (haveManager) {
						// set this.localizedName for parameters
						// preserve this.localizedName, as we might be in a rezip.
						// as, for example, a JDX file within a Bruker dataset
						String oldOriginPath = this.originPath;
						String oldLocal = this.localizedName;
						this.originPath = originPath;
						this.localizedName = localizedName;
						// indicating "this" here notifies the vendor plug-in that
						// this is a one-shot file, not a collection.
						if (logging())
							log("Phase 2a accepting " + originPath);
						boolean isIFDMetadataFile = zipEntry.getName().endsWith(ifdRelatedMetadataFileName);
						type = (isIFDMetadataFile ? IFDConst.IFD_PROPERTY_FLAG
								: isStructure && hasStructureFor(bytes) ? IFDProperty.NULL
										: mgr.accept(this, originPath, bytes));

						if (type == IFDConst.IFD_PROPERTY_FLAG) {
							// IFD metadata file
							// Q: What triggers this? Nothing?
							// could be accepting a metadata file?
							addIFDMetadata(new String(bytes));
							// this is now handled in Phase 2c
						} else {
							addDeferredPropertyOrRepresentation(null);
							this.localizedName = oldLocal;
							this.originPath = oldOriginPath;
							if (type == null) {
								logWarn("Failed to read " + originPath + " (ignored)", mgr.getClass().getName());
							} else if (type == IFDProperty.NULL) {
								lstIgnored.add(originPath, len);
								logDigitalItemIgnored(originPath, localizedName, "phase 2a equivalent structure",
										"phase2a");
								return;
							} else if (IFDConst.isStructure(type)
									|| type.startsWith(DefaultStructureHelper.STRUC_FILE_DATA_KEY)) {
								return;
							}
						}
					}
				} else {
					doWrite = true;
					len = f.length();
				}
				if (doWrite)
					writeOriginToCollection(originPath, bytes, len);
				addFileAndCacheRepresentation(originPath, localizedName, embedOnly || embeddableImage ? bytes : null,
						len, type, ext, null);
			}
		}

		// here we look for the "trigger" file within a zip file that indicates that we
		// (may) have a certain vendor's files that need looking into. The case in point
		// is finding a procs file within a Bruker data set. Or, in principle, an acqus
		// file and just an FID but no pdata/ directory. But for now we want to see that
		// processed data.

		if (rezipCachePattern != null && (managerMatcher = rezipCachePattern.matcher(originPath)).find()) {

			// e.g. exptno/pdata/1/procs or xxxx/xxx/fid.zip|procs

			DataObjectVendorPluginI v = phase2aGetVendorForRezip(managerMatcher);
			originPath = managerMatcher.group("path" + v.getIndex());
			if (originPath.equals(lastRezipPath)) {
				// e.g. exptno/pdata/1/procs
				// e.g. exptno/pdata/999/procs
				if (logging())
					log("duplicate path (pdata/n, pdata/m)" + originPath);
			} else {
				lastRezipPath = originPath;
				byte[] bytes = FAIRSpecUtilities.getLimitedStreamBytes(ais, len, null, false, false);

				String validDir = v.getRezipPrefix(originPath, bytes);
				String basePath = (validDir != null ? originPath
						: baseOriginPath.endsWith("|") && isTopZipDir(originPath)
								? baseOriginPath.substring(0, baseOriginPath.length() - 1)
								// else
								: getFileParent(originPath) + "/");
				String newZipPath = (validDir == null ? originPath + "101/" : originPath);
				String localPath = localizePath(newZipPath);
				CacheRepresentation rep = new CacheRepresentation(new IFDReference(faHelper.getCurrentSource().getID(),
						originPath, extractorResource.rootPath, localPath), v, len, null, "application/zip");
				// if this is a zip file, the data object will have been set to xxx.zip
				// but later we might give this up
				rep.setRezipOrigin(basePath);
				phase2aRezipCacheAdd(originPath, rep);
			}
		}
	}

	String lastRezipOriginPath = null;

	private void phase2aRezipCacheAdd(String originPath, CacheRepresentation rep) {
		log("!rezip pattern found \n" + originPath + " as \n" + rep.getRef().getLocalName());
		int i = rezipCache.size();
		if (lastRezipOriginPath != null && lastRezipOriginPath.startsWith(originPath)) {
			i--;
		}
		rezipCache.add(i, rep);
		lastRezipOriginPath = originPath;
	}

	/**
	 * MNova files, for example. Or JDX. We can set the type here.
	 * 
	 * @param originPath
	 * @param localizedName
	 * @param vendorDataSetKey
	 * @throws IFDException
	 */
	private IFDDataObject phase2aAutomationEnsureCompoundForManagedPath(String originPath, String localizedName,
			String type) throws IFDException {
		if (localizedName == null)
			localizedName = localizePath(originPath);
		IFDDataObject o = (IFDDataObject) getObjectFromLocalizedName(localizedName, null);
		if (o != null)
			return o;
		String cmpdID = automationParser.getAutomationCompoundIDFromPath(originPath);
		log("!Phase2a automation creating data object for unidentified dataset: " + originPath + " for " + cmpdID);
		return automationCreateDataObjectForCompound(originPath, localizedName, cmpdID, originPath, type);
	}

	/**
	 * Phase 2a only - check for rejected, ignored, and accepted.
	 * 
	 * rejected -- do not include in collection. Example: .MACOS files
	 * 
	 * ignored -- include in collection, but do not process. Example: .doc files
	 * 
	 * accepted -- accept such files without regard to vendor acceptance, pulling
	 * them from ZIP files if necessary.
	 * 
	 * @param phase
	 * @param oPath
	 * @param size
	 * @param ais
	 * @return
	 * @throws IOException
	 */
	private boolean phase2aCheckRejected(String phase, String oPath, long size, ArchiveInputStream ais)
			throws IOException {
		if (lstRejected.accept(oPath)) {
			// Test 9: acs.orglett.0c01153/22284726,22284729 MACOSX,
			// acs.joc.0c00770/22567817
			addFileToFileLists(oPath, LOG_REJECTED, size, null);
			return false;
		}
		if (lstIgnored.accept(oPath)) {
			// Test 9: acs.orglett.0c01153/22284726,22284729 MACOSX,
			// acs.joc.0c00770/22567817
			addFileToFileLists(oPath, LOG_IGNORED, size, ais);
			return false;
		}
		if (!lstAccepted.accept(oPath)) {
			return false;
		}
		addFileToFileLists(oPath, LOG_ACCEPTED, size, null);
		return true;
	}

	private void phase2bIterate(Map<String, Map<String, ArchiveEntry>> htArchiveContents)
			throws IOException, IFDException {
		ParserIterator iter = new ParserIterator();
		while (iter.hasNext()) {
			ObjectParser parser = nextParser(iter);
			if (parser.isAutomationParser) {
				// automationParser will have created objects already.
				continue;
			}
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
	 * {IFD.representation.spec.nmr.dataobject.dataset::{IFD.property.sample.label::*-*}-{IFD.property.dataobject.label::*}.jdf}
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
		System.out.println(originPath + " found");
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
		// System.out.println("EXL2b " + keyList);
		// for example,
		// {compound=IFD.property.fairspec.compound.id::*}/{IFD.representation.structure.cdxml::{IFD.property.structure.id::*}.cdxml}
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
			}
		}
	}

	private void phase2cIterate() throws MalformedURLException, IOException, IFDException {
		ParserIterator iter = new ParserIterator();
		while (iter.hasNext()) {
			ObjectParser parser = nextParser(iter);
			if (parser.hasData())
				phase2ReadFAIRSpecReadyCollectionIteratively(getTopZipStream(), "", PHASE_2C, null, 0);
		}
	}

	/**
	 * Pull the next rezip parent directory name off the stack, setting the
	 * currentRezipPath and currentRezipVendor fields.
	 * 
	 */
	private String phase2cGetNextRezipName() {
		if (rezipCache.size() == 0) {
			currentRezipPath = null;
			currentRezipLocalPath = null;
			currentRezipRepresentation = null;
			// don't do this!currentRezipVendor = null;
			return null;
		}
		String path = currentRezipPath = (String) (currentRezipRepresentation = rezipCache.remove(0)).getRef()
				.getOriginPath();
		System.out.println("rezip looking for " + path);
		currentRezipVendor = (DataObjectVendorPluginI) currentRezipRepresentation.getData();
		currentRezipLocalPath = currentRezipRepresentation.getRef().getLocalName();
		return (path.endsWith("|") ? path.substring(0, path.length() - 1) : null);

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
	 * @param baseName   xxxx.zip|
	 * @param originPath
	 * @param ais
	 * @param entry
	 * @return firstEntry (if the first entry was read in order to start this zip
	 *         operation) or null.
	 * @throws IOException
	 * @throws IFDException
	 */
	private ArchiveEntry phase2cRezipEntry(String baseName, String originPath, ArchiveInputStream ais,
			ArchiveEntry entry, ArchiveEntry firstEntry, DataObjectVendorPluginI rezipVendor, int level)
			throws IOException, IFDException {
		String nextRezipPath = phase2cGetNextRezipName();
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
		boolean isDir = (entry.isDirectory());
		String dirName = (isDir ? entryName : entryName.substring(0, entryName.lastIndexOf('/') + 1));
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
		String parent = getFileParent(entryName);
		if (parent.equals(entryName))
			parent = null;
		int lenOffset = (parent == null ? 0 : parent.length() + 1);
		// because Bruker directories must start with a number
		// xxx/1/ is OK
		// yyy.zip|xxx/ is not
		int pt = dirName.length() - 1;
		String thisDir = (lenOffset < pt ? dirName.substring(lenOffset, pt) : "");
		String newDir = (pt < 0 ? null : rezipVendor.getRezipPrefix(dirName, null));
		String localizedName = localizePath(originPath);
		String lNameForObj = localizedName;
		// look for the object associated with this path
		// 8f/HBMC.zip|HMBC/250/ will be under HMBC.zip
		IFDRepresentableObject<?> obj = getObjectFromLocalizedName(lNameForObj, IFDConst.IFD_DATAOBJECT_FLAG);
		if (obj == null) {
			// another possibility is that this is a directory in the top-level zip file or
			// directory.
			// Compounds.zip|spectra/co011/NMR/1/ will be just the entry name
			obj = getObjectFromLocalizedName(localizePath(entryName), IFDConst.IFD_DATAOBJECT_FLAG);
			if (obj == null) {
				if (automationParser != null) {
					obj = phase2aAutomationEnsureCompoundForManagedPath(originPath, localizedName,
							rezipVendor.getType());
				}
				if (obj == null && isDir) {
					// was a directory
					obj = getObjectFromLocalizedName(localizePath(getFileParent(originPath) + "/"),
							IFDConst.IFD_DATAOBJECT_FLAG);
				}
				if (obj == null && baseName.endsWith("|")) {
					// was a zip file
					obj = getObjectFromLocalizedName(localizePath(baseName.substring(0, baseName.length() - 1)),
							IFDConst.IFD_DATAOBJECT_FLAG);
				}
				if (obj == null) {
					log("! phase2cRezipEntry could not find object for " + lNameForObj + "\n REASON: "
							+ extractScriptFile + " does not catch this recognized " + rezipVendor.getVendorName()
							+ " pattern" + "\n ignoring this dataset");
				}
			}
		}
		String basePath = baseName + (parent == null ? "" : parent);
		String msg = null;
		if (newDir != null) {
			newDir = "";
			if (parent == null) {
				originPath = basePath.substring(0, basePath.length() - 1);
			} else {
				String localParent = localizePath(parent);
				if (obj == null) {
					obj = getObjectFromLocalizedName(localParent, IFDConst.IFD_DATAOBJECT_FLAG);
				}
				// allow the first spectrum to not have _n in its id,
				// but any later spectrum must have this
				String ext = (localParent.equals(lastLocalClonedParent) ? "_" + thisDir : "");
				lastLocalClonedParent = localParent;
				obj = cloneData((IFDDataObject) obj, ext, true);
			}
			if (originPath.toLowerCase().endsWith(".zip")) {
				if (lenOffset > 0) {
					htZipRenamed.put(localizePath(basePath), localizedName);
				}
			}
			this.originPath = originPath;
			localizedName = localizePath(originPath);
			if (!localizedName.endsWith(".zip")) {
				originPath += ".zip";
				localizedName += ".zip";
			}
			if (this.localizedName == null)
				this.localizedName = localizedName;
		} else {
			newDir = "101/";
			lenOffset = dirName.length();
			this.originPath = originPath;
			if (originPath.toLowerCase().endsWith(".zip")) {
				if (lenOffset > 0) {
					htZipRenamed.put(localizePath(basePath), localizedName);
				}
			} else {
//				oPath += ".zip";
			}
			if (this.localizedName == null)
				this.localizedName = localizedName;
			msg = "Extractor correcting " + rezipVendor.getVendorName() + " directory name to " + localizedName + "|"
					+ newDir;
		}
		localizedName = localizePath(originPath);
		if (obj != null)
			putLocalizedNameToObject(localizedName, obj);
		this.originPath = originPath;
		this.localizedName = localizedName;
//		if (automationParser != null)
//			addProperty(DeferredProperty.AUTOMATION_CHECK, new String[0]);
//
		if (msg != null) {
			addProperty(IFDConst.IFD_PROPERTY_NOTE, msg);
			log("!" + msg);
		}
		File outFile = getAbsoluteFileTarget(originPath);
		log("!Extractor Phase 2c rezipping " + baseName + entry + " as " + outFile);
		OutputStream fos = (insitu || noOutput ? new ByteArrayOutputStream() : new FileOutputStream(outFile));
		ZipOutputStream zos = (insitu ? null : new ZipOutputStream(fos));
		rezipVendor.initializeDataSet(this);
		long len = 0;
		while ((entry = (firstEntry == null ? ais.getNextEntry() : firstEntry)) != null) {
			firstEntry = null;
			entryName = entry.getName();
			String entryPath = baseName + entryName;
			isDir = entry.isDirectory();
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

			String localName = localizePath(baseName + entryName);
			boolean isNextRezipEntry = entryPath.equals(nextRezipPath);
			if (isNextRezipEntry) {
					phase2ReadFAIRSpecReadyCollectionIteratively(ais, baseName + entryName + "|", currentPhase, null, level + 1);
			}

			// prevent this file from being placed on the ignored list

			boolean isInlineBytes = false;
			boolean isBytesOnly = false;
			boolean isIFDMetadataFile = entryName.endsWith(ifdRelatedMetadataFileName);
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
			this.originPath = entryPath;
			PropertyManagerI mgr = null;
			Matcher managerMatcher = null;
			boolean doCache = (!isIFDMetadataFile && propertyManagerCachePattern != null
					&& (managerMatcher = propertyManagerCachePattern.matcher(entryName)).find()
					&& phase2cGetParamName(managerMatcher) != null
					&& ((mgr = getPropertyManager(managerMatcher, true, true)) == null || mgr.doExtract(entryName)));
			boolean doCheck = (doCache || mgr != null || isIFDMetadataFile);

			if (mgr == null || mgr == rezipVendor)
				putLocalizedNameToObject(localName, obj);

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
				this.localizedName = localizePath(this.originPath);
				if (isIFDMetadataFile) {
					addIFDMetadata(new String(bytes));
				} else if (mgr == rezipVendor) {
					rezipVendor.accept(null, this.originPath, bytes);
				} else if (mgr instanceof DefaultStructureHelper) {
					// does not happen.
				}
			}
			if (doInclude && !insitu)
				zos.closeEntry();
			if (typeData != null) {
				String key = (String) typeData[0];
				typeData[0] = (isBytesOnly ? null : localName);
				typeData[1] = bytes;
				// extract this file into the collection
				DeferredProperty dp = new DeferredProperty(key, (isBytesOnly || isInlineBytes ? typeData : localName));
				dp.isInline = (isInlineBytes || insitu);
				addDeferredPropertyOrRepresentation(dp);
			}
		}
		rezipVendor.endDataSet();
		if (zos != null)
			zos.close();
		fos.close();
		String dataType = rezipVendor.getVendorDataSetKey();
		len = (noOutput || insitu ? ((ByteArrayOutputStream) fos).size() : outFile.length());
		if (!insitu)
			writeOriginToCollection(originPath, null, len);
		IFDRepresentation r = faHelper.getSpecDataRepresentation(localizedName);
		if (r == null) {
			// probably the case, as this renamed representation has not been added yet.
		} else if (!insitu) {
			r.setLength(len);
		}
		if (len < 100) {
			logWarn("dataset is empty! " + originPath + " > " + this.localizedName, "phase2cRezipEntry");
		}
		if (originPath.toLowerCase().endsWith(".zip"))
			originPath = originPath.substring(0, originPath.length() - 4); // remove ".zip"
		addFileAndCacheRepresentation(originPath, localizedName, null, len, dataType, null, "application/zip");
		if (obj != null && !localizedName.equals(lNameForObj)) {
			putLocalizedNameToObject(localizedName, obj);
		}
		return entry;
	}

	/**
	 * parent of /test/name is /test
	 * 
	 * parent of /test/1/ is /test
	 * 
	 * parent of xxx.zip|test is xxx.zip|test ?? parent of /test/xxx.zip|test is
	 * /test/ ??
	 * 
	 * @param entryName
	 * @return
	 */
	private static String getFileParent(String entryName) {
		int pt1 = entryName.lastIndexOf("|");
		entryName = entryName.replace('\\', '/');
		int pt = entryName.lastIndexOf('/', entryName.length() - 2);
		if (pt <= pt1) {
			pt = entryName.lastIndexOf("/");
		}
		return (pt < 0 ? entryName : entryName.substring(0, pt));
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
		if (val != null && val != IFDProperty.NULL) {
			String sval = (val instanceof String[] ? Arrays.toString((String[]) val) : val.toString());
			log(this.localizedName + " addProperty " + key + "=" + val);
			System.out.println("addProperty " + String.format("%s:%s", key, sval));
		}
		addDeferredPropertyOrRepresentation(new DeferredProperty(key, val));
	}

	private void resetDeferredPropertyList(boolean andClear) {
		if (andClear)
			deferredPropertyList.clear();
		deferredPropertyPointer = 0;
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
	 * Note that when the deferredPropertyPointer is reset to 0, items are added
	 * BEFORE items that were already on the list before the pointer was reset to 0.
	 * 
	 * @param p
	 */
	@Override
	public void addDeferredPropertyOrRepresentation(DeferredProperty p) {
		if (!deferringProperties)
			return;
		if (p == null) {
			deferredPropertyList.add(deferredPropertyPointer++, null);
			return;
		}
		p.originPath = originPath;
		p.localizedName = localizedName;
		p.resource = extractorResource;
		p.setPhase(currentPhase);

		StructureData struc = (p.key.startsWith(DefaultStructureHelper.STRUC_FILE_DATA_KEY) ? (StructureData) p.value
				: null);
		if (struc != null) {
			if (!processMnovaMOL && struc.originPath.indexOf("page") >= 0) {
				// testing only
				return;
			}
			if (automationParser != null) {
				struc.cmpdID = automationParser.getAutomationCompoundIDFromPath(p.originPath);
			}
		}
		deferredPropertyList.add(deferredPropertyPointer++, p);
//		if (p.key == DeferredProperty.AUTOMATION_CHECK) {
//			if (currentPhase == PHASE_2C)
//			System.out.println(p.value);
//			System.out.println(p.localizedName);
//			System.out.println(p.originPath + " " + currentPhase);
//			p.autoPath = (String[]) p.value;
//		}
		if (struc != null) {
			// Phase 2a has identified a structure before a compound has been established in
			// Phase 2b.
			// Mestrelab vendor plug-in has found a MOL or SDF file in Phase 2b.

			// val is Object[] {byte[] bytes, String name}
			// Pass data to structure property manager in order
			// to add (by coming right back here) InChI, SMILES, and InChIKey.
			getStructurePropertyManager().processStructureData(struc);
		}
	}

	/**
	 * Process the properties in deferredPropertyList after the IFDObject objects
	 * have been created for all resources. This includes writing extracted
	 * representations to files.
	 * 
	 * In the end, clears the deferredPropertyList,
	 * 
	 * @throws IFDException
	 * @throws IOException
	 */
	private void phase2dProcessDeferredObjectProperties(String phase2OriginPath) throws IFDException, IOException {
		ProcessState state = new ProcessState();
		String lastLocal = null;
		// end of property list
		if (dumpPropertyList) {
			writePropertyList();
		}
		for (int i = 0, n = deferredPropertyList.size(); i < n; i++) {
			DeferredProperty dp = deferredPropertyList.get(i);
			if (dp == null) {
				// end of reading MNova file
				state.sample = null;
				state.cloneOriginObject = null;
				continue;
			}
			state.assoc = null;
			// note that multi-page MNOVA here would be the same as previous.
			final String key = dp.key;// a[2];
			final Object value = dp.value;// a[3];
			final boolean isInline = dp.isInline;// (a[4] == Boolean.TRUE);
			boolean isNewPage = false;
			switch (key) {
			case DeferredProperty.NEW_RESOURCE_KEY:
				initializeResource(dp.resource, false);
				continue;
			case DeferredProperty.NEW_PAGE_KEY:
				if (!state.processNewPageKey(value))
					continue;
				isNewPage = true;
				break;
			case DeferredProperty.PAGE_ID_PROPERTY_SOURCE:
				// looking at the title of an MNova page
				// if it starts with "COMPOUND ", then we accept that.
				if (state.pageTitleCompoundID == null)
					state.pageTitleCompoundID = inferCompoundID((String) value);
				continue;
			}

//			if (dp.autoPath != null) {
//				if (dp.autoPath.length == 0) {
//					cmpdID = dataObjectID = null;
//				} else {
//					String[] row = dp.autoPath;
//					cmpdID = row[0];
//					dataObjectID = row[1];
//				}
//				continue;
//			}

			String type = FAIRSpecFindingAidHelper.getObjectTypeForPropertyOrRepresentationKey(key, true);

			// not implemented -- opted for property originating_sample_id
//			boolean isSample = (type == FAIRSpecFindingAidHelper.ClassTypes.Sample);
//			if (isSample) {
//				state.sample = faHelper.getSampleByName(dp.sampleName);
//				continue;
//			}

			final boolean isNew = dp.localizedName != null && !dp.localizedName.equals(lastLocal);
			if (isNew) {
				lastLocal = dp.localizedName;
			}
			// link to the originating spec representation -- xxx.mnova, xxx.zip
			// this could pick up the first spectrum in an mnova file.

			IFDRepresentableObject<?> ifdObject = getObjectFromLocalizedName(dp.localizedName,
					IFDConst.getObjectTypeFlag(key));
			if (ifdObject != null && !ifdObject.isValid()) {
				// this is a clone second -- get first
				ifdObject = phase2dGetClonedData(ifdObject);
			}
			if (ifdObject == null && !state.cloning) {
				// just here to notify of an issue
				logDigitalItemIgnored(dp.originPath, dp.localizedName,
						"phase 2d no spec data to associate this structure with", "processDeferredObjectProperties");
				continue;
			}

			state.processIFDObject(ifdObject, isNew);
			if (IFDConst.isRepresentation(key)) {
				state.processRepresentation(dp, isInline);
				continue;
			}
			boolean isStructureHelperStructureKey = key.startsWith(DefaultStructureHelper.STRUC_FILE_DATA_KEY);
			if (isStructureHelperStructureKey) {
				state.processStructureKey(dp, (StructureData) value);
				continue;
			}

			if (isNewPage) {
				if (state.cloning)
					state.processNewPage(dp);
				continue;
			}
			// just a property
			boolean isStructure = (type == FAIRSpecFindingAidHelper.ClassTypes.Structure);
			if (isStructure) {
				if (state.struc == null) {
					logErr("No structure found for " + lastLocal + " " + key, "processDeferredObjectProperies");
					continue; // already added?
				} else {
					phase2dSetPropertyIfNotAlreadySet(state.struc, key, value, dp.originPath);
				}
			} else {
				// dataobject
				if (key.equals(FAIRSpecExtractorHelper.DATAOBJECT_ORIGINATING_SAMPLE_ID)) {
					helper.addSpecOriginatingSampleRef(extractorResource.rootPath, state.localSpec, (String) value);
				}
				if (key == IFDConst.IFD_PROPERTY_DATAOBJECT_EXPT_TIMESTAMP) {
					phase2AddSpectraToTimeStampHashMap(state.localSpec, (Long) value);
				}
				System.out
						.println("adding " + key + " value " + value + " to " + state.localSpec + " " + dp.originPath);
				phase2dSetPropertyIfNotAlreadySet(state.localSpec, key, value, dp.originPath);

			}
		}
		if (state.assoc == null) {
			resetDeferredPropertyList(true);
		} else if (state.cloning) {
			// but why is it the lastLocal?
			propertyManagerCache.remove(lastLocal);
		}
	}

	class ProcessState {
		boolean cloning;
		IFDStructure struc;
		IFDDataObject prevSpec;
		IFDDataObject localSpec;
		IFDSample sample;
		IFDAssociation assoc;
		IFDDataObject cloneOriginObject;
		String pageTitleCompoundID;
		private IFDRepresentableObject<?> object;
		private String thisPageInvalidatedStructureRepKey;

		protected Map<AWrap, IFDStructure> htStructureRepCache = new HashMap<>();

		private IFDStructure getCachedStructure(AWrap w, byte[] bytes, String inchi) {
			bytes = (inchi == null || inchi.length() < 2 ? bytes : inchi.getBytes());
			w.setBytes(bytes);
			return htStructureRepCache.get(w);
		}

		/**
		 * we have found an object associated with this representation or property
		 * 
		 * @param ifdObject
		 * @param isNew
		 */
		public void processIFDObject(IFDRepresentableObject<?> ifdObject, boolean isNew) {
			if (ifdObject instanceof IFDStructure) {
				struc = (IFDStructure) ifdObject;
				ifdObject = null;
			} else if (ifdObject instanceof IFDSample) {
				sample = (IFDSample) ifdObject;
				ifdObject = null;
			} else if (isNew && ifdObject instanceof IFDDataObject) {
				localSpec = (IFDDataObject) ifdObject;
				String label = (struc == null ? null : struc.getLabel());
				if (label != null && label.startsWith("Structure_"))
					struc = null;
			}
			this.object = ifdObject;
		}

		public boolean processNewPageKey(Object value) {
			pageTitleCompoundID = null;
			thisPageInvalidatedStructureRepKey = null;
			if (value == IFDProperty.NULL) {
				// done reading MNova file
				cloning = false;
				cloneOriginObject = null;
				struc = null;
				localSpec = prevSpec;
				return false;
			}
			if (value.equals("_page=1")) {
				prevSpec = localSpec;
				localSpec = null;
			}
			cloning = true;
			return true;
		}

		public void processRepresentation(DeferredProperty dp, boolean isInline) throws IOException, IFDException {
			Object value = dp.value;
			boolean isWritableRepresentation = (!isInline && value instanceof StructureData);
			// from reportVendor-- Bruker adds this for thumb.png and pdf files.
			// Mestrec will add #page1.cdx or #page1.mol
			Object data = null;
			String mediaType = dp.mediaType;// (String) a[5];
			String note = dp.note;// (String) a[6];
			if (isWritableRepresentation) {
				// from DefaultStructureHelper - a PNG version of a CIF file, for example.
				StructureData sd = (StructureData) value;
				byte[] bytes = sd.bytes;
				String oPath = sd.originPath;
				String localName = localizePath(oPath);
				if (thisPageInvalidatedStructureRepKey != null
						&& localName.startsWith(thisPageInvalidatedStructureRepKey)) {
					// "NMR DATA/product/3a-H.mnova#page1.cdx.mol"
					// but 3a-C and 3a-H might have the same cdx on different pages
				}
				if (!insitu)
					writeOriginToCollection(oPath, bytes, 0);
				if (extractorResource.isDefaultStructurePath) {
					IFDStructureRepresentation rep = (IFDStructureRepresentation) faHelper.getStructureCollection()
							.getRepresentation("" + extractorResource.id, localName);
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
				if (value instanceof String) {
					data = value;
				} else {
					keyPath = ((Object[]) value)[0].toString();
					data = ((Object[]) value)[1];
				}
			} else {
				keyPath = value.toString();
			}
			// note --- not allowing for AnalysisObject or Sample here
			boolean isStructureRep = IFDConst.isStructure(dp.key);
			if (isStructureRep && struc == null) {
				struc = helper.addStructureForSpec(extractorResource.rootPath, (IFDDataObject) object, dp.key,
						dp.originPath, localizePath(keyPath), null);
			}
			IFDRepresentableObject<?> obj = (isStructureRep ? struc : phase2dGetClonedData(object));
			linkLocalizedNameToObject(keyPath, null, obj);
			IFDRepresentation r = obj.findOrAddRepresentation(null, faHelper.getCurrentSource().getID(), dp.originPath,
					extractorResource.rootPath, keyPath, data, dp.key, mediaType);
			if (note != null)
				r.addNote(note);
			if (!isInline)
				setLocalFileLength(r);
		}

		protected void processNewPage(DeferredProperty dp) throws IFDException {
			String localName = dp.localizedName;
			Object value = dp.value;
			String newLocalName = null;
			boolean removeAllRepresentations = false;
			boolean replaceOld = true;
			IFDDataObject specToClone = null;
			// we will clone localSpec
			if (value instanceof String) {
				// e.g. MNova extracted _page=10
				// clear structure for not allowing an MNova structure to carry to next page?
//				struc = null; 
				if (cloneOriginObject == null) {
					// first time through, get the parent MNova object
					// which has the origin information and at least one property.
					specToClone = cloneOriginObject = localSpec;
				} else {
					specToClone = localSpec = cloneOriginObject;
				}
			} else if (value instanceof Object[]) {
				// e.g. Bruker created a new object from multiple <n>/ directories
				Object[] a = (Object[]) value;
				value = (String) a[0];
				IFDDataObject sp = (IFDDataObject) a[1];
				newLocalName = (String) a[2];
				removeAllRepresentations = true;
				replaceOld = sp.isValid(); // will be true first time only
				specToClone = localSpec = sp;
			} else {
				System.out.println("L2 ? How can this be??");
			}

			String idExtension = (String) value;
			if (assoc == null && specToClone != null)
				assoc = faHelper.findCompound(null, specToClone);
			IFDDataObject newSpec;
			if (specToClone == null) {
				// we are no longer cloning
				specToClone = localSpec = (IFDDataObject) object;
				replaceOld = false;
			} else {
				// internal Bruker directories
				// or MNova extract
			}
			// specToClone.dumpProperties("local");
			newSpec = cloneData(specToClone, idExtension, replaceOld);
			object = localSpec = newSpec;
			// newSpec.dumpProperties("new");
			struc = faHelper.getFirstStructureForSpec(localSpec, assoc == null);
			if (sample == null)
				sample = faHelper.getFirstSampleForSpec(localSpec, assoc == null);
			if (assoc == null) {
				if (struc != null) {
					faHelper.createCompound(struc, newSpec);
					log("!Structure " + struc + " found and associated with " + object);
				}
				if (sample != null) {
					faHelper.associateSampleSpec(sample, newSpec);
					log("!Structure " + struc + " found and associated with " + object);
				}
			} else {
				// we have an association already in Phase 2, and now we need to
				// update that.
				if (removeAllRepresentations)
					newSpec.clear();
				((FAIRSpecCompoundAssociation) assoc).addDataObject(newSpec);
			}
			if (struc == null && sample == null) {
				log("!SpecData " + object + " added " + (assoc == null ? "" : "to " + assoc));
			}
			if (newLocalName != null)
				localName = newLocalName;
			putLocalizedNameToObject(localName, object); // for internal use
			CacheRepresentation rep = propertyManagerCache.get(localName);
			if (newLocalName != localName) {
				String ckey = localName + idExtension.replace('_', '#') + "\0" + idExtension;
				propertyManagerCache.put(ckey, rep);
				putLocalizedNameToObject(ckey, object);
			}
		}

		public void processStructureKey(DeferredProperty dp, StructureData sd) throws IFDException, IOException {
			// _struc.cdx,png,mol
			// e.g. extracted xxxx/xxx#page1.mol
			byte[] bytes = sd.bytes;
			String oPath = sd.originPath;
			String localName = localizePath(oPath);
			String inchi = sd.standardInChI;
			String cmpdID = sd.cmpdID;
			String name = null;
			String ifdRepType = sd.type;
			if (ifdRepType == null) {
				ifdRepType = DefaultStructureHelper.getType(dp.key.substring(dp.key.lastIndexOf(".") + 1), bytes, true);
			} else {
				name = phase2dGetStructureNameFromPath(oPath);
			}
			// use the byte[] for the structure as a unique identifier.
			AWrap w = new AWrap();

			//
			struc = getCachedStructure(w, bytes, inchi);
			if (struc == null) {
				thisPageInvalidatedStructureRepKey = null;
				// this representation has not been associated with an object yet
				// will have pageN for mnova

				struc = faHelper.getFirstStructureForSpec((IFDDataObject) object, false);
				if (struc == null) {
					if (cmpdID != null) {
						// System.out.println("IFD2 adding structure to " + cmpdID + " " + localName);
						FAIRSpecCompoundAssociation c = (FAIRSpecCompoundAssociation) helper.getCompoundCollection()
								.getObjectById(cmpdID);
						if (c != null)
							struc = helper.addStructureForCompound(extractorResource.rootPath, c, ifdRepType, oPath,
									localName, name);
					}
					if (struc == null)
						struc = helper.addStructureForSpec(extractorResource.rootPath, (IFDDataObject) object,
								ifdRepType, oPath, localName, name);
				}
				htStructureRepCache.put(w, struc);
				if (!insitu)
					writeOriginToCollection(oPath, bytes, 0);
				if (extractorResource.isDefaultStructurePath) {
					IFDStructureRepresentation rep = (IFDStructureRepresentation) faHelper.getStructureCollection()
							.getRepresentation("" + extractorResource.id, localName);
					if (rep != null)
						rep.setData(bytes);
				}

				addFileAndCacheRepresentation(oPath, localName, insitu ? bytes : null, bytes.length, ifdRepType, null,
						null);
				linkLocalizedNameToObject(localName, null, struc);
				if (sample == null) {
					assoc = faHelper.findCompound(struc, (IFDDataObject) object);
				} else {
					assoc = faHelper.associateSampleStructure(sample, struc);
				}
				// MNova 1 page, 1 spec, 1 structure Test #5
				log("!Structure " + struc + " created and associated with " + object);
			} else {
				// structure has already been found for this representation
				// invalidate this (cdx/mol) representation and all derived representations
				// (.mol, .png)
				thisPageInvalidatedStructureRepKey = localName + ".";
				invalidateCachedRepresentation(oPath, localName, struc);
				assoc = faHelper.findCompound(struc, (IFDDataObject) object);
				if (assoc == null) {
					assoc = faHelper.createCompound(struc, (IFDDataObject) object);
					log("!Structure " + struc + " found and associated with " + object);
				}
			}
			if (pageTitleCompoundID != null) {
				// MNova "Compound XX" in title
				assoc.setID(pageTitleCompoundID);
				pageTitleCompoundID = null;
			}
			if (struc.getID() == null) {
				String id = assoc.getID();
				if (id == null && object != null) {
					id = object.getID();
					assoc.setID(id);
				}
				struc.setID(id);
			}
		}

	}

	private IFDDataObject cloneData(IFDDataObject specToClone, String idExtension, boolean replaceOld) {
		if (specToClone == null)
			return null;
		IFDDataObject newSpec = faHelper.cloneData(specToClone, idExtension, replaceOld);
		phase2AddCloneMap(specToClone, newSpec);
		return newSpec;
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
	 * 
	 * @param spec
	 * @return
	 */
	private IFDRepresentableObject<?> phase2dGetClonedData(IFDRepresentableObject<?> spec) {
		if (htCloneMap.isEmpty())
			return spec;
		IFDRepresentableObject<?> d = (spec.isValid() ? null : getCloned(spec));
		return (d == null ? spec : d);
	}

	protected void phase2AddCloneMap(IFDDataObject oldSpec, IFDDataObject newSpec) {
		htCloneMap.put(oldSpec.getIDorIndex(), newSpec);
		htCloneMap.put("$" + newSpec.getIDorIndex(), oldSpec);
	}

	protected IFDRepresentableObject<?> getCloned(IFDRepresentableObject<?> oldSpec) {
		return htCloneMap.get(oldSpec.getIDorIndex());
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

	/**
	 * Use a 30-minute hash to group spectra into initial bins. Later, we will check
	 * more carefully.
	 * 
	 * @param obj
	 * @param time
	 */
	protected void phase2AddSpectraToTimeStampHashMap(IFDDataObject obj, Long time) {
		Integer timeStamp = Integer.valueOf((int) (time % 1800)); // 30-min intervals
		if (timestampSpectraObjectHashMap == null)
			timestampSpectraObjectHashMap = new HashMap<>();
		ArrayList<IFDDataObject> objList = timestampSpectraObjectHashMap.get(timeStamp);
		if (objList == null) {
			timestampSpectraObjectHashMap.put(timeStamp, objList = new ArrayList<>());
		}
		objList.add(obj);
	}

	private void phase2eIterate() throws MalformedURLException, IOException, IFDException {
		ParserIterator iter = new ParserIterator();
		while (iter.hasNext()) {
			nextParser(iter);
			phase2ReadFAIRSpecReadyCollectionIteratively(getTopZipStream(), "", PHASE_2E, null, 0);
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
	private CacheRepresentation addFileAndCacheRepresentation(String originPath, String localizedName, Object data,
			long len, String ifdType, String fileNameForMediaType, String mediaType) throws IOException {
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
		propertyManagerCache.put(localizedName, rep);
		return rep;
	}

	/**
	 * As duplicate structure representation has been found. Perhaps from two
	 * different pages in an MNova file.
	 * 
	 * @param localName
	 * @param struc
	 */
	private void invalidateCachedRepresentation(String originPath, String localizedName, IFDStructure struc) {
		CacheRepresentation rep = new CacheRepresentation(new IFDReference(faHelper.getCurrentSource().getID(),
				originPath, extractorResource.rootPath, localizedName), null, 0, null, null);
		rep.isValid = false;
		logWarn("Duplicate structure " + originPath + " ignored! Found: " + struc, "invalidateCachedRepresentation");
		propertyManagerCache.put(localizedName, rep);
	}

	/**
	 * Add metadata from a simple file (default IFD_METADATA) with key=value pairs
	 * one per line.
	 * 
	 * @param data
	 */
	private void addIFDMetadata(String data) {
		addProperties(FAIRSpecUtilities.getIFDPropertyList(data, ifdRelatedMetadataMap));
	}

	private void addProperties(List<IFDProperty> props) {
		for (int i = 0, n = props.size(); i < n; i++) {
			IFDProperty p = props.get(i);
			addDeferredPropertyOrRepresentation(new DeferredProperty(p.getName(), p.getValue()));
		}
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
		return (localizedTopLevelZipURL.equals(FindingAidCreator.CRAWLER_NAME) ? crawlerInputStream
				: localizedTopLevelZipURL.endsWith("/") ? new DirectoryInputStream(localizedTopLevelZipURL)
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
				new File(targetPath + "/" + rootPath).mkdir();
			}
			resource.setLists(rootPath, ignoreRegex, acceptRegex);

		}
		lstManifest = resource.lstManifest;
		lstIgnored = resource.lstIgnored;
		boolean addProp = ((faHelper.getCurrentSource() != faHelper.addOrSetSource(resource.getRemoteSource(),
				resource.rootPath)) && isInit);
		extractorResource = resource;
		if (addProp)
			addDeferredPropertyOrRepresentation(new DeferredProperty(DeferredProperty.NEW_RESOURCE_KEY, null));

		// only used for logging:
		thisRootPath = resource.rootPath;
	}

	/**
	 * Link one or more local names and type to an object such as a spectrum or
	 * structure. Later in the process, this representation will be added to the
	 * object.
	 * 
	 * @param localizedName
	 * @param type
	 * @param obj
	 * @throws IOException
	 */
	private void linkLocalizedNameToObject(String localizedName, String type, IFDRepresentableObject<?> obj) {
		if (localizedName != null && (type == null || IFDConst.isRepresentation(type))) {
			String pre = obj.getObjectFlag();
			putLocalizedNameToObject(localizedName, obj);
			putLocalizedNameToObject(pre + localizedName, obj);
			String renamed = htZipRenamed.get(localizedName);
			if (renamed != null) {
				putLocalizedNameToObject(renamed, obj);
				// deferred representations could be for multiple object types.
				putLocalizedNameToObject(pre + renamed, obj);
			}
		}
	}

	private void putLocalizedNameToObject(String name, IFDRepresentableObject<?> obj) {
		if (obj == null)
			System.out.println("!!!!putLN2O " + name + " > " + obj);
		htLocalizedNameToObject.put(name, obj);
	}

	private ObjectParser nextParser(ParserIterator iter) {
		ObjectParser parser = iter.next();
		if (parser.getDataSource() != extractorResource) {
			try {
				initializeResource(parser.getDataSource(), true);
				if (parser.isAutomationParser) {
					automationParser = (AutomationParser) parser;
					processAutomationObjects();
				} else {
					automationParser = null;
				}
			} catch (IFDException | IOException e) {
				e.printStackTrace();
			}
		}
		return parser;
	}

	/**
	 * Create Compound and DataObject instances for all identified objects and
	 * paths. Tie these paths via their localized path to the data object
	 * 
	 * @param parser
	 * @throws IFDException
	 * @throws IOException
	 */
	private void processAutomationObjects() throws IFDException {
		if (automationParser.automationProcessed)
			return;
		List<String[]> data = automationParser.automationData;
		log("!processAutomatoinObjects for " + automationParser.dataSource);
		for (int i = data.size(); --i >= 0;) {
			String[] info = data.get(i);
			String originPath = info[AutomationParser.PATH];
			String cmpdID = info[AutomationParser.CMPD_ID];
			String specID = info[AutomationParser.SPEC_ID];
			String type = (info.length > AutomationParser.TYPE ? info[AutomationParser.TYPE] : "nmr"); // default to
																										// "nmr"
			if (type == null)
				type = "nmr";
			String localizedName = localizePath(originPath);
			automationCreateDataObjectForCompound(originPath, localizedName, cmpdID, specID, type);
		}
		automationParser.automationProcessed = true;
	}

	/**
	 * Create or find a compound object, set faHelper.currentDataObject, and add
	 * this data object to faHelper.thisCompound
	 * 
	 * @param originPath
	 * @param cmdID
	 * @param specID
	 * @param type
	 * @throws IFDException
	 */
	private IFDDataObject automationCreateDataObjectForCompound(String originPath, String localizedName, String cmpdID,
			String specID, String type) throws IFDException {
		// create the compound object
		// set faHelper.currentDataObject
		// and add this data object to faHelper.thisCompound
		if (cmpdID == null)
			cmpdID = automationParser.getAutomationCompoundIDFromPath(originPath);
		faHelper.findOrCreateCompound(cmpdID);
		IFDDataObject o = faHelper.findOrCreateDataObject(specID, type);
		// tie this object to its localized name for deferred property processing
		linkLocalizedNameToObject(localizedName, null, o);
		return o;
	}

	/**
	 * A new Pull a full row from a file such as Manifest.xlsx with headers
	 * including a compound numbers, for example.
	 * 
	 * Called from FAIRSpecExtractorHelper.addObject and checkAddNewObject
	 * 
	 * @param o     an IFDObject such as a Structure, DataObject, or Compound
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

	private void writePropertyList() {
		String s = deferredPropertyList.toString().replace(", [", "\n[");
		try {
			if (propertyListFile == null) {
				if (targetPath == null)
					return;
				propertyListFile = new File(targetPath, "_IFD_property_list.txt");
			}
			writeBytesToFile(s.getBytes(), propertyListFile);
			log("!" + propertyListFile.getAbsolutePath() + " created");
		} catch (IOException e) {
			e.printStackTrace();
		}
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
		return new File(targetPath + "/" + extractorResource.rootPath + "/" + localizePath(originPath));
	}

	/**
	 * Clean up the zip entry name to remove '/', ' ', and add ".zip" if there is a
	 * trailing '/' in the name. Zip paths marks '|' are switched to '..'.
	 * 
	 * Used in Phases 2 (directly) and 3 (indirectly) only.
	 * 
	 * @param path
	 * @return localized path
	 */
	private static String localizePath(String path) {
		if (path.endsWith("|"))
			path = path.substring(0, path.length() - 1);
		path = path.replace('\\', '/');
		boolean isDir = path.endsWith("/");
		if (isDir)
			path = path.substring(0, path.length() - 1);
		int pt = -1;
		while ((pt = path.indexOf('|', pt + 1)) >= 0)
			path = path.substring(0, pt) + ".." + path.substring(++pt);
		return path.replace('/', '_').replace('#', '_').replace(' ', '_')
				// Windows disallowed characters
				.replace('\\', '_').replace('<', '_').replace('>', '_').replace(':', '_').replace('\"', '_')
				.replace('?', '_').replace('*', '_').replace(".ZIP", ".zip") + (isDir ? ".zip" : "");
	}

//	static {
//		String s = "^[^|/]+\\.zip\\|[^|/]+/$";
//		s = "^(?<id>[^|/]+)\\Q.zip|\\E(?<IFD0property0dataobject0id>(?<IFD0representation0dataobject0fairspec0nmr0vendor1dataset>[^|/]+))\\Q/\\E$";
//		s = "^(?<id>[^|/]+)\\Q.zip|\\E(?<IFD0property0dataobject0id>(?<IFD0representation0dataobject0fairspec0nmr0vendor1dataset>[^|/]+))\\Q/\\E$";
//		Pattern p = Pattern.compile(s);
//		Matcher m = p.matcher("start.zip|xx/");
//		m = p.matcher("PB-1997B-lc1/1/");
//		System.out.println(m.matches());		
//		System.out.println(m.matches());
//		System.out.println(m.matches());
//
//	}

}
