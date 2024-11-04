package org.iupac.fairdata.contrib.fairspec;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.iupac.fairdata.core.IFDReference;
import org.iupac.fairdata.dataobject.IFDDataObject;
import org.iupac.fairdata.dataobject.IFDDataObjectRepresentation;
import org.iupac.fairdata.structure.IFDStructure;
import org.iupac.fairdata.structure.IFDStructureRepresentation;

public interface FAIRSpecFindingAidHelperI {
 
	public IFDStructure createStructure(String id);

	public IFDStructureRepresentation createStructureRepresentation(IFDReference ref, Object data, long len, String ifdStructureType, String mediatype);

	public IFDDataObject createDataObject(String id, String type);

	public IFDDataObjectRepresentation createDataObjectRepresentation(IFDReference ref, Object data, long len, String ifdStructureType, String mediatype);

	FAIRSpecFindingAid getFindingAid();

	String addRelatedInfo(String pubdoi, boolean addPublicationMetadata, List<Map<String, Object>> list, String type) throws IOException;

	public void setById(boolean isByID);
	
}
