package org.iupac.fairspec.common;

import java.util.HashMap;
import java.util.Map;

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
public abstract class IFSDataObject<T> extends IFSObject<T> {

	protected final Map<String, IFSRepresentation> htReps = new HashMap<>();
	
	public IFSDataObject(String name, ObjectType type) {
		super(name, type);
	}
	


}
