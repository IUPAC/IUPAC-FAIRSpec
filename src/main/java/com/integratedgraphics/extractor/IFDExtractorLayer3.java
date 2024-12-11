package com.integratedgraphics.extractor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.iupac.fairdata.api.IFDSerializerI;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecCompoundAssociation;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecCompoundCollection;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecExtractorHelper.FileList;
import org.iupac.fairdata.core.IFDAssociation;
import org.iupac.fairdata.core.IFDCollection;
import org.iupac.fairdata.core.IFDObject;
import org.iupac.fairdata.core.IFDRepresentableObject;
import org.iupac.fairdata.core.IFDRepresentation;
import org.iupac.fairdata.dataobject.IFDDataObject;
import org.iupac.fairdata.dataobject.IFDDataObjectCollection;
import org.iupac.fairdata.dataobject.IFDDataObjectRepresentation;
import org.iupac.fairdata.util.IFDDefaultJSONSerializer;

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
 * @author hanso
 *
 */
abstract class IFDExtractorLayer3 extends IFDExtractorLayer2 {

	private List<FileList> rootLists;

	private String resourceList;

	@SuppressWarnings("unchecked")
	protected String processPhase3() throws IFDException, IOException {

		phase3aUpdateCachedRepresentations();
		checkStopAfter("3a");
		
		// clean up the collection

		if (insitu) {
			phase3bSetInSitu((IFDCollection<IFDRepresentableObject<?>>)(Object) helper.getStructureCollection());
			phase3bSetInSitu((IFDCollection<IFDRepresentableObject<?>>)(Object) helper.getSpecCollection());
		} else {
			phase3bRemoveUnmanifestedRepresentations();
		}
		checkStopAfter("3b");

		phase3cCheckForDuplicateSpecData();
		helper.removeInvalidData();
		checkStopAfter("3c");
		

		// write the files and create the finding aid serialization

		writeRootManifests();
		
		String msg = helper.finalizeExtraction(htURLReferences);
		log(msg);
		
		String serializedFA = phase3SerializeFindingAid();
		return serializedFA;
	}

	private void phase3bSetInSitu(IFDCollection<IFDRepresentableObject<?>> c) {
		for(IFDRepresentableObject<?> o : c) {
			for (IFDRepresentation r : o) {
				r.setInSitu();
			}
		}
	}

	/**
	 * Implementing subclass could use a different serializer.
	 * 
	 * @return a serializer
	 */
	private IFDSerializerI getSerializer() {
		return new IFDDefaultJSONSerializer(isByID);
	}

	private void outputListJSON(String name, File file) throws IOException {
		int[] ret = new int[1];
		String json = helper.getFileListJSON(name, rootLists, resourceList, extractScriptFile.getName(), ret);
		writeBytesToFile(json.getBytes(), file);
		log("!saved " + file + " (" + ret[0] + " items)");
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
	private void phase3cCheckForDuplicateSpecData() {
		BitSet bs = new BitSet();
		FAIRSpecCompoundCollection ssc = faHelper.getCompoundCollection();
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
	private void phase3bRemoveUnmanifestedRepresentations() {
		boolean isRemoved = false;
		for (IFDRepresentableObject<IFDDataObjectRepresentation> spec : faHelper.getSpecCollection()) {
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
			products.add(new File(targetDir + "/_IFD_extract.json"));
			products.add(new File(targetDir + "/_IFD_ignored.json"));
			products.add(new File(targetDir + "/_IFD_manifest.json"));
		}
		long[] times = new long[3];
		String serializedFindingAid = faHelper.createSerialization((readOnly && !createFindingAidOnly ? null : targetDir),
				createZippedCollection ? products : null, ser, times);
		log("!Extractor serialization done " + times[0] + " " + times[1] + " " + times[2] + " ms "
				+ serializedFindingAid.length() + " bytes");
		return serializedFindingAid;
	}

	/**
	 * Set the type and len fields for structure and spec data
	 */
	private void phase3aUpdateCachedRepresentations() {

		for (String ckey : vendorCache.keySet()) {
			CacheRepresentation r = vendorCache.get(ckey);
			IFDRepresentableObject<?> obj = htLocalizedNameToObject.get(ckey);
			if (obj == null) {
				String path = r.getRef().getOriginPath().toString();
				logDigitalItemIgnored(path, ckey, "addCachedRepresentationsToObjects");
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
					r.getRef().getlocalDir(), localName, r.getData(), type, null);
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
			File f = new File(targetDir + "/_IFD_extract.json");
			writeBytesToFile(extractScript.getBytes(), f);

			outputListJSON("manifest", new File(targetDir + "/_IFD_manifest.json"));
			if (nign > 0)
				outputListJSON("ignored", new File(targetDir + "/_IFD_ignored.json"));
			if (nrej > 0)
				outputListJSON("rejected", new File(targetDir + "/_IFD_rejected.json"));
		}
	}


}
