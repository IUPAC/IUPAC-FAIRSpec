package org.iupac.fairdata.derived;

import org.iupac.fairdata.analysisobject.IFDAnalysisObject;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDAssociationCollection;
import org.iupac.fairdata.dataobject.IFDDataObject;
import org.iupac.fairdata.structure.IFDStructure;

/**
 * 
 * @author hansonr
 *
 */
@SuppressWarnings({ "serial" })
public class IFDStructureDataAnalysisAssociationCollection extends IFDAssociationCollection {

	public IFDStructureDataAnalysisAssociationCollection() {
		super(null, null);
	}

	public void addAnalysis(IFDStructureDataAnalysisAssociation a) {
		super.add(a);
	}

	public IFDStructureDataAnalysisAssociation addAssociation(IFDStructure struc, IFDDataObject data, IFDAnalysisObject analysis) throws IFDException {
		IFDStructureDataAnalysisAssociation sdaa = newAssociation(struc, data, analysis);
		return sdaa;
	}
	
	public IFDStructureDataAnalysisAssociation addAssociation(IFDStructureDataAnalysisAssociation a) throws IFDException {
		add(a);
		return a;
	}
	

	protected IFDStructureDataAnalysisAssociation newAssociation(IFDStructure struc, IFDDataObject data, IFDAnalysisObject analysis) throws IFDException {
			return new IFDStructureDataAnalysisAssociation(struc, data, analysis);
	}
	
	@Override
	protected String getDefaultName(int i) {
		return IFDStructureDataAnalysisAssociation.getItemName(i);
	}
	
}