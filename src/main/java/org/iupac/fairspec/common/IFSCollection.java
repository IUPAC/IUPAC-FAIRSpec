package org.iupac.fairspec.common;

import org.iupac.fairspec.api.IFSApi;

@SuppressWarnings("serial")
public abstract class IFSCollection<T> extends IFSObject<T> {

	private CollectionType type;

	public IFSCollection(String name, CollectionType type) {
		this.name = name;
		this.type = type;
	}

	@Override
	public ObjectType getObjectType() {
		return IFSApi.ObjectType.Collection;
	}

	public CollectionType getCollectionType() {
		return type;
	}

}