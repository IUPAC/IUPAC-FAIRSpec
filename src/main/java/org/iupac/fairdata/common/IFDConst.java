package org.iupac.fairdata.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * Constants relating to properties and extraction.
 * 
 * @author hansonr
 *
 */
public class IFDConst {

	public interface ClassTypes {

		public final static String FindingAid = "org.iupac.fairdata.core.IFDFindingAid";

		public final static String Sample = "org.iupac.fairdata.sample.IFDSample";
		public final static String SampleCollection = "org.iupac.fairdata.sample.IFDSampleCollection";

		public final static String Structure = "org.iupac.fairdata.structure.IFDStructure";
		public final static String StructureCollection = "org.iupac.fairdata.structure.IFDStructureCollection";

		public final static String DataObject = "org.iupac.fairdata.dataobject.IFDDataObject";
		public final static String DataObjectCollection = "org.iupac.fairdata.dataobject.IFDDataObjectCollection";

		public final static String SampleDataAssociation = "org.iupac.fairdata.helpers.IFDSampleDataAssociation";
		public final static String SampleDataAssociationCollection = "org.iupac.fairdata.helpers.IFDSampleDataAssociationCollection";

		public final static String StructureDataAssociation = "org.iupac.fairdata.helpers.IFDStructureDataAssociation";
		public final static String StructureDataAssociationCollection = "org.iupac.fairdata.helpers.IFDStructureDataAssociationCollection";

		public final static String SampleDataAnalysis = "org.iupac.fairdata.helpers.IFDSampleDataAnalysis";
		public final static String SampleDataAnalysisCollection = "org.iupac.fairdata.helpers.IFDSampleDataAnalysisCollection";

		public final static String StructureDataAnalysis = "org.iupac.fairdata.helpers.IFDStructureDataAnalysis";
		public final static String StructureDataAnalysisCollection = "org.iupac.fairdata.helpers.IFDStructureDataAnalysisCollection";

	}

	private static Properties props;
	private static Map<String, String> propertyMap;

	static {
		try {
			File f = new File(IFDConst.class.getName().replace('.', '/'));
			String s = f.getParent().replace('\\', '/') + "/fairspec.properties";
			URL u = IFDConst.class.getClassLoader().getResource(s);
			System.out.println(u.getFile());
			props = new Properties();
			InputStream is = u.openStream();
			props.load(is);
			System.out.println("IFDConst: " + props.size() + " properties");
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	public static String getProp(String key) {
		String s = props.getProperty(key);
		if (s == null) {
			System.err.println("IDFDefaultVendorPlugin Property " + key + " was not found");
			s = key;
		}
		return s;
	}

	public static final String IFD_VERSION = getProp("IFD_VERSION");

	public static final String IFD_PROP_SAMPLE_LABEL = getProp("IFD_PROP_SAMPLE_LABEL");

	public static final String IFD_FINDING_AID_FLAG = getProp("IFD_FINDING_AID_FLAG");
	public static final String IFD_PROPERTY_FLAG = getProp("IFD_PROPERTY_FLAG");
	public static final String IFD_REPRESENTATION_FLAG = getProp("IFD_REPRESENTATION_FLAG");
	public static final String IFD_COMPOUND_LABEL_FLAG = getProp("IFD_COMPOUND_LABEL_FLAG");
	public static final String IFD_EXPT_LABEL_FLAG = getProp("IFD_EXPT_LABEL_FLAG");

	public static final String IFD_FINDING_AID = getProp("IFD_FINDING_AID"); // root name for JSON

	public static final String IFD_PROP_FAIRDATA_COLLECTION = getProp("IFD_PROP_FAIRDATA_COLLECTION");
	public static final String IFD_PROP_FAIRDATA_COLLECTION_ID = getProp("IFD_PROP_FAIRDATA_COLLECTION_ID");
	public static final String IFD_PROP_FAIRDATA_COLLECTION_REF = getProp("IFD_PROP_FAIRDATA_COLLECTION_REF");
	public static final String IFD_PROP_FAIRDATA_COLLECTION_LEN = getProp("IFD_PROP_FAIRDATA_COLLECTION_LEN");
	public static final String IFD_PROP_FAIRDATA_COLLECTION_OBJECT = getProp("IFD_PROP_FAIRDATA_COLLECTION_OBJECT");
	public static final String IFD_PROP_FAIRDATA_COLLECTION_SOURCE_DATA_URI = getProp(
			"IFD_PROP_FAIRDATA_COLLECTION_SOURCE_DATA_URI");
	public static final String IFD_PROP_FAIRDATA_COLLECTION_SOURCE_PUBLICATION_URI = getProp(
			"IFD_PROP_FAIRDATA_COLLECTION_SOURCE_PUBLICATION_URI");

	public static final String IFD_REP_STRUCTURE_MOL = getProp("IFD_REP_STRUCTURE_MOL");
	public static final String IFD_REP_STRUCTURE_MOL_2D = getProp("IFD_REP_STRUCTURE_MOL_2D");
	public static final String IFD_REP_STRUCTURE_MOL_3D = getProp("IFD_REP_STRUCTURE_MOL_3D");
	public static final String IFD_REP_STRUCTURE_SDF = getProp("IFD_REP_STRUCTURE_SDF");
	public static final String IFD_REP_STRUCTURE_SDF_2D = getProp("IFD_REP_STRUCTURE_SDF_2D");
	public static final String IFD_REP_STRUCTURE_SDF_3D = getProp("IFD_REP_STRUCTURE_SDF_3D");
	public static final String IFD_REP_STRUCTURE_CDX = getProp("IFD_REP_STRUCTURE_CDX");
	public static final String IFD_REP_STRUCTURE_CDXML = getProp("IFD_REP_STRUCTURE_CDXML");
	public static final String IFD_REP_STRUCTURE_PNG = getProp("IFD_REP_STRUCTURE_PNG");
	public static final String IFD_REP_STRUCTURE_UNKNOWN = getProp("IFD_REP_STRUCTURE_UNKNOWN");

	public enum PROPERTY_TYPE {
		INT, FLOAT, STRING, NUCL, OBJ
	};

	public enum PROPERTY_UNIT {
		NONE, HZ, MHZ, CELCIUS, KELVIN
	};

	// Finding Aid properties:

	public static final String IFD_PROP_FAIRDATA_COLLECTION_DATA_LICENSE_NAME = "IFD.property.collection.data.license.name";
	public static final String IFD_PROP_FAIRDATA_COLLECTION_DATA_LICENSE_URI = "IFD.property.collection.data.license.uri";

	// IFDExtractorI constants:

	/**
	 * regex for files that are absolutely worthless
	 */
	public static final String junkFilePattern = "(MACOSX)|(desktop\\.ini)|(\\.DS_Store)";

	/**
	 * the files we want extracted -- just PDF and PNG here; all others are taken
	 * care of by individual IFDVendorPluginI classes
	 */
	public static final String defaultCachePattern = "" + "(?<img>\\.pdf$|\\.png$)"
//			+ "|(?<text>\\.log$|\\.out$|\\.txt$)"// maybe put these into JSON only? 
	;

	public static boolean isLabel(String propName) {
		return (propName != null
				&& (propName.endsWith(IFD_EXPT_LABEL_FLAG) || propName.endsWith(IFD_COMPOUND_LABEL_FLAG)));
	}

	public static boolean isRepresentation(String propName) {
		return (propName != null && propName.startsWith(IFD_REPRESENTATION_FLAG));
	}

	public static boolean isProperty(String propName) {
		return (propName != null && propName.startsWith(IFD_PROPERTY_FLAG));
	}

	public static boolean isFindingAid(String propName) {
		return (propName != null && propName.startsWith(IFD_FINDING_AID_FLAG));
	}

	public static final String DATA_FLAG = getProp("DATA_FLAG");
	public static final String IFD_PROP_SAMPLE_ID = getProp("IFD_PROP_SAMPLE_ID");

	public static void main(String[] args) {
		System.out.println(getProp("IFD_REP_SPEC_UVVIS_PEAKLIST"));
	}

	public static Map<String, IFDProperty> setProperties(Map<String, IFDProperty> htProps, String key, String notKey) {
		if (htProps == null)
			htProps = new Hashtable<String, IFDProperty>();
		for (Entry<Object, Object> e : props.entrySet()) {
			String k = (String) e.getKey();
			if (k.startsWith(key)) {
				// to be continued! -- need units and type
				htProps.put(k, new IFDProperty(k, e.getValue(), null, null));
			}
		}
		return htProps;
	}

	public static String[] getPropertiesAsArray(String key, String notKey) {
		List<String> lst = new ArrayList<>();
		for (Object o : props.keySet()) {
			String k = (String) o;
			if (k.startsWith(key) && notKey == null || !key.startsWith(notKey)) {
				lst.add(k);
			}
		}
		return lst.toArray(new String[lst.size()]);
	}
}
