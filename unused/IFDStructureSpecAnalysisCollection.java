package org.iupac.fairdata.spec;

import org.iupac.fairdata.analysis.IFDStructureDataAnalysisCollection;
import org.iupac.fairdata.common.IFDException;

/**
 * A collection of IFDSpecAnalysis, which may be for completely different compounds.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings({ "serial" })
public class IFDStructureDataAnalysisCollection extends IFDStructureDataAnalysisCollection  {

	public IFDStructureDataAnalysisCollection(String name) throws IFDException {
		super(name, IFDSpecDataFindingAid.SpecType.SpecAnalysisCollection);
	}

	@Override
	public Class<?>[] getCollectionTypes() {
		return new Class<?>[] {IFDStructureDataAssociation.class, IFDSpecDataAnalysisCollection.class};
	}

}