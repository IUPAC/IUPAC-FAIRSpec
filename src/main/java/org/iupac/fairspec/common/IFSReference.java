package org.iupac.fairspec.common;

/**
 * An IFSReference object allows for saving a String or other form of reference. 
 * (But for now, just a String.)
 *  
 * @author hansonr

 */
public class IFSReference {

	private final Object ref;
	private final String path;
	
	public IFSReference(Object ref) {
		this(ref, null);
	}
	
	public IFSReference(Object ref, String path) {
		this.ref = ref;
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


}