package org.iupac.fairspec.common;

public class IFSConst {

	public final static String IFS_FAIRSpec_version = "0.0.1-2021_06_14";
	/**
	 * 	the files we want extracted -- no $ for cdx, as that could be cdxml
	 */
	public final static String defaultCachePattern = ""
			+ "(?<param>procs$|proc2s$|acqus$|acqu2s$|audita.txt$)|" // for parameters
			+ "\\.pdf$|\\.png$|"
			+ "\\.mol$|\\.sdf$|\\.cdx|"
//			+ "\\.log$|\\.out$|\\.txt$|"// maybe put these into JSON only? 
			+ "\\.jdx$|\\.dx$|\\.jdf$|\\.mnova$";

	

	// . 

	/**
	 * this is the pattern to the files we want rezipped; the "path" group is the
	 * one used and should point to the directory just above pdata; "dir" is not used 
	 */
	public final static String defaultRezipPattern = "^(?<path>.+(?:/|\\|)(?<dir>[^/]+)(?:/|\\|))pdata/[^/]+/procs$";
	
	public final static String defaultRezipIgnorePattern = "\\.mnova$";

	public enum PROPERTY_TYPE { INT, NUCL, OBJ };
	public enum UNITS { NONE, MHZ };
		
}
