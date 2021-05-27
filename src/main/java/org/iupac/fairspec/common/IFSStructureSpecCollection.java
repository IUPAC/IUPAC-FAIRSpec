package org.iupac.fairspec.common;

import org.iupac.fairspec.api.IFSApi;

@SuppressWarnings({ "serial" })
public class IFSStructureSpecCollection extends IFSCollection<IFSStructureSpec> {

	public IFSStructureSpecCollection(String name) {
		super(name, IFSApi.CollectionType.StructureSpecCollection);
	}

}