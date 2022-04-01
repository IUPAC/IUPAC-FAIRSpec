package org.iupac.fairdata.contrib;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.iupac.fairdata.api.IFDSerializerI;
import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDAssociation;
import org.iupac.fairdata.core.IFDCollection;
import org.iupac.fairdata.core.IFDFAIRDataFindingAid;
import org.iupac.fairdata.core.IFDObject;
import org.iupac.fairdata.core.IFDRepresentableObject;
import org.iupac.fairdata.core.IFDRepresentation;
import org.iupac.fairdata.dataobject.IFDDataObject;
import org.iupac.fairdata.dataobject.IFDDataObjectCollection;
import org.iupac.fairdata.sample.IFDSample;
import org.iupac.fairdata.sample.IFDSampleCollection;
import org.iupac.fairdata.structure.IFDStructure;
import org.iupac.fairdata.structure.IFDStructureCollection;
import org.iupac.fairdata.derived.IFDSampleDataAnalysisCollection;
import org.iupac.fairdata.derived.IFDSampleDataAssociationCollection;
import org.iupac.fairdata.derived.IFDStructureDataAnalysisCollection;
import org.iupac.fairdata.derived.IFDStructureDataAssociation;
import org.iupac.fairdata.derived.IFDStructureDataAssociationCollection;

import javajs.util.PT;

/**
 * 
 * This class is tailored to the task of creating an IUPAC FAIRData Collection
 * and its associated IUPAC FAIRData Finding Aid.
 * 
 * It is currently under development and should NOT be considered to be a
 * an IUPAC standard.
 * 
 * 
 * 
 * 
 * @author hansonr
 *
 */
public class IFDFAIRSpecFindingAidHelper {

	private final IFDFAIRDataFindingAid findingAid;
	
	@SuppressWarnings("rawtypes")
	private IFDCollection[] objects = new IFDCollection[3];
	@SuppressWarnings("rawtypes")
	private IFDCollection[] associations = new IFDCollection[4];

	public final static int SAMPLE_COLLECTION    = 0;
	public final static int STRUCTURE_COLLECTION = 1;
	public final static int DATA_COLLECTION      = 2;

	public final static int SAMPLE_DATA_COLLECTION             = 0;
	public final static int STRUCTURE_DATA_COLLECTION          = 1;
	public final static int SAMPLE_DATA_ANALYSIS_COLLECTION    = 2;
	public final static int STRUCTURE_DATA_ANALYSIS_COLLECTION = 3;

	private IFDStructureCollection structureCollection;
	private IFDDataObjectCollection specDataCollection;
	private IFDStructureDataAssociationCollection structureDataCollection;
	private IFDStructureDataAnalysisCollection structureDataAnalysisCollection;
	private IFDSampleCollection sampleCollection;
	private IFDSampleDataAssociationCollection sampleDataCollection;
	private IFDSampleDataAnalysisCollection sampleDataAnalysisCollection;


	private String currentObject;
	private IFDStructure currentStructure;
	private IFDDataObject currentSpecData;
	private IFDSample currentSample;

	public IFDFAIRSpecFindingAidHelper(String creator) {
		findingAid = new IFDFAIRSpecFindingAid(null, IFDConst.getProp("IFD_FAIRSPEC_FINDING_AID"), creator);
	}

	public IFDFAIRDataFindingAid getFindingAid() {
		return findingAid;
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

	public IFDDataObject getCurrentSpecData() {
		return currentSpecData;
	}

	public void setCurrentSpecData(IFDDataObject spec) {
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
			return "Unknown";
		else
			throw new IFDException("bad IFD identifier: " + propName);
		if (propName.startsWith("\0struc."))
			return IFDConst.ClassTypes.Structure;
		if (propName.startsWith("\0analysis.structure.spec."))
			return IFDConst.ClassTypes.StructureDataAnalysis;
		if (propName.startsWith("\0analysis.sample.spec."))
			return IFDConst.ClassTypes.SampleDataAnalysis;
		if (propName.startsWith("\0spec."))
			return propName.substring(1, propName.indexOf(".", 6));
		return "Unknown";
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

		if (type.startsWith(IFDConst.DATA_FLAG)) {
			currentSpecData = getDataObjectCollection().getDataObjectFor(currentObject, rootPath, localName, param,
					id, type, mediaTypeFromName(localName));
			currentSpecData.setUrlIndex(findingAid.getCurrentSourceIndex());
			return currentSpecData;
		}
		switch (type) {
		case IFDConst.ClassTypes.SampleDataAnalysisCollection:
		case IFDConst.ClassTypes.SampleDataAnalysis:
		case IFDConst.ClassTypes.StructureDataAnalysisCollection:
		case IFDConst.ClassTypes.StructureDataAnalysis:
			System.out.println("Analysis not implemented");
			getStructureDataAnalysisCollection();
			// TODO
			return null;
		case IFDConst.ClassTypes.Sample:
			if (currentSample == null) {
				currentSample = getSampleCollection().getSampleFor(rootPath, localName, param, id,
						currentObject, mediaTypeFromName(localName));
				System.out.println("creating Sample " + currentSample.getName());
				currentSample.setUrlIndex(findingAid.getCurrentSourceIndex());
			} else {
				currentSample.setPropertyValue(param, id);
			}
			return currentSample;
		case IFDConst.ClassTypes.Structure:
			if (currentStructure == null) {
				currentStructure = getStructureCollection().getStructureFor(rootPath, localName, param, id,
						currentObject, mediaTypeFromName(localName));
				System.out.println("creating Structure " + currentStructure.getName());
				currentStructure.setUrlIndex(findingAid.getCurrentSourceIndex());
			} else {
				currentStructure.setPropertyValue(param, id);
			}
			return currentStructure;
		case IFDConst.ClassTypes.SampleDataAssociationCollection:
			getSampleDataCollection();
			break;
		case IFDConst.ClassTypes.StructureDataAssociationCollection:
			// valid data information? maybe
			getStructureDataCollection();
			break;
		case IFDConst.ClassTypes.DataObject:
		case IFDConst.ClassTypes.DataObjectCollection:
		case IFDConst.ClassTypes.StructureCollection:
			// should not be generic
		default:
			System.err.println(
					"IFDSpeDataFindingAid could not add " + param + " " + id + " for " + currentObject);
			break;
		}
		return null;
	}

	
	public IFDObject<?> endAddingObjects() {
		if (!isAddingObjects())
			return null;
		try {
			if (currentStructure != null && currentSpecData != null)
				return getStructureDataCollection().addAssociation(currentObject, currentStructure, currentSpecData);
			return (currentStructure != null ? currentStructure : currentSpecData);
		} catch (IFDException e) {
			// not possible
			return null;
		} finally {
			currentObject = null;
			currentStructure = null;
			currentSpecData = null;
		}
	}

	public void finalizeExtraction() {
		for (int i = 0; i < objects.length; i++)
			if (objects[i] != null)
				findingAid.addCollection(objects[i]);
		for (int i = 0; i < associations.length; i++)
			if (associations[i] != null)
				findingAid.addCollection(associations[i]);
		findingAid.finalizeCollections(null);
		dumpSummary();
	}

	private void dumpSummary() {
		if (getStructureCollection().size() == 0 && getDataObjectCollection().size() == 0)
			System.out.println("IFDSpecDataFindingAid no structures or spectra?");
		System.out.println("! IFDFindingAid extraction complete:\n! " + getFindingAid().getDataSources() + "\n! " + getStructureCollection().size()
				+ " structures " + getDataObjectCollection().size() + " specdata " + getStructureDataCollection().size()
				+ " structure-spec bindings");
		for (IFDAssociation ssc : getStructureDataCollection()) {
			System.out.println("Structure " + ssc.getFirstObj1().toString());
			for (IFDObject<?> sd : ssc.get(1)) {
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

	/**
	 * Remove structures for which there are no data associations.
	 * This, of course, is a completely optional step. 
	 * 
	 * @return
	 */
	public int removeStructuresWithNoAssociations() {
		List<IFDAssociation> lstRemove = new ArrayList<>();
		IFDStructureDataAssociationCollection specData = getStructureDataCollection();
		int n = 0;
		for (IFDAssociation assoc : specData) {
			List<IFDDataObject> empty = new ArrayList<>();
			IFDCollection<IFDObject<?>> dataCollection = assoc.getObject(1);
			for (IFDObject<?> d : dataCollection) {
				if (d.size() == 0)
					empty.add((IFDDataObject) d);
			}
			n += empty.size();
			dataCollection.removeAll(empty);
			if (dataCollection.size() == 0) {
				lstRemove.add(assoc);
				IFDStructure st = (IFDStructure) assoc.getFirstObj1();
				System.out.println("removing structure " + st.getName());
				getStructureCollection().remove(st);
				n++;
			}
		}
		n += lstRemove.size();
		specData.removeAll(lstRemove);
		return n;
	}

	/**
	 * This method will return the FIRST structure associated with a spectrum
	 * and optionally remove it if found
	 * @param spec
	 * @param andRemove
	 * @return
	 */
	public IFDStructure getFirstStructureForSpec(IFDRepresentableObject<?> spec, boolean andRemove) {
		return (IFDStructure) getStructureDataCollection().getFirstObj1ForObj2(spec, andRemove);
	}

	public void associate(String name, IFDStructure struc, IFDDataObject spec) throws IFDException {
		getStructureDataCollection().addAssociation(name, struc, spec);
	}

	public IFDStructure addStructureForSpec(String rootPath, IFDDataObject spec, String ifdRepType, String ifdPath, String localName, String name)
			throws IFDException {
		if (getDataObjectCollection().indexOf(spec) < 0)
			getDataObjectCollection().add(spec);			
		IFDStructure struc = getStructureCollection().getStructureFor(rootPath, localName, IFDConst.IFD_PROP_SAMPLE_LABEL, name, ifdPath, null);
		struc.addRepresentation(ifdPath, localName, ifdRepType, mediaTypeFromName(localName));
		getStructureCollection().add(struc);
		IFDStructureDataAssociation ss = (IFDStructureDataAssociation) getStructureDataCollection().getAssociationForSingleObj2(spec);
		if (ss == null) {
			ss = getStructureDataCollection().addAssociation(name, struc, spec);
		} else {
			ss.getStructureCollection().add(struc);
		}
		return struc;
	}

	public IFDStructureDataAssociation getAssociation(IFDStructure struc, IFDDataObject spec) {
		return (IFDStructureDataAssociation) getStructureDataCollection().findAssociation(struc, spec);
	}
	
	public IFDRepresentation getSpecDataRepresentation(String zipName) {
		return (specDataCollection == null ? null : specDataCollection.getRepresentation(zipName));
	}

	public IFDSampleCollection getSampleCollection() {
		if (sampleCollection == null) {
			objects[SAMPLE_COLLECTION] = sampleCollection = new IFDSampleCollection("samples");
		}
		return sampleCollection;
	}

	public IFDStructureCollection getStructureCollection() {
		if (structureCollection == null) {
			objects[STRUCTURE_COLLECTION] = structureCollection = new IFDStructureCollection("structures");
		}
		return structureCollection;
	}

	public IFDDataObjectCollection getDataObjectCollection() {
		if (specDataCollection == null) {
			objects[DATA_COLLECTION] = specDataCollection = new IFDDataObjectCollection("dataObjects");
		}
		return specDataCollection;
	}
	
	public IFDSampleDataAssociationCollection getSampleDataCollection() {
		if (sampleDataCollection == null) {
			associations[SAMPLE_DATA_COLLECTION] = sampleDataCollection = new IFDSampleDataAssociationCollection("sampleDataAssociations");
		}
		return sampleDataCollection;
	}

	public IFDStructureDataAssociationCollection getStructureDataCollection() {
		if (structureDataCollection == null) {
			associations[STRUCTURE_DATA_COLLECTION] =
					structureDataCollection = new IFDStructureDataAssociationCollection("structureDataAssociations");
		}
		return structureDataCollection;
	}

	public IFDSampleDataAnalysisCollection getSampleDataAnalysisCollection() {
		if (sampleDataAnalysisCollection == null)
			associations[SAMPLE_DATA_ANALYSIS_COLLECTION] =
					sampleDataAnalysisCollection = new IFDSampleDataAnalysisCollection("sampleDataAnalyses");
		return sampleDataAnalysisCollection;
	}

	public IFDStructureDataAnalysisCollection getStructureDataAnalysisCollection() {
		if (structureDataAnalysisCollection == null)
			associations[STRUCTURE_DATA_ANALYSIS_COLLECTION] =
					structureDataAnalysisCollection = new IFDStructureDataAnalysisCollection("structureDataAnalyses");
		return structureDataAnalysisCollection;
	}

	public String createSerialization(File targetFile, String findingAidFileNameRoot, List<Object> products,
			IFDSerializerI serializer) throws IOException {
		return findingAid.createSerialization(targetFile, findingAidFileNameRoot, products, serializer);
	}

}