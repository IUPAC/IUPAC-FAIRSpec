package org.iupac.fairdata.sample;

import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDCollection;

/**
 * 
 * @author hansonr
 *
 */
@SuppressWarnings({ "serial" })
public abstract class IFDSampleAnalysisCollection extends IFDCollection<IFDSampleAnalysis> {

	public IFDSampleAnalysisCollection(String name, String type) throws IFDException {
		super(name, (type == null ? ObjectType.SampleAnalysisCollection : type));
	}

}