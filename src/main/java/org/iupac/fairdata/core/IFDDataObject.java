package org.iupac.fairdata.core;

import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.common.IFDRepresentation;

/**
 * A generic interface indicating some sort of data. Implemented here as
 * IFDSpecData, but potentially implemented for any sort of data object.
 * 
 * Allows for named representions. 
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public abstract class IFDDataObject<T extends IFDRepresentation> extends IFDRepresentableObject<T> {

	public IFDDataObject(String name, String type) throws IFDException {
		super(name, type);
	}
	
}
