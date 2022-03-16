package org.iupac.fairdata.spec;

import org.iupac.fairdata.assoc.IFDStructureDataAssociation;
import org.iupac.fairdata.assoc.IFDStructureDataAssociationCollection;
import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDDataObject;
import org.iupac.fairdata.struc.IFDStructure;

@SuppressWarnings({ "serial" })
public class IFDStructureSpecCollection extends IFDStructureDataAssociationCollection {

	public IFDStructureSpecCollection(String name) throws IFDException {
		super(name, IFDSpecDataFindingAid.SpecType. StructureSpecCollection);
	}

	/**
	 * A collection of IFDStructureSpec objects. 
	 * 
	 * @param name
	 * @param struc
	 * @param spec
	 * @return this
	 * @throws IFDException 
	 */
	public IFDStructureSpec addSpec(String name, IFDStructure struc, IFDSpecData spec) {
		return (IFDStructureSpec) super.addAssociation(name, struc, spec);
	}

	@Override
	protected IFDStructureDataAssociation newAssociation(String name, IFDStructure struc, IFDDataObject<?> data) {
		try {
			return new IFDStructureSpec(name, struc, (IFDSpecData) data);
		} catch (IFDException e) {
			// unattainable
			return null;
		}
	}

}