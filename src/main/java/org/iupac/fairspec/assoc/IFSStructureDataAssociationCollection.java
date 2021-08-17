package org.iupac.fairspec.assoc;

import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.core.IFSCollection;
import org.iupac.fairspec.core.IFSDataObject;
import org.iupac.fairspec.spec.IFSStructureSpec;
import org.iupac.fairspec.struc.IFSStructure;

@SuppressWarnings({ "serial" })
public abstract class IFSStructureDataAssociationCollection extends IFSCollection<IFSStructureDataAssociation> {

	public IFSStructureDataAssociationCollection(String name, String type) throws IFSException {
		super(name, type);
	}

	/**
	 * Find the structure collection associated with this structure as its only
	 * item.
	 * 
	 * @param struc
	 * @return the found item or null
	 */
	public IFSStructureDataAssociation getAssociationForSingleStruc(IFSStructure struc) {
		for (IFSStructureDataAssociation ssc : this) {
			if (ssc.getStructureCollection().size() == 1 && ssc.getFirstStructure() == struc)
				return ssc;
		}
		return null;
	}

	public IFSStructureDataAssociation addAssociation(String name, IFSStructure struc, IFSDataObject<?> data) {
		IFSStructureDataAssociation ssc = getAssociationForSingleStruc(struc);
		if (ssc == null) {
			add(ssc = newAssociation(name, struc, data));
		} else if (!ssc.getDataObjectCollection().contains(data)) {
			if (struc.getName() == null)
				struc.setPropertyValue(IFSStructure.IFS_PROP_STRUC_COMPOUND_LABEL, name);
			ssc.getDataObjectCollection().add(data);
		}
		return ssc;
	}

	protected abstract IFSStructureDataAssociation newAssociation(String name, IFSStructure struc,
			IFSDataObject<?> data);

	
	public IFSStructure findStructureForSpec(IFSDataObject<?> data) {
		for (IFSStructureDataAssociation a : this) {
			if (a.getDataObjectCollection().indexOf(data) >= 0) {
				return a.getStructureCollection().get(0);
			}
		}
		return null;
	}

	public IFSStructureSpec getAssociationForSingleSpec(IFSDataObject<?> data) {
		for (IFSStructureDataAssociation a : this) {
			if (a.getDataObjectCollection().get(0) == data) {
				return (IFSStructureSpec) a;
			}
		}
		return null;
	}

	public IFSStructureDataAssociation findAssociation(IFSStructure struc, IFSDataObject<?> data) {
		for (IFSStructureDataAssociation a : this) {
			if (a.getDataObjectCollection().indexOf(data) >= 0) {
				if (a.getStructureCollection().indexOf(struc) >= 0) {
					return a;
				}
			}
		}
		return null;
	}

	
}