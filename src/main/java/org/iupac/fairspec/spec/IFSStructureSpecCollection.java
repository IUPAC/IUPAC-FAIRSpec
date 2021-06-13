package org.iupac.fairspec.spec;

import org.iupac.fairspec.api.IFSObjectAPI;
import org.iupac.fairspec.common.IFSCollection;
import org.iupac.fairspec.common.IFSStructure;

@SuppressWarnings({ "serial" })
public class IFSStructureSpecCollection extends IFSCollection<IFSStructureSpec> {

	public IFSStructureSpecCollection(String name) {
		super(name, IFSObjectAPI.ObjectType.StructureSpecCollection);
	}

	/**
	 * Add a spectrum to a collection that has this structure as its sole associated
	 * structure. A one-to-many structure-to-spectrum may result
	 * 
	 * @param name
	 * @param struc
	 * @param spec
	 * @return this
	 */
	public IFSStructureSpecCollection addSpec(String name, IFSStructure struc, IFSSpecData spec) {
		IFSStructureSpec ssc = get(struc);
		if (ssc == null) {
			add(new IFSStructureSpec(name, struc, spec));
		} else if (!ssc.getSpecDataCollection().contains(spec)) {
			ssc.getSpecDataCollection().add(spec);
		}
		return this;
	}

	/**
	 * Find the structure collection associated with this structure as its only item.
	 * 
	 * @param struc
	 * @return the found item or null
	 */
	private IFSStructureSpec get(IFSStructure struc) {
		for (IFSStructureSpec ssc : this) {
			if (ssc.getStructureCollection().size() == 1 && ssc.getFirstStructure() == struc)
				return ssc;
		}
		return null;
	}
	
}