package org.iupac.fairspec.common;

import org.iupac.fairspec.api.IFSObjectAPI;

@SuppressWarnings("serial")
public class IFSStructureCollection extends IFSCollection<IFSStructure> {

	public IFSStructureCollection(String name) {
		super(name, IFSObjectAPI.ObjectType.StructureCollection);
	}
	
	public void addStructure(IFSStructure s) {
		super.add(s);
	}

}