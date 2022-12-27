package org.iupac.fairdata.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
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
				String val = trimValue(e.getValue().toString());
				if (!k.endsWith("_FLAG")) {
					htProps.put(val, new IFDProperty(val, null, null, null));
				}
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

	// IFD constants:

	public static final String IFD_FLAG = getProp("IFD_FLAG");
	public static final String IFD_PROPERTY_FLAG = getProp("IFD_PROPERTY_FLAG");
	public static final String IFD_REPRESENTATION_FLAG = getProp("IFD_REPRESENTATION_FLAG");
	public static final String IFD_OBJECT_FLAG = getProp("IFD_OBJECT_FLAG");

	public static final int propertyPrefixLength = IFD_PROPERTY_FLAG.length();


	public static final String IFD_FINDINGAID_FLAG = getProp("IFD_FINDINGAID_FLAG");
	public static final String IFD_COLLECTIONSET_FLAG = getProp("IFD_COLLECTIONSET_FLAG");
	public static final String IFD_COLLECTION_FLAG = getProp("IFD_COLLECTION_FLAG");
	public static final String IFD_ASSOCIATION_FLAG = getProp("IFD_ASSOCIATION_FLAG");

	public static final String IFD_SAMPLE_FLAG = getProp("IFD_SAMPLE_FLAG");
	public static final String IFD_STRUCTURE_FLAG = getProp("IFD_STRUCTURE_FLAG");
	public static final String IFD_COMPOUND_FLAG = getProp("IFD_COMPOUND_FLAG");
	public static final String IFD_DATAOBJECT_FLAG = getProp("IFD_DATAOBJECT_FLAG");
	public static final String IFD_ANALYSISOBJECT_FLAG = getProp("IFD_ANALYSISOBJECT_FLAG");

	public static String getObjectTypeFlag(String key) {
		return (isStructure(key) ? IFD_STRUCTURE_FLAG
				: isSample(key) ? IFD_SAMPLE_FLAG
						: isDataObject(key) ? IFD_DATAOBJECT_FLAG
								: isAnalysisObject(key) ? IFD_ANALYSISOBJECT_FLAG : null);
	}


	public static final String IFD_LABEL_FLAG = getProp("IFD_LABEL_FLAG");
	public static final String IFD_ID_FLAG = getProp("IFD_ID_FLAG");
	public static final String IFD_NOTE_FLAG = getProp("IFD_NOTE_FLAG");
	public static final String IFD_DESCRIPTION_FLAG = getProp("IFD_DESCRIPTION_FLAG");

	public static final String IFD_FINDINGAID = getProp("IFD_FINDINGAID"); 

	public static final String IFD_PROPERTY_COLLECTIONSET_BYID = IFDConst.getProp("IFD_PROPERTY_COLLECTIONSET_BYID");

	public static final String IFD_PROPERTY_COLLECTIONSET_SOURCE_DATA_LICENSE_NAME = getProp("IFD_PROPERTY_COLLECTIONSET_SOURCE_DATA_LICENSE_NAME");
	public static final String IFD_PROPERTY_COLLECTIONSET_SOURCE_DATA_LICENSE_URI = getProp("IFD_PROPERTY_COLLECTIONSET_SOURCE_DATA_LICENSE_URI");

	public static final String IFD_PROPERTY_COLLECTIONSET_ID = concat(IFD_PROPERTY_FLAG, IFD_COLLECTIONSET_FLAG, IFD_ID_FLAG);
	public static final String IFD_PROPERTY_COLLECTIONSET_REF = getProp("IFD_PROPERTY_COLLECTIONSET_REF");
	public static final String IFD_PROPERTY_COLLECTIONSET_LEN = getProp("IFD_PROPERTY_COLLECTIONSET_LEN");
	public static final String IFD_PROPERTY_COLLECTIONSET_SOURCE_DATA_URI = getProp(
			"IFD_PROPERTY_COLLECTIONSET_SOURCE_DATA_URI");
	public static final String IFD_PROPERTY_COLLECTIONSET_SOURCE_PUBLICATION_URI = getProp(
			"IFD_PROPERTY_COLLECTIONSET_SOURCE_PUBLICATION_URI");

	public static final String IFD_REP_STRUCTURE_MOL = getProp("IFD_REP_STRUCTURE_MOL");
	public static final String IFD_REP_STRUCTURE_MOL_2D = getProp("IFD_REP_STRUCTURE_MOL_2D");
	public static final String IFD_REP_STRUCTURE_MOL_3D = getProp("IFD_REP_STRUCTURE_MOL_3D");
	public static final String IFD_REP_STRUCTURE_SDF = getProp("IFD_REP_STRUCTURE_SDF");
	public static final String IFD_REP_STRUCTURE_SDF_2D = getProp("IFD_REP_STRUCTURE_SDF_2D");
	public static final String IFD_REP_STRUCTURE_SDF_3D = getProp("IFD_REP_STRUCTURE_SDF_3D");
	public static final String IFD_REP_STRUCTURE_CDX = getProp("IFD_REP_STRUCTURE_CDX");
	public static final String IFD_REP_STRUCTURE_CDXML = getProp("IFD_REP_STRUCTURE_CDXML");
	public static final String IFD_REP_STRUCTURE_CIF = getProp("IFD_REP_STRUCTURE_CIF");
	public static final String IFD_REP_STRUCTURE_CML = getProp("IFD_REP_STRUCTURE_CML");
	public static final String IFD_REP_STRUCTURE_PNG = getProp("IFD_REP_STRUCTURE_PNG");
	public static final String IFD_REP_STRUCTURE_UNKNOWN = getProp("IFD_REP_STRUCTURE_UNKNOWN");

	public static final String IFD_COMPOUNDDATA_ASSOCIATION_FLAG = getProp("IFD_COMPOUNDDATA_ASSOCIATION_FLAG");
	public static final String IFD_STRUCTUREDATA_ASSOCIATION_FLAG = getProp("IFD_STRUCTUREDATA_ASSOCIATION_FLAG");
	public static final String IFD_SAMPLEDATA_ASSOCIATION_FLAG = getProp("IFD_SAMPLEDATA_ASSOCIATION_FLAG");
	public static final String IFD_SAMPLESTRUCTURE_ASSOCIATION_FLAG = getProp("IFD_SAMPLESTRUCTURE_ASSOCIATION_FLAG");

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

	public static boolean isRepresentation(String propName) {
		return (propName != null && propName.startsWith(IFDConst.IFD_REPRESENTATION_FLAG));
	}

	public static boolean isProperty(String propName) {
		return (propName != null && propName.startsWith(IFDConst.IFD_PROPERTY_FLAG));
	}

	public static boolean isLabel(String propName) {
		return (propName != null && propName.endsWith(IFD_LABEL_FLAG));
	}

	public static boolean isID(String propName) {
		return (propName != null && propName.endsWith(IFD_ID_FLAG));
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
		return checkPropOrRep(key, IFD_DATAOBJECT_FLAG);
	}
	
	public static boolean isAnalysisObject(String key) {
		return checkPropOrRep(key, IFD_ANALYSISOBJECT_FLAG);
	}

	public static boolean isObject(String key) {
		return checkPropOrRep(key, IFDConst.IFD_OBJECT_FLAG);
	}

	/**
	 * 
	 * @param components
	 * @return
	 */
	public static String concat(String... components) {
		String s = "";
		for (int i = 0; i < components.length; i++) {
			s += components[i];			
		}
		s = s.replaceAll("\\.+", ".");
		if (s.endsWith("."))
			s = s.substring(0, s.length() - 1);
		return s;		
	}

	
	private static Map<String, String> htMediaTypes;
	
	public static String getMediaTypesForExtension(String ext) {
		if (htMediaTypes == null) {
			htMediaTypes = new HashMap<String, String>();
			String key = "IFD_MEDIATYPE_";
			for (Entry<Object, Object> e : props.entrySet()) {
				String k = (String) e.getKey();
				if (k.startsWith(key)) {
					String val = e.getValue().toString();
					val = trimValue(val);
					htMediaTypes.put(k, val);
				}
			}
		}
		return htMediaTypes.get("IFD_MEDIATYPE_" + ext.toUpperCase());
	}

	private static String trimValue(String val) {
		int pt = val.indexOf("#");
		if (pt >= 0)
			val = val.substring(0, pt);
		pt = val.indexOf(";");
		if (pt > 0)
			val = val.substring(0, pt);
		return val.trim();
	}

}
