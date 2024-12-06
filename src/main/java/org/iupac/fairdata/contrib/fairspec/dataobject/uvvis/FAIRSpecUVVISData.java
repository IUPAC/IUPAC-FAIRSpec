package org.iupac.fairdata.contrib.fairspec.dataobject.uvvis;

import org.iupac.fairdata.contrib.fairspec.dataobject.FAIRSpecDataObject;
import org.iupac.fairdata.core.IFDReference;
import org.iupac.fairdata.dataobject.IFDDataObjectRepresentation;

/**
 *
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class FAIRSpecUVVISData extends FAIRSpecDataObject {

	public FAIRSpecUVVISData() {
		super("uvvis");
	}

	@Override
	protected IFDDataObjectRepresentation newRepresentation(IFDReference ref, Object obj, long len, String type, String subtype) {
		return new FAIRSpecUVVISDataRepresentation(ref, obj, len, type, subtype);
	}
	
}
