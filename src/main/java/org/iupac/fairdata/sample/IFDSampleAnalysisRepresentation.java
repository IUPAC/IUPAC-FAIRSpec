package org.iupac.fairdata.sample;

import org.iupac.fairdata.common.IFDReference;
import org.iupac.fairdata.common.IFDRepresentation;

public abstract class IFDSampleAnalysisRepresentation extends IFDRepresentation {

	public IFDSampleAnalysisRepresentation(IFDReference ref, Object data, long len, String type, String subtype) {
		super(ref, data, len, type, subtype);
	}

}
