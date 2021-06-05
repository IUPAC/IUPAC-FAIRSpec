package org.iupac.fairspec.common;

import org.iupac.fairspec.api.IFSObjectApi;

@SuppressWarnings("serial")
public class IFSFindingAid extends IFSCollection<IFSCollection<?>> {

	public IFSFindingAid(String name) {
		super(name, IFSObjectApi.CollectionType.FindingAid);
	}

}