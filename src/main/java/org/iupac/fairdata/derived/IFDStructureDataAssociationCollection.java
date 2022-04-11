package org.iupac.fairdata.derived;

import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDAssociationCollection;
import org.iupac.fairdata.dataobject.IFDDataObject;
import org.iupac.fairdata.structure.IFDStructure;

@SuppressWarnings({ "serial" })
public class IFDStructureDataAssociationCollection extends IFDAssociationCollection {

	public IFDStructureDataAssociationCollection(String label, String type) {
		super(label, type);
	}

	public IFDStructureDataAssociationCollection(String label) {
		this(label, null);
	}

	public IFDStructureDataAssociation addAssociation(IFDStructure struc, IFDDataObject data) throws IFDException {
		IFDStructureDataAssociation ssc = (IFDStructureDataAssociation) getAssociationForSingleObj1(struc);
		if (ssc == null) {
			add(ssc = newAssociation(struc, data));
		} else if (!ssc.getDataObjectCollection().contains(data)) {
			ssc.getDataObjectCollection().add(data);
		}
		return ssc;
	}

	protected IFDStructureDataAssociation newAssociation(IFDStructure struc, IFDDataObject data) throws IFDException {
			return new IFDStructureDataAssociation(struc, data);
	}

}