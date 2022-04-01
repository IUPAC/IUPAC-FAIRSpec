package org.iupac.fairdata.common;

import org.iupac.fairdata.api.IFDSerializableI;
import org.iupac.fairdata.api.IFDSerializerI;

/**
 * A simple reference/length holder.
 * 
 * @author hansonr
 *
 */
public class IFDResource implements IFDSerializableI {
	private String ref;
	private long len;

	public IFDResource(String ref, long length) {
		this.setRef(ref);
		this.setLength(length);
	}

	@Override
	public void serialize(IFDSerializerI serializer) {
		if (getRef() != null)
			serializer.addAttr("ref", getRef());
		if (getLength() > 0)
			serializer.addAttrInt("len", getLength());
	}

	@Override
	public String getSerializedType() {
		return "resource";
	}

	@Override
	public String toString() {
		return "[Resource " + getRef() + " len " + getLength() + "]";
	}

	public long getLength() {
		return len;
	}

	public void setLength(long len) {
		this.len = len;
	}

	public String getRef() {
		return ref;
	}

	public void setRef(String ref) {
		this.ref = ref;
	}
}