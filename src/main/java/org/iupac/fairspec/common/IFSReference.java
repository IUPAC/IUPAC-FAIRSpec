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

	private final Object ref;
	private final String path;
	private String localName;
	
	public IFSReference(Object ref) {
		this(ref, null, null);
	}
	
	public IFSReference(Object ref, String localName, String path) {
		this.ref = ref;
		this.localName = localName;
		this.path = path;
	}

	public Object getValue() {
		return ref;
	}

	public String getPath() {
		return path;
	}
	
	@Override
	public String toString() {
		return "[IFSReference " + (path == null ? "" : path + "::") + ref + "]";
	}

	@Override
	public void serialize(IFSSerializerI serializer) {
		serializer.addAttr("path", path + "/" + localName);
		//serializer.addValue(ref);
	}

	@Override
	public String getSerializedType() {
		return "IFSReference";
	}


}