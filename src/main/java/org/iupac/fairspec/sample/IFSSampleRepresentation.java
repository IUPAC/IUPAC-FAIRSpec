package org.iupac.fairspec.sample;

import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.common.IFSRepresentation;

/**
 * Some sort of representation of a Sample (image, for example?)
 * 
 * @author hansonr
 *
 */
public class IFSSampleRepresentation extends IFSRepresentation {

	public IFSSampleRepresentation(IFSReference ref, Object data, long len, String type, String subtype) {
		super(ref, data, len, type, subtype);
	}

}
