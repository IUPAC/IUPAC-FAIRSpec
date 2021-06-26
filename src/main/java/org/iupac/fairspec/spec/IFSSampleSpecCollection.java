package org.iupac.fairspec.spec;

import org.iupac.fairspec.api.IFSObjectI;
import org.iupac.fairspec.assoc.IFSSampleAssociationCollection;
import org.iupac.fairspec.common.IFSException;

/**
 * A collection of IFSSampleAssociation objects.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings({ "serial" })
public class IFSSampleSpecCollection extends IFSSampleAssociationCollection  {

	public IFSSampleSpecCollection(String name) throws IFSException {
		super(name, IFSObjectI.ObjectType.SampleSpecCollection);
	}
	
}