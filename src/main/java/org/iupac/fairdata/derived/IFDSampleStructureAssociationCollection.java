package org.iupac.fairdata.derived;

import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDAssociationCollection;
import org.iupac.fairdata.sample.IFDSample;
import org.iupac.fairdata.structure.IFDStructure;

/**
 * Just one IFDSample collection and one IFDDataObject collection.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings({ "serial" })
public class IFDSampleStructureAssociationCollection extends IFDAssociationCollection {

	public IFDSampleStructureAssociationCollection(boolean byID) {
		super(null, null, byID);
	}

	public IFDSampleStructureAssociation addAssociation(IFDSample sample, IFDStructure structure) throws IFDException {
		IFDSampleStructureAssociation ssa = (IFDSampleStructureAssociation) getAssociationForSingleObj1(sample);
		if (ssa == null) {
			add(ssa = newAssociation(sample, structure));
		} else if (!ssa.getStructureCollection().contains(structure)) {
			ssa.getStructureCollection().add(structure);
		}
		ssa.setByID(byID);
		return ssa;
	}

	protected IFDSampleStructureAssociation newAssociation(IFDSample sample, IFDStructure structure) throws IFDException {
			return new IFDSampleStructureAssociation(null, sample, structure);
	}

	@Override
	protected String getDefaultName(int i) {
		return (i == 0 ? "samples" : "structures");
	}


}