package org.iupac.fairdata.contrib.fairspec;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecExtractorHelper.FileList;
import org.iupac.fairdata.core.IFDObject;
import org.iupac.fairdata.core.IFDResource;
import org.iupac.fairdata.dataobject.IFDDataObject;
import org.iupac.fairdata.sample.IFDSample;
import org.iupac.fairdata.structure.IFDStructure;

/**
 * An interface for extractors
 * 
 * @author hansonr
 *
 */
public interface FAIRSpecExtractorHelperI extends FAIRSpecFindingAidHelperI {

	IFDObject<?> addObject(String rootPath, String param, String id, String localizedName, long len)
			throws IFDException;

	IFDResource addOrSetSource(String source, String rootPath);

	IFDResource getCurrentSource();

	void beginAddingObjects(String ifdPath);

	IFDStructure addStructureForCompound(String rootPath, FAIRSpecCompoundAssociation assoc, String ifdRepType,
			String oPath, String localName, String name) throws IFDException;

	IFDStructure addStructureForSpec(String rootPath, IFDDataObject spec, String ifdRepType, String ifdPath,
			String localName, String name) throws IFDException;

	IFDSample addSpecOriginatingSampleRef(String rootPath, IFDDataObject spec, String originatingSampleID) throws IFDException;

	IFDDataObject cloneData(IFDDataObject localSpec, String idExtension, boolean andReplace);

	IFDObject<?> endAddingObjects();

	String finalizeExtraction();

	IFDStructure getCurrentStructure();

	String getFileListJSON(String name, List<FileList> rootLists, String resourceList, String scriptFileName, int[] ret) throws IOException;

	void removeInvalidData();

	int removeStructuresWithNoAssociations();

	void setCurrentResourceByteLength(long len);

	void setCompoundRefMap(Map<String, Map<String, Object>> htCompoundFileReferences);

	/**
	 * Set a property value only if it is not already set.
	 * 
	 * @param obj
	 * @param key
	 * @param value      if null, allows removal of current value
	 * @param originPath
	 * @return current value if not equal to value and value != NULL
	 */
	Object setPropertyValueNotAlreadySet(IFDObject<?> obj, String key, Object value, String originPath);

}
