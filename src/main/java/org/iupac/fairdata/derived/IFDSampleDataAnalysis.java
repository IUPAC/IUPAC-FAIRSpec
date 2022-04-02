package org.iupac.fairdata.derived;

import org.iupac.fairdata.analysis.IFDAnalysisObjectCollection;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDAnalysis;
import org.iupac.fairdata.dataobject.IFDDataObjectCollection;
import org.iupac.fairdata.sample.IFDSampleCollection;

/**
 * An IFDSampleDataAssociation specifically for sample-data (not structure-data) analysis. 
 * 
 * See IFDAnalysis for details.
 * 
 * Just a convenience
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class IFDSampleDataAnalysis extends IFDAnalysis {
	
	public IFDSampleDataAnalysis(String name, String type, IFDSampleCollection sampleCollection,
			IFDDataObjectCollection dataCollection, IFDAnalysisObjectCollection aoCollection) throws IFDException {
		super(name, type, sampleCollection, dataCollection, aoCollection);
	}

}
