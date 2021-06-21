package org.iupac.fairspec.api;

import java.io.File;
import java.io.IOException;

import org.iupac.fairspec.assoc.IFSFindingAid;
import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.spec.IFSSpecDataFindingAid;

/**
 * An interface for extractors
 * 
 * @author hansonr
 *
 */
public interface IFSExtractorI {

	void readIFSExtractJSON(File ifsExtractScriptFile) throws IOException;

	void setLocalSourceDir(String sourceDir);

	void setCachePattern(String s);

	void setRezipCachePattern(String procs, String toExclude);
	
	IFSFindingAid getFindingAid();

	/**
	 * Find and extract all objects of interest from a ZIP file.
	 * 
	 */
	IFSSpecDataFindingAid extractObjects(File targetDir) throws IFSException, IOException;

	void addProperty(String param, Object val);

	boolean extractAndCreateFindingAid(File ifsExtractScriptFile, String localSourceDir, File targetDir, String prefix)
			throws IOException, IFSException;

}
