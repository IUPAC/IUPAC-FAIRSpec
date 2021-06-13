package org.iupac.fairspec.common;

/**
 * Metadata referencing a representation (an actual digital object), optionally
 * holding a copy of it.
 * 
 * @author hansonr
 *
 */
public class IFSRepresentation {

	/**
	 * The type of this data -- to be specified...
	 */
	private final String type;
	private final IFSReference ref;
	private final Object data;
	private final long len;
	
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

	public long getLength() {
		return len;
	}


}
