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
	private final String path;
	private String localName;
	
	public IFSReference(Object origin) {
		this(origin, null, null);
	}
	
	public IFSReference(Object origin, String localName, String path) {
		this.origin = origin;
		this.localName = localName;
		this.path = path;
	}

	public Object getOrigin() {
		return origin;
	}

	public String getPath() {
		return path;
	}
	
	public Object getLocalName() {
		return localName;
	}

	@Override
	public String toString() {
		return "[IFSReference " + (path == null ? "" : path + "::") + origin + " :as::" + localName + "]";
	}

	@Override
	public void serialize(IFSSerializerI serializer) {
		if (origin != null)
			serializer.addAttr("origin", origin.toString());
		if (localName != null)
			serializer.addAttr("localPath", path + "/" + localName);
		//serializer.addValue(ref);
	}

	@Override
	public String getSerializedType() {
		return "IFSReference";
	}


}