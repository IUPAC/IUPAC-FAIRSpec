package org.iupac.fairdata.contrib.fairspec;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.iupac.fairdata.api.IFDSerializerI;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDReference;
import org.iupac.fairdata.core.IFDRepresentation;
import org.iupac.fairdata.dataobject.IFDDataObject;
import org.iupac.fairdata.dataobject.IFDDataObjectCollection;
import org.iupac.fairdata.dataobject.IFDDataObjectRepresentation;
import org.iupac.fairdata.derived.IFDSampleDataAssociation;
import org.iupac.fairdata.derived.IFDSampleStructureAssociation;
import org.iupac.fairdata.sample.IFDSample;
import org.iupac.fairdata.structure.IFDStructure;
import org.iupac.fairdata.structure.IFDStructureRepresentation;

public interface FAIRSpecFindingAidHelperI {

	public IFDSampleDataAssociation associateSampleSpec(IFDSample sample, IFDDataObject newSpec) throws IFDException;

	public IFDSampleStructureAssociation associateSampleStructure(IFDSample sample, IFDStructure struc)
			throws IFDException;

	public FAIRSpecCompoundAssociation createCompound(IFDStructure struc, IFDDataObject newSpec) throws IFDException;

	public FAIRSpecCompoundAssociation createCompound(String thisCompoundID) throws IFDException;

	public IFDDataObject createDataObject(String id, String type);

	public IFDDataObjectRepresentation createDataObjectRepresentation(IFDReference ref, Object data, long len,
			String ifdStructureType, String mediatype);

	public String createSerialization(File targetDir, ArrayList<Object> products, IFDSerializerI serializer,
			long[] t) throws IOException;

	public IFDStructure createStructure(String id);

	public IFDStructureRepresentation createStructureRepresentation(IFDReference ref, Object data, long len,
			String ifdStructureType, String mediatype);

	public String generateFindingAid(File topDir) throws IOException;

	public FAIRSpecCompoundCollection getCompoundCollection();

	public IFDDataObjectCollection getSpecCollection();

	public IFDRepresentation getSpecDataRepresentation(String ifdPath);

	public void setById(boolean isByID);

	String addRelatedInfo(String pubdoi, boolean addPublicationMetadata, List<Map<String, Object>> list, String type)
			throws IOException;

	FAIRSpecCompoundAssociation findCompound(IFDStructure struc, IFDDataObject spec);

	FAIRSpecFindingAid getFindingAid();

	IFDSample getFirstSampleForSpec(IFDDataObject localSpec, boolean b);

	IFDStructure getFirstStructureForSpec(IFDDataObject localSpec, boolean andRemove);

	IFDSample getSampleByName(String value);
	
}
