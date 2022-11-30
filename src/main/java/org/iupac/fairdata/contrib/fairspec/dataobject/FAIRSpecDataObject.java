package org.iupac.fairdata.contrib.fairspec.dataobject;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.dataobject.IFDDataObject;

/**
 * A final class for high-resolution mass spec data.
 * It is final because it is created by reflection.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public abstract class FAIRSpecDataObject extends IFDDataObject {

	@Override
	protected String getPropertyPrefixForSerialization() {
		return serializerPropertyPrefix;
	}
	

	public FAIRSpecDataObject() {
		super();
	}

	private String serializerPropertyPrefix;
	
	public static FAIRSpecDataObject createFAIRSpecObject(String key) {
		String type = key.substring(key.lastIndexOf(".") + 1);
		String ucType = type.toUpperCase();
		String className = FAIRSpecDataObject.class.getName();
		className = className.substring(0, className.lastIndexOf(".") + 1) + type + ".FAIRSpec" + ucType
				+ "Data";
		try {			
			FAIRSpecDataObject o = (FAIRSpecDataObject) Class.forName(className).newInstance();
			// properties are loaded based on subtype
			o.serializerPropertyPrefix = IFDConst.concat(IFDConst.IFD_PROPERTY_FLAG, 
					IFDConst.getProp("FAIRSPEC_DATAOBJECT_FAIRSPEC_" + ucType + "_FLAG"));
			o.setProperties("IFD_PROPERTY" + key, null); 
			return o;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
