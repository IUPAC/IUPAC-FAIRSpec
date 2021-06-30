package org.iupac.fairspec.common;

import org.iupac.fairspec.api.IFSSerializableI;
import org.iupac.fairspec.api.IFSSerializerI;

/**
 * An IFSReference object allows for saving a String or other form of reference. 
 * (But for now, just a String.)
 *  
 * @author hansonr

 */
public class IFSReference implements IFSSerializableI {

	private final Object origin;
	private final String localPath;
	private String localName;
	
	public IFSReference(Object origin) {
		this(origin, null, null);
	}
	
	public IFSReference(Object origin, String localName, String localPath) {
		this.origin = origin;
		this.localName = localName;
		this.localPath = localPath;
	}

	public Object getOrigin() {
		return origin;
	}

	public String getLocalPath() {
		return localPath;
	}
	
	public Object getLocalName() {
		return localName;
	}

	@Override
	public String toString() {
		return "[IFSReference " + (localPath == null ? "" : localPath + "::") + origin + " :as::" + localName + "]";
	}

	@Override
	public void serialize(IFSSerializerI serializer) {
		if (origin != null)
			serializer.addAttr("origin", origin.toString());
		if (localName != null) {
			serializer.addAttr("localPath", localPath);
			serializer.addAttr("localName", localName);
		}
	}

	@Override
	public String getSerializedType() {
		return "IFSReference";
	}


}