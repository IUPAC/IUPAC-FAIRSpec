package org.iupac.fairspec.common;

@SuppressWarnings("serial")
public abstract class IFSCollection<T> extends IFSObject<T> {

	private ObjectType type;

	protected IFSCollection(String name, ObjectType type) {
		// only called by super()
		this.name = name;
		this.type = type;
	}

	@Override
	public ObjectType getObjectType() {
		return type;
	}

}