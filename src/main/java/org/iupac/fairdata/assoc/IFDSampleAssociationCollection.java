package org.iupac.fairdata.assoc;

import org.iupac.fairdata.common.IFDException;
import org.iupac.fairdata.core.IFDCollection;

@SuppressWarnings({ "serial" })
public abstract class IFDSampleAssociationCollection extends IFDCollection<IFDSampleAssociation> {

	public IFDSampleAssociationCollection(String name, String type) throws IFDException {
		super(name, type);
	}

}