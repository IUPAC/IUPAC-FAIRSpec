package org.iupac.fairspec.spec;

import org.iupac.fairspec.assoc.IFSStructureDataAssociation;
import org.iupac.fairspec.core.IFSStructure;
import org.iupac.fairspec.core.IFSStructureCollection;

/**
 * An abstract class to correlation one or more IFSStructure with one or more
 * IFSSpecData objects. Only two array items are allowed -- one
 * IFSStructureCollection and one IFSSpecDataCollection.
 * 
 * Generally, but not necessarily, this will be one structure and one spectrum.
 * But nothing says the spectrum collection could not be more than one spectrum,
 * and the structure collection could not be more than one structure. Thus, we
 * allow for a "many to one" (spectrum of a mixture), "one to many," (NMR, IR,
 * MS of a specific compound), and "many to many" (mixture with multiple NMR)
 * associations.
 * 
 * This class implements IFSAstractObjectI and does not allow representations.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class IFSStructureSpec extends IFSStructureDataAssociation {
	
	public IFSStructureSpec(String name, IFSStructure structure, IFSSpecData data) {
		super(name, ObjectType.StructureSpec, new IFSStructureCollection("structures", structure), new IFSSpecDataCollection("specData", data));
	}

	public IFSStructureSpec(String name, IFSStructureCollection structureCollection, IFSSpecDataCollection specDataCollection) {
		super(name, ObjectType.StructureSpec, structureCollection, specDataCollection);
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof IFSStructureSpec))
			return false;
		IFSStructureSpec ss = (IFSStructureSpec) o;
		return (ss.get(0).equals(get(0)) && ss.get(1).equals(get(1)));
	}

	public IFSSpecDataCollection getSpecDataCollection() {
		return (IFSSpecDataCollection) get(1);
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

	public IFSSpecData getFirstDataObject() {
		return getSpecDataCollection().get(0);
	}


}
