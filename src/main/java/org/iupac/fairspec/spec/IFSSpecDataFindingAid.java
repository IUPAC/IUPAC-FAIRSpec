package org.iupac.fairspec.spec;

import java.util.ArrayList;
import java.util.List;

import org.iupac.fairspec.api.IFSObjectI;
import org.iupac.fairspec.api.IFSSerializerI;
import org.iupac.fairspec.assoc.IFSStructureDataAssociation;
import org.iupac.fairspec.common.IFSConst;
import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.common.IFSRepresentation;
import org.iupac.fairspec.core.IFSCollection;
import org.iupac.fairspec.core.IFSDataObject;
import org.iupac.fairspec.core.IFSDataObjectCollection;
import org.iupac.fairspec.core.IFSFindingAid;
import org.iupac.fairspec.core.IFSObject;
import org.iupac.fairspec.core.IFSRepresentableObject;
import org.iupac.fairspec.sample.IFSSample;
import org.iupac.fairspec.sample.IFSSampleCollection;
import org.iupac.fairspec.struc.IFSStructure;
import org.iupac.fairspec.struc.IFSStructureCollection;

import javajs.util.PT;

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

	private String currentObjectZipName;
	private IFSStructure currentStructure;
	private IFSSpecData currentSpecData;
	private IFSSample currentSample;

	public final static int STRUCTURE_COLLECTION = 0;
	public final static int SPECDATA_COLLECTION = 1;
	public final static int STRUCTURE_SPEC_COLLECTION = 2;
	public final static int STRUCTURE_SPEC_ANALYSIS_COLLECTION = 3;
	public final static int SAMPLE_COLLECTION = 4;
	public final static int SAMPLE_SPEC_COLLECTION = 5;
	public final static int SAMPLE_SPEC_ANALYSIS_COLLECTION = 6;
	
	private IFSStructureCollection structureCollection;
	private IFSSpecDataCollection specDataCollection;
	private IFSStructureSpecCollection structureSpecCollection;
	private IFSStructureSpecAnalysisCollection structureSpecAnalysisCollection;
	private IFSSampleCollection sampleCollection;
	private IFSSampleSpecCollection sampleSpecCollection;
	private IFSSampleSpecAnalysisCollection sampleSpecAnalysisCollection;
	
	public IFSSpecDataFindingAid(String id, String sUrl) throws IFSException {
		super(null, IFSObjectI.ObjectType.SpecDataFindingAid, sUrl);
		setID(id);
		add(null);
		add(null);
		add(null);
		add(null);
		add(null);
		add(null);
		add(null);
		super.setMinCount(7);
	}
	
	public void beginAddObject(String fname) {
		if (currentObjectZipName != null)
			endAddObject();
		currentObjectZipName = fname;
	}
	
	/** This list will grow.
	 * 
	 * @param propName
	 * @return
	 * @throws IFSException 
	 */
	public static ObjectType getObjectTypeForName(String propName) throws IFSException {
		if (IFSConst.isFindingAid(propName))
			return ObjectType.SpecDataFindingAid;
		if (IFSConst.isProperty(propName))
			propName = PT.rep(propName,IFSConst.IFS_PROPERTY_FLAG, "\0");
		else if (IFSConst.isRepresentation(propName))
			propName = PT.rep(propName,IFSConst.IFS_REPRESENTATION_FLAG, "\0");
		else
			throw new IFSException("bad IFS identifier: " + propName);
		if (propName.startsWith("\0struc."))
			return ObjectType.Structure;
		if (propName.startsWith("\0analysis.structure.spec."))
			return ObjectType.StructureSpecAnalysis;
		if (propName.startsWith("\0analysis.sample.spec."))
			return ObjectType.SampleSpecAnalysis;
		if (propName.startsWith("\0spec.nmr."))
			return ObjectType.NMRSpecData;
		if (propName.startsWith("\0spec.ir."))
			return ObjectType.IRSpecData;
		if (propName.startsWith("\0spec.raman."))
			return ObjectType.RAMANSpecData;
		if (propName.startsWith("\0spec.hrms."))
			return ObjectType.HRMSSpecData;
		if (propName.startsWith("\0spec.ms."))
			return ObjectType.MSSpecData;
		if (propName.startsWith("\0spec.uvvis."))
			return ObjectType.UVVisSpecData;
		return ObjectType.Unknown;		
	}

	/**
	 * Add the object to the appropriate collection. 
	 * 
	 * @param rootPath
	 * @param param
	 * @param value
	 * @param localName
	 * @return
	 * @throws IFSException
	 */
	public IFSRepresentableObject<?> addObject(String rootPath, String param, String value, String localName) throws IFSException {
		if (currentObjectZipName == null)
			throw new IFSException("addObject " + param + " " + value + " called with no current object file name");
		
		ObjectType type = getObjectTypeForName(param);
		switch (type) {
		case SampleSpecAnalysisCollection:
		case SampleSpecAnalysis:
		case StructureSpecAnalysisCollection:
		case StructureSpecAnalysis:
			System.out.println("Analysis not implemented");
			getStructureSpecAnalysisCollection();
			// TODO
			return null;
		case NMRSpecData:
		case IRSpecData:
		case RAMANSpecData:
		case HRMSSpecData:
		case MSSpecData:
			currentSpecData = getSpecDataCollection().getSpecDataFor(rootPath, localName, param, value, currentObjectZipName, type, mediaTypeFromName(localName));
			currentSpecData.setUrlIndex(currentUrlIndex);
			return currentSpecData;
		case Sample:
			if (currentSample == null) {
				currentSample = getSampleCollection().getSampleFor(rootPath, localName, param, value, currentObjectZipName, mediaTypeFromName(localName));
				System.out.println("creating Sample " + currentSample.getName());
				currentSample.setUrlIndex(currentUrlIndex);
			} else {
				currentSample.setPropertyValue(param, value);
			}
			return currentSample;
		case Structure:
			if (currentStructure == null) {
				currentStructure = getStructureCollection().getStructureFor(rootPath, localName, param, value, currentObjectZipName, mediaTypeFromName(localName));
				System.out.println("creating Structure " + currentStructure.getName());
				currentStructure.setUrlIndex(currentUrlIndex);
			} else {
				currentStructure.setPropertyValue(param, value);
			}
			return currentStructure;
		case SampleSpecCollection:
			getSampleSpecCollection();
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
			System.err.println("IFSSpeDataFindingAid could not add " + param + " " + value + " for " + currentObjectZipName);
			break;
		}
		return null;
	}

	public IFSSpecDataCollection getSpecDataCollection() {
		if (specDataCollection == null) {
			try {
				setSafely(SPECDATA_COLLECTION, specDataCollection = new IFSSpecDataCollection(name));
			} catch (IFSException e) {
				// unattainable
			}
		}
		return specDataCollection;
	}

	public IFSStructureCollection getStructureCollection() {
		if (structureCollection == null) {
			try {
				setSafely(STRUCTURE_COLLECTION, structureCollection = new IFSStructureCollection(name));
			} catch (IFSException e) {
				// type is defined
			}			
		}
		return structureCollection;
	}

	public IFSSampleCollection getSampleCollection() {
		if (sampleCollection == null) {
			try {
				setSafely(SAMPLE_COLLECTION, sampleCollection = new IFSSampleCollection(name));
			} catch (IFSException e) {
				// type is defined
			}			
		}
		return sampleCollection;
	}

	public IFSStructureSpecCollection getStructureSpecCollection() {
		if (structureSpecCollection == null) {
			try {
				setSafely(STRUCTURE_SPEC_COLLECTION, structureSpecCollection = new IFSStructureSpecCollection(name));
			} catch (IFSException e) {
				// not attainable
			}
		}
		return structureSpecCollection;
	}

	public IFSSampleSpecCollection getSampleSpecCollection() {
		if (sampleSpecCollection == null) {
			try {
				setSafely(SAMPLE_SPEC_COLLECTION, sampleSpecCollection = new IFSSampleSpecCollection(name));
			} catch (IFSException e) {
				// not attainable
			}
		}
		return sampleSpecCollection;
	}

	public IFSStructureSpecAnalysisCollection getStructureSpecAnalysisCollection() {
		if (structureSpecAnalysisCollection == null)
			try {
				setSafely(STRUCTURE_SPEC_ANALYSIS_COLLECTION, structureSpecAnalysisCollection = new IFSStructureSpecAnalysisCollection(name));
			} catch (IFSException e) {
				// not attainable
			}
		return structureSpecAnalysisCollection;
	}
	
	public IFSSampleSpecAnalysisCollection getSampleSpecAnalysisCollection() {
		if (sampleSpecAnalysisCollection == null)
			try {
				setSafely(SAMPLE_SPEC_ANALYSIS_COLLECTION, sampleSpecAnalysisCollection = new IFSSampleSpecAnalysisCollection(name));
			} catch (IFSException e) {
				// not attainable
			}
		return sampleSpecAnalysisCollection;
	}
	
	public IFSObject<?> endAddObject() {
		try {
			if (currentObjectZipName == null)
				return null;
			if (currentStructure != null && currentSpecData != null)
				return getStructureSpecCollection().addSpec(currentObjectZipName, currentStructure, currentSpecData);
			return (currentStructure != null ? currentStructure : currentSpecData);
		} finally {
			currentObjectZipName = null;
			currentStructure = null;
			currentSpecData = null;
		}
	}

	public void finalizeExtraction() {
		if (getStructureCollection().size() == 0 && getSpecDataCollection().size() == 0)
			System.out.println("IFSSpecDataFindingAid no structures or spectra?");
		System.out.println("! IFSFindingAid extraction complete:\n! " + urls + "\n! "
				+ getStructureCollection().size() + " structures "
				+ getSpecDataCollection().size() + " specdata "
				+ getStructureSpecCollection().size() + " structure-spec bindings");
		for (IFSStructureDataAssociation ssc : getStructureSpecCollection()) {
			System.out.println("Structure " + ssc.getFirstStructure().getName());
			for (IFSDataObject<?> sd : ssc.getDataObjectCollection()) {
				System.out.println("\t" + sd);
			}
		}
	}

	public static String mediaTypeFromName(String fname) {
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
	
	public IFSRepresentation getSpecDataRepresentation(String zipName) {
		return (specDataCollection == null ? null : specDataCollection.getRepresentation(zipName));
	}
	
	@Override
	protected void serializeList(IFSSerializerI serializer) {
		listCollection(serializer, "structures", structureCollection);
		listCollection(serializer, "specData", specDataCollection);
		listCollection(serializer, "structureSpecData", structureSpecCollection);
		listCollection(serializer, "structureSpecAnalyses", structureSpecAnalysisCollection);
		listCollection(serializer, "samples", sampleCollection);
		listCollection(serializer, "sampleSpecData", sampleSpecCollection);
		listCollection(serializer, "sampleSpecAnalyses", sampleSpecAnalysisCollection);
	}

	private void listCollection(IFSSerializerI serializer, String name, IFSCollection<?> c) {
		if (c != null && c.size() > 0) {
			// normalize indices
			for (int i = c.size(); --i >= 0;)
				((IFSObject<?>) c.get(i)).setIndex(i);
			serializer.addAttrInt(name + "Count", c.size());
			serializer.addObject(name, c);
		}
	}

	public int cleanStructureSpecs() {
		List<IFSStructureDataAssociation> lstRemove = new ArrayList<>();
		IFSStructureSpecCollection ssc = getStructureSpecCollection();
		int n = 0;
		for (IFSStructureDataAssociation ss : ssc) {
			List<IFSSpecData> empty = new ArrayList<>();
			IFSDataObjectCollection<IFSDataObject<?>> dc = ss.getDataObjectCollection();
			for (IFSDataObject<?> d : dc) {
				if (d.size() == 0)
					empty.add((IFSSpecData) d);
			}
			n += empty.size();
			dc.removeAll(empty);
			if (dc.size() == 0) {
				lstRemove.add(ss);
				IFSStructure st = ss.getFirstStructure();
				System.out.println("removing structure " + st.getName());
				getStructureCollection().remove(st);
				n++;
			}
		}
		n += lstRemove.size();
		ssc.removeAll(lstRemove);
		return n;
	}

}