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

import org.iupac.fairdata.contrib.IFDFAIRSpecExtractorHelper;

/**
 * Constants relating to properties and extraction.
 * 
 * @author hansonr
 *
 */
public class IFDConst {

	private static Properties props;

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

	public static final String IFD_FINDING_AID = getProp("IFD_FINDING_AID"); // root name for JSON

	public static final String IFD_PROP_FAIRDATA_COLLECTION_DATA_LICENSE_NAME = getProp("IFD_PROP_FAIRDATA_COLLECTION_DATA_LICENSE_NAME");
	public static final String IFD_PROP_FAIRDATA_COLLECTION_DATA_LICENSE_URI = getProp("IFD_PROP_FAIRDATA_COLLECTION_DATA_LICENSE_URI");

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

	// IFDExtractorI constants:

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

	public static final String IFD_REPRESENTATION_FLAG = getProp("IFD_REPRESENTATION_FLAG");
	public static final String IFD_PROPERTY_FLAG = getProp("IFD_PROPERTY_FLAG");
	public static final String DATA_FLAG = getProp("DATA_FLAG");
	public static final String IFD_PROP_SAMPLE_ID = getProp("IFD_PROP_SAMPLE_ID");

	public static boolean isRepresentation(String propName) {
		return (propName != null && propName.startsWith(IFDConst.IFD_REPRESENTATION_FLAG));
	}

	public static boolean isProperty(String propName) {
		return (propName != null && propName.startsWith(IFDConst.IFD_PROPERTY_FLAG));
	}

	public static boolean isLabel(String propName) {
		return (propName != null
				&& (propName.endsWith(IFDFAIRSpecExtractorHelper.IFD_EXPT_LABEL_FLAG) || propName.endsWith(IFDFAIRSpecExtractorHelper.IFD_COMPOUND_LABEL_FLAG)));
	}

	public static boolean isFindingAid(String propName) {
		return (propName != null && propName.startsWith(IFDFAIRSpecExtractorHelper.IFD_FINDING_AID_FLAG));
	}

}
