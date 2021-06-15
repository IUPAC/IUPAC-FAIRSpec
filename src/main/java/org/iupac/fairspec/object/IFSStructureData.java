package org.iupac.fairspec.object;

import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.common.IFSReference;

/**
 * An class to correlation one or more IFSStructure with one or more
 * IFSDataObject. Only two objects are allowed -- one IFSStructureCollection and
 * one IFSSpecDataCollection. 
 * 
 * Generally, but not necessarily, this will be one
 * structure and one spectrum. But nothing says the spectrum collection could
 * not be more than one spectrum, and the structure collection could not be more
 * than one structure. Thus, we allow for a "many to one" (spectrum of a mixture), 
 * "one to many," (NMR, IR, MS of a specific compound), and "many to many" (mixture with multiple NMR) associations.
 * 
 * An abstract object that does not allow representations. 
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public abstract class IFSStructureData extends IFSObject<IFSObject<?>> {
	
	public IFSStructureData(String name, ObjectType type, IFSStructureCollection structureCollection, IFSDataObjectCollection<?> dataCollection) {
		super(name, type, 2, structureCollection, dataCollection);
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof IFSStructureData))
			return false;
		IFSStructureData ss = (IFSStructureData) o;
		return (ss.get(0).equals(get(0)) && ss.get(1).equals(get(1)));
	}

	public IFSStructureCollection getStructureCollection() {
		return (IFSStructureCollection) get(0);
	}

	public IFSDataObjectCollection<?> getDataObjectCollection() {
		return (IFSDataObjectCollection<?>) get(1);
	}

	public IFSStructure getStructure(int i) {
		return getStructureCollection().get(i);
	}

	public IFSDataObject<?> getDataObject(int i) {
		return (IFSDataObject<?>) getDataObjectCollection().get(i);
	}

	public IFSStructure getFirstStructure() {
		return getStructureCollection().get(0);
	}

	public IFSDataObject<?> getFirstDataObject() {
		return (IFSDataObject<?>) getDataObjectCollection().get(0);
	}

	@Override
	protected IFSStructureData newRepresentation(String objectName, IFSReference ifsReference, Object object, long len) throws IFSException {
		throw new IFSException("IFSStructureData is an abstract object; representations are not allowed");
	}


}
