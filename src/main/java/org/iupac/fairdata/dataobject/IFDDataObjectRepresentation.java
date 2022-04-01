package org.iupac.fairdata.core;

import org.iupac.fairdata.common.IFDReference;
import org.iupac.fairdata.common.IFDRepresentation;

public abstract class IFDDataObjectRepresentation extends IFDRepresentation {

	public IFDDataObjectRepresentation(IFDReference ref, Object data, long len, String type, String subtype) {
		super(ref, data, len, type, subtype);
	}

}
