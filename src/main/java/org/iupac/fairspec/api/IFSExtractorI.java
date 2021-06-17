package org.iupac.fairspec.api;

import java.io.File;
import java.io.IOException;

import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.spec.IFSSpecDataFindingAid;

/**
 * An interface for extractors
 * 
 * @author hansonr
 *
 */
public interface IFSExtractorI {

	void initialize(File ifsExtractScriptFile) throws IOException;

	void setLocalSourceDir(String sourceDir);

	void setCachePattern(String s);

	void setRezipCachePattern(String procs, String toExclude);

	/**
	 * Find and extract all objects of interest from a ZIP file.
	 * 
	 */
	IFSSpecDataFindingAid extractObjects(File targetDir) throws IFSException, IOException;

}
