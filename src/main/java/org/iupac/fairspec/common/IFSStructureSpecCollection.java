package org.iupac.fairspec.common;

import org.iupac.fairspec.api.IFSObjectAPI;

@SuppressWarnings({ "serial" })
public class IFSStructureSpecCollection extends IFSCollection<IFSStructureSpec> {

	public IFSStructureSpecCollection(String name) {
		super(name, IFSObjectAPI.ObjectType.StructureSpecCollection);
	}

}