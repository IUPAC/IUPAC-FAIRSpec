package org.iupac.fairdata.derived;

import org.iupac.fairdata.core.IFDCollection;

/**
 * just a convenience
 * 
 * @author hansonr
 *
 */
@SuppressWarnings({ "serial" })
public class IFDSampleDataAnalysisCollection extends IFDCollection<IFDSampleDataAnalysis> {

	public IFDSampleDataAnalysisCollection(String name) {
		this(name, null);
	}

	public IFDSampleDataAnalysisCollection(String name, String type) {
		super(name, type);
	}

	@Override
	public Class<?>[] getObjectTypes() {
		return new Class<?>[] { IFDSampleDataAnalysis.class };
	}

}