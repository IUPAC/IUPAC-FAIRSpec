package org.iupac.fairspec.spec;

import org.iupac.fairspec.api.IFSObjectI;
import org.iupac.fairspec.assoc.IFSStructureDataAssociation;
import org.iupac.fairspec.assoc.IFSStructureDataAssociationCollection;
import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.core.IFSDataObject;
import org.iupac.fairspec.struc.IFSStructure;

@SuppressWarnings({ "serial" })
public class IFSStructureSpecCollection extends IFSStructureDataAssociationCollection {

	public IFSStructureSpecCollection(String name) throws IFSException {
		super(name, IFSObjectI.ObjectType.StructureSpecCollection);
	}

	/**
	 * A collection of IFSStructureSpec objects. 
	 * 
	 * @param name
	 * @param struc
	 * @param spec
	 * @return this
	 * @throws IFSException 
	 */
	public IFSStructureSpec addSpec(String name, IFSStructure struc, IFSSpecData spec) {
		return (IFSStructureSpec) super.addData(name, struc, spec);
	}

	@Override
	protected IFSStructureDataAssociation newAssociation(String name, IFSStructure struc, IFSDataObject<?> data) {
		try {
			return new IFSStructureSpec(name, struc, (IFSSpecData) data);
		} catch (IFSException e) {
			// unattainable
			return null;
		}
	}

}