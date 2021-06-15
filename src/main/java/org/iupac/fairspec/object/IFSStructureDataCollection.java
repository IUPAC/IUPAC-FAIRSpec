package org.iupac.fairspec.object;

@SuppressWarnings({ "serial" })
public abstract class IFSStructureDataCollection extends IFSCollection<IFSStructureData> {

	public IFSStructureDataCollection(String name, ObjectType type) {
		super(name, type);
	}

	/**
	 * Find the structure collection associated with this structure as its only item.
	 * 
	 * @param struc
	 * @return the found item or null
	 */
	public IFSStructureData get(IFSStructure struc) {
		for (IFSStructureData ssc : this) {
			if (ssc.getStructureCollection().size() == 1 && ssc.getFirstStructure() == struc)
				return ssc;
		}
		return null;
	}
	
}