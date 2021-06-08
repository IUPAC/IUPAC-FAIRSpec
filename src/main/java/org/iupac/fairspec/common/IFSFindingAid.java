package org.iupac.fairspec.common;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;

import org.iupac.fairspec.api.IFSObjectAPI;

/**
 * The master class for a full collection, as from a publication or thesis or whatever.
 * This class ultimately extends ArrayList, so all of the methods of that class are allowed, 
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class IFSFindingAid extends IFSCollection<IFSCollection<?>> {

	private Map<String, ZipEntry> zipContents;
	private String url;

	private String currentObjectFileName;
	private IFSStructure currentStructure;
	private IFSSpecData currentSpecData;

	public final static int STRUCTURE_COLLECTION = 0;
	public final static int SPECDATA_COLLECTION = 1;
	public final static int STRUCTURESPEC_COLLECTION = 2;
	public final static int ANALYSIS_COLLECTION = 3;
	
	{
		// these four slots are given
		add(null);
		add(null);
		add(null);
		add(null);
	}
	
	private IFSStructureCollection structureCollection;
	private IFSSpecDataCollection specDataCollection;
	private IFSStructureSpecCollection structureSpecCollection;
	private IFSAnalysisCollection analysisCollection;
	
	public IFSFindingAid(String name, String sUrl) {
		super(name, IFSObjectAPI.ObjectType.FindingAid);
		url = sUrl;
	}
	
	public String getURL() {
		return url;
	}

	public void beginAddObject(String fname) {
		if (currentObjectFileName != null)
			endAddObject();
		currentObjectFileName = fname;
	}

	public void addObject(String param, String value) throws IFSException {
		if (currentObjectFileName == null)
			throw new IFSException("addObject " + param + " " + value + " called with no current object file name");
		ObjectType type = IFSObjectAPI.getObjectTypeForName(param);
		switch (type) {
		case AnalysisCollection:
		case Analysis:
			System.out.println("Analysis not implemented");
			getAnalysisCollection();
			break;
		case IRSpecData:
		case MSSpecData:
		case NMRSpecData:
		case RAMANSpecData:
			currentSpecData = getSpecDataCollection(type).getSpecDataFor(param, value, currentObjectFileName, type);
//			if (currentSpecData == null) {
//			} else {
//				String key = param + ";" + value;
//				currentSpecData.getRepresentation(key);
//			}
			break;
		case Structure:
			if (currentStructure == null) {
				currentStructure = getStructureCollection().getStructureFor(param, value, currentObjectFileName);
			} else {
				currentStructure.getRepresentation(param + ";" + value);
			}
			break;
		case StructureSpecCollection:
			// valid data information? maybe
			getStructureSpecCollection();
			break;
		case SpecData: 
		case SpecDataCollection:
		case StructureCollection:
			// should not be generic
		case FindingAid:
		default:
			System.err.println("IFSFindingAid could not add " + param + " " + value + " for " + currentObjectFileName);
			break;
		}
	}

	public IFSSpecDataCollection getSpecDataCollection(ObjectType type) {
		if (specDataCollection == null) {
			super.set(SPECDATA_COLLECTION, specDataCollection = new IFSSpecDataCollection(name, type));			
		}
		return specDataCollection;
	}

	public IFSStructureCollection getStructureCollection() {
		if (structureCollection == null) {
			super.set(STRUCTURE_COLLECTION, structureCollection = new IFSStructureCollection(name));			
		}
		return structureCollection;
	}

	public IFSStructureSpecCollection getStructureSpecCollection() {
		if (structureSpecCollection == null) {
			super.set(STRUCTURESPEC_COLLECTION, structureSpecCollection = new IFSStructureSpecCollection(name));			
		}
		return structureSpecCollection;
	}

	public IFSAnalysisCollection getAnalysisCollection() {
		if (analysisCollection == null)
			super.set(ANALYSIS_COLLECTION, analysisCollection = new IFSAnalysisCollection(name));
		return analysisCollection;
	}

	@Override
	public IFSCollection<?> remove(int index) {
		return (index < 4 ? null : super.remove(index));		
	}

	@Override
	public boolean remove(Object o) {
		for (int i = 4; --i >= 0;) {
			if (get(i) == o)
				return false;
		}
		return super.remove(o);
	}
	
	@Override
	public IFSCollection<?> set(int i, IFSCollection<?> c) {
		return (i >= 4 ? super.set(i, c) : null);
	}

	public void endAddObject() {
		if (currentObjectFileName == null)
			return;
		if (currentStructure != null && currentSpecData != null) {
			getStructureSpecCollection().addPair(currentStructure, currentSpecData);
		}
			
		currentObjectFileName = null;
		currentStructure = null;
		currentSpecData = null;		
	}

	public Map<String, ZipEntry> getZipContents() {
		if (zipContents == null)
			zipContents = new LinkedHashMap<String, ZipEntry>();
		return zipContents;
	}

	public void finalizeExtraction() {
		System.out.println("IFSFindingAid extraction complete:\n" 
				+ getStructureCollection().size() + " structures "
				+ getSpecDataCollection(ObjectType.Unknown).size() + " specdata "
				+ getStructureSpecCollection().size() + " structure-spectra bindings");
	}

}