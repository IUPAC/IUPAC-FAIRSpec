package org.iupac.fairdata.spec;

import java.util.ArrayList;
import java.util.List;

import org.iupac.fairdata.api.IFDSerializerI;
import org.iupac.fairdata.assoc.IFDStructureDataAssociation;
import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.common.IFDRepresentation;
import org.iupac.fairdata.core.IFDCollection;
import org.iupac.fairdata.core.IFDDataObject;
import org.iupac.fairdata.core.IFDDataObjectCollection;
import org.iupac.fairdata.core.IFDFindingAid;
import org.iupac.fairdata.core.IFDObject;
import org.iupac.fairdata.core.IFDRepresentableObject;
import org.iupac.fairdata.sample.IFDSample;
import org.iupac.fairdata.sample.IFDSampleCollection;
import org.iupac.fairdata.struc.IFDStructure;
import org.iupac.fairdata.struc.IFDStructureCollection;

import javajs.util.PT;

/**
 * The master class for a full FAIRSpec collection, as from a publication or
 * thesis, lab experiment, or spectral prediction.
 * 
 * This class ultimately extends ArrayList, so all of the methods of that class
 * are allowed. Access to this list is restricted, with the first four slots
 * reserved for an IFDStructureCollection, an IFDSpectDataCollection, an
 * IFDStructureSpecCollection, and an IFDAnalysisCollection.
 * 
 * 
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class IFDSpecDataFindingAid extends IFDFindingAid {

	public static interface SpecType {
		public final static String StructureSpec = "StructureSpec";
		public final static String StructureSpecCollection = "StructureSpecCollection";
		public final static String StructureSpecAnalysis = "StructureSpecAnalysis";
		public final static String StructureSpecAnalysisCollection = "StructureSpecAnalysisCollection";

		public final static String SampleSpec = "SampleSpec";
		public final static String SampleSpecCollection = "SampleSpecCollection";
		public final static String SampleSpecAnalysis = "SampleSpecAnalysis";
		public final static String SampleSpecAnalysisCollection = "SampleSpecAnalysisCollection";

		public final static String SpecData = "SpecData";
		public final static String SpecDataCollection = "SpecDataCollection";
		public final static String SpecAnalysis = "SpecAnalysis";
		public final static String SpecAnalysisCollection = "SpecAnalysisCollection";
		public final static String SpecDataFindingAid = "SpecDataFindingAid";

		// Predefined types include hrms, ir, ms, nmr, raman, and uvvis
		//
		// To add another type xxx, simply carry out these few steps:
		//
		// 1) Create the org/iupac/fairdata/spec/xxx/ folder
		//
		// 2) Create the xxx/IFDXXXSpecData and xxx/IFDXXXSpecDataRepresentation classes
		//
		// 3) Modify the code to suit, adding properties and patterns and methods to run to
		//    extract metadata from the data.
		
	}

	private IFDStructureCollection structureCollection;
	private IFDSpecDataCollection specDataCollection;
	private IFDStructureSpecCollection structureSpecCollection;
	private IFDStructureSpecAnalysisCollection structureSpecAnalysisCollection;
	private IFDSampleCollection sampleCollection;
	private IFDSampleSpecCollection sampleSpecCollection;
	private IFDSampleSpecAnalysisCollection sampleSpecAnalysisCollection;

	private String currentObject;
	private IFDStructure currentStructure;
	private IFDSpecData currentSpecData;
	private IFDSample currentSample;

	public final static int STRUCTURE_COLLECTION = 0;
	public final static int SPECDATA_COLLECTION = 1;
	public final static int STRUCTURE_SPEC_COLLECTION = 2;
	public final static int STRUCTURE_SPEC_ANALYSIS_COLLECTION = 3;
	public final static int SAMPLE_COLLECTION = 4;
	public final static int SAMPLE_SPEC_COLLECTION = 5;
	public final static int SAMPLE_SPEC_ANALYSIS_COLLECTION = 6;


	public IFDSpecDataFindingAid(String id, String creator) throws IFDException {
		super(null, SpecType.SpecDataFindingAid, creator);
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

	public void beginAddingObjects(String fname) {
		if (isAddingObjects())
			endAddingObjects();
		currentObject = fname;
	}
	
	public boolean isAddingObjects() {
		return (currentObject != null);
	}

	public void setCurrentObject(String fname) {
		currentObject = fname;
	}
	
	public IFDStructure getCurrentStructure() {
		return currentStructure;
	}
	
	public void setCurrentStructure(IFDStructure struc) {
		currentStructure = struc;
	}

	public IFDSpecData getCurrentSpecData() {
		return currentSpecData;
	}

	public void setCurrentSpecData(IFDSpecData spec) {
		currentSpecData = spec;
	}

	/**
	 * This list will grow.
	 * 
	 * @param propName
	 * @return
	 * @throws IFDException
	 */
	public static String getObjectTypeForName(String propName, boolean allowError) throws IFDException {
		if (IFDConst.isProperty(propName))
			propName = PT.rep(propName, IFDConst.IFD_PROPERTY_FLAG, "\0");
		else if (IFDConst.isRepresentation(propName))
			propName = PT.rep(propName, IFDConst.IFD_REPRESENTATION_FLAG, "\0");
		else if (allowError)
			return ObjectType.Unknown;
		else
			throw new IFDException("bad IFD identifier: " + propName);
		if (propName.startsWith("\0struc."))
			return ObjectType.Structure;
		if (propName.startsWith("\0analysis.structure.spec."))
			return SpecType.StructureSpecAnalysis;
		if (propName.startsWith("\0analysis.sample.spec."))
			return SpecType.SampleSpecAnalysis;
		if (propName.startsWith("\0spec."))
			return propName.substring(1, propName.indexOf(".", 6));
		return ObjectType.Unknown;
	}

	/**
	 * Add the object to the appropriate collection.
	 * 
	 * @param rootPath
	 * @param param
	 * @param id
	 * @param localName
	 * @return
	 * @throws IFDException
	 */
	public IFDRepresentableObject<?> addObject(String rootPath, String param, String id, String localName)
			throws IFDException {
		if (!isAddingObjects())
			throw new IFDException("addObject " + param + " " + id + " called with no current object file name");

		String type = getObjectTypeForName(param, false);

		if (type.startsWith("spec.")) {
			currentSpecData = getSpecDataCollection().getDataObjectFor(currentObject, rootPath, localName, param,
					id, type, mediaTypeFromName(localName));
			currentSpecData.setUrlIndex(currentSourceIndex);
			return currentSpecData;
		}
		switch (type) {
		case SpecType.SampleSpecAnalysisCollection:
		case SpecType.SampleSpecAnalysis:
		case SpecType.StructureSpecAnalysisCollection:
		case SpecType.StructureSpecAnalysis:
			System.out.println("Analysis not implemented");
			getStructureSpecAnalysisCollection();
			// TODO
			return null;
		case ObjectType.Sample:
			if (currentSample == null) {
				currentSample = getSampleCollection().getSampleFor(rootPath, localName, param, id,
						currentObject, mediaTypeFromName(localName));
				System.out.println("creating Sample " + currentSample.getName());
				currentSample.setUrlIndex(currentSourceIndex);
			} else {
				currentSample.setPropertyValue(param, id);
			}
			return currentSample;
		case ObjectType.Structure:
			if (currentStructure == null) {
				currentStructure = getStructureCollection().getStructureFor(rootPath, localName, param, id,
						currentObject, mediaTypeFromName(localName));
				System.out.println("creating Structure " + currentStructure.getName());
				currentStructure.setUrlIndex(currentSourceIndex);
			} else {
				currentStructure.setPropertyValue(param, id);
			}
			return currentStructure;
		case SpecType.SampleSpecCollection:
			getSampleSpecCollection();
			break;
		case SpecType.StructureSpecCollection:
			// valid data information? maybe
			getStructureSpecCollection();
			break;
		case SpecType.SpecData:
		case SpecType.SpecDataCollection:
		case ObjectType.StructureCollection:
			// should not be generic
		default:
			System.err.println(
					"IFDSpeDataFindingAid could not add " + param + " " + id + " for " + currentObject);
			break;
		}
		return null;
	}

	public IFDSpecDataCollection getSpecDataCollection() {
		if (specDataCollection == null) {
			try {
				setSafely(SPECDATA_COLLECTION, specDataCollection = new IFDSpecDataCollection(name));
			} catch (IFDException e) {
				// unattainable
			}
		}
		return specDataCollection;
	}

	public IFDStructureCollection getStructureCollection() {
		if (structureCollection == null) {
			try {
				setSafely(STRUCTURE_COLLECTION, structureCollection = new IFDStructureCollection(name));
			} catch (IFDException e) {
				// type is defined
			}
		}
		return structureCollection;
	}

	public IFDSampleCollection getSampleCollection() {
		if (sampleCollection == null) {
			try {
				setSafely(SAMPLE_COLLECTION, sampleCollection = new IFDSampleCollection(name));
			} catch (IFDException e) {
				// type is defined
			}
		}
		return sampleCollection;
	}

	public IFDStructureSpecCollection getStructureSpecCollection() {
		if (structureSpecCollection == null) {
			try {
				setSafely(STRUCTURE_SPEC_COLLECTION, structureSpecCollection = new IFDStructureSpecCollection(name));
			} catch (IFDException e) {
				// not attainable
			}
		}
		return structureSpecCollection;
	}

	public IFDSampleSpecCollection getSampleSpecCollection() {
		if (sampleSpecCollection == null) {
			try {
				setSafely(SAMPLE_SPEC_COLLECTION, sampleSpecCollection = new IFDSampleSpecCollection(name));
			} catch (IFDException e) {
				// not attainable
			}
		}
		return sampleSpecCollection;
	}

	public IFDStructureSpecAnalysisCollection getStructureSpecAnalysisCollection() {
		if (structureSpecAnalysisCollection == null)
			try {
				setSafely(STRUCTURE_SPEC_ANALYSIS_COLLECTION,
						structureSpecAnalysisCollection = new IFDStructureSpecAnalysisCollection(name));
			} catch (IFDException e) {
				// not attainable
			}
		return structureSpecAnalysisCollection;
	}

	public IFDSampleSpecAnalysisCollection getSampleSpecAnalysisCollection() {
		if (sampleSpecAnalysisCollection == null)
			try {
				setSafely(SAMPLE_SPEC_ANALYSIS_COLLECTION,
						sampleSpecAnalysisCollection = new IFDSampleSpecAnalysisCollection(name));
			} catch (IFDException e) {
				// not attainable
			}
		return sampleSpecAnalysisCollection;
	}

	public IFDObject<?> endAddingObjects() {
		if (!isAddingObjects())
			return null;
		try {
			if (currentStructure != null && currentSpecData != null)
				return getStructureSpecCollection().addSpec(currentObject, currentStructure, currentSpecData);
			return (currentStructure != null ? currentStructure : currentSpecData);
		} finally {
			currentObject = null;
			currentStructure = null;
			currentSpecData = null;
		}
	}

	public void finalizeExtraction() {
		finalizeCollections(null);
		dumpSummary();
	}

	private void dumpSummary() {
		if (getStructureCollection().size() == 0 && getSpecDataCollection().size() == 0)
			System.out.println("IFDSpecDataFindingAid no structures or spectra?");
		System.out.println("! IFDFindingAid extraction complete:\n! " + sources + "\n! " + getStructureCollection().size()
				+ " structures " + getSpecDataCollection().size() + " specdata " + getStructureSpecCollection().size()
				+ " structure-spec bindings");
		for (IFDStructureDataAssociation ssc : getStructureSpecCollection()) {
			System.out.println("Structure " + ssc.getFirstStructure().toString());
			for (IFDDataObject<?> sd : ssc.getDataObjectCollection()) {
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
				: fname.endsWith(".txt") || fname.endsWith(".log") || fname.endsWith(".out") ? "text/plain"
				: fname.endsWith(".inchi") ? "chemical/x-inchi"
				: fname.endsWith(".smiles") || fname.endsWith(".smi") ? "chemical/x-daylight-smiles"
				: fname.endsWith(".pdf") ? "application/pdf" : fname.endsWith(".jpf") ? "application/octet-stream (JEOL)"
				: fname.endsWith(".mnova") ? "application/octet-stream (mnova)"
				: pt >= 0 ? "?" + fname.substring(pt) : "?");
	}

	public IFDRepresentation getSpecDataRepresentation(String zipName) {
		return (specDataCollection == null ? null : specDataCollection.getRepresentation(zipName));
	}

	public int cleanStructureSpecs() {
		List<IFDStructureDataAssociation> lstRemove = new ArrayList<>();
		IFDStructureSpecCollection ssc = getStructureSpecCollection();
		int n = 0;
		for (IFDStructureDataAssociation ss : ssc) {
			List<IFDSpecData> empty = new ArrayList<>();
			IFDDataObjectCollection<IFDDataObject<?>> dc = ss.getDataObjectCollection();
			for (IFDDataObject<?> d : dc) {
				if (d.size() == 0)
					empty.add((IFDSpecData) d);
			}
			n += empty.size();
			dc.removeAll(empty);
			if (dc.size() == 0) {
				lstRemove.add(ss);
				IFDStructure st = ss.getFirstStructure();
				System.out.println("removing structure " + st.getName());
				getStructureCollection().remove(st);
				n++;
			}
		}
		n += lstRemove.size();
		ssc.removeAll(lstRemove);
		return n;
	}

	@Override
	protected void serializeList(IFDSerializerI serializer) {
		finalizeCollections(serializer);
	}

	/**
	 * 
	 * @param serializer
	 */
	private void finalizeCollections(IFDSerializerI serializer) {
		finalizeCollection(serializer, "structures", structureCollection);
		finalizeCollection(serializer, "specData", specDataCollection);
		finalizeCollection(serializer, "structureSpecData", structureSpecCollection);
		finalizeCollection(serializer, "structureSpecAnalyses", structureSpecAnalysisCollection);
		finalizeCollection(serializer, "samples", sampleCollection);
		finalizeCollection(serializer, "sampleSpecData", sampleSpecCollection);
		finalizeCollection(serializer, "sampleSpecAnalyses", sampleSpecAnalysisCollection);
	}

	/**
	 * Reset all indices and optionally serialize this collection.
	 * 
	 * @param serializer
	 * @param name
	 * @param c
	 */
	private void finalizeCollection(IFDSerializerI serializer, String name, IFDCollection<?> c) {
		if (c == null || c.size() == 0)
			return;
		if (serializer == null) {
			// normalize indices
			for (int i = c.size(); --i >= 0;)
				((IFDObject<?>) c.get(i)).setIndex(i);
		} else {
			serializer.addAttrInt(name + "Count", c.size());
			serializer.addObject(name, c);
		}
	}

	public IFDSpecData cloneSpec(IFDSpecData spec, String idExtension) {
		IFDSpecData newSpec = (IFDSpecData) spec.clone();
		newSpec.setID(spec.getID() + idExtension);
		getSpecDataCollection().remove(spec);
		getSpecDataCollection().add(newSpec);
		return newSpec;
	}

	/**
	 * This method will return the FIRST structure associated with a spectrum
	 * and remove it if found
	 * @param spec
	 * @param andRemove
	 * @return
	 */
	public IFDStructure firstStructureForSpec(IFDSpecData spec, boolean andRemove) {
		return getStructureSpecCollection().findStructureForSpec(spec, andRemove);
	}

	public void associate(String name, IFDStructure struc, IFDSpecData spec) {
		getStructureSpecCollection().addAssociation(name, struc, spec);
	}

	public IFDStructure addStructureForSpec(String rootPath, IFDSpecData spec, String ifdRepType, String ifdPath, String localName, String name)
			throws IFDException {
		if (getSpecDataCollection().indexOf(spec) < 0)
			getSpecDataCollection().addSpecData(spec);			
		IFDStructure struc = getStructureCollection().getStructureFor(rootPath, localName, IFDStructure.IFD_PROP_STRUC_COMPOUND_LABEL, name, ifdPath, null);
		struc.addRepresentation(ifdPath, localName, ifdRepType, mediaTypeFromName(localName));
		getStructureCollection().addStructure(struc);
		IFDStructureSpec ss = getStructureSpecCollection().getAssociationForSingleSpec(spec);
		if (ss == null) {
			ss = getStructureSpecCollection().addSpec(name, struc, spec);
		} else {
			ss.getStructureCollection().addStructure(struc);
		}
		return struc;
	}

	public IFDStructureDataAssociation getAssociation(IFDStructure struc, IFDSpecData spec) {
		return getStructureSpecCollection().findAssociation(struc, spec);
	}

}