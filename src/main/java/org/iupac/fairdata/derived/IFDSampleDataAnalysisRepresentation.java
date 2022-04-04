package org.iupac.fairdata.derived;

import org.iupac.fairdata.analysis.IFDAnalysisObjectRepresentation;
import org.iupac.fairdata.core.IFDReference;

/**
 * just a convenience
 * 
 * @author hansonr
 *
 */

public class IFDSampleDataAnalysisRepresentation extends IFDAnalysisObjectRepresentation {

	public IFDSampleDataAnalysisRepresentation(IFDReference ref, Object data, long len, String type, String subtype) {
		super(ref, data, len, type, subtype);
	}

}
