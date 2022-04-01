package org.iupac.fairdata.spec;

import org.iupac.fairdata.analysis.IFDSampleDataAnalysisCollection;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.sample.IFDSample;

/**
 * A collection of IFDSpecAnalysis, which may be for completely different compounds.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings({ "serial" })
public class IFDSampleSpecAnalysisCollection extends IFDSampleDataAnalysisCollection  {

	public IFDSampleSpecAnalysisCollection(String name) throws IFDException {
		super(name, IFDSpecDataFindingAid.SpecType.SampleSpecAnalysisCollection);
	}
	
	public void addAnalysis(IFDSampleSpecAnalysis a) {
		super.add(a);
	}

	@Override
	public Class<?>[] getObjectTypes() {
		return new Class<?>[] { IFDSample.class, IFDSpecData.class };
	}

}