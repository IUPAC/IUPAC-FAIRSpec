package org.iupac.fairspec.spec;

import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.core.IFSDataObjectCollection;
import org.iupac.fairspec.struc.IFSStructureAnalysis;
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
		super(name, ObjectType.SpecAnalysis, structureCollection, dataCollection);
	}

}
