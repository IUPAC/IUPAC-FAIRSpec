package org.iupac.fairspec.assoc;

import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.core.IFSCollection;
import org.iupac.fairspec.core.IFSDataObject;
import org.iupac.fairspec.struc.IFSStructure;

@SuppressWarnings({ "serial" })
public abstract class IFSStructureDataAssociationCollection extends IFSCollection<IFSStructureDataAssociation> {

	public IFSStructureDataAssociationCollection(String name, ObjectType type) throws IFSException {
		super(name, type);
	}

	/**
	 * Find the structure collection associated with this structure as its only
	 * item.
	 * 
	 * @param struc
	 * @return the found item or null
	 */
	public IFSStructureDataAssociation get(IFSStructure struc) {
		for (IFSStructureDataAssociation ssc : this) {
			if (ssc.getStructureCollection().size() == 1 && ssc.getFirstStructure() == struc)
				return ssc;
		}
		return null;
	}

	public IFSStructureDataAssociation addData(String name, IFSStructure struc, IFSDataObject<?> data) {
		IFSStructureDataAssociation ssc = get(struc);
		if (ssc == null) {
			add(newAssociation(name, struc, data));
		} else if (!ssc.getDataObjectCollection().contains(data)) {
			ssc.getDataObjectCollection().add(data);
		}
		return ssc;
	}

	protected abstract IFSStructureDataAssociation newAssociation(String name, IFSStructure struc,
			IFSDataObject<?> data);

}