package org.iupac.fairspec.spec;

import org.iupac.fairspec.api.IFSObjectAPI;
import org.iupac.fairspec.common.IFSCollection;

@SuppressWarnings({ "serial" })
public class IFSAnalysisCollection extends IFSCollection<IFSAnalysis> {

	public IFSAnalysisCollection(String name) {
		super(name, IFSObjectAPI.ObjectType.AnalysisCollection);
	}

	public void addAnalysis(IFSAnalysis a) {
		super.add(a);
	}

}