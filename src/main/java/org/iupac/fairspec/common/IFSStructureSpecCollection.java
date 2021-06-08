package org.iupac.fairspec.common;

import org.iupac.fairspec.api.IFSObjectAPI;

@SuppressWarnings({ "serial" })
public class IFSStructureSpecCollection extends IFSCollection<IFSStructureSpec> {

	public IFSStructureSpecCollection(String name) {
		super(name, IFSObjectAPI.ObjectType.StructureSpecCollection);
	}

	public void addPair(IFSStructure struc, IFSSpecData spec) {
		if (get(struc, spec) == null) {
			add(new IFSStructureSpec(struc, spec));
		}
	}

	private Object get(IFSStructure struc, IFSSpecData specData) {
		IFSStructureSpec ss;
		for (int i = size(); --i >= 0;)
			if ((ss = this.get(i)).getStructure() == struc && ss.getSpecData() == specData)
				return ss; 
		return null;
	}
	
}