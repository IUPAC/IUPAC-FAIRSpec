package org.iupac.fairdata.struc;

import org.iupac.fairdata.common.IFDReference;
import org.iupac.fairdata.common.IFDRepresentation;

public abstract class IFDStructureAnalysisRepresentation extends IFDRepresentation {

	private final static String[] repNames = new String[] {
	};

	public static String[] getRepnames() {
		return repNames;
	}

	public IFDStructureAnalysisRepresentation(IFDReference ref, Object data, long len, String type, String subtype) {
		super(ref, data, len, type, subtype);
	}

}
