package org.iupac.fairspec.common;

import org.iupac.fairspec.api.IFSObjectAPI;

@SuppressWarnings({ "serial" })
public class IFSAnalysisCollection extends IFSCollection<IFSAnalysis> {

	public IFSAnalysisCollection(String name) {
		super(name, IFSObjectAPI.ObjectType.AnalysisCollection);
	}

	public void addAnalysis(IFSAnalysis a) {
		super.add(a);
	}

}