package org.iupac.fairdata.contrib.fairspec.dataobject.nmr;

import org.iupac.fairdata.contrib.fairspec.dataobject.FAIRSpecDataObject;
import org.iupac.fairdata.core.IFDReference;
import org.iupac.fairdata.dataobject.IFDDataObjectRepresentation;

/**
 *
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public final class FAIRSpecNMRData extends FAIRSpecDataObject {
	
	@Override
	protected IFDDataObjectRepresentation newRepresentation(IFDReference ref, Object obj, long len, String type, String subtype) {
		return new FAIRSpecNMRDataRepresentation(ref, obj, len, type, subtype);

	}	
	
}
