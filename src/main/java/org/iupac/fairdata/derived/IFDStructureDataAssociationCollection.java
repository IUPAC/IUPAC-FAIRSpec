package org.iupac.fairdata.derived;

import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDAssociationCollection;
import org.iupac.fairdata.dataobject.IFDDataObject;
import org.iupac.fairdata.structure.IFDStructure;

@SuppressWarnings({ "serial" })
public class IFDStructureDataAssociationCollection extends IFDAssociationCollection {

	public IFDStructureDataAssociationCollection() {
		super(null, null);
	}

	public IFDStructureDataAssociation addAssociation(IFDStructure struc, IFDDataObject data) throws IFDException {
		IFDStructureDataAssociation sda = (IFDStructureDataAssociation) getAssociationForSingleObj1(struc);
		if (sda == null) {
			add(sda = newAssociation(struc, data));
		} else if (!sda.getDataObjectCollection().contains(data)) {
			sda.addDataObject(data);
		}
		return sda;
	}
	
	public IFDStructureDataAssociation addAssociation(IFDStructureDataAssociation a) throws IFDException {
		add(a);
		return a;
	}
	

	protected IFDStructureDataAssociation newAssociation(IFDStructure struc, IFDDataObject data) throws IFDException {
			return new IFDStructureDataAssociation(struc, data);
	}

	
	@Override
	protected String getDefaultName(int i) {
		return IFDStructureDataAssociation.getItemName(i);
	}

}