package org.iupac.fairspec.common;

import org.iupac.fairspec.api.IFSApi;

@SuppressWarnings("serial")
public class IFSFindingAid extends IFSCollection<IFSCollection<?>> {

	public IFSFindingAid(String name) {
		super(name, IFSApi.CollectionType.FindingAid);
	}

}