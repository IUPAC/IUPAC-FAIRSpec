package org.iupac.fairdata.contrib.fairspec;

import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.dataobject.IFDDataObject;
import org.iupac.fairdata.derived.IFDStructureDataAssociationCollection;
import org.iupac.fairdata.structure.IFDStructure;

/**
 * A class that identifies this as an IFDStructureDataAssociationCollection
 * specifically with elements of type FAIRSpecCompoundAssociation.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class FAIRSpecCompoundCollection extends IFDStructureDataAssociationCollection {

	protected FAIRSpecCompoundCollection(boolean byID) {
		super(byID);
	}

	@Override
	public FAIRSpecCompoundAssociation addAssociation(IFDStructure struc, IFDDataObject data) throws IFDException {
		FAIRSpecCompoundAssociation ca = (FAIRSpecCompoundAssociation) getAssociationForSingleObj1(struc);
		if (ca == null) {
			add(ca = newAssociation(struc, data));
		} else if (!ca.getDataObjectCollection().contains(data)) {
			ca.addDataObject(data);
		}
		ca.setByID(byID);
		return ca;
	}

	@Override
	protected FAIRSpecCompoundAssociation newAssociation(IFDStructure struc, IFDDataObject data) throws IFDException {
		FAIRSpecCompoundAssociation ca = new FAIRSpecCompoundAssociation();
		ca.addStructure(struc);
		ca.addDataObject(data);
		return ca;
	}

}
