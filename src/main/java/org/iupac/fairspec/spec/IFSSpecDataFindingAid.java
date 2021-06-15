package org.iupac.fairspec.spec;

import org.iupac.fairspec.api.IFSObjectI;
import org.iupac.fairspec.assoc.IFSFindingAid;
import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.core.IFSObject;
import org.iupac.fairspec.core.IFSStructure;
import org.iupac.fairspec.core.IFSStructureCollection;

/**
 * The master class for a full FAIRSpec collection, as from a publication or
 * thesis, lab experiment, or spectral prediction.
 * 
 * This class ultimately extends ArrayList, so all of the methods of that class
 * are allowed. Access to this list is restricted, with the first four slots
 * reserved for an IFSStructureCollection, an IFSSpectDataCollection, an
 * IFSStructureSpecCollection, and an IFSAnalysisCollection.
 * 
 * 
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
	private IFSSpecAnalysisCollection analysisCollection;
	
	public IFSSpecDataFindingAid(String name, String sUrl) {
		super(name, IFSObjectI.ObjectType.SpecDataFindingAid, sUrl);
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
			return ObjectType.SpecAnalysis;
		if (propName.startsWith("IFS.spec.nmr."))
			return ObjectType.NMRSpecData;
		if (propName.startsWith("IFS.spec.ir."))
			return ObjectType.IRSpecData;
		if (propName.startsWith("IFS.spec.raman."))
			return ObjectType.RAMANSpecData;
		if (propName.startsWith("IFS.spec.hrms."))
			return ObjectType.HRMSSpecData;
		if (propName.startsWith("IFS.spec.ms."))
			return ObjectType.MSSpecData;
		if (propName.startsWith("IFS.spec.uvvis."))
			return ObjectType.UVVisSpecData;
		return ObjectType.Unknown;		
	}

	public IFSObject<?> addObject(String rootPath, String param, String value) throws IFSException {
		if (currentObjectFileName == null)
			throw new IFSException("addObject " + param + " " + value + " called with no current object file name");
		ObjectType type = getObjectTypeForName(param);
		switch (type) {
		case SpecAnalysisCollection:
		case SpecAnalysis:
			System.out.println("Analysis not implemented");
			getAnalysisCollection();
			return null;
		case IRSpecData:
		case MSSpecData:
		case NMRSpecData:
		case RAMANSpecData:
			currentSpecData = getSpecDataCollection(type).getSpecDataFor(rootPath, param, value, currentObjectFileName, type);
//			if (currentSpecData == null) {
//			} else {
//				String key = param + ";" + value;
//				currentSpecData.getRepresentation(key);
//			}
			return currentSpecData;
		case Structure:
			if (currentStructure == null) {
				currentStructure = getStructureCollection().getStructureFor(rootPath, param, value, currentObjectFileName);
			} else {
				currentStructure.setPath(rootPath);
				currentStructure.getRepresentation(param + ";" + value);
			}
			return currentStructure;
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
		return null;
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

	public IFSSpecAnalysisCollection getAnalysisCollection() {
		if (analysisCollection == null)
			setSafely(ANALYSIS_COLLECTION, analysisCollection = new IFSSpecAnalysisCollection(name));
		return analysisCollection;
	}
	
	public IFSObject<?> endAddObject() {
		try {
			if (currentObjectFileName == null)
				return null;
			if (currentStructure != null && currentSpecData != null)
				return getStructureSpecCollection().addSpec(currentObjectFileName, currentStructure, currentSpecData);
			return (currentStructure != null ? currentStructure : currentSpecData);
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

	public static String MediaTypeFromName(String fname) {
		int pt = Math.max(fname.lastIndexOf('/'), fname.lastIndexOf('.'));
		return (fname.endsWith(".zip") ? "application/zip"
				: fname.endsWith(".png") ? "image/png"
				: fname.endsWith(".cdx") ? "chemical/x-cdx (ChemDraw CDX)"
				: fname.endsWith(".cdxml") ? "chemical/x-cdxml (ChemDraw XML)"
						// see https://en.wikipedia.org/wiki/Chemical_file_format
				: fname.endsWith(".mol") ? "chemical/x-mdl-molfile"
				: fname.endsWith(".sdf") ? "chemical/x-mdl-sdfile"
				: fname.endsWith(".txt") || fname.endsWith(".log")
					|| fname.endsWith(".out") ? "text/plain"
				: fname.endsWith(".inchi") ? "chemical/x-inchi"
				: fname.endsWith(".smiles") 
				  || fname.endsWith(".smi") ? "chemical/x-daylight-smiles"
				: fname.endsWith(".pdf") ? "application/pdf"
				: fname.endsWith(".jpf") ? "application/octet-stream (JEOL)" 
				: fname.endsWith(".mnova") ? "application/octet-stream (mnova)"
				: pt >= 0 ? "?" + fname.substring(pt)
				: "?");
	}

}