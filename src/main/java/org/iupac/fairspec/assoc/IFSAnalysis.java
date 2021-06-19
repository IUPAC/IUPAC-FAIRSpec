package org.iupac.fairspec.assoc;

import org.iupac.fairspec.data.IFSDataObjectCollection;
import org.iupac.fairspec.struc.IFSStructureCollection;

/**
 * An IFSAnalysis is a specialized IFSStructureDataAssociation that provides more
 * detailed metadata correlating one or more structures and one or more spectra.
 * 
 * Typically, only one structure will be involved, but the class allows for any
 * number of structures (such as in the case of a chemical mixture).
 * 
 * There can be as many spectra as are relevant to an analysis. For example, the
 * analysis can be just one structure and a 1H NMR spectrum. Or it can be a
 * compound along with its associated 1H, 13C, DEPT, HSQC, and HMBC spectra.
 * 
 * Unlike its superclass, IFSAnalysis is expected to describe in detail the
 * correlation between structure and spectra -- using specific structure
 * representations that map atom numbers to spectral signals or sets of signals.
 * 
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public abstract class IFSAnalysis extends IFSStructureDataAssociation {

	public IFSAnalysis(String name, ObjectType type, IFSStructureCollection structureCollection,
			IFSDataObjectCollection<?> dataCollection) {
		super(name, type, structureCollection, dataCollection);
	}

	

}
