package org.iupac.fairdata.analysis;

import org.iupac.fairdata.core.IFDReference;
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
@SuppressWarnings("serial")
public class IFDAnalysisObject extends IFDRepresentableObject<IFDAnalysisObjectRepresentation> {

	{
		setProperties("IFD_PROP_ANALYSIS_OBJECT_", null);
	}

	public IFDAnalysisObject(String name, String type) {
		super(name, type);
	}

	public IFDAnalysisObject(String path, String param, String value) {
		super(param + ";" + value, null);
		setPath(path);
		setPropertyValue(param, value);
	}


	@Override
	protected IFDRepresentation newRepresentation(String objectName, IFDReference ifdReference, Object object, long len,
			String type, String subtype) {
		// TODO Auto-generated method stub
		return null;
	}

}
