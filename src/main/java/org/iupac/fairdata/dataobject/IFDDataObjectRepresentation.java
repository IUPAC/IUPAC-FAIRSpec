package org.iupac.fairdata.dataobject;

import org.iupac.fairdata.core.IFDReference;
import org.iupac.fairdata.core.IFDRepresentation;

abstract public class IFDDataObjectRepresentation extends IFDRepresentation {

	public IFDDataObjectRepresentation(IFDReference ref, Object data, long len, String type, String mediatype) {
		super(ref, data, len, type, mediatype);
	}

}
