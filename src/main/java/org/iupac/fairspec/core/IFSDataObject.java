package org.iupac.fairspec.core;

import org.iupac.fairspec.api.IFSRepresentableObjectI;

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
public abstract class IFSDataObject<T> extends IFSObject<T> implements IFSRepresentableObjectI {

	public IFSDataObject(String name, ObjectType type) {
		super(name, type);
	}
	
}
