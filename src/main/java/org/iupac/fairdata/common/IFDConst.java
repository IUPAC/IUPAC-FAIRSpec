package org.iupac.fairdata.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.iupac.fairdata.core.IFDProperty;

/**
 * Constants relating to properties
 * 
 * @author hansonr
 *
 */
public class IFDConst {

	private static Properties props;

	static {
		File f = new File(IFDConst.class.getName().replace('.', '/'));
		String propertyFile = f.getParent().replace('\\', '/') + "/ifd.properties";
		addProperties(propertyFile);
	}

	public static String getProp(String key) {
		String s = props.getProperty(key.toUpperCase().replace('.','_'));
		if (s == null) {
			System.err.println("IFDConst Property " + key + " was not found");
			s = key;
		}
		return s.trim();
	}

	public static void addProperties(String propertyFile) {
		try {			
			URL u = IFDConst.class.getClassLoader().getResource(propertyFile);
			System.out.println(u.getFile());
			if (props == null)
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

	public static Map<String, IFDProperty> setProperties(Map<String, IFDProperty> htProps, String key, String notKey) {
		if (htProps == null)
			htProps = new Hashtable<String, IFDProperty>();
		key = key.toUpperCase().replace('.', '_');
		for (Entry<Object, Object> e : props.entrySet()) {
			String k = (String) e.getKey();
			if (k.startsWith(key)) {
				// to be continued! -- need units and type
				String val = e.getValue().toString();
				int pt = val.indexOf(";");
				if (pt > 0)
					val = val.substring(0, pt);
				htProps.put(val, new IFDProperty(val.trim(), null, null, null));
			}
		}
		return htProps;
	}

	public static IFDProperty getIFDProperty(Map<String, IFDProperty> htProps, String name) {
		return htProps.get(name);
	}

//	public static String[] getPropertiesAsArray(String key, String notKey) {
//		List<String> lst = new ArrayList<>();
//		for (Object o : props.keySet()) {
//			String k = (String) o;
//			if (k.startsWith(key) && notKey == null || !key.startsWith(notKey)) {
//				lst.add(k);
//			}
//		}
//		return lst.toArray(new String[lst.size()]);
//	}

	// see org.iupac.fairdata.common.fairspec.properties
	
	public static final String IFD_VERSION = getProp("IFD_VERSION");

	public static final String IFD_PROP_SAMPLE_LABEL = getProp("IFD_PROP_SAMPLE_LABEL");

	public static final String IFD_FINDING_AID = getProp("IFD_FINDING_AID"); // root name for JSON

	public static final String IFD_PROP_COLLECTIONSET_SOURCE_DATA_LICENSE_NAME = getProp("IFD_PROP_COLLECTIONSET_SOURCE_DATA_LICENSE_NAME");
	public static final String IFD_PROP_COLLECTIONSET_SOURCE_DATA_LICENSE_URI = getProp("IFD_PROP_COLLECTIONSET_SOURCE_DATA_LICENSE_URI");

	public static final String IFD_PROP_COLLECTIONSET = getProp("IFD_PROP_COLLECTIONSET");
	public static final String IFD_PROP_COLLECTIONSET_ID = getProp("IFD_PROP_COLLECTIONSET_ID");
	public static final String IFD_PROP_COLLECTIONSET_REF = getProp("IFD_PROP_COLLECTIONSET_REF");
	public static final String IFD_PROP_COLLECTIONSET_LEN = getProp("IFD_PROP_COLLECTIONSET_LEN");
	public static final String IFD_PROP_COLLECTIONSET_SOURCE_DATA_URI = getProp(
			"IFD_PROP_COLLECTIONSET_SOURCE_DATA_URI");
	public static final String IFD_PROP_COLLECTIONSET_SOURCE_PUBLICATION_URI = getProp(
			"IFD_PROP_COLLECTIONSET_SOURCE_PUBLICATION_URI");

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

	public static String getVersion() {
		return "IFD " + IFD_VERSION;
	}



	/**
	 * not implemented
	 * 
	 * @author hansonr
	 *
	 */
	public enum PROPERTY_TYPE {
		INT, FLOAT, STRING, NUCL, OBJ
	};

	/**
	 * not implemented
	 * 
	 * @author hansonr
	 *
	 */
	public enum PROPERTY_UNIT {
		NONE, HZ, MHZ, CELCIUS, KELVIN
	};

	// IFDExtractorI constants:

	public static final String IFD_LABEL_FLAG = getProp("IFD_LABEL_FLAG");
	public static final String IFD_REPRESENTATION_FLAG = getProp("IFD_REPRESENTATION_FLAG");
	public static final String IFD_PROPERTY_FLAG = getProp("IFD_PROPERTY_FLAG");
	public static final String IFD_SAMPLE_FLAG = getProp("IFD_SAMPLE_FLAG");
	public static final String IFD_STRUCTURE_FLAG = getProp("IFD_STRUCTURE_FLAG");
	public static final String IFD_DATA_FLAG = getProp("IFD_DATA_FLAG");
	public static final String IFD_ANALYSIS_FLAG = getProp("IFD_ANALYSIS_FLAG");

	public static boolean isRepresentation(String propName) {
		return (propName != null && propName.startsWith(IFDConst.IFD_REPRESENTATION_FLAG));
	}

	public static boolean isProperty(String propName) {
		return (propName != null && propName.startsWith(IFDConst.IFD_PROPERTY_FLAG));
	}

	public static boolean isLabel(String propName) {
		return (propName != null && propName.endsWith(IFD_LABEL_FLAG));
	}

	public static boolean checkPropOrRep(String key, String type) {
		if (key == null || type == null)
			return false;
		String prefix = (isProperty(key) ? IFD_PROPERTY_FLAG : isRepresentation(key) ? IFD_REPRESENTATION_FLAG : null);
		return (prefix != null && key.indexOf(type) == prefix.length() - 1);
	}
	
	public static boolean isSample(String key) {
		return checkPropOrRep(key, IFD_SAMPLE_FLAG);
	}

	public static boolean isStructure(String key) {
		return checkPropOrRep(key, IFD_STRUCTURE_FLAG);
	}
	
	public static boolean isDataObject(String key) {
		return checkPropOrRep(key, IFD_DATA_FLAG);
	}
	
	public static boolean isAnalysisObject(String key) {
		return checkPropOrRep(key, IFD_ANALYSIS_FLAG);
	}
	

}
