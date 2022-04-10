package org.iupac.fairdata.contrib.fairspec.dataobject.ir;

import org.iupac.fairdata.core.IFDReference;
import org.iupac.fairdata.dataobject.IFDDataObjectRepresentation;

public class FAIRSpecIRDataRepresentation extends IFDDataObjectRepresentation {

	public FAIRSpecIRDataRepresentation(IFDReference ref, Object data, long len, String type, String subtype) {
		super(ref, data, len, type, null);
	}

}
