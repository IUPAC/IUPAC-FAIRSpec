package org.iupac.fairdata.derived;

import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDAssociationCollection;
import org.iupac.fairdata.dataobject.IFDDataObject;
import org.iupac.fairdata.structure.IFDStructure;

@SuppressWarnings({ "serial" })
public class IFDStructureDataAssociationCollection extends IFDAssociationCollection {

	public IFDStructureDataAssociationCollection(boolean byID) {
		super(null, null, byID);
	}

	public IFDStructureDataAssociation addAssociation(IFDStructure struc, IFDDataObject data) throws IFDException {
		IFDStructureDataAssociation sda = (IFDStructureDataAssociation) getAssociationForSingleObj1(struc);
		if (sda == null) {
			add(sda = newAssociation(struc, data));
		} else if (!sda.getDataObjectCollection().contains(data)) {
			sda.getDataObjectCollection().add(data);
		}
		sda.setByID(byID);
		return sda;
	}
	
	public IFDStructureDataAssociation addAssociation(IFDStructureDataAssociation a) throws IFDException {
		add(a);
		a.setByID(byID);
		return a;
	}
	

	protected IFDStructureDataAssociation newAssociation(IFDStructure struc, IFDDataObject data) throws IFDException {
			return new IFDStructureDataAssociation(struc, data);
	}

}