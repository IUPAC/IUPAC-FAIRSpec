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
	private String representationType;
	private final IFDReference ref;
	private final Object data;
	private long len;
	private String mediaType;
	private int test;
	
	private static int staticTest;
	
	public IFDRepresentation(IFDReference ref, Object data, long len, String type, String subtype) {
		this.representationType = type;
		this.ref = ref;
		this.test = staticTest++;
//			System.out.println(this.test + " " + subtype + " " + len + " " + ref);
//			if (this.test == 44)
//				System.out.println("IDFReptest");
		this.data = data;
		this.len = (ref != null || len != 0 ? len
				: data instanceof String ? ((String) data).length()
						: data instanceof byte[] ? ((byte[]) data).length : 0);
		this.mediaType = subtype;
//		if (ref != null && ref.getLocalName().indexOf("/") >= 0)
//			System.out.println("IDFRef ???");
//		if (this.len == 0)
//			System.out.println("IDFRef " + this);
	}

	public IFDRepresentation(IFDRepresentation rep) {
		this(rep.getRef(), rep.getData(), rep.getLength(), rep.getType(), rep.getMediaType());
	}

	public String getType() {
		return representationType;
	}

	public void setType(String type) {
		this.representationType = type;
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


	public String getMediaType() {
		return mediaType;
	}

	public void setMediaType(String mediaType) {
		this.mediaType = mediaType;
	}

	@Override
	public String toString() {
		return "[IFDRepresentation type=" + representationType + " ref=" + ref + "]";
	}

	@Override
	public String getSerializedType() {
		return representationType;
	}

	@Override
	public void serialize(IFDSerializerI serializer) {
		IFDObject.serializeClass(serializer, getClass(), null);
		serializer.addAttr("representationType", representationType == null ? "unknown" : representationType);
		if (mediaType != null && !mediaType.equals(representationType))
			serializer.addAttr("mediaType", mediaType);
		serializer.addAttrInt("len", (int) len);
		if (ref != null)
			serializer.addObject("ref", ref);
		if (data != null)
			serializer.addObject("data", data);
	}


}
