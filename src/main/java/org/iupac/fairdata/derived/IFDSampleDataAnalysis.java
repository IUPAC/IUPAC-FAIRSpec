package org.iupac.fairdata.derived;

import org.iupac.fairdata.analysis.IFDAnalysisObject;
import org.iupac.fairdata.api.IFDAnalysisI;
import org.iupac.fairdata.api.IFDSerializerI;
import org.iupac.fairdata.derived.IFDSampleDataAssociation;
import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.common.IFDReference;
import org.iupac.fairdata.core.IFDAnalysis;
import org.iupac.fairdata.core.IFDRepresentableObject;
import org.iupac.fairdata.core.IFDRepresentation;
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
			IFDDataObjectCollection dataCollection, IFDAnalysisObject analysis) throws IFDException {
		super(name, type, sampleCollection, dataCollection, analysis);
	}

}
