package org.iupac.fairdata.contrib.fairspec.dataobject.unknown;

import org.iupac.fairdata.core.IFDReference;
import org.iupac.fairdata.dataobject.IFDDataObjectRepresentation;

public class FAIRSpecUNKNOWNDataRepresentation extends IFDDataObjectRepresentation {

	public FAIRSpecUNKNOWNDataRepresentation(IFDReference ref, Object data, long len, String type, String subtype) {
		super(ref, data, len, type, null);
	}

}
