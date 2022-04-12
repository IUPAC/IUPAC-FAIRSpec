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

	public IFDSampleStructureAssociationCollection() {
		super(null, null);
	}

	public IFDSampleStructureAssociation addAssociation(IFDSample sample, IFDStructure structure) throws IFDException {
		IFDSampleStructureAssociation ssc = (IFDSampleStructureAssociation) getAssociationForSingleObj1(sample);
		if (ssc == null) {
			add(ssc = newAssociation(sample, structure));
		} else if (!ssc.getStructureCollection().contains(structure)) {
			ssc.getStructureCollection().add(structure);
		}
		return ssc;
	}

	protected IFDSampleStructureAssociation newAssociation(IFDSample sample, IFDStructure structure) throws IFDException {
			return new IFDSampleStructureAssociation(null, sample, structure);
	}

}