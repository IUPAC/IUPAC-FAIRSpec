package org.iupac.fairspec.spec;

import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.common.IFSRepresentation;
import org.iupac.fairspec.core.IFSDataObjectCollection;
import org.iupac.fairspec.struc.IFSStructureAnalysis;
import org.iupac.fairspec.struc.IFSStructureAnalysisRepresentation;
import org.iupac.fairspec.struc.IFSStructureCollection;

/**
 * A subclass of IFSAnalysis that provides a detailed atom-based analysis of
 * chemical structure in relation to spectroscopy.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class IFSStructureSpecAnalysis extends IFSStructureAnalysis {

	public IFSStructureSpecAnalysis(String name, IFSStructureCollection structureCollection,
			IFSDataObjectCollection<?> dataCollection) throws IFSException {
		super(name, IFSSpecDataFindingAid.SpecType.SpecAnalysis, structureCollection, dataCollection);
	}
	
	@Override
	protected IFSRepresentation newRepresentation(String objectName, IFSReference ifsReference, Object object, long len,
			String type, String subtype) {
		return new IFSStructureSpecAnalysisRepresentation(ifsReference, object, len, type, subtype);
	}

	

}
