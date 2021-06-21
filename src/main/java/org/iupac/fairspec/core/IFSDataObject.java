package org.iupac.fairspec.core;

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
public abstract class IFSDataObject<T> extends IFSRepresentableObject<T> {

	public IFSDataObject(String name, ObjectType type) {
		super(name, type);
	}
	
}
