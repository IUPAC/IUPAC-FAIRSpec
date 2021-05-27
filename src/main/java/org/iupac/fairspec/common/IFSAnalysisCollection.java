package org.iupac.fairspec.common;

import org.iupac.fairspec.api.IFSApi;

@SuppressWarnings({ "serial" })
public class IFSAnalysisCollection extends IFSCollection<IFSAnalysis> {

	public IFSAnalysisCollection(String name) {
		super(name, IFSApi.CollectionType.AnalysisCollection);
	}

}