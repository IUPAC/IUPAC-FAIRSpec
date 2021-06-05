package org.iupac.fairspec.common;

import org.iupac.fairspec.api.IFSObjectApi;

@SuppressWarnings({ "serial" })
public class IFSAnalysisCollection extends IFSCollection<IFSAnalysis> {

	public IFSAnalysisCollection(String name) {
		super(name, IFSObjectApi.CollectionType.AnalysisCollection);
	}

	public void addAnalysis(IFSAnalysis a) {
		super.add(a);
	}

}