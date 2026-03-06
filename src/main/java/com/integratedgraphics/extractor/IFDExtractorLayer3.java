package com.integratedgraphics.extractor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.iupac.fairdata.api.IFDSerializerI;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecExtractorHelper.FileList;
import org.iupac.fairdata.core.IFDCollection;
import org.iupac.fairdata.core.IFDRepresentableObject;
import org.iupac.fairdata.core.IFDRepresentation;
import org.iupac.fairdata.dataobject.IFDDataObject;
import org.iupac.fairdata.dataobject.IFDDataObjectRepresentation;
import org.iupac.fairdata.util.IFDDefaultJSONSerializer;

import com.integratedgraphics.extractor.ExtractorUtils.ArchiveEntry;
import com.integratedgraphics.extractor.ExtractorUtils.CacheRepresentation;
import com.integratedgraphics.extractor.ExtractorUtils.ExtractorResource;

/**
 * Phase 3: Clean up the collections.
 * 
 * Carry out a final update of object type and len records.
 * 
 * Remove unmanifested representations, check for duplicate specdata.
 * 
 * Remove invalidated data (as might occur from splitting a Bruker directory
 * into two single-experiment zip files).
 *  
 * Finish writing the files, finalize the extraction, and create the finding aid.
 * 
 * @author Bob Hanson (hansonr@stolaf.edu)
 *
 */
abstract class IFDExtractorLayer3 extends IFDExtractorLayer2 {

	private List<FileList> rootLists;

	private String resourceList;

	private String contentsFile = "c:/temp/t.xls";

	private int timestampRemovalCount;

	@SuppressWarnings("unchecked")
	protected String processPhase3() throws IFDException, IOException {

		// Phase 3a
		setCurrentPhase("3a");

		// Update lengths for representations.
		// Flag files that are unused because they did not fit a configuration record.

		phase3aUpdateCachedRepresentations();
		checkStopAfter("3a");

		// Phase 3b
		setCurrentPhase("3b");
		
		// Clean up the collection by removing any unmanifested files.

		System.out.println(helper.dumpState());
		
		if (insitu) {
			
			// grab any remaining representations that should be included 
			// in the Finding Aid *as well as* in the collection.
			// also remove all localDir references
			
			phase3bUpdateInSitu((IFDCollection<IFDRepresentableObject<?>>) (Object) helper.getStructureCollection());
			phase3bUpdateInSitu((IFDCollection<IFDRepresentableObject<?>>) (Object) helper.getSpecCollection());
		} else {
			// otherwise, we remove any unmanifested representations.
			phase3bRemoveUnmanifestedRepresentations();
		}
		checkStopAfter("3b");

		setCurrentPhase("3c");

		phase3cCheckForDuplicateSpecData();
		checkStopAfter("3c1");
		helper.removeInvalidData();
		checkStopAfter("3c");
		
		if (contentsFile != null) {
			writeContents();
		}
		

		// Write the _IFD_manifest.json, _IFD_ignored.json and _IFD_extract.json files.

		writeRootManifests();

		// Push collections to the Finding Aid

		String msg = helper.finalizeExtraction(htURLReferences);
		log(msg);

		// Create the finding aid serialization

		String serializedFA = phase3SerializeFindingAid();
		
		return serializedFA;
	}

	private void writeContents() {
		if (noOutput)
			return;
		StringBuilder sb = new StringBuilder();
		 for (String k : htArchiveContents.keySet()) {
			 sb.append(k).append('\n');
			 Map<String, ArchiveEntry> map = htArchiveContents.get(k);
			 for ( Entry<String, ArchiveEntry> e : map.entrySet()) {
				 String s = e.getKey().replace('/', '\t').replaceAll("\\|", "\t|\t");
				 sb.append(s).append('\n');
			 }
		 }

		 try {
			writeBytesToFile(sb.toString().getBytes(), new File(contentsFile));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void phase3bUpdateInSitu(IFDCollection<IFDRepresentableObject<?>> c) {
		for(IFDRepresentableObject<?> o : c) {
			for (IFDRepresentation r : o) {
				r.updateInSitu();
			}
		}
	}

	/**
	 * Implementing subclass could use a different serializer.
	 * 
	 * @return a serializer
	 */
	private IFDSerializerI getSerializer() {
		return new IFDDefaultJSONSerializer();
	}

	private void outputListJSON(String name, File file) throws IOException {
		if (noOutput)
			return;
		int[] ret = new int[1];
		String json = helper.getFileListJSON(name, rootLists, resourceList, extractScriptFile.getName(), ret);
		writeBytesToFile(json.getBytes(), file);
		log("!saved " + file + " (" + ret[0] + " items)");
	}

	/**
	 * Look for spectra with identical timestamps mod 1800 (30 minutes).
	 * Some of these will be valid differences, so we now check to see
	 * if they are truly identical, and if so, we merge the second (which will be MNova)
	 * into the first (Bruker) as simply another representation, not a new spectrum.
	 * 
	 * 
	 */
	private void phase3cCheckForDuplicateSpecData() {
		timestampRemovalCount = 0;
		if (timestampSpectraObjectHashMap == null) 
			return;
		for (Entry<Integer, ArrayList<IFDDataObject>> e : 		 
			timestampSpectraObjectHashMap.entrySet()) {
			List<IFDDataObject> list = e.getValue();
			int n = list.size();
			if (n < 2)
				continue;
			for (int i = 0; i < list.size(); i++) {
				IFDDataObject o1 = list.get(i);
				for (int j = i + 1; j < list.size(); j++) {
					IFDDataObject o2 = list.get(j);
					if (helper.mergeDataObjectsIfMatching(o2, o1)) {
						list.remove(o2);
						--j;
						log("!phase 3c timestamp duplicate objects merged: " + o2.getID() + " -> " + o1.getID());
						timestampRemovalCount++;
					}
					
				}
			}
			
			log("!phase 3c timestamp check merged " + timestampRemovalCount + "data objects");
		}
	}

	/**
	 * Remove all data objects that (no longer) have any representations.
	 * 
	 * The issue here is that sometimes we have to identify directories that are not
	 * going to be zipped up in the end, because they do not match the rezip
	 * trigger.
	 */
	private void phase3bRemoveUnmanifestedRepresentations() {
		boolean isRemoved = false;
		for (IFDRepresentableObject<IFDDataObjectRepresentation> spec : faHelper.getSpecCollection()) {
			List<IFDRepresentation> lstRepRemoved = new ArrayList<>();
			for (Object o : spec) {
				IFDRepresentation rep = (IFDRepresentation) o;
				if (setLocalFileLength(rep) == 0) {
					lstRepRemoved.add(rep);
					// zip file reference in extact.json could 
					// actually reference only an extracted PDF
					// this can be normal 
					// -- pdf created two different ways, for example.
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

	/**
	 * Serialize the finding aid (as JSON).
	 * 
	 * @return serialized finding aid
	 * @throws IOException
	 */
	private String phase3SerializeFindingAid() throws IOException {
		log("!Extractor.extractAndCreateFindingAid serializing...");
		ArrayList<Object> products = rootPaths;
		IFDSerializerI ser = getSerializer();
		if (createZippedCollection && rootPaths != null) {
			products.add(new File(targetPath + "/_IFD_extract.json"));
			products.add(new File(targetPath + "/_IFD_ignored.json"));
			products.add(new File(targetPath + "/_IFD_manifest.json"));
		}
		long[] times = new long[3];
		String serializedFindingAid = faHelper.createSerialization((insitu || readOnly && !createFindingAidOnly ? null : targetPath),
				createZippedCollection ? products : null, ser, times);
		log("!Extractor serialization done " + times[0] + " " + times[1] + " " + times[2] + " ms "
				+ serializedFindingAid.length() + " bytes");
		return serializedFindingAid;
	}

	/**
	 * Set the type and len fields for structure and spec data
	 */
	private void phase3aUpdateCachedRepresentations() {
		for (String ckey : propertyManagerCache.keySet()) {
			CacheRepresentation r = propertyManagerCache.get(ckey);
			IFDRepresentableObject<?> obj = getObjectFromLocalizedName(ckey, null);
			if (obj == null || !r.isValid) {
				String path = r.getRef().getOriginPath().toString();
				if (r.isValid) {
					logDigitalItemIgnored(path, ckey, "it was never associated with an object",
							"phase 3a addCachedRepresentationsToObjects");
					try {
						addFileToFileLists(path, LOG_IGNORED, r.getLength(), null);
					} catch (IOException e) {
						// not possible
					}
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
			IFDRepresentation r1 = obj.findOrAddRepresentation(null, r.getRef().getResourceID(),
					originPath, r.getRef().getlocalDir(), localName, r.getData(), type, null);
			if (type != null && r1.getType() == null)
				r1.setType(type);
			if (mediatype != null && r1.getMediaType() == null)
				r1.setMediaType(mediatype);
			if (r1.getLength() == 0)
				r1.setLength(r.getLength());
		}
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
	private void writeRootManifests() throws IOException {
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
			File f = new File(targetPath + "/_IFD_extract.json");
			writeBytesToFile(extractScript.getBytes(), f);

			outputListJSON("manifest", new File(targetPath + "/_IFD_manifest.json"));
			if (nign > 0)
				outputListJSON("ignored", new File(targetPath + "/_IFD_ignored.json"));
			if (nrej > 0)
				outputListJSON("rejected", new File(targetPath + "/_IFD_rejected.json"));
		}
	}


}
