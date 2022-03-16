package org.iupac.fairdata.spec;

import org.iupac.fairdata.common.IFDReference;
import org.iupac.fairdata.common.IFDRepresentation;

/**
 * A class that refers to a specific representation of a spectroscopy data object. 
 * The data object may be stored directly in the representation or may be referenced.
 * @author hansonr
 *
 */
public abstract class IFDSpecDataRepresentation extends IFDRepresentation {

	public IFDSpecDataRepresentation(IFDReference ref, Object data, long len, String type, String subtype) {
		super(ref, data, len, type, subtype);
	}

}
