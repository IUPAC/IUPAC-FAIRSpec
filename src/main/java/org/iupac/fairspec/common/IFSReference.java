package org.iupac.fairspec.common;

/**
 * An IFSReference object allows for saving a String or other form of reference. (But for now, just a String.)
 *  
 * @author hansonr

 */
public class IFSReference {

	private String ref;
	
	public IFSReference(String ref) {
		this.ref = ref;
	}

	public String getRef() {
		return ref;
	}

	public void setRef(String ref) {
		this.ref = ref;
	}
	
}