package org.iupac.fairdata.contrib.fairspec.dataobject.hrms;

import org.iupac.fairdata.contrib.fairspec.dataobject.FAIRSpecDataObject;
import org.iupac.fairdata.core.IFDReference;
import org.iupac.fairdata.dataobject.IFDDataObjectRepresentation;

/**
 * A final class for high-resolution mass spec data.
 * It is final because it is created by reflection.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public final class FAIRSpecHRMSData extends FAIRSpecDataObject {

	@Override
	protected IFDDataObjectRepresentation newRepresentation(IFDReference ref, Object obj, long len, String type, String subtype) {
		return new FAIRSpecHRMSDataRepresentation(ref, obj, len, type, subtype);
	}


}
