package org.iupac.fairspec.spec;

import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.struc.IFSStructureAnalysisCollection;

/**
 * A collection of IFSSpecAnalysis, which may be for completely different compounds.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings({ "serial" })
public class IFSStructureSpecAnalysisCollection extends IFSStructureAnalysisCollection  {

	public IFSStructureSpecAnalysisCollection(String name) throws IFSException {
		super(name, ObjectType.SpecAnalysisCollection);
	}

}