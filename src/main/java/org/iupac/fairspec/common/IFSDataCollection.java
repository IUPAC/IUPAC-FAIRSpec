package org.iupac.fairspec.common;

import org.iupac.fairspec.api.IFSApi;

@SuppressWarnings("serial")
public class IFSDataCollection extends IFSCollection<IFSSpecData> {

	public IFSDataCollection(String name) {
		super(name, IFSApi.CollectionType.SpecDataCollection);
	}

}