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
import org.iupac.fairdata.contrib.fairspec.FAIRSpecFindingAidHelper;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecUtilities.SpreadsheetReader;
import org.iupac.fairdata.core.IFDObject;
import org.iupac.fairdata.core.IFDReference;
import org.iupac.fairdata.core.IFDRepresentableObject;
import org.iupac.fairdata.core.IFDRepresentation;
import org.iupac.fairdata.dataobject.IFDDataObject;
import org.iupac.fairdata.extract.DefaultStructureHelper;
import org.iupac.fairdata.extract.PropertyManagerI;
import org.iupac.fairdata.sample.IFDSample;
import org.iupac.fairdata.structure.IFDStructure;

import com.integratedgraphics.extractor.ExtractorUtils.AWrap;
import com.integratedgraphics.extractor.ExtractorUtils.ArchiveEntry;
import com.integratedgraphics.extractor.ExtractorUtils.ArchiveInputStream;
import com.integratedgraphics.extractor.ExtractorUtils.CacheRepresentation;
import com.integratedgraphics.extractor.ExtractorUtils.DirectoryInputStream;
import com.integratedgraphics.extractor.ExtractorUtils.ExtractorResource;
import com.integratedgraphics.extractor.ExtractorUtils.ObjectParser;
import com.integratedgraphics.ifd.api.VendorPluginI;

abstract class IFDExtractorLayer2 extends IFDExtractorLayer1 {

	/**
	 * Phase 2: Carry out the actual extraction of metadata.
	 * 
	 * Set up the target directory for files:
	 * 
	 * _IFD_warnings.txt, _IFD_rejected.json, _IFD_ignored.json, _IFD_manifest.json,
	 * IFD.findingaid.json, and IFD.collection.zip
	 * 
	 * Phase 2a:
	 * 
	 * Scan zip entries or directories for representations of structures and
	 * spectral data.
	 * 
	 * Since some files (MNova, e.g.) contain multiple objects that are mixtures of
	 * structure and spectrum representations, we two two full scans.
	 * 
	 * Also, some file structures need to be fixed. Specifically, the name of the
	 * Bruker directory containing acqu must be a simple integer, or the dataset
	 * cannot be read back into TopSpin. Not all authors realize this. So we flag
	 * those for fixing.
	 * 
	 * In addition, files may come from more than one source (two FigShare ZIP
	 * files, for example).
	 * 
	 * First generate the ordered map of the archive contents, by resource.
	 * 
	 * Set up the rezipCache for vendors that need to do that (Bruker) where we want
	 * to fix the directory structure and break multiple numbered directories into
	 * separate zip files.
	 * 
	 * Iterate through all parser objects, loooking for objects.
	 * 
	 * There is one parser object created for each of the IFD-extract.json
	 * 
	 * "FAIRSpec.extractor.object" records.
	 *
	 * Phase 2b:
	 * 
	 * The file path points to a digital item in the aggregation that potentially
	 * could be a digital object in the IUPAC FAIRData Collection.
	 * 
	 * The StructureHelper identifies structures by file extensions (see
	 * ifd.properties), adding deferred properties such as InChI, InChIKey, and
	 * SMILES, as well as MOL files from CDX or CDXML.
	 * 
	 * Vendor plug-ins such as Bruker "claim" zip files or directories based on
	 * contained files, such as "acqu".
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
	 * Phase 2c:
	 * 
	 * All objects have been created.
	 * 
	 * An important feature of Extractor is that it can repackage zip files,
	 * removing resources that are totally unnecessary and extracting properties and
	 * representations using IFDVendorPluginI services.
	 * 
	 * If there is rezipping to be done, we iterate over all files again, this time
	 * doing all the fixes and creating new ZIP files in the IUPAC FAIRSpec
	 * Collection.
	 * 
	 * In Phase 2c we process the deferred properties and extracted representations.
	 * 
	 * 
	 * @author hanso
	 *
	 */
	private class ParserIterator implements Iterator<ObjectParser> {

		int i;

		ParserIterator() {
			extractorResource = null;
			thisRootPath = "";
		}

		@Override
		public boolean hasNext() {
			return (i < objectParsers.size() && !doneLocally());
		}

		@Override
		public ObjectParser next() {
			return objectParsers.get(i++);
		}

		private boolean doneLocally() {
			// BH I don't remember what this is about.
			// Why would be stop this process in the
			// case of test#9 we have two local caches.
//			if (localSourceDir != null) {
//				ObjectParser parser = objectParsers.get(i);
//				ExtractorResource source = parser.getDataSource();
//				System.out.println("doneLocally ? " + source);
//				// what is this about?? 
//				boolean done =
//						!source.isLocalStructures &&
//						(source.getSourceFile() != objectParsers.get(0).getDataSource().getSourceFile());
//				return done;
//			}
			return false;
		}

	}

	/**
	 * rezip data saved as an ISFRepresentation (for no particularly good reason)
	 */
	private CacheRepresentation currentRezipRepresentation;

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

	/**
	 * the number of IFDObjects created
	 */
	private int ifdObjectCount;

	/**
	 * cache of top-level resources to be rezipped
	 */
	private List<CacheRepresentation> rezipCache;

	/**
	 * working map from manifest names to structure or data object
	 */
	private Map<String, String> htZipRenamed = new LinkedHashMap<>();

	private Map<AWrap, IFDStructure> htStructureRepCache;

	private File currentZipFile;

	/**
	 * Slows this down a bit, but allows, for example, a CIF file to be both a
	 * structure and an object
	 */
	private boolean allowMultipleObjectsForRepresentations = true;

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

		// Phase 2a
		// -- generate the ordered map of the archive contents, by resource.
		// -- set up the rezipCache for vendors that need to do that (Bruker, multiple
		// <n>/)

		rezipCache = new ArrayList<>();
		Map<String, Map<String, ArchiveEntry>> htArchiveContents = phase2aInitializeZipData();

		ParserIterator iter = new ParserIterator();
		while (iter.hasNext()) {
			ObjectParser parser = nextParser(iter);

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
			// -- Vendor plug-ins such as MestreNova extract structure byte[]
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
				ObjectParser parser = nextParser(iter);
				if (parser.hasData())
					phase2ReadZipContentsIteratively(getTopZipStream(), "", PHASE_2C, null);
			}
		}

		// Vendors may produce new objects that need association or properties of those
		// objects. This happens in Phases 2a, 2b, and 2c

		phase2cProcessDeferredObjectProperties(null);

		// Phase 2d

		log("!Phase 2d check rejected files and update lists");

		iter = new ParserIterator();
		while (iter.hasNext()) {
			nextParser(iter);
			phase2ReadZipContentsIteratively(getTopZipStream(), "", PHASE_2D, null);
		}
	}

	private Map<String, Map<String, ArchiveEntry>> phase2aInitializeZipData() throws IOException, IFDException {

		Map<String, Map<String, ArchiveEntry>> contents = new LinkedHashMap<>();

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
						helper.setCurrentResourceByteLength(len);
					zipFileMap = phase2ReadZipContentsIteratively(is, "", PHASE_2A,
							new LinkedHashMap<String, ArchiveEntry>());
					contents.put(localizedTopLevelZipURL, zipFileMap);
				}
			}
		}
		return contents;
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
				CacheRepresentation rep = new CacheRepresentation(new IFDReference(helper.getCurrentSource().getID(),
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
	private IFDObject<?> phase2bAddIFDObjectsForName(ObjectParser parser, String originPath, String localizedName,
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
//			System.out.println("MEX " + zipEntry);
			IFDObject<?> obj = phase2bAddIFDObjectsForName(parser, originPath, localizedName, len);
			if (obj instanceof IFDRepresentableObject) {
				addFileToFileLists(originPath, LOG_OUTPUT, len, null);
				ifdObjectCount++;
			}
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
	private void phase2cProcessDeferredObjectProperties(String phase2OriginPath) throws IFDException, IOException {
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
				System.out.println("MEX NEW_RESOURCE " + value);
				initializeResource((ExtractorResource) value, false);
				continue;
			}
			cloning = key.equals(NEW_PAGE_KEY);
			if (cloning) {
				System.out.println("MEX NEW_PAGE_KEY" + value);
			}
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
			if (spec != null && !spec.isValid()) {
				// this is a second -- get first
				// spec = getClonedData(spec);
			}

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
				boolean removeAllRepresentations = false;
				boolean replaceOld = true;
				if (value instanceof String) {
					// e.g. MNova extracted _page=10
					// clear structure for not allowing an MNova structure to carry to next page?
//					struc = null; 
					if (dataObject == null) {
						dataObject = localSpec;
					} else {
						localSpec = dataObject;
					}
				} else {
					// e.g. Bruker created a new object from multiple <n>/ directories
					a = (Object[]) value;
					value = (String) a[0];
					IFDDataObject sp = (IFDDataObject) a[1];
					removeAllRepresentations = true;
					replaceOld = sp.isValid(); // will be true first time only
					localSpec = sp;
					newLocalName = (String) a[2];
				}
				String idExtension = (String) value;
				if (assoc == null && localSpec != null)
					assoc = faHelper.findCompound(null, localSpec);
				System.out.println("cloning for association " + assoc);
				IFDDataObject newSpec;
				if (localSpec == null) {
					// second Bruker directory, for example
					localSpec = (IFDDataObject) spec;
					newSpec = helper.cloneData(localSpec, idExtension, false);
					mapClonedData(localSpec, newSpec);
				} else {
					// internal Bruker directories
					// or MNova extract
					newSpec = helper.cloneData(localSpec, idExtension, replaceOld);
					mapClonedData(localSpec, newSpec);
				}
				spec = localSpec = newSpec;
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
					struc = faHelper.getFirstStructureForSpec((IFDDataObject) spec, false);
					if (struc == null) {
						struc = helper.addStructureForSpec(extractorResource.rootPath, (IFDDataObject) spec, ifdRepType,
								oPath, localName, name);

					}
					htStructureRepCache.put(w, struc);
					if (sample == null) {
						assoc = faHelper.findCompound(struc, (IFDDataObject) spec);
					} else {
						faHelper.associateSampleStructure(sample, struc);
					}
					// MNova 1 page, 1 spec, 1 structure Test #5
					addFileAndCacheRepresentation(oPath, null, bytes.length, ifdRepType, null, null);
					linkLocalizedNameToObject(localName, ifdRepType, struc);
					log("!Structure " + struc + " created and associated with " + spec);
				} else {
					assoc = faHelper.findCompound(struc, (IFDDataObject) spec);
					if (assoc == null) {
						assoc = faHelper.createCompound(struc, (IFDDataObject) spec);
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
			ArchiveEntry firstEntry, VendorPluginI vendor) throws IOException, IFDException {

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
				throw new IFDException("phase2cRezipEntry could not find object for " + lNameForObj);
			}
		}
		String basePath = baseName + (parent == null ? "" : parent);
		if (newDir == null) {
			newDir = "";
			boolean isMultiple = currentRezipRepresentation.isMultiple();
			if (isMultiple) {
				System.out.println("isMltiple!!" + currentRezipRepresentation);
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
		IFDRepresentation r = faHelper.getSpecDataRepresentation(localizedName);
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

	private void phase2dCheckOrReject(ArchiveInputStream ais, String oPath, long len) throws IOException {
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
	private Map<String, ArchiveEntry> phase2ReadZipContentsIteratively(InputStream is, String baseOriginPath, int phase,
			Map<String, ArchiveEntry> originToEntryMap) throws IOException, IFDException {
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
			if (FAIRSpecUtilities.isZip(oPath)) {
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

	/**
	 * Starting with "xxxx/xx#page1.mol" return "page1".
	 * 
	 * These will be from MNova processing.
	 * 
	 * @param originPath
	 * @return
	 */
	private static String getStructureNameFromPath(String originPath) {
		String name = originPath.substring(originPath.lastIndexOf("/") + 1);
		name = name.substring(name.indexOf('#') + 1);
		int pt = name.indexOf('.');
		if (pt >= 0)
			name = name.substring(0, pt);
		return name;
	}

	private Map<String, IFDRepresentableObject<?>> htCloneMap = new HashMap<>();

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
	private IFDRepresentation addFileAndCacheRepresentation(String originPath, String localizedName, long len,
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

	/**
	 * Add metadata from a simple file (default IFD_METADATA) with key=value pairs
	 * one per line.
	 * 
	 * @param data
	 */
	private void addIFDMetadata(String data) {
		List<String[]> list = FAIRSpecUtilities.getIFDPropertyMap(data);
		for (int i = 0, n = list.size(); i < n; i++) {
			String[] item = list.get(i);
			addProperty(item[0], item[1]);
		}
	}

	private ArchiveInputStream getArchiveInputStream(InputStream is) throws IOException {
		return new ArchiveInputStream(is, extractorResource.getSourceFile());
	}

	private IFDRepresentableObject<?> getClonedData(IFDRepresentableObject<?> spec) {
		IFDRepresentableObject<?> d = (spec.isValid() ? null : htCloneMap.get(spec.getID()));
		return (d == null ? spec : d);
	}

//	private IFDRepresentableObject<?> getClonedDataSource(IFDRepresentableObject<?> spec) {
//		IFDRepresentableObject<?> d = (spec.isValid() ? null : htCloneMap.get("$" + spec.getID()));
//		return d;
//	}

	private IFDRepresentableObject<?> getObjectFromLocalizedName(String name, String type) {
		IFDRepresentableObject<?> obj = (type == null ? null : htLocalizedNameToObject.get(type + name));
		return (obj == null ? htLocalizedNameToObject.get(name) : obj);
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
	private PropertyManagerI getPropertyManager(Matcher m) {
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

	private InputStream getTopZipStream() throws MalformedURLException, IOException {
		return (localizedTopLevelZipURL.endsWith("/") ? new DirectoryInputStream(localizedTopLevelZipURL)
				: new URL(localizedTopLevelZipURL).openStream());
	}

	/**
	 * Find the matching pattern for rezipN where N is the vendor index in
	 * activeVendors. Presumably there will be only one vendor per match. (Two
	 * vendors will not be looking for MOL files, for example.)
	 * 
	 * @param m
	 * @return
	 */
	private VendorPluginI getVendorForRezip(Matcher m) {
		for (int i = bsRezipVendors.nextSetBit(0); i >= 0; i = bsRezipVendors.nextSetBit(i + 1)) {
			String ret = m.group("rezip" + i);
			if (ret != null && ret.length() > 0) {
				return VendorPluginI.activeVendors.get(i).vendor;
			}
		}
		return null;
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
		// System.out.println(resource);
		// localize the URL if we are using a local copy of a remote resource.
		localizedTopLevelZipURL = localizeURL(resource.getSourceFile());
		String s = resource.getSourceFile();
		String zipPath = s.substring(s.lastIndexOf(":") + 1);
		if (isInit) {

			if (debugging)
				log("opening " + localizedTopLevelZipURL);
			String rootPath = resource.createZipRootPath(zipPath);
			File rootDir = new File(targetDir + "/" + rootPath);
			rootDir.mkdir();
			resource.setLists(rootPath, ignoreRegex, acceptRegex);

		}
		lstManifest = resource.lstManifest;
		lstIgnored = resource.lstIgnored;
		if (helper.getCurrentSource() != helper.addOrSetSource(resource.getRemoteSource(), resource.rootPath)) {
			if (isInit)
				addDeferredPropertyOrRepresentation(NEW_RESOURCE_KEY, resource, false, null, null);
		}
		extractorResource = resource;

		// only used for logging:
		thisRootPath = resource.rootPath;
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

	private void mapClonedData(IFDRepresentableObject<?> oldSpec, IFDRepresentableObject<?> newSpec) {
		htCloneMap.put(oldSpec.getID(), newSpec);
		htCloneMap.put("$" + newSpec.getID(), oldSpec);
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
	 * Pull a full row from a file such as Manifest.xlsx with headers including a
	 * compound numbers, for example.
	 */
	@Override
	public void setNewObjectMetadata(IFDObject<?> o, String param) {
		// from FAIRSpecExtractorHelper
		String id;
		Map<String, Object> map;
		List<Object[]> metadata;
		if (htMetadata == null || (id = o.getID()) == null || (map = htMetadata.get(param)) == null
				|| !SpreadsheetReader.hasDataKey(map)
				|| (metadata = SpreadsheetReader.getRowDataForIndex(map, id)) == null)
			return;
		log("!Extractor adding " + metadata.size() + " metadata items for " + param + "=" + id);
		FAIRSpecExtractorHelper.addProperties(o, metadata);
	}

	private void setPropertyIfNotAlreadySet(IFDObject<?> obj, String key, Object value, String originPath) {
		Object currentValue = helper.setPropertyValueNotAlreadySet(obj, key, value, originPath);
		if (currentValue != null) {
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

	private void writeOriginToCollection(String originPath, byte[] bytes, long len) throws IOException {
		lstWritten.add(localizePath(originPath), (bytes == null ? len : bytes.length));
		if (!noOutput && bytes != null)
			writeBytesToFile(bytes, getAbsoluteFileTarget(originPath));
	}

}
