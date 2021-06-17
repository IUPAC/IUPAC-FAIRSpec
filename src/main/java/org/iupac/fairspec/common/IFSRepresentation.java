package org.iupac.fairspec.common;

import org.iupac.fairspec.api.IFSSerializableI;
import org.iupac.fairspec.api.IFSSerializerI;

/**
 * Metadata referencing a representation (an actual digital object), optionally
 * holding a copy of it.
 * 
 * @author hansonr
 *
 */
public class IFSRepresentation implements IFSSerializableI {

	/**
	 * The type of this data -- to be specified...
	 */
	private final String type;
	private final IFSReference ref;
	private final Object data;
	private long len;
	
	public IFSRepresentation(String type, IFSReference ref, Object data, long len) {
		this.type = type;
		this.ref = ref;
		this.data = data;
		this.len = len;
	}

	public String getType() {
		return type;
	}

	public IFSReference getRef() {
		return ref;
	}

	public Object getData() {
		return data;
	}

	public void setLength(long len) {
		this.len = len;
	}
	public long getLength() {
		return len;
	}

	public String toString() {
		return "[IFSRepresentation type=" + type + " ref=" + ref + "]";
	}

	@Override
	public void serialize(IFSSerializerI serializer) {
		serializer.addAttr("type", type);
		serializer.addAttrInt("len", (int) len);
		serializer.addObject("ref", ref);
		if (data != null)
			serializer.addObject("data", data);
	}

	@Override
	public String getSerializedType() {
		return type;
	}

}
