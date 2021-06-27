package org.iupac.fairspec.assoc;

import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.core.IFSCollection;

@SuppressWarnings({ "serial" })
public abstract class IFSSampleAssociationCollection extends IFSCollection<IFSSampleAssociation> {

	public IFSSampleAssociationCollection(String name, String type) throws IFSException {
		super(name, type);
	}

}