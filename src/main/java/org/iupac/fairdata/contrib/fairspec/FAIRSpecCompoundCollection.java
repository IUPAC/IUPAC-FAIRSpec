package org.iupac.fairdata.contrib.fairspec;

import org.iupac.fairdata.derived.IFDStructureDataAssociationCollection;

/**
 * A class that identifies this as an IFDStructureDataAssociationCollection 
 * specifically with elements of type FAIRSpecCompoundAssociation.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class FAIRSpecCompoundCollection extends IFDStructureDataAssociationCollection {

	protected FAIRSpecCompoundCollection(boolean byID) {
		super(byID);
	}
}
