package org.iupac.fairdata.analysis;

import org.iupac.fairdata.common.IFDReference;
import org.iupac.fairdata.core.IFDRepresentableObject;
import org.iupac.fairdata.core.IFDRepresentation;

/**
 * An IFDAnalysisObject is an IFDRepresentableObject that provides
 * representations for an IFSAnalysisI. 
 * 
 * 
 * @author hansonr
 *
 */
public class IFDAnalysisObject extends IFDRepresentableObject<IFDAnalysisRepresentation> {

	public IFDAnalysisObject(String name, String type) {
		super(name, type);
	}

	@Override
	protected IFDRepresentation newRepresentation(String objectName, IFDReference ifdReference, Object object, long len,
			String type, String subtype) {
		// TODO Auto-generated method stub
		return null;
	}

}
