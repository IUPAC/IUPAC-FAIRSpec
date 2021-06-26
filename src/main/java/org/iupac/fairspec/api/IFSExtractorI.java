package org.iupac.fairspec.api;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.core.IFSFindingAid;
import org.iupac.fairspec.spec.IFSSpecDataFindingAid;

import com.integratedgraphics.ifs.Extractor.ObjectParser;

/**
 * An interface for extractors
 * 
 * @author hansonr
 *
 */
public interface IFSExtractorI {

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

	/**
	 * 
	 * @param ifsExtractScriptFile
	 * @param localSourceDir
	 * @param targetDir
	 * @param prefix
	 * @return true if successful
	 * @throws IOException
	 * @throws IFSException
	 */
	boolean extractAndCreateFindingAid(File ifsExtractScriptFile, String localSourceDir, File targetDir, String prefix)
			throws IOException, IFSException;

	/**
	 * Get all {object} data from IFS-extract.json.
	 * 
	 * @param ifsExtractScript
	 * @return list of {objects}
	 * @throws IOException
	 * @throws IFSException 
	 */
	List<ObjectParser> getObjectParsersForFile(File ifsExtractScript) throws IOException, IFSException;

}
