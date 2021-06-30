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
public abstract class IFSRepresentation implements IFSSerializableI {

	/**
	 * The type of this data -- to be specified...
	 */
	private String type;
	private final IFSReference ref;
	private final Object data;
	private long len;
	private String subtype;
	
	public IFSRepresentation(IFSReference ref, Object data, long len, String type, String subtype) {
		this.type = type;
		this.ref = ref;
		this.data = data;
		this.len = len;
		this.subtype = subtype;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
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


	public String getSubtype() {
		return subtype;
	}

	public void setSubtype(String subtype) {
		this.subtype = subtype;
	}

	@Override
	public String toString() {
		return "[IFSRepresentation type=" + type + " ref=" + ref + "]";
	}

	@Override
	public void serialize(IFSSerializerI serializer) {
		if (type != null && !type.equals("unknown"))
			serializer.addAttr("type", type);
		if (subtype != null)
			serializer.addAttr("subtype", subtype);
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
