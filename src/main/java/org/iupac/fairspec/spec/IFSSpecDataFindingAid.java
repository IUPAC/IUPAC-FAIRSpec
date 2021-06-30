package org.iupac.fairspec.spec;

import java.util.ArrayList;
import java.util.List;

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
		// 1) Create the org/iupac/fairspec/spec/xxx/ folder
		//
		// 2) Create the xxx/IFSXXXSpecData and xxx/IFSXXXSpecDataRepresentation classes
		//
		// 3) Modify the code to suit, adding properties and patterns and methods to run to
		//    extract metadata from the data.
		
	}

	private IFSStructureCollection structureCollection;
	private IFSSpecDataCollection specDataCollection;
	private IFSStructureSpecCollection structureSpecCollection;
	private IFSStructureSpecAnalysisCollection structureSpecAnalysisCollection;
	private IFSSampleCollection sampleCollection;
	private IFSSampleSpecCollection sampleSpecCollection;
	private IFSSampleSpecAnalysisCollection sampleSpecAnalysisCollection;

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


	public IFSSpecDataFindingAid(String id, String sUrl) throws IFSException {
		super(null, SpecType.SpecDataFindingAid, sUrl);
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

	/**
	 * This list will grow.
	 * 
	 * @param propName
	 * @return
	 * @throws IFSException
	 */
	public static String getObjectTypeForName(String propName) throws IFSException {
		if (IFSConst.isFindingAid(propName))
			return SpecType.SpecDataFindingAid;
		if (IFSConst.isProperty(propName))
			propName = PT.rep(propName, IFSConst.IFS_PROPERTY_FLAG, "\0");
		else if (IFSConst.isRepresentation(propName))
			propName = PT.rep(propName, IFSConst.IFS_REPRESENTATION_FLAG, "\0");
		else
			throw new IFSException("bad IFS identifier: " + propName);
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
	 * @param value
	 * @param localName
	 * @return
	 * @throws IFSException
	 */
	public IFSRepresentableObject<?> addObject(String rootPath, String param, String value, String localName)
			throws IFSException {
		if (currentObjectZipName == null)
			throw new IFSException("addObject " + param + " " + value + " called with no current object file name");

		String type = getObjectTypeForName(param);

		if (type.startsWith("spec.")) {
			currentSpecData = getSpecDataCollection().getDataObjectFor(rootPath, localName, param, value,
					currentObjectZipName, type, mediaTypeFromName(localName));
			currentSpecData.setUrlIndex(currentUrlIndex);
			return currentSpecData;
		}
		switch (type) {
		case SpecType.SampleSpecAnalysisCollection:
		case SpecType.SampleSpecAnalysis:
		case SpecType.StructureSpecAnalysisCollection:
		case SpecType.StructureSpecAnalysis:
			System.out.println("Analysise not implemented");
			getStructureSpecAnalysisCollection();
			// TODO
			return null;
		case ObjectType.Sample:
			if (currentSample == null) {
				currentSample = getSampleCollection().getSampleFor(rootPath, localName, param, value,
						currentObjectZipName, mediaTypeFromName(localName));
				System.out.println("creating Sample " + currentSample.getName());
				currentSample.setUrlIndex(currentUrlIndex);
			} else {
				currentSample.setPropertyValue(param, value);
			}
			return currentSample;
		case ObjectType.Structure:
			if (currentStructure == null) {
				currentStructure = getStructureCollection().getStructureFor(rootPath, localName, param, value,
						currentObjectZipName, mediaTypeFromName(localName));
				System.out.println("creating Structure " + currentStructure.getName());
				currentStructure.setUrlIndex(currentUrlIndex);
			} else {
				currentStructure.setPropertyValue(param, value);
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
		case SpecType.SpecDataFindingAid:
		default:
			System.err.println(
					"IFSSpeDataFindingAid could not add " + param + " " + value + " for " + currentObjectZipName);
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
				setSafely(STRUCTURE_SPEC_ANALYSIS_COLLECTION,
						structureSpecAnalysisCollection = new IFSStructureSpecAnalysisCollection(name));
			} catch (IFSException e) {
				// not attainable
			}
		return structureSpecAnalysisCollection;
	}

	public IFSSampleSpecAnalysisCollection getSampleSpecAnalysisCollection() {
		if (sampleSpecAnalysisCollection == null)
			try {
				setSafely(SAMPLE_SPEC_ANALYSIS_COLLECTION,
						sampleSpecAnalysisCollection = new IFSSampleSpecAnalysisCollection(name));
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
		System.out.println("! IFSFindingAid extraction complete:\n! " + urls + "\n! " + getStructureCollection().size()
				+ " structures " + getSpecDataCollection().size() + " specdata " + getStructureSpecCollection().size()
				+ " structure-spec bindings");
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
				: fname.endsWith(".txt") || fname.endsWith(".log") || fname.endsWith(".out") ? "text/plain"
				: fname.endsWith(".inchi") ? "chemical/x-inchi"
				: fname.endsWith(".smiles") || fname.endsWith(".smi") ? "chemical/x-daylight-smiles"
				: fname.endsWith(".pdf") ? "application/pdf" : fname.endsWith(".jpf") ? "application/octet-stream (JEOL)"
				: fname.endsWith(".mnova") ? "application/octet-stream (mnova)"
				: pt >= 0 ? "?" + fname.substring(pt) : "?");
	}

	public IFSRepresentation getSpecDataRepresentation(String zipName) {
		return (specDataCollection == null ? null : specDataCollection.getRepresentation(zipName));
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


}