package org.iupac.fairdata.contrib.fairspec;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.iupac.fairdata.api.IFDSerializerI;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDObject;
import org.iupac.fairdata.core.IFDRepresentation;
import org.iupac.fairdata.core.IFDResource;
import org.iupac.fairdata.dataobject.IFDDataObject;
import org.iupac.fairdata.dataobject.IFDDataObjectCollection;
import org.iupac.fairdata.derived.IFDSampleDataAssociation;
import org.iupac.fairdata.derived.IFDSampleStructureAssociation;
import org.iupac.fairdata.derived.IFDStructureDataAssociation;
import org.iupac.fairdata.derived.IFDStructureDataAssociationCollection;
import org.iupac.fairdata.sample.IFDSample;
import org.iupac.fairdata.structure.IFDStructure;

/**
 * An interface for extractors
 * 
 * @author hansonr
 *
 */
public interface FAIRSpecExtractorHelperI {

	IFDObject<?> addObject(String rootPath, String param, String id, String localizedName, long len)
			throws IFDException;

	IFDResource addOrSetSource(String resource);

 	IFDStructure addStructureForSpec(String rootPath, IFDDataObject spec, String ifdRepType, String ifdPath,
			String localName, String name) throws IFDException;

	IFDSampleDataAssociation associateSampleSpec(IFDSample sample, IFDDataObject newSpec)
			throws IFDException;

	IFDSampleStructureAssociation associateSampleStructure(IFDSample sample, IFDStructure struc)
			throws IFDException;

	IFDStructureDataAssociation associateStructureSpec(IFDStructure struc, IFDDataObject newSpec)
			throws IFDException;

	void beginAddingObjects(String ifdPath);

	String createSerialization(File targetFile, String findingAidFileNameRoot, List<Object> products,
			IFDSerializerI serializer, long[] t) throws IOException;

	IFDObject<?> endAddingObjects();

	String finalizeExtraction();

	IFDDataObjectCollection getDataObjectCollection();

	FAIRSpecFindingAid getFindingAid();

	IFDSample getFirstSampleForSpec(IFDDataObject localSpec, boolean b);

	IFDStructure getFirstStructureForSpec(IFDDataObject localSpec, boolean b);

	IFDSample getSampleByName(String value);

	IFDRepresentation getSpecDataRepresentation(String ifdPath);

	IFDStructureDataAssociation getStructureAssociation(IFDStructure struc, IFDDataObject spec);

	IFDStructureDataAssociationCollection getStructureDataCollection();

	int removeStructuresWithNoAssociations();

	void setCurrentResourceByteLength(long len);

	void setAssociationsById(boolean equalsIgnoreCase);

	IFDDataObject cloneData(IFDDataObject localSpec, String idExtension);

	void removeInvalidData();

}
