package org.iupac.fairdata.todo;

import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDCollection;

/**
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