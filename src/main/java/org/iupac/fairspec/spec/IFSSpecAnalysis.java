package org.iupac.fairspec.spec;

import org.iupac.fairspec.assoc.IFSAnalysis;
import org.iupac.fairspec.data.IFSDataObjectCollection;
import org.iupac.fairspec.struc.IFSStructureCollection;

/**
 * A subclass of IFSAnalysis that provides a detailed atom-based analysis of
 * chemical structure in relation to spectroscopy.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class IFSSpecAnalysis extends IFSAnalysis {

	public IFSSpecAnalysis(String name, IFSStructureCollection structureCollection,
			IFSDataObjectCollection<?> dataCollection) {
		super(name, ObjectType.SpecAnalysis, structureCollection, dataCollection);
	}

}
