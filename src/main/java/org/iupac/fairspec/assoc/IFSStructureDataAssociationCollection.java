package org.iupac.fairspec.assoc;

import org.iupac.fairspec.core.IFSCollection;
import org.iupac.fairspec.core.IFSStructure;

@SuppressWarnings({ "serial" })
public abstract class IFSStructureDataAssociationCollection extends IFSCollection<IFSStructureDataAssociation> {

	public IFSStructureDataAssociationCollection(String name, ObjectType type) {
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

}