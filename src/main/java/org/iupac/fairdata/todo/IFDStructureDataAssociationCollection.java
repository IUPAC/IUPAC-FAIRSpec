package org.iupac.fairdata.assoc;

import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDCollection;
import org.iupac.fairdata.core.IFDDataObject;
import org.iupac.fairdata.core.IFDDataObjectCollection;
import org.iupac.fairdata.spec.IFDStructureSpec;
import org.iupac.fairdata.struc.IFDStructure;

@SuppressWarnings({ "serial" })
public abstract class IFDStructureDataAssociationCollection extends IFDCollection<IFDStructureDataAssociation> {

	public IFDStructureDataAssociationCollection(String name, String type) throws IFDException {
		super(name, type);
	}

	/**
	 * Find the structure collection associated with this structure as its only
	 * item.
	 * 
	 * @param struc
	 * @return the found item or null
	 */
	public IFDStructureDataAssociation getAssociationForSingleStruc(IFDStructure struc) {
		for (IFDStructureDataAssociation ssc : this) {
			if (ssc.getStructureCollection().size() == 1 && ssc.getFirstStructure() == struc)
				return ssc;
		}
		return null;
	}

	public IFDStructureDataAssociation addAssociation(String name, IFDStructure struc, IFDDataObject<?> data) {
		IFDStructureDataAssociation ssc = getAssociationForSingleStruc(struc);
		if (ssc == null) {
			add(ssc = newAssociation(name, struc, data));
		} else if (!ssc.getDataObjectCollection().contains(data)) {
			if (struc.getName() == null)
				struc.setPropertyValue(IFDStructure.IFD_PROP_STRUC_COMPOUND_LABEL, name);
			ssc.getDataObjectCollection().add(data);
		}
		return ssc;
	}

	protected abstract IFDStructureDataAssociation newAssociation(String name, IFDStructure struc,
			IFDDataObject<?> data);

	
	public IFDStructure findStructureForSpec(IFDDataObject<?> data, boolean andRemove) {
		for (IFDStructureDataAssociation a : this) {
			IFDDataObjectCollection<IFDDataObject<?>> c = a.getDataObjectCollection();
			int i = c.indexOf(data);
			if (i >= 0) {
				c.remove(i);
				return a.getStructureCollection().get(0);
			}
		}
		return null;
	}

	public IFDStructureSpec getAssociationForSingleSpec(IFDDataObject<?> data) {
		for (IFDStructureDataAssociation a : this) {
			if (a.getDataObjectCollection().get(0) == data) {
				return (IFDStructureSpec) a;
			}
		}
		return null;
	}

	public IFDStructureDataAssociation findAssociation(IFDStructure struc, IFDDataObject<?> data) {
		for (IFDStructureDataAssociation a : this) {
			if (a.getDataObjectCollection().indexOf(data) >= 0) {
				if (a.getStructureCollection().indexOf(struc) >= 0) {
					return a;
				}
			}
		}
		return null;
	}

	
}