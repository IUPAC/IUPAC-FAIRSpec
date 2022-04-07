package org.iupac.fairdata.core;

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
	 * Origin object; typically a ZIP file name; toString() will be used for serialization
	 */
	private final Object origin;
	
	/**
	 * root path to this file
	 */
	private final String localPath;
	
	/**
	 * name of this file
	 */
	private String localName;
	
	public IFDReference(Object origin, String localPath, String localName) {
		this.origin = origin;
		this.localPath = localPath;
		this.localName = localName;
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
			serializer.addAttr("path", localPath + "/" + localName);
			// TODO: Could add #page=" to origin; localPath is null?
//			serializer.addAttr("localName", localName);
		}
	}

	@Override
	public String getSerializedType() {
		return "IFDReference";
	}


}