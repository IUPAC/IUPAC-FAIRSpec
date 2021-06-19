package org.iupac.fairspec.data;

import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.common.IFSRepresentation;

public class IFSDataObjectRepresentation extends IFSRepresentation {

	public IFSDataObjectRepresentation(String type, IFSReference ref, Object data, long len) {
		super(type, ref, data, len);
	}

}
