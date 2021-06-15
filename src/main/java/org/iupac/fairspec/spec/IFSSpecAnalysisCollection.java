package org.iupac.fairspec.spec;

import org.iupac.fairspec.api.IFSObjectI;
import org.iupac.fairspec.object.IFSDataObjectCollection;

@SuppressWarnings({ "serial" })
public class IFSSpecAnalysisCollection extends IFSDataObjectCollection<IFSSpecAnalysis> {

	public IFSSpecAnalysisCollection(String name) {
		super(name, IFSObjectI.ObjectType.SpecAnalysisCollection);
	}

	public void addAnalysis(IFSSpecAnalysis a) {
		super.add(a);
	}

}