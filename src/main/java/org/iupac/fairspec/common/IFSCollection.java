package org.iupac.fairspec.common;

@SuppressWarnings("serial")
public abstract class IFSCollection<T> extends IFSObject<T> {

	private ObjectType type;

	protected IFSCollection(String name, ObjectType type) {
		super(name);
		this.type = type;
	}

	@Override
	public ObjectType getObjectType() {
		return type;
	}
	
	@Override
	public T getRepresentation(String objectName) {
		// n/a
		return null;
	}

	@Override
	protected T newRepresentation(String objectName, IFSReference ifsReference, Object object) {
		// n/a
		return null;
	}






}