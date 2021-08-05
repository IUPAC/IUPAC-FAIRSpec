package org.iupac.fairspec.common;

/**
 * Constants relating to properties and extraction.
 * 
 * @author hansonr
 *
 */
public class IFSConst {

	
	public static final String IFS_FAIRSpec_version = "0.0.1-alpha-2021_06_27";

	public enum PROPERTY_TYPE { INT, FLOAT, STRING, NUCL, OBJ };
	
	public enum PROPERTY_UNIT { NONE, HZ, MHZ, CELCIUS, KELVIN };
	

	// Finding Aid properties:

	public static final String IFS_PROP_COLLECTION_DATA_LICENSE_NAME = "IFS.property.collection.data.license.name";
	public static final String IFS_PROP_COLLECTION_DATA_LICENSE_URI  = "IFS.property.collection.data.license.uri";

	
	// IFSExtractorI constants:
	
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

	public static final String IFS_FINDINGAID                        = "IFS.findingaid"; // root name for JSON
	public static final String IFS_FINDINGAID_FLAG                   = "IFS.findingaid.";

	public static final String IFS_PROP_COLLECTION                        = "IFS.property.collection";
	public static final String IFS_PROP_COLLECTION_ID                     = "IFS.property.collection.id";
	public static final String IFS_PROP_COLLECTION_REF                    = "IFS.property.collection.ref";
	public static final String IFS_PROP_COLLECTION_LEN                    = "IFS.property.collection.len";
	public static final String IFS_PROP_COLLECTION_OBJECT                 = "IFS.property.collection.object";
	public static final String IFS_PROP_COLLECTION_SOURCE_DATA_URI        = "IFS.property.collection.source.data.uri";
	public static final String IFS_PROP_COLLECTION_SOURCE_PUBLICATION_URI = "IFS.property.collection.source.publication.uri";

	public static final String IFS_PROPERTY_FLAG       = "IFS.property.";
	public static final String IFS_REPRESENTATION_FLAG = "IFS.representation.";

	public static final String IFS_COMPOUND_LABEL_FLAG       = ".compound.label";
	public static final String IFS_EXPT_LABEL_FLAG        = ".expt.label";
	

	public static boolean isLabel(String propName) {
		return (propName != null && (propName.endsWith(IFS_EXPT_LABEL_FLAG) || propName.endsWith(IFS_COMPOUND_LABEL_FLAG)));
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
