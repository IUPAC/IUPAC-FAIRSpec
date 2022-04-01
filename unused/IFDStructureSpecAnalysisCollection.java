package org.iupac.fairdata.spec;

import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.struc.IFDStructureAnalysisCollection;

/**
 * A collection of IFDSpecAnalysis, which may be for completely different compounds.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings({ "serial" })
public class IFDStructureSpecAnalysisCollection extends IFDStructureAnalysisCollection  {

	public IFDStructureSpecAnalysisCollection(String name) throws IFDException {
		super(name, IFDSpecDataFindingAid.SpecType.SpecAnalysisCollection);
	}

}