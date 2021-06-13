package org.iupac.fairspec.spec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;

import org.iupac.fairspec.api.IFSObjectAPI;
import org.iupac.fairspec.api.IFSObjectAPI.ObjectType;
import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.common.IFSFindingAid;
import org.iupac.fairspec.common.IFSObject;
import org.iupac.fairspec.common.IFSStructure;
import org.iupac.fairspec.common.IFSStructureCollection;

/**
 * The master class for a full collection, as from a publication or thesis or whatever.
 * This class ultimately extends ArrayList, so all of the methods of that class are allowed, 
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class IFSSpecDataFindingAid extends IFSFindingAid {

	private String currentObjectFileName;
	private IFSStructure currentStructure;
	private IFSSpecData currentSpecData;

	public final static int STRUCTURE_COLLECTION = 0;
	public final static int SPECDATA_COLLECTION = 1;
	public final static int STRUCTURESPEC_COLLECTION = 2;
	public final static int ANALYSIS_COLLECTION = 3;
	
	private IFSStructureCollection structureCollection;
	private IFSSpecDataCollection specDataCollection;
	private IFSStructureSpecCollection structureSpecCollection;
	private IFSAnalysisCollection analysisCollection;
	
	public IFSSpecDataFindingAid(String name, String sUrl) {
		super(name, IFSObjectAPI.ObjectType.SpecDataFindingAid, sUrl);
		add(null);
		add(null);
		add(null);
		add(null);
	}
	
	public void beginAddObject(String fname) {
		if (currentObjectFileName != null)
			endAddObject();
		currentObjectFileName = fname;
	}
	
	/** This list will grow.
	 * 
	 * @param propName
	 * @return
	 */
	public static ObjectType getObjectTypeForName(String propName) {
		if (propName.startsWith("IFS.finding.aid."))
			return ObjectType.SpecDataFindingAid;
		if (propName.startsWith("IFS.structure."))
			return ObjectType.Structure;
		if (propName.startsWith("IFS.analysis."))
			return ObjectType.Analysis;
		if (propName.startsWith("IFS.nmr."))
			return ObjectType.NMRSpecData;
		if (propName.startsWith("IFS.ir."))
			return ObjectType.IRSpecData;
		if (propName.startsWith("IFS.ms."))
			return ObjectType.MSSpecData;
		if (propName.startsWith("IFS.raman."))
			return ObjectType.RAMANSpecData;
		return ObjectType.Unknown;		
	}

	public void addObject(String param, String value) throws IFSException {
		if (currentObjectFileName == null)
			throw new IFSException("addObject " + param + " " + value + " called with no current object file name");
		ObjectType type = getObjectTypeForName(param);
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
		case SpecDataFindingAid:
		default:
			System.err.println("IFSSpeDataFindingAid could not add " + param + " " + value + " for " + currentObjectFileName);
			break;
		}
	}

	public IFSSpecDataCollection getSpecDataCollection(ObjectType type) {
		if (specDataCollection == null) {
			setSafely(SPECDATA_COLLECTION, specDataCollection = new IFSSpecDataCollection(name, type));			
		}
		return specDataCollection;
	}

	public IFSStructureCollection getStructureCollection() {
		if (structureCollection == null) {
			setSafely(STRUCTURE_COLLECTION, structureCollection = new IFSStructureCollection(name));			
		}
		return structureCollection;
	}

	public IFSStructureSpecCollection getStructureSpecCollection() {
		if (structureSpecCollection == null) {
			setSafely(STRUCTURESPEC_COLLECTION, structureSpecCollection = new IFSStructureSpecCollection(name));			
		}
		return structureSpecCollection;
	}

	public IFSAnalysisCollection getAnalysisCollection() {
		if (analysisCollection == null)
			setSafely(ANALYSIS_COLLECTION, analysisCollection = new IFSAnalysisCollection(name));
		return analysisCollection;
	}
	public IFSObject<?> endAddObject() {
		try {
			return (currentObjectFileName == null ? null
					: currentStructure != null && currentSpecData != null
							? getStructureSpecCollection().addSpec(currentObjectFileName, currentStructure, currentSpecData)
							: currentStructure != null ? currentStructure : currentSpecData);
		} finally {
			currentObjectFileName = null;
			currentStructure = null;
			currentSpecData = null;
		}
	}

	public void finalizeExtraction() {
		if (getStructureCollection().size() == 0 && getSpecDataCollection(ObjectType.Unknown).size() == 0)
			System.out.println("FA error");
		System.out.println("! IFSFindingAid extraction complete:\n! " + urls + "\n! "
				+ getStructureCollection().size() + " structures "
				+ getSpecDataCollection(ObjectType.Unknown).size() + " specdata "
				+ getStructureSpecCollection().size() + " structure-spec bindings");
		for (IFSStructureSpec ssc : getStructureSpecCollection()) {
			System.out.println("Structure " + ssc.getFirstStructure().getName());
			for (IFSSpecData sd : ssc.getSpecDataCollection()) {
				System.out.println("\t" + sd);
			}
		}
	}

}