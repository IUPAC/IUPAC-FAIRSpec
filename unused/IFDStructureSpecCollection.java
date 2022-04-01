package org.iupac.fairdata.spec;

import org.iupac.fairdata.assoc.IFDStructureDataAssociation;
import org.iupac.fairdata.assoc.IFDStructureDataAssociationCollection;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDDataObject;
import org.iupac.fairdata.struc.IFDStructure;

@SuppressWarnings({ "serial" })
public class IFDStructureDataCollection extends IFDStructureDataAssociationCollection {

	@Override
	public Class<?>[] getObjectTypes() {
		return new Class<?>[] { IFDStructure.class, IFDSpecData.class };
	}
	

	public IFDStructureDataCollection(String name) throws IFDException {
		super(name, IFDSpecDataFindingAid.SpecType. StructureDataCollection);
	}

	/**
	 * A collection of IFDStructureData objects. 
	 * 
	 * @param name
	 * @param struc
	 * @param spec
	 * @return this
	 * @throws IFDException 
	 */
	public IFDStructureDataAssociation addSpec(String name, IFDStructure struc, IFDSpecData spec) {
		return (IFDStructureDataAssociation) super.addAssociation(name, struc, spec);
	}

	@Override
	protected IFDStructureDataAssociation newAssociation(String name, IFDStructure struc, IFDDataObject<?> data) {
		try {
			return new IFDStructureDataAssociation(name, struc, (IFDSpecData) data);
		} catch (IFDException e) {
			// unattainable
			return null;
		}
	}

}