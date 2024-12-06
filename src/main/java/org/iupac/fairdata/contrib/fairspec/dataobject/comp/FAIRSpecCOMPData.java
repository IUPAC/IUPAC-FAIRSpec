package org.iupac.fairdata.contrib.fairspec.dataobject.comp;

import org.iupac.fairdata.contrib.fairspec.dataobject.FAIRSpecDataObject;
import org.iupac.fairdata.core.IFDReference;
import org.iupac.fairdata.dataobject.IFDDataObjectRepresentation;

/**
 * An extension of an IFDDataObject 
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class FAIRSpecCOMPData extends FAIRSpecDataObject {

	public FAIRSpecCOMPData() {
		super("comp");
	}

	@Override
	protected IFDDataObjectRepresentation newRepresentation(IFDReference ref, Object obj, long len, String type, String subtype) {
		return new FAIRSpecCOMPDataRepresentation(ref, obj, len, type, subtype);
	}	

	
}