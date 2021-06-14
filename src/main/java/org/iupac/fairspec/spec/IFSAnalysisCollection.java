package org.iupac.fairspec.spec;

import org.iupac.fairspec.api.IFSObjectI;
import org.iupac.fairspec.object.IFSCollection;

@SuppressWarnings({ "serial" })
public class IFSAnalysisCollection extends IFSCollection<IFSAnalysis> {

	public IFSAnalysisCollection(String name) {
		super(name, IFSObjectI.ObjectType.AnalysisCollection);
	}

	public void addAnalysis(IFSAnalysis a) {
		super.add(a);
	}

}