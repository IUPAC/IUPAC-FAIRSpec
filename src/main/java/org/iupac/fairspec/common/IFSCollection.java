package org.iupac.fairspec.common;

import org.iupac.fairspec.spec.IFSStructureSpec;

@SuppressWarnings("serial")
public abstract class IFSCollection<T> extends IFSObject<T> {

	protected IFSCollection(String name, ObjectType type) {
		super(name, type);
	}

	@Override
	public T getRepresentation(String objectName) {
		return null;
	}

	@Override
	protected T newRepresentation(String objectName, IFSReference ifsReference, Object object, long len) throws IFSException {
		throw new IFSException("IFSCollection is an abstract object; representations are not allowed");
	}






}