package org.iupac.fairspec.spec;

import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.common.IFSObject;
import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.common.IFSStructure;
import org.iupac.fairspec.common.IFSStructureCollection;

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
public class IFSStructureSpec extends IFSObject<IFSObject<?>> {
	
	public IFSStructureSpec(String name, IFSStructure structure, IFSSpecData data) {
		super(name, ObjectType.StructureSpec, 2, new IFSStructureCollection("structures", structure), new IFSSpecDataCollection("specData", data));
	}

	public IFSStructureSpec(String name, IFSStructureCollection structureCollection, IFSSpecDataCollection specDataCollection) {
		super(name, ObjectType.StructureSpec, 2, structureCollection, specDataCollection);
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof IFSStructureSpec))
			return false;
		IFSStructureSpec ss = (IFSStructureSpec) o;
		return (ss.get(0).equals(get(0)) && ss.get(1).equals(get(1)));
	}

	public IFSStructureCollection getStructureCollection() {
		return (IFSStructureCollection) get(0);
	}

	public IFSSpecDataCollection getSpecDataCollection() {
		return (IFSSpecDataCollection) get(1);
	}

	@Override
	protected IFSStructureSpec newRepresentation(String objectName, IFSReference ifsReference, Object object, long len) throws IFSException {
		throw new IFSException("IFSStructureSpec is an abstract object; representations are not allowed");
	}

	public IFSStructure getStructure(int i) {
		return getStructureCollection().get(i);
	}

	public IFSSpecData getSpecData(int i) {
		return getSpecDataCollection().get(i);
	}

	public IFSStructure getFirstStructure() {
		return getStructureCollection().get(0);
	}

	public IFSSpecData getFirstSpecData() {
		return getSpecDataCollection().get(0);
	}


}
