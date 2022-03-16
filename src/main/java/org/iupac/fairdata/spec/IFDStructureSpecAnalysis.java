package org.iupac.fairdata.spec;

import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.common.IFDReference;
import org.iupac.fairdata.common.IFDRepresentation;
import org.iupac.fairdata.core.IFDDataObjectCollection;
import org.iupac.fairdata.struc.IFDStructureAnalysis;
import org.iupac.fairdata.struc.IFDStructureCollection;

/**
 * A subclass of IFDAnalysis that provides a detailed atom-based analysis of
 * chemical structure in relation to spectroscopy.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class IFDStructureSpecAnalysis extends IFDStructureAnalysis {

	public IFDStructureSpecAnalysis(String name, IFDStructureCollection structureCollection,
			IFDDataObjectCollection<?> dataCollection) throws IFDException {
		super(name, IFDSpecDataFindingAid.SpecType.SpecAnalysis, structureCollection, dataCollection);
	}
	
	@Override
	protected IFDRepresentation newRepresentation(String objectName, IFDReference ifdReference, Object object, long len,
			String type, String subtype) {
		return new IFDStructureSpecAnalysisRepresentation(ifdReference, object, len, type, subtype);
	}

	

}
