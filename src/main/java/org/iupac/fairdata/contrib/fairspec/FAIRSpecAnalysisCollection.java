package org.iupac.fairdata.contrib.fairspec;

import org.iupac.fairdata.analysisobject.IFDAnalysisObject;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.dataobject.IFDDataObject;
import org.iupac.fairdata.derived.IFDStructureDataAnalysisAssociationCollection;
import org.iupac.fairdata.derived.IFDStructureDataAssociationCollection;
import org.iupac.fairdata.structure.IFDStructure;

/**
 * A class that identifies this as an IFDStructureDataAnalysisAssociationCollection
 * specifically with elements of type FAIRSpecAnalysisAssociation.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class FAIRSpecAnalysisCollection extends IFDStructureDataAnalysisAssociationCollection {

	protected FAIRSpecAnalysisCollection(boolean byID) {
		super(byID);
	}

	@Override
	public FAIRSpecAnalysisAssociation addAssociation(IFDStructure struc, IFDDataObject data, IFDAnalysisObject analysis) throws IFDException {
		FAIRSpecAnalysisAssociation ca = newAssociation(struc, data, analysis);
			add(ca);
		ca.setByID(byID);
		return ca;
	}

	@Override
	protected FAIRSpecAnalysisAssociation newAssociation(IFDStructure struc, IFDDataObject data, IFDAnalysisObject analysis) throws IFDException {
		FAIRSpecAnalysisAssociation ca = new FAIRSpecAnalysisAssociation();
		if (struc != null)
			ca.addStructure(struc);
		if (data != null)
			ca.addDataObject(data);
		if (analysis != null)
			ca.addAnalysisObject(analysis);
		return ca;
	}

}
