package org.iupac.fairspec.spec;

import org.iupac.fairspec.api.IFSObjectI;
import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.sample.IFSSampleAnalysisCollection;

/**
 * A collection of IFSSpecAnalysis, which may be for completely different compounds.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings({ "serial" })
public class IFSSampleSpecAnalysisCollection extends IFSSampleAnalysisCollection  {

	public IFSSampleSpecAnalysisCollection(String name) throws IFSException {
		super(name, IFSObjectI.ObjectType.SampleSpecAnalysisCollection);
	}
	
	public void addAnalysis(IFSSampleSpecAnalysis a) {
		super.add(a);
	}

}