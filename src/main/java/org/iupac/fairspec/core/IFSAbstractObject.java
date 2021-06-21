package org.iupac.fairspec.core;

import org.iupac.fairspec.api.IFSObjectI.ObjectType;

@SuppressWarnings("serial")
public class IFSAbstractObject<T> extends IFSObject<T> {

	public IFSAbstractObject(String name, ObjectType type) {
		super(name, type);
	}
	
	public IFSAbstractObject(String name, ObjectType type, int maxCount, T... initialSet) {
		super(name, type, maxCount, initialSet);
	}


}
