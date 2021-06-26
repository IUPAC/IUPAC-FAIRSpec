package org.iupac.fairspec.struc;

import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.common.IFSRepresentation;

public class IFSStructureAnalysisRepresentation extends IFSRepresentation {

	private final static String[] repNames = new String[] {
	};

	public static String[] getRepnames() {
		return repNames;
	}

	public IFSStructureAnalysisRepresentation(IFSReference ref, Object data, long len, String type, String subtype) {
		super(ref, data, len, type, subtype);
	}

}
