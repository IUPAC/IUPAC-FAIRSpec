package org.iupac.fairdata.contrib.fairspec.dataobject.raman;

import org.iupac.fairdata.contrib.fairspec.dataobject.FAIRSpecDataObject;
import org.iupac.fairdata.core.IFDReference;
import org.iupac.fairdata.dataobject.IFDDataObjectRepresentation;

/**
 *
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public final class FAIRSpecRamanData extends FAIRSpecDataObject {

	@Override
	protected IFDDataObjectRepresentation newRepresentation(IFDReference ref, Object obj, long len, String type, String subtype) {
		return new FAIRSpecRamanDataRepresentation(ref, obj, len, type, subtype);

	}	

}
