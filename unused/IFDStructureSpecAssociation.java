package org.iupac.fairdata.spec;

import org.iupac.fairdata.assoc.IFDStructureDataAssociation;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDCollection;
import org.iupac.fairdata.struc.IFDStructure;
import org.iupac.fairdata.struc.IFDStructureCollection;

/**
 * A class that correlates one or more IFDStructure with one or more
 * IFDSpecData objects. Only two array items are allowed -- one
 * IFDStructureCollection and one IFDSpecDataCollection.
 * 
 * Generally, but not necessarily, this will be one structure and one spectrum.
 * But nothing says the spectrum collection could not be more than one spectrum,
 * and the structure collection could not be more than one structure. Thus, we
 * allow for a "many to one" (spectrum of a mixture), "one to many," (NMR, IR,
 * MS of a specific compound), and "many to many" (mixture with multiple NMR)
 * associations.
 * 
 * This class implements IFDAstractObjectI and does not allow representations.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class IFDStructureDataAssociation extends IFDStructureDataAssociation {
	
	@Override
	public Class<?>[] getObjectTypes() {
		return new Class<?>[] { IFDSpecData.class };
	}

	public IFDStructureDataAssociation(String name, IFDStructure structure, IFDSpecData data) throws IFDException {
		super(name, IFDSpecDataFindingAid.SpecType.StructureData, new IFDStructureCollection("structures", structure), new IFDSpecDataCollection("specData", data));
	}

	public IFDStructureDataAssociation(String name, IFDStructureCollection structureCollection, IFDSpecDataCollection specDataCollection) throws IFDException {
		super(name, IFDSpecDataFindingAid.SpecType.StructureData, structureCollection, specDataCollection);		
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof IFDStructureDataAssociation))
			return false;
		IFDStructureDataAssociation ss = (IFDStructureDataAssociation) o;
		return (ss.get(0).equals(get(0)) && ss.get(1).equals(get(1)));
	}

	public IFDSpecDataCollection getSpecDataCollection() {	
		return (IFDSpecDataCollection) (IFDCollection<?>) get(1);
	}


}
