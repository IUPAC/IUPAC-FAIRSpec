package org.iupac.fairdata.analysisobject;

import org.iupac.fairdata.core.IFDReference;
import org.iupac.fairdata.core.IFDRepresentation;

abstract public class IFDAnalysisObjectRepresentation extends IFDRepresentation {

	private final static String[] repNames = new String[] {
	};

	public static String[] getRepnames() {
		return repNames;
	}

	public IFDAnalysisObjectRepresentation(IFDReference ref, Object data, long len, String type, String mediaType) {
		super(ref, data, len, type, mediaType);
	}

}
