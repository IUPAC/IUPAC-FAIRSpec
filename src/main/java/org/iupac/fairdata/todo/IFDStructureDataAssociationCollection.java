package org.iupac.fairdata.todo;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.core.IFDAssociationCollection;
import org.iupac.fairdata.dataobject.IFDDataObject;
import org.iupac.fairdata.structure.IFDStructure;

@SuppressWarnings({ "serial" })
public class IFDStructureDataAssociationCollection extends IFDAssociationCollection {

	@Override
	public Class<?>[] getObjectTypes() {
		return new Class<?>[] { IFDStructureDataAssociation.class };
	}
	

	public IFDStructureDataAssociationCollection(String name) {
		super(name, "Structures", null);
	}

	public IFDStructureDataAssociationCollection(String name, String type) {
		super(name, type, null);
	}

	public IFDStructureDataAssociation addAssociation(String name, IFDStructure struc, IFDDataObject data) {
		IFDStructureDataAssociation ssc = (IFDStructureDataAssociation) getAssociationForSingleObj1(struc);
		if (ssc == null) {
			add(ssc = newAssociation(name, struc, data));
		} else if (!ssc.getDataObjectCollection().contains(data)) {
			if (struc.getName() == null)
				struc.setPropertyValue(IFDConst.IFD_PROP_SAMPLE_LABEL, name);
			ssc.getDataObjectCollection().add(data);
		}
		return ssc;
	}

	protected IFDStructureDataAssociation newAssociation(String name, IFDStructure struc, IFDDataObject data) {
			return new IFDStructureDataAssociation(name, struc, data);
	}

}