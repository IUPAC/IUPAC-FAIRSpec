package org.iupac.fairspec.core;

import org.iupac.fairspec.common.IFSException;

@SuppressWarnings("serial")
public abstract class IFSAbstractObject<T> extends IFSObject<T> {

	public IFSAbstractObject(String name, ObjectType type) throws IFSException {
		super(name, type);
	}
	
	@SafeVarargs
	public IFSAbstractObject(String name, ObjectType type, int maxCount, T... initialSet) throws IFSException {
		super(name, type, maxCount, initialSet);
	}


}
