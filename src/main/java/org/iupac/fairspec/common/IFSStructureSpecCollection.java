package org.iupac.fairspec.common;

import org.iupac.fairspec.api.IFSObjectApi;

@SuppressWarnings({ "serial" })
public class IFSStructureSpecCollection extends IFSCollection<IFSStructureSpec> {

	public IFSStructureSpecCollection(String name) {
		super(name, IFSObjectApi.CollectionType.StructureSpecCollection);
	}

}