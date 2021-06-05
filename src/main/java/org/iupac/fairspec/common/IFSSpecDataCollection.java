package org.iupac.fairspec.common;

import org.iupac.fairspec.api.IFSObjectApi;

@SuppressWarnings("serial")
public class IFSSpecDataCollection extends IFSCollection<IFSSpecData> {

	public IFSSpecDataCollection(String name) {
		super(name, IFSObjectApi.CollectionType.SpecDataCollection);
	}

	
	public void addSpecData(IFSSpecData sd) {
		super.add(sd);
	}

}