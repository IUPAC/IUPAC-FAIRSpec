package org.iupac.fairspec.common;

import org.iupac.fairspec.api.IFSObjectApi;

@SuppressWarnings("serial")
public class IFSStructureCollection extends IFSCollection<IFSStructure> {

	public IFSStructureCollection(String name) {
		super(name, IFSObjectApi.CollectionType.StructureCollection);
	}
	
	public void addStructure(IFSStructure s) {
		super.add(s);
	}

}