package org.iupac.fairdata.derived;

import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDAssociationCollection;
import org.iupac.fairdata.sample.IFDSample;
import org.iupac.fairdata.sample.IFDSampleCollection;
import org.iupac.fairdata.structure.IFDStructure;
import org.iupac.fairdata.structure.IFDStructureCollection;

/**
 * Just one IFDSample collection and one IFDDataObject collection.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings({ "serial" })
public class IFDSampleStructureAssociationCollection extends IFDAssociationCollection {

	@Override
	public Class<?>[] getObjectTypes() {
		return new Class<?>[] { IFDSampleCollection.class, IFDStructureCollection.class };
	}

	public IFDSampleStructureAssociationCollection(String name, String type) {
		super(name, type);
	}

	public IFDSampleStructureAssociationCollection(String name) {
		this(name, null);
	}

	public IFDSampleStructureAssociation addAssociation(String name, IFDSample sample, IFDStructure structure) throws IFDException {
		IFDSampleStructureAssociation ssc = (IFDSampleStructureAssociation) getAssociationForSingleObj1(sample);
		if (ssc == null) {
			add(ssc = newAssociation(name, sample, structure));
		} else if (!ssc.getStructureCollection().contains(structure)) {
			ssc.getStructureCollection().add(structure);
		}
		return ssc;
	}

	protected IFDSampleStructureAssociation newAssociation(String name, IFDSample sample, IFDStructure structure) throws IFDException {
			return new IFDSampleStructureAssociation(name, sample, structure);
	}

}