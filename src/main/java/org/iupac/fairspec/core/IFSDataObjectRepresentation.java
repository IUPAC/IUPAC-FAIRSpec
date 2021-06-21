package org.iupac.fairspec.core;

import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.common.IFSRepresentation;

public class IFSDataObjectRepresentation extends IFSRepresentation {

	public IFSDataObjectRepresentation(IFSReference ref, Object data, long len, String type, String subtype) {
		super(ref, data, len, type, subtype);
	}

}
