package org.iupac.fairdata.spec;

import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.common.IFDReference;
import org.iupac.fairdata.common.IFDRepresentation;
import org.iupac.fairdata.core.IFDDataObjectCollection;
import org.iupac.fairdata.sample.IFDSampleAnalysis;
import org.iupac.fairdata.sample.IFDSampleCollection;

/**
 * A subclass of IFDAnalysis that provides a correlation between an IFDSample and its associated spectroscopic data.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class IFDSampleSpecAnalysis extends IFDSampleAnalysis {

	public IFDSampleSpecAnalysis(String name, IFDSampleCollection sampleCollection,
			IFDDataObjectCollection<?> dataCollection) throws IFDException {
		super(name, IFDSpecDataFindingAid.SpecType.SampleSpecAnalysis, sampleCollection, dataCollection);
	}

	@Override
	protected IFDRepresentation newRepresentation(String objectName, IFDReference ifdReference, Object object, long len,
			String type, String subtype) {
		return new IFDSampleSpecAnalysisRepresentation(ifdReference, object, len, type, subtype);
	}


}
