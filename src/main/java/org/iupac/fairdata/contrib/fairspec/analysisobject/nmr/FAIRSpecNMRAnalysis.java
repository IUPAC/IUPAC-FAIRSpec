package org.iupac.fairdata.contrib.fairspec.analysisobject.nmr;

import org.iupac.fairdata.contrib.fairspec.analysisobject.FAIRSpecAnalysisObject;
import org.iupac.fairdata.contrib.fairspec.dataobject.nmr.FAIRSpecNMRDataRepresentation;
import org.iupac.fairdata.core.IFDReference;
import org.iupac.fairdata.dataobject.IFDDataObjectRepresentation;

/**
 *
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public final class FAIRSpecNMRAnalysis extends FAIRSpecAnalysisObject {
	
	public FAIRSpecNMRAnalysis() {
		super("nmr");
	}

	@Override
	protected IFDDataObjectRepresentation newRepresentation(IFDReference ref, Object obj, long len, String type, String subtype) {
		return new FAIRSpecNMRDataRepresentation(ref, obj, len, type, subtype);

	}	
	
}
