package org.iupac.fairspec.common;

/**
 * An IFSReference object allows for saving a String or other form of reference. 
 * (But for now, just a String.)
 *  
 * @author hansonr

 */
public class IFSReference {

	private final Object ref;
	
	public IFSReference(Object ref) {
		this.ref = ref;
	}

	public Object getValue() {
		return ref;
	}

	@Override
	public String toString() {
		return ref.toString();
	}
	
}