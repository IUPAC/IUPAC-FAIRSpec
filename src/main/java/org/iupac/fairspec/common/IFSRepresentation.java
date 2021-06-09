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
	private String type;
	private IFSReference ref;
	private Object data;
	
	public IFSRepresentation(String type, IFSReference ref, Object data) {
		this.type = type;
		this.ref = ref;
		this.data = data;
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

	public void setRef(IFSReference ref) {
		this.ref = ref;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

}
