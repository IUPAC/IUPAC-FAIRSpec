package org.iupac.fairdata.common;

/**
 * Constants relating to properties and extraction.
 * 
 * @author hansonr
 *
 */
public class IFDConst {

	
	public static final String IFD_FAIRSpec_version = "0.0.1-alpha-2021_06_27";

	public enum PROPERTY_TYPE { INT, FLOAT, STRING, NUCL, OBJ };
	
	public enum PROPERTY_UNIT { NONE, HZ, MHZ, CELCIUS, KELVIN };
	

	// Finding Aid properties:

	public static final String IFD_PROP_COLLECTION_DATA_LICENSE_NAME = "IFD.property.collection.data.license.name";
	public static final String IFD_PROP_COLLECTION_DATA_LICENSE_URI  = "IFD.property.collection.data.license.uri";

	
	// IFDExtractorI constants:
	
	/**
	 * regex for files that are absolutely worthless
	 */
	public static final String junkFilePattern = "(MACOSX)|(desktop\\.ini)|(\\.DS_Store)";

	/**
	 * 	the files we want extracted -- just PDF and PNG here; all others are taken care of by
	 *  individual IFDVendorPluginI classes
	 */
	public static final String defaultCachePattern = ""
			+ "(?<img>\\.pdf$|\\.png$)"
//			+ "|(?<text>\\.log$|\\.out$|\\.txt$)"// maybe put these into JSON only? 
			;

	public static final String IFD_FINDINGAID                        = "IFD.findingaid"; // root name for JSON
	public static final String IFD_FINDINGAID_FLAG                   = "IFD.findingaid.";

	public static final String IFD_PROP_COLLECTION                        = "IFD.property.collection";
	public static final String IFD_PROP_COLLECTION_ID                     = "IFD.property.collection.id";
	public static final String IFD_PROP_COLLECTION_REF                    = "IFD.property.collection.ref";
	public static final String IFD_PROP_COLLECTION_LEN                    = "IFD.property.collection.len";
	public static final String IFD_PROP_COLLECTION_OBJECT                 = "IFD.property.collection.object";
	public static final String IFD_PROP_COLLECTION_SOURCE_DATA_URI        = "IFD.property.collection.source.data.uri";
	public static final String IFD_PROP_COLLECTION_SOURCE_PUBLICATION_URI = "IFD.property.collection.source.publication.uri";

	public static final String IFD_PROPERTY_FLAG       = "IFD.property.";
	public static final String IFD_REPRESENTATION_FLAG = "IFD.representation.";

	public static final String IFD_COMPOUND_LABEL_FLAG       = ".compound.label";
	public static final String IFD_EXPT_LABEL_FLAG        = ".expt.label";
	

	public static boolean isLabel(String propName) {
		return (propName != null && (propName.endsWith(IFD_EXPT_LABEL_FLAG) || propName.endsWith(IFD_COMPOUND_LABEL_FLAG)));
	}

	public static boolean isRepresentation(String propName) {
		return (propName != null && propName.startsWith(IFD_REPRESENTATION_FLAG));
	}

	public static boolean isProperty(String propName) {
		return (propName != null && propName.startsWith(IFD_PROPERTY_FLAG));
	}

	public static boolean isFindingAid(String propName) {
		return (propName != null && propName.startsWith(IFD_FINDINGAID_FLAG));
	}
	

}
