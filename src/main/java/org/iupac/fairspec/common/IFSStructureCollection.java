package org.iupac.fairspec.common;

import org.iupac.fairspec.api.IFSApi;

@SuppressWarnings("serial")
public class IFSStructureCollection extends IFSCollection<IFSStructure> {

	public IFSStructureCollection(String name) {
		super(name, IFSApi.CollectionType.StructureCollection);
	}

}