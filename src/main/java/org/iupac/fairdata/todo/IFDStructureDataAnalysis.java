package org.iupac.fairdata.todo;

import org.iupac.fairdata.analysis.IFDAnalysisObject;
import org.iupac.fairdata.api.IFDAnalysisI;
import org.iupac.fairdata.api.IFDSerializerI;
import org.iupac.fairdata.todo.IFDStructureDataAssociation;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.dataobject.IFDDataObjectCollection;
import org.iupac.fairdata.structure.IFDStructureCollection;

/**
 * An IFDStructureDataAnalysis is a specialized IFDStructureDataAssociation that provides more
 * detailed metadata correlating one or more IFDStructure and one or more IFDDataObject because
 * it also maintains an IFDRepresentableObject.
 * 
 * Typically, only one structure will be involved, but the class allows for any
 * number of structures (such as in the case of a chemical mixture).
 * 
 * There can be as many spectra as are relevant to an analysis. For example, the
 * analysis can be just one structure and a 1H NMR spectrum. Or it can be a
 * compound along with its associated 1H, 13C, DEPT, HSQC, and HMBC spectra.
 * 
 * Unlike its superclass, IFDStructureDataAnalysis is expected to describe in detail the
 * correlation between structure and spectra -- using specific structure
 * representations that map atom numbers to spectral signals or sets of signals.
 * 
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class IFDStructureDataAnalysis extends IFDStructureDataAssociation implements IFDAnalysisI {
	
	IFDAnalysisObject analysis;
	
	public IFDStructureDataAnalysis(String name, String type, IFDStructureCollection sampleCollection,
			IFDDataObjectCollection dataCollection) throws IFDException {
		super(name, null, sampleCollection, dataCollection);
		if (sampleCollection == null || dataCollection == null)
			throw new IFDException("IFDStructureAnalysis sampleCollection and dataCollection must be non-null.");
	}

	@Override
	protected void serializeList(IFDSerializerI serializer) {
		super.serializeList(serializer);
		if (analysis != null)
			analysis.serialize(serializer);
	}

}
