package org.iupac.fairdata.derived;

import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDAssociationCollection;
import org.iupac.fairdata.dataobject.IFDDataObject;
import org.iupac.fairdata.structure.IFDStructure;

@SuppressWarnings({ "serial" })
public class IFDStructureDataAssociationCollection extends IFDAssociationCollection {

	@Override
	public Class<?>[] getObjectTypes() {
		return new Class<?>[] { IFDStructureDataAssociation.class };
	}
	
	public IFDStructureDataAssociationCollection(String name, String type) {
		super(name, type);
	}

	public IFDStructureDataAssociationCollection(String name) {
		this(name, null);
	}

	public IFDStructureDataAssociation addAssociation(String name, IFDStructure struc, IFDDataObject data) throws IFDException {
		IFDStructureDataAssociation ssc = (IFDStructureDataAssociation) getAssociationForSingleObj1(struc);
		if (ssc == null) {
			add(ssc = newAssociation(name, struc, data));
		} else if (!ssc.getDataObjectCollection().contains(data)) {
			ssc.getDataObjectCollection().add(data);
		}
		return ssc;
	}

	protected IFDStructureDataAssociation newAssociation(String name, IFDStructure struc, IFDDataObject data) throws IFDException {
			return new IFDStructureDataAssociation(name, struc, data);
	}

}