package org.iupac.fairspec.spec;

import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.common.IFSRepresentation;
import org.iupac.fairspec.core.IFSDataObjectCollection;
import org.iupac.fairspec.sample.IFSSampleAnalysis;
import org.iupac.fairspec.sample.IFSSampleCollection;

/**
 * A subclass of IFSAnalysis that provides a correlation between an IFSSample and its associated spectroscopic data.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class IFSSampleSpecAnalysis extends IFSSampleAnalysis {

	public IFSSampleSpecAnalysis(String name, IFSSampleCollection sampleCollection,
			IFSDataObjectCollection<?> dataCollection) throws IFSException {
		super(name, ObjectType.SampleSpecAnalysis, sampleCollection, dataCollection);
	}

	@Override
	protected IFSRepresentation newRepresentation(String objectName, IFSReference ifsReference, Object object, long len,
			String type, String subtype) {
		return new IFSSampleSpecAnalysisRepresentation(ifsReference, object, len, type, subtype);
	}

}
