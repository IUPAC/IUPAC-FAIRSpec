package org.iupac.fairspec.core;

@SuppressWarnings("serial")
public abstract class IFSDataObjectCollection<T> extends IFSCollection<T> {

	protected IFSDataObjectCollection(String name, ObjectType type) {
		super(name, type);
	}


}
