package org.iupac.fairspec.spec;

import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.common.IFSRepresentation;

/**
 * A class that refers to a specific representation of a spectroscopy data object. 
 * The data object may be stored directly in the representation or may be referenced.
 * @author hansonr
 *
 */
public abstract class IFSSpecDataRepresentation extends IFSRepresentation {

	public IFSSpecDataRepresentation(String type, IFSReference ref, Object data, long len) {
		super(type, ref, data, len);
	}

}
