package org.iupac.fairdata.common;

import org.iupac.fairdata.api.IFDSerializableI;
import org.iupac.fairdata.api.IFDSerializerI;

/**
 * An IFDReference object allows for saving a String or other form of reference.
 * (But for now, just a String.) It is intended to represent an actual file, not
 * a property.
 * 
 * @author hansonr
 * 
 */
public class IFDReference implements IFDSerializableI {

	/**
	 * typically a ZIP file name
	 */
	private final Object origin;
	
	/**
	 * path to this file
	 */
	private final String localPath;
	private String localName;
	
	public IFDReference(Object origin, String localName, String localPath) {
		this.origin = origin;
		if (localName != null && localName.indexOf("#")>= 0)
			System.out.println("????" + localName);
		this.localName = localName;
		this.localPath = localPath;
	}

	public Object getOrigin() {
		return origin;
	}

	public String getLocalPath() {
		return localPath;
	}
	
	public String getLocalName() {
		return localName;
	}

	@Override
	public String toString() {
		return "[IFDReference " + (localPath == null ? "" : localPath + "::") + origin + " :as::" + localName + "]";
	}

	@Override
	public void serialize(IFDSerializerI serializer) {
		if (origin != null)
			serializer.addAttr("origin", origin.toString());
		if (localName != null) {
			serializer.addAttr("localPath", localPath);
			if (localName.indexOf("#") >= 0)
				System.out.println("?????2" + localName);
			serializer.addAttr("localName", localName);
		}
	}

	@Override
	public String getSerializedType() {
		return "IFDReference";
	}


}