package org.iupac.fairdata.contrib.fairspec.dataobject.xray;

import org.iupac.fairdata.contrib.fairspec.dataobject.FAIRSpecDataObject;
import org.iupac.fairdata.core.IFDReference;
import org.iupac.fairdata.dataobject.IFDDataObject;
import org.iupac.fairdata.dataobject.IFDDataObjectRepresentation;

/**
 * An extension of an IFDDataObject 
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class FAIRSpecXRAYData extends FAIRSpecDataObject {

	@Override
	protected IFDDataObjectRepresentation newRepresentation(IFDReference ref, Object obj, long len, String type, String subtype) {
		return new FAIRSpecXRAYDataRepresentation(ref, obj, len, type, subtype);
	}	

	
}