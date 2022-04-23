package org.iupac.fairdata.analysisobject;

import org.iupac.fairdata.common.IFDConst;
import org.iupac.fairdata.core.IFDCollection;
import org.iupac.fairdata.core.IFDRepresentableObject;

/**
 * A collection of IFDSample objects.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings({ "serial" })
public class IFDAnalysisObjectCollection extends IFDCollection<IFDRepresentableObject<IFDAnalysisObjectRepresentation>> {

	private static String propertyPrefix = IFDConst.concat(IFDConst.IFD_PROPERTY_FLAG, IFDConst.IFD_ANALYSISOBJECT_FLAG);
	
	@Override
	protected String getPropertyPrefix() {
		return propertyPrefix;
	}

	public IFDAnalysisObjectCollection() {
		super(null, null);
	}
	
	
	public IFDAnalysisObjectCollection(IFDAnalysisObject ao) {
		this();
		add(ao);
	}

}