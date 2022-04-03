package org.iupac.fairdata.analysis;

import org.iupac.fairdata.common.IFDReference;
import org.iupac.fairdata.core.IFDRepresentation;

public class IFDAnalysisObjectRepresentation extends IFDRepresentation {

	private final static String[] repNames = new String[] {
	};

	public static String[] getRepnames() {
		return repNames;
	}

	public IFDAnalysisObjectRepresentation(IFDReference ref, Object data, long len, String type, String subtype) {
		super(ref, data, len, type, subtype);
	}

}
