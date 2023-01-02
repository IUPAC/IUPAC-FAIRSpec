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

	static int idcount = 0;
	/**
	 * the IFDFindingAid id of this resource
	 */
	private String id = null;

	private final String rootPath;

	public IFDResource(String ref, String rootPath, String id, long length) {
		this.ref = ref;
		this.id = (id == null ? "" + (++idcount) : id);
		this.len = length;
		this.rootPath = rootPath;
	}

	public String getRootPath() {
		return rootPath;
	}
	
	@Override
	public void serialize(IFDSerializerI serializer) {
		IFDObject.serializeClass(serializer, getClass(), null);
		if (getRef() != null)
			serializer.addAttr("ref", ref);
		if (getLength() > 0)
			serializer.addAttrInt("len", len);
		serializer.addAttr("id", id);
	}

	@Override
	public String getSerializedType() {
		return "resource";
	}

	@Override
	public String toString() {
		return "[Resource " + id + ": " + ref + " len " + len + "]";
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

	public String getID() {
		return id;
	}
	
	public void setID(String id) {
		this.id = id;
	}

}