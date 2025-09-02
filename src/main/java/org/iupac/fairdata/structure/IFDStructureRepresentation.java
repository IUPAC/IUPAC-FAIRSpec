package org.iupac.fairdata.structure;

import org.iupac.fairdata.core.IFDReference;
import org.iupac.fairdata.core.IFDRepresentation;

public class IFDStructureRepresentation extends IFDRepresentation {

	/**
	 * 
	 * @param ref
	 * @param data
	 * @param len
	 * @param ifdStructureType
	 * @param mediaType
	 */
	public IFDStructureRepresentation(IFDReference ref, Object data, long len, String ifdStructureType, String mediaType) {
		super(ref, data, len, ifdStructureType, mediaType);
	}

	/**
	 * Allow for transfer from a temporary generic representation to a structure representation.
	 * 
	 * @param rep
	 */
	public IFDStructureRepresentation(IFDRepresentation rep) {
		super(rep);
	}

}
