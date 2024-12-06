package org.iupac.fairdata.contrib.fairspec.dataobject;

import org.iupac.fairdata.api.IFDSerializerI;
import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.core.IFDReference;
import org.iupac.fairdata.core.IFDRepresentation;
import org.iupac.fairdata.dataobject.IFDDataObject;

/**
 * A final class for high-resolution mass spec data. It is final because it is
 * created by reflection.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class FAIRSpecDataObject extends IFDDataObject {

	private static String basePropertyPrefix = IFDConst.concat(IFDConst.IFD_PROPERTY_FLAG, IFDConst.IFD_DATAOBJECT_FLAG,
			"fairspec.");

	protected String exptMethod;
	
	/**
	 * e.g. NMR, IR, MS, ...
	 * 
	 * @param method
	 */
	public void setExptMethod(String method) {
		exptMethod = method.toUpperCase();
	}

	@Override
	protected String getIFDPropertyPrefix() {
		return (serializerPropertyPrefix == null ? basePropertyPrefix : serializerPropertyPrefix);
	}

	@Override
	protected String getPropertyPrefixForSerialization() {
		return serializerPropertyPrefix;
	}

	public FAIRSpecDataObject(String name) {
		super();
		setProperties(basePropertyPrefix + "." + name, null);
	}

	private String serializerPropertyPrefix;
	private String objectType;

//	@Override
//	public String getObjectFlag() {
//		return objectType;
//	}

	/**
	 * This works with "nmr" or "xxx.xxx.nmr"
	 * 
	 * @param key
	 * @return
	 */
	public static FAIRSpecDataObject createFAIRSpecObject(String key) {
		// backward compatibility:
		//
		if (key == null)
			key = "unknown";
		String type = key.substring(key.lastIndexOf(".") + 1);
		String ucType = type.toUpperCase();
		String className = FAIRSpecDataObject.class.getName();
		className = className.substring(0, className.lastIndexOf(".") + 1) + type + ".FAIRSpec" + ucType + "Data";
		try {
			FAIRSpecDataObject o = (FAIRSpecDataObject) Class.forName(className).newInstance();
			// properties are loaded based on subtype
			o.objectType = IFDConst.getProp("DATAOBJECT_FAIRSPEC_" + ucType + "_FLAG");
			o.setExptMethod(ucType);
			String prefix = IFDConst.concat(IFDConst.IFD_PROPERTY_FLAG, o.objectType);
			prefix = prefix.substring(0, prefix.length() -1 - ucType.length());
			o.serializerPropertyPrefix = prefix;
			o.setProperties(prefix + "." + type, null);
			return o;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	protected IFDRepresentation newRepresentation(IFDReference ifdReference, Object object, long len, String type,
			String subtype) {
		// not applicable to this pseudo-abstract object with private constructor
		throw new NullPointerException("FAIRSpecDataObject cannot be instantialized directly");
	}


	@Override
	protected void serializeProps(IFDSerializerI serializer) {
		super.serializeProps(serializer);
		if (exptMethod != null)
			serializer.addAttr("exptMethod", exptMethod);
	}
}
