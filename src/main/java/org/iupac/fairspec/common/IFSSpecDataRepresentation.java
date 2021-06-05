package org.iupac.fairspec.common;

/**
 * A class that refers to a specific representation of a data object. 
 * The data object may be stored directly in the representation or may be referenced.
 * @author hansonr
 *
 */
public abstract class IFSSpecDataRepresentation extends IFSRepresentation {

	public IFSSpecDataRepresentation(String type, IFSReference ref, Object data) {
		super(type, ref, data);
	}

}
