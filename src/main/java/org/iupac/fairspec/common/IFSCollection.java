package org.iupac.fairspec.common;

import org.iupac.fairspec.api.IFSObjectApi;

@SuppressWarnings("serial")
public abstract class IFSCollection<T> extends IFSObject<T> {

	private CollectionType type;

	public IFSCollection(String name, CollectionType type) {
		this.name = name;
		this.type = type;
	}

	@Override
	public ObjectType getObjectType() {
		return IFSObjectApi.ObjectType.Collection;
	}

	public CollectionType getCollectionType() {
		return type;
	}

}