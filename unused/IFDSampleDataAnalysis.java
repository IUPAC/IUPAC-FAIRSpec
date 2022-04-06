package org.iupac.fairdata.derived;

import org.iupac.fairdata.analysis.IFDAnalysisObjectCollection;
import org.iupac.fairdata.api.IFDAnalysisI;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDAssociation;
import org.iupac.fairdata.core.IFDCollection;
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
public class IFDSampleDataAnalysis  extends IFDAssociation implements IFDAnalysisI {
	
	@SuppressWarnings("unchecked")
	public IFDSampleDataAnalysis(String name, String type, IFDSampleCollection sampleCollection,
			IFDDataObjectCollection dataCollection, IFDAnalysisObjectCollection aoCollection) throws IFDException {
		super(name, type, new IFDCollection[] { sampleCollection, dataCollection, aoCollection });
	}

}
