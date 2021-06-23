package org.iupac.fairspec.common;

public class IFSConst {
	
	public static final String IFS_FAIRSpec_version = "0.0.1-2021_06_21";

	public enum PROPERTY_TYPE { INT, FLOAT, STRING, NUCL, OBJ };
	
	public enum PROPERTY_UNITS { NONE, MHZ };
	

	/**
	 * regex for files that are absolutely worthless
	 */
	public static final String junkFilePattern = "(MACOSX)|(desktop\\.ini)|(\\.DS_Store)";

	/**
	 * 	the files we want extracted -- just PDF and PNG here; all others are taken care of by
	 *  individual IFSVendorPluginI classes
	 */
	public static final String defaultCachePattern = ""
			+ "(?<img>\\.pdf$|\\.png$)"
//			+ "|(?<text>\\.log$|\\.out$|\\.txt$)"// maybe put these into JSON only? 
			;


	public static final String IFS_FINDINGAID_DATA_LICENSE_NAME = "IFS.findingaid.data.license.name";
	public static final String IFS_FINDINGAID_DATA_LICENSE_URI  = "IFS.findingaid.data.license.uri";


	// generic standard names:

	public static final String IFS_FINDINGAID                 = "IFS.findingaid";
	public static final String IFS_FINDINGAID_OBJECT          = "IFS.findingaid.object";
	public static final String IFS_FINDINGAID_SOURCE_DATA_URI = "IFS.findingaid.source.data.uri";

	public static final String IFS_EXPT_ID_FLAG = ".expt.id";
	public static final String IFS_REPRESENTATION_FLAG = "IFS.representation.";
	public static final String IFS_PROPERTY_FLAG = "IFS.property.";
	public static final String IFS_FINDINGAID_FLAG = "IFS.findingaid.";


	

	public static boolean isExptID(String propName) {
		return (propName != null && propName.endsWith(IFS_EXPT_ID_FLAG));
	}

	public static boolean isRepresentation(String propName) {
		return (propName != null && propName.startsWith(IFS_REPRESENTATION_FLAG));
	}

	public static boolean isProperty(String propName) {
		return (propName != null && propName.startsWith(IFS_PROPERTY_FLAG));
	}

	public static boolean isFindingAid(String propName) {
		return (propName != null && propName.startsWith(IFS_FINDINGAID_FLAG));
	}
	

}
