package org.iupac.fairdata.core;

import org.iupac.fairdata.analysis.IFDAnalysisObject;
import org.iupac.fairdata.api.IFDAnalysisI;
import org.iupac.fairdata.common.IFDException;

/**
 * An IFDAssociation with an added IFDAnalysisObject. 
 * 
 * See IFDAnalysisObject for details.
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class IFDAnalysis extends IFDAssociation implements IFDAnalysisI {
	
	IFDAnalysisObject analysis;
	
	public IFDAnalysis(String name, String type, IFDCollection<IFDObject<?>> collection1, IFDCollection<IFDObject<?>> collection2, IFDAnalysisObject analysis) throws IFDException {
		super(name, type, collection1, collection2);
		if (analysis == null)
			analysis = new IFDAnalysisObject(null, null);
		types = new Class<?>[] { collection1.getClass(), collection2.getClass(), analysis.getClass() };
	}
	
}
