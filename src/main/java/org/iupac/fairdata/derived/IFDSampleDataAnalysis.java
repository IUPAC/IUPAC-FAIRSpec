package org.iupac.fairdata.todo;

import org.iupac.fairdata.analysis.IFDAnalysisObject;
import org.iupac.fairdata.api.IFDAnalysisI;
import org.iupac.fairdata.api.IFDSerializerI;
import org.iupac.fairdata.todo.IFDSampleDataAssociation;
import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.common.IFDReference;
import org.iupac.fairdata.core.IFDRepresentableObject;
import org.iupac.fairdata.core.IFDRepresentation;
import org.iupac.fairdata.dataobject.IFDDataObjectCollection;
import org.iupac.fairdata.sample.IFDSampleCollection;

/**
 * An IFDSampleDataAssociation specifcally for sample-data (not structure-data) analysis. 
 * 
 * See IFDAnalysis for details.
 * 
 * Just a placeholder
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class IFDSampleDataAnalysis extends IFDSampleDataAssociation implements IFDAnalysisI {
	
	IFDAnalysisObject analysis; // TODO
	
	public IFDSampleDataAnalysis(String name, String type, IFDSampleCollection sampleCollection,
			IFDDataObjectCollection dataCollection) throws IFDException {
		super(name, type, sampleCollection, dataCollection);
		if (sampleCollection == null || dataCollection == null)
			throw new IFDException("IFDDAnalysis sampleCollection and dataCollection must be non-null.");
	}

	@Override
	protected void serializeList(IFDSerializerI serializer) {
		super.serializeList(serializer);
		if (analysis != null)
			analysis.serialize(serializer);
	}
	
}
