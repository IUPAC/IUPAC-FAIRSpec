package org.iupac.fairdata.contrib.fairspec.dataobject;

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

	public FAIRSpecDataObject() {
		super();
	}
	
	public static FAIRSpecDataObject createFAIRSpecObject(String rootPath, String key) {
		String type = key.substring(key.lastIndexOf(".") + 1);
		String className = FAIRSpecDataObject.class.getName();
		className = className.substring(0, className.lastIndexOf(".") + 1) + type + ".FAIRSpec" + type.toUpperCase()
				+ "Data";
		try {
			FAIRSpecDataObject o = (FAIRSpecDataObject) Class.forName(className).newInstance();
			o.setPath(rootPath);
			// properties are loaded based on subtype
			o.setProperties("IFD_PROP_" + key, null); 
			return o;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
