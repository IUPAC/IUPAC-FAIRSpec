package org.iupac.fairdata.core;

import org.iupac.fairdata.api.IFDSerializableI;
import org.iupac.fairdata.api.IFDSerializerI;

/**
 * A simple reference/length holder.
 * 
 * @author hansonr
 *
 */
public class IFDResource implements IFDSerializableI {
	
	/**
	 * the URI to this resource or some local descriptor
	 */
	private String ref;
	
	/**
	 * the byte length of this resource, or 0 if not known
	 * 
	 */
	private long len;
	
	/**
	 * the IFDFindingAid index of this resource
	 */
	private int index = -1;

	public IFDResource(String ref, int index, long length) {
		this.ref = ref;
		this.index = index;
		this.len = length;
	}

	@Override
	public void serialize(IFDSerializerI serializer) {
		if (getRef() != null)
			serializer.addAttr("ref", ref);
		if (getLength() > 0)
			serializer.addAttrInt("len", len);
		if (index >= 0)
			serializer.addAttrInt("index", index);
	}

	@Override
	public String getSerializedType() {
		return "resource";
	}

	@Override
	public String toString() {
		return "[Resource " + index + ": " + ref + " len " + len + "]";
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

	public int getIndex() {
		return index;
	}
	
	public void setIndex(int index) {
		this.index = index;
	}

}