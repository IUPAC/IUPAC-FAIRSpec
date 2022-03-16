package org.iupac.fairdata.sample;

import org.iupac.fairdata.common.IFDReference;
import org.iupac.fairdata.common.IFDRepresentation;

/**
 * Some sort of representation of a Sample (image, for example?)
 * 
 * @author hansonr
 *
 */
public class IFDSampleRepresentation extends IFDRepresentation {

	public IFDSampleRepresentation(IFDReference ref, Object data, long len, String type, String subtype) {
		super(ref, data, len, type, subtype);
	}

}
