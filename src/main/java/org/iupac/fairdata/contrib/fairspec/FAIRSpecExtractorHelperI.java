package org.iupac.fairdata.contrib.fairspec;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.contrib.fairspec.FAIRSpecExtractorHelper.FileList;
import org.iupac.fairdata.core.IFDObject;
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

	void beginAddingObjects(String ifdPath);

	public IFDStructure addStructureForSpec(String rootPath, IFDDataObject spec, String ifdRepType, String ifdPath,
			String localName, String name) throws IFDException;

	IFDStructure addStructureForCompound(String rootPath, FAIRSpecCompoundAssociation assoc, String ifdRepType,
			String oPath, String localName, String name) throws IFDException;

	IFDSample addSpecOriginatingSampleRef(String rootPath, IFDDataObject spec, String originatingSampleID) throws IFDException;

	IFDObject<?> endAddingObjects();

	String getFileListJSON(String name, List<FileList> rootLists, String resourceList, String scriptFileName, int[] ret) throws IOException;

	void removeInvalidData();

	int removeStructuresWithNoAssociations();

	/**
	 * Finalize the extraction
	 * @param htURLReferences an optional JSON-derived map that ties a compound id or a compound id + "|" + file short name with a doi or url 
	 * @return
	 */
	String finalizeExtraction(Map<String, Map<String, Object>> htURLReferences);

	String dumpState();

}
