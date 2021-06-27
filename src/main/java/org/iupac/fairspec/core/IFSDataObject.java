package org.iupac.fairspec.core;

import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.common.IFSRepresentation;

/**
 * A generic interface indicating some sort of data. Implemented here as
 * IFSSpecData, but potentially implemented for any sort of data object.
 * 
 * Allows for named representions. 
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public abstract class IFSDataObject<T extends IFSRepresentation> extends IFSRepresentableObject<T> {

	public IFSDataObject(String name, String type) throws IFSException {
		super(name, type);
	}
	
}
