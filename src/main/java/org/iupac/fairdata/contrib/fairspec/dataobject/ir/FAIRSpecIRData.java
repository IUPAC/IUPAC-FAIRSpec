package org.iupac.fairdata.contrib.fairspec.dataobject.ir;

import org.iupac.fairdata.contrib.fairspec.dataobject.FAIRSpecDataObject;
import org.iupac.fairdata.core.IFDReference;
import org.iupac.fairdata.dataobject.IFDDataObjectRepresentation;

/**
 *
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public final class FAIRSpecIRData extends FAIRSpecDataObject {

	@Override
	protected IFDDataObjectRepresentation newRepresentation(IFDReference ref, Object obj, long len, String type, String subtype) {
		return new FAIRSpecIRDataRepresentation(ref, obj, len, type, subtype);
	}	

}
