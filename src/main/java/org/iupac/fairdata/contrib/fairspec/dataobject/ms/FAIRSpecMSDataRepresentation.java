package org.iupac.fairdata.contrib.fairspec.dataobject.ms;

import org.iupac.fairdata.core.IFDReference;
import org.iupac.fairdata.dataobject.IFDDataObjectRepresentation;

public class FAIRSpecMSDataRepresentation extends IFDDataObjectRepresentation {

	public FAIRSpecMSDataRepresentation(IFDReference ref, Object data, long len, String type, String subtype) {
		super(ref, data, len, type, null);
	}

}
