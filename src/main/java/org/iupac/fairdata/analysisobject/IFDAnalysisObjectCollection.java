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

	public IFDAnalysisObjectCollection() {
		super(null, null);
	}
	
	
	public IFDAnalysisObjectCollection(IFDAnalysisObject ao) {
		this();
		add(ao);
	}

}