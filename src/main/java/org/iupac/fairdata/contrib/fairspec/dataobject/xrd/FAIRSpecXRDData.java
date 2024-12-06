package org.iupac.fairdata.contrib.fairspec.dataobject.xrd;

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
public class FAIRSpecXRDData extends FAIRSpecDataObject {

	public FAIRSpecXRDData() {
		super("xrd");
	}
	
	@Override
	protected IFDDataObjectRepresentation newRepresentation(IFDReference ref, Object obj, long len, String type, String subtype) {
		return new FAIRSpecXRDDataRepresentation(ref, obj, len, type, subtype);
	}	

	
}