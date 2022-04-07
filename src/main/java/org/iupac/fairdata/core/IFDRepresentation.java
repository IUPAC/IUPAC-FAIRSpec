package org.iupac.fairdata.core;

import org.iupac.fairdata.api.IFDSerializableI;
import org.iupac.fairdata.api.IFDSerializerI;

/**
 * Metadata referencing a representation (an actual digital object), optionally
 * holding a copy of it.
 * 
 * @author hansonr
 *
 */
public abstract class IFDRepresentation implements IFDSerializableI {

	/**
	 * The type of this data -- to be specified...
	 */
	private String type;
	private final IFDReference ref;
	private final Object data;
	private long len;
	private String subtype;
	
	public IFDRepresentation(IFDReference ref, Object data, long len, String type, String subtype) {
		this.type = type;
		this.ref = ref;
		this.data = data;
		this.len = (ref != null || len != 0 ? len
				: data instanceof String ? ((String) data).length()
						: data instanceof byte[] ? ((byte[]) data).length : 0);
		this.subtype = subtype;
	}

	public IFDRepresentation(IFDRepresentation rep) {
		this(rep.getRef(), rep.getData(), rep.getLength(), rep.getType(), rep.getSubtype());
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public IFDReference getRef() {
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
		return "[IFDRepresentation type=" + type + " ref=" + ref + "]";
	}

	@Override
	public void serialize(IFDSerializerI serializer) {
		serializer.addAttr("type", type == null ? "unknown" : type);
		if (subtype != null && !subtype.equals(type))
			serializer.addAttr("subtype", subtype);
		serializer.addAttrInt("len", (int) len);
		if (ref != null)
			serializer.addObject("ref", ref);
		if (data != null)
			serializer.addObject("data", data);
	}

	@Override
	public String getSerializedType() {
		return type;
	}

}
