package org.iupac.fairspec.spec;

import org.iupac.fairspec.api.IFSObjectI;
import org.iupac.fairspec.assoc.IFSStructureDataAssociation;
import org.iupac.fairspec.assoc.IFSStructureDataAssociationCollection;
import org.iupac.fairspec.core.IFSDataObject;
import org.iupac.fairspec.struc.IFSStructure;

@SuppressWarnings({ "serial" })
public class IFSStructureSpecCollection extends IFSStructureDataAssociationCollection {

	public IFSStructureSpecCollection(String name) {
		super(name, IFSObjectI.ObjectType.StructureSpecCollection);
	}

	public IFSStructureSpecCollection(String name, ObjectType type) {
		super(name, type);
	}


	/**
	 * A collection of IFSStructureSpec objects. 
	 * 
	 * @param name
	 * @param struc
	 * @param spec
	 * @return this
	 */
	public IFSStructureSpec addSpec(String name, IFSStructure struc, IFSSpecData spec) {
		return (IFSStructureSpec) super.addData(name, struc, spec);
	}

	@Override
	protected IFSStructureDataAssociation newAssociation(String name, IFSStructure struc, IFSDataObject<?> data) {
		return new IFSStructureSpec(name, struc, (IFSSpecData) data);
	}

}